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

package com.haulmont.addon.saml.saml.authentication;

import com.haulmont.addon.saml.security.SamlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.haulmont.addon.saml.web.security.saml.SamlSessionPrincipal.*;

/**
 * @author kuchmin
 */
@Component("saml_SamlSessionAttributeHandler")
public class SamlSessionAttributeHandle implements AuthenticationSuccessHandler {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        SamlSession samlSession = new SamlSession();
        samlSession.setPrincipal((String) authentication.getPrincipal());
        samlSession.setCredential((SAMLCredential) authentication.getCredentials());
        samlSession.setConnectionCode((String) request.getSession().getAttribute(SAML_CONNECTION_CODE));
        request.getSession().removeAttribute(SAML_CONNECTION_CODE);
        request.getSession().removeAttribute(SAML_ERROR_ATTRIBUTE);

        request.getSession().setAttribute(SAML_SESSION_ATTRIBUTE, samlSession);
    }
}
