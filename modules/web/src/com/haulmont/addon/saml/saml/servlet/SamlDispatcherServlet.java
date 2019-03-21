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

package com.haulmont.addon.saml.saml.servlet;

import com.haulmont.cuba.core.sys.AppContext;
import org.apache.commons.lang.text.StrTokenizer;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.File;

/**
 * @author kuchmin
 */
public class SamlDispatcherServlet extends DispatcherServlet {

    private static final long serialVersionUID = -6798133652564117679L;

    public static final String DISPATCHER_CONFIG_FILE = "com/haulmont/addon/saml/saml-dispatcher-spring.xml";

    private volatile boolean initialized = false;

    @Override
    public String getContextConfigLocation() {
        //noinspection ConstantConditions
        File baseDir = new File(AppContext.getProperty("cuba.confDir"));

        String[] tokenArray = new StrTokenizer(DISPATCHER_CONFIG_FILE).getTokenArray();
        StringBuilder locations = new StringBuilder();

        for (String token : tokenArray) {
            String location;
            if (ResourceUtils.isUrl(token)) {
                location = token;
            } else {
                if (token.startsWith("/")) {
                    token = token.substring(1);
                }
                File file = new File(baseDir, token);
                if (file.exists()) {
                    location = file.toURI().toString();
                } else {
                    location = "classpath:" + token;
                }
            }
            locations.append(location).append(" ");
        }
        return locations.toString();
    }

    @Override
    protected WebApplicationContext initWebApplicationContext() {
        WebApplicationContext wac = findWebApplicationContext();
        if (wac == null) {
            ApplicationContext parent = AppContext.getApplicationContext();
            wac = createWebApplicationContext(parent);
        }

        onRefresh(wac);

        String attrName = getServletContextAttributeName();
        getServletContext().setAttribute(attrName, wac);
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Published WebApplicationContext of servlet '" + getServletName()
                    + "' as ServletContext attribute with name [" + attrName + "]");
        }

        return wac;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        if (!initialized) {
            super.init(config);
            initialized = true;
        }
    }
}
