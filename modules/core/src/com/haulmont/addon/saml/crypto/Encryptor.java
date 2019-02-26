package com.haulmont.addon.saml.crypto;


import com.haulmont.addon.saml.entity.KeyStore;

public interface Encryptor {

    String getEncryptedPassword(KeyStore keyStore);

    String getPlainPassword(KeyStore keyStore);
}
