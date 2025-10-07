package com.am.register.util;

import com.am.register.model.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates and formats receipts for transactions.
 */
public class ReceiptGenerator {

    private static final String RECEIPTS_DIR = "receipts/";
    private static final int RECEIPT_WIDTH = 50;

    /**
     * Creates a receipt from a completed transaction.
     */
    public static Receipt createReceipt(Transaction transaction) {
        if (!transaction.isPaid()) {
            throw new IllegalStateException("Cannot create receipt for unpaid transaction");
        }

        Receipt receipt = new Receipt();
        receipt.setReceiptNumber(Receipt.generateReceiptNumber());
        receipt.setTimestamp(transaction.getPayment().getPaymentTime());
        receipt.setTransactionItems(transaction.getItems());  // CHANGED
        receipt.setSubtotal(transaction.getSubtotal());
        receipt.setTax(transaction.getTaxAmount());  // NEW
        receipt.setPayment(transaction.getPayment());

        return receipt;
    }

    /**
     * Formats receipt as text.
     */
    public static String formatReceipt(Receipt receipt) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append(centerText("MOCK REGISTER SYSTEM")).append("\n");
        sb.append(centerText("Thank you for your purchase!")).append("\n");
        sb.append(line()).append("\n");

        // Receipt details
        sb.append("Receipt #: ").append(receipt.getReceiptNumber()).append("\n");
        sb.append("Date: ").append(receipt.getFormattedTimestamp()).append("\n");
        sb.append(line()).append("\n");

        // Items header
        sb.append("QTY  ITEM                                    TOTAL\n");
        sb.append(line()).append("\n");

        for (TransactionItem txItem : receipt.getTransactionItems()) {
            String qtyStr = String.format("%2d", txItem.getQuantity());
            String desc = truncate(txItem.getDescription(), RECEIPT_WIDTH - 16);
            String lineTotal = String.format("$%.2f", txItem.getLineTotal());

            // Format: "2x  Coca-Cola 12oz                      $3.98"
            String itemLine = String.format("%sx  %-" + (RECEIPT_WIDTH - 12) + "s %6s",
                    qtyStr, desc, lineTotal);
            sb.append(itemLine).append("\n");
        }

        // Subtotal
        sb.append(leftRightText("SUBTOTAL:",
                String.format("$%.2f", receipt.getSubtotal()))).append("\n");

        // Tax Breakdown
        TaxBreakdown breakdown = calculateReceiptTaxBreakdown(receipt);

        if (breakdown.hasMultipleTaxRates()) {
            sb.append("\n");
            breakdown.getCategoryTaxes().values().stream()
                    .sorted((a, b) -> a.getCategory().compareTo(b.getCategory()))
                    .forEach(catTax -> {
                        String label = String.format("  %s Tax (%s):",
                                catTax.getCategory(), catTax.getFormattedRate());
                        sb.append(leftRightText(label,
                                String.format("$%.2f", catTax.getTaxAmount()))).append("\n");
                    });
            sb.append("\n");
        }

        // Total Tax
        sb.append(leftRightText("TOTAL TAX:",
                String.format("$%.2f", receipt.getTax()))).append("\n");

        // Discount (if any)
        if (receipt.getDiscount() > 0) {
            sb.append(leftRightText("DISCOUNT:",
                    String.format("-$%.2f", receipt.getDiscount()))).append("\n");
        }

        // Discount (if any)
        if (receipt.getDiscount() > 0) {
            sb.append(leftRightText("DISCOUNT:",
                    String.format("-$%.2f", receipt.getDiscount()))).append("\n");
        }

        // Total
        sb.append(line()).append("\n");
        sb.append(leftRightText("TOTAL:",
                String.format("$%.2f", receipt.getTotal()))).append("\n");

        // Payment info
        Payment payment = receipt.getPayment();
        sb.append("\nPayment Method: ").append(payment.getMethod().getDisplayName()).append("\n");

        if (payment.isCash()) {
            sb.append(leftRightText("Tendered:",
                    String.format("$%.2f", payment.getAmountTendered()))).append("\n");
            sb.append(leftRightText("Change:",
                    String.format("$%.2f", payment.getChangeAmount()))).append("\n");
        } else if (payment.isCard()) {
            sb.append("Card Type: ").append(payment.getCardType().getDisplayName()).append("\n");
            sb.append("Transaction Approved\n");
        }

        sb.append(line()).append("\n");

        // Footer
        sb.append("\n");
        sb.append(centerText("Items Sold: " + receipt.getTransactionItems().size())).append("\n");
        sb.append("\n");
        sb.append(centerText("Visit us again soon!")).append("\n");
        sb.append(centerText("www.mockregister.com")).append("\n");

        return sb.toString();
    }

    /**
     * Calculates tax breakdown for a receipt.
     */
    private static TaxBreakdown calculateReceiptTaxBreakdown(Receipt receipt) {
        TaxBreakdown breakdown = new TaxBreakdown();

        // Group items by category
        Map<String, Double> categorySubtotals = new HashMap<>();

        for (TransactionItem txItem : receipt.getTransactionItems()) {
            String category = txItem.getItem().getCategory();
            double lineTotal = txItem.getLineTotal();

            categorySubtotals.put(category,
                    categorySubtotals.getOrDefault(category, 0.0) + lineTotal);
        }

        // Calculate tax for each category
        for (Map.Entry<String, Double> entry : categorySubtotals.entrySet()) {
            String category = entry.getKey();
            double categorySubtotal = entry.getValue();
            double taxRate = TaxBreakdown.getTaxRateForCategory(category);

            breakdown.addCategoryTax(category, categorySubtotal, taxRate);
        }

        return breakdown;
    }

    /**
     * Saves receipt to file.
     */
    public static boolean saveReceipt(Receipt receipt) {
        // Create receipts directory if it doesn't exist
        File dir = new File(RECEIPTS_DIR);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                System.err.println("Failed to create receipts directory");
                return false;
            }
        }

        // Generate filename
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String filename = RECEIPTS_DIR + "receipt_" +
                receipt.getTimestamp().format(formatter) + ".txt";

        // Write to file
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(formatReceipt(receipt));
            System.out.println("Receipt saved: " + filename);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to save receipt: " + e.getMessage());
            return false;
        }
    }

    /**
     * Centers text within receipt width.
     */
    private static String centerText(String text) {
        if (text.length() >= RECEIPT_WIDTH) {
            return text;
        }
        int padding = (RECEIPT_WIDTH - text.length()) / 2;
        return " ".repeat(padding) + text;
    }

    /**
     * Formats text with left and right alignment.
     */
    private static String leftRightText(String left, String right) {
        int leftLen = left.length();
        int rightLen = right.length();
        int spaces = RECEIPT_WIDTH - leftLen - rightLen;

        if (spaces < 1) {
            spaces = 1;
        }

        return left + " ".repeat(spaces) + right;
    }

    /**
     * Creates a horizontal line.
     */
    private static String line() {
        return "-".repeat(RECEIPT_WIDTH);
    }

    /**
     * Truncates text to specified length.
     */
    private static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}