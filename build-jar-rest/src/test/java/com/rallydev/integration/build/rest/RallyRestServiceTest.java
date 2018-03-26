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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

public class RallyRestServiceTest extends AbstractRestTest {
	  private RallyRestService rallyRestService;
	
	  private MockControl httpClientMockControl;
	
	  private MockControl httpStateMockControl;
	
	  private MockControl hostConfigurationMockControl;
	
	  private MockControl postMethodMockControl;
	
	  private MockControl statusLineMockControl;
	
	  private HttpClient httpClientMock;
	
	  private HttpState httpStateMock;
	
	  private HostConfiguration hostConfigurationMock;
	
	  private PostMethod postMethodMock;
	
	  private StatusLine statusLineMock;
	
	  private static String wsRef, projRef, buildDefRef, response, revision, projectName, wsName, hostname;

	  /*
	   * ********
		 Note - if you want to run these tests, you will have to setup your
		 own my.properties file.
		 ********
	  */
	  protected void setUp() throws Exception {
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
			hostname = properties.getProperty("rally.server");
			
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

			rallyRestService = new RallyRestService(username,password, hostname, "Build Plugin", "1.21");  
	
		    httpClientMockControl = MockClassControl.createControl(HttpClient.class);
		    httpClientMock = (HttpClient) httpClientMockControl.getMock();
		    postMethodMockControl = MockClassControl.createNiceControl(PostMethod.class);
		    postMethodMock = (PostMethod) postMethodMockControl.getMock();
		    httpStateMockControl = MockClassControl.createNiceControl(HttpState.class);
		    httpStateMock = (HttpState) httpStateMockControl.getMock();
			hostConfigurationMockControl = MockClassControl.createControl(HostConfiguration.class);
			hostConfigurationMock = (HostConfiguration) hostConfigurationMockControl.getMock();
		    statusLineMockControl = MockClassControl.createControl(StatusLine.class);
		    statusLineMock = (StatusLine) statusLineMockControl.getMock();    
		    httpClientMockControl.expectAndReturn(httpClientMock.getState(), httpStateMock);
		    postMethodMock.setRequestHeader("foo","foo");
		    postMethodMockControl.setMatcher(MockControl.ALWAYS_MATCHER);
		    postMethodMockControl.setVoidCallable(6);
		    httpStateMock.setCredentials(new AuthScope("host", -1, null), new UsernamePasswordCredentials("user", "password"));
		    postMethodMock.setRequestEntity(new StringRequestEntity(""));
		    postMethodMockControl.setMatcher(MockControl.ALWAYS_MATCHER);
		    httpStateMockControl.replay();
	  }

  protected void tearDown() throws Exception {
  }

  private void verify() {
    httpClientMockControl.verify();
    httpStateMockControl.verify();
    postMethodMockControl.verify();
  }

  public void testRallyRestServiceBuildSuccess() throws Exception {
    String xml = readFile(BUILD_SUCCESS_FILE);
    httpClientMockControl.expectAndReturn(httpClientMock.executeMethod(postMethodMock), HttpStatus.SC_OK);
    postMethodMockControl.expectAndReturn(postMethodMock.getResponseBodyAsStream(),
        readFileAsStream(BUILD_SUCCESS_RESPONSE_FILE));
    postMethodMock.releaseConnection();
    httpClientMockControl.replay();
    postMethodMockControl.replay();

    String response = rallyRestService.doCreate(xml, httpClientMock, postMethodMock);
    assertEquals(readFile(BUILD_SUCCESS_RESPONSE_FILE), response);
//    verify();
  }

  public void testRallyRestServiceBuildSuccessWithProxy() throws Exception {
    String xml = readFile(BUILD_SUCCESS_FILE);
    httpClientMockControl.expectAndReturn(httpClientMock.getHostConfiguration(), hostConfigurationMock);
	hostConfigurationMock.setProxy("10.1.0.12", 3128);
	
    httpClientMockControl.expectAndReturn(httpClientMock.executeMethod(postMethodMock), HttpStatus.SC_OK);
    postMethodMockControl.expectAndReturn(postMethodMock.getResponseBodyAsStream(),
        readFileAsStream(BUILD_SUCCESS_RESPONSE_FILE));
    postMethodMock.releaseConnection();
    httpClientMockControl.replay();
    postMethodMockControl.replay();
	hostConfigurationMockControl.replay();

	rallyRestService.setProxyInfo("10.1.0.12", 3128, null, null);
    String response = rallyRestService.doCreate(xml, httpClientMock, postMethodMock);
    assertEquals(readFile(BUILD_SUCCESS_RESPONSE_FILE), response);
	hostConfigurationMockControl.verify();
//    verify();
  }
  
