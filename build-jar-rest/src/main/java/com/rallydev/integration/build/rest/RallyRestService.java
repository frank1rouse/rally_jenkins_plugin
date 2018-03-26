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
package com.rallydev.integration.build.rest;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.httpclient.NameValuePair;

import org.jdom.*;
import org.jdom.input.SAXBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.List;
import java.io.PrintStream;

public class RallyRestService {
    public static final String PRODUCTION = "rally";
    public static final String WSAPI_VERSION = "1.21";
    public static final String BUILD_PLUGIN = "Build Plugin";
    public static final String UNKNOWN_VERSION = "Unknown Version";
    private String username;
    private String password;
    private String host;
    private String version;
    private int port;
    private String pluginName;
    private String pluginVersion;
    private boolean secure;
    private String proxyServer;
    private int proxyPort;
	private String proxyUsername;
	private String proxyPassword;
    private PrintStream logger;

    public RallyRestService(String username, String password, String host,
        String version, int port, boolean secure, String pluginName,
        String pluginVersion) {
        this.host = host;
        this.password = password;
        this.username = username;
        this.version = version;
        this.port = port;
        this.secure = secure;
        this.pluginName = pluginName;
        this.pluginVersion = pluginVersion;
		this.proxyServer = null;
		this.proxyPort = -1;
    }

    public RallyRestService(String username, String password) {
        this(username, password, PRODUCTION, WSAPI_VERSION, -1, true,
            BUILD_PLUGIN, UNKNOWN_VERSION);
    }

    public RallyRestService(String username, String password, String host,
        String pluginName, String pluginVersion) {
        this(username, password, host, WSAPI_VERSION, -1, true, pluginName,
            pluginVersion);
    }

    public RallyRestService(String username, String password, String host) {
        this(username, password, host, WSAPI_VERSION, -1, true, BUILD_PLUGIN,
            UNKNOWN_VERSION);
    }
    
	public String getUrl() {
		String port = (this.port != -1) ? (":" + this.port) : "";
		return (secure ? "https" : "http") + "://" + host + port
				+ "/slm/webservice/" + version;
		//For on-prem (http only), comment out the previous return and uncomment the one below.
		//return "http://" + host + port + "/slm/webservice/" + version;
	}

	public String create(String artifact, String xml) throws IOException {
		return doCreate(xml, new HttpClient(), new PostMethod(getUrl()
				+ "/" + artifact + "/create"));
	}
    
	public String update(String ref, String xml) throws IOException {
		return doCreate(xml, new HttpClient(), new PostMethod(ref));
	}
    
	public String query(String artifact, String query) throws IOException{
	    return doGet(new HttpClient(), new GetMethod(getUrl()+"/"+URLEncoder.encode(artifact,"UTF-8")+"?query="+URLEncoder.encode(query,"UTF-8")));
	}
	
    public String findWorkspace(String workspaceName) throws IOException {
		String response = getSubscriptionInfo();
		if (response == null) {
		    logMessage("findWorkspace: null response from getSubscriptionInfo");
		} else {
		    logMessage("findWorkspace: response from getSubscriptionInfo was " + response);
		}
		
		if (response == null || !isValidResponse(response)) {
            throw new IOException("Query for workspace " + workspaceName + " returned errors: " +
            		getErrors(response));
		}
		SAXBuilder sb = new SAXBuilder();
		Document doc = null;
		Element workspace = null;
		try {
			doc = sb.build(new StringReader(response));
			Element queryResult = doc.getRootElement();
			
			if (queryResult.getChild("Workspaces") != null) {
				
				Element wsElement = queryResult.getChild("Workspaces");
				List workspaces = wsElement.getChildren("Workspace");
			    for (int i = 0; i < workspaces.size(); i++) {
			    	workspace = (Element) workspaces.get(i);
			        if (workspace.getAttributeValue("refObjectName").equals(workspaceName)) {
			        	String wsRef = workspace.getAttributeValue("ref");
			        	String queryStr = "((Name = \"" + workspaceName + "\") AND (State = \"Open\"))";
			        	String tmp = queryForArtifact("workspace", queryStr, wsRef, "false");
			        	if (tmp.indexOf("<Errors>") >= 0) {
			        		throw new IOException("Workspace " + workspaceName + " is closed.");
			        	}
			        	return wsRef;
			        }
			    }
			}

		} catch (JDOMException e) {
            throw new IOException("Query for workspace " + workspaceName + 
            		" caused JDOM exception: " + e.getMessage());
		} catch (IOException e) {
            throw new IOException("Query for workspace " + workspaceName + 
            		" caused IOException: " + e.getMessage());
		}
		
		throw new IOException("Rally workspace " + workspaceName + " could not be found");   
	}
	
