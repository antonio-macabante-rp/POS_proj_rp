package com.am.register.view;

import com.am.register.model.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;  // ADD THIS (for cell renderers)
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Display panel showing the current transaction.
 * Shows scanned items in a table and running subtotal.
 */
public class DisplayPanel extends JPanel {

    private final JTable itemsTable;
    private final DefaultTableModel tableModel;
    private final JLabel subtotalLabel;
    private final JButton clearButton;
    private final JButton voidItemButton;
    private java.util.function.Consumer<Boolean> paymentEnabledCallback;
    private java.util.function.Consumer<TransactionState> stateChangeListener;
    private final JLabel taxLabel;
    private final JLabel totalLabel;
    private JPanel taxBreakdownPanel;

    /**
     * Sets listener for transaction state changes.
     */
    public void setStateChangeListener(java.util.function.Consumer<TransactionState> listener) {
        this.stateChangeListener = listener;
        this.taxBreakdownPanel = taxBreakdownPanel;  // Store reference
    }

    public DisplayPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // UPDATED: 5 columns instead of 2
        String[] columnNames = {"#", "Description", "Qty", "Price", "Line Total"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 2) return Integer.class;
                return String.class;
            }
        };

        // Create table
        itemsTable = new JTable(tableModel);
        itemsTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        itemsTable.setRowHeight(28);
        itemsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 16));
        itemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);  // NEW: Enable selection
        itemsTable.setGridColor(Color.LIGHT_GRAY);

        setupSelectionListener();  // ADD THIS LINE

        // Set column widths
        itemsTable.getColumnModel().getColumn(0).setPreferredWidth(40);  // #
        itemsTable.getColumnModel().getColumn(1).setPreferredWidth(220); // Description
        itemsTable.getColumnModel().getColumn(2).setPreferredWidth(50);  // Qty
        itemsTable.getColumnModel().getColumn(3).setPreferredWidth(70);  // Price
        itemsTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // Line Total

        // Center align numeric columns
        javax.swing.table.DefaultTableCellRenderer centerRenderer =
                new javax.swing.table.DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        itemsTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        itemsTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        // Right align price columns
        javax.swing.table.DefaultTableCellRenderer rightRenderer =
                new javax.swing.table.DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        itemsTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        itemsTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

        // Add table to scroll pane
        JScrollPane scrollPane = new JScrollPane(itemsTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Transaction Items"));

        // Bottom panel (keep existing code)
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bottomPanel.setPreferredSize(new Dimension(0, 180));

        // Right panel with subtotal, tax breakdown, and total
        JPanel totalsPanel = new JPanel();
        totalsPanel.setLayout(new BoxLayout(totalsPanel, BoxLayout.Y_AXIS));
        totalsPanel.setBackground(Color.WHITE);
        totalsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        totalsPanel.setPreferredSize(new Dimension(250, 160));  // ← ADD THIS
        totalsPanel.setMinimumSize(new Dimension(250, 160));

        // Subtotal row
        JPanel subtotalRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
        subtotalRow.setBackground(Color.WHITE);
        JLabel subtotalLabelText = new JLabel("Subtotal:");
        subtotalLabelText.setFont(new Font("SansSerif", Font.BOLD, 14));
        subtotalLabel = new JLabel("$0.00");
        subtotalLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        subtotalLabel.setForeground(Color.BLACK);
        subtotalRow.add(subtotalLabelText);
        subtotalRow.add(subtotalLabel);

        // Tax breakdown panel (will be populated dynamically)
        taxBreakdownPanel = new JPanel();
        taxBreakdownPanel.setLayout(new BoxLayout(taxBreakdownPanel, BoxLayout.Y_AXIS));
        taxBreakdownPanel.setBackground(Color.WHITE);

        // Total tax row
        JPanel taxRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
        taxRow.setBackground(Color.WHITE);
        JLabel taxLabelText = new JLabel("Total Tax:");
        taxLabelText.setFont(new Font("SansSerif", Font.PLAIN, 13));
        taxLabel = new JLabel("$0.00");
        taxLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        taxLabel.setForeground(Color.DARK_GRAY);
        taxRow.add(taxLabelText);
        taxRow.add(taxLabel);

        // Separator
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);

        // Total row
        JPanel totalRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
        totalRow.setBackground(Color.WHITE);
        JLabel totalLabelText = new JLabel("TOTAL:");
        totalLabelText.setFont(new Font("SansSerif", Font.BOLD, 18));
        totalLabel = new JLabel("$0.00");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        totalLabel.setForeground(new Color(0, 100, 0));
        totalRow.add(totalLabelText);
        totalRow.add(totalLabel);

        // Assemble totals panel
        totalsPanel.add(subtotalRow);
        totalsPanel.add(Box.createRigidArea(new Dimension(0, 3)));
        totalsPanel.add(taxBreakdownPanel);  // Dynamic tax breakdown goes here
        totalsPanel.add(Box.createRigidArea(new Dimension(0, 3)));
        totalsPanel.add(taxRow);
        totalsPanel.add(Box.createRigidArea(new Dimension(0, 3)));
        totalsPanel.add(separator);
        totalsPanel.add(Box.createRigidArea(new Dimension(0, 3)));
        totalsPanel.add(totalRow);

        totalRow.add(totalLabelText);
        totalRow.add(totalLabel);

        // Add rows to totals panel
        totalsPanel.add(subtotalRow);
        totalsPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        totalsPanel.add(taxRow);
        totalsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        totalsPanel.add(separator);
        totalsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        totalsPanel.add(totalRow);

        // Button panel with Void and Clear buttons
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 5, 5));  // 2 rows, 1 column
        buttonPanel.setBackground(Color.WHITE);

        // Void Item button (top)
        voidItemButton = new JButton("Void Selected Item");
        voidItemButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        voidItemButton.setBackground(new Color(255, 150, 0)); // Orange
        voidItemButton.setForeground(Color.BLACK);
        voidItemButton.setFocusPainted(false);
        voidItemButton.setEnabled(false); // Disabled until row selected

        // Clear Transaction button (bottom) - rename to Void Transaction
        clearButton = new JButton("Void Transaction");  // RENAMED
        clearButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        clearButton.setBackground(new Color(220, 20, 60)); // Crimson red
        clearButton.setForeground(Color.BLACK);
        clearButton.setFocusPainted(false);
        clearButton.setEnabled(false);

        buttonPanel.add(voidItemButton);
        buttonPanel.add(clearButton);

        bottomPanel.add(buttonPanel, BorderLayout.WEST);
        bottomPanel.add(totalsPanel, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void setPaymentEnabledCallback(java.util.function.Consumer<Boolean> callback) {
        this.paymentEnabledCallback = callback;
    }

    /**
     * Sets the action listener for the clear button.
     * @param listener The action listener to handle clear button clicks
     */
    public void setClearButtonListener(ActionListener listener) {
        // Remove existing listeners first
        for (ActionListener al : clearButton.getActionListeners()) {
            clearButton.removeActionListener(al);
        }
        clearButton.addActionListener(listener);
    }

    /**
     * Updates the display with current transaction data.
     * Called by controller when transaction changes.
     * @param transaction The current transaction
     */
    public void updateDisplay(Transaction transaction) {
        // Clear existing rows
        tableModel.setRowCount(0);

        // Add items with quantities
        int rowNum = 1;
        for (TransactionItem txItem : transaction.getItems()) {
            Object[] row = {
                    rowNum++,
                    txItem.getDescription(),
                    txItem.getQuantity(),
                    String.format("$%.2f", txItem.getUnitPrice()),
                    String.format("$%.2f", txItem.getLineTotal())
            };
            tableModel.addRow(row);
        }

        boolean hasItems = transaction.getItemCount() > 0;
        clearButton.setEnabled(hasItems);

        if (paymentEnabledCallback != null) {
            paymentEnabledCallback.accept(hasItems && !transaction.isPaid());
        }

        // Update financial display with tax breakdown
        double subtotal = transaction.getSubtotal();
        double tax = transaction.getTaxAmount();
        double total = transaction.getTotal();

        subtotalLabel.setText(String.format("$%.2f", subtotal));
        taxLabel.setText(String.format("$%.2f", tax));
        totalLabel.setText(String.format("$%.2f", total));

        // Update tax breakdown
        updateTaxBreakdown(transaction);

        // Notify state change
        if (stateChangeListener != null) {
            stateChangeListener.accept(transaction.getState());
        }

        // Scroll to bottom
        if (tableModel.getRowCount() > 0) {
            itemsTable.scrollRectToVisible(
                    itemsTable.getCellRect(tableModel.getRowCount() - 1, 0, true)
            );
        }
    }

    /**
     * Updates the tax breakdown display.
     */
    private void updateTaxBreakdown(Transaction transaction) {
        this.taxBreakdownPanel.removeAll();

        if (transaction.getItemCount() == 0) {
            taxBreakdownPanel.revalidate();
            taxBreakdownPanel.repaint();
            return;
        }

        TaxBreakdown breakdown = transaction.calculateTaxBreakdown();

        // Only show breakdown if there are special tax rates
        if (breakdown.hasMultipleTaxRates()) {
            // Sort categories for consistent display
            breakdown.getCategoryTaxes().values().stream()
                    .sorted((a, b) -> a.getCategory().compareTo(b.getCategory()))
                    .forEach(catTax -> {
                        JPanel catRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
                        catRow.setBackground(Color.WHITE);

                        JLabel catLabel = new JLabel(String.format("  %s (%s):",
                                catTax.getCategory(), catTax.getFormattedRate()));
                        catLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
                        catLabel.setForeground(Color.GRAY);

                        JLabel catTaxLabel = new JLabel(String.format("$%.2f", catTax.getTaxAmount()));
                        catTaxLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
                        catTaxLabel.setForeground(Color.GRAY);

                        catRow.add(catLabel);
                        catRow.add(catTaxLabel);

                        taxBreakdownPanel.add(catRow);
                    });
        }

        taxBreakdownPanel.revalidate();
        taxBreakdownPanel.repaint();
    }

    /**
     * Sets up row selection listener to enable/disable void button.
     */
    private void setupSelectionListener() {
        itemsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean rowSelected = itemsTable.getSelectedRow() >= 0;
                voidItemButton.setEnabled(rowSelected);
            }
        });
    }

    /**
     * Sets the action listener for the void item button.
     */
    public void setVoidItemButtonListener(java.awt.event.ActionListener listener) {
        for (java.awt.event.ActionListener al : voidItemButton.getActionListeners()) {
            voidItemButton.removeActionListener(al);
        }
        voidItemButton.addActionListener(listener);
    }

    /**
     * Gets the currently selected row index.
     * @return Selected row index, or -1 if none selected
     */
    public int getSelectedRowIndex() {
        return itemsTable.getSelectedRow();
    }

    /**
     * Sets a listener for row selection changes.
     */
    public void setSelectionListener(javax.swing.event.ListSelectionListener listener) {
        itemsTable.getSelectionModel().addListSelectionListener(listener);
    }

    /**
     * Clears row selection.
     */
    public void clearSelection() {
        itemsTable.clearSelection();
    }

    /**
     * Shows an error message in a dialog.
     * @param message The error message to display
     */
    public void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Shows an information message in a dialog.
     * @param message The message to display
     */
    public void showInfo(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Information",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}