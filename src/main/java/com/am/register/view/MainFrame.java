package com.am.register.view;

import com.am.register.controller.RegisterController;
import com.am.register.controller.ScannerInputHandler;
import com.am.register.database.DatabaseManager;
import com.am.register.model.*;
import com.am.register.util.PopularityManager;
import com.am.register.model.SuspendedTransaction;
import com.am.register.util.SuspensionCleanupScheduler;

import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Main application window - CORRECTED LAYOUT
 */
public class MainFrame extends JFrame {

    private final DisplayPanel displayPanel;
    private final ItemGridPanel itemGridPanel;
    private final RegisterController controller;
    private JButton changeQuantityButton;
    private PaymentPanel paymentPanel;
    private JTextField manualUpcField;
    private JButton tenderButton;
    private PopularityManager popularityManager;
    private JButton suspendButton;
    private SuspensionCleanupScheduler cleanupScheduler;

    public MainFrame(ScannerInputHandler scannerHandler, RegisterController controller) {
        this.controller = controller;
        this.popularityManager = new PopularityManager(controller.getDatabaseManager());

        setTitle("Mock Register System");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
        setMinimumSize(new Dimension(1200, 700));

        displayPanel = new DisplayPanel();
        itemGridPanel = new ItemGridPanel(controller);

        displayPanel.setClearButtonListener(e -> clearTransaction());

        displayPanel.setPaymentEnabledCallback(enabled -> {
            boolean isShopping = controller.getCurrentTransaction().isShopping();
            tenderButton.setEnabled(enabled && isShopping);
            suspendButton.setEnabled(enabled && !controller.isSuspensionLimitReached());  // NEW
        });

        setupMenuBar();
        setupLayout();
        setupKeyboardShortcuts();
        setupWindowCloseHandler();
        loadItemGrid();


        // Initialize and start cleanup scheduler (7-day retention)
        cleanupScheduler = new SuspensionCleanupScheduler(controller, 7);
        cleanupScheduler.start();

        setupShutdownHook();
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerLocation(450);
        mainSplitPane.setEnabled(false);

        mainSplitPane.setLeftComponent(displayPanel);

        // RIGHT SIDE: Container for everything on the right
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));

        // TOP CONTROLS: Scanner status + Manual UPC (separate from grid)
        JPanel topControlsBar = new JPanel(new BorderLayout());
        topControlsBar.setBackground(Color.WHITE);
        topControlsBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Scanner status (left side)
        JPanel scannerStatusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        scannerStatusPanel.setBackground(Color.WHITE);
        JLabel scannerIcon = new JLabel("⚡ Scanner Ready");
        scannerIcon.setFont(new Font("SansSerif", Font.ITALIC, 11));
        scannerIcon.setForeground(new Color(0, 150, 0));
        scannerStatusPanel.add(scannerIcon);

        // Manual UPC (right side)
        JPanel manualUpcPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        manualUpcPanel.setBackground(Color.WHITE);

        JLabel manualLabel = new JLabel("Manual UPC:");
        manualLabel.setFont(new Font("SansSerif", Font.BOLD, 11));

        manualUpcField = new JTextField(15);
        manualUpcField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        manualUpcField.setToolTipText("Type UPC and press Enter");
        manualUpcField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(3, 5, 3, 5)
        ));
        manualUpcField.addActionListener(e -> {
            String upc = manualUpcField.getText().trim();
            if (!upc.isEmpty()) {
                controller.processUPCScan(upc, InputSource.MANUAL);
                manualUpcField.setText("");
                manualUpcField.requestFocusInWindow();
            }
        });

        manualUpcPanel.add(manualLabel);
        manualUpcPanel.add(manualUpcField);

        topControlsBar.add(scannerStatusPanel, BorderLayout.WEST);
        topControlsBar.add(manualUpcPanel, BorderLayout.EAST);

        // ITEM GRID (center)
        JPanel gridContainer = new JPanel(new BorderLayout());
        gridContainer.add(itemGridPanel, BorderLayout.CENTER);

        // ACTION BUTTONS PANEL (between grid and payment)
        JPanel actionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        actionButtonsPanel.setBackground(Color.WHITE);
        actionButtonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Change Quantity button
        changeQuantityButton = new JButton("Change Quantity");
        changeQuantityButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        changeQuantityButton.setPreferredSize(new Dimension(160, 35));
        changeQuantityButton.setBackground(new Color(70, 130, 180));
        changeQuantityButton.setForeground(Color.BLACK);
        changeQuantityButton.setFocusPainted(false);
        changeQuantityButton.setEnabled(false);
        changeQuantityButton.addActionListener(e -> showChangeQuantityDialog());

        // Suspend Transaction button (NEW)
        suspendButton = new JButton("⏸ Suspend");
        suspendButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        suspendButton.setPreferredSize(new Dimension(160, 35));
        suspendButton.setBackground(new Color(255, 140, 0)); // Dark orange
        suspendButton.setForeground(Color.BLACK);
        suspendButton.setFocusPainted(false);
        suspendButton.setEnabled(false);
        suspendButton.addActionListener(e -> suspendTransaction());

        // Tender Items button
        tenderButton = new JButton("💳 Tender Items");
        tenderButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        tenderButton.setPreferredSize(new Dimension(160, 35));
        tenderButton.setBackground(new Color(34, 139, 34));
        tenderButton.setForeground(Color.BLACK);
        tenderButton.setFocusPainted(false);
        tenderButton.setEnabled(false);
        tenderButton.addActionListener(e -> startTendering());

        actionButtonsPanel.add(changeQuantityButton);
        actionButtonsPanel.add(suspendButton);  // NEW
        actionButtonsPanel.add(tenderButton);

        // PAYMENT PANEL (bottom)
        paymentPanel = new PaymentPanel();
        paymentPanel.setCashExactListener(e -> controller.processExactCashPayment());
        paymentPanel.setCashNextDollarListener(e -> controller.processNextDollarPayment());
        paymentPanel.setCashCustomListener(e -> showCustomCashDialog());
        paymentPanel.setCardPaymentListener(e -> {
            CardType cardType = CardType.valueOf(e.getActionCommand());
            controller.processCardPayment(cardType);
        });
        controller.setPaymentPanel(paymentPanel);

        // Assemble right panel: Controls (top) + Grid (center) + Payment (bottom)
        // Create middle panel for grid + action buttons
        JPanel middlePanel = new JPanel(new BorderLayout());
        middlePanel.add(gridContainer, BorderLayout.CENTER);
        middlePanel.add(actionButtonsPanel, BorderLayout.SOUTH);

        rightPanel.add(topControlsBar, BorderLayout.NORTH);
        rightPanel.add(middlePanel, BorderLayout.CENTER);
        rightPanel.add(paymentPanel, BorderLayout.SOUTH);

        mainSplitPane.setRightComponent(rightPanel);
        add(mainSplitPane, BorderLayout.CENTER);

        // Wire void item button
        displayPanel.setVoidItemButtonListener(e -> voidSelectedItem());

        // Wire selection listener to enable/disable quantity button
        displayPanel.setSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean rowSelected = displayPanel.getSelectedRowIndex() >= 0;
                changeQuantityButton.setEnabled(rowSelected);
            }
        });

        // Wire state change listener
        displayPanel.setStateChangeListener(state -> updateUIForState(state));
    }

    private void loadItemGrid() {
        SwingUtilities.invokeLater(() -> {
            java.util.List<com.am.register.model.Item> items = controller.getAllItems();
            itemGridPanel.loadItems(items);
        });
    }

    /**
     * Suspends the current transaction.
     */
    private void suspendTransaction() {
        if (controller.getCurrentTransaction().getItemCount() == 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Cannot suspend an empty transaction",
                    "No Items",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Check if limit reached
        if (controller.isSuspensionLimitReached()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Maximum suspended transactions reached (10).\n\n" +
                            "Please resume or delete existing suspensions first.",
                    "Limit Reached",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Ask for optional note
        String note = JOptionPane.showInputDialog(
                this,
                "Suspend current transaction?\n\n" +
                        String.format("Items: %d  |  Total: $%.2f\n\n" +
                                        "Optional note (reason for suspension):",
                                controller.getCurrentTransaction().getItemCount(),
                                controller.getCurrentTransaction().getTotal()),
                "Suspend Transaction",
                JOptionPane.QUESTION_MESSAGE
        );

        // User cancelled
        if (note == null) {
            return;
        }

        // Suspend transaction
        String suspensionId = controller.suspendCurrentTransaction(note.trim());

        if (suspensionId != null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Transaction suspended successfully!\n\n" +
                            "Suspension ID: " + suspensionId + "\n" +
                            "Use 'Resume Transaction' to restore it.",
                    "Suspended",
                    JOptionPane.INFORMATION_MESSAGE
            );

            // Update UI for new empty transaction
            updateUIForState(TransactionState.SHOPPING);
        }
    }

    /**
     * Shows dialog to resume a suspended transaction.
     */
    private void resumeTransaction() {
        List<SuspendedTransaction> suspensions = controller.getSuspendedTransactions();

        if (suspensions.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "No suspended transactions available",
                    "None Found",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        // Check if current transaction has items
        if (controller.getCurrentTransaction().getItemCount() > 0) {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "Current transaction has items!\n\n" +
                            "You must suspend or void the current transaction\n" +
                            "before resuming a suspended one.\n\n" +
                            "Suspend current transaction first?",
                    "Current Transaction Active",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (result == JOptionPane.YES_OPTION) {
                suspendTransaction();
                // After suspending, show resume dialog again
                resumeTransaction();
            }
            return;
        }

        // Show suspended transactions dialog
        SuspendedTransactionsDialog dialog = new SuspendedTransactionsDialog(this, suspensions);
        dialog.setVisible(true);

        // Handle result
        if (dialog.isResumed()) {
            String suspensionId = dialog.getSelectedSuspensionId();
            boolean success = controller.resumeSuspendedTransaction(suspensionId);

            if (success) {
                JOptionPane.showMessageDialog(
                        this,
                        "Transaction resumed successfully!\n\n" +
                                "Suspension ID: " + suspensionId,
                        "Resumed",
                        JOptionPane.INFORMATION_MESSAGE
                );

                // Update UI to match restored state
                updateUIForState(controller.getCurrentTransaction().getState());
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Failed to resume transaction",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        } else if (dialog.isDeleted()) {
            String suspensionId = dialog.getSelectedSuspensionId();
            boolean success = controller.deleteSuspension(suspensionId);

            if (success) {
                JOptionPane.showMessageDialog(
                        this,
                        "Suspended transaction deleted",
                        "Deleted",
                        JOptionPane.INFORMATION_MESSAGE
                );

                // Refresh the dialog if there are more suspensions
                if (controller.getSuspendedTransactionCount() > 0) {
                    resumeTransaction();
                }
            }
        }
    }

    /**
     * Shows dialog to change quantity of selected item.
     */
    private void showChangeQuantityDialog() {
        int selectedRow = displayPanel.getSelectedRowIndex();

        if (selectedRow < 0) {
            return; // No selection
        }

        TransactionItem txItem = controller.getCurrentTransaction().getItem(selectedRow);

        if (txItem == null) {
            return;
        }

        String input = JOptionPane.showInputDialog(
                this,
                String.format("Item: %s\nCurrent Quantity: %d\n\nEnter new quantity:",
                        txItem.getDescription(), txItem.getQuantity()),
                "Change Quantity",
                JOptionPane.QUESTION_MESSAGE
        );

        if (input != null && !input.trim().isEmpty()) {
            try {
                int newQuantity = Integer.parseInt(input.trim());

                if (newQuantity < 1) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Quantity must be at least 1.\nUse 'Void Selected Item' to remove.",
                            "Invalid Quantity",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }

                controller.changeItemQuantity(selectedRow, newQuantity);
                displayPanel.clearSelection();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Invalid quantity. Please enter a whole number.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    /**
     * Voids the selected item from transaction.
     */
    private void voidSelectedItem() {
        int selectedRow = displayPanel.getSelectedRowIndex();

        if (selectedRow < 0) {
            return;
        }

        TransactionItem txItem = controller.getCurrentTransaction().getItem(selectedRow);

        if (txItem == null) {
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                String.format("Void this item?\n\n%s\nQuantity: %d\nLine Total: $%.2f",
                        txItem.getDescription(), txItem.getQuantity(), txItem.getLineTotal()),
                "Confirm Void",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            controller.voidItem(selectedRow);
            displayPanel.clearSelection();
        }
    }

    /**
     * Starts the tendering phase.
     */
    private void startTendering() {
        if (controller.getCurrentTransaction().getItemCount() == 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Cannot tender an empty transaction",
                    "No Items",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Transition to tendering state
        controller.startTendering();

        // Update UI state
        updateUIForState(controller.getCurrentTransaction().getState());
    }

    /**
     * Updates all UI components based on transaction state.
     */
    private void updateUIForState(TransactionState state) {
        boolean isShopping = (state == TransactionState.SHOPPING);
        boolean isTendering = (state == TransactionState.TENDERING);

        // Item grid
        itemGridPanel.setEnabled(isShopping);

        // Manual UPC field
        manualUpcField.setEnabled(isShopping);
        if (!isShopping) {
            manualUpcField.setBackground(Color.LIGHT_GRAY);
            manualUpcField.setToolTipText("Disabled during payment");
        } else {
            manualUpcField.setBackground(Color.WHITE);
            manualUpcField.setToolTipText("Type UPC and press Enter");
        }

        // Tender button
        tenderButton.setEnabled(isShopping && controller.getCurrentTransaction().getItemCount() > 0);

        // Suspend button (NEW) - enabled when items exist, regardless of state
        suspendButton.setEnabled(controller.getCurrentTransaction().getItemCount() > 0 &&
                !controller.isSuspensionLimitReached());

        // Change quantity button (only in shopping)
        changeQuantityButton.setEnabled(isShopping && displayPanel.getSelectedRowIndex() >= 0);

        // Payment panel
        if (paymentPanel != null) {
            paymentPanel.setPaymentEnabled(isTendering);
            if (isTendering) {
                Transaction tx = controller.getCurrentTransaction();
                paymentPanel.setTotals(tx.getSubtotal(), tx.getTaxAmount(), tx.getTotal());
            }
        }
    }


    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Transaction Menu
        JMenu transactionMenu = new JMenu("Transaction");
        transactionMenu.setMnemonic(KeyEvent.VK_T);

        JMenuItem clearItem = new JMenuItem("Void Transaction");
        clearItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        clearItem.addActionListener(e -> clearTransaction());

        JMenuItem suspendItem = new JMenuItem("Suspend Transaction");
        suspendItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));  // F3 shortcut
        suspendItem.addActionListener(e -> suspendTransaction());

        JMenuItem resumeItem = new JMenuItem("Resume Transaction...");
        resumeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));  // F4 shortcut
        resumeItem.addActionListener(e -> resumeTransaction());

        transactionMenu.add(suspendItem);   // NEW
        transactionMenu.add(resumeItem);    // NEW
        transactionMenu.addSeparator();
        transactionMenu.add(clearItem);

        // Tools Menu
        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.setMnemonic(KeyEvent.VK_O);

        JMenuItem refreshPopularItem = new JMenuItem("Refresh Popular Items");
        refreshPopularItem.addActionListener(e -> refreshPopularItems());

        JMenuItem salesReportItem = new JMenuItem("View Sales Report");
        salesReportItem.addActionListener(e -> viewSalesReport());

        toolsMenu.add(refreshPopularItem);
        toolsMenu.add(salesReportItem);

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAbout());

        JMenuItem shortcutsItem = new JMenuItem("Keyboard Shortcuts");
        shortcutsItem.addActionListener(e -> showShortcuts());

        helpMenu.add(aboutItem);
        helpMenu.add(shortcutsItem);

        menuBar.add(transactionMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    /**
     * Refreshes popular items based on sales data.
     */
    private void refreshPopularItems() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "Recalculate popular items based on sales data?\n\n" +
                        "This will analyze the last 30 days of transactions\n" +
                        "and update the Popular Items page.",
                "Refresh Popular Items",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            // Run in background to avoid freezing UI
            SwingUtilities.invokeLater(() -> {
                popularityManager.recalculatePopularItems();

                // Reload grid to show updated popular items
                loadItemGrid();

                JOptionPane.showMessageDialog(
                        this,
                        "Popular items refreshed!\n\n" +
                                "The Popular Items page now reflects\n" +
                                "your actual best-selling products.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );
            });
        }
    }

    /**
     * Shows sales report in console.
     */
    private void viewSalesReport() {
        JOptionPane.showMessageDialog(
                this,
                "Sales report printed to console.\n\n" +
                        "Check the console output for detailed sales data.",
                "Sales Report",
                JOptionPane.INFORMATION_MESSAGE
        );

        popularityManager.printSalesReport();
    }

    private void showCustomCashDialog() {
        double total = controller.getCurrentTransaction().getTotal();  // Changed

        String input = JOptionPane.showInputDialog(
                this,
                String.format("Total (with tax): $%.2f\nEnter amount tendered:", total),  // Updated message
                "Custom Cash Amount",
                JOptionPane.QUESTION_MESSAGE
        );

        if (input != null && !input.trim().isEmpty()) {
            try {
                double amount = Double.parseDouble(input.trim());
                controller.processCustomCashPayment(amount);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Invalid amount entered. Please enter a valid number.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void setupKeyboardShortcuts() {
        // F2 - Void transaction
        getRootPane().registerKeyboardAction(
                e -> clearTransaction(),
                KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // F3 - Suspend transaction (NEW)
        getRootPane().registerKeyboardAction(
                e -> suspendTransaction(),
                KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // F4 - Resume transaction (NEW)
        getRootPane().registerKeyboardAction(
                e -> resumeTransaction(),
                KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Ctrl+U - Focus manual UPC
        getRootPane().registerKeyboardAction(
                e -> manualUpcField.requestFocusInWindow(),
                KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private void setupWindowCloseHandler() {
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                confirmAndClose();
            }
        });
    }

    private void clearTransaction() {
        int itemCount = controller.getCurrentTransaction().getItemCount();

        if (itemCount == 0) {
            displayPanel.showInfo("Transaction is already empty");
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                "Clear transaction with " + itemCount + " item(s)?",
                "Confirm Clear",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            controller.startNewTransaction();
            displayPanel.showInfo("Transaction cleared");
        }
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(
                this,
                "Mock Register System\n" +
                        "Version 2.0\n\n" +
                        "Features:\n" +
                        "- Touch/click item selection\n" +
                        "- Global barcode scanner\n" +
                        "- Manual UPC entry\n" +
                        "- Payment processing\n" +
                        "- Receipt generation\n" +
                        "- Category system with popular items",
                "About",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showShortcuts() {
        JOptionPane.showMessageDialog(
                this,
                "Keyboard Shortcuts:\n\n" +
                        "F2 - Void transaction\n" +
                        "F3 - Suspend transaction\n" +
                        "F4 - Resume suspended transaction\n" +
                        "Ctrl+U - Focus manual UPC entry\n" +
                        "\nScanner works globally - just scan!",
                "Keyboard Shortcuts",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void confirmAndClose() {
        int itemCount = controller.getCurrentTransaction().getItemCount();

        String message = itemCount > 0
                ? "There are " + itemCount + " item(s) in the current transaction.\nAre you sure you want to exit?"
                : "Are you sure you want to exit?";

        int result = JOptionPane.showConfirmDialog(
                this,
                message,
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            dispose();
        }
    }

    public DisplayPanel getDisplayPanel() {
        return displayPanel;
    }


    /**
     * Ensures cleanup scheduler is stopped when application closes
     */
    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (cleanupScheduler != null) {
                cleanupScheduler.stop();
            }
        }));
    }
}