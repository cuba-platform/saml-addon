<p>
    <a href="http://www.apache.org/licenses/LICENSE-2.0"><img src="https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat" alt="license" title=""></a>
    <a href="https://travis-ci.org/cuba-platform/saml-addon"><img src="https://travis-ci.org/cuba-platform/saml-addon.svg?branch=master" alt="Build Status" title=""></a>
</p>

# SAML

- [1. Overview](#overview)
- [2. Installation](#installation)
 - [2.1. From the Marketplace](#from-the-marketplace)
 - [2.2. By Coordinates](#by-coordinates)
- [3. Configuration](#configuration)
  - [3.1. Keystore](#keystore)
    - [3.1.1. Creating Keystore](#keystore-create)
  - [3.2. SAML Connection](#saml-connection)
  - [3.3. Tenant Logging](#tenant-logging)
  - [3.4. SAML Processor](#saml-processor)
- [4. Implementation](#implementation)
  - [4.1. Extension of the Standard Login Window](#extension-login-window)
  - [4.2. Setup signing method for SAML messages](#setup-signing-method)
- [5. General Application Properties](#general-properties)

# 1. Overview <a name="overview"></a>

This component provides a readily available instrument of authentication in any CUBA-based application using SAML open standard. That allows identity provider (IdP) to pass authorization credentials to your applications - service providers (SP).

The add-on enables Single Sign-On in your application. You log in once with the IdP and this set of credentials will be used to log in your CUBA applications.

Key features:
 -  simplified authorization procedure for users and service providers;
 -  separately existing of an identity provider and service providers, which centralizes user management;
 -  user interface to set and configure SAML connections.

See [sample project](https://github.com/cuba-platform/saml-addon-demo) using this add-on.

# 2. Installation <a name="installation"></a>

The add-on can be added to your project in one of the ways described below. Installation from the Marketplace is the simplest way. The last version of the add-on compatible with the used version of the platform will be installed.
Also, you can install the add-on by coordinates choosing the required version of the add-on from the table.

In case you want to install the add-on by manual editing or by building from sources see the complete add-ons installation guide in [CUBA Platform documentation](https://doc.cuba-platform.com/manual-latest/manual.html#app_components_usage).

## 2.1. From the Marketplace <a name="from-the-marketplace"></a>

1. Open your application in CUBA Studio. Check the latest version of CUBA Studio on the [CUBA Platform site](https://www.cuba-platform.com/download/previous-studio/).
2. Go to *CUBA -> Marketplace* in the main menu.

 ![marketplace](img/marketplace.png)

3. Find the SAML add-on there.

 ![addons](img/addons.png)

4. Click *Install* and apply the changes.
The add-on corresponding to the used platform version will be installed.

## 2.2. By coordinates <a name="by-coordinates"></a>

1. Open your application in CUBA Studio. Check the latest version of CUBA Studio on the [CUBA Platform site](https://www.cuba-platform.com/download/previous-studio/).
2. Go to *CUBA -> Marketplace* in the main menu.
3. Click the icon in the upper-right corner.

 ![by-coordinates](img/by-coordinates.png)

4. Paste the add-on coordinates in the corresponding field as follows:

 `com.haulmont.addon.saml:saml-addon-global:<add-on version>`

 where `<add-on version>` is compatible with the used version of the CUBA platform.

 | Platform Version | Add-on Version |
|------------------|----------------|
|7.2.x             | 0.4.1          |
|7.1.x             | 0.3.0          |
|7.0.x             | 0.2.2          |
|6.10.x            | 0.1.0          |

5. Click *Install* and apply the changes. The add-on will be installed to your project.

# 3. Configuration <a name="configuration"></a>

To use your own key for keystore passwords encryption specify `encryption.key` and `encryption.iv` properties in `app.properties.xml` in the `core` module. Otherwise, the default keys declared in the `app-component.xml` file will be used.

The further configuration consists of creating keystore and setting SAML connection.

## 3.1. Keystore <a name="keystore"></a>

Before setting SAML connection you need to create keystore containing a username, password, description, and JKS (Java Key Store) file. Your service provider application must have a unique public/private key pair.

### 3.1.1 Creating keystore <a name="keystore-create"></a>

Firstly, you need to generate a public/private key pair. Use the following links to instructions:

- [to generate a private key](https://docs.spring.io/spring-security-saml/docs/1.0.4.RELEASE/reference/html/security.html#configuration-key-management-private-keys);
- [to generate a public key](https://docs.spring.io/spring-security-saml/docs/1.0.4.RELEASE/reference/html/security.html#configuration-key-management-public-keys).

You will get JKS file as the result.

Create a keystore using your application UI:
1. Go to *Administration -> SAML* screen.
2. Click the *KeyStore* button.
2. Click the *Create* button.
3. Fill in the *Login* field - login that was used for JKS file generation.
4. Fill in the *Password* field - password that was used for JKS file generation.
4. (Optional) Fill in the *Description* field - will be used with the login as keystore representation in *SAML Connection editor* screen.
5. Upload `.jks` keystore file.
6. Click *OK* to create the keystore with entered settings.

You can not delete keystore if it is linked at least to one connection. Firstly, you need to unselect keystore in *SAML Connection editor* screen.

## 3.2 SAML Connection <a name="saml-connection"></a>

To configure SAML connection to identity provider do the following steps:
1. Go to *Administration -> SAML* screen.
2. Click the *Create* button.
3. Fill in the *Name* field - it will be shown to users in the login screen.
4. Fill in the *SSO Path* field - it will be used for tenant login.
5. Select the required keystore in the drop-down list of *Keystore* field.
6. Choose *Default access group* that will be set to new users logged in with this IdP.
7. Choose *Processing service* to process new users logged in with this IdP.
8. Fill in the *Service provider identity*. This field will be used by IdP to identify your application. For example: `cuba-saml-demo`. Then click the *Refresh* button. Copy the generated XML from the field below and register it in the IdP.
9. Fill in the *Identity Provider metadata URL* field provided by this IdP. Example: `http://idp.ssocircle.com/idp-meta.xml`.
    Then click the *Refresh* button. If the URL is correct and IdP works OK - you will see some XML content below.
    Another way to specify IdP metadata is to upload an XML file using the corresponding button.
10. Click *User Creation* checkbox, if you want to create a user from information received from IdP in case the user does not exist in the application.
11. Click *Active* checkbox. After that, the IdP will be shown in the login screen.
12. Click *OK* to save settings.

## 3.3 Tenant Logging <a name="tenant-logging"></a>

Using a specific tenant URL is a simple way to log in. For example,
`http://localhost:8080/app/saml/login?tenant=ssoPath`, where `ssoPath` is the value of the field with the same name in SAML Connection entity. When you use such URL, the system automatically redirects you to the specific IdP.

## 3.4 SAML Processor <a name="saml-processor"></a>

By default, the component provides `BaseSamlProcessor` which fills in the following attributes for the new user from the SAML session:

- FirstName
- LastName
- MiddleName
- EmailAddress

However, you can define your own implementation of the interface `com.haulmont.addon.saml.core.SamlProcessor` which will handle the SAML data using your own logic.
The `getName()` method should return a user-friendly name, to show it in the lookup field on the *SAML Connection editor* screen.

# 4. Implementation <a name="implementation"></a>

## 4.1. Extension of the Standard Login Window <a name="extension-login-window"></a>

To extend the standard login screen:
1. Open your project in CUBA Studio.
2. Expand the *Generic UI* in the CUBA project tree.
3. Right-click *Screens* and go to *New -> Screen*.
4. Go to the *Legacy Screen Templates* tab and select the *Login window*.
5. Click *Next -> Finish*.

Then add a lookup field with the list of IdP providers in the screen controller. When you choose one of providers SAML request will be initiated.

Here is an example of the implementation of the whole controller:

1. Screen controller `ext-loginWindow.xml`:

<details><summary>Click to expand the code</summary>

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<window xmlns="http://schemas.haulmont.com/cuba/window.xsd"
        class="com.haulmont.sd.web.screens.ExtAppLoginWindow"
        extends="/com/haulmont/cuba/web/app/loginwindow/loginwindow.xml"
        xmlns:ext="http://schemas.haulmont.com/cuba/window-ext.xsd"
        messagesPack="com.haulmont.sd.web.screens">
    <dialogMode height="600"
                width="800"/>
    <layout>
        <vbox id="loginWrapper">
            <vbox id="loginMainBox">
                <grid id="loginFormLayout">
                    <columns>
                        <column id="loginFormCaptionColumn"/>
                        <column id="loginFormFieldColumn"/>
                    </columns>
                    <rows>
                        <row id="ssoRow" ext:index="0">
                            <label id="ssoLookupFieldLabel" value="msg://captions.loginBy" align="MIDDLE_CENTER"/>
                            <lookupField id="ssoLookupField" nullOptionVisible="true" align="MIDDLE_CENTER"/>
                        </row>
                    </rows>
                </grid>
            </vbox>
        </vbox>
    </layout>
</window>
```
 </details>


2. Java class `ExtAppLoginWindow.java`


<details><summary>Click to expand the example for 6.10</summary>

```java
import com.haulmont.addon.saml.entity.SamlConnection;
import com.haulmont.addon.saml.security.SamlSession;
import com.haulmont.addon.saml.security.config.SamlConfig;
import com.haulmont.addon.saml.service.SamlService;
import com.haulmont.addon.saml.web.security.saml.SamlSessionPrincipal;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.SecurityContext;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.UIAccessor;
import com.haulmont.cuba.security.app.TrustedClientService;
import com.haulmont.cuba.security.auth.Credentials;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.LoginException;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.app.loginwindow.AppLoginWindow;
import com.haulmont.cuba.web.auth.WebAuthConfig;
import com.haulmont.cuba.web.security.ExternalUserCredentials;
import com.vaadin.server.*;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExtAppLoginWindow extends AppLoginWindow {

    private static final Logger log = LoggerFactory.getLogger(ExtAppLoginWindow.class);

    @Inject
    protected SamlService samlService;
    @Inject
    protected TrustedClientService trustedClientService;
    @Inject
    protected DataManager dataManager;
    @Inject
    protected BackgroundWorker backgroundWorker;

    @Inject
    protected SamlConfig samlConfig;
    @Inject
    protected WebAuthConfig webAuthConfig;

    @Inject
    protected Label ssoLookupFieldLabel;
    @Inject
    protected LookupField ssoLookupField;

    protected RequestHandler samlCallbackRequestHandler = this::handleSamlCallBackRequest;

    protected UIAccessor uiAccessor;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        uiAccessor = backgroundWorker.getUIAccessor();

        ssoLookupField.setOptionsList(getActiveConnections());
        ssoLookupFieldLabel.setVisible(!CollectionUtils.isEmpty(ssoLookupField.getOptionsList()));
        ssoLookupField.setVisible(!CollectionUtils.isEmpty(ssoLookupField.getOptionsList()));
        ssoLookupField.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                SamlConnection connection = (SamlConnection) e.getValue();
                VaadinSession.getCurrent().getSession().setAttribute(SamlSessionPrincipal.SAML_CONNECTION_CODE, connection.getCode());
                Page.getCurrent().setLocation(getLoginUrl());
            }
            ssoLookupField.setValue(null);
        });
    }

    @Override
    public void ready() {
        super.ready();

        VaadinSession.getCurrent().addRequestHandler(samlCallbackRequestHandler);
        try {
            samlCallbackRequestHandler.handleRequest(VaadinSession.getCurrent(), null, null);
        } catch (IOException e) {
            log.error("Failed to check SAML login", e);
        }
    }

    protected boolean handleSamlCallBackRequest(VaadinSession session, @Nullable VaadinRequest request,
                                                @Nullable VaadinResponse response) throws IOException {
        Principal principal = VaadinService.getCurrentRequest().getUserPrincipal();
        if (principal instanceof SamlSessionPrincipal) {
            SamlSessionPrincipal samlPrincipal = (SamlSessionPrincipal) principal;
            if (samlPrincipal.isActive()) {
                final SamlSession samlSession = samlPrincipal.getSamlSession();
                uiAccessor.accessSynchronously(() -> {
                    try {
                        User user = samlService.getUser(samlSession);
                        ExternalUserCredentials credentials = new ExternalUserCredentials(user.getLogin());
                        doLogin(credentials);
                    } catch (LoginException e) {
                        log.info("Login by SAML failed", e);

                        showLoginException(String.format(getMessage("errors.message.samlLoginFailed"), samlSession.getPrincipal()));
                    } catch (Exception e) {
                        log.warn("Login by SAML failed. Internal error.", e);

                        showUnhandledExceptionOnLogin(e);
                    }
                });
            }
        }
        return false;
    }

    @Override
    protected void doLogin(Credentials credentials) throws LoginException {
        super.doLogin(credentials);

        VaadinSession.getCurrent().removeRequestHandler(samlCallbackRequestHandler);
    }

    protected List<SamlConnection> getActiveConnections() {
        UserSession systemSession;
        try {
            systemSession = trustedClientService.getSystemSession(webAuthConfig.getTrustedClientPassword());
        } catch (LoginException e) {
            log.error("Unable to obtain system session", e);
            return Collections.emptyList();
        }
        return AppContext.withSecurityContext(new SecurityContext(systemSession), () -> {
            List<SamlConnection> items = dataManager.loadList(LoadContext.create(SamlConnection.class)
                    .setQuery(new LoadContext.Query("select e from samladdon$SamlConnection e where e.active = true order by e.code"))
                    .setView(View.MINIMAL));
            return items;
        });
    }

    protected String getLoginUrl() {
        return (samlConfig.getProxyEnabled() ? samlConfig.getProxyServerUrl() : globalConfig.getWebAppUrl())
                 + samlConfig.getSamlBasePath() + samlConfig.getSamlLoginPath();
    }
}
 ```

</details>

<details><summary>Click to expand the example for 7.0</summary>

```java
import com.haulmont.addon.saml.entity.SamlConnection;
import com.haulmont.addon.saml.security.SamlSession;
import com.haulmont.addon.saml.security.config.SamlConfig;
import com.haulmont.addon.saml.service.SamlService;
import com.haulmont.addon.saml.web.security.saml.SamlSessionPrincipal;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.SecurityContext;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.UIAccessor;
import com.haulmont.cuba.security.app.TrustedClientService;
import com.haulmont.cuba.security.auth.Credentials;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.LoginException;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.app.loginwindow.AppLoginWindow;
import com.haulmont.cuba.web.auth.WebAuthConfig;
import com.haulmont.cuba.web.security.ExternalUserCredentials;
import com.vaadin.server.*;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

public class ExtAppLoginWindow extends AppLoginWindow {

    private static final Logger log = LoggerFactory.getLogger(ExtAppLoginWindow.class);

    @Inject
    protected SamlService samlService;
    @Inject
    protected TrustedClientService trustedClientService;
    @Inject
    protected DataManager dataManager;
    @Inject
    protected BackgroundWorker backgroundWorker;

    @Inject
    protected SamlConfig samlConfig;
    @Inject
    protected WebAuthConfig webAuthConfig;

    @Inject
    protected Label<String> ssoLookupFieldLabel;
    @Inject
    protected LookupField<SamlConnection> ssoLookupField;

    protected RequestHandler samlCallbackRequestHandler = this::handleSamlCallBackRequest;

    protected UIAccessor uiAccessor;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        uiAccessor = backgroundWorker.getUIAccessor();

        ssoLookupField.setOptionsList(getActiveConnections());
        ssoLookupFieldLabel.setVisible(!CollectionUtils.isEmpty(ssoLookupField.getOptionsList()));
        ssoLookupField.setVisible(!CollectionUtils.isEmpty(ssoLookupField.getOptionsList()));
        ssoLookupField.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                SamlConnection connection = e.getValue();
                VaadinSession.getCurrent().getSession().setAttribute(SamlSessionPrincipal.SAML_CONNECTION_CODE, connection.getSsoPath());
                Page.getCurrent().setLocation(getLoginUrl());
            }
            ssoLookupField.setValue(null);
        });
    }

    @Override
    public void ready() {
        super.ready();

        VaadinSession.getCurrent().addRequestHandler(samlCallbackRequestHandler);
        try {
            samlCallbackRequestHandler.handleRequest(VaadinSession.getCurrent(), null, null);
        } catch (IOException e) {
            log.error("Failed to check SAML login", e);
        }
    }

    protected boolean handleSamlCallBackRequest(VaadinSession session, @Nullable VaadinRequest request,
                                                @Nullable VaadinResponse response) throws IOException {
        Principal principal = VaadinService.getCurrentRequest().getUserPrincipal();
        if (principal instanceof SamlSessionPrincipal) {
            SamlSessionPrincipal samlPrincipal = (SamlSessionPrincipal) principal;
            if (samlPrincipal.isActive()) {
                final SamlSession samlSession = samlPrincipal.getSamlSession();
                uiAccessor.accessSynchronously(() -> {
                    try {
                        User user = samlService.getUser(samlSession);
                        if (isNull(user)) {
                            throw new LoginException("User does not exists");
                        }
                        ExternalUserCredentials credentials = new ExternalUserCredentials(user.getLogin());
                        doLogin(credentials);
                    } catch (LoginException e) {
                        log.info("Login by SAML failed", e);

                        showLoginException(String.format(getMessage("errors.message.samlLoginFailed"), samlSession.getPrincipal()));
                    } catch (Exception e) {
                        log.warn("Login by SAML failed. Internal error.", e);

                        showUnhandledExceptionOnLogin(e);
                    }
                });
            }
        }
        //check the error
        Object error = VaadinService.getCurrentRequest().getWrappedSession()
                .getAttribute(SamlSessionPrincipal.SAML_ERROR_ATTRIBUTE);
        if (error != null) {
            uiAccessor.accessSynchronously(() -> {
                showUnhandledExceptionOnLogin((Exception) error);
            });
        }
        return false;
    }

    @Override
    protected void doLogin(Credentials credentials) throws LoginException {
        super.doLogin(credentials);

        VaadinSession.getCurrent().removeRequestHandler(samlCallbackRequestHandler);
    }

    protected List<SamlConnection> getActiveConnections() {
        UserSession systemSession;
        try {
            systemSession = trustedClientService.getSystemSession(webAuthConfig.getTrustedClientPassword());
        } catch (LoginException e) {
            log.error("Unable to obtain system session", e);
            return Collections.emptyList();
        }
        return AppContext.withSecurityContext(new SecurityContext(systemSession), () -> {
            List<SamlConnection> items = dataManager.loadList(LoadContext.create(SamlConnection.class)
                    .setQuery(new LoadContext.Query("select e from samladdon$SamlConnection e where e.active = true order by e.ssoPath"))
                    .setView(View.MINIMAL));
            return items;
        });
    }

    protected String getLoginUrl() {
        return (samlConfig.getProxyEnabled() ? samlConfig.getProxyServerUrl() : globalConfig.getWebAppUrl())
                + samlConfig.getSamlBasePath() + samlConfig.getSamlLoginPath();
    }
}
```

</details>

3. The `messages.properties` file should contain the following strings:

```properties
captions.loginBy = Login by
errors.message.samlLoginFailed = User '%s' hasn't been logged by SAML.
```

4. The `web-app.properties` file should contain the following strings:

```properties
cuba.addon.saml.basePath = /saml
cuba.addon.saml.logoutPath = /logout
cuba.addon.saml.loginPath = /login
cuba.addon.saml.metadataPath = /metadata
cuba.addon.saml.responseSkewSec = 60
cuba.addon.saml.maxAuthenticationAgeSec = 7200
cuba.addon.saml.maxAssertionTimeSec = 3000
cuba.addon.saml.logAllSamlMessages = true
```

Also, you can observe the details of the implementation in the corresponding [demo project](https://git.haulmont.com/app-components/saml-addon-demo).

## 4.2. Setup signing method for SAML messages <a name="setup-signing-method"></a>

By default, OpenSAML component uses SHA1 digest algorithm for signing SAML messages. The most convenient way to use different signing messages
is to create a class in the `web` module with additional changes in `SecurityContext`.

<details><summary>Click to expand the example</summary>

```java
import org.opensaml.xml.Configuration;
import org.opensaml.xml.security.BasicSecurityConfiguration;
import org.opensaml.xml.signature.SignatureConstants;

public class SecurityConfiguration {

 public void initialize() {
        BasicSecurityConfiguration configuration = (BasicSecurityConfiguration) Configuration.getGlobalSecurityConfiguration();

        // Asymmetric key algorithms
        configuration.registerSignatureAlgorithmURI("RSA", SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        configuration.registerSignatureAlgorithmURI("DSA", SignatureConstants.ALGO_ID_SIGNATURE_DSA);
        configuration.registerSignatureAlgorithmURI("EC", SignatureConstants.ALGO_ID_SIGNATURE_ECDSA_SHA256);

        // HMAC algorithms
        configuration.registerSignatureAlgorithmURI("AES", SignatureConstants.ALGO_ID_MAC_HMAC_SHA256);
        configuration.registerSignatureAlgorithmURI("DESede", SignatureConstants.ALGO_ID_MAC_HMAC_SHA256);

        // Other signature-related params
        configuration.setSignatureCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        configuration.setSignatureHMACOutputLength(null);
        configuration.setSignatureReferenceDigestMethod(SignatureConstants.ALGO_ID_DIGEST_SHA256);
    }
}
```
</details>

Create a file in the `web` module for additional configuration of SAML servlet and declare the `SecurityConfiguration` class as a bean. For example, you can name this file as `saml-dispatcher-spring.xml`.

```xml

<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
           http://www.springframework.org/schema/beans/spring-beans-4.3.xsd">

    <bean class="com.haulmont.demo.saml.web.SecurityConfiguration" init-method="initialize" depends-on="samlBootstrap"/>

</beans>
```
Basic configuration is initialized in the `org.springframework.security.saml.SAMLBootstrap` class.
To make sure that security config is initialized and does not override your changes set depends-on attribute with the value of the bean id of the `org.springframework.security.saml.SAMLBootstrap` class (related bean is declared in the `saml-dispatcher-spring.xml` file of the addon).

Then add the `saml.springContextConfig` property to the `web-app.properties` file and set the value with the path of your additional configuration file.
(The `plus` sign is necessary, see [documentation](https://doc.cuba-platform.com/manual-latest/app_properties.html#additive_app_properties)).
```properties
saml.springContextConfig = +com/haulmont/demo/saml/saml-dispatcher-spring.xml
```
**Pay attention that the signing method declared in your configuration will be used for all created SAML connections!**
All supported signing methods are declared in the `org.opensaml.xml.signature.SignatureConstants` class.

# 5. General Application Properties <a name="general-properties"></a>

### cuba.addon.saml.basePath

* __Description:__ URL SAML context path, e.g. `/saml`
* __Interface:__ *SamlConfig*    
Used in the Web Client.

### cuba.addon.saml.loginPath

* __Description:__ SAML login path part, e.g. `/login`, and with the base path the result will be `/saml/logout`
* __Interface:__ *SamlConfig*    
Used in the Web Client.

### cuba.addon.saml.logoutPath

* __Description:__ SAML logout path part, e.g. `/logout` and with the base path the result will be `/saml/logout`
* __Interface:__ *SamlConfig*    
Used in the Web Client.

### cuba.addon.saml.metadataPath

* __Description:__ SAML metadata display path part, e.g. `/metadata` and with the base path the result will be `/saml/metadata?tenant=code` where code is `SAMLConnection.code`
* __Interface:__ *SamlConfig*    
Used in the Web Client.

### cuba.addon.saml.responseSkewSec

* __Description:__ Maximum difference between local time and time of the assertion creation which still allows message to be processed. Basically determines maximum difference between clocks of the IdP and SP machines (in seconds).
* __Default value:__ `60`
* __Interface:__ *SamlConfig*    
Used in the Web Client.

### cuba.addon.saml.maxAuthenticationAgeSec

* __Description:__ Maximum time between users authentication and processing of the AuthNResponse message (in seconds).
* __Default value:__ `7200`
* __Interface:__ *SamlConfig*    
Used in the Web Client.

### cuba.addon.saml.maxAssertionTimeSec

* __Description:__ Maximum time between assertion creation and current time when the assertion is usable (in seconds).
* __Default value:__ `3000`
* __Interface:__ *SamlConfig*    
Used in the Web Client.

### cuba.addon.saml.ssoLogout

* __Description:__ Defines whether the logout action will be also performed on the IdP when user performs logout in the CUBA application (SP)
* __Default value:__ `false`
* __Interface:__ *SamlConfig*    
Used in the Web Client.

### cuba.addon.saml.proxy.enabled

* __Description:__ Defines is a application use a proxy server or not
* __Default value:__ `false`
* __Interface:__ *SamlConfig*    
Used in the Web Client.


### cuba.addon.saml.proxy.serverUrl

* __Description:__ Defines the address of remote proxy server if a proxy server is using, e.g. `https://myhost.com`
* __Default value:__ **
* __Interface:__ *SamlConfig*    
Used in the Web Client.

### cuba.addon.saml.logAllSamlMessages

* __Description:__ Determines if all SAML messages should be logged
* __Default value:__ `true`
* __Interface:__ *SamlConfig*    
Used in the Web Client.
