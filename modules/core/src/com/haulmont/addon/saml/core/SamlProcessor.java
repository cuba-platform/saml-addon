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

package com.haulmont.addon.saml.core;

import com.haulmont.addon.saml.entity.SamlConnection;
import com.haulmont.addon.saml.security.SamlSession;
import com.haulmont.cuba.security.entity.User;

/**
 * Implementations of this interface using to process information from SAML sessions
 *
 * @author adiatullin
 */
public interface SamlProcessor {
    /**
     * @return unique user friendly caption. User can specify this processor for each SAML connection.
     */
    String getName();

    /**
     * Find existing user from provided SAML session details or create a new one if user not exist.
     *
     * @param samlSession SAML sessions
     * @param connection  current session SAML connection
     * @return SAML session user entity
     */
    User findOrRegisterUser(SamlSession samlSession, SamlConnection connection);
}
