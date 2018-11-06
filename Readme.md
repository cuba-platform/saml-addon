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

Before you start using the component, you need to make some settings.

### Service Provider Id

Set an id for the CUBA.platform application. This id must be unique within the Identity Provider. This id is specified in web-app.properties:

```
cuba.addon.saml.spId = cuba-saml-demo
```

### IDP Metadata

IDP provider metadata is generally supplied as an XML file and available on some IDP url. You must specify this URL in web-app.properties:

```
cuba.addon.saml.idp.metadataUrl = http://idp.ssocircle.com/idp-meta.xml
```

### Keystore

Each Service Provider must have a unique secret/public connection key. To generate the corresponding pair, you can use the following instruction:

- https://docs.spring.io/spring-security-saml/docs/1.0.4.RELEASE/reference/html/security.html#configuration-key-management-private-keys
- https://docs.spring.io/spring-security-saml/docs/1.0.4.RELEASE/reference/html/security.html#configuration-key-management-public-keys

Then you need to specify the Keystore's parameters in the web-app.properties:

```
cuba.addon.saml.keystore.path = classpath: com / company / samladdonreferenceproject / keys / samlKeystore.jks
cuba.addon.saml.keystore.login = apollo
cuba.addon.saml.keystore.password = nalle123
```

For a detailed parameters description, see Appendix A.

### Generating the Service Provider Metadata

To register the Service Provider in the IDP server you must provide an XML description for the first. To do this you can use the SAML Add-on administrating screen.

Open the menu item `Administration` -> `SAML` ->` SP Metadata` in running CUBA.platform application with the connected add-on. This screen generates the Service Provider ID and the XML Description for the Service Provider to be provided to IDP.


### Extension of the standard login window in the CUBA.platform application

First, you need to extend the standard Login screen. It is recommended to perform it in Cuba Studio. To do this open the project in Cuba Studio, select the tab `Generic UI` and press` New`. In the Templates list, select `Login window` and click `Create`.

Then add a button to log in via the IDP SAML server. By pressing this button the loginSsoCircle() method will be called - this is the entry point.

Here is an example of implementation of the whole controller:

```java
package com.company.samladdonreferenceproject.web.login;

import com.haulmont.addon.saml.security.SamlSession;
import com.haulmont.addon.saml.security.config.SamlConfig;
import com.haulmont.addon.saml.service.SamlRegistrationService;
import com.haulmont.addon.saml.web.security.saml.SamlSessionPrincipal;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.UIAccessor;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.LoginException;
import com.haulmont.cuba.web.app.loginwindow.AppLoginWindow;
import com.haulmont.cuba.web.controllers.ControllerUtils;
import com.haulmont.cuba.web.security.ExternalUserCredentials;
import com.vaadin.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.Map;

import static java.lang.String.format;

public class ExtAppLoginWindow extends AppLoginWindow {

    protected final Logger log = LoggerFactory.getLogger (ExtAppLoginWindow.class);

    @Inject
    protected SamlRegistrationService samlRegistrationService;

    protected RequestHandler samlCallbackRequestHandler =
            this :: handleSamlCallBackRequest;

    @Inject
    protected BackgroundWorker backgroundWorker;

    @Inject
    private SamlConfig samlConfig;

    protected URI redirectUri;

    protected UIAccessor uiAccessor;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        this.uiAccessor = backgroundWorker.getUIAccessor();
    }

    public void loginSsoCircle() {
        VaadinSession.getCurrent().addRequestHandler(samlCallbackRequestHandler);

        this.redirectUri = Page.getCurrent().getLocation();

        Page.getCurrent().setLocation(samlConfig.getSamlLoginUrl());
    }

    public boolean handleSamlCallBackRequest(VaadinSession session, VaadinRequest request,
                                             VaadinResponse response) throws IOException {
        Principal principal = VaadinService.getCurrentRequest().getUserPrincipal();
        if (principal instanceof SamlSessionPrincipal) {
            SamlSession samlSession = ((SamlSessionPrincipal) principal).getSamlSession();

            uiAccessor.accessSynchronously(() -> {
                try {
                    User user = samlRegistrationService.findOrRegisterUser(samlSession);

                    ExternalUserCredentials credentials =
                            new ExternalUserCredentials(user.getLogin());

                    app.getConnection().login(credentials);
                } catch (LoginException e) {
                    showLoginException(format("User hasn't been logged by SAML (user: %s)",
                            samlSession.getPrincipal()));
                } finally {
                    session.removeRequestHandler(samlCallbackRequestHandler);
                }
            });

            ((VaadinServletResponse) response).getHttpServletResponse().
                    sendRedirect(ControllerUtils.getLocationWithoutParams(redirectUri));

            return false;
        }

        return false;
    }
}
```

You can observe the details of the implementation in the corresponding demo project.

### Conclusion

After performing all the actions described above you will have the following result.
The SAML login screen will appear on the CUBA Application login screen. When you click the button you will be redirected to the IDP authorization page. After successful logging in you will be redirected back to the SP being already logged in in the SP.

## Absent users registration

By default, if there is no user in the CUBA.platform application the SP should register the new user. The SAML Add-on does not perform any actions on the user's creation. Developer of the CUBA.platform application needs to implement it manually (see the example of adding the login button via SAML to the login page). SAML-addon provides a service that simplifies the process of finding a user and registering him in case he was not found.

### SamlRegistrationService

This service provides methods for searching and registering users in case they are absent in the system. In the default implementation when a new user is registered the following attributes will be user, if they are present in the response:

- FirstName
- LastName
- ParticipantName - the name that SAML IDP provides as participant's ID (in the case of https://www.ssocircle.com/en/ this is the email of the user)

In case if the developer needs to change the attributes used for user registration they can extend the service and override his methods according to the documentation of CUBA.platform.

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
