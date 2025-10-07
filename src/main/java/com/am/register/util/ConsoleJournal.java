package com.am.register.util;

import com.am.register.model.Item;
import com.am.register.model.InputSource;
import com.am.register.model.TaxBreakdown;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Console-based journal for logging register events.
 */
public class ConsoleJournal {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Logs when an item is added to transaction.
     */
    public static void logItemScanned(Item item, int quantity, double subtotal, double tax, double total, InputSource source) {
        printHeader(source.getJournalLabel());
        System.out.println("  UPC: " + item.getUpc());
        System.out.println("  Description: " + item.getDescription());
        System.out.println("  Category: " + item.getCategory());
        System.out.println("  Price: $" + String.format("%.2f", item.getPrice()));
        System.out.println("  Quantity: " + quantity);
        System.out.println("  Line Total: $" + String.format("%.2f", item.getPrice() * quantity));
        System.out.println("  ---");
        System.out.println("  Subtotal: $" + String.format("%.2f", subtotal));
        System.out.println("  Tax: $" + String.format("%.2f", tax));
        System.out.println("  Total: $" + String.format("%.2f", total));
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
     * Logs when a transaction is suspended.
     */
    public static void logTransactionSuspended(String suspensionId, int itemCount, double total, String note) {
        printHeader("TRANSACTION SUSPENDED");
        System.out.println("  Suspension ID: " + suspensionId);
        System.out.println("  Items: " + itemCount);
        System.out.println("  Total: $" + String.format("%.2f", total));
        if (note != null && !note.trim().isEmpty()) {
            System.out.println("  Note: " + note);
        }
        System.out.println();
    }

    /**
     * Logs when a suspended transaction is resumed.
     */
    public static void logTransactionResumed(String suspensionId, int itemCount, double total) {
        printHeader("TRANSACTION RESUMED");
        System.out.println("  Suspension ID: " + suspensionId);
        System.out.println("  Items: " + itemCount);
        System.out.println("  Total: $" + String.format("%.2f", total));
        System.out.println();
    }

    /**
     * Logs daily cleanup of expired suspensions.
     */
    public static void logSuspensionCleanup(int count) {
        printHeader("SUSPENSION CLEANUP");
        if (count > 0) {
            System.out.println("  Removed " + count + " expired suspensions from previous days");
        } else {
            System.out.println("  No expired suspensions to remove");
        }
        System.out.println();
    }

    /**
     * Logs when an item is voided from the transaction.
     */
    public static void logItemVoided(Item item, int quantity, double lineTotal) {
        printHeader("ITEM VOIDED");
        System.out.println("  UPC: " + item.getUpc());
        System.out.println("  Description: " + item.getDescription());
        System.out.println("  Quantity Voided: " + quantity);
        System.out.println("  Amount Removed: $" + String.format("%.2f", lineTotal));
        System.out.println();
    }

    /**
     * Logs when item quantity is changed.
     */
    public static void logQuantityChanged(Item item, int oldQuantity, int newQuantity, double newSubtotal) {
        printHeader("QUANTITY CHANGED");
        System.out.println("  UPC: " + item.getUpc());
        System.out.println("  Description: " + item.getDescription());
        System.out.println("  Old Quantity: " + oldQuantity);
        System.out.println("  New Quantity: " + newQuantity);
        System.out.println("  Change: " + (newQuantity > oldQuantity ? "+" : "") + (newQuantity - oldQuantity));
        System.out.println("  New Subtotal: $" + String.format("%.2f", newSubtotal));
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

    /**
     * Logs tax calculation with category breakdown.
     */
    public static void logTaxCalculated(double subtotal, TaxBreakdown breakdown, double total) {
        printHeader("TAX CALCULATED");
        System.out.println("  Subtotal: $" + String.format("%.2f", subtotal));

        if (breakdown.hasMultipleTaxRates()) {
            System.out.println("\n  Tax Breakdown:");
            breakdown.getCategoryTaxes().values().stream()
                    .sorted((a, b) -> a.getCategory().compareTo(b.getCategory()))
                    .forEach(catTax -> {
                        System.out.println(String.format("    %s (%s): $%.2f on $%.2f",
                                catTax.getCategory(),
                                catTax.getFormattedRate(),
                                catTax.getTaxAmount(),
                                catTax.getSubtotal()));
                    });
            System.out.println();
        }

        System.out.println("  Total Tax: $" + String.format("%.2f", breakdown.getTotalTax()));
        System.out.println("  Grand Total: $" + String.format("%.2f", total));
        System.out.println();
    }
}