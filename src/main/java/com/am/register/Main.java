package com.am.register;

import com.am.register.controller.RegisterController;
import com.am.register.controller.ScannerInputHandler;
import com.am.register.database.DatabaseManager;
import com.am.register.database.DatabaseMigration;  // ADD THIS
import com.am.register.model.Item;
import com.am.register.model.Transaction;
import com.am.register.util.H2ServerManager;
import com.am.register.util.PopularityManager;
import com.am.register.view.MainFrame;
import com.am.register.model.SuspendedTransaction;
import com.am.register.model.TransactionItem;
import com.am.register.util.TransactionSerializer;
import java.util.List;

import javax.swing.*;

/**
 * Main entry point for Mock Register System.
 * Complete integrated application with GUI.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║     MOCK REGISTER SYSTEM - STARTING      ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println();

        // Step 1: Start H2 Server
        System.out.print("[1/6] Starting H2 Database Server... ");
        H2ServerManager.startServer();
        System.out.println("✓");

        // Wait for server initialization
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Step 2: Initialize Database
        System.out.println("[2/6] Connecting to database...");
        DatabaseManager dbManager = new DatabaseManager();

        if (!dbManager.connect()) {
            System.err.println("      FAILED: Could not connect to database");
            showErrorAndExit("Failed to connect to database!");
            return;
        }
        System.out.println("      ✓ Connected");

        // Step 2.5: Run Migration
        System.out.println("[2.5/6] Running database migration...");
        DatabaseMigration migration = new DatabaseMigration(dbManager.getConnection());
        migration.migrateToVersion2();

        // Step 3: Create Tables
        System.out.print("[3/6] Creating database tables... ");
        dbManager.createTables();
        System.out.println("✓");

        // Step 4: Create Controller
        System.out.print("[4/6] Initializing controller... ");
        RegisterController controller = new RegisterController(dbManager);
        System.out.println("✓");

        // Step 5: Load Price Book
        System.out.print("[5/6] Loading price book... ");
        dbManager.clearAllItems();
        System.out.println("✓");
        System.out.print("[5.5/6] Loading price book... ");
        if (!controller.loadPriceBook("pricebook_categorized.tsv")) {
            System.out.println("✗");
            System.err.println("\nERROR: Failed to load price book!");
            showErrorAndExit("Failed to load price book!\n\n" +
                    "Please check:\n" +
                    "- pricebook_categorized.tsv exists in src/main/resources/\n" +
                    "- File format is correct\n" +
                    "- Console output for details");
            controller.shutdown();
            H2ServerManager.stopServer();
            return;
        }
        System.out.println("✓");

        // After loading price book, before creating GUI
        System.out.println("\n=== TESTING SUSPENSION SYSTEM ===");

        // Test 1: Create and serialize a transaction
        Transaction testTx = new Transaction();
        Item testItem1 = dbManager.getItemByUPC("049000053418"); // Coca Cola
        Item testItem2 = dbManager.getItemByUPC("028200003843"); // Marlboro
        if (testItem1 != null) testTx.addItem(testItem1);
        if (testItem1 != null) testTx.addItem(testItem1); // Add twice (qty=2)
        if (testItem2 != null) testTx.addItem(testItem2);

        System.out.println("Test transaction: " + testTx.getItemCount() + " items, $" +
                String.format("%.2f", testTx.getTotal()));

        // Test 2: Serialize
        String json = TransactionSerializer.serializeItems(testTx.getItems());
        System.out.println("Serialized JSON length: " + json.length() + " characters");

        // Test 3: Deserialize
        List<TransactionItem> restored = TransactionSerializer.deserializeItems(json);
        System.out.println("Deserialized: " + restored.size() + " line items");

        // Test 4: Create suspension
        int sequence = dbManager.getNextSuspensionSequence();
        String suspensionId = SuspendedTransaction.generateSuspensionId(sequence);
        SuspendedTransaction suspension = TransactionSerializer.createSuspension(
                testTx, suspensionId, "Test suspension"
        );

        System.out.println("Created suspension: " + suspension.getSuspensionId());

        // Test 5: Save to database
        boolean saved = dbManager.saveSuspendedTransaction(suspension);
        System.out.println("Saved to database: " + (saved ? "✓" : "✗"));

        // Test 6: Load from database
        List<SuspendedTransaction> loaded = dbManager.getAllSuspendedTransactions();
        System.out.println("Loaded from database: " + loaded.size() + " suspensions");

        // Test 7: Restore transaction
        if (!loaded.isEmpty()) {
            Transaction restoredTx = TransactionSerializer.restoreTransaction(loaded.get(0));
            System.out.println("Restored transaction: " + restoredTx.getItemCount() + " items, $" +
                    String.format("%.2f", restoredTx.getTotal()));
            System.out.println("State: " + restoredTx.getState());
        }

        // Test 8: Cleanup
        dbManager.deleteSuspendedTransaction(suspensionId);
        System.out.println("Cleanup: ✓");

        System.out.println("=== SUSPENSION SYSTEM TEST COMPLETE ===\n");

        System.out.println("\n=== PHASE 13B: CONTROLLER SUSPENSION TEST ===");

        // Create a test transaction
        Transaction tx = new Transaction();
        Item item1 = dbManager.getItemByUPC("049000053418");
        Item item2 = dbManager.getItemByUPC("028200003843");
        if (item1 != null) {
            tx.addItem(item1);
            tx.addItem(item1); // Qty = 2
        }
        if (item2 != null) {
            tx.addItem(item2);
        }

        // Temporarily set as current transaction
        Transaction originalTx = controller.getCurrentTransaction();
        // We can't directly set it, so we'll test through the methods instead

        System.out.println("Current suspended count: " + controller.getSuspendedTransactionCount());
        System.out.println("Limit reached: " + controller.isSuspensionLimitReached());
        System.out.println("Max limit: " + 10);

        // Test cleanup
        System.out.println("\nTesting daily cleanup...");
        controller.performDailyCleanup();

        System.out.println("\n=== PHASE 13B TEST COMPLETE ===\n");

        // Step 5.5: Refresh popular items based on sales
        System.out.print("[5.5/6] Updating popular items... ");
        PopularityManager popularityManager = new PopularityManager(dbManager);
        popularityManager.recalculatePopularItems();
        System.out.println("✓");

        // Step 6: Create Scanner Handler
        System.out.print("[6/6] Initializing scanner handler... ");
        ScannerInputHandler scannerHandler = new ScannerInputHandler(controller);
        controller.setScannerHandler(scannerHandler);  // ADD THIS LINE
        System.out.println("✓");

        System.out.println();
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║    INITIALIZATION COMPLETE - READY!      ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println();

        // Step 7: Launch GUI
        SwingUtilities.invokeLater(() -> {
            try {
                // Set system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Use default if system L&F fails
            }

            // Create main frame
            MainFrame frame = new MainFrame(scannerHandler, controller);

            // Connect view to controller
            controller.setDisplayPanel(frame.getDisplayPanel());

            // Add window close handler
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    // Shutdown when window actually closes (after dispose)
                    System.out.println("\n╔══════════════════════════════════════════╗");
                    System.out.println("║           SHUTTING DOWN...               ║");
                    System.out.println("╚══════════════════════════════════════════╝");
                    controller.shutdown();
                    H2ServerManager.stopServer();
                    System.out.println("\n✓ Goodbye!");
                    System.exit(0);
                }
            });

            // Show the window
            frame.setVisible(true);

            // Print usage instructions
            printUsageInstructions();
        });
    }

    /**
     * Prints usage instructions to console.
     */
    private static void printUsageInstructions() {
        System.out.println("USAGE INSTRUCTIONS:");
        System.out.println("─────────────────────────────────────────");
        System.out.println("• Scan items with barcode scanner (works globally)");
        System.out.println("• Click items in grid for quick selection");
        System.out.println("• Type UPC in manual entry field + Enter");
        System.out.println("• Press F2 to clear transaction");
        System.out.println("• Press Ctrl+U to focus manual UPC field");
        System.out.println("• Click ⭐ Popular to view popular items");
        System.out.println("• Close window to exit application");
        System.out.println("─────────────────────────────────────────");
        System.out.println();
    }

    /**
     * Shows error dialog and exits application.
     */
    private static void showErrorAndExit(String message) {
        JOptionPane.showMessageDialog(null,
                message,
                "Startup Error",
                JOptionPane.ERROR_MESSAGE);
        H2ServerManager.stopServer();
        System.exit(1);
    }
}