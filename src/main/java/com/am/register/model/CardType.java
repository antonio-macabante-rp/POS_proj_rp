package com.am.register.model;

/**
 * Enum representing types of payment cards.
 */
public enum CardType {
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    AMERICAN_EXPRESS("American Express"),
    DISCOVER("Discover"),
    OTHER("Other");

    private final String displayName;

    CardType(String displayName) {
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