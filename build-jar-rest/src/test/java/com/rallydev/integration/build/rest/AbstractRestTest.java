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

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.URL;


public class AbstractRestTest extends TestCase {
	public static final String BUILD_DEFINITION_NAME = "Test Build Definition";
	public static final String BUILD_DEFINITION_DESCRIPTION = "Sample build definition description.";
    public static final String BUILD_DEFINITION_SUCCESS_FILE = "build-definition-success.xml";
    public static final String BUILD_DEFINITION_SUCCESS_EVERYTHING_FILE = "build-definition-success-everything.xml";
    public static final String BUILD_DEFINITION_SUCCESS_WITH_BUILDS_FILE = "build-definition-success-with-builds.xml";
    public static final String SAMPLE_URL = "http://localhost:7001/slm/webservice/1.21";
    public static final String BUILD_SUCCESS_FILE = "build-success.xml";
    public static final String BUILD_SUCCESS_EVERYTHING_FILE = "build-success-everything.xml";
    public static final String BUILD_SUCCESS_SECURE_FILE = "build-success-secure.xml";
    public static final String BUILD_FAILURE_FILE = "build-failure.xml";
    public static final String BUILD_SUCCESS_MESSAGE = "Build succeeded!";
    public static final String BUILD_FAILURE_MESSAGE = "Build for job I broke the build failed.";
    public static final String BUILD_SUCCESS_RESPONSE_FILE = "build-success-response.xml";
    public static final String BUILD_SUCCESS_WITH_ERRORS_RESPONSE_FILE = "build-success-with-errors-response.xml";
    public static final String BUILD_SUCCESS_WITH_CHANGESETS_FILE = "build-success-changesets.xml";
    
    protected String readFile(String filename) throws Exception {
        StringBuffer output = new StringBuffer();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                    readFileAsStream(filename)));
        String str;

        while ((str = in.readLine()) != null) {
            output.append(str);

            if (in.ready()) {
                output.append("\n");
            }
        }

        in.close();

        return output.toString();
    }

    protected InputStream readFileAsStream(String filename)
        throws Exception {
        URL result = Thread.currentThread().getContextClassLoader()
                           .getResource(filename);

        if (result == null) {
            throw new Exception(filename + " Not Found.");
        }

        return result.openStream();
    }
}
