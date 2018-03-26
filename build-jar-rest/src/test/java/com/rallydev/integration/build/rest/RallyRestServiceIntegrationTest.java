package com.rallydev.integration.build.rest;

import java.io.IOException;
import java.lang.NullPointerException;
import java.io.StringReader;
import java.util.Properties;
import java.io.InputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import org.junit.*;
import static org.junit.Assert.*;

public class RallyRestServiceIntegrationTest {

	private static RallyRestService service;
	private static String wsRef, projRef, buildDefRef, response, revision, projectName, wsName;
	
	/* this setup function is class based, it only gets run once 
	 * note that the vars reference had to be declared as static.
	 ********
	 Note - if you want to run these tests, you will have to setup your
	 own my.properties file.
	 ********
	 */
	@BeforeClass
	public static void oneTimeSetup() {
		Properties properties = new Properties();
		String property_file_name = "my.properties";
		
		try {
			properties.load(new FileReader(new File(property_file_name)));
		} catch (FileNotFoundException e) {
			System.out.println("-- ERROR: Cannot find property file: " + property_file_name);
			System.out.println("   Property file belongs at the root of build-jar-rest");
		} catch (IOException ioe) {
			System.out.println("-- ERROR: Cannot read property file: " + property_file_name);
		}
		// check that properties are set
		if (! properties.containsKey("rally.server")) {
			System.out.println("-- ERROR: Property file must have entry for rally.server");
		}
		String server = properties.getProperty("rally.server");
		
		if (! properties.containsKey("rally.username")) {
			System.out.println("-- ERROR: Property file must have entry for rally.username");
		}
		String username = properties.getProperty("rally.username");

		if (! properties.containsKey("rally.password")) {
			System.out.println("-- ERROR: Property file must have entry for rally.password");
		}
		String password = properties.getProperty("rally.password");

		if (! properties.containsKey("rally.project")) {
			System.out.println("-- ERROR: Property file must have entry for rally.project");
		}
		projectName = properties.getProperty("rally.project");

		if (! properties.containsKey("rally.workspace")) {
			System.out.println("-- ERROR: Property file must have entry for rally.workspace");
		}
		wsName = properties.getProperty("rally.workspace");

        service = new RallyRestService(username,
                password, server, "1.21", -1, true,
                "Hudson Plugin", "1.3");  
        try {
		    wsRef = service.findWorkspace(wsName);
		    assertNotNull(wsRef);
        } catch (IOException e) {
        	System.out.println("Unable to find the workspace for the given server/credentials.");
        }
        
        try {
        	projRef  = service.findProject(wsRef, projectName);
        	assertNotNull(projRef);
        } catch (IOException e) {
        	System.out.println("Unable to find the project for the given server/credentials.");
        }
        
        try {
        	buildDefRef  = service.findBuildDefinition("Default Build Definition", projRef);
        	assertNotNull(buildDefRef);
        } catch (IOException e) {
        	System.out.println("Unable to find the default build definition");
        }
        assertNotNull(buildDefRef);
        
        /* 
         * The query for changeset and subsequent extraction of a revision is looking for presence 
         * of content format rather than specific revision string/number
         */
		response = service.queryForArtifact("Changeset", "", wsRef, "true");
		revision = extractRevisionFromResponse(response);
		assertNotNull(revision);		
	}

	
	@Test
	public void testFindWorkspace() throws Exception {
		assertNotNull(wsRef);
	}
	
	@Test(expected=IOException.class)
	public void testFindClosedUseCaseWorkspace() throws Exception {
		String wsRef = service.findWorkspace("Closed Workspace");
	}
	
	@Test(expected=IOException.class)
	public void testFindNonExistingWorkspace() throws Exception {
		service.findWorkspace("IIIIJJJJKKKK");
	}	
	
	@Test(expected=IOException.class)
	public void testFindBlankWorkspaceName() throws Exception {
		service.findWorkspace("");
	}
	
	@Test(expected=IOException.class)
	public void testFindNullWorkspaceName() throws Exception {
		service.findWorkspace(null);
	}
		
	@Test
	public void testFindProject() throws Exception {
		assertNotNull(wsRef);	
		assertNotNull(projRef);
	}
	
	@Test(expected=IOException.class)
	public void testFindNonExistingProject() throws Exception {
		service.findProject(wsRef, "Team O'Shoopers");
	}
	
