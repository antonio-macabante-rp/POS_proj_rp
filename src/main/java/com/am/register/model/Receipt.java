package com.am.register.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.am.register.model.TransactionItem;  // ADD THIS

/**
 * Represents a transaction receipt.
 */
@Data
public class Receipt {

    private String receiptNumber;
    private LocalDateTime timestamp;
    private List<TransactionItem> transactionItems;  // CHANGED from List<Item>
    private double subtotal;
    private double tax;      // NEW
    private Payment payment;
    private double discount = 0.0;
    private String promoCode = null;

    /**
     * Generates a unique receipt number based on timestamp.
     */
    public static String generateReceiptNumber() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return "R" + LocalDateTime.now().format(formatter);
    }

    /**
     * Gets formatted timestamp.
     */
    public String getFormattedTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return timestamp.format(formatter);
    }

    /**
     * Gets total (subtotal - discount for now).
     */
    public double getTotal() {
        return subtotal - discount;
    }
}