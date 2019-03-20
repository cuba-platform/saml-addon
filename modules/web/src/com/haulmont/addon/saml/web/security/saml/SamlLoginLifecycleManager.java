/*
 * Copyright (c) 2008-2019 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.haulmont.addon.saml.web.security.saml;

import com.haulmont.addon.saml.security.SamlSession;
import com.haulmont.addon.saml.security.config.SamlConfig;
import com.haulmont.cuba.core.global.Events;
import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.web.security.events.AppLoggedOutEvent;
import com.haulmont.cuba.web.security.events.UserDisconnectedEvent;
import com.haulmont.cuba.web.sys.RequestContext;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;

import java.security.Principal;

import static com.haulmont.addon.saml.web.security.saml.SamlSessionPrincipal.*;

/**
 * @author kuchmin
 */
@Component("saml_SamlLoginLifecycleManager")
public class SamlLoginLifecycleManager {

    private static final Logger log = LoggerFactory.getLogger(SamlLoginLifecycleManager.class);

    @Inject
    protected SamlCommunicationService samlService;

    @Inject
    protected SamlConfig samlConfig;
    @Inject
    protected GlobalConfig globalConfig;

    @EventListener
    @Order(Events.HIGHEST_PLATFORM_PRECEDENCE + 10)
    public void onUserLogout(UserDisconnectedEvent event) {
        if (event.getLoggedOutSession() != null) {
            RequestContext requestContext = RequestContext.get();
            if (requestContext != null) {
                Principal principal = requestContext.getRequest() == null ? null : requestContext.getRequest().getUserPrincipal();
                if (principal instanceof SamlSessionPrincipalImpl) {
                    ((SamlSessionPrincipalImpl) principal).setActive(false);
                }
            }
        }
    }

    @EventListener
    @Order(Events.HIGHEST_PLATFORM_PRECEDENCE + 10)
    public void onAppLoggedOut(AppLoggedOutEvent event) {
        if (event.getLoggedOutSession() != null) {
            String connectionCode = null;

            RequestContext requestContext = RequestContext.get();
            if (requestContext != null) {
                HttpSession session = requestContext.getSession();
                SamlSession samlSession = session == null ? null : (SamlSession) session.getAttribute(SAML_SESSION_ATTRIBUTE);
                if (samlSession != null) {
                    connectionCode = samlSession.getConnectionCode();
                    session.removeAttribute(SAML_SESSION_ATTRIBUTE);
                }
            }

            if (event.getRedirectUrl() == null && Boolean.TRUE.equals(samlConfig.getSsoLogout())) {
                if (StringUtils.isEmpty(connectionCode)) {
                    log.info("SAML logout will be ignored for user '{}'.", event.getLoggedOutSession().getCurrentOrSubstitutedUser().getLogin());
                } else {
                    if (!samlService.isActiveConnection(connectionCode)) {
                        log.info("SAML connection was deactivated or removed. SAML logout will be ignored for user '{}'.",
                                event.getLoggedOutSession().getCurrentOrSubstitutedUser().getLogin());
                    } else {
                        event.setRedirectUrl(getLogoutUrl() + "?" + SAML_CONNECTION_CODE + "=" + connectionCode);
                    }
                }
            }
        }
    }

    protected String getLogoutUrl() {
        return (samlConfig.getProxyEnabled() ? samlConfig.getProxyServerUrl() : globalConfig.getWebAppUrl())
                + samlConfig.getSamlBasePath() + samlConfig.getSamlLogoutPath();
    }
}
