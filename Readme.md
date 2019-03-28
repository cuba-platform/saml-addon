[![license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

- [1. Overview](#overview)
- [2. Installation](#installation)
  - [2.1. Adding the Add-on](#adding-the-addon)
  - [2.2. Setting Repositories](#setting-repositories)
- [3. Configuration](#configuration)
  - [3.1. Keystore](#keystore)
    - [3.1.1. Creating Keystore](#keystore-create)
  - [3.2. SAML Connection](#saml-connection)
  - [3.3. Tenant Login](#tenant-login)
  - [3.4. SAML Processor](#saml-processor)
- [4. Implementation](#implementation)
  - [4.1. Extension of the Standard Login Window](#extension-login-window)
- [5. General Application Properties](#general-properties)

# 1. Overview

The addon provides a readily available instrument of authentication via a SAML Identity Provider (IdP) service in any CUBA-based application wich is service provider (SP).

See [sample project](https://git.haulmont.com/app-components/saml-addon-demo), using this add-on.

# 2. Installation <a name="installation"></a>
The installation process consists of two steps: adding the component and specifying repositories.

## 2.1. Adding the Add-on <a name="adding-the-addon"></a>

To install the component in your project, do the following steps:

1. Open your application in CUBA Studio.

2. Open *Project -> Properties* in the project tree.

3. On the *App components* panel click the *Plus* button next to *Custom components*.

4. Paste the add-on coordinates in the corresponding field as follows: `group:name:version`.

  - Artifact group: `com.haulmont.addon.saml`
  - Artifact name: `saml-global`
  - Version: `add-on version`

Specify the add-on version compatible with the used version of the CUBA platform.

| Platform Version | Add-on Version |
|------------------|----------------|
|7.0.x             | 0.2-SNAPSHOT   |
|6.10.x            | 0.1-SNAPSHOT   |

5. Click **OK** to confirm and save project properties.

<!--After that the SAML SSO functions will be available.-->

To use your own key for keystore passwords encryption specify `encryption.key` and `encryption.iv` properties in `app.properties.xml` in the `core` module. Otherwise the default keys declared in the `app-component.xml` file will be used.

## 2.2. Setting Repositories <a name="setting-repositories"></a>

The add-on use references artifacts that are not available in `CUBA.platform` repo. To use the add-on you need to add the following repositories to the `build.gradle` file in the `buildscript -> repositories` section with the following repositories:

```groovy
maven {
    url 'https://repository.mulesoft.org/releases/'
}
maven {
    url 'https://artifacts.alfresco.com/nexus/content/repositories/public/'
}
maven {
    url 'https://build.shibboleth.net/nexus/content/repositories/releases/'
}
```
You can also do it in the *Properties* window:

1. Go to *Project -> Properties*.
2. On the *Repositories* panel click the *Plus* button and specify the URL of repositories.

# 3. Configuration <a name="configuration"></a>
# 3.1. Keystore <a name="keystore"></a>

Before setting SAML connection you need to create keystore containing a usename, password, description and JKS (Java Key Store) file. Your service provider application must have a unique public/private key pair.

### 3.1.1 Creating keystore <a name="keystore-create"></a>

Firstly, you need to generate a public/private key pair. Use the following links to instructions:

- [to generate private key](https://docs.spring.io/spring-security-saml/docs/1.0.4.RELEASE/reference/html/security.html#configuration-key-management-private-keys);
- [to generate public key](https://docs.spring.io/spring-security-saml/docs/1.0.4.RELEASE/reference/html/security.html#configuration-key-management-public-keys).

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
8. Fill in the *Service provider identity*. This field will be used by IdP to identify your application. For example: `cuba-saml-demo`. Then click *Refresh* button. Copy the generated XML from the field below and register it in the IdP.
9. Fill in the *Identity Provider metadata URL* field provided by this IdP. Example: `http://idp.ssocircle.com/idp-meta.xml`.
    Then click *Refresh* button. If the URL is correct and IdP works OK - you will see some XML content below.
    Another way to specify IdP metadata is to upload an XML file using the corresponding button.
10. Click *User Creation* checkbox, if you want to create user from information received from IdP in case user does not exist in the application.
11. Click *Active* checkbox. After that the IdP will be shown in the login screen.
12. Click *OK* to save settings.

### 3.3 Tenant Login <a name="tenant-login"></a>

The common example to login using IdP is to use the lookup field with list of IdP providers.
To simplify login, you can use a specific tenant URL. For example, `http://localhost:8080/app/ssocircle?`, where `ssocircle` is the `SSO Path` set while configuring SAML Connection. When you use such URL, the system automatically redirects you to the specific IdP.

### 3.4 SAML Processor <a name="saml-processor"></a>

By default, the component provides `BaseSamlProcessor` which fills in the following attributes for the new user from the SAML session:

- FirstName
- LastName
- MiddleName
- EmailAddress

However, you can define your own implementation of the interface `com.haulmont.addon.saml.core.SamlProcessor` which will handle the SAML data using your own logic.
The `getName()` method should return a user-friendly name, to show it in the lookup field on the *SAML Connection editor* screen.

# 4. Implementation <a name="implementation"></a>

## 4.1. Extension of the Standard Login Window <a name="extension-login-window"></a>

To extent the standard login screen:
1. Open your project in CUBA Studio.
2. Expand the *Generic UI* in the CUBA project tree.
3. Right-click *Screens* and go to *New -> Screen*.
4. Go to the *Legacy Screen Templates* tab and select *Login window*.
5. Click *Next -> Finish*.

Then add a lookup field with list of IdP providers in the screen controller. When you choose one of providers SAML request will be initiated.

Here is an example of implementation of the whole controller:

1. Screen controller `ext-loginWindow.xml`:

<details><summary>Click to expand the code</summary>

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<window xmlns="http://schemas.haulmont.com/cuba/window.xsd"
        class="com.company.sd.web.screens.ExtAppLoginWindow"
        extends="/com/haulmont/cuba/web/app/loginwindow/loginwindow.xml"
        xmlns:ext="http://schemas.haulmont.com/cuba/window-ext.xsd"
        messagesPack="com.company.sd.web.screens">
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

```xml
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

```xml
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

/**
 * @author kuchmin
 */
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

* __Description:__ Maximum difference between local time and time of the assertion creation which still allows message to be processed. Basically determines maximum difference between clocks of the IdP and SP machines. (in seconds)
* __Default value:__ `60`
* __Interface:__ *SamlConfig*    
Used in the Web Client.

### cuba.addon.saml.maxAuthenticationAgeSec

* __Description:__ Maximum time between users authentication and processing of the AuthNResponse message (in seconds)
* __Default value:__ `7200`
* __Interface:__ *SamlConfig*    
Used in the Web Client.

### cuba.addon.saml.maxAssertionTimeSec

* __Description:__ Maximum time between assertion creation and current time when the assertion is usable. (in seconds)
* __Default value:__ `3000`
* __Interface:__ *SamlConfig*    
Used in the Web Client.

### cuba.addon.saml.ssoLogout

* __Description:__ Defines whether the logout action will be also performed on the IdP when user performs logout in the CUBA.platform application (SP)
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
