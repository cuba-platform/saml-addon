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

import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.io.MarshallingException;
import org.springframework.security.saml.context.SAMLMessageContext;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.metadata.MetadataDisplayFilter;
import org.springframework.security.saml.util.SAMLUtil;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * This class using for process and show metadata information.
 *
 * @author adiatullin
 */
public class SamlConnectionsMetadataDisplayFilter extends MetadataDisplayFilter {

    @Inject
    protected SamlConnectionsKeyManager connectionsKeyManager;

    /**
     * The filter attempts to generate application metadata (if configured so) and in case the call is made
     * to the expected URL the metadata value is displayed and no further filters are invoked. Otherwise
     * filter chain invocation continues.
     *
     * @param request  request
     * @param response response
     * @throws javax.servlet.ServletException error
     * @throws java.io.IOException            io error
     */
    @Override
    protected void processMetadataDisplay(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            SAMLMessageContext context = contextProvider.getLocalEntity(request, response);
            response.setContentType("application/samlmetadata+xml"); // SAML_Meta, 4.1.1 - line 1235
            response.setCharacterEncoding("UTF-8");
            response.addHeader("Content-Disposition", "attachment; filename=\"spring_saml_metadata.xml\"");
            displayMetadata(context, response.getWriter());
        } catch (MetadataProviderException e) {
            throw new ServletException("Error initializing metadata", e);
        }
    }

    /**
     * Method writes metadata document into given writer object.
     *
     * @param context sp message context
     * @param writer  output for metadata
     * @throws ServletException error retrieving or writing the metadata
     */
    protected void displayMetadata(SAMLMessageContext context, PrintWriter writer) throws ServletException {
        try {
            String entityId = context.getLocalEntityId();
            EntityDescriptor descriptor = manager.getEntityDescriptor(entityId);
            if (descriptor == null) {
                throw new ServletException("Metadata entity with ID " + entityId + " wasn't found");
            } else {
                writer.print(getMetadataAsString(context, descriptor));
            }
        } catch (MarshallingException e) {
            log.error("Error marshalling entity descriptor", e);
            throw new ServletException(e);
        } catch (MetadataProviderException e) {
            log.error("Error retrieving metadata", e);
            throw new ServletException("Error retrieving metadata", e);
        }
    }

    protected String getMetadataAsString(SAMLMessageContext context, EntityDescriptor descriptor) throws MarshallingException {
        return SAMLUtil.getMetadataAsString(manager, getKeyManager(context), descriptor, null);
    }

    protected KeyManager getKeyManager(SAMLMessageContext context) {
        KeyManager km = connectionsKeyManager;
        if (context instanceof SamlConnectionMessageContext) {
            km = connectionsKeyManager.getKeyManager(((SamlConnectionMessageContext) context).getConnectionCode());
        }
        return km;
    }
}
