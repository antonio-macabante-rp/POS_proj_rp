package com.am.register.model;

/**
 * Enum representing payment methods accepted by the register.
 */
public enum PaymentMethod {
    CASH("Cash"),
    CARD("Card"),
    NONE("None");  // For incomplete transactions

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}