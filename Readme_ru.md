# Overview

The purpose of the SAML Integration CUBA component is to provide a readily
available instrument of authentication via a SAML Identity Provider service
in any CUBA-based application.


# Getting Started

## Установка SAML Add-on

Для того, чтобы у пользователя появилась возможность использовать add-on в CUBA.platform приложении
необходимо произвести установку add-on в локальный репозиторий пользователя. Рекомендуется делать эту
процедуру в Cuba Studio, для этого импортируйте add-on в Cuba Studio и выполните `Run` -> `Install app component`.

## Подключение SAML Add-on в CUBA.platform App

Данную операцию рекомендуется выполнять в Cuba Studio. Откройте проект в Cuba Studio и
перейдтите к редактированию его своств (`Project properties` -> `Edit`). Нажмите на `+` расположенный
рядом с надписью `Custom components`. В выподающем спике (второе поле) должен быть доступен компонент
с именем `saml-addon` (в случае если вы выполнили его установку ранее). Выберите компонент, нажмите `ok`,
сохраните настройки проекта. SAML Add-on подключен.

### Добавление Maven репозиториев в проект

Компонент в качестве зависиомстей использует артифакты, которые не доступны в репозитории CUBA.platform.
Соответсвенно, для работоспособности компонента необходимо подключить дополнительные репозитории. Для этого
в файл `build.gradle`, в секцию `buildscript` -> `repositories` необходимо добавить следующие репозитории:

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
 
## Минимальная конфигурация

Перед тем как начать пользоваться компонентом необходимо произвести некоторые настройки.

### Определение Service Provider Id

Указать id для данного CUBA.platform application. Данный id должен быть уникальным в рамках
предпологаемого Identity Provider. Данный id указывается в web-app.properties:

```
cuba.addon.saml.spId = cuba-saml-demo
``` 

### Определение IDP Metadata 

Описание IDP провайдера в общем случае поставлется в виде XML описания и доступно для загрзуки
из IDP по некоторому URL. Данный URL необходимо указать в web-app.properties:

```
cuba.addon.saml.idp.metadataUrl = http://idp.ssocircle.com/idp-meta.xml
```

### Настройка Keystore

Каждый Service Provider должен, в рамках протокола SAML, обладать уникальной свзякой секретный/публичный
ключ. Для генерации соответствующей пары можно восопльзоваться инструкцией:

- https://docs.spring.io/spring-security-saml/docs/1.0.4.RELEASE/reference/html/security.html#configuration-key-management-private-keys
- https://docs.spring.io/spring-security-saml/docs/1.0.4.RELEASE/reference/html/security.html#configuration-key-management-public-keys

Далее, необходимо указать параметры Keystore в в web-app.properties:

```
cuba.addon.saml.keystore.path = classpath:com/company/samladdonreferenceproject/keys/samlKeystore.jks
cuba.addon.saml.keystore.login = apollo
cuba.addon.saml.keystore.password = nalle123
```

Подробное описание параметров смотрите в Appendix A.

### Получение Service Provider Metadata

Для регистрации Service Provider на сервере IDP необходимо получить XML описание для первого.
Для этого пользователь может воспользоваться специальным административным экраном. Для этого
необходимо в запущенном CUBA.platform application с подключенным add-on открыть меню
`Администрирование` -> `SAML` -> `SP Metadata`. На данном экране пользователю доступны
Service Provider ID и XML описание Service Provider

### Расширение стандартного окна логина в CUBA.platform application

Для начала, необходимо выполнить расширение стандартного экрана Login. Данную операцию
рекомендуется выполнять в Cuba Studio. Для этого откройте проект в Cuba Studio, выберете вкладку
`Generic UI` и нажмите `New`. В списке Templates выберете `Login window` и нажмите `Create`.

Далее добавьте кнопку для входа в систему через IDP SAML сервер. По нажатию этой кнопки
будет вызываться метод loginSsoCircle() - наша точка входа.

В данном случае мы приведем реализацию для контроллера целиком:

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

    protected final Logger log = LoggerFactory.getLogger(ExtAppLoginWindow.class);

    @Inject
    protected SamlRegistrationService samlRegistrationService;

    protected RequestHandler samlCallbackRequestHandler =
            this::handleSamlCallBackRequest;

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

