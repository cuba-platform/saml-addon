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

import com.haulmont.addon.saml.security.SamlSession;

import javax.annotation.Nullable;
import java.security.Principal;
import java.util.Locale;

/**
 * @author kuchmin
 */
public class SamlSessionPrincipalImpl implements Principal, SamlSessionPrincipal {

    protected SamlSession samlSession;

    public SamlSessionPrincipalImpl(SamlSession samlSession) {
        this.samlSession = samlSession;
    }

    @Override
    public String getName() {
        return samlSession.getPrincipal();
    }

    @Override
    public SamlSession getSamlSession() {
        return samlSession;
    }

    @Nullable
    public Locale getLocale() {
        String locale = samlSession
                .getCredential().getAttributeAsString("language");
        if (locale == null) {
            return null;
        }

        return Locale.forLanguageTag(locale);
    }
}