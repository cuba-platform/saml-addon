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

package com.haulmont.addon.saml.entity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.entity.annotation.OnDeleteInverse;
import com.haulmont.cuba.core.global.DeletePolicy;
import com.haulmont.cuba.security.entity.Group;

/**
 * @author adiatullin
 */
@NamePattern("%s|name,code")
@Table(name = "SAMLADDON_SAML_CONNECTION")
@Entity(name = "samladdon$SamlConnection")
public class SamlConnection extends StandardEntity {
    private static final long serialVersionUID = -9042413170802634475L;

    @NotNull
    @Column(name = "NAME", nullable = false)
    private String name;

    @NotNull
    @Column(name = "CODE", nullable = false, length = 100)
    private String code;

    @NotNull
    @Column(name = "SP_ID", nullable = false)
    private String spId;

    @Column(name = "ACTIVE")
    private Boolean active = false;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "KEYSTORE_ID")
    private FileDescriptor keystore;

    @Column(name = "KEYSTORE_LOGIN")
    private String keystoreLogin;

    @Column(name = "KEYSTORE_PASSWORD")
    private String keystorePassword;

    @Column(name = "IDP_METADATA_URL")
    private String idpMetadataUrl;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDP_METADATA_ID")
    private FileDescriptor idpMetadata;

    @NotNull
    @OnDeleteInverse(DeletePolicy.DENY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DEFAULT_GROUP_ID", nullable = false)
    private Group defaultGroup;

    @NotNull
    @Column(name = "PROCESSING_SERVICE", nullable = false)
    private String processingService;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getSpId() {
        return spId;
    }

    public void setSpId(String spId) {
        this.spId = spId;
    }

    public FileDescriptor getKeystore() {
        return keystore;
    }

    public void setKeystore(FileDescriptor keystore) {
        this.keystore = keystore;
    }

    public String getKeystoreLogin() {
        return keystoreLogin;
    }

    public void setKeystoreLogin(String keystoreLogin) {
        this.keystoreLogin = keystoreLogin;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getIdpMetadataUrl() {
        return idpMetadataUrl;
    }

    public void setIdpMetadataUrl(String idpMetadataUrl) {
        this.idpMetadataUrl = idpMetadataUrl;
    }

    public FileDescriptor getIdpMetadata() {
        return idpMetadata;
    }

    public void setIdpMetadata(FileDescriptor idpMetadata) {
        this.idpMetadata = idpMetadata;
    }

    public Group getDefaultGroup() {
        return defaultGroup;
    }

    public void setDefaultGroup(Group defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    public String getProcessingService() {
        return processingService;
    }

    public void setProcessingService(String processingService) {
        this.processingService = processingService;
    }
}