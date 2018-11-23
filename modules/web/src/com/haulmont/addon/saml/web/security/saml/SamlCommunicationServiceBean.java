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

package com.haulmont.addon.saml.web.security.saml;

import com.haulmont.addon.saml.entity.SamlConnection;
import com.haulmont.addon.saml.saml.internal.SamlConnectionsKeyManager;
import com.haulmont.addon.saml.saml.internal.SamlConnectionsMetadataManager;
import com.haulmont.addon.saml.saml.internal.SamlProxyServerConfiguration;
import com.haulmont.addon.saml.security.config.SamlConfig;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.SecurityContext;
import com.haulmont.cuba.security.app.TrustedClientService;
import com.haulmont.cuba.security.global.LoginException;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.auth.WebAuthConfig;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author adiatullin
 */
@Component(SamlCommunicationService.NAME)
public class SamlCommunicationServiceBean implements SamlCommunicationService {
    private static final Logger log = LoggerFactory.getLogger(SamlCommunicationServiceBean.class);

    protected SamlConnectionsMetadataManager metadataManager;

    protected SamlConnectionsKeyManager keyManager;

    @Inject
    protected DataManager dataManager;
    @Inject
    protected TrustedClientService trustedClientService;

    @Inject
    protected WebAuthConfig webAuthConfig;
    @Inject
    protected GlobalConfig globalConfig;
    @Inject
    protected SamlConfig samlConfig;

    protected ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    protected Map<UUID, String> cache = new HashMap<>();
    protected volatile boolean initialized = false;

    @Override
    public void initialize() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    UserSession systemSession;
                    try {
                        systemSession = trustedClientService.getSystemSession(getTrustedClientPassword());
                    } catch (LoginException e) {
                        throw new RuntimeException("Unable to obtain system session to refresh SAML connections", e);
                    }
                    AppContext.withSecurityContext(new SecurityContext(systemSession), this::init);

