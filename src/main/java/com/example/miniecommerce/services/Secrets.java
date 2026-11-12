package com.example.miniecommerce.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class Secrets {
    private static final SecureRandom RANDOM = new SecureRandom();
    private Secrets() { }
    public static String token() {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes); return HexFormat.of().formatHex(bytes);
    }
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public static String fingerprint(CheckoutDetails details) {
        return hash(details.prenom().length()+":"+details.prenom()+details.nom().length()+":"+details.nom()
                +details.email().length()+":"+details.email()+details.adresse().length()+":"+details.adresse());
    }
}
