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

package com.haulmont.addon.saml.core;

import com.haulmont.addon.saml.entity.SamlConnection;
import com.haulmont.addon.saml.security.SamlSession;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.TypedQuery;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.security.entity.User;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Objects;

/**
 * Base implementation of processing SAML session
 *
 * @author adiatullin
 */
@Component
public class BaseSamlProcessor implements SamlProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSamlProcessor.class);

    @Inject
    protected Messages messages;
    @Inject
    protected Persistence persistence;
    @Inject
    protected Metadata metadata;

    @Override
    public String getName() {
        return getMessage("baseSamlConnectionProcessor.name");
    }

    /**
     * Method return null if user does not exist in the system and user creation flag is disabled
     * Return existed user information if user present in the system
     * Create and return new user if user is not present in the system and user creation flag is enabled
     *
     * @param samlSession SAML sessions
     * @param connection  current session SAML connection
     * @return
     */
    @Nullable
    @Override
    public User findOrRegisterUser(SamlSession samlSession, SamlConnection connection) {
        EntityManager em = persistence.getEntityManager();

        User existing = getExistingUser(samlSession, connection);
        if (existing != null) {
            return existing;
        }

        if (connection.getCreateUsers()) {
            // Register new user
            User user = metadata.create(User.class);
            user.setGroup(connection.getDefaultGroup());
            user.setActive(true);

            populateLogin(user, samlSession, connection);
            populateEmail(user, samlSession, connection);
            populateFirstName(user, samlSession, connection);
            populateMiddleName(user, samlSession, connection);
            populateLastName(user, samlSession, connection);
            populateDetails(user, samlSession, connection);

            em.persist(user);

            return user;
        } else {
            LOGGER.debug("User Creation is disabled for current SAML connection '{}'", connection.getName());
            LOGGER.debug("User does not exist in the system");
            return null;
        }

    }

    protected void populateLogin(User user, SamlSession samlSession, SamlConnection connection) {
        String login = samlSession.getCredential().getAttributeAsString(getLoginAttributeName(samlSession, connection));
        if (StringUtils.isEmpty(login)) {
            login = samlSession.getPrincipal();
        }
        user.setLogin(login);
    }

    protected String getLoginAttributeName(SamlSession samlSession, SamlConnection connection) {
        return "UserID";
    }

    protected void populateEmail(User user, SamlSession samlSession, SamlConnection connection) {
        String email = samlSession.getCredential().getAttributeAsString(getEmailAddressAttributeName(samlSession, connection));
        if (StringUtils.isEmpty(email)) {
            email = samlSession.getPrincipal();
        }
        user.setEmail(email);
    }

    protected String getEmailAddressAttributeName(SamlSession samlSession, SamlConnection connection) {
        return "EmailAddress";
    }

    protected void populateFirstName(User user, SamlSession samlSession, SamlConnection connection) {
        String firstName = samlSession.getCredential().getAttributeAsString(getFirstNameAttributeName(samlSession, connection));
        user.setFirstName(firstName);
    }

    protected String getFirstNameAttributeName(SamlSession samlSession, SamlConnection connection) {
        return "FirstName";
    }

    protected void populateMiddleName(User user, SamlSession samlSession, SamlConnection connection) {
        String middleName = samlSession.getCredential().getAttributeAsString(getMiddleNameAttributeName(samlSession, connection));
        user.setMiddleName(middleName);
    }

    protected String getMiddleNameAttributeName(SamlSession samlSession, SamlConnection connection) {
        return "MiddleName";
    }

    protected void populateLastName(User user, SamlSession samlSession, SamlConnection connection) {
        String lastName = samlSession.getCredential().getAttributeAsString(getLastNameAttributeName(samlSession, connection));
        user.setLastName(lastName);
    }

    protected String getLastNameAttributeName(SamlSession samlSession, SamlConnection connection) {
        return "LastName";
    }

    protected void populateDetails(User user, SamlSession samlSession, SamlConnection connection) {
        String name = null;

        String firstName = user.getFirstName();
        String lastName = user.getLastName();

        if (!StringUtils.isEmpty(firstName) && !StringUtils.isEmpty(lastName)) {
            name = firstName + " " + lastName;
        } else if (!StringUtils.isEmpty(firstName)) {
            name = firstName;
        } else if (!StringUtils.isEmpty(lastName)) {
            name = lastName;
        }

        user.setName(name);
    }

    @Nullable
    protected User getExistingUser(SamlSession samlSession, SamlConnection connection) {
        EntityManager em = persistence.getEntityManager();

        String email = samlSession.getCredential().getAttributeAsString(getEmailAddressAttributeName(samlSession, connection));
        if (StringUtils.isEmpty(email)) {
            email = samlSession.getPrincipal();
        }

        TypedQuery<User> query = em.createQuery("select u from sec$User u where u.email = :email", User.class);
        query.setParameter("email", email);
        query.setViewName(View.LOCAL);

        return query.getFirstResult();
    }

    /**
     * Get deep search localized message
     *
     * @param messageKey message key
     * @return localized message
     */
    protected String getMessage(String messageKey) {
        Class clazz = getClass();
        String message = messages.getMessage(clazz.getPackage().getName(), messageKey);
        while (Objects.equals(messageKey, message)) {
            clazz = clazz.getSuperclass();
            if (clazz == null) return messageKey;
            message = messages.getMessage(clazz.getPackage().getName(), messageKey);
        }
        return message;
    }
}
