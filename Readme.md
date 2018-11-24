# Overview

The purpose of the SAML Integration CUBA component is to provide a readily available instrument of authentication via a SAML Identity Provider service in any CUBA-based application.

# Getting Started

## Installing SAML Add-on

To use the add-on developer needs to install it to the their local repository. It is recommended to do it in Cuba Studio. To import the add-on click `Run` ->` Install app component`.

## Referencing the SAML Add-on in a CUBA.platform App

It is recommended to perform this operation in Cuba Studio. Open the project in Cuba Studio and go to editing its properties (`Project properties` ->` Edit`). Click `+` located next to the caption `Custom components`. The component should appear in the dropdown list (second field) with the name `saml-addon` (if you did install it earlier). Select the component, press `ok`, save the project settings. SAML Add-on is referenced.

## Minimal configuration

Before you start using the component in your project, you need to make some changes in your project.

### Adding Maven repos to a project

The Add-on references artifacts that are not available in CUBA.platform repo. To use the Add-on you need to add the following repos to the `build.gradle` file (section `buildscript` -> `repositories`):

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

### Extension of the standard login window in the CUBA.platform application

First, you need to extend the standard Login screen. It is recommended to perform it in Cuba Studio. To do this open the project in Cuba Studio, select the tab `Generic UI` and press` New`. In the Templates list, select `Login window` and click `Create`.

Then add a lookup field with list of IDP providers. When you choose one of the providers - SAML request is initiated.

Here is an example of implementation of the whole controller for CUBA 6.9 
(in CUBA 6.10 the login screen has been slightly changed, so you need to make small changes in the screen layout):

___ext-loginWindow.xml___
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
___ExtAppLoginWindow.java___
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
            final SamlSession samlSession = ((SamlSessionPrincipal) principal).getSamlSession();
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
___messages.properties___
```
captions.loginBy = Login by
errors.message.samlLoginFailed = User '%s' hasn't been logged by SAML.
```
```
___web-app.properties___
```
###############################################################################
#                                  SAML Configuration                         #
###############################################################################

cuba.addon.saml.basePath = /saml
cuba.addon.saml.logoutPath = /logout
cuba.addon.saml.loginPath = /login
cuba.addon.saml.metadataPath = /metadata
```

You can observe the details of the implementation in the corresponding demo project.

### Keystore

Each Service Provider must have a unique secret/public connection key. To generate the corresponding pair, you can use the following instruction:

- https://docs.spring.io/spring-security-saml/docs/1.0.4.RELEASE/reference/html/security.html#configuration-key-management-private-keys
- https://docs.spring.io/spring-security-saml/docs/1.0.4.RELEASE/reference/html/security.html#configuration-key-management-public-keys

## Register IDP

1. Navigate to `Administration` -> `SAML`
2. Click `Create` button
3. Fill the `Name` field - it will be shown to users on the login screen
4. Fill the `Code` field - it will be used for tenant login
5. Upload the keystore file (generated in the previous section) and fill `Key Store Login` and `Key Store Password` fields
6. Choose `Default access group` to be set to the new users logged with this IDP
7. Choose `Processing service` to process the new users logged with this IDP
8. Fill the `Service provider identity`. This field will be used by IDP, to identify the service provider. Example: `cuba-saml-demo`
    Then click `Refresh` button. Copy the generated XML from the field below and register it in the IDP.
9. Fill the `Provider metadata URL` provided by this IDP. Example: `http://idp.ssocircle.com/idp-meta.xml`
    Then click `Refresh` button. If the URL is correct and IDP works fine - you will see some XML content below.
    Another way to specify IDP metadata is to upload a xml file directly to application by drop it to IDP metadata text area.
10. Click `Active` checkbox. After that the IDP will be shown on the login screen

## Tenant login

By default, the example shows the lookup field with list of IDP providers.
To simplify login, you can use a specific tenant URL. When a user uses such URL, the system automatically redirects you to the specific IDP.
Example: `http://localhost:8080/app/ssocircle/`. Here `ssocircle` is the SAMLConnection.code. 

## Absent users registration

By default, if there is no user in the CUBA.platform application the SP should register the new user. The SAML Add-on provides only straightforward logic of the new user creation, defined in BaseSamlProcessor.

### SamlProcessor

By default the component provides BaseSamlProcessor which populates to the new user only several attributes from the SAML session:
- FirstName
- LastName
- MiddleName
- EmailAddress

However, you can define your own implementation of the interface `com.haulmont.addon.saml.core.SamlProcessor` which will handle the SAML data using your own logic.
The `getName()` method should return a user-friendly name, to show in the lookup field on the SAMLCOnnection.edit screen.

# Appendix A: Application Properties

## General Properties

### cuba.addon.saml.basePath

* __Description:__ URL SAML context path, e.g. "/saml"

* __Type:__ used in the Web Client

* __Interface:__ *SamlConfig*

### cuba.addon.saml.loginPath

* __Description:__ SAML login path part, e.g. "/login", and with the base path the result will be "/saml/logout".

* __Type:__ used in the Web Client

* __Interface:__ *SamlConfig*

### cuba.addon.saml.logoutPath

* __Description:__ SAML logout path part, e.g. "/logout" and with the base path the result will be "/saml/logout".

* __Type:__ used in the Web Client

* __Interface:__ *SamlConfig*

### cuba.addon.saml.metadataPath

* __Description:__ SAML metadata display path part, e.g. "/metadata" and with the base path the result will be "/saml/metadata?tenant=code" where code is SAMLConnection.code.

* __Type:__ used in the Web Client

* __Interface:__ *SamlConfig*


### cuba.addon.saml.ssoLogout

* __Description:__ Defines whether the logout action will be also performed on the IDP when user performs logout in the CUBA.platform application (SP)

* __Default value:__ *false*

* __Type:__ used in the Web Client

* __Interface:__ *SamlConfig*


### cuba.addon.saml.proxy.enabled

* __Description:__ Defines is a application use a proxy server or not.

* __Default value:__ *false*

* __Type:__ used in the Web Client

* __Interface:__ *SamlConfig*


### cuba.addon.saml.proxy.serverUrl

* __Description:__ Defines the address of remote proxy server if a proxy server is using, e.g. "https://myhost.com".

* __Default value:__ **

* __Type:__ used in the Web Client

* __Interface:__ *SamlConfig*