                    initialized = true;
                }
            }
        }
    }

    public String getTrustedClientPassword() {
        return webAuthConfig.getTrustedClientPassword();
    }

    public String getEntityBaseUrl() {
        return isProxyEnabled() ? getProxyServerUrl() : globalConfig.getWebAppUrl();
    }

    public boolean isProxyEnabled() {
        return samlConfig.getProxyEnabled();
    }

    protected String getProxyServerUrl() {
        StringBuilder sb = new StringBuilder();
        sb.append(samlConfig.getProxyScheme());
        sb.append("://");
        sb.append(samlConfig.getProxyServerName());
        if (samlConfig.getProxyIncludePort()) {
            sb.append(":").append(samlConfig.getProxyServerPort());
        }
        sb.append(samlConfig.getProxyContextPath());
        return sb.toString();
    }

    public SamlProxyServerConfiguration getProxyConfiguration() {
        return new SamlProxyServerConfiguration(
                getProxyServerUrl(),
                samlConfig.getProxyScheme(),
                samlConfig.getProxyServerName(),
                samlConfig.getProxyServerPort(),
                samlConfig.getProxyIncludePort(),
                samlConfig.getProxyContextPath()
        );
    }

    protected void init() {
        StopWatch sw = new Slf4JStopWatch(log);
        int success = 0;
        int failed = 0;
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            List<SamlConnection> connections = dataManager.loadList(LoadContext.create(SamlConnection.class)
                    .setQuery(new LoadContext.Query("select e from samladdon$SamlConnection e where e.active = true order by e.code"))
                    .setView("connection.activation"));

            if (!CollectionUtils.isEmpty(connections)) {
                Set<SamlConnection> update = null;

                for (SamlConnection connection : connections) {
                    try {
                        keyManager.add(connection);
                        metadataManager.add(connection);
                        success += 1;

                        cache.put(connection.getId(), connection.getCode());

                    } catch (Exception e) {
                        log.error(String.format("Failed to instantiate SAML connection '%s'", connection.getCode()), e);

                        failed += 1;
                        if (update == null) {
                            update = new HashSet<>();
                        }
                        update.add(connection);
                        connection.setActive(Boolean.FALSE);
                    }
                }
                try {
                    metadataManager.refresh();
                } catch (Exception e) {
                    log.error("Failed to refresh metadata manager", e);

                    for (SamlConnection connection : connections) {
                        connection.setActive(Boolean.FALSE);
                        if (update == null) {
                            update = new HashSet<>();
                        }
                        update.add(connection);
                    }
                    cache.clear();
                    success = 0;
                    failed = connections.size();
                }

                if (!CollectionUtils.isEmpty(update)) {
                    dataManager.commit(new CommitContext(update));
                }
            }
        } finally {
            writeLock.unlock();
            sw.stop(getClass().getSimpleName(), String.format("SAML connections initialized. Success: %d. Failed: %d.", success, failed));
        }
    }

    @Override
    public boolean isActiveConnection(String connectionCode) {
        if (!StringUtils.isEmpty(connectionCode)) {
            initialize();

            ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
            readLock.lock();
            try {
                for (String code : cache.values()) {
                    if (connectionCode.equals(code)) {
                        return true;
                    }
                }
            } finally {
                readLock.unlock();
            }
        }
        return false;
    }

    @Override
    public void setActive(boolean active, SamlConnection connection) {
        Preconditions.checkNotNullArgument(connection, "SAML connection is empty");
        initialize();

        connection = reload(connection, "connection.activation");

        if (Boolean.TRUE.equals(connection.getActive()) != active) {
            connection.setActive(active);
            refresh(connection);
            //every fine - changes can be saved
            dataManager.commit(connection);

            log.info("SAML connection '{}' successfully " + (connection.getActive() ? "activated" : "deactivated"), connection.getCode());
        }
    }

    @Override
    public void refresh(SamlConnection connection) {
        Preconditions.checkNotNullArgument(connection, "SAML connection is empty");
        initialize();

        connection = reload(connection, "connection.activation");

        try {
            if (Boolean.TRUE.equals(connection.getActive())) {
                keyManager.add(connection);
                metadataManager.add(connection);
            } else {
                keyManager.remove(connection);
                metadataManager.remove(connection);
            }
            metadataManager.refresh();

            ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
            writeLock.lock();
            try {
                if (Boolean.TRUE.equals(connection.getActive())) {
                    cache.put(connection.getId(), connection.getCode());
                } else {
                    cache.remove(connection.getId());
                }
            } finally {
                writeLock.unlock();
            }

        } catch (Exception e) {
            log.error("Failed to refresh SAML connection", e);

            if (Boolean.TRUE.equals(connection.getActive())) {
                connection.setActive(Boolean.FALSE);
                dataManager.commit(connection);

                ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
                writeLock.lock();
                try {
                    cache.remove(connection.getId());
                } finally {
                    writeLock.unlock();
                }
            }

            throw new RuntimeException(String.format("Failed to refresh SAML connection: %s", e.getMessage()));
        }
    }

    @Override
    public String getSpMetadata(SamlConnection connection) {
        Preconditions.checkNotNullArgument(connection, "SAML connection is empty");
        initialize();

        connection = reload(connection, "connection.activation");

        try {
            return metadataManager.generateSpMetadata(connection);
        } catch (Exception e) {
            log.error("Failed to get SP metadata", e);
            throw new RuntimeException(String.format("Error in SP metadata: %s", e.getMessage()));
        }
    }

    @Override
    public String getIdpMetadata(SamlConnection connection) {
        Preconditions.checkNotNullArgument(connection, "SAML connection is empty");
        initialize();

        connection = reload(connection, "connection.activation");

        try {
            return metadataManager.generateIdpMetadata(connection);
        } catch (Exception e) {
            log.error("Failed to get IDP metadata", e);
            throw new RuntimeException(String.format("Error in IDP metadata: %s", e.getMessage()));
        }
    }

    /**
     * Setup dispatcher MetadataManager
     *
     * @param metadataManager dispatcher MetadataManager
     */
    public void setMetadataManager(SamlConnectionsMetadataManager metadataManager) {
        this.metadataManager = metadataManager;
    }

    /**
     * Setup dispatcher Key Manager
     *
     * @param keyManager dispatcher KeyManager
     */
    public void setKeyManager(SamlConnectionsKeyManager keyManager) {
        this.keyManager = keyManager;
    }

    protected <E extends Entity> E reload(E entity, String view) {
        if (!PersistenceHelper.isLoadedWithView(entity, view)) {
            return dataManager.reload(entity, view);
        }
        return entity;
    }
}
