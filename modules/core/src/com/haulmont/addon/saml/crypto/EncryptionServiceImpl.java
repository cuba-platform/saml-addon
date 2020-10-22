/*
 * Copyright (c) 2008-2019 Haulmont.
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

package com.haulmont.addon.saml.crypto;

import com.haulmont.addon.saml.crypto.config.EncryptionConfig;
import com.haulmont.addon.saml.entity.KeyStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Base64;
import java.util.UUID;


@Service(EncryptionService.NAME)
public class EncryptionServiceImpl implements EncryptionService {

    private static final Logger log = LoggerFactory.getLogger(EncryptionServiceImpl.class);

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    private final EncryptionConfig encryptorConfig;

    private SecretKey secretKey;

    private byte[] iv;

    @SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "CdiInjectionPointsInspection"})
    @Inject
    public EncryptionServiceImpl(EncryptionConfig encryptorConfig) {
        this.encryptorConfig = encryptorConfig;
    }

    @PostConstruct
    void initKey() {
        if (StringUtils.isBlank(encryptorConfig.getEncryptionKey())) {
            throw new IllegalStateException(String.format(
                    "Cannot configure encryptor %s, property \"encryption.key\" is not set",
                    getClass().getName()
            ));
        }
        byte[] encryptionKey = Base64.getDecoder().decode(encryptorConfig.getEncryptionKey());
        secretKey = new SecretKeySpec(encryptionKey, "AES");

        String encryptionIv = encryptorConfig.getEncryptionIv();
        if (StringUtils.isNotBlank(encryptionIv)) {
            iv = Base64.getDecoder().decode(encryptionIv);
        }

        log.info("EncryptionService has been initialised with key {} and init vector {}",
                encryptorConfig.getEncryptionKey(), encryptorConfig.getEncryptionIv()
        );
    }

    @Override
    public String getEncryptedPrivateKeyPassword(KeyStore keyStore) {
        if (keyStore.getPassword() == null) {
            return null;
        }
        log.debug("Encrypt password for keystore {}", keyStore);
        try {
            return encryptPassword(keyStore.getPassword());
        } catch (Exception e) {
            throw new RuntimeException("Can't encrypt password for keystore " + keyStore, e);
        }
    }

    private String encryptPassword(String password) throws GeneralSecurityException {
        byte[] encrypted = getCipher(Cipher.ENCRYPT_MODE)
                .doFinal(saltedPassword(password).getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String saltedPassword(String password) {
        String salt = UUID.randomUUID().toString();
        salt = salt.substring(0, 16);
        salt = StringUtils.rightPad(salt, 16);
        return salt + password;
    }

    @Override
    public String getPlainPrivateKeyPassword(KeyStore keyStore) {
        if (keyStore.getPassword() == null) {
            return null;
        }
        log.debug("Decrypt password for keystore {}", keyStore);
        try {
            return decryptPassword(keyStore.getPassword());
        } catch (Exception e) {
            throw new RuntimeException("Can't decrypt password for keystore " + keyStore, e);
        }
    }

    private String decryptPassword(String password) throws GeneralSecurityException {
        byte[] passwordBytes = Base64.getDecoder().decode(password);
        byte[] decrypted = getCipher(Cipher.DECRYPT_MODE).doFinal(passwordBytes);
        String saltedPassword = new String(decrypted, StandardCharsets.UTF_8);
        return saltedPassword.substring(16);
    }

    @Nullable
    @Override
    public String getEncryptedKeystorePassword(KeyStore keyStore) {
        if (keyStore.getKeystorePassword() == null) {
            return null;
        }
        log.debug("Encrypt keystore password for keystore {}", keyStore);
        try {
            return encryptPassword(keyStore.getKeystorePassword());
        } catch (Exception e) {
            throw new RuntimeException("Can't encrypt keystore password for keystore " + keyStore, e);
        }
    }

    @Nullable
    @Override
    public String getPlainKeystorePassword(KeyStore keyStore) {
        if (keyStore.getKeystorePassword() == null) {
            return null;
        }
        log.debug("Decrypt keystore password for keystore {}", keyStore);
        try {
            return decryptPassword(keyStore.getKeystorePassword());
        } catch (Exception e) {
            throw new RuntimeException("Can't decrypt keystore password for keystore " + keyStore, e);
        }
    }

    private Cipher getCipher(int mode) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        if (iv != null) {
            cipher.init(mode, secretKey, getAlgorithmParameterSpec());
        } else {
            cipher.init(mode, secretKey);
        }
        return cipher;
    }

    private AlgorithmParameterSpec getAlgorithmParameterSpec() {
        return new IvParameterSpec(iv, 0, iv.length);
    }

}