Уточнить подробности реализации можно в соответствующем demo проекте.

### Заключение

По окончании указанных выше монипуляций при нажатии на ранее добавленную кнопку,
на странице логина CUBA.platform application, будет осуществлен переход на страницу
авторизации IDP сервера. После успешный авторизации управление будет переданно обратно
на CUBA.platform application. В случае если авторизация на IDP была успешной, то пользователь
будет авторизован.

## Регистрация пользователя в случае его отсутствия

По умолчанию, в случае отсутствия пользователя в CUBA.platform приложении выполняется его
регистрация. saml-addon не выполняет никаких действий по логину пользователя или его созданию,
разработчику CUBA.platform приложения необходимо делать это самостоятельно. См. пример добавляющий
кнопку входа через SAML на страницу логина. В свою очередь, saml-addon предоставляет сервис позволяющий
упростить процесс поиска пользователя и его регистрацию в случае если он небыл найден.

### SamlRegistrationService

Данный сервис предоставлет методы поиска и регистрации пользователей в случае если
они отсутствуют в системе. В реализации по умолчанию, при регистрации нового пользователя используются
следующие атрибуты, в случае если они присутствуют в ответе:

- FirstName - имя пользователя
- LastName - фамилия пользователя
- ParticipantName - имя которое передается SAML IDP как participant (в случае с https://www.ssocircle.com/en/
это email пользователя)

В случае, если раработчику необходимо изменить количество атрибутов используемых при регистрации
пользователя он может расширить сервис и переопределить его методы, согласно документации CUBA.platform

# Appendix A: Application Properties

## General Properties

### cuba.addon.saml.spId

* __Description:__ Устанавливает ServiceProvider ID, данный идентификатор должен быть
уникальным в рамках подключаемого IDP

* __Default value:__ *cuba-sp-identifier*

* __Type:__ Used in the Web Client

* __Interface:__ *SamlWebAppConfig*

### cuba.addon.saml.defaultGroupId

* __Description:__ Идентификатор объекта AccessGroup который используется при
регистрации нового пользователя

* __Default value:__ *0fa2b1a5-1d68-4d69-9fbd-dff348347f93 (Company)*

* __Type:__ stored in the database

* __Interface:__ *SamlConfig*
 
### cuba.addon.saml.loginUrl

* __Description:__ URL для SAML login на Service Provider

* __Default value:__ *http://localhost:8080/app/saml/login*

* __Type:__ stored in the database

* __Interface:__ *SamlConfig*

### cuba.addon.saml.logoutUrl

* __Description:__ URL для SAML logout на Service Provider

* __Default value:__ *http://localhost:8080/app/saml/logout*

* __Type:__ stored in the database

* __Interface:__ *SamlConfig*

### cuba.addon.saml.metadataUrl

* __Description:__ URL для SAML metadata на Service Provider

* __Default value:__ *http://localhost:8080/app/saml/metadata*

* __Type:__ stored in the database

* __Interface:__ *SamlConfig*

### cuba.addon.saml.keystore.path

* __Description:__ расположение файла keystore для ServiceProvider.
Файл keystore может быть расположен в classpath, например:
`classpath:com/company/samladdonreferenceproject/keys/samlKeystore.jks`

* __Type:__ Used in the Web Client

* __Interface:__ *SamlWebAppConfig*

### cuba.addon.saml.keystore.login

* __Description:__ username для используемого keystore

* __Type:__ Used in the Web Client

* __Interface:__ *SamlWebAppConfig*

### cuba.addon.saml.keystore.password

* __Description:__ password для используемого keystore

* __Type:__ Used in the Web Client

* __Interface:__ *SamlWebAppConfig*

### cuba.addon.saml.ssoLogout

* __Description:__ Определяется будет ли выполняться logout на SAML IDP в ситуации
 когда пользователя выполняет logout в CUBA.platform приложении

* __Default value:__ *false*

* __Type:__ stored in the database

* __Interface:__ *SamlConfig*

### cuba.addon.saml.idp.metadataUrl

* __Description:__ URL по которому расположен IDP Metadata

* __Type:__ Used in the Web Client

* __Interface:__ *SamlWebAppConfig*
