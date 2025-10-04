package com.am.register;

import com.am.register.controller.RegisterController;
import com.am.register.controller.ScannerInputHandler;
import com.am.register.database.DatabaseManager;
import com.am.register.util.H2ServerManager;
import com.am.register.view.MainFrame;

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
        System.out.print("[2/6] Connecting to database... ");
        DatabaseManager dbManager = new DatabaseManager();

        if (!dbManager.connect()) {
            System.out.println("✗");
            System.err.println("\nERROR: Failed to connect to database!");
            showErrorAndExit("Failed to connect to database!");
            return;
        }
        System.out.println("✓");

        System.out.print("[3/6] Creating database tables... ");
        dbManager.createTables();
        System.out.println("✓");

        // Step 3: Create Controller
        System.out.print("[4/6] Initializing controller... ");
        RegisterController controller = new RegisterController(dbManager);
        System.out.println("✓");

        // Step 4: Load Price Book
        System.out.print("[5/6] Loading price book... ");
        if (!controller.loadPriceBook("pricebook.tsv")) {
            System.out.println("✗");
            System.err.println("\nERROR: Failed to load price book!");
            showErrorAndExit("Failed to load price book!");
            controller.shutdown();
            H2ServerManager.stopServer();
            return;
        }
        System.out.println("✓");

        // Step 5: Create Scanner Handler
        System.out.print("[6/6] Initializing scanner handler... ");
        ScannerInputHandler scannerHandler = new ScannerInputHandler(controller);
        System.out.println("✓");

        System.out.println();
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║    INITIALIZATION COMPLETE - READY!      ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println();

        // Step 6: Launch GUI
        SwingUtilities.invokeLater(() -> {
            // Create main frame
            MainFrame frame = new MainFrame(scannerHandler, controller);

            // Connect view to controller
            controller.setDisplayPanel(frame.getDisplayPanel());

            // Add window closing handler
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    System.out.println("\n╔══════════════════════════════════════════╗");
                    System.out.println("║           SHUTTING DOWN...               ║");
                    System.out.println("╚══════════════════════════════════════════╝");
                    controller.shutdown();
                    H2ServerManager.stopServer();
                    System.out.println("\n✓ Goodbye!");
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
        System.out.println("• Scan items with barcode scanner");
        System.out.println("• Or type UPC in input field and press Enter");
        System.out.println("• Click 'Clear Transaction' or press F1 to start new transaction");
        System.out.println("• Press F2 to focus scanner input field");
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