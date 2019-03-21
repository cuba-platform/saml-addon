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
import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.SecurityContext;
import com.haulmont.cuba.security.app.TrustedClientService;
import com.haulmont.cuba.security.global.LoginException;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.auth.WebAuthConfig;
import com.haulmont.cuba.web.security.HttpRequestFilter;
import com.vaadin.shared.ApplicationConstants;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.haulmont.addon.saml.web.security.saml.SamlSessionPrincipal.SAML_ERROR_ATTRIBUTE;
import static com.haulmont.addon.saml.web.security.saml.SamlSessionPrincipal.SAML_SESSION_ATTRIBUTE;

/**
 * @author kuchmin
 */
@Component("SamlLoginHttpRequestFilter")
public class SamlLoginHttpRequestFilter implements HttpRequestFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(SamlLoginHttpRequestFilter.class);

    protected static final String ORIGINAL_URL_ATTRIBUTE = "saml.original_url";

    @Inject
    protected SamlCommunicationService samlCommunicationService;
    @Inject
    protected TrustedClientService trustedClientService;

    @Inject
    protected GlobalConfig globalConfig;
    @Inject
    protected SamlConfig samlConfig;
    @Inject
    protected WebAuthConfig webAuthConfig;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // send static files without authentication
        if (StringUtils.startsWith(httpRequest.getRequestURI(), httpRequest.getContextPath() + "/VAADIN/")) {
            chain.doFilter(request, response);
            return;
        }
        //ignore filtering for saml servlet
        if (StringUtils.startsWith(httpRequest.getRequestURI(), httpRequest.getContextPath() + samlConfig.getSamlBasePath())) {
            chain.doFilter(request, response);
            return;
        }

        Object error = httpRequest.getSession().getAttribute(SAML_ERROR_ATTRIBUTE);
        if (error != null) {
            chain.doFilter(request, response);
            return;
        }

        SamlSession samlSession = (SamlSession) httpRequest.getSession()
                .getAttribute(SAML_SESSION_ATTRIBUTE);
        if (samlSession == null) {

            String code = getSamlConnectionCode(httpRequest);
            if (!StringUtils.isEmpty(code)) {

                httpRequest.getSession().setAttribute(ORIGINAL_URL_ATTRIBUTE, getUrl(httpRequest));

                log.debug("Redirecting to SAML connection '{}' login page", code);

                httpRequest.getSession().setAttribute(SamlSessionPrincipal.SAML_CONNECTION_CODE, code);

                String loginUrl = samlConfig.getProxyEnabled() ? samlConfig.getProxyServerUrl() : globalConfig.getWebAppUrl();
                loginUrl = loginUrl + samlConfig.getSamlBasePath() + samlConfig.getSamlLoginPath();
                httpResponse.sendRedirect(loginUrl);
                return;
            }

            chain.doFilter(request, response);
            return;
        }

        String redirectUrl = (String) httpRequest.getSession().getAttribute(ORIGINAL_URL_ATTRIBUTE);
        if (!StringUtils.isEmpty(redirectUrl)) {
            httpRequest.getSession().removeAttribute(ORIGINAL_URL_ATTRIBUTE);
            log.debug("Redirecting to '{}'", redirectUrl);
            httpResponse.sendRedirect(redirectUrl);
            return;
        }

        SamlServletRequestWrapper samlRequest = new SamlServletRequestWrapper(httpRequest,
                new SamlSessionPrincipalImpl(samlSession));

        chain.doFilter(samlRequest, response);
    }


    protected String getUrl(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (queryString == null) {
            return request.getRequestURL().toString();
        } else {
            return request.getRequestURL() + "?" + queryString;
        }
    }

    @Nullable
    protected String getSamlConnectionCode(HttpServletRequest request) {
        UserSession systemSession;
        try {
            systemSession = trustedClientService.getSystemSession(webAuthConfig.getTrustedClientPassword());
        } catch (LoginException e) {
            log.error("Unable to obtain system session to determinate SAML connection", e);
            return null;
        }
        return AppContext.withSecurityContext(new SecurityContext(systemSession), () -> {
            samlCommunicationService.initialize();

            //check from params
            String[] params = request.getParameterMap().get(SamlSessionPrincipal.SAML_CONNECTION_CODE);
            if (params != null && params.length > 0) {
                String possibleConnection = params[0];
                if (samlCommunicationService.isActiveConnection(possibleConnection)) {
                    return possibleConnection;
                }
            }
            //check from path
            String info = request.getPathInfo();
            if (StringUtils.isEmpty(info) || "/".equals(info)) {
                return null;
            }
            if (info.startsWith("/")) {
                info = info.substring(1, info.length());
            }
            int index = info.indexOf("/");
            if (index != -1) {
                info = info.substring(0, index);
            }
            //ignore default vaadin requests
            if (ApplicationConstants.UIDL_PATH.equals(info)
                    || ApplicationConstants.PUSH_PATH.equals(info)
                    || ApplicationConstants.HEARTBEAT_PATH.equals(info)) {
                return null;
            }
            String possibleConnectionCode = info;
            if (samlCommunicationService.isActiveConnection(possibleConnectionCode)) {
                return possibleConnectionCode;
            }

            return null;
        });
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        //do nothing
    }

    @Override
    public void destroy() {
        //do nothing
    }

    @Override
    public int getOrder() {
        return HIGHEST_PLATFORM_PRECEDENCE + 10;
    }
}
