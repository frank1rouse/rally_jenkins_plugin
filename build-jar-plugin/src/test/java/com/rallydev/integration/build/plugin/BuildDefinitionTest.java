package com.rallydev.integration.build.plugin;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.rallydev.integration.build.rest.AbstractRestTest;
import com.rallydev.integration.build.rest.RallyRestService;

public class BuildDefinitionTest extends AbstractRestTest {

	private static final String PROJECT_REF = "http://localhost:7001/slm/webservice/1.21/project/7219";
	private BuildDefinition buildDefinition;

	@Before
	protected void setUp() throws Exception {
		RallyRestService service = new RallyRestService("user@acme.com",
				"pass", "localhost:7001", "1.21", -1, false,
				"Hudson Plugin", "1.3");
		buildDefinition = new BuildDefinition(service);
	}

	@Test
	public void testToXmlSuccess() throws Exception {
		buildDefinition.setProjectRef(PROJECT_REF);
		buildDefinition.setName(BUILD_DEFINITION_NAME);

		String xml = buildDefinition.toXml();
		assertEquals(readFile(BUILD_DEFINITION_SUCCESS_FILE), xml);
	}
	
	@Test
	public void testToXmlSuccessWithEverything() throws Exception {
		buildDefinition.setProjectRef(PROJECT_REF);
		buildDefinition.setName(BUILD_DEFINITION_NAME);
		buildDefinition.setDescription(BUILD_DEFINITION_DESCRIPTION);
		buildDefinition.setUri(SAMPLE_URL);
		
		String xml = buildDefinition.toXml();
		assertEquals(readFile(BUILD_DEFINITION_SUCCESS_EVERYTHING_FILE), xml);
	}

	@Test
	public void testToXmlSuccessWithBuilds() throws Exception {
		buildDefinition.setProjectRef(PROJECT_REF);
		buildDefinition.setName(BUILD_DEFINITION_NAME);
		List<String> buildRefs = new ArrayList<String> ();
		buildRefs.add("http://localhost:7001/slm/webservice/1.21/build/145");
		buildRefs.add("http://localhost:7001/slm/webservice/1.21/build/2432");
		buildDefinition.setBuildRefs(buildRefs);
		buildDefinition.setUri("http://localhost:9000/job/BuildConnectors/16/");
		
		String xml = buildDefinition.toXml();
		assertEquals(readFile(BUILD_DEFINITION_SUCCESS_WITH_BUILDS_FILE), xml);
	}
}