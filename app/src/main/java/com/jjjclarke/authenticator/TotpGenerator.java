package com.jjjclarke.authenticator;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base32;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class TotpGenerator {

    private static final int[] DIGITS_POWER = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};

    /**
     * Generates a TOTP code from a Base32-encoded secret.
     *
     * @param base32Secret The secret key in Base32 format (from QR code)
     * @param digits Number of digits in the code (usually 6, sometimes 8)
     * @param period Time step in seconds (usually 30)
     * @param algorithm HMAC algorithm (usually "HmacSHA1", sometimes "HmacSHA256")
     * @return The current TOTP code as a String
     */
    public static String generateTotp(String base32Secret, int digits, int period, String algorithm)
            throws NoSuchAlgorithmException, InvalidKeyException {

        // Step 1: Decode the Base32 secret to raw bytes
        // Normalize: remove spaces and convert to uppercase for the decoder
        String normalizedSecret = base32Secret.replace(" ", "").toUpperCase();
        Base32 base32 = new Base32();
        byte[] secretBytes = base32.decode(normalizedSecret);

        // Step 2: Calculate the time counter (number of time steps since Unix epoch)
        long currentTime = System.currentTimeMillis() / 1000L;
        long timeCounter = currentTime / period;

        // Step 3: Generate the TOTP
        return generateTotpFromCounter(secretBytes, timeCounter, digits, algorithm);
    }

    /**
     * Core TOTP generation logic using HMAC.
     */
    private static String generateTotpFromCounter(byte[] secret, long counter, int digits, String algorithm)
            throws NoSuchAlgorithmException, InvalidKeyException {

        // Step 1: Convert counter to 8-byte array (big-endian)
        byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();

        // Step 2: Perform HMAC
        Mac hmac = Mac.getInstance(algorithm);
        SecretKeySpec keySpec = new SecretKeySpec(secret, algorithm);
        hmac.init(keySpec);
        byte[] hash = hmac.doFinal(counterBytes);

        // Step 3: Dynamic truncation (this is the RFC 6238 magic)
        int offset = hash[hash.length - 1] & 0x0F;  // Use last 4 bits as offset

        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);

        int otp = binary % DIGITS_POWER[digits];

        // Step 4: Pad with leading zeros if necessary
        String result = Integer.toString(otp);
        while (result.length() < digits) {
            result = "0" + result;
        }

        return result;
    }

    // Helper method to print bytes as hex
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * Convenience method with default parameters (6 digits, 30s period, SHA1).
     */
    public static String generateTotp(String base32Secret)
            throws NoSuchAlgorithmException, InvalidKeyException {
        return generateTotp(base32Secret, 6, 30, "HmacSHA1");
    }

    /**
     * Calculates how many seconds until the current code expires.
     */
    public static int getSecondsUntilExpiry(int period) {
        long currentTime = System.currentTimeMillis() / 1000L;
        return period - (int)(currentTime % period);
    }
}