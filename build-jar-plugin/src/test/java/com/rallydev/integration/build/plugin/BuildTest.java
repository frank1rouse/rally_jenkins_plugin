package com.rallydev.integration.build.plugin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.rallydev.integration.build.rest.AbstractRestTest;
import com.rallydev.integration.build.rest.RallyRestService;

public class BuildTest extends AbstractRestTest {
	private static final String NUMBER = "1";
	private static final long BUILD_DEF_ID = 5169;
	private static final float DURATION = 0;
	private Build build;
	private List<String> changesets = new ArrayList<String>();
	private Date startTime;
	
    private static final SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

	@Before
	protected void setUp() throws Exception {
		RallyRestService service = new RallyRestService("user@acme.com",
				"pass", "localhost:7001", "1.21", -1, false,
				"Hudson Plugin", "1.3");
		build = new Build(service);
		changesets.add("/changeset/1839135128");
		changesets.add("/changeset/1839135129");
	    startTime = isoFormat.parse("2010-10-25T15:14:13Z");
	}

	@Test
	public void testToXmlSuccess() throws Exception {
		build.setBuildDefinitionId(BUILD_DEF_ID);
		build.setDuration(DURATION);
		build.setNumber(NUMBER);
		build.setStatus("SUCCESS");

		String xml = build.toXml();
		assertEquals(readFile(BUILD_SUCCESS_FILE), xml);
	}
	
	@Test
	public void testToXmlSuccessWithEverything() throws Exception {
		build.setBuildDefinitionId(BUILD_DEF_ID);
		build.setDuration(DURATION);
		build.setNumber(NUMBER);
		build.setStatus("SUCCESS");
		build.setMessage(BUILD_SUCCESS_MESSAGE);
		build.setUri(SAMPLE_URL);
		build.setStartTime(startTime);
		
		String xml = build.toXml();
		assertEquals(readFile(BUILD_SUCCESS_EVERYTHING_FILE), xml);
	}

	@Test
	public void testToXmlSuccessWithChangesets() throws Exception {
		build.setBuildDefinitionId(BUILD_DEF_ID);
		build.setDuration(DURATION);
		build.setNumber(NUMBER);
		build.setStatus("SUCCESS");
		build.setChangeSets(changesets);
		build.setUri("http://localhost:9000/job/BuildConnectors/16/");
		String xml = build.toXml();
		assertEquals(readFile(BUILD_SUCCESS_WITH_CHANGESETS_FILE), xml);
	}

	@Test
	public void testToXmlFailure() throws Exception {
		build.setBuildDefinitionId(BUILD_DEF_ID);
		build.setDuration(DURATION);
		build.setNumber(NUMBER);
		build.setStatus("FAILURE");
		build.setMessage(BUILD_FAILURE_MESSAGE);

		String xml = build.toXml();
		assertEquals(readFile(BUILD_FAILURE_FILE), xml);
	}

}
