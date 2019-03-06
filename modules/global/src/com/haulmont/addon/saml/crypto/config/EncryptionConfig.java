package com.haulmont.addon.saml.crypto.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;

@Source(type = SourceType.APP)
public interface EncryptionConfig extends Config {

    @Property("encryption.key")
    String getEncryptionKey();

    @Property("encryption.iv")
    String getEncryptionIv();
}