	public String findProject(String workspaceRef, String projectName) throws IOException {
		String response;
		List projects;
		
		String workspaceOid = extractId(workspaceRef);
		if (workspaceOid != null) {
			response = getObject("workspace", workspaceOid);
			if (response == null || !isValidResponse(response)) {
	            throw new IOException("Query for workspace " + workspaceRef + " returned errors: " +
	            		getErrors(response));			}
		} else {
            throw new IOException("Invalid workspace ref " + workspaceRef + " looking for project " + projectName);
		}
		
		SAXBuilder sb = new SAXBuilder();
		Document doc = null;
		Element project = null;
		try {
			doc = sb.build(new StringReader(response));
			Element queryResult = doc.getRootElement();
			
			if (queryResult.getChild("Projects") != null) {
				Element projectsElement = queryResult.getChild("Projects");
				projects = projectsElement.getChildren("Project");
			    for (int i = 0; i < projects.size(); i++) {
			        project = (Element) projects.get(i);
			        if (project.getAttributeValue("refObjectName").equals(projectName)) {
			        	return project.getAttributeValue("ref");
			        }
			    }
			}

		} catch (JDOMException e) {
            throw new IOException("Query for project " + projectName + 
            		" caused JDOM exception: " + e.getMessage());
		} catch (IOException e) {
            throw new IOException("Query for project " + projectName + 
            		" caused IOException: " + e.getMessage());
		}
		
		throw new IOException("Rally project " + projectName + " could not be found");   
	}
	
	public String findWorkspaceFlag(String workspaceName) throws IOException {
		NameValuePair wsParam, fetchParam;
		NameValuePair[] pairs;
		String response, workspaceRef=null, workspaceOid=null;

		workspaceRef = findWorkspace(workspaceName);
		workspaceOid = extractId(workspaceRef);
		if (workspaceOid != null) {
			response = getObject("workspace", workspaceOid);
			if (response == null || !isValidResponse(response)) {
				return null;
			}
		} else {
			return null;
		}

		String reqUrl = getUrl() + "/workspaceconfiguration";
		GetMethod get = new GetMethod(reqUrl);
		wsParam = new NameValuePair("workspace", workspaceRef);
		fetchParam = new NameValuePair("fetch", "true");
		pairs = new NameValuePair[] { wsParam, fetchParam };

		get.setQueryString(pairs);
		response = doGet(new HttpClient(), get);
		if (response == null || !isValidResponse(response)) {
			return null;
		} else {
			return extractFlagFromResponse(response);
		}
	}
	
	public String findBuildDefinition(String name, String projectRef) throws IOException {
		String response;
		List buildDefinitions;
		
		String projectOid = extractId(projectRef);
		if (projectOid != null) {
			response = getObject("project", projectOid);
			if (response == null || !isValidResponse(response)) {
	            throw new IOException("Unable to find project " + projectRef + " looking for build definition " + name);
			}
		} else {
            throw new IOException("Unable to find project " + projectRef + " looking for build definition " + name);
		}

		SAXBuilder sb = new SAXBuilder();
		Document doc = null;
		Element project = null;
		try {
			doc = sb.build(new StringReader(response));
			Element queryResult = doc.getRootElement();
			
			if (queryResult.getChild("BuildDefinitions") != null) {
				Element buildDefsElement = queryResult.getChild("BuildDefinitions");
				buildDefinitions = buildDefsElement.getChildren("BuildDefinition");
			    for (int i = 0; i < buildDefinitions.size(); i++) {
			        project = (Element) buildDefinitions.get(i);
			        if (project.getAttributeValue("refObjectName").equals(name)) {
			        	return project.getAttributeValue("ref");
			        }
			    }
			}

		} catch (JDOMException e) {
            throw new IOException("Query for build definition " + name + 
            		" caused JDOM exception: " + e.getMessage());
		} catch (IOException e) {
            throw new IOException("Query for build definition " + name + 
            		" caused IOException: " + e.getMessage());
		}
		
		throw new IOException("Unable to find build definition " + name + "in project " + projectRef);
	}
	
	public String findChangeset(String revision, String workspaceRef) {
		logMessage("findChangeset looking for " + revision + " in " + workspaceRef);
		String response = queryForArtifact("changeset", "(Revision = \"" + revision + "\")",
				workspaceRef, "false");

		if (response == null || !isValidResponse(response)) {
			return null;
		} else {
			return extractRefFromResponse(response);
		}
	}

