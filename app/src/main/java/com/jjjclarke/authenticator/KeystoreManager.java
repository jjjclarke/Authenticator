package com.jjjclarke.authenticator;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class KeystoreManager {

    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "totp_master_key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LEN = 128;

    public static void init() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);

        // Check if key already exists
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKey();
        }
    }

    private static void generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);

        KeyGenParameterSpec keySpec = new KeyGenParameterSpec.Builder
                (KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .setRandomizedEncryptionRequired(true) // Important for GCM
                .build();

        keyGenerator.init(keySpec);
        keyGenerator.generateKey();
    }

    public static String encryptSecret(String plaintext) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);

        SecretKey sk = (SecretKey) keyStore.getKey(KEY_ALIAS, null);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, sk);

        // Encrypt the secret key
        byte[] plain = plaintext.getBytes(StandardCharsets.UTF_8);
        byte[] ciphertext = cipher.doFinal(plain);

        byte[] iv = cipher.getIV();

        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

        return Base64.encodeToString(combined, Base64.DEFAULT); // Return as Base64 for SQLite storage later
    }

    public static String decryptSecret(String encryptedBase64) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);

        SecretKey key = (SecretKey) keyStore.getKey(KEY_ALIAS, null);

        // Decode from Base64
        byte[] combined = Base64.decode(encryptedBase64, Base64.DEFAULT);

        // Extract IV (first 12 bytes for GCM)
        byte[] iv = new byte[12];
        System.arraycopy(combined, 0, iv, 0, 12);

        // Extract ciphertext (everything after IV)
        byte[] ciphertext = new byte[combined.length - 12];
        System.arraycopy(combined, 12, ciphertext, 0, ciphertext.length);

        // Decrypt
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LEN, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] plainBytes = cipher.doFinal(ciphertext);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }
}
