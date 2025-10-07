package com.am.register.controller;

import com.am.register.database.DatabaseManager;
import com.am.register.database.PriceBookParser;
import com.am.register.model.*;
import com.am.register.util.ConsoleJournal;
import com.am.register.view.DisplayPanel;
import com.am.register.util.ReceiptGenerator;
import com.am.register.view.ReceiptDialog;
import com.am.register.model.SuspendedTransaction;
import com.am.register.util.TransactionSerializer;

import java.time.LocalDateTime;
import java.util.ArrayList;

import javax.swing.*;
import java.awt.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Main controller for the register application.
 * Orchestrates interactions between Model, Database, and View layers.
 */
public class RegisterController {

    private final DatabaseManager databaseManager;
    private Transaction currentTransaction;
    private DisplayPanel displayPanel; // View reference
    private com.am.register.view.PaymentPanel paymentPanel;
    private ScannerInputHandler scannerHandler;
    private static final int MAX_SUSPENDED_TRANSACTIONS = 10;
    private List<SuspendedTransaction> suspendedTransactions = new ArrayList<>();

    /**
     * Creates a new register controller.
     * @param databaseManager The database manager for item lookups
     */
    public RegisterController(DatabaseManager databaseManager) {
        if (databaseManager == null) {
            throw new IllegalArgumentException("DatabaseManager cannot be null");
        }

        this.databaseManager = databaseManager;
        currentTransaction = new Transaction();

        ConsoleJournal.logInfo("RegisterController initialized");

        loadSuspendedTransactions();
    }

    /**
     * Loads suspended transactions from database on startup.
     */
    private void loadSuspendedTransactions() {
        suspendedTransactions = databaseManager.getAllSuspendedTransactions();

        if (!suspendedTransactions.isEmpty()) {
            ConsoleJournal.logInfo("Loaded " + suspendedTransactions.size() +
                    " suspended transactions from database");
        }
    }

    /**
     * Suspends the current transaction.
     * Can be called from any state (SHOPPING or TENDERING).
     * @param note Optional note about why suspended
     * @return The suspension ID if successful, null if failed
     */
    public String suspendCurrentTransaction(String note) {
        // Validate transaction has items
        if (currentTransaction.getItemCount() == 0) {
            ConsoleJournal.logError("Cannot suspend empty transaction");
            if (displayPanel != null) {
                displayPanel.showError("Cannot suspend an empty transaction");
            }
            return null;
        }

        // Check suspension limit
        if (suspendedTransactions.size() >= MAX_SUSPENDED_TRANSACTIONS) {
            ConsoleJournal.logError("Maximum suspended transactions reached (" + MAX_SUSPENDED_TRANSACTIONS + ")");
            if (displayPanel != null) {
                displayPanel.showError("Cannot suspend transaction.\n\n" +
                        "Maximum of " + MAX_SUSPENDED_TRANSACTIONS + " suspensions reached.\n" +
                        "Please resume or delete existing suspensions.");
            }
            return null;
        }

        // Generate suspension ID
        int sequence = databaseManager.getNextSuspensionSequence();
        String suspensionId = SuspendedTransaction.generateSuspensionId(sequence);

        // Create suspension from current transaction
        SuspendedTransaction suspension = TransactionSerializer.createSuspension(
                currentTransaction,
                suspensionId,
                note
        );

        // Save to database
        boolean saved = databaseManager.saveSuspendedTransaction(suspension);

        if (!saved) {
            ConsoleJournal.logError("Failed to save suspended transaction to database");
            if (displayPanel != null) {
                displayPanel.showError("Failed to suspend transaction");
            }
            return null;
        }

        // Add to in-memory list
        suspendedTransactions.add(suspension);

        // Log the suspension
        ConsoleJournal.logTransactionSuspended(
                suspensionId,
                currentTransaction.getItemCount(),
                currentTransaction.getTotal(),
                note
        );

        // Re-enable scanner if it was disabled
        if (scannerHandler != null) {
            scannerHandler.setEnabled(true);
        }

        // Clear current transaction and start fresh
        this.currentTransaction = new Transaction();

        // Update view
        if (displayPanel != null) {
            displayPanel.updateDisplay(currentTransaction);
        }

        ConsoleJournal.logInfo("Transaction suspended successfully. Started new transaction.");

        return suspensionId;
    }