	public String extractRefFromResponse(String response) {
		SAXBuilder sb = new SAXBuilder();
		Document doc = null;
		try {
			doc = sb.build(new StringReader(response));
			Element queryResult = doc.getRootElement();
			if (queryResult.getChild("TotalResultCount") != null && 
					queryResult.getChild("TotalResultCount").getValue().equals("0")) {
				return null;
			}

			if (queryResult.getChild("Results") != null && 
					queryResult.getChild("Results").getChild("Object") != null) {
				Element object = queryResult.getChild("Results").getChild("Object");
				return object.getAttributeValue("ref");
			} else {
				return null;
			}

		} catch (JDOMException e) {
			logMessage("JDOM Exception extracting a ref:\n" + e.getMessage());
			logMessage("Response was: " + response);
			return null;
		} catch (IOException e) {
			logMessage("IOException extracting a ref:\n" + e.getMessage());
			logMessage("Response was: " + response);
			return null;
		}
	}
	
    public String extractId(String ref) {
    	String[] tokens;
    	if (ref == null) { return null; }
    	tokens = ref.split("/");
    	if (tokens.length > 0) {
        	try { //Make sure it's a number
        		Long.parseLong(tokens[tokens.length-1]);
        		return tokens[tokens.length-1];
        	} catch (NumberFormatException nfe) {
        		return null;
        	}
    	} else {
    		return null;
    	}
    }
	
	protected String extractFlagFromResponse(String response) {
		SAXBuilder sb = new SAXBuilder();
		Document doc = null;
		try {
			doc = sb.build(new StringReader(response));
			Element queryResult = doc.getRootElement();
			if (queryResult.getChild("TotalResultCount") != null && 
					queryResult.getChild("TotalResultCount").getValue().equals("0")) {
				return null;
			}

			if (queryResult.getChild("Results") != null && 
					queryResult.getChild("Results").getChild("Object") != null &&
					queryResult.getChild("Results").getChild("Object").getChild("BuildandChangesetEnabled") != null) {
				Element flag = queryResult.getChild("Results").getChild("Object").getChild("BuildandChangesetEnabled");
				return flag.getValue();
			} else {
				return null;
			}

		} catch (JDOMException e) {
			logMessage("JDOM Exception extracting build/changeset workspace flag:\n" + e.getMessage());
			logMessage("Response was: " + response);
			return null;
		} catch (IOException e) {
			logMessage("IOException extracting build/changeset workspace flag:\n" + e.getMessage());
			logMessage("Response was: " + response);
			return null;
		}
	}
	
	protected String queryForArtifact(String object, String queryStr, String workspaceRef, String fetch) {
		NameValuePair queryParam, fetchParam, wsParam;
		NameValuePair[] pairs;
		
		String reqUrl = getUrl() + "/" + object;
		GetMethod get = new GetMethod(reqUrl);
		queryParam = new NameValuePair("query", queryStr);
		wsParam = new NameValuePair("workspace", workspaceRef);
		fetchParam = new NameValuePair("fetch", fetch);
		pairs = new NameValuePair[] { queryParam, wsParam, fetchParam };
		get.setQueryString(pairs);
		try {
			String response = doGet(new HttpClient(), get);
			return response;
		} catch (IOException ex) {
			logMessage("IOException querying for artifact: " + object + 
					", with query " + queryStr + ", in workspace " + workspaceRef);
			logMessage("Exception message was" + ex.getMessage());
			return null;
		}
	}
	
	protected String getSubscriptionInfo() {
	    logMessage("getSubscriptionInfo: attempting to retrieve subscription object ...");
		String reqUrl = getUrl() + "/subscription";
		GetMethod get = new GetMethod(reqUrl);
		try {
			String response = doGet(new HttpClient(), get);
			logMessage("getSubscriptionInfo: response from doGet was " + response);
			return response;
		} catch (IOException ex) {
			logMessage("getSubscriptionInfo: IOException " + ex.getClass() + ". Message is " + ex.getMessage());
			return null;
		}
	}
	
	protected String getObject(String objectType, String objectId) {
		String reqUrl = getUrl() + "/" + objectType + "/" + objectId;
		GetMethod get = new GetMethod(reqUrl);
		try {
			String response = doGet(new HttpClient(), get);
			return response;
		} catch (IOException ex) {
			logMessage("IOException " + ex.getMessage() + " trying to GET "+ reqUrl);
			return null;
		}
	}
	
