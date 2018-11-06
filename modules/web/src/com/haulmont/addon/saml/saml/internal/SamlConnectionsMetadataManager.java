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
import org.opensaml.saml2.metadata.provider.MetadataProviderException;

import javax.annotation.Nullable;

/**
 * SAML metadata manager which contains and operate with SAML metadata.
 *
 * @author adiatullin
 */
public interface SamlConnectionsMetadataManager {
    /**
     * Retrieve IDP entity Id by SAML connection code.
     *
     * @param connectionCode connection code
     * @return IDP entity Id or null
     * @throws MetadataProviderException in case of any problem
     */
    @Nullable
    String getIdpEntityId(String connectionCode) throws MetadataProviderException;

    /**
     * Retrieve SP entity Id by SAML connection code
     *
     * @param connectionCode connection code
     * @return SP entity Id or null
     * @throws MetadataProviderException in case of any problem
     */
    @Nullable
    String getSpEntityId(String connectionCode) throws MetadataProviderException;

    /**
     * Add and activate SAML connection. Note: before add connection to metadata set SAML connection to KeyManager
     *
     * @param connection SAML connection
     * @throws MetadataProviderException in case provider can't be added
     * @see SamlConnectionsKeyManager
     */
    void add(SamlConnection connection) throws MetadataProviderException;

    /**
     * Remove SAML connections from metadata. Note: do not forget remove SAML connection key from KeyManager
     *
     * @param connection SAML connection
     * @see SamlConnectionsKeyManager
     */
    void remove(SamlConnection connection);

    /**
     * Refresh all metadata providers
     */
    void refresh();

    /**
     * Generate and get text SP (which can be not active) metadata by SAML connection
     *
     * @param connection SAML connection
     * @return SP metadata text
     */
    String generateSpMetadata(SamlConnection connection) throws MetadataProviderException;

    /**
     * Generate and get text IdP (which can be not active) metadata by SAML connection
     *
     * @param connection SAML connection
     * @return IdP metadata text
     */
    String generateIdpMetadata(SamlConnection connection) throws MetadataProviderException;
}
