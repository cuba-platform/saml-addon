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

package com.haulmont.addon.saml.saml.internal.impl;

import com.haulmont.addon.saml.crypto.EncryptionService;
import com.haulmont.addon.saml.entity.SamlConnection;
import com.haulmont.addon.saml.saml.internal.SamlConnectionsKeyManager;
import com.haulmont.addon.saml.web.security.saml.SamlCommunicationServiceBean;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.cuba.core.app.FileStorageService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.SecurityContext;
import com.haulmont.cuba.security.app.TrustedClientService;
import com.haulmont.cuba.security.global.LoginException;
import com.haulmont.cuba.security.global.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.saml.key.EmptyKeyManager;
import org.springframework.security.saml.key.JKSKeyManager;
import org.springframework.security.saml.key.KeyManager;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author adiatullin
 */
public class SamlConnectionsKeyManagerImpl extends EmptyKeyManager implements KeyManager, SamlConnectionsKeyManager {
    private static final Logger log = LoggerFactory.getLogger(SamlConnectionsKeyManagerImpl.class);

    @Inject
    protected FileStorageService storageService;
    @Inject
    protected TrustedClientService trustedClientService;
    @Inject
    protected SamlCommunicationServiceBean samlCommunicationService;
    @Inject
    protected EncryptionService encryptionService;

    protected ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    protected Map<String, KeyManager> cache = new HashMap<>();

    @PostConstruct
    public void init() {
        samlCommunicationService.setKeyManager(this);
    }

    @Override
    public void add(SamlConnection connection) {
        Preconditions.checkNotNullArgument(connection, "SAML connection is empty");

        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            cache.put(connection.getSsoPath(), create(connection));
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to create key manager for SAML connection '%s'", connection.getSsoPath()));
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void remove(SamlConnection connection) {
        Preconditions.checkNotNullArgument(connection, "SAML connection is empty");

        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            cache.remove(connection.getSsoPath());
        } finally {
            writeLock.unlock();
        }
    }

    protected KeyManager create(SamlConnection connection) {
        FileDescriptor fd = connection.getKeystore().getKey();
        if (fd == null) {
            return new EmptyKeyManager();
        }

        Resource keystoreResource = new ByteArrayResource(loadFile(fd));
        Map<String, String> passwords = new HashMap<>();
        passwords.put(connection.getKeystore().getLogin(), encryptionService.getPlainPassword(connection.getKeystore()));
        String defaultKey = connection.getKeystore().getLogin();

        return new JKSKeyManager(keystoreResource, null, passwords, defaultKey);
    }

    protected byte[] loadFile(FileDescriptor fd) {
        UserSession systemSession;
        try {
            systemSession = trustedClientService.getSystemSession(samlCommunicationService.getTrustedClientPassword());
        } catch (LoginException e) {
            log.error("Unable to obtain system session", e);
            throw new RuntimeException("Failed to load file. Unable to obtain system session.");
        }
        return AppContext.withSecurityContext(new SecurityContext(systemSession), () -> {
            try {
                return storageService.loadFile(fd);
            } catch (FileStorageException e) {
                throw new RuntimeException(String.format("Failed to load file by file descriptor %s", fd.getId()));
            }
        });
    }

    @Override
    public KeyManager getKeyManager(String connectionCode) {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        readLock.lock();
        try {
            return cache.get(connectionCode);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public KeyManager generateKeyManager(SamlConnection connection) {
        Preconditions.checkNotNullArgument(connection, "SAML connection is empty");
        return create(connection);
    }
}
