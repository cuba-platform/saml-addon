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

/**
 * SAML connections communication bridge-service.
 *
 * @author adiatullin
 */
public interface SamlCommunicationService {
    String NAME = "samladdon_SamlCommunicationService";

    /**
     * Direct initialization
     */
    void initialize();

    /**
     * @param connectionCode SAML connection code
     * @return is a provided code a reference to active SAML connection
     */
    boolean isActiveConnection(String connectionCode);

    /**
     * Activate or deactivate current SAML connection if possible.
     *
     * @param active     is SAML connection should be active or not.
     * @param connection SAML connection.
     */
    void setActive(boolean active, SamlConnection connection);

    /**
     * Refresh SAML connection.
     *
     * @param connection SAML connection.
     */
    void refresh(SamlConnection connection);

    /**
     * Get SP metadata by provided SAML connection.
     *
     * @param connection SAML connection.
     * @return SP metadata.
     */
    String getSpMetadata(SamlConnection connection);

    /**
     * Get IDP metadata by provided SAML connection.
     *
     * @param connection SAML connection.
     * @return IDP metadata.
     */
    String getIdpMetadata(SamlConnection connection);
}
