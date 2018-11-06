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

package com.haulmont.addon.saml.saml.internal;

import com.haulmont.addon.saml.entity.SamlConnection;
import org.springframework.security.saml.key.KeyManager;

import javax.annotation.Nullable;

/**
 * Key manager which store and provide each SP entity keys.
 *
 * @author adiatullin
 */
public interface SamlConnectionsKeyManager extends KeyManager {
    /**
     * Create and store key manager.
     *
     * @param connection SAML connection
     */
    void add(SamlConnection connection);

    /**
     * Remove if exist key manager.
     *
     * @param connection SAML connection
     */
    void remove(SamlConnection connection);

    /**
     * Get stored key manager.
     *
     * @param connectionCode SAML connection code
     * @return stored key manager
     */
    @Nullable
    KeyManager getKeyManager(String connectionCode);

    /**
     * Generate new key manager entity from SAML connection.
     *
     * @param connection SAML connection
     * @return detached Key Manager for this SAML connection
     */
    KeyManager generateKeyManager(SamlConnection connection);
}
