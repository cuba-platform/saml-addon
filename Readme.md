# Overview

The purpose of the SAML Integration CUBA component is to provide a readily available instrument of authentication via a SAML Identity Provider service in any CUBA-based application.


# Getting Started

## Installing SAML Add-on

To use the add-on developer needs to install it to the their local repository. It is recommended to do it in Cuba Studio. To import the add-on click `Run` ->` Install app component`.

## Referencing the SAML Add-on in a CUBA.platform App

It is recommended to perform this operation in Cuba Studio. Open the project in Cuba Studio and go to editing its properties (`Project properties` ->` Edit`). Click `+` located next to the caption `Custom components`. The component should appear in the dropdown list (second field) with the name `saml-addon` (if you did install it earlier). Select the component, press `ok`, save the project settings. SAML Add-on is referenced.

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

## Minimal configuration

Before you start using the component with static IDP, you need to make some settings.

### Extension of the standard login window in the CUBA.platform application

First, you need to extend the standard Login screen. It is recommended to perform it in Cuba Studio. To do this open the project in Cuba Studio, select the tab `Generic UI` and press` New`. In the Templates list, select `Login window` and click `Create`.

Then add a button to log in via the IDP SAML server. By pressing this button the loginSsoCircle() method will be called - this is the entry point.

Here is an example of implementation of the whole controller:

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
                Page.getCurrent().setLocation(samlConfig.getSamlLoginUrl());
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
}
```

```
captions.loginBy = Login by
errors.message.samlLoginFailed = User '%s' hasn't been logged by SAML.
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
4. Fill the `Code` field - it will be internally
5. Upload the keystore file and fill `Key Store Login` and `Key Store Password` fields
6. Choose `Default access group` to be set to the new users logged with this IDP
7. Choose `Processing service` to process the new users logged with this IDP
8. Fill the `Service provider identity`. This field will be used by IDP, to identify the service provider. Example: `cuba-saml-demo`
    Then click `Refresh` button. Copy the generated XML from the field below and register it in the IDP.
9. Fill the `Provider metadata URL` provided by this IDP. Example: `http://idp.ssocircle.com/idp-meta.xml`
10. Click `Active` checkbox. After that the IDP will be shown on the login screen

### For static IDP (to be removed) 

#### Service Provider Id

Set an id for the CUBA.platform application. This id must be unique within the Identity Provider. This id is specified in web-app.properties:

```
cuba.addon.saml.spId = cuba-saml-demo
```

#### IDP Metadata

IDP provider metadata is generally supplied as an XML file and available on some IDP url. You must specify this URL in web-app.properties:

```
cuba.addon.saml.idp.metadataUrl = http://idp.ssocircle.com/idp-meta.xml
```

#### Keystore

You need to specify the Keystore's parameters in the web-app.properties:

```
cuba.addon.saml.keystore.path = classpath: com / company / samladdonreferenceproject / keys / samlKeystore.jks
cuba.addon.saml.keystore.login = apollo
cuba.addon.saml.keystore.password = nalle123
```

For a detailed parameters description, see Appendix A.

#### Generating the Service Provider Metadata

To register the Service Provider in the IDP server you must provide an XML description for the first. To do this you can use the SAML Add-on administrating screen.

Open the menu item `Administration` -> `SAML` ->` SP Metadata` in running CUBA.platform application with the connected add-on. This screen generates the Service Provider ID and the XML Description for the Service Provider to be provided to IDP.

#### Conclusion

After performing all the actions described above you will have the following result.
The SAML login screen will appear on the CUBA Application login screen. When you click the button you will be redirected to the IDP authorization page. After successful logging in you will be redirected back to the SP being already logged in in the SP.

## Absent users registration

By default, if there is no user in the CUBA.platform application the SP should register the new user. The SAML Add-on does not perform any actions on the user's creation. Developer of the CUBA.platform application needs to implement it manually (see the example of adding the login button via SAML to the login page). SAML-addon provides an interface that simplifies the process of finding a user and registering him in case he was not found.

### SamlProcessor

By default the component provides BaseSamlProcessor which populates to the new user only several attributes from the SAML session:
- FirstName
- LastName
- MiddleName
- EmailAddress

However, you can define your own implementation of the interface `com.haulmont.addon.saml.core.SamlProcessor` which will handle the SAML data using your own logic.
Please keep in mind, that `getName()` method should return a value matching SAMLConnection.code of any registered SAML Connection.

# Appendix A: Application Properties

## General Properties

### cuba.addon.saml.spId

* __Description:__ Sets the ServiceProvider ID, this identifier must be
unique within the IDP

* __Default value:__ *cuba-sp-identifier*

* __Type:__ Used in the Web Client

* __Interface:__ *SamlWebAppConfig*

### cuba.addon.saml.defaultGroupId

* __Description:__ The AccessGroup object identifier that is used for new user registration

* __Default value:__ *0fa2b1a5-1d68-4d69-9fbd-dff348347f93 (Company)*

* __Type:__ stored in the database

* __Interface:__ *SamlConfig*

### cuba.addon.saml.loginUrl

* __Description:__ URL for SAML login on Service Provider

* __Default value:__ *http://localhost:8080/app/saml/login*

* __Type:__ stored in the database

* __Interface:__ *SamlConfig*

### cuba.addon.saml.logoutUrl

* __Description:__ URL for SAML logout on the Service Provider

* __Default value:__ *http://localhost:8080/app/saml/logout*

* __Type:__ stored in the database

* __Interface:__ *SamlConfig*

### cuba.addon.saml.metadataUrl

* __Description:__ Service Provider SAML metadata URL

* __Default value:__ *http://localhost:8080/app/saml/metadata*

* __Type:__ stored in the database

* __Interface:__ *SamlConfig*

### cuba.addon.saml.keystore.path

* __Description:__ location of the keystore file for ServiceProvider.
The file keystore can be located in the classpath, for example:
`classpath: com / company / samladdonreferenceproject / keys / samlKeystore.jks`

* __Type:__ Used in the Web Client

* __Interface:__ *SamlWebAppConfig*

### cuba.addon.saml.keystore.login

* __Description:__ username for the keystore

* __Type:__ Used in the Web Client

* __Interface:__ *SamlWebAppConfig*

### cuba.addon.saml.keystore.password

* __Description:__ password for the keystore

* __Type:__ Used in the Web Client

* __Interface:__ *SamlWebAppConfig*

### cuba.addon.saml.ssoLogout

* __Description:__ Defines whether the logout action will be also performed on the IDP when user performs logout in the CUBA.platform application (SP)

* __Default value:__ *false*

* __Type:__ stored in the database

* __Interface:__ *SamlConfig*

### cuba.addon.saml.idp.metadataUrl

* __Description:__ The IDP Metadata location URL

* __Type:__ Used in the Web Client

* __Interface:__ *SamlWebAppConfig*