	@Test(expected=IOException.class)
	public void testFindBlankProjectName() throws Exception {
		service.findProject(wsRef, "");
	}
	
	@Test(expected=IOException.class)
	public void testFindNullProjectName() throws Exception {
		service.findProject(wsRef, null);
	}
  
	@Test(expected=IOException.class)
	public void testFindProjectNameWithBadWorkspace() throws Exception {
    	String wsRef = "Foobar";
    	service.findProject(wsRef, projectName);
    }
	
	@Test(expected=IOException.class)
	public void testFindProjectNameWithBlankWorkspace() throws Exception {
    	service.findProject("", projectName);
    }
	
	@Test(expected=IOException.class)
	public void testFindProjectNameWithNullWorkspace() throws Exception {
    	service.findProject(null, projectName);
    }
    
	@Test
	public void testBuildDefinitionQuery() throws Exception {
		String buildRef = service.findBuildDefinition("Default Build Definition", projRef);
		assertNotNull(buildRef);
	}
	
	@Test (expected=IOException.class)
	public void testBlankBuildDefinitionQuery() throws Exception {
		service.findBuildDefinition("", projRef);
	}
	
	@Test (expected=IOException.class)
	public void testNonExistentBuildDefinitionQuery() throws Exception {
		service.findBuildDefinition("Frainek Bofda", projRef);
	}
	
	@Test (expected=IOException.class)
	public void testNullBuildDefinitionQuery() throws Exception {
		service.findBuildDefinition(null, projRef);
	}
	
	@Test (expected=IOException.class)
	public void testNullProjectDefinitionQuery() throws Exception {
		service.findBuildDefinition("Default Build Definition", null);
	}
	
	@Test
	public void testChangesetQuery() throws Exception {
		assertNotNull(revision);
    	String changeset = service.findChangeset(revision, wsRef);
		assertNotNull(changeset	);
	}
	
	@Test
	public void testNonExistingRevisionChangesetQuery() throws Exception {
    	String changeset = service.findChangeset("-1", wsRef);
		assertNull(changeset);
	}
	
	@Test
	public void testBlankRevisionChangesetQuery() throws Exception {
    	String changeset = service.findChangeset("", wsRef); 
		assertNull(changeset);
	}
	
	@Test
	public void testNullRevisionChangesetQuery() throws Exception {
    	String changeset = service.findChangeset(null, wsRef); 
		assertNull(changeset);
	}
	
	@Test
	public void testNullWorkspaceGoodRevisionChangesetQuery() throws Exception {
    	String changeset = service.findChangeset(revision, null); 
		assertNull(changeset);
	}
	
	
	protected static String extractRevisionFromResponse(String response) {
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
				return object.getChild("Revision").getValue();
			} else {
				return null;
			}

		} catch (JDOMException e) {
			System.out.println("JDOM Exception extracting a ref:\n" + e.getMessage());
			System.out.println("Response was: " + response);
			return null;
		} catch (IOException e) {
			System.out.println("IOException extracting a ref:\n" + e.getMessage());
			System.out.println("Response was: " + response);
			return null;
		}
	}
	
	@Test
	public void testWorkspaceFlagQuery() throws Exception {
    	String flag = service.findWorkspaceFlag(wsName);
		assertEquals("true", flag);
	}
	
	@Test (expected=IOException.class)
	public void testNonExistentWorkspaceFlagQuery() throws Exception {
    	service.findWorkspaceFlag("Barfoo");
	}
	
//	@Test
//	public void testAlternateWorkspaceFlagQuery() throws Exception {
//    	String flag = service.findWorkspaceFlag("Yeti Manual Test Workspace");
//		assertEquals("false", flag);
//	}
	
	@Test (expected=IOException.class)
	public void testClosedWorkspaceFlagQuery() throws Exception {
    	service.findWorkspaceFlag("Closed Workspace");
	}
	
	@Test
    public void testCreateBuildSuccess() throws Exception {
        String successXML = "<Build><BuildDefinition ref=\"" + buildDefRef + 
        "\" type=\"BuildDefinition\"/>" +
		"<Status>SUCCESS</Status><Duration>0.0</Duration>" + 
		"<Number><![CDATA[1]]></Number></Build>";
        response = service.create("build", successXML);
        assertTrue(response != null && response.indexOf("<Errors>") < 0);
    }
	
}
