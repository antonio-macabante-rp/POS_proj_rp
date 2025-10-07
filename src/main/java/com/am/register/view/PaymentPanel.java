package com.am.register.view;

import com.am.register.model.CardType;
import com.am.register.model.PaymentMethod;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Panel for handling payment processing.
 * Supports cash and card payments.
 */
public class PaymentPanel extends JPanel {

    private final JButton cashButton;
    private final JButton cardButton;
    private final JPanel paymentOptionsPanel;
    private final JLabel statusLabel;

    private ActionListener cashExactListener;
    private ActionListener cashNextDollarListener;
    private ActionListener cashCustomListener;
    private ActionListener cardPaymentListener;

    private double currentSubtotal = 0.0;  // Keep this
    private double currentTotal = 0.0;  // Changed from currentSubtotal
    private double currentTax = 0.0;    // NEW

    public PaymentPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2),
                "Payment Processing",
                0, 0,
                new Font("SansSerif", Font.BOLD, 16)
        ));
        setPreferredSize(new Dimension(0, 180));

        // Top panel - Payment method selection
        JPanel methodPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        methodPanel.setBackground(Color.WHITE);

        cashButton = new JButton("Cash Payment");
        cashButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        cashButton.setPreferredSize(new Dimension(150, 40));
        cashButton.setBackground(new Color(100, 200, 100));
        cashButton.setFocusPainted(false);
        cashButton.addActionListener(e -> showCashOptions());

        cardButton = new JButton("Card Payment");
        cardButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        cardButton.setPreferredSize(new Dimension(150, 40));
        cardButton.setBackground(new Color(100, 150, 250));
        cardButton.setFocusPainted(false);
        cardButton.addActionListener(e -> showCardOptions());

        methodPanel.add(cashButton);
        methodPanel.add(cardButton);

        // Center panel - Payment options (dynamic)
        paymentOptionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        paymentOptionsPanel.setBackground(Color.WHITE);
        paymentOptionsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Bottom panel - Status
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        statusPanel.setBackground(Color.WHITE);
        statusLabel = new JLabel("Select payment method");
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        statusLabel.setForeground(Color.GRAY);
        statusPanel.add(statusLabel);

        // Add panels
        add(methodPanel, BorderLayout.NORTH);
        add(paymentOptionsPanel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);

        // Initially disable payment buttons
        setPaymentEnabled(false);
    }

    /**
     * Shows cash payment options.
     */
    private void showCashOptions() {
        paymentOptionsPanel.removeAll();

        JButton exactButton = new JButton("Exact Dollar");
        exactButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        exactButton.setPreferredSize(new Dimension(130, 35));
        exactButton.addActionListener(e -> {
            if (cashExactListener != null) {
                cashExactListener.actionPerformed(e);
            }
        });

        JButton nextDollarButton = new JButton("Next Dollar");
        nextDollarButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        nextDollarButton.setPreferredSize(new Dimension(130, 35));
        nextDollarButton.addActionListener(e -> {
            if (cashNextDollarListener != null) {
                cashNextDollarListener.actionPerformed(e);
            }
        });

        JButton customButton = new JButton("Enter Amount");
        customButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        customButton.setPreferredSize(new Dimension(130, 35));
        customButton.addActionListener(e -> {
            if (cashCustomListener != null) {
                cashCustomListener.actionPerformed(e);
            }
        });

        paymentOptionsPanel.add(exactButton);
        paymentOptionsPanel.add(nextDollarButton);
        paymentOptionsPanel.add(customButton);

        statusLabel.setText("Select cash payment option");

        paymentOptionsPanel.revalidate();
        paymentOptionsPanel.repaint();
    }

    /**
     * Shows card payment options.
     */
    private void showCardOptions() {
        // Show card type selection dialog
        Object[] cardTypes = {
                CardType.VISA,
                CardType.MASTERCARD,
                CardType.AMERICAN_EXPRESS,
                CardType.DISCOVER,
                CardType.OTHER
        };

        CardType selectedType = (CardType) JOptionPane.showInputDialog(
                this,
                "Select card type:",
                "Card Payment",
                JOptionPane.QUESTION_MESSAGE,
                null,
                cardTypes,
                CardType.VISA
        );

        if (selectedType != null && cardPaymentListener != null) {
            // Store card type and trigger payment
            ActionListener listener = cardPaymentListener;
            // We'll pass the card type through the action command
            listener.actionPerformed(new java.awt.event.ActionEvent(
                    this,
                    java.awt.event.ActionEvent.ACTION_PERFORMED,
                    selectedType.name()
            ));
        }
    }

    /**
     * Sets the current transaction totals.
     */
    public void setTotals(double subtotal, double tax, double total) {
        this.currentSubtotal = subtotal;
        this.currentTax = tax;
        this.currentTotal = total;
    }

    // Keep old method for compatibility but update it
    public void setSubtotal(double subtotal) {
        this.currentSubtotal = subtotal;
        this.currentTax = subtotal * 0.07;
        this.currentTotal = subtotal + currentTax;
    }

    /**
     * Enables or disables payment buttons.
     */
    public void setPaymentEnabled(boolean enabled) {
        cashButton.setEnabled(enabled);
        cardButton.setEnabled(enabled);
        if (!enabled) {
            paymentOptionsPanel.removeAll();
            statusLabel.setText(enabled ? "Select payment method" : "Add items to enable payment");
            paymentOptionsPanel.revalidate();
            paymentOptionsPanel.repaint();
        }
    }

    /**
     * Sets the status message.
     */
    public void setStatus(String message) {
        statusLabel.setText(message);
    }

    /**
     * Resets the payment panel.
     */
    public void reset() {
        paymentOptionsPanel.removeAll();
        statusLabel.setText("Select payment method");
        setPaymentEnabled(false);
        paymentOptionsPanel.revalidate();
        paymentOptionsPanel.repaint();
    }

    // Setters for action listeners
    public void setCashExactListener(ActionListener listener) {
        this.cashExactListener = listener;
    }

    public void setCashNextDollarListener(ActionListener listener) {
        this.cashNextDollarListener = listener;
    }

    public void setCashCustomListener(ActionListener listener) {
        this.cashCustomListener = listener;
    }

    public void setCardPaymentListener(ActionListener listener) {
        this.cardPaymentListener = listener;
    }

    /**
     * Gets the current subtotal.
     */
    public double getCurrentSubtotal() {
        return currentSubtotal;
    }
}