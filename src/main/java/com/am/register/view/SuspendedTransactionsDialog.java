package com.am.register.view;

import com.am.register.model.SuspendedTransaction;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Dialog for viewing and managing suspended transactions.
 */
public class SuspendedTransactionsDialog extends JDialog {

    private final List<SuspendedTransaction> suspensions;
    private final JTable suspensionsTable;
    private final DefaultTableModel tableModel;
    private String selectedSuspensionId = null;
    private boolean resumed = false;
    private boolean deleted = false;

    public SuspendedTransactionsDialog(Frame parent, List<SuspendedTransaction> suspensions) {
        super(parent, "Suspended Transactions", true);
        this.suspensions = suspensions;

        setSize(700, 400);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Create table model
        String[] columnNames = {"ID", "Items", "Total", "State", "Time", "Note"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Create table
        suspensionsTable = new JTable(tableModel);
        suspensionsTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        suspensionsTable.setRowHeight(25);
        suspensionsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        suspensionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Set column widths
        suspensionsTable.getColumnModel().getColumn(0).setPreferredWidth(140); // ID
        suspensionsTable.getColumnModel().getColumn(1).setPreferredWidth(60);  // Items
        suspensionsTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Total
        suspensionsTable.getColumnModel().getColumn(3).setPreferredWidth(90);  // State
        suspensionsTable.getColumnModel().getColumn(4).setPreferredWidth(120); // Time
        suspensionsTable.getColumnModel().getColumn(5).setPreferredWidth(200); // Note

        // Center align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        suspensionsTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);

        // Right align total
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        suspensionsTable.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);

        // Populate table
        populateTable();

        // Create UI
        setupUI();
    }

    private void populateTable() {
        tableModel.setRowCount(0);

        if (suspensions.isEmpty()) {
            return;
        }

        for (SuspendedTransaction suspension : suspensions) {
            Object[] row = {
                    suspension.getSuspensionId(),
                    suspension.getItemCount(),
                    String.format("$%.2f", suspension.getTotal()),
                    suspension.getTransactionState(),
                    suspension.getTimeAgo(),
                    suspension.getNote() != null ? suspension.getNote() : ""
            };
            tableModel.addRow(row);
        }
    }

    private void setupUI() {
        setLayout(new BorderLayout(10, 10));

        // Title panel
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(70, 130, 180));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel("Suspended Transactions (" + suspensions.size() + ")");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(Color.BLACK);
        titlePanel.add(titleLabel);

        // Table in scroll pane
        JScrollPane scrollPane = new JScrollPane(suspensionsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Empty state message
        if (suspensions.isEmpty()) {
            JPanel emptyPanel = new JPanel(new GridBagLayout());
            JLabel emptyLabel = new JLabel("No suspended transactions");
            emptyLabel.setFont(new Font("SansSerif", Font.ITALIC, 16));
            emptyLabel.setForeground(Color.GRAY);
            emptyPanel.add(emptyLabel);

            add(titlePanel, BorderLayout.NORTH);
            add(emptyPanel, BorderLayout.CENTER);
            add(createButtonPanel(), BorderLayout.SOUTH);
            return;
        }

        // Button panel
        JPanel buttonPanel = createButtonPanel();

        add(titlePanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton resumeButton = new JButton("Resume Selected");
        resumeButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        resumeButton.setPreferredSize(new Dimension(150, 40));
        resumeButton.setBackground(new Color(34, 139, 34));
        resumeButton.setForeground(Color.BLACK);
        resumeButton.setFocusPainted(false);
        resumeButton.setEnabled(!suspensions.isEmpty());
        resumeButton.addActionListener(e -> resumeSelected());

        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        deleteButton.setPreferredSize(new Dimension(150, 40));
        deleteButton.setBackground(new Color(220, 20, 60));
        deleteButton.setForeground(Color.BLACK);
        deleteButton.setFocusPainted(false);
        deleteButton.setEnabled(!suspensions.isEmpty());
        deleteButton.addActionListener(e -> deleteSelected());

        JButton closeButton = new JButton("Close");
        closeButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        closeButton.setPreferredSize(new Dimension(150, 40));
        closeButton.addActionListener(e -> dispose());

        buttonPanel.add(resumeButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(closeButton);

        return buttonPanel;
    }

    private void resumeSelected() {
        int selectedRow = suspensionsTable.getSelectedRow();

        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a transaction to resume",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        selectedSuspensionId = (String) tableModel.getValueAt(selectedRow, 0);
        resumed = true;
        dispose();
    }

    private void deleteSelected() {
        int selectedRow = suspensionsTable.getSelectedRow();

        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a transaction to delete",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String suspensionId = (String) tableModel.getValueAt(selectedRow, 0);

        int result = JOptionPane.showConfirmDialog(
                this,
                "Permanently delete this suspended transaction?\n\n" +
                        "ID: " + suspensionId + "\n" +
                        "This action cannot be undone.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            selectedSuspensionId = suspensionId;
            deleted = true;
            dispose();
        }
    }

    /**
     * Checks if user chose to resume a transaction.
     */
    public boolean isResumed() {
        return resumed;
    }

    /**
     * Checks if user chose to delete a transaction.
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * Gets the selected suspension ID.
     */
    public String getSelectedSuspensionId() {
        return selectedSuspensionId;
    }
}