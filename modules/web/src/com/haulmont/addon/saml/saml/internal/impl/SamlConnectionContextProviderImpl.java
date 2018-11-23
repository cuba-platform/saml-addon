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

import com.haulmont.addon.saml.saml.internal.SamlConnectionMessageContext;
import com.haulmont.addon.saml.saml.internal.SamlConnectionsKeyManager;
import com.haulmont.addon.saml.saml.internal.SamlConnectionsMetadataManager;
import com.haulmont.addon.saml.saml.internal.SamlProxyServerConfiguration;
import com.haulmont.addon.saml.web.security.saml.SamlCommunicationServiceBean;
import com.haulmont.addon.saml.web.security.saml.SamlSessionPrincipal;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.encryption.Decrypter;
import org.opensaml.saml2.encryption.EncryptedElementTypeEncryptedKeyResolver;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.security.MetadataCredentialResolver;
import org.opensaml.ws.transport.http.HTTPInTransport;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.encryption.ChainingEncryptedKeyResolver;
import org.opensaml.xml.encryption.InlineEncryptedKeyResolver;
import org.opensaml.xml.encryption.SimpleRetrievalMethodEncryptedKeyResolver;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xml.security.keyinfo.StaticKeyInfoCredentialResolver;
import org.opensaml.xml.security.trust.ExplicitX509CertificateTrustEngine;
import org.opensaml.xml.security.trust.TrustEngine;
import org.opensaml.xml.security.x509.BasicX509CredentialNameEvaluator;
import org.opensaml.xml.security.x509.PKIXValidationInformationResolver;
import org.opensaml.xml.security.x509.PKIXX509CredentialTrustEngine;
import org.opensaml.xml.security.x509.X509Credential;
import org.opensaml.xml.signature.SignatureTrustEngine;
import org.opensaml.xml.signature.impl.ExplicitKeySignatureTrustEngine;
import org.opensaml.xml.signature.impl.PKIXSignatureTrustEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.saml.SAMLEntryPoint;
import org.springframework.security.saml.context.SAMLContextProviderImpl;
import org.springframework.security.saml.context.SAMLMessageContext;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.MetadataManager;
import org.springframework.security.saml.trust.CertPathPKIXTrustEvaluator;
import org.springframework.security.saml.trust.PKIXInformationResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

/**
 * Extended SAML context provider which communicate with SAML connections
 *
 * @author adiatullin
 */
public class SamlConnectionContextProviderImpl extends SAMLContextProviderImpl {
    private static final Logger log = LoggerFactory.getLogger(SamlConnectionContextProviderImpl.class);

    // Way to obtain encrypted key info from XML Encryption
    protected static ChainingEncryptedKeyResolver encryptedKeyResolver = new ChainingEncryptedKeyResolver();

    static {
        encryptedKeyResolver.getResolverChain().add(new InlineEncryptedKeyResolver());
        encryptedKeyResolver.getResolverChain().add(new EncryptedElementTypeEncryptedKeyResolver());
        encryptedKeyResolver.getResolverChain().add(new SimpleRetrievalMethodEncryptedKeyResolver());
    }

    @Inject
    protected SamlConnectionsMetadataManager connectionsMetadataManager;
    @Inject
    protected SamlConnectionsKeyManager connectionsKeyManager;
    @Inject
    protected SamlCommunicationServiceBean samlCommunicationServiceBean;

    /**
     * Creates a SAMLContext with local entity values filled. Also request and response must be stored in the context
     * as message transports.
     *
     * @param request  request
     * @param response response
     * @return context
     * @throws MetadataProviderException in case of metadata problems
     */
    @Override
    public SAMLMessageContext getLocalEntity(HttpServletRequest request, HttpServletResponse response) throws MetadataProviderException {
        SAMLMessageContext context = createContext(request, response);

        populateConnection(request, response, context);
        populateGenericContext(request, response, context);
        populateLocalEntityId(context, request.getRequestURI());
        populateLocalContext(context);

        return context;
    }

