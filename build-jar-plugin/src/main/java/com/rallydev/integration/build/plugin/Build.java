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
package com.rallydev.integration.build.plugin;

import com.rallydev.integration.build.rest.RallyRestService;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class Build {
    private static final int MESSAGE_SIZE = 4000;
    private RallyRestService service;
    private String message;
    private float duration;
    private String number;
    private String status;
    private long buildDefinitionId;
    private List<String> changesets;
    private String uri;
    private Date startTime;
    private static final SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public Build(RallyRestService service) {
        this.service = service;
    }

    private String truncate(String string) {
        if (string.length() <= MESSAGE_SIZE) {
            return string;
        } else {
            return string.substring(string.length() - MESSAGE_SIZE,
                string.length());
        }
    }

    public String create() throws IOException {
        // call post
        return getService().create("build", toXml());
    }

    protected String toXml() throws IOException {
        // create Build XML
        StringBuffer xml = new StringBuffer();
        xml.append("<Build>");
        xml.append("<BuildDefinition ref=\"");
        xml.append(getService().getUrl());
        xml.append("/builddefinition/");
        xml.append(buildDefinitionId);
        xml.append("\" type=\"BuildDefinition\"/>");
        xml.append("<Status>");
        xml.append(status);
        xml.append("</Status>");
        xml.append("<Duration>");
        xml.append(duration);
        xml.append("</Duration>");
        
        if (startTime != null) {
            xml.append("<Start>");
            xml.append(isoFormat.format(startTime));
            xml.append("</Start>");
        }

        if (message != null) {
            xml.append("<Message>");
            xml.append("<![CDATA[");
            xml.append(truncate(message));
            xml.append("]]>");
            xml.append("</Message>");
        }

        if (number != null) {
            xml.append("<Number>");
            xml.append("<![CDATA[");
            xml.append(number);
            xml.append("]]>");
            xml.append("</Number>");
        }
        
        //System.out.println("Build has changesets " + changesets);
        if (changesets != null) {
        	xml.append("<Changesets>");
        	for(String ref:changesets) {
        		//System.out.println("   appending ref " + ref);
        		xml.append("<Ref>" + ref + "</Ref>");
        	}
        	xml.append("</Changesets>");
        }
        
        if (uri != null) {
        	xml.append("<Uri>");
        	xml.append(uri);
        	xml.append("</Uri>");
        }

        xml.append("</Build>");

        return xml.toString();
    }

    public RallyRestService getService() {
        return service;
    }

    public void setService(RallyRestService service) {
        this.service = service;
    }

    public long getBuildDefinitionId() {
        return buildDefinitionId;
    }

    public void setBuildDefinitionId(long buildDefinitionId) {
        this.buildDefinitionId = buildDefinitionId;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String label) {
        this.number = label;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
    public List<String> getChangesets() {
    	return changesets;
    }
    
    public void setChangeSets(List<String> changesets) {
    	this.changesets = changesets;
    }

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getUri() {
		return uri;
	}
	
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getStartTime() {
		return startTime;
	}
}