  public void testRallyRestServiceBuildSuccessWithProxyAndAuth() throws Exception {
    MockControl httpStateMockControl1;
    HttpState httpStateMock1;
    
    httpStateMockControl1 = MockClassControl.createControl(HttpState.class);
    httpStateMock1 = (HttpState) httpStateMockControl1.getMock();
    httpStateMock1.setCredentials(new AuthScope("host", -1, null), new UsernamePasswordCredentials("user", "password"));
    httpStateMock1.setProxyCredentials(new AuthScope("proxyserver.mycompany.com", 3128, null), 
        new UsernamePasswordCredentials("user@rallydev.com", "password"));
    httpStateMockControl1.replay();

    String xml = readFile(BUILD_SUCCESS_FILE);
    httpClientMockControl.expectAndReturn(httpClientMock.getHostConfiguration(), hostConfigurationMock);
	hostConfigurationMock.setProxy("proxyserver.mycompany.com", 3128);

    httpClientMockControl.expectAndReturn(httpClientMock.getState(), httpStateMock1);

    httpClientMockControl.expectAndReturn(httpClientMock.executeMethod(postMethodMock), HttpStatus.SC_OK);
    postMethodMockControl.expectAndReturn(postMethodMock.getResponseBodyAsStream(),
        readFileAsStream(BUILD_SUCCESS_RESPONSE_FILE));
    postMethodMock.releaseConnection();
    
    httpClientMockControl.replay();
    postMethodMockControl.replay();
	hostConfigurationMockControl.replay();

	rallyRestService.setProxyInfo("proxyserver.mycompany.com", 3128, "user@rallydev.com", "password");
    String response = rallyRestService.doCreate(xml, httpClientMock, postMethodMock);
    assertEquals(readFile(BUILD_SUCCESS_RESPONSE_FILE), response);
//    httpStateMockControl.verify();
//	hostConfigurationMockControl.verify();
//    verify();
  }

  public void testRallyRestServiceIOFailure() throws Exception {
    String xml = readFile(BUILD_SUCCESS_FILE);
    httpClientMockControl.expectAndReturn(httpClientMock.executeMethod(postMethodMock), HttpStatus.SC_FORBIDDEN);
    postMethodMockControl.expectAndReturn(postMethodMock.getStatusLine(), statusLineMock);
    postMethodMock.releaseConnection();
    httpClientMockControl.replay();
    postMethodMockControl.replay();

    try {
      rallyRestService.doCreate(xml, httpClientMock, postMethodMock);
      fail();
    } catch (IOException e) {
    }

//    verify();
  }

  public void testRallyRestServiceBuildSuccessWithErrors() throws Exception {
    String xml = readFile(BUILD_SUCCESS_FILE);
    httpClientMockControl.expectAndReturn(httpClientMock.executeMethod(postMethodMock), HttpStatus.SC_OK);
    postMethodMockControl.expectAndReturn(postMethodMock.getResponseBodyAsStream(),
        readFileAsStream(BUILD_SUCCESS_WITH_ERRORS_RESPONSE_FILE));
    postMethodMock.releaseConnection();
    httpClientMockControl.replay();
    postMethodMockControl.replay();

    try {
      rallyRestService.doCreate(xml, httpClientMock, postMethodMock);
      fail();
    } catch (IOException e) {
      assertEquals("Create failed with errors: <Errors>error</Errors>", e.getMessage());
    }

//    verify();
  }

  public void testGetNonSecureUrl() throws Exception {
    rallyRestService = new RallyRestService("user", "password", "localhost", RallyRestService.WSAPI_VERSION, 7001, false,
    		RallyRestService.BUILD_PLUGIN, "1.0");

    String url = rallyRestService.getUrl();
    assertEquals(SAMPLE_URL, url);
  }

  public void testGetSecureUrl() throws Exception {
    String url = rallyRestService.getUrl();
    assertEquals("https://" + hostname + "/slm/webservice/" + RallyRestService.WSAPI_VERSION, url);
  }

}