    /**
     * Creates a SAMLContext with local entity and peer values filled. Also request and response must be stored in the context
     * as message transports. Should be used when both local entity and peer entity can be determined from the request.
     *
     * @param request  request
     * @param response response
     * @return context
     * @throws MetadataProviderException in case of metadata problems
     */
    @Override
    public SAMLMessageContext getLocalAndPeerEntity(HttpServletRequest request, HttpServletResponse response) throws MetadataProviderException {
        SAMLMessageContext context = createContext(request, response);

        populateConnection(request, response, context);
        populateGenericContext(request, response, context);
        populateLocalEntityId(context, request.getRequestURI());
        populateLocalContext(context);
        populatePeerEntityId(context);
        populatePeerContext(context);

        return context;
    }

    protected SAMLMessageContext createContext(HttpServletRequest request, HttpServletResponse response) {
        return new SamlConnectionMessageContext();
    }

    protected void populateConnection(HttpServletRequest request, HttpServletResponse response, SAMLMessageContext context) {
        if (context instanceof SamlConnectionMessageContext) {
            String code = (String) request.getSession().getAttribute(SamlSessionPrincipal.SAML_CONNECTION_CODE);
            if (StringUtils.isEmpty(code)) {
                String[] params = request.getParameterMap().get(SamlSessionPrincipal.SAML_CONNECTION_CODE);
                if (params != null && params.length > 0) {
                    code = params[0];
                }
                if (!StringUtils.isEmpty(code)) {
                    request.getSession().setAttribute(SamlSessionPrincipal.SAML_CONNECTION_CODE, code);
                }
            }
            if (StringUtils.isEmpty(code)) {
                throw new RuntimeException("Failed to determinate SAML connection");
            }
            ((SamlConnectionMessageContext) context).setConnectionCode(code);
        }
    }

    @Override
    protected void populateGenericContext(HttpServletRequest request, HttpServletResponse response, SAMLMessageContext context) throws MetadataProviderException {
        if (samlCommunicationServiceBean.isProxyEnabled()) {
            request = new ProxyRequestWrapper(request, samlCommunicationServiceBean.getProxyConfiguration());
        }
        super.populateGenericContext(request, response, context);
    }