    /**
     * Resumes a suspended transaction by ID.
     * Replaces current transaction (current is lost if not empty).
     * @param suspensionId The ID of the suspension to resume
     * @return true if successful, false if failed
     */
    public boolean resumeSuspendedTransaction(String suspensionId) {
        // Find the suspension
        SuspendedTransaction suspension = suspendedTransactions.stream()
                .filter(s -> s.getSuspensionId().equals(suspensionId))
                .findFirst()
                .orElse(null);

        if (suspension == null) {
            ConsoleJournal.logError("Suspension not found: " + suspensionId);
            return false;
        }

        // Warn if current transaction has items
        if (currentTransaction.getItemCount() > 0) {
            ConsoleJournal.logError("Cannot resume while current transaction has items. Suspend or void current transaction first.");
            if (displayPanel != null) {
                displayPanel.showError("Current transaction has items!\n\n" +
                        "Please suspend or void the current transaction\n" +
                        "before resuming a suspended one.");
            }
            return false;
        }

        // Restore transaction from suspension
        try {
            Transaction restoredTransaction = TransactionSerializer.restoreTransaction(suspension);

            // Replace current transaction
            currentTransaction = restoredTransaction;

            // Remove from suspension list
            suspendedTransactions.remove(suspension);

            // Delete from database
            databaseManager.deleteSuspendedTransaction(suspensionId);

            // Update scanner state based on restored transaction state
            if (scannerHandler != null) {
                scannerHandler.setEnabled(currentTransaction.isShopping());
            }

            // Log the resume
            ConsoleJournal.logTransactionResumed(
                    suspensionId,
                    currentTransaction.getItemCount(),
                    currentTransaction.getTotal()
            );

            // Update view
            if (displayPanel != null) {
                displayPanel.updateDisplay(currentTransaction);
            }

            ConsoleJournal.logInfo("Transaction resumed successfully");

            return true;

        } catch (Exception e) {
            ConsoleJournal.logError("Failed to restore transaction: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Removes suspended transactions older than the specified cutoff date
     * @param cutoffDate Transactions suspended before this date will be removed
     * @return Number of transactions removed
     */
    public int cleanupOldSuspensions(LocalDateTime cutoffDate) {
        List<SuspendedTransaction> toRemove = suspendedTransactions.stream()
                .filter(st -> st.getSuspendedAt().isBefore(cutoffDate))
                .collect(Collectors.toList());

        int removedCount = toRemove.size();
        suspendedTransactions.removeAll(toRemove);

        return removedCount;
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
     * Processes a UPC code from any input source.
     *
     * @param upc The UPC code
     * @param source The input source (scanner/manual/grid)
     */
    public void processUPCScan(String upc, InputSource source) {
        if (currentTransaction.isTendering()) {
            ConsoleJournal.logError("Cannot add items during tendering phase");
            if (displayPanel != null) {
                displayPanel.showError("Cannot add items during payment.\nVoid transaction to start over.");
            }
            return;
        }

        if (upc == null || upc.trim().isEmpty()) {
            ConsoleJournal.logError("Invalid UPC: empty or null");
            return;
        }

        upc = upc.trim();

        Item item = databaseManager.getItemByUPC(upc);

        if (item != null) {
            // Add to transaction (will increment if exists)
            currentTransaction.addItem(item);

            // Find the transaction item to get current quantity
            TransactionItem txItem = null;
            for (TransactionItem ti : currentTransaction.getItems()) {
                if (ti.getUpc().equals(upc)) {
                    txItem = ti;
                    break;
                }
            }

            // Log with quantity
            if (txItem != null) {
                double subtotal = currentTransaction.getSubtotal();
                double tax = currentTransaction.getTaxAmount();
                double total = currentTransaction.getTotal();
                ConsoleJournal.logItemScanned(item, txItem.getQuantity(), subtotal, tax, total, source);
            }

            // Update view
            if (displayPanel != null) {
                displayPanel.updateDisplay(currentTransaction);
            }

        } else {
            ConsoleJournal.logItemNotFound(upc);

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

        currentTransaction.clearTransaction();

        // Re-enable scanner
        if (scannerHandler != null) {
            scannerHandler.setEnabled(true);
        }

        ConsoleJournal.logTransactionCleared(itemCount, total);

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

    public void setPaymentPanel(com.am.register.view.PaymentPanel paymentPanel) {
        this.paymentPanel = paymentPanel;
    }

    /**
     * Shuts down the controller and releases resources.
     * Should be called when the application closes.
     */
    public void shutdown() {
        ConsoleJournal.logInfo("Shutting down RegisterController");

        // Warn if there are suspended transactions
        if (!suspendedTransactions.isEmpty()) {
            ConsoleJournal.logInfo("Note: " + suspendedTransactions.size() +
                    " suspended transactions will persist until tomorrow's cleanup");
        }

        // Disconnect from database
        databaseManager.disconnect();

        ConsoleJournal.logInfo("RegisterController shutdown complete");
    }

    /**
     * Gets all items from the database for grid display.
     * @return List of all items
     */
    public List<Item> getAllItems() {
        // Query database for all items
        // For now, we'll need to add this method to DatabaseManager
        return databaseManager.getAllItems();
    }

    /**
     * Gets popular items from the database for quick access.
     * @return List of popular items
     */
    public List<Item> getPopularItems() {
        return databaseManager.getPopularItems();
    }

    /**
     * Voids an item at the specified index.
     */
    public void voidItem(int index) {
        if (index < 0 || index >= currentTransaction.getLineCount()) {
            ConsoleJournal.logError("Invalid item index for void: " + index);
            return;
        }

        TransactionItem txItem = currentTransaction.getItem(index);

        if (txItem == null) {
            ConsoleJournal.logError("Item not found at index: " + index);
            return;
        }

        // Log the void
        ConsoleJournal.logItemVoided(
                txItem.getItem(),
                txItem.getQuantity(),
                txItem.getLineTotal()
        );

        // Remove from transaction
        currentTransaction.removeItem(index);

        // Update view
        if (displayPanel != null) {
            displayPanel.updateDisplay(currentTransaction);
        }
    }

    /**
     * Changes the quantity of an item at the specified index.
     */
    public void changeItemQuantity(int index, int newQuantity) {
        if (index < 0 || index >= currentTransaction.getLineCount()) {
            ConsoleJournal.logError("Invalid item index for quantity change: " + index);
            return;
        }

        if (newQuantity < 1) {
            ConsoleJournal.logError("Invalid quantity: " + newQuantity);
            return;
        }

        TransactionItem txItem = currentTransaction.getItem(index);

        if (txItem == null) {
            ConsoleJournal.logError("Item not found at index: " + index);
            return;
        }

        int oldQuantity = txItem.getQuantity();

        // Change quantity
        currentTransaction.changeQuantity(index, newQuantity);

        // Log the change
        ConsoleJournal.logQuantityChanged(
                txItem.getItem(),
                oldQuantity,
                newQuantity,
                currentTransaction.getSubtotal()
        );

        // Update view
        if (displayPanel != null) {
            displayPanel.updateDisplay(currentTransaction);
        }
    }

    /**
     * Starts the tendering phase.
     */
    public void startTendering() {
        if (currentTransaction.getItemCount() == 0) {
            ConsoleJournal.logError("Cannot tender empty transaction");
            return;
        }

        currentTransaction.startTendering();

        // Log tax calculation with breakdown
        TaxBreakdown breakdown = currentTransaction.calculateTaxBreakdown();
        ConsoleJournal.logTaxCalculated(
                currentTransaction.getSubtotal(),
                breakdown,
                currentTransaction.getTotal()
        );

        // Disable scanner
        if (scannerHandler != null) {
            scannerHandler.setEnabled(false);
        }

        ConsoleJournal.logInfo("Tendering phase started - Scanner disabled, items locked");

        if (displayPanel != null) {
            displayPanel.updateDisplay(currentTransaction);
        }
    }

    /**
     * Processes exact dollar cash payment.
     */
    public void processExactCashPayment() {
        if (currentTransaction.getItemCount() == 0) {
            ConsoleJournal.logError("Cannot process payment: transaction is empty");
            return;
        }

        double total = currentTransaction.getTotal();  // Changed from getSubtotal()
        Payment payment = Payment.createCashPayment(total, 0.0);
        currentTransaction.setPayment(payment);

        ConsoleJournal.logInfo(String.format(
                "Cash payment processed: Amount=$%.2f, Change=$0.00",
                total
        ));

        completeTransaction();
    }

    /**
     * Processes next dollar cash payment (rounds up).
     */
    public void processNextDollarPayment() {
        if (currentTransaction.getItemCount() == 0) {
            ConsoleJournal.logError("Cannot process payment: transaction is empty");
            return;
        }

        double total = currentTransaction.getTotal();  // Changed
        double nextDollar = Math.ceil(total);
        double change = nextDollar - total;

        Payment payment = Payment.createCashPayment(nextDollar, change);
        currentTransaction.setPayment(payment);

        ConsoleJournal.logInfo(String.format(
                "Cash payment processed: Amount=$%.2f, Change=$%.2f",
                nextDollar, change
        ));

        completeTransaction();
    }

    /**
     * Processes custom amount cash payment.
     */
    public void processCustomCashPayment(double amountTendered) {
        if (currentTransaction.getItemCount() == 0) {
            ConsoleJournal.logError("Cannot process payment: transaction is empty");
            return;
        }

        double total = currentTransaction.getTotal();  // Changed

        if (amountTendered < total) {
            if (displayPanel != null) {
                displayPanel.showError(String.format(
                        "Insufficient payment: $%.2f tendered for $%.2f total",
                        amountTendered, total
                ));
            }
            ConsoleJournal.logError("Payment rejected: insufficient amount");
            return;
        }

        double change = amountTendered - total;
        Payment payment = Payment.createCashPayment(amountTendered, change);
        currentTransaction.setPayment(payment);

        ConsoleJournal.logInfo(String.format(
                "Cash payment processed: Amount=$%.2f, Change=$%.2f",
                amountTendered, change
        ));

        completeTransaction();
    }

    /**
     * Processes card payment.
     */
    public void processCardPayment(CardType cardType) {
        if (currentTransaction.getItemCount() == 0) {
            ConsoleJournal.logError("Cannot process payment: transaction is empty");
            return;
        }

        double total = currentTransaction.getTotal();  // Changed
        Payment payment = Payment.createCardPayment(cardType, total);
        currentTransaction.setPayment(payment);

        ConsoleJournal.logInfo(String.format(
                "Card payment processed: Type=%s, Amount=$%.2f",
                cardType.getDisplayName(), total
        ));

        completeTransaction();
    }

    /**
     * Completes the transaction and prepares for receipt.
     */
    private void completeTransaction() {
        // Generate receipt
        Receipt receipt = ReceiptGenerator.createReceipt(currentTransaction);

        // Save receipt to file
        boolean savedFile = ReceiptGenerator.saveReceipt(receipt);
        if (savedFile) {
            ConsoleJournal.logInfo("Receipt saved to file: " + receipt.getReceiptNumber());
        }

        // NEW: Save transaction to database for analytics
        boolean savedToDB = databaseManager.saveTransaction(currentTransaction, receipt.getReceiptNumber());
        if (savedToDB) {
            ConsoleJournal.logInfo("Transaction saved to database: " + receipt.getReceiptNumber());
        } else {
            ConsoleJournal.logError("Failed to save transaction to database");
        }

        // Show receipt dialog
        if (displayPanel != null) {
            Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(displayPanel);
            ReceiptDialog dialog = new ReceiptDialog(parentFrame, receipt);
            dialog.setVisible(true);
        }

        ConsoleJournal.logInfo("Transaction completed: " + receipt.getReceiptNumber());

        // Clear transaction (returns to SHOPPING state)
        startNewTransaction();
    }

    public void setScannerHandler(ScannerInputHandler handler) {
        this.scannerHandler = handler;
    }

    /**
     * Gets the database manager (for advanced operations).
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * Gets all suspended transactions.
     * @return List of suspended transactions (newest first)
     */
    public List<SuspendedTransaction> getSuspendedTransactions() {
        return new ArrayList<>(suspendedTransactions);  // Return copy for safety
    }

    /**
     * Gets count of suspended transactions.
     */
    public int getSuspendedTransactionCount() {
        return suspendedTransactions.size();
    }

    /**
     * Checks if suspension limit has been reached.
     */
    public boolean isSuspensionLimitReached() {
        return suspendedTransactions.size() >= MAX_SUSPENDED_TRANSACTIONS;
    }

    /**
     * Deletes a suspended transaction without resuming it.
     * @param suspensionId The ID to delete
     */
    public boolean deleteSuspension(String suspensionId) {
        SuspendedTransaction suspension = suspendedTransactions.stream()
                .filter(s -> s.getSuspensionId().equals(suspensionId))
                .findFirst()
                .orElse(null);

        if (suspension == null) {
            return false;
        }

        // Remove from list
        suspendedTransactions.remove(suspension);

        // Delete from database
        boolean deleted = databaseManager.deleteSuspendedTransaction(suspensionId);

        if (deleted) {
            ConsoleJournal.logInfo("Deleted suspended transaction: " + suspensionId);
        }

        return deleted;
    }

    /**
     * Performs daily cleanup of expired suspensions.
     * Removes all suspensions from previous days.
     */
    public void performDailyCleanup() {
        // Remove from database
        int deletedCount = databaseManager.cleanupExpiredSuspensions();

        // Remove from in-memory list
        suspendedTransactions.removeIf(s -> s.isFromPreviousDay());

        // Log cleanup
        ConsoleJournal.logSuspensionCleanup(deletedCount);
    }
}