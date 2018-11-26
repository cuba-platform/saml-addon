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

package com.haulmont.addon.saml.service;

import com.haulmont.addon.saml.core.SamlProcessor;
import com.haulmont.addon.saml.entity.SamlConnection;
import com.haulmont.addon.saml.security.SamlSession;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.TypedQuery;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.security.entity.User;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.*;

/**
 * @author adiatullin
 */
@Service(SamlService.NAME)
public class SamlServiceBean implements SamlService {
    private static final Logger log = LoggerFactory.getLogger(SamlServiceBean.class);

    @Inject
    protected Persistence persistence;

    @PostConstruct
    public void init() {
        try {
            if (Configuration.getMarshallerFactory().getMarshallers().size() == 0) {
                DefaultBootstrap.bootstrap();
            }
        } catch (Exception e) {
            log.error("Failed to initialize SAML context", e);
        }
    }

    @Override
    public Map<String, String> getProcessingServices() {
        Map<String, SamlProcessor> processors = AppBeans.getAll(SamlProcessor.class);
        if (processors == null || processors.size() == 0) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new TreeMap<>();
        for (Map.Entry<String, SamlProcessor> entry : processors.entrySet()) {
            String name = entry.getValue().getName();
            String serviceName = entry.getKey();
            result.put(name, serviceName);
        }
        return result;
    }

    /**
     * Method uses EntityManager because it should be worked if
     * DataManager the secure flag is enabled
     */
    @Transactional
    @Override
    public User getUser(SamlSession session) {
        Preconditions.checkNotNullArgument(session, "SAML session is empty");

        Map<String, SamlProcessor> processors = AppBeans.getAll(SamlProcessor.class);
        if (processors == null || processors.size() == 0) {
            throw new DevelopmentException("SAML processing services not exist");
        }

        SamlConnection connection = getConnection(session);
        for (Map.Entry<String, SamlProcessor> entry : processors.entrySet()) {
            String serviceName = entry.getKey();
            if (Objects.equals(serviceName, connection.getProcessingService())) {
                SamlProcessor processor = entry.getValue();
                return processor.findOrRegisterUser(session, connection);
            }
        }

        throw new DevelopmentException(String.format("SAML connection processor '%s' not found", connection.getProcessingService()));
    }

    protected SamlConnection getConnection(SamlSession session) {
        EntityManager em = persistence.getEntityManager();

        TypedQuery<SamlConnection> query = em.createQuery("select e from samladdon$SamlConnection e where e.code = :code", SamlConnection.class);
        query.setParameter("code", session.getConnectionCode());
        query.setViewName("connection.userCreation");

        SamlConnection result = query.getFirstResult();
        if (result == null) {
            throw new RuntimeException(String.format("SAML connection '%s' not found", session.getConnectionCode()));
        }
        return result;
    }
}
