package com.am.register.view;

import com.am.register.model.Item;
import com.am.register.model.Transaction;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
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

    /**
     * Creates the display panel.
     */
    public DisplayPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create table model with columns
        String[] columnNames = {"Description", "Price"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };

        // Create table
        itemsTable = new JTable(tableModel);
        itemsTable.setFont(new Font("SansSerif", Font.PLAIN, 16));
        itemsTable.setRowHeight(30);
        itemsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 18));

        // Set column widths
        itemsTable.getColumnModel().getColumn(0).setPreferredWidth(400); // Description
        itemsTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Price

        // Add table to scroll pane
        JScrollPane scrollPane = new JScrollPane(itemsTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Scanned Items"));

        // Create bottom panel with subtotal and button
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Subtotal panel
        JPanel subtotalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JLabel subtotalLabelText = new JLabel("Subtotal: ");
        subtotalLabelText.setFont(new Font("SansSerif", Font.BOLD, 24));

        subtotalLabel = new JLabel("$0.00");
        subtotalLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        subtotalLabel.setForeground(new Color(0, 100, 0)); // Dark green

        subtotalPanel.add(subtotalLabelText);
        subtotalPanel.add(subtotalLabel);

        // Clear button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        clearButton = new JButton("Clear Transaction");
        clearButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        clearButton.setPreferredSize(new Dimension(200, 50));
        buttonPanel.add(clearButton);

        // Add to bottom panel
        bottomPanel.add(buttonPanel, BorderLayout.WEST);
        bottomPanel.add(subtotalPanel, BorderLayout.EAST);

        // Add components to main panel
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
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

        // Add items to table
        for (Item item : transaction.getScannedItems()) {
            Object[] row = {
                    item.getDescription(),
                    String.format("$%.2f", item.getPrice())
            };
            tableModel.addRow(row);
        }

        // Update subtotal
        double subtotal = transaction.getSubtotal();
        subtotalLabel.setText(String.format("$%.2f", subtotal));

        // Scroll to bottom to show most recent item
        if (tableModel.getRowCount() > 0) {
            itemsTable.scrollRectToVisible(
                    itemsTable.getCellRect(tableModel.getRowCount() - 1, 0, true)
            );
        }
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