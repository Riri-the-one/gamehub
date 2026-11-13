package com.example.miniecommerce.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** DH amounts: two decimal places, HALF_UP arithmetic, bounded by DECIMAL(19,2). */
public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal MAX = new BigDecimal("99999999999999999.99");
    private Money() { }

    public static BigDecimal amount(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(MAX) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Montant invalide.");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal price(BigDecimal value) {
        if(value==null || value.signum()<0 || value.compareTo(MAX)>0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Prix invalide.");
        }
        BigDecimal normalized = amount(value);
        if (normalized.compareTo(value) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le prix doit comporter au maximum deux décimales.");
        }
        return normalized;
    }
}
