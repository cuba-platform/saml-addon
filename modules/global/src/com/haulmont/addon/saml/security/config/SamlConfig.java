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

package com.haulmont.addon.saml.security.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.Default;
import com.haulmont.cuba.core.config.defaults.DefaultBoolean;

/**
 * @author kuchmin
 */
@Source(type = SourceType.APP)
public interface SamlConfig extends Config {

    /**
     * @return SAML servlet context path, e.g. "/saml".
     */
    @Property("cuba.addon.saml.basePath")
    String getSamlBasePath();

    /**
     * @return SAML login path part, e.g. "/login" and with the base path the result will be "/saml/logout".
     */
    @Property("cuba.addon.saml.loginPath")
    String getSamlLoginPath();

    /**
     * @return SAML logout path part, e.g. "/logout" and with the base path the result will be "/saml/logout".
     */
    @Property("cuba.addon.saml.logoutPath")
    String getSamlLogoutPath();

    /**
     * @return SAML metadata display path part, e.g. "/metadata", and with the base path the result will be "/saml/metadata?tenant=tenant_code".
     */
    @Property("cuba.addon.saml.metadataPath")
    String getSamlMetadataPath();

    /**
     * @return defines whether the logout action will be also performed on the IDP when user performs logout in application.
     */
    @Property("cuba.addon.saml.ssoLogout")
    @DefaultBoolean(false)
    Boolean getSsoLogout();

    /**
     * @return defines is a application use a proxy server or not.
     */
    @Property("cuba.addon.saml.proxy.enabled")
    @DefaultBoolean(false)
    Boolean getProxyEnabled();

    /**
     * @return defines the address of remote proxy server if a proxy server is using, e.g. "https://myhost.com".
     */
    @Property("cuba.addon.saml.proxy.serverUrl")
    @Default("")
    String getProxyServerUrl();
}
