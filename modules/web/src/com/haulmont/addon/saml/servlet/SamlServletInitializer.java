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

package com.haulmont.addon.saml.servlet;

import com.haulmont.bali.util.ReflectionHelper;
import com.haulmont.cuba.core.sys.AbstractWebAppContextLoader;
import com.haulmont.cuba.core.sys.servlet.ServletRegistrationManager;
import com.haulmont.cuba.core.sys.servlet.events.ServletContextInitializedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.DelegatingFilterProxy;

import javax.inject.Inject;
import javax.servlet.*;
import java.util.EnumSet;

@Component(SamlServletInitializer.NAME)
public class SamlServletInitializer {

    public static final String NAME = "saml_samlInitializer";

    protected final Logger log = LoggerFactory.getLogger(SamlServletInitializer.class);

    protected static final String SERVLET_NAME = "saml_dispatcher";
    protected static final String SERVLET_PATH = "/saml/*";
    protected static final String SERVLET_CONTEXT_PREFIX = "org.springframework.web.servlet.FrameworkServlet.CONTEXT.";

    @Inject
    protected ServletRegistrationManager servletRegistrationManager;

    @EventListener
    protected void init(ServletContextInitializedEvent event) throws ServletException {
        ServletContext servletCtx = event.getSource();
        log.info("Registering SAML Dispatcher servlet");
        try {
            ApplicationContext appCtx = event.getApplicationContext();
            Servlet servlet = servletRegistrationManager.createServlet(appCtx, SamlDispatcherServlet.class.getName());
            servlet.init(new AbstractWebAppContextLoader.CubaServletConfig(SERVLET_NAME, servletCtx));
            servletCtx.addServlet(SERVLET_NAME, servlet).addMapping(SERVLET_PATH);
        } catch (ServletException e) {
            throw new RuntimeException("An error occurred while initializing " + SERVLET_NAME + " servlet", e);
        }
        log.info("SAML Dispatcher servlet is registered");

        registerSecurityFilter(servletCtx);
    }

    protected void initServlet(ServletContext ctx) throws ServletException {
        log.info("Registering SAML Dispatcher servlet");
        try {
            //noinspection unchecked
            Class<SamlDispatcherServlet> clazz = (Class<SamlDispatcherServlet>) ReflectionHelper
                    .loadClass(SamlDispatcherServlet.class.getName());
            ServletRegistration.Dynamic webdavServletReg = ctx.addServlet(SERVLET_NAME, clazz);
            webdavServletReg.setLoadOnStartup(8);
            webdavServletReg.addMapping(SERVLET_PATH);
        } catch (ClassNotFoundException e) {
            throw new ServletException("Saml servlet isn't initialized", e);
        }
        log.info("SAML Dispatcher servlet is registered");
    }

    protected void registerSecurityFilter(ServletContext ctx) throws ServletException {
        log.info("Registering security filter");

        try {
            //noinspection unchecked
            Class<DelegatingFilterProxy> filterClass = (Class<DelegatingFilterProxy>) ReflectionHelper
                    .loadClass(DelegatingFilterProxy.class.getName());

            FilterRegistration.Dynamic filterReg = ctx.addFilter("dispatchSpringSecurityFilterChain", filterClass);
            filterReg.addMappingForUrlPatterns(
                    EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR, DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ASYNC),
                    true,
                    SERVLET_PATH);
            filterReg.setInitParameter("contextAttribute", SERVLET_CONTEXT_PREFIX + SERVLET_NAME);
            filterReg.setInitParameter("targetBeanName", AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME);
            filterReg.setInitParameter("dispatchOptionsRequest", "true");

        } catch (ClassNotFoundException e) {
            throw new ServletException(e);
        }

        log.info("Security filter registered");
    }
}
