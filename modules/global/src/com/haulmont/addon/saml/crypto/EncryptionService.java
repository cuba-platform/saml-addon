package com.haulmont.addon.saml.crypto;


import com.haulmont.addon.saml.entity.KeyStore;

public interface EncryptionService {
    String NAME = "saml_EnctyptionService";

    String getEncryptedPassword(KeyStore keyStore);

    String getPlainPassword(KeyStore keyStore);
}
