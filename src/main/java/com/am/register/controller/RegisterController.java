package com.am.register.controller;

import com.am.register.database.DatabaseManager;
import com.am.register.database.PriceBookParser;
import com.am.register.model.Item;
import com.am.register.model.Transaction;
import com.am.register.util.ConsoleJournal;
import com.am.register.view.DisplayPanel;

/**
 * Main controller for the register application.
 * Orchestrates interactions between Model, Database, and View layers.
 */
public class RegisterController {

    private final DatabaseManager databaseManager;
    private final Transaction currentTransaction;
    private DisplayPanel displayPanel; // View reference

    /**
     * Creates a new register controller.
     * @param databaseManager The database manager for item lookups
     */
    public RegisterController(DatabaseManager databaseManager) {
        if (databaseManager == null) {
            throw new IllegalArgumentException("DatabaseManager cannot be null");
        }

        this.databaseManager = databaseManager;
        this.currentTransaction = new Transaction();

        ConsoleJournal.logInfo("RegisterController initialized");
    }

    /**
     * Sets the display panel for UI updates.
     * Should be called after creating the view.
     * @param displayPanel The display panel to update
     */
    public void setDisplayPanel(DisplayPanel displayPanel) {
        this.displayPanel = displayPanel;

        // Initial display update
        if (displayPanel != null) {
            displayPanel.updateDisplay(currentTransaction);
        }
    }

    /**
     * Loads the price book into the database.
     * Should be called once at application startup.
     *
     * @param filename The price book file in resources (e.g., "pricebook.tsv")
     * @return true if loaded successfully, false otherwise
     */
    public boolean loadPriceBook(String filename) {
        ConsoleJournal.logInfo("Loading price book: " + filename);

        PriceBookParser parser = new PriceBookParser(databaseManager);
        boolean success = parser.parseFile(filename);

        if (success) {
            int itemCount = databaseManager.getItemCount();
            ConsoleJournal.logInfo("Price book loaded: " + itemCount + " items available");
        } else {
            ConsoleJournal.logError("Failed to load price book");
        }

        return success;
    }

    /**
     * Processes a scanned UPC code.
     * This is the main workflow triggered when a barcode is scanned.
     *
     * @param upc The scanned UPC code
     */
    public void processUPCScan(String upc) {
        // Validate input
        if (upc == null || upc.trim().isEmpty()) {
            ConsoleJournal.logError("Invalid UPC: empty or null");
            return;
        }

        // Clean up UPC (remove whitespace)
        upc = upc.trim();

        // Query database for item
        Item item = databaseManager.getItemByUPC(upc);

        if (item != null) {
            // Item found - add to transaction
            currentTransaction.addItem(item);

            // Log the scan
            double subtotal = currentTransaction.getSubtotal();
            ConsoleJournal.logItemScanned(item, subtotal);

            // Update view
            if (displayPanel != null) {
                displayPanel.updateDisplay(currentTransaction);
            }

        } else {
            // Item not found
            ConsoleJournal.logItemNotFound(upc);

            // Show error in view
            if (displayPanel != null) {
                displayPanel.showError("Item not found: " + upc);
            }
        }
    }

    /**
     * Starts a new transaction, clearing the current one.
     * Used when starting a new customer checkout.
     */
    public void startNewTransaction() {
        int itemCount = currentTransaction.getItemCount();
        double total = currentTransaction.getSubtotal();

        // Clear the transaction
        currentTransaction.clearTransaction();

        // Log the clear
        ConsoleJournal.logTransactionCleared(itemCount, total);

        // Update view
        if (displayPanel != null) {
            displayPanel.updateDisplay(currentTransaction);
        }
    }

    /**
     * Gets the current transaction.
     * Used by the view to display transaction details.
     *
     * @return The current transaction
     */
    public Transaction getCurrentTransaction() {
        return currentTransaction;
    }

    /**
     * Shuts down the controller and releases resources.
     * Should be called when the application closes.
     */
    public void shutdown() {
        ConsoleJournal.logInfo("Shutting down RegisterController");

        // Disconnect from database
        databaseManager.disconnect();

        ConsoleJournal.logInfo("RegisterController shutdown complete");
    }
}