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

package com.haulmont.addon.saml.security;

import org.springframework.security.saml.SAMLCredential;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author kuchmin
 */
public class SamlSession implements Serializable {
    private static final long serialVersionUID = -5496173942765972842L;

    protected String principal;
    protected SAMLCredential credential;
    protected String connectionCode;

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public SAMLCredential getCredential() {
        return credential;
    }

    public void setCredential(SAMLCredential credential) {
        this.credential = credential;
    }

    public String getConnectionCode() {
        return connectionCode;
    }

    public void setConnectionCode(String connectionCode) {
        this.connectionCode = connectionCode;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SamlSession that = (SamlSession) o;
        return Objects.equals(principal, that.principal)
                && Objects.equals(credential, that.credential)
                && Objects.equals(connectionCode, that.connectionCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(principal, credential, connectionCode);
    }
}