	protected String doCreate(String xml, HttpClient httpClient, PostMethod post)  throws IOException {
		try {
			// Prepare HTTP post
			// Request content will be retrieved directly
			// from the input stream
			RequestEntity entity = new StringRequestEntity(xml, "text/xml",
			"UTF-8");
			post.setRequestEntity(entity);

			logMessage("Issuing POST to " + post.getURI());

			// Execute request
			httpClient.getState()
			.setCredentials(new AuthScope(host, port, null),
					new UsernamePasswordCredentials(username, password));
			setRequestHeaderInfo(post);

			setProxyParameters(httpClient);

			int result = httpClient.executeMethod(post);
			logMessage("POST response code was: " + result);

			// check status
			if (result != HttpStatus.SC_OK) {
				throw new IOException("HTTP POST Failed" + post.getStatusLine());
			}

			String response = inputStreamToString(post.getResponseBodyAsStream());

			if (!isValidResponse(response)) {
				throw new IOException("Create failed with errors: " + getErrors(response));
			}

			return response;
		} finally {
			// Release current connection to the connection pool once you are done
			post.releaseConnection();
		}
	}
	
	protected String doGet(HttpClient httpClient, GetMethod getMethod)
			throws IOException {

		httpClient.getState().setCredentials(new AuthScope(host, port, null),
				new UsernamePasswordCredentials(username, password));

		setProxyParameters(httpClient);

		getMethod.setDoAuthentication(true);
		
		logMessage("Issuing GET to " + getMethod.getURI());
		logMessage("  with username " + username + " and password " + password);

		try {
			// execute the GET
			int result = httpClient.executeMethod(getMethod);
			logMessage("GET response code was: " + result);

			// check status
			if (result != HttpStatus.SC_OK) {
				throw new IOException("HTTP GET Failed" + getMethod.getStatusLine());
			}

			String response = inputStreamToString(getMethod
					.getResponseBodyAsStream());

			logMessage("GET Response was: " + response);
			return response;

		} finally {
			// release any connection resources used by the method
			getMethod.releaseConnection();
		}
	}
	
	private void setProxyParameters(HttpClient httpClient) {
		if (this.proxyServer != null && this.proxyPort >= 0) {
			httpClient.getHostConfiguration().setProxy(this.proxyServer,
					this.proxyPort);
			logMessage("Using proxy server and port: " + this.proxyServer + " " + this.proxyPort);

			if (this.proxyUsername != null && this.proxyPassword != null) {
				httpClient.getState().setProxyCredentials(
						new AuthScope(this.proxyServer, this.proxyPort, null),
						new UsernamePasswordCredentials(this.proxyUsername,
								this.proxyPassword));
				logMessage("Using proxy username and password: " + this.proxyUsername + " " + this.proxyPassword);
			}
		}
	}
    
    private void setRequestHeaderInfo(PostMethod post) {
        post.setRequestHeader("X-RallyIntegrationName",
            (getPluginName() != null) ? getPluginName() : BUILD_PLUGIN);
        post.setRequestHeader("X-RallyIntegrationVersion", getPluginVersion());
        post.setRequestHeader("X-RallyIntegrationVendor", "Rally Software");
        post.setRequestHeader("X-RallyIntegrationOS",
            System.getProperty("os.name", "unknown os name") + " " +
            System.getProperty("os.version", "unknown os version") + " " +
            System.getProperty("os.arch", "unknown os arch"));
        post.setRequestHeader("X-RallyIntegrationPlatform",
            System.getProperty("java.vendor", "unknown java vendor") + " " +
            System.getProperty("java.version", "unknown java version"));
        post.setRequestHeader("X-RallyIntegrationLibrary",
            "Rally Java REST API 1.0");
    }

    private String getErrors(String response) {
    	if (response == null) {
    		return "Null response from Rally server (check Rally credentials on Hudson configuration page)";
    	} else {
            return response.substring(response.indexOf("<Errors>"),
                    response.indexOf("</Errors>") + 9);
    	}
    }

    private boolean isValidResponse(String response) {
        if (contains(response, "<Errors>")) {
            return false;
        } else {
            return true;
        }
    }

    private boolean contains(String str, String searchStr) {
        if ((str == null) || (searchStr == null)) {
            return false;
        }

        return str.indexOf(searchStr) >= 0;
    }

    private String inputStreamToString(InputStream stream)
        throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        StringBuffer sb = new StringBuffer();
        String line = null;

        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();

        return sb.toString();
    }
    
    private void logMessage(String msg) {
    	if (getLogger() != null) {
    		getLogger().println(msg);
    	}
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public void setPluginVersion(String pluginVersion) {
        this.pluginVersion = pluginVersion;
    }

	public void setProxyInfo(String server, int port, String username, String password) {
		this.proxyServer = server;
		this.proxyPort = port;
		this.proxyUsername = username;
		this.proxyPassword = password;
	}

	public void setLogger(PrintStream logger) {
		this.logger = logger;
	}

	public PrintStream getLogger() {
		return logger;
	}
}