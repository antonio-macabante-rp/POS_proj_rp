package com.am.register.util;

import com.am.register.model.Item;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Console-based journal for logging register events.
 * Provides formatted, timestamped entries for debugging and audit trail.
 */
public class ConsoleJournal {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Logs when an item is successfully scanned.
     */
    public static void logItemScanned(Item item, double subtotal) {
        printHeader("ITEM SCANNED");
        System.out.println("  UPC: " + item.getUpc());
        System.out.println("  Description: " + item.getDescription());
        System.out.println("  Price: $" + String.format("%.2f", item.getPrice()));
        System.out.println("  Subtotal: $" + String.format("%.2f", subtotal));
        System.out.println();
    }

    /**
     * Logs when a scanned UPC is not found in the database.
     */
    public static void logItemNotFound(String upc) {
        printHeader("ITEM NOT FOUND");
        System.out.println("  UPC: " + upc);
        System.out.println("  Action: Item not in price book");
        System.out.println();
    }

    /**
     * Logs when a transaction is cleared.
     */
    public static void logTransactionCleared(int itemCount, double total) {
        printHeader("TRANSACTION CLEARED");
        System.out.println("  Items: " + itemCount);
        System.out.println("  Total: $" + String.format("%.2f", total));
        System.out.println();
    }

    /**
     * Logs a general informational message.
     */
    public static void logInfo(String message) {
        printHeader("INFO");
        System.out.println("  " + message);
        System.out.println();
    }

    /**
     * Logs an error message.
     */
    public static void logError(String message) {
        printHeader("ERROR");
        System.err.println("  " + message);
        System.out.println();
    }

    /**
     * Prints a formatted header with timestamp.
     */
    private static void printHeader(String eventType) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        System.out.println("[" + timestamp + "] " + eventType);
    }
}