package com.am.register.model;

/**
 * Enum representing the source of item input.
 */
public enum InputSource {
    SCANNER("ITEM SCANNED"),
    MANUAL("ITEM ADDED - MANUAL"),
    QUICK_ADD("QUICK ADD");

    private final String journalLabel;

    InputSource(String journalLabel) {
        this.journalLabel = journalLabel;
    }

    public String getJournalLabel() {
        return journalLabel;
    }
}