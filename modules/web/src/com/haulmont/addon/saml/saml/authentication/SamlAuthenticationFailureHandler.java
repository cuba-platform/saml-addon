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
package com.haulmont.addon.saml.saml.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationTargetUrlRequestHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static com.haulmont.addon.saml.web.security.saml.SamlSessionPrincipal.SAML_ERROR_ATTRIBUTE;

/**
 * @author adiatullin
 */
@Component("failureRedirectHandler")
public class SamlAuthenticationFailureHandler extends AbstractAuthenticationTargetUrlRequestHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(SamlAuthenticationFailureHandler.class);

    protected RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException e)
            throws IOException, ServletException {
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            httpSession.setAttribute(SAML_ERROR_ATTRIBUTE, e);
        }

        log.error("Failed to login by saml", e);

        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest == null) {
            handle(request, response, null);
            return;
        }
        String targetUrl = savedRequest.getRedirectUrl();
        logger.debug("Redirecting to DefaultSavedRequest Url: " + targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
