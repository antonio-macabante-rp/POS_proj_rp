package com.am.register.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Represents a suspended (parked) transaction.
 * Can be resumed later or auto-deleted after expiration.
 */
@Data
public class SuspendedTransaction {

    private String suspensionId;
    private LocalDateTime suspendedAt;
    private String transactionState;  // SHOPPING or TENDERING
    private double subtotal;
    private double tax;
    private double total;
    private int itemCount;
    private String itemsJson;  // Serialized List<TransactionItem>
    private String note;  // Optional: "Customer forgot wallet"

    /**
     * Generates a suspension ID based on current date and sequence.
     * Format: S-YYYYMMDD-NNN
     */
    public static String generateSuspensionId(int sequenceNumber) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String dateStr = LocalDateTime.now().format(formatter);
        return String.format("S-%s-%03d", dateStr, sequenceNumber);
    }

    /**
     * Gets formatted suspension time for display.
     */
    public String getFormattedSuspendedAt() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm");
        return suspendedAt.format(formatter);
    }

    /**
     * Gets how long ago the transaction was suspended.
     */
    public String getTimeAgo() {
        long minutes = ChronoUnit.MINUTES.between(suspendedAt, LocalDateTime.now());

        if (minutes < 1) return "Just now";
        if (minutes == 1) return "1 minute ago";
        if (minutes < 60) return minutes + " minutes ago";

        long hours = minutes / 60;
        if (hours == 1) return "1 hour ago";
        if (hours < 24) return hours + " hours ago";

        long days = hours / 24;
        if (days == 1) return "Yesterday";
        return days + " days ago";
    }

    /**
     * Checks if suspension is from a previous day (eligible for cleanup).
     */
    public boolean isFromPreviousDay() {
        LocalDateTime now = LocalDateTime.now();
        return suspendedAt.toLocalDate().isBefore(now.toLocalDate());
    }

    /**
     * Gets display summary for UI.
     */
    public String getDisplaySummary() {
        return String.format("%s - %d items - $%.2f - %s",
                suspensionId, itemCount, total, getTimeAgo());
    }
}