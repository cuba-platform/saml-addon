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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.web.filter.DelegatingFilterProxy;

import javax.servlet.*;
import java.util.EnumSet;

public class SingleAppSamlServletInitializer implements ServletContextListener {

    protected final Logger log = LoggerFactory.getLogger(SingleAppSamlServletInitializer.class);

    public static final String NAME = "saml_samlInitializer";
    protected static final String SERVLET_NAME = "saml_dispatcher";
    protected static final String SERVLET_PATH = "/saml/*";
    protected static final String SERVLET_CONTEXT_PREFIX = "org.springframework.web.servlet.FrameworkServlet.CONTEXT.";

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        try {
            registerServlet(servletContext);
            registerSecurityFilter(servletContext);
        } catch (ServletException e) {
            log.error("Saml servlet isn't initialized", e);
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    protected void registerServlet(ServletContext servletContext) throws ServletException {
        log.info("Registering SAML Dispatcher servlet");
        try {
            //noinspection unchecked
            Class<SamlDispatcherServlet> clazz = (Class<SamlDispatcherServlet>) ReflectionHelper
                    .loadClass(SamlDispatcherServlet.class.getName());
            ServletRegistration.Dynamic webdavServletReg = servletContext.addServlet(SERVLET_NAME, clazz);
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
