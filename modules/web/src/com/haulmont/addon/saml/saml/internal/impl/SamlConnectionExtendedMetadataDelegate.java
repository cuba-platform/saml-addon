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

import com.haulmont.addon.saml.saml.internal.SamlConnectionMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.ExtendedMetadataDelegate;

import java.util.Map;

/**
 * Metadata provider delegate with SAML connection information
 *
 * @author adiatullin
 */
public class SamlConnectionExtendedMetadataDelegate extends ExtendedMetadataDelegate implements SamlConnectionMetadataProvider {

    protected final String connectionCode;
    protected final boolean sp;

    public SamlConnectionExtendedMetadataDelegate(MetadataProvider delegate, String connectionCode, boolean sp, boolean metadataTrustCheck) {
        super(delegate);
        this.connectionCode = connectionCode;
        this.sp = sp;

        setMetadataTrustCheck(metadataTrustCheck);
    }

    public SamlConnectionExtendedMetadataDelegate(MetadataProvider delegate, ExtendedMetadata defaultMetadata,
                                                  String connectionCode, boolean sp, boolean metadataTrustCheck) {
        super(delegate, defaultMetadata);
        this.connectionCode = connectionCode;
        this.sp = sp;

        setMetadataTrustCheck(metadataTrustCheck);
    }

    public SamlConnectionExtendedMetadataDelegate(MetadataProvider delegate, Map<String, ExtendedMetadata> extendedMetadataMap,
                                                  String connectionCode, boolean sp,  boolean metadataTrustCheck) {
        super(delegate, extendedMetadataMap);
        this.connectionCode = connectionCode;
        this.sp = sp;

        setMetadataTrustCheck(metadataTrustCheck);
    }

    public SamlConnectionExtendedMetadataDelegate(MetadataProvider delegate, ExtendedMetadata defaultMetadata,
                                                  Map<String, ExtendedMetadata> extendedMetadataMap, String connectionCode, boolean sp,
                                                  boolean metadataTrustCheck) {
        super(delegate, defaultMetadata, extendedMetadataMap);
        this.connectionCode = connectionCode;
        this.sp = sp;

        setMetadataTrustCheck(metadataTrustCheck);
    }

    @Override
    public String getConnectionCode() {
        return connectionCode;
    }

    @Override
    public boolean isSp() {
        return sp;
    }
}
