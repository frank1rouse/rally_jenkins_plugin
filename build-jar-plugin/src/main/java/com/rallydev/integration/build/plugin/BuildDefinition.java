package com.rallydev.integration.build.plugin;

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

import java.io.IOException;
import java.util.List;

import com.rallydev.integration.build.rest.RallyRestService;

public class BuildDefinition {
    private RallyRestService service;
    private String description;
    private String name;
    private String projectRef;
	private List<String> buildRefs;
	private String uri;

    public BuildDefinition(RallyRestService service) {
        this.service = service;
    }

    public String create() throws IOException {
        // call post
        return getService().create("builddefinition", toXml());
    }

    protected String toXml() throws IOException {
        // create Build XML
        StringBuffer xml = new StringBuffer();
        xml.append("<BuildDefinition>");
        xml.append("<Project ref=\"");
        xml.append(getProjectRef());
        xml.append("\" type=\"Project\"/>");
        xml.append("<Name>");
        xml.append(getName());
        xml.append("</Name>");
        xml.append("<Description>");
        xml.append(getDescription());
        xml.append("</Description>");
        
        if (getBuildRefs() != null) {
        	xml.append("<Builds>");
        	for(String ref:getBuildRefs()) {
        		xml.append("<Build>" + ref + "</Build>");
        	}
        	xml.append("</Builds>");
        }
        
        if (getUri() != null) {
        	xml.append("<Uri>");
        	xml.append(getUri());
        	xml.append("</Uri>");
        }

        xml.append("</BuildDefinition>");

        return xml.toString();
    }

    public RallyRestService getService() {
        return service;
    }

    public void setService(RallyRestService service) {
        this.service = service;
    }

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setProjectRef(String projectRef) {
		this.projectRef = projectRef;
	}

	public String getProjectRef() {
		return projectRef;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getUri() {
		return uri;
	}

	public void setBuildRefs(List<String> buildRefs) {
		this.buildRefs = buildRefs;
	}

	public List<String> getBuildRefs() {
		return buildRefs;
	}

}

