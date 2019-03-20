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

package com.haulmont.addon.saml.saml.internal;

import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;

/**
 * SAML connection metadata provider
 *
 * @author adiatullin
 */
public interface SamlConnectionMetadataProvider extends MetadataProvider {
    /**
     * @return SAMP connection code
     */
    String getConnectionCode();

    /**
     * @return check what current metadata provider is SP or IdP
     */
    boolean isSp();

    void initialize() throws MetadataProviderException;

    void destroy() throws MetadataProviderException;
}
