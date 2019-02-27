package com.haulmont.addon.saml.listener;

import com.haulmont.addon.saml.crypto.EncryptionService;
import com.haulmont.addon.saml.entity.KeyStore;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.PersistenceTools;
import com.haulmont.cuba.core.listener.BeforeInsertEntityListener;
import com.haulmont.cuba.core.listener.BeforeUpdateEntityListener;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class KeystoreListener implements BeforeInsertEntityListener<KeyStore>, BeforeUpdateEntityListener<KeyStore> {

    private final EncryptionService encryptionService;
    private final PersistenceTools persistenceTools;

    @Inject
    public KeystoreListener(EncryptionService encryptionService, PersistenceTools persistenceTools) {
        this.encryptionService = encryptionService;
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
        String encryptedPassword = encryptionService.getEncryptedPassword(entity);
        entity.setPassword(encryptedPassword);
    }
}
