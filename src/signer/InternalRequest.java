/*
 * Copyright 2014 Baidu, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package signer;

import com.google.common.collect.Maps;

import java.net.URI;
import java.util.Map;


public class InternalRequest {
	
	/**
	 * appKey
	 */
	private String appkey;
	/**
	 * appId
	 */
	private String appId;
    /**
     * 请求参数
     */
    private Map<String, Object> parameters = Maps.newHashMap();

    /**
     * 请求头
     */
    private Map<String, String> headers = Maps.newHashMap();

    /**
     * 请求链接
     */
    private URI uri;

    /**
     * 请求类型.
     */
    private String httpMethod;

    /**
     * 请求负载.
     */
    private String content;

    public String getAppkey() {
		return appkey;
	}

	public void setAppkey(String appkey) {
		this.appkey = appkey;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public InternalRequest(String httpMethod, URI uri) {
        this.httpMethod = httpMethod;
        this.uri = uri;
    }

    public void addHeader(String name, String value) {
        this.headers.put(name, value);
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public void addParameter(String name, String value) {
        this.parameters.put(name, value);
    }

    public Map<String, Object> getParameters() {
        return this.parameters;
    }

    public String getHttpMethod() {
        return this.httpMethod;
    }

    public URI getUri() {
        return this.uri;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers.clear();
        this.headers.putAll(headers);
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters.clear();
        this.parameters.putAll(parameters);
    }


    @Override
    public String toString() {
        return "InternalRequest [httpMethod=" + this.httpMethod + ", uri="  + this.uri + ", "
               + "expectContinueEnabled=" + "parameters=" + this.parameters + ", " + "headers=" + this.headers + "]";
    }
}
