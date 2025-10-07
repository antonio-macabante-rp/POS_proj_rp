package com.am.register.util;

import com.am.register.database.DatabaseManager;

/**
 * Manages dynamic popularity calculations based on sales data.
 */
public class PopularityManager {

    private final DatabaseManager databaseManager;

    // Configuration
    private static final int DEFAULT_TOP_N = 65;  // Number of items to mark popular
    private static final int SALES_PERIOD_DAYS = 30;  // Look at last 30 days

    public PopularityManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Recalculates popular items based on recent sales.
     * Should be run periodically (daily, weekly, or on-demand).
     */
    public void recalculatePopularItems() {
        System.out.println("\n╔══════════════════════════════════════════╗");
        System.out.println("║   RECALCULATING POPULAR ITEMS...         ║");
        System.out.println("╚══════════════════════════════════════════╝");

        // Get sales data
        java.util.Map<String, Integer> salesData =
                databaseManager.getSalesDataForDays(SALES_PERIOD_DAYS);

        if (salesData.isEmpty()) {
            System.out.println("\n⚠ No sales data available yet");
            System.out.println("  Popular items will be based on static configuration");
            System.out.println("  until transactions are processed.\n");
            return;
        }

        System.out.println("Analyzing sales from last " + SALES_PERIOD_DAYS + " days...");
        System.out.println("Total unique items sold: " + salesData.size());

        // Update database
        boolean success = databaseManager.updatePopularItems(DEFAULT_TOP_N);

        if (success) {
            System.out.println("\n✓ Popular items updated successfully");
            System.out.println("  Top " + DEFAULT_TOP_N + " selling items marked as popular\n");
        } else {
            System.out.println("\n✗ Failed to update popular items\n");
        }
    }

    /**
     * Prints sales report for debugging.
     */
    public void printSalesReport() {
        java.util.Map<String, Integer> salesData =
                databaseManager.getSalesDataForDays(SALES_PERIOD_DAYS);

        if (salesData.isEmpty()) {
            System.out.println("\nNo sales data available\n");
            return;
        }

        System.out.println("\n=== SALES REPORT (Last " + SALES_PERIOD_DAYS + " Days) ===");
        System.out.println();

        salesData.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(20)
                .forEach(entry -> {
                    System.out.printf("  %s: %d units sold%n", entry.getKey(), entry.getValue());
                });

        System.out.println();
    }
}