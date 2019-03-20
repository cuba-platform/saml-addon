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

package com.haulmont.addon.saml.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Listeners;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Listeners("keystoreListener")
@NamePattern("%s : %s|login,description")
@Table(name = "SAMLADDON_KEY_STORE")
@Entity(name = "samladdon$KeyStore")
public class KeyStore extends StandardEntity {
    private static final long serialVersionUID = 420054615939249553L;

    @NotNull
    @Column(name = "LOGIN", nullable = false, length = 100)
    protected String login;

    @NotNull
    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "KEY_ID")
    protected FileDescriptor key;

    @NotNull
    @Column(name = "PASSWORD", nullable = false, length = 100)
    protected String password;

    @Column(name = "DESCRIPTION")
    protected String description;

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setKey(FileDescriptor key) {
        this.key = key;
    }

    public FileDescriptor getKey() {
        return key;
    }


    public void setLogin(String login) {
        this.login = login;
    }

    public String getLogin() {
        return login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

}
