package com.example.food.model;

public enum AuthProvider {
    EMAIL("EMAIL"),
    GOOGLE("GOOGLE"),
    FACEBOOK("FACEBOOK");

    private final String value;

    AuthProvider(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
