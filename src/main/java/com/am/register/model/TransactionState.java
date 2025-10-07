package com.am.register.model;

/**
 * Represents the state of a transaction.
 */
public enum TransactionState {
    SHOPPING("Shopping - Add Items"),
    TENDERING("Tendering - Process Payment");

    private final String displayName;

    TransactionState(String displayName) {
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