package com.haulmont.addon.saml.listener;

import com.haulmont.addon.saml.crypto.Encryptor;
import com.haulmont.addon.saml.entity.KeyStore;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.PersistenceTools;
import com.haulmont.cuba.core.listener.BeforeInsertEntityListener;
import com.haulmont.cuba.core.listener.BeforeUpdateEntityListener;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class KeystoreListener implements BeforeInsertEntityListener<KeyStore>, BeforeUpdateEntityListener<KeyStore> {

    private final Encryptor encryptor;
    private final PersistenceTools persistenceTools;

    @Inject
    public KeystoreListener(Encryptor encryptor, PersistenceTools persistenceTools) {
        this.encryptor = encryptor;
        this.persistenceTools = persistenceTools;
    }

    @Override
    public void onBeforeUpdate(KeyStore entity, EntityManager entityManager) {
        if (persistenceTools.isDirty(entity, "password")) {
            setEncryptedPassword(entity);
        }
    }

    @Override
    public void onBeforeInsert(KeyStore entity, EntityManager entityManager) {
        setEncryptedPassword(entity);
    }

    private void setEncryptedPassword(KeyStore entity) {
        String encryptedPassword = encryptor.getEncryptedPassword(entity);
        entity.setPassword(encryptedPassword);
    }
}
