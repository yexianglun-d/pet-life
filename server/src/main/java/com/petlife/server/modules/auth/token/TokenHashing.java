package com.petlife.server.modules.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Token 摘要工具。
 */
public final class TokenHashing {

    private TokenHashing() {
    }

    public static String sha256Hex(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexBuilder = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                hexBuilder.append(String.format("%02x", item));
            }
            return hexBuilder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }
}
