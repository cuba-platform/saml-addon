/*
 * Copyright (c) 2008-2020 Haulmont.
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

package com.haulmont.addon.saml.role;

import com.haulmont.addon.saml.entity.KeyStore;
import com.haulmont.addon.saml.entity.SamlConnection;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.security.app.role.AnnotatedRoleDefinition;
import com.haulmont.cuba.security.app.role.annotation.EntityAccess;
import com.haulmont.cuba.security.app.role.annotation.EntityAttributeAccess;
import com.haulmont.cuba.security.app.role.annotation.Role;
import com.haulmont.cuba.security.app.role.annotation.ScreenAccess;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.cuba.security.entity.Group;
import com.haulmont.cuba.security.role.EntityAttributePermissionsContainer;
import com.haulmont.cuba.security.role.EntityPermissionsContainer;
import com.haulmont.cuba.security.role.ScreenPermissionsContainer;

import static com.haulmont.cuba.security.entity.EntityOp.*;

@Role(name = SamlAdminRole.ROLE_NAME)
public class SamlAdminRole extends AnnotatedRoleDefinition {

    public static final String ROLE_NAME = "saml-admin";

    @Override
    public String getLocName() {
        return "SAML admin";
    }

    @EntityAccess(entityClass = SamlConnection.class,
            operations = {CREATE, READ, UPDATE, DELETE})
    @EntityAccess(entityClass = KeyStore.class,
            operations = {CREATE, READ, UPDATE, DELETE})
    @EntityAccess(entityClass = FileDescriptor.class,
            operations = {CREATE, READ, UPDATE, DELETE})
    @EntityAccess(entityClass = Group.class,
            operations = {READ})
    @Override
    public EntityPermissionsContainer entityPermissions() {
        return super.entityPermissions();
    }

    @EntityAttributeAccess(entityClass = SamlConnection.class, modify = "*")
    @EntityAttributeAccess(entityClass = KeyStore.class, modify = "*")
    @EntityAttributeAccess(entityClass = FileDescriptor.class, modify = "*")
    @EntityAttributeAccess(entityClass = Group.class, modify = "*")
    @Override
    public EntityAttributePermissionsContainer entityAttributePermissions() {
        return super.entityAttributePermissions();
    }

    @ScreenAccess(screenIds = {"administration",
            "samladdon$SamlConnection.browse",
            "samladdon$SamlConnection.edit",
            "samladdon$KeyStore.browse",
            "samladdon$KeyStore.edit"})
    @Override
    public ScreenPermissionsContainer screenPermissions() {
        return super.screenPermissions();
    }
}
