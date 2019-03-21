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

package com.haulmont.addon.saml.service;

import com.haulmont.addon.saml.security.SamlSession;
import com.haulmont.cuba.security.entity.User;

import java.util.Map;

/**
 * SAML addon service
 *
 * @author adiatullin
 */
public interface SamlService {
    String NAME = "samladdon_SamlService";

    /**
     * @return System SAML processors names with classes
     */
    Map<String, String> getProcessingServices();

    /**
     * Retrieve user by provided SAML session. If user not exist, a new one will be created.
     *
     * @param session SAML sessions
     * @return SAML session user entity
     */
    User getUser(SamlSession session);
}
