/*
 * Copyright (c) 2008-2018 Haulmont.
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

package com.haulmont.addon.saml.saml.internal;

import java.io.Serializable;

/**
 * Application proxy configuration
 *
 * @author adiatullin
 */
public final class SamlProxyServerConfiguration implements Serializable {
    private static final long serialVersionUID = -7816616336831639680L;

    private final String url;
    private final String scheme;
    private final String serverName;
    private final Integer serverPort;
    private final boolean includePortInUrl;
    private final String contextPath;

    public SamlProxyServerConfiguration(String url, String scheme, String serverName, Integer serverPort, boolean includePortInUrl, String contextPath) {
        this.url = url;
        this.scheme = scheme;
        this.serverName = serverName;
        this.serverPort = serverPort;
        this.includePortInUrl = includePortInUrl;
        this.contextPath = contextPath;
    }

    public String getUrl() {
        return url;
    }

    public String getScheme() {
        return scheme;
    }

    public String getServerName() {
        return serverName;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public boolean isIncludePortInUrl() {
        return includePortInUrl;
    }

    public String getContextPath() {
        return contextPath;
    }
}
