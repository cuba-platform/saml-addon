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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.Assert;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * @author kuchmin
 */
public class CompositeAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected final List<AuthenticationSuccessHandler> delegateHandlers;

    public CompositeAuthenticationSuccessHandler(List<AuthenticationSuccessHandler> delegateHandlers) {
        Assert.notEmpty(delegateHandlers, "delegateHandlers cannot be null or empty");
        for (AuthenticationSuccessHandler handler : delegateHandlers) {
            if (handler == null) {
                throw new IllegalArgumentException(
                        "delegateHandlers cannot contain null entries");
            }
        }

        this.delegateHandlers = delegateHandlers;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        for (AuthenticationSuccessHandler delegate : this.delegateHandlers) {
            if (logger.isDebugEnabled()) {
                logger.debug("Delegating to " + delegate);
            }

            delegate.onAuthenticationSuccess(request, response, authentication);
        }

    }
}
