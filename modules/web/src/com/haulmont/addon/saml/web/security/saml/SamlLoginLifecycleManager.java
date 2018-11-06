/*
 * Copyright (c) 2008-2018 Haulmont.
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
 */

package com.haulmont.addon.saml.web.security.saml;

import com.haulmont.addon.saml.security.SamlSession;
import com.haulmont.addon.saml.security.config.SamlConfig;
import com.haulmont.cuba.core.global.Events;
import com.haulmont.cuba.web.security.events.AppLoggedOutEvent;
import com.haulmont.cuba.web.sys.RequestContext;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

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

    @EventListener
    @Order(Events.HIGHEST_PLATFORM_PRECEDENCE + 10)
    public void onAppLoggedOut(AppLoggedOutEvent event) {
        if (event.getLoggedOutSession() != null) {
            String connectionCode = null;

            RequestContext requestContext = RequestContext.get();
            if (requestContext != null) {
                SamlSession samlSession = (SamlSession) requestContext.getSession().getAttribute(SAML_SESSION_ATTRIBUTE);
                if (samlSession != null) {
                    connectionCode = samlSession.getConnectionCode();
                    requestContext.getSession().removeAttribute(SAML_SESSION_ATTRIBUTE);
                }
            }

            if (event.getRedirectUrl() == null && Boolean.TRUE.equals(samlConfig.getSsoLogout())) {
                if (StringUtils.isEmpty(connectionCode)) {
                    log.info("SAML connection not found. Global SAML logout will be ignored.");
                } else {
                    if (!samlService.isActiveConnection(connectionCode)) {
                        log.info("SAML connection was deactivated or removed. Global SAML logout will be ignored.");
                    } else {
                        String samlLogoutUrl = samlConfig.getSamlLogoutUrl() + "?" + SAML_CONNECTION_CODE + "=" + connectionCode;
                        event.setRedirectUrl(samlLogoutUrl);
                    }
                }
            }
        }
    }
}