    /**
     * First tries to find pre-configured IDP from the request attribute. If not found
     * loads the IDP_PARAMETER from the request and if it is not null verifies whether IDP with this value is valid
     * IDP in our circle of trust. Processing fails when IDP is not valid. IDP is set as PeerEntityId in the context.
     * <p>
     * If request parameter is null the default IDP is returned.
     *
     * @param context context to populate ID for
     * @throws MetadataProviderException in case provided IDP value is invalid
     */
    @Override
    protected void populatePeerEntityId(SAMLMessageContext context) throws MetadataProviderException {
        HTTPInTransport inTransport = (HTTPInTransport) context.getInboundMessageTransport();
        String entityId;

        entityId = (String) inTransport.getAttribute(org.springframework.security.saml.SAMLConstants.PEER_ENTITY_ID);
        if (entityId != null) { // Pre-configured entity Id
            log.debug("Using protocol specified IDP {}", entityId);
        } else {
            entityId = inTransport.getParameterValue(SAMLEntryPoint.IDP_PARAMETER);
            if (entityId != null) { // IDP from request
                log.debug("Using user specified IDP {} from request", entityId);
                context.setPeerUserSelected(true);
            } else if (context instanceof SamlConnectionMessageContext) {
                entityId = connectionsMetadataManager.getIdpEntityId(((SamlConnectionMessageContext) context).getConnectionCode());
                context.setPeerUserSelected(false);
            } else { // Default IDP
                entityId = metadata.getDefaultIDP();
                log.debug("No IDP specified, using default {}", entityId);
                context.setPeerUserSelected(false);
            }
        }

        context.setPeerEntityId(entityId);
        context.setPeerEntityRole(IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
    }

    /**
     * Returns localEntityId to be populated for the context in case alias is missing from the path
     *
     * @param context    context to retrieve localEntityId for
     * @param requestURI context path to parse entityId from
     * @return localEntityId
     * @throws MetadataProviderException in case entityId can't be retrieved
     */
    @Override
    protected String getDefaultLocalEntityId(SAMLMessageContext context, String requestURI) throws MetadataProviderException {
        if (context instanceof SamlConnectionMessageContext) {
            return connectionsMetadataManager.getSpEntityId(((SamlConnectionMessageContext) context).getConnectionCode());
        }
        return super.getDefaultLocalEntityId(context, requestURI);
    }

    /**
     * Method populates fields localEntityId, localEntityRole, localEntityMetadata, localEntityRoleMetadata and peerEntityRole.
     * In case fields localAlias, localEntityId, localEntiyRole or peerEntityRole are set they are used, defaults of default SP and IDP as a peer
     * are used instead.
     *
     * @param samlContext context to populate
     * @throws org.opensaml.saml2.metadata.provider.MetadataProviderException in case metadata do not contain expected entities or localAlias is specified but not found
     */
    @Override
    protected void populateLocalEntity(SAMLMessageContext samlContext) throws MetadataProviderException {

        String localEntityId = samlContext.getLocalEntityId();
        QName localEntityRole = samlContext.getLocalEntityRole();

        if (localEntityId == null) {
            throw new MetadataProviderException("No hosted service provider is configured and no alias was selected");
        }

        EntityDescriptor entityDescriptor = metadata.getEntityDescriptor(localEntityId);
        RoleDescriptor roleDescriptor = metadata.getRole(localEntityId, localEntityRole, SAMLConstants.SAML20P_NS);
        ExtendedMetadata extendedMetadata = metadata.getExtendedMetadata(localEntityId);

        if (entityDescriptor == null || roleDescriptor == null) {
            throw new MetadataProviderException("Metadata for entity " + localEntityId + " and role " + localEntityRole + " wasn't found");
        }

        samlContext.setLocalEntityMetadata(entityDescriptor);
        samlContext.setLocalEntityRoleMetadata(roleDescriptor);
        samlContext.setLocalExtendedMetadata(extendedMetadata);

        if (extendedMetadata.getSigningKey() != null) {
            samlContext.setLocalSigningCredential(getKeyManager(samlContext).getCredential(extendedMetadata.getSigningKey()));
        } else {
            samlContext.setLocalSigningCredential(getKeyManager(samlContext).getDefaultCredential());
        }

    }

    /**
     * Populates X509 Credential used to authenticate this machine against peer servers. Uses key with alias specified
     * in extended metadata under TlsKey, when not set uses the default credential.
     *
     * @param samlContext context to populate
     */
    @Override
    protected void populateSSLCredential(SAMLMessageContext samlContext) {

        X509Credential tlsCredential;
        if (samlContext.getLocalExtendedMetadata().getTlsKey() != null) {
            tlsCredential = (X509Credential) getKeyManager(samlContext).getCredential(samlContext.getLocalExtendedMetadata().getTlsKey());
        } else {
            tlsCredential = null;
        }

        samlContext.setLocalSSLCredential(tlsCredential);

    }

    /**
     * Populates a decrypter based on settings in the extended metadata or using a default credential when no
     * encryption credential is specified in the extended metadata.
     *
     * @param samlContext context to populate decryptor for.
     */
    @Override
    protected void populateDecrypter(SAMLMessageContext samlContext) {

        // Locate encryption key for this entity
        Credential encryptionCredential;
        if (samlContext.getLocalExtendedMetadata().getEncryptionKey() != null) {
            encryptionCredential = getKeyManager(samlContext).getCredential(samlContext.getLocalExtendedMetadata().getEncryptionKey());
        } else {
            encryptionCredential = getKeyManager(samlContext).getDefaultCredential();
        }

        // Entity used for decrypting of encrypted XML parts
        // Extracts EncryptedKey from the encrypted XML using the encryptedKeyResolver and attempts to decrypt it
        // using private keys supplied by the resolver.
        KeyInfoCredentialResolver resolver = new StaticKeyInfoCredentialResolver(encryptionCredential);

        Decrypter decrypter = new Decrypter(null, resolver, encryptedKeyResolver);
        decrypter.setRootInNewDocument(true);

        samlContext.setLocalDecrypter(decrypter);

    }

    /**
     * Based on the settings in the extended metadata either creates a PKIX trust engine with trusted keys specified
     * in the extended metadata as anchors or (by default) an explicit trust engine using data from the metadata or
     * from the values overridden in the ExtendedMetadata.
     *
     * @param samlContext context to populate
     */
    @Override
    protected void populateTrustEngine(SAMLMessageContext samlContext) {
        SignatureTrustEngine engine;
        if ("pkix".equalsIgnoreCase(samlContext.getLocalExtendedMetadata().getSecurityProfile())) {
            engine = new PKIXSignatureTrustEngine(getInformationResolver(samlContext), Configuration.getGlobalSecurityConfiguration().getDefaultKeyInfoCredentialResolver(), pkixTrustEvaluator, new BasicX509CredentialNameEvaluator());
        } else {
            engine = new ExplicitKeySignatureTrustEngine(getMetadataResolver(samlContext), Configuration.getGlobalSecurityConfiguration().getDefaultKeyInfoCredentialResolver());
        }
        samlContext.setLocalTrustEngine(engine);
    }

    /**
     * Based on the settings in the extended metadata either creates a PKIX trust engine with trusted keys specified
     * in the extended metadata as anchors or (by default) an explicit trust engine using data from the metadata or
     * from the values overridden in the ExtendedMetadata. The trust engine is used to verify SSL connections.
     *
     * @param samlContext context to populate
     */
    @Override
    protected void populateSSLTrustEngine(SAMLMessageContext samlContext) {
        TrustEngine<X509Credential> engine;
        if ("pkix".equalsIgnoreCase(samlContext.getLocalExtendedMetadata().getSslSecurityProfile())) {
            engine = new PKIXX509CredentialTrustEngine(getInformationResolver(samlContext), pkixTrustEvaluator, new BasicX509CredentialNameEvaluator());
        } else {
            engine = new ExplicitX509CertificateTrustEngine(getMetadataResolver(samlContext));
        }
        samlContext.setLocalSSLTrustEngine(engine);
    }

    protected PKIXValidationInformationResolver getInformationResolver(SAMLMessageContext samlContext) {
        PKIXValidationInformationResolver resolver = pkixResolver;
        if (resolver == null) {
            resolver = new PKIXInformationResolver(getMetadataResolver(samlContext), (MetadataManager) connectionsMetadataManager, getKeyManager(samlContext));
        }
        return resolver;
    }

    protected MetadataCredentialResolver getMetadataResolver(SAMLMessageContext samlContext) {
        MetadataCredentialResolver resolver = metadataResolver;
        if (resolver == null) {
            resolver = new org.springframework.security.saml.trust.MetadataCredentialResolver((MetadataManager) connectionsMetadataManager, getKeyManager(samlContext));
            resolver.setMeetAllCriteria(false);
            resolver.setUnevaluableSatisfies(true);
        }
        return resolver;
    }

    protected KeyManager getKeyManager(SAMLMessageContext samlContext) {
        KeyManager km = connectionsKeyManager;
        if (samlContext instanceof SamlConnectionMessageContext) {
            km = connectionsKeyManager.getKeyManager(((SamlConnectionMessageContext) samlContext).getConnectionCode());
        }
        return km;
    }

    /**
     * Verifies that required entities were autowired or set and initializes resolvers used to construct trust engines.
     */
    @Override
    public void afterPropertiesSet() throws ServletException {
        Assert.notNull(keyManager, "Key manager must be set");
        Assert.notNull(metadata, "Metadata must be set");
        Assert.notNull(storageFactory, "MessageStorageFactory must be set");

        if (pkixTrustEvaluator == null) {
            pkixTrustEvaluator = new CertPathPKIXTrustEvaluator();
        }
    }

    protected class ProxyRequestWrapper extends HttpServletRequestWrapper {

        protected SamlProxyServerConfiguration proxyConfig;

        protected ProxyRequestWrapper(HttpServletRequest request, SamlProxyServerConfiguration proxyConfig) {
            super(request);

            this.proxyConfig = proxyConfig;
        }

        @Override
        public String getContextPath() {
            return proxyConfig.getContextPath();
        }

        @Override
        public String getScheme() {
            return proxyConfig.getScheme();
        }

        @Override
        public String getServerName() {
            return proxyConfig.getServerName();
        }

        @Override
        public int getServerPort() {
            return proxyConfig.getServerPort();
        }

        @Override
        public String getRequestURI() {
            return getContextPath() + getServletPath();
        }

        @Override
        public StringBuffer getRequestURL() {
            StringBuffer sb = new StringBuffer();

            sb.append(getScheme()).append("://").append(getServerName());
            if (proxyConfig.isIncludePortInUrl()) {
                sb.append(":").append(getServerPort());
            }
            sb.append(getContextPath());
            sb.append(getServletPath());
            if (getPathInfo() != null) {
                sb.append(getPathInfo());
            }

            return sb;
        }

        @Override
        public boolean isSecure() {
            return "https".equalsIgnoreCase(getScheme());
        }
    }
}
