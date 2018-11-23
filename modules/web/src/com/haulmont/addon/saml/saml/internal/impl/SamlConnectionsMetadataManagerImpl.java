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

import com.haulmont.addon.saml.entity.SamlConnection;
import com.haulmont.addon.saml.saml.internal.SamlConnectionMetadataProvider;
import com.haulmont.addon.saml.saml.internal.SamlConnectionsKeyManager;
import com.haulmont.addon.saml.saml.internal.SamlConnectionsMetadataManager;
import com.haulmont.addon.saml.web.security.saml.SamlCommunicationServiceBean;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.cuba.core.app.FileStorageService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.core.global.FileStorageException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.lang.StringUtils;
import org.eclipse.persistence.internal.oxm.ByteArraySource;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml2.metadata.provider.*;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.parse.StaticBasicParserPool;
import org.opensaml.xml.security.CriteriaSet;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.x509.BasicPKIXValidationInformation;
import org.opensaml.xml.security.x509.PKIXValidationInformation;
import org.opensaml.xml.security.x509.PKIXValidationInformationResolver;
import org.opensaml.xml.security.x509.StaticPKIXValidationInformationResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.saml.*;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.metadata.*;
import org.springframework.security.saml.util.SAMLUtil;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author adiatullin
 */
public class SamlConnectionsMetadataManagerImpl extends CachingMetadataManager implements SamlConnectionsMetadataManager {
    private static final Logger log = LoggerFactory.getLogger(SamlConnectionsMetadataManagerImpl.class);

    @Inject
    protected SamlConnectionsKeyManager connectionsKeyManager;
    @Inject
    protected SamlCommunicationServiceBean samlCommunicationService;
    @Inject
    protected StaticBasicParserPool parserPool;

    @Autowired(required = false)
    @Qualifier("samlWebSSOProcessingFilter")
    protected SAMLProcessingFilter samlWebSSOFilter;
    @Autowired(required = false)
    @Qualifier("samlWebSSOHoKProcessingFilter")
    protected SAMLWebSSOHoKProcessingFilter samlWebSSOHoKFilter;
    @Autowired(required = false)
    protected SAMLLogoutProcessingFilter samlLogoutProcessingFilter;
    @Autowired(required = false)
    protected SAMLEntryPoint samlEntryPoint;


    public SamlConnectionsMetadataManagerImpl() throws MetadataProviderException {
        super(Collections.emptyList());
    }

    @PostConstruct
    public void init() {
        samlCommunicationService.setMetadataManager(this);
    }

    @Override
    public String getIdpEntityId(String connectionCode) throws MetadataProviderException {
        if (!StringUtils.isEmpty(connectionCode)) {
            SamlConnectionMetadataProvider provider = getProvider(connectionCode, false);
            if (provider != null) {
                return getIdpEntityId(provider);
            }
        }
        return null;
    }

    @Nullable
    protected String getIdpEntityId(SamlConnectionMetadataProvider provider) throws MetadataProviderException {
        List<String> values = parseProvider(provider);
        for (String value : values) {
            RoleDescriptor idpRoleDescriptor = provider.getRole(value, IDPSSODescriptor.DEFAULT_ELEMENT_NAME, SAMLConstants.SAML20P_NS);
            if (idpRoleDescriptor != null) {
                return value;
            }
        }
        return null;
    }

    @Override
    public String getSpEntityId(String connectionCode) throws MetadataProviderException {
        if (!StringUtils.isEmpty(connectionCode)) {
            SamlConnectionMetadataProvider provider = getProvider(connectionCode, true);
            if (provider != null) {
                return getSpEntityId(provider);
            }
        }
        return null;
    }

    @Nullable
    protected String getSpEntityId(SamlConnectionMetadataProvider provider) throws MetadataProviderException {
        List<String> values = parseProvider(provider);
        for (String value : values) {
            RoleDescriptor spRoleDescriptor = provider.getRole(value, SPSSODescriptor.DEFAULT_ELEMENT_NAME, SAMLConstants.SAML20P_NS);
            if (spRoleDescriptor != null) {
                return value;
            }
        }
        return null;
    }

    @Override
    public void addMetadataProvider(MetadataProvider provider) {
        throw new UnsupportedOperationException("Please provide SAML connection and use 'add' method");
    }

    @Override
    public void removeMetadataProvider(MetadataProvider provider) {
        throw new UnsupportedOperationException("Please provide SAML connection and use 'remove' method");
    }

    @Override
    public void add(SamlConnection connection) throws MetadataProviderException {
        Preconditions.checkNotNullArgument(connection, "SAML connection is empty");
        remove(connection);

        SamlConnectionMetadataProvider spProvider = generateSpProvider(connection, connectionsKeyManager.getKeyManager(connection.getCode()));
        super.addMetadataProvider(spProvider);
        SamlConnectionMetadataProvider idpProvider = generateIdpProvider(connection);
        super.addMetadataProvider(idpProvider);
    }

