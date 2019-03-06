package com.haulmont.addon.saml.crypto;

import com.haulmont.addon.saml.crypto.config.EncryptionConfig;
import com.haulmont.addon.saml.entity.KeyStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

    private final static Logger log = LoggerFactory.getLogger(EncryptionServiceImpl.class);

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
    public String getEncryptedPassword(KeyStore keyStore) {
        if (keyStore.getPassword() == null) {
            return null;
        }
        log.debug("Encrypt password for keystore {}", keyStore);
        try {
            byte[] encrypted = getCipher(Cipher.ENCRYPT_MODE)
                    .doFinal(saltedPassword(keyStore).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Can't encrypt password for keystore " + keyStore, e);
        }
    }

    private String saltedPassword(KeyStore keyStore) {
        String password = keyStore.getPassword();
        String salt = UUID.randomUUID().toString();
        salt = salt.substring(0, 16);
        salt = StringUtils.rightPad(salt, 16);
        return salt + password;
    }

    @Override
    public String getPlainPassword(KeyStore keyStore) {
        if (keyStore.getPassword() == null) {
            return null;
        }
        log.debug("Decrypt password for keystore {}", keyStore);
        try {
            byte[] password = Base64.getDecoder().decode(keyStore.getPassword());
            byte[] decrypted = getCipher(Cipher.DECRYPT_MODE).doFinal(password);
            String saltedPassword = new String(decrypted, StandardCharsets.UTF_8);
            return saltedPassword.substring(16);
        } catch (Exception e) {
            throw new RuntimeException("Can't decrypt password for keystore " + keyStore, e);
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
