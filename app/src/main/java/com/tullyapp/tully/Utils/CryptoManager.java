package com.tullyapp.tully.Utils;

import android.os.Build;
import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class CryptoManager {
    protected static final String UTF8 = "utf-8";
    private static final String ALGORITHM = "PBEWithMD5AndDES";
    private static String PASSWORD;
    private static SecretKey secretKey;
    private static PBEParameterSpec PBESpec;

    public CryptoManager(String password) {
        PASSWORD = password;
    }

    private SecretKey getSecretKey(String passphraseOrPin) {
        if (secretKey == null)
            secretKey = generateSecretKey(passphraseOrPin);
        return secretKey;
    }

    private PBEParameterSpec getPBESpec() {
        if (PBESpec == null)
            PBESpec = generatePBESpec();
        return PBESpec;
    }

    /**
     * @param value string to encrypt
     * @return encrypted string using secret key
     */
    public String encrypt(String value) {
        try {
            final byte[] bytes = value != null ? value.getBytes(UTF8) : new byte[0];
            Cipher pbeCipher = Cipher.getInstance(ALGORITHM);
            pbeCipher.init(Cipher.ENCRYPT_MODE, getSecretKey(PASSWORD), getPBESpec());
            byte[] encrypted = Base64.encode(pbeCipher.doFinal(bytes), Base64.NO_WRAP);
            return new String(encrypted, UTF8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param value string to decrypt
     * @return decrypted string using secret key
     */
    public String decrypt(String value) {
        try {
            final byte[] bytes = value != null ? Base64.decode(value, Base64.DEFAULT) : new byte[0];
            Cipher pbeCipher = Cipher.getInstance(ALGORITHM);
            pbeCipher.init(Cipher.DECRYPT_MODE, getSecretKey(PASSWORD), getPBESpec());
            return new String(pbeCipher.doFinal(bytes), UTF8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SecretKey generateSecretKey(String passphraseOrPin) {
        // Number of PBKDF2 hardening rounds to use. Larger values increase
        // computation time.
        final int iterations = 128;

        // Generate a 64-bit key
        final int outputKeyLength = 64;

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            KeySpec keySpec = new PBEKeySpec(passphraseOrPin.toCharArray(), getUniquePseudoDeviceID().getBytes(UTF8), iterations, outputKeyLength);
            return factory.generateSecret(keySpec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PBEParameterSpec generatePBESpec() {
        try {
            return new PBEParameterSpec(getUniquePseudoDeviceID().getBytes(UTF8), 20);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private String getUniquePseudoDeviceID() {
        return "35" + //we make this look like a valid IMEI for example 355715565309247
                Build.BOARD.length() % 10 + Build.BRAND.length() % 10 +
                Build.BOOTLOADER.length() % 10 + Build.DEVICE.length() % 10 +
                Build.DISPLAY.length() % 10 + Build.HOST.length() % 10 +
                Build.ID.length() % 10 + Build.MANUFACTURER.length() % 10 +
                Build.MODEL.length() % 10 + Build.PRODUCT.length() % 10 +
                Build.TAGS.length() % 10 + Build.TYPE.length() % 10 +
                Build.USER.length() % 10;
    }
}