    protected SamlConnectionMetadataProvider generateSpProvider(SamlConnection connection, KeyManager keyManager) throws MetadataProviderException {
        MetadataGenerator generator = new MetadataGenerator();
        generator.setEntityId(connection.getSpId());
        generator.setEntityBaseURL(samlCommunicationService.getEntityBaseUrl());
        generator.setSamlWebSSOFilter(samlWebSSOFilter);
        generator.setSamlWebSSOHoKFilter(samlWebSSOHoKFilter);
        generator.setSamlLogoutProcessingFilter(samlLogoutProcessingFilter);
        generator.setSamlEntryPoint(samlEntryPoint);
        generator.setKeyManager(keyManager);

        EntityDescriptor descriptor = generator.generateMetadata();
        ExtendedMetadata extendedMetadata = generator.generateExtendedMetadata();

        MetadataMemoryProvider memoryProvider = new MetadataMemoryProvider(descriptor);
        //memoryProvider.initialize();
        return new SamlConnectionExtendedMetadataDelegate(memoryProvider, extendedMetadata, connection.getCode(), true);
    }

    protected SamlConnectionMetadataProvider generateIdpProvider(SamlConnection connection) throws MetadataProviderException {
        MetadataProvider provider;
        if (!StringUtils.isEmpty(connection.getIdpMetadataUrl())) {
            HttpClientParams clientParams = new HttpClientParams();
            clientParams.setSoTimeout(15000);
            HttpClient httpClient = new HttpClient(clientParams);

            HTTPMetadataProvider httpProvider = new HTTPMetadataProvider(null, httpClient, connection.getIdpMetadataUrl());
            httpProvider.setParserPool(parserPool);

            provider = httpProvider;
        } else if (connection.getIdpMetadata() != null) {
            FileStorageMetadataProvider fileStorageProvider = new FileStorageMetadataProvider(connection.getIdpMetadata());
            fileStorageProvider.setParserPool(parserPool);

            provider = fileStorageProvider;
        } else {
            throw new DevelopmentException(String.format("IDP metadata not specified in SAML connection %s", connection.getName()));
        }
        // provider.initialize();
        return new SamlConnectionExtendedMetadataDelegate(provider, connection.getCode(), false);
    }

    @Override
    public void remove(SamlConnection connection) {
        Preconditions.checkNotNullArgument(connection, "SAML connection is empty");

        SamlConnectionMetadataProvider spProvider = getProvider(connection.getCode(), true);
        if (spProvider != null) {
            super.removeMetadataProvider(spProvider);
            try {
                spProvider.destroy();
            } catch (Exception e) {
                log.error("Failed to destroy SP metadata provider", e);
            }
        }
        SamlConnectionMetadataProvider idpProvider = getProvider(connection.getCode(), false);
        if (idpProvider != null) {
            super.removeMetadataProvider(idpProvider);
            try {
                idpProvider.destroy();
            } catch (Exception e) {
                log.error("Failed to destroy IDP metadata provider", e);
            }
        }
    }

    @Override
    public void refresh() {
        refreshMetadata();
    }

    @Override
    public String generateSpMetadata(SamlConnection connection) throws MetadataProviderException {
        Preconditions.checkNotNullArgument(connection, "SAML connection is empty");

        KeyManager keyManager = connectionsKeyManager.generateKeyManager(connection);
        SamlConnectionMetadataProvider provider = generateSpProvider(connection, keyManager);

        provider.initialize();
        try {
            String entityId = getSpEntityId(provider);
            EntityDescriptor descriptor = entityId == null ? null : provider.getEntityDescriptor(entityId);
            if (descriptor == null) {
                throw new MetadataProviderException("Metadata entity with ID " + entityId + " wasn't found");
            }
            ExtendedMetadata extendedMetadata = getExtendedMetadata(entityId, provider);
            if (extendedMetadata == null) {
                throw new MetadataProviderException("Extended metadata entity with ID " + entityId + " wasn't found");
            }
            try {
                String result = SAMLUtil.getMetadataAsString(null, keyManager, descriptor, extendedMetadata);
                result = prettyPrint(result);
                return result;
            } catch (MarshallingException e) {
                throw new MetadataProviderException("Metadata parsing failed", e);
            }
        } finally {
            try {
                provider.destroy();
            } catch (Exception e) {
                log.error("Failed to destroy SP metadata provider", e);
            }
        }
    }

    @Override
    public String generateIdpMetadata(SamlConnection connection) throws MetadataProviderException {
        Preconditions.checkNotNullArgument(connection, "SAML connection is empty");
        SamlConnectionMetadataProvider provider = generateIdpProvider(connection);

        provider.initialize();
        try {
            String entityId = getIdpEntityId(provider);
            EntityDescriptor descriptor = entityId == null ? null : provider.getEntityDescriptor(entityId);
            if (descriptor == null) {
                throw new MetadataProviderException("Metadata entity with ID " + entityId + " wasn't found");
            }
            ExtendedMetadata extendedMetadata = getExtendedMetadata(entityId, provider);
            if (extendedMetadata == null) {
                throw new MetadataProviderException("Extended metadata entity with ID " + entityId + " wasn't found");
            }
            try {
                String result = SAMLUtil.getMetadataAsString(null, keyManager, descriptor, extendedMetadata);
                result = prettyPrint(result);
                return result;
            } catch (MarshallingException e) {
                throw new MetadataProviderException("Metadata parsing failed", e);
            }
        } finally {
            try {
                provider.destroy();
            } catch (Exception e) {
                log.error("Failed to destroy IDP metadata provider", e);
            }
        }
    }

