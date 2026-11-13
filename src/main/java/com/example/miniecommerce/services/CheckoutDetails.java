package com.example.miniecommerce.services;


/** Only customer-supplied contact fields; prices, amounts and owners are never bound. */
public record CheckoutDetails(String prenom, String nom, String email, String adresse) { }
