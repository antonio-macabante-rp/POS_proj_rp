package com.am.register.view;

import com.am.register.model.Receipt;
import com.am.register.util.ReceiptGenerator;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for displaying receipts - DYNAMIC SIZING
 */
public class ReceiptDialog extends JDialog {

    private final Receipt receipt;

    public ReceiptDialog(Frame parent, Receipt receipt) {
        super(parent, "Receipt - " + receipt.getReceiptNumber(), true);
        this.receipt = receipt;

        // Don't set fixed size - let it calculate based on content
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setupUI();

        // Size based on content
        pack();

        // Set reasonable bounds
        Dimension size = getSize();
        if (size.width < 500) setSize(500, size.height);
        if (size.width > 700) setSize(700, size.height);
        if (size.height < 400) setSize(size.width, 400);
        if (size.height > 900) setSize(size.width, 900);

        // Re-center after sizing
        setLocationRelativeTo(parent);
    }

    private void setupUI() {
        setLayout(new BorderLayout(10, 10));

        // Receipt content in text area
        JTextArea receiptArea = new JTextArea();
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        receiptArea.setText(ReceiptGenerator.formatReceipt(receipt));
        receiptArea.setEditable(false);
        receiptArea.setCaretPosition(0);

        // Calculate preferred rows based on content
        int lineCount = receiptArea.getLineCount();
        receiptArea.setRows(Math.min(lineCount + 2, 50)); // Max 50 rows visible
        receiptArea.setColumns(52); // Width for 50-char receipts + margin

        JScrollPane scrollPane = new JScrollPane(receiptArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Receipt"));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton printButton = new JButton("Print");
        printButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        printButton.setPreferredSize(new Dimension(120, 40));
        printButton.setEnabled(false);
        printButton.setToolTipText("Print functionality - Coming soon");

        JButton saveButton = new JButton("Save Copy");
        saveButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        saveButton.setPreferredSize(new Dimension(120, 40));
        saveButton.addActionListener(e -> saveReceipt());

        JButton closeButton = new JButton("Close");
        closeButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        closeButton.setPreferredSize(new Dimension(120, 40));
        closeButton.addActionListener(e -> dispose());

        buttonPanel.add(printButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(closeButton);

        // Add components
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void saveReceipt() {
        boolean saved = ReceiptGenerator.saveReceipt(receipt);

        if (saved) {
            JOptionPane.showMessageDialog(
                    this,
                    "Receipt saved successfully to receipts/ folder",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to save receipt. Check console for details.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}