    protected String prettyPrint(String value) {
        if (!StringUtils.isEmpty(value)) {
            try (StringWriter sw = new StringWriter()) {
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                transformer.transform(new ByteArraySource(value.getBytes(StandardCharsets.UTF_8)), new StreamResult(sw));
                return sw.toString();
            } catch (Exception e) {
                log.error("Failed to pretty print", e);
            }
        }
        return value;
    }

    protected ExtendedMetadata getExtendedMetadata(String entityId, MetadataProvider provider) throws MetadataProviderException {
        if (provider instanceof ExtendedMetadataProvider) {
            ExtendedMetadataProvider extendedProvider = (ExtendedMetadataProvider) provider;
            ExtendedMetadata extendedMetadata = extendedProvider.getExtendedMetadata(entityId);
            if (extendedMetadata != null) {
                return extendedMetadata.clone();
            }
        }
        return null;
    }

    /**
     * Method is expected to construct information resolver with all trusted data available for the given provider.
     *
     * @param provider     provider
     * @param trustedKeys  trusted keys for the providers
     * @param trustedNames trusted names for the providers (always null)
     * @return information resolver
     */
    @Override
    protected PKIXValidationInformationResolver getPKIXResolver(MetadataProvider provider, Set<String> trustedKeys, Set<String> trustedNames) {

        KeyManager km = getKeyManager(provider);
        // Use all available keys
        if (trustedKeys == null) {
            trustedKeys = km.getAvailableCredentials();
        }

        // Resolve allowed certificates to build the anchors
        List<X509Certificate> certificates = new LinkedList<X509Certificate>();
        for (String key : trustedKeys) {
            log.debug("Adding PKIX trust anchor {} for metadata verification of provider {}", key, provider);
            X509Certificate certificate = km.getCertificate(key);
            if (certificate != null) {
                certificates.add(certificate);
            } else {
                log.warn("Cannot construct PKIX trust anchor for key with alias {} for provider {}, key isn't included in the keystore", key, provider);
            }
        }

        List<PKIXValidationInformation> info = new LinkedList<PKIXValidationInformation>();
        info.add(new BasicPKIXValidationInformation(certificates, null, 4));
        return new StaticPKIXValidationInformationResolver(info, trustedNames) {
            @Override
            public Set<String> resolveTrustedNames(CriteriaSet criteriaSet)
                    throws SecurityException, UnsupportedOperationException {
                Set<String> names = super.resolveTrustedNames(criteriaSet);
                //previous implementation returned true
                //if trustedNames was empty(), not just null
                //https://git.shibboleth.net/view/?p=java-xmltooling.git;a=commitdiff;h=c3c19e4857b815c7c05fa3b675f9cd1adde43429#patch2
                if (names.isEmpty()) {
                    return null;
                } else {
                    return names;
                }
            }
        };

    }

    @Nullable
    protected SamlConnectionMetadataProvider getProvider(String connectionCode, boolean sp) {
        if (!StringUtils.isEmpty(connectionCode)) {
            List<MetadataProvider> providers = getProviders();
            for (MetadataProvider provider : providers) {
                SamlConnectionMetadataProvider connectionMetadataProvider = (SamlConnectionMetadataProvider) provider;
                if (sp == connectionMetadataProvider.isSp() && connectionCode.equals(connectionMetadataProvider.getConnectionCode())) {
                    return connectionMetadataProvider;
                }
            }
        }
        return null;
    }

    protected KeyManager getKeyManager(MetadataProvider provider) {
        KeyManager km = connectionsKeyManager;
        if (provider instanceof SamlConnectionMetadataProvider) {
            km = connectionsKeyManager.getKeyManager(((SamlConnectionMetadataProvider) provider).getConnectionCode());
        }
        return km;
    }

    protected static class FileStorageMetadataProvider extends AbstractReloadingMetadataProvider {
        protected FileDescriptor fd;

        protected FileStorageMetadataProvider(FileDescriptor metadataFileDescriptor) {
            fd = metadataFileDescriptor;
        }

        @Override
        protected String getMetadataIdentifier() {
            return fd.getName();
        }

        @Override
        protected byte[] fetchMetadata() throws MetadataProviderException {
            try {
                return getFileStorageService().loadFile(fd);
            } catch (FileStorageException e) {
                throw new MetadataProviderException(e);
            }
        }

        protected FileStorageService getFileStorageService() {
            return AppBeans.get(FileStorageService.NAME);
        }
    }
}
