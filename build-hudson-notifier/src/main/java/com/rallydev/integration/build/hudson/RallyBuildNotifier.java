/*
 * Copyright 2007- the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rallydev.integration.build.hudson;

import hudson.Launcher;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Notifier;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rallydev.integration.build.plugin.BuildDefinition;
import com.rallydev.integration.build.plugin.Tag;
import com.rallydev.integration.build.rest.RallyRestService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link RallyBuildNotifier} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)} method
 * will be invoked. 
 *
 */
public class RallyBuildNotifier extends Notifier {
	
    private static final String PLUGIN_NAME = "Hudson Plugin";
    private static final String PLUGIN_VERSION = "2.5.ts.1";
    /**Tag to apply to defect**/
    private static final String defectTagName = "BRM_Build_Failure";
    /**Severity of defect**/
    private static String severity = "Major Problem";
    /**Priority of defect**/
    private static String priority = "Resolve Immediately";

    private String workspaceName;
    private String projectName;
    private boolean createDefectOnFail;
    private RallyRestService service;
    private PrintStream logger;
    private Map<String, String> revisionMethodMap;

    public RallyBuildNotifier(RallyRestService service) {
        this.service = service;
    }
    
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}
	
	//Causes perform to be called after build is final, which is necessary to get non-zero duration
	@Override
	public boolean needsToRunAfterFinalized() {
		return true;
	}

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
    	
    	setLogger(listener.getLogger());
    	String debugFlag = System.getProperty("rally.debug");
    	if (debugFlag != null && debugFlag.equals("true")) {
    	    service.setLogger(listener.getLogger());
    	}
    	//Uncomment the following line for a debug-always-on version
    	//service.setLogger(listener.getLogger());
	    
    	revisionMethodMap = new HashMap<String, String>();  
    	revisionMethodMap.put("Subversion", "getRevision");
    	revisionMethodMap.put("Perforce", "getChangeNumber");
    	revisionMethodMap.put("Git", "getRevision");
    	revisionMethodMap.put("Mercurial", "getNode");
        
        try {
        	checkWorkspaceFlagEnabled(getWorkspaceName());
        	
        	String jobName = build.getProject().getName();
        	String jobDescription = build.getProject().getDescription();
        	String buildDefinitionRef = getBuildDefinition(getWorkspaceName(), getProjectName(), 
        			jobName, jobDescription);
        	if (buildDefinitionRef == null) {
                getLogger().println("Rally Build Publisher Failed: " + 
                		"Unable to find or create build definition named " + jobName + " \n" +
                		"    for Workspace " + getWorkspaceName() + ", Project " + getProjectName());
                build.setResult(Result.UNSTABLE);
                return true;
        	}
        	
        	boolean createDefectOnFail = getCreateDefectOnFail();
        	
        	getLogger().println("  If fail, create defect: " + String.valueOf(createDefectOnFail) );
        	
            com.rallydev.integration.build.plugin.Build rallyBuild = 
            	new com.rallydev.integration.build.plugin.Build(service);
            
            String logUri = getLogFileUrl(jobName, String.valueOf(build.number));
            rallyBuild.setBuildDefinitionId(extractId(buildDefinitionRef));
            rallyBuild.setUri(logUri);
            rallyBuild.setNumber(String.valueOf(build.number));
            rallyBuild.setDuration((float) build.getDuration() / 1000.0f);
            rallyBuild.setStartTime(build.getTimestamp().getTime());
            
            if (build.getResult() == Result.SUCCESS) {
                rallyBuild.setStatus("SUCCESS");
            } else if (build.getResult() == Result.FAILURE) {
                rallyBuild.setMessage("Build for job " + jobName + " failed.");
                rallyBuild.setStatus("FAILURE");
                if ( createDefectOnFail ) {
                	createDefect(build,logUri,"FAILED");
                }
            } else if (build.getResult() == Result.ABORTED) {
            	rallyBuild.setMessage("Build for job " + jobName + " was aborted.");
            	rallyBuild.setStatus("UNKNOWN");
            } else if (build.getResult() == Result.NOT_BUILT) {
            	rallyBuild.setMessage("Build for job " + jobName + " was not built.");
            	rallyBuild.setStatus("INCOMPLETE");
            } else if (build.getResult() == Result.UNSTABLE) {
            	rallyBuild.setMessage("Build for job " + jobName + " was unstable");
            	rallyBuild.setStatus("UNKNOWN");
                if ( createDefectOnFail ) {
                    createDefect(build,logUri,"UNSTABLE");
                }
            } else {
            	rallyBuild.setMessage("Build for job " + jobName + " had unrecognized result: " 
            			+ build.getResult().toString());
            	rallyBuild.setStatus("UNKNOWN");
            }
            
            List<String> refs = findChangeSetRefs(build);
            rallyBuild.setChangeSets(refs);

            rallyBuild.create();
			getLogger().println("Rally Build Notifier created a build object in Rally for " + rallyBuild.getUri());
   
        } catch (Throwable e) {
            e.printStackTrace();
            getLogger().println("Rally Build Notifier failed with exception: " + e.getMessage());
        }

        return true;
    }
    
    private Object invokeMethod(Object object, String methodName) {
    	Object result = null;
		Method[] methods = object.getClass().getDeclaredMethods();
		for (Method m : methods) {
			if (methodName.equals(m.getName())) {
				try {
					result = m.invoke(object);
					break;
				} catch (Throwable e) {
					getLogger().println("Exception " + e.getMessage() + " invoking method " + 
							methodName + " on class " + object.getClass().getName());
				}
			}
		}
		return result;
    }
    
    private List<String> findChangeSetRefs(AbstractBuild<?, ?> build) {
    
		List<String> refs = new ArrayList<String>();
		String revision, workspaceRef;
		Object revisionResult, msgResult;
		
		String scmName = build.getProject().getScm().getDescriptor().getDisplayName();

		try {
			workspaceRef = service.findWorkspace(getWorkspaceName());
		} catch (Throwable e) {
            getLogger().println("Rally Build Notifier failed with exception: " + e.getMessage());
            return refs;
		}
		getLogger().println("SCM type detected: " + scmName);

		ChangeLogSet<? extends Entry> cs =  build.getChangeSet();
		getLogger().println("Result from build.getChangeSet: " + cs.toString());
		getLogger().println("Result from build.isEmptySet: " + cs.isEmptySet());
		
		if (cs.isEmptySet()) {
			getLogger().println("No changesets found for this build");

		} else {
		    if (revisionMethodMap.containsKey(scmName)) {
		        getLogger().println("Changesets found:");
    			for (Object entry : cs.getItems()) {
    			    getLogger().println("ChangeLogSet entry: " + entry.toString());
    				revisionResult = invokeMethod(entry, revisionMethodMap.get(scmName));
    				msgResult = invokeMethod(entry, "getMsg");
    				if (revisionResult != null && msgResult != null) {
    					getLogger().println("   Commit message: " + msgResult);
    					getLogger().println("   Revision: " + revisionResult + "\n");
    					revision = revisionResult.toString();

    					try {
    						String ref = service.findChangeset(revision, workspaceRef);
    						if (ref != null) {
    							refs.add(ref);
    						}

    					} catch (Throwable e) {
    			            e.printStackTrace();
    			            getLogger().println("Exception encountered trying to find ChangeSet: " + 
    			            		revision + "\n" + e.getMessage());
    			            build.setResult(Result.UNSTABLE);
    			        }
    				}
    			}
		    } else {
		        getLogger().println(scmName + " is not a supported SCM for the Rally Build Notifier plugin.");
		    }
		}

		return refs;
    }
    
    protected String getProjectRef(String workspaceName, String projectName) {
    	if (workspaceName == null || projectName == null) {
    		return null;
    	}
    	
    	String projectRef, wsRef;
		try {
			wsRef = service.findWorkspace(workspaceName);
			projectRef = service.findProject(wsRef, projectName);
		} catch (IOException ex) {
			getLogger().println("Exception encountered trying to find Rally workspace " +
					workspaceName + " and project " + projectName);
			getLogger().println(ex.getMessage());
			return null;
		}
		return projectRef;
    }
    
    protected String getBuildDefinition(String workspaceName, String projectName, String jobName, String jobDescription) {
    	if (workspaceName == null || projectName == null) {
    		return null;
    	}
    	
    	String projectRef, wsRef;
		try {
			wsRef = service.findWorkspace(workspaceName);
			projectRef = service.findProject(wsRef, projectName);
		} catch (IOException ex) {
			getLogger().println("Exception encountered trying to find Rally workspace " +
					workspaceName + " and project " + projectName);
			getLogger().println(ex.getMessage());
			return null;
		}

    	String buildDef;
		try {
			buildDef = service.findBuildDefinition(jobName, projectRef);
		} catch (IOException ex) {
			buildDef = null;
		}
    	
    	if (buildDef == null) {
    		buildDef = createBuildDefinition(jobName, jobDescription, projectRef);
    	}
    	
    	return buildDef;
    }
    
    protected String createDefect(AbstractBuild<?, ?> build, String logUri, String status ) {
    	getLogger().println("Rally Build Notifier creating a defect in Rally");
        com.rallydev.integration.build.plugin.Defect rallyDefect = 
            	new com.rallydev.integration.build.plugin.Defect(service);
        
    	String jobName = build.getProject().getName();
    	
    	rallyDefect.setUri(logUri);
    	
    	String linkToLog = "<a href='" + logUri + "'>Jenkins Log</a>";
    	
    	//rallyDefect.setDescription(getLogFileUrl(jobName, String.valueOf(build.number)));
    	rallyDefect.setDescription(linkToLog);
    	rallyDefect.setProjectRef( getProjectRef(getWorkspaceName(), getProjectName()) );

    	String name = "Build " + String.valueOf(build.number) + " for job " + jobName + " " + status;
    	
    	rallyDefect.setName(name);
    	
    	rallyDefect.setFoundInBuild(Integer.toString(build.getNumber()));
    	
    	rallyDefect.setSeverity(severity);
    	
    	rallyDefect.setPriority(priority);
    	
    	try {
    		rallyDefect.create();
    		getLogger().println("Rally Build Notifier created a defect in Rally");
    		try{

    		      getLogger().println("Tagging defect with: "+defectTagName);
    		      Tag rallyTag = new Tag(service, defectTagName);
    	          rallyDefect.tag(rallyTag);
    	          String ref = rallyDefect.getRef();
    	          //This is hacky, sorry but it's a quick addition
    	          String [] id = ref.split("/");
    	          getLogger().println("ref: http://rally1.rallydev.com/#/detail/defect/" + id[id.length-1]);
    		}
    		catch(Exception e){
    		    getLogger().println("Failed to tag the defect: \n"+e.toString());
    		}
    		//return service.findBuildDefinition(jobName, projectRef);
    		return null;
		} catch (IOException ex) {
	    	getLogger().println("Exception: " + ex.getMessage() + " creating Defect");
			ex.printStackTrace();
			return null;
		}
    	
    }
    
    protected String createBuildDefinition(String jobName, String jobDescription, String projectRef) {
    	getLogger().println("Creating Build Definition for job " + jobName + 
    			" in Rally project: " + projectRef);
    	BuildDefinition buildDef = new BuildDefinition(service);
    	buildDef.setName(jobName);
    	if (jobDescription != null) {
    		buildDef.setDescription(jobDescription);
    	}
    	buildDef.setProjectRef(projectRef);
    	try {
    		buildDef.create();
	        return service.findBuildDefinition(jobName, projectRef);
		} catch (IOException ex) {
	    	getLogger().println("Exception: " + ex.getMessage() + " creating Build Definition");
			ex.printStackTrace();
			return null;
		}
    }
    
    protected boolean checkWorkspaceFlagEnabled(String workspaceName) {
    	String flag;
		try {
			flag = service.findWorkspaceFlag(workspaceName);
			if (flag != null && flag.toLowerCase().equals("false")) {
        		getLogger().println("The Build and Changeset flag is not enabled for your workspace. " +
        				"Most build and changeset data will not show in Rally until a " + 
        				"workspace administrator enables this flag by editing the workspace.");
			}
		} catch (IOException ex) {
			getLogger().println("IOException detected attempting to retrieve build/changeset flag for workspace " +
					workspaceName);
			getLogger().println("Message was: " + ex.getMessage());
			return false;
		}
    	return (flag != null && flag.toLowerCase().equals("true"));
    }

    protected String getLogFileUrl(String projectName, String label)
        throws URISyntaxException {
        StringBuffer url = new StringBuffer();
        url.append("http://");
        url.append(DESCRIPTOR.getHudsonServer());

        String context = DESCRIPTOR.getHudsonContext();
        if( context != null && context.length() != 0 ) {
            url.append("/");
            url.append(DESCRIPTOR.getHudsonContext());
        }

        url.append("/job/");
        url.append(projectName);
        url.append("/");
        url.append(label);
        url.append("/");

        StringBuffer sb = new StringBuffer();
        URI uri = new URI(null, url.toString(), null);
        sb.append(uri.toASCIIString());

        return sb.toString();
    }
    
    public static long extractId(String ref) {
    	String[] tokens;
    	tokens = ref.split("/");
    	try {
    		return Long.parseLong(tokens[tokens.length-1]);
    	} catch (NumberFormatException nfe) {
    		return -1L;
    	}    
    }
    
    public static int toInt(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return -1;
		}
	}
    
    public void setWorkspaceName(String workspaceName) {
		this.workspaceName = workspaceName;
	}

	public String getWorkspaceName() {
		return workspaceName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setCreateDefectOnFail(boolean createDefectOnFail) {
		this.createDefectOnFail = createDefectOnFail;
	}

	public boolean getCreateDefectOnFail() {
		return createDefectOnFail;
	}
	
	public void setLogger(PrintStream logger) {
		this.logger = logger;
	}

	public PrintStream getLogger() {
		return logger;
	}

    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }
    
	public void setService (RallyRestService service) {
		this.service = service;
	}

    /**
     * Descriptor for {@link RallyBuildNotifier}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension     
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String rallyServer;
        private String username;
        private String password;
        private String hudsonServer;
        private String hudsonContext = "";
        private String proxyServer;
        private String proxyPort;
        private String proxyUsername;
        private String proxyPassword;
        

		private DescriptorImpl() {
			super(RallyBuildNotifier.class);
			load();
		}
		
        /**
         * Creates a new instance of {@link RallyBuildNotifier} when job config page is saved.
         */
        public RallyBuildNotifier newInstance(StaplerRequest req, JSONObject formData)
            throws FormException {
        	
			//System.out.println("DescriptorImpl newInstance called with JSON " + formData.toString());

            RallyRestService service = new RallyRestService(DESCRIPTOR.getUsername(),
                    DESCRIPTOR.getPassword(), DESCRIPTOR.getRallyServer(), RallyRestService.WSAPI_VERSION, 
                    -1, true, PLUGIN_NAME, PLUGIN_VERSION);
            
            if (DESCRIPTOR.getProxyServer() != null && toInt(DESCRIPTOR.getProxyPort()) >= 0) {
    			service.setProxyInfo(DESCRIPTOR.getProxyServer(), toInt(DESCRIPTOR.getProxyPort()), 
    			    DESCRIPTOR.getProxyUsername(), DESCRIPTOR.getProxyPassword());
    		}
            
            RallyBuildNotifier rallyBuildPublisher = new RallyBuildNotifier(service);
            req.bindParameters(rallyBuildPublisher, "rallybuildpublisher_");

            return rallyBuildPublisher;
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // indicates that this notifier can be used with all kinds of project types 
            return true;
        }

        @Override
		public String getDisplayName() {
            // Displayed in the publisher section
            return "Rally Build Notifier";
        }

        /**
         * Called when global config page is saved.
         */
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        	req.bindJSON(this, formData);
            save();
            return super.configure(req,formData);
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
        
        public String getRallyServer() {
            return rallyServer;
        }

        public void setRallyServer(String host) {
            this.rallyServer = host;
        }

        public String getHudsonServer() {
            return hudsonServer;
        }

        public void setHudsonServer(String hudsonhost) {
            this.hudsonServer = hudsonhost;
        }

        public String getHudsonContext() {
            return hudsonContext;
        }

        public void setHudsonContext(String hudsonContext) {
            this.hudsonContext = hudsonContext;
        }
        
        public String getProxyServer() {
    		return proxyServer;
    	}

    	public void setProxyServer(String proxyServer) {
    	    this.proxyServer = proxyServer;
    	}

    	public String getProxyPort() {
    		return proxyPort;
    	}

    	public void setProxyPort(String proxyPort) {
    		this.proxyPort = proxyPort;
    	}

    	public String getProxyUsername() {
    		return proxyUsername;
    	}

    	public void setProxyUsername(String proxyUsername) {
    		this.proxyUsername = proxyUsername;
    	}

    	public String getProxyPassword() {
    		return proxyPassword;
    	}

    	public void setProxyPassword(String proxyPassword) {
    		this.proxyPassword = proxyPassword;
    	}
    }
}