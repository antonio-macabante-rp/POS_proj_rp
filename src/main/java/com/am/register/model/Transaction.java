package com.am.register.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a transaction with quantity-aware items.
 */
@NoArgsConstructor
public class Transaction {

    // Tax rate constant
    public static final double TAX_RATE = 0.07;  // 7%

    @Getter
    private List<TransactionItem> items = new ArrayList<>();  // CHANGED: TransactionItem instead of Item

    @Getter
    private Payment payment = new Payment();

    @Getter
    private TransactionState state = TransactionState.SHOPPING;  // NEW

    /**
     * Adds an item to the transaction.
     * If item already exists, increments its quantity.
     * @param item The item to add
     */
    public void addItem(Item item) {
        if (item == null) {
            return;
        }

        // Check if item already exists in transaction
        for (TransactionItem txItem : items) {
            if (txItem.getUpc().equals(item.getUpc())) {
                // Item exists - increment quantity
                txItem.incrementQuantity();
                return;
            }
        }

        // Item doesn't exist - add new TransactionItem
        items.add(new TransactionItem(item));
    }

    /**
     * Gets all scanned items (for backward compatibility).
     * Expands quantities into individual items.
     * @deprecated Use getItems() instead for quantity-aware access
     */
    @Deprecated
    public List<Item> getScannedItems() {
        List<Item> expandedItems = new ArrayList<>();
        for (TransactionItem txItem : items) {
            for (int i = 0; i < txItem.getQuantity(); i++) {
                expandedItems.add(txItem.getItem());
            }
        }
        return expandedItems;
    }

    /**
     * Calculates subtotal from all line totals.
     */
    public double getSubtotal() {
        double total = 0.0;
        for (TransactionItem txItem : items) {
            total += txItem.getLineTotal();
        }
        return total;
    }

    /**
     * Calculates tax breakdown by category.
     */
    public TaxBreakdown calculateTaxBreakdown() {
        TaxBreakdown breakdown = new TaxBreakdown();

        // Group items by category and calculate subtotals
        Map<String, Double> categorySubtotals = new HashMap<>();

        for (TransactionItem txItem : items) {
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
     * Calculates total tax amount.
     */
    public double getTaxAmount() {
        return calculateTaxBreakdown().getTotalTax();
    }

    /**
     * Calculates total (subtotal + tax).
     */
    public double getTotal() {
        return getSubtotal() + getTaxAmount();
    }

    /**
     * Gets total number of items (sum of all quantities).
     */
    public int getItemCount() {
        int count = 0;
        for (TransactionItem txItem : items) {
            count += txItem.getQuantity();
        }
        return count;
    }

    /**
     * Gets number of unique line items (rows in basket).
     */
    public int getLineCount() {
        return items.size();
    }

    /**
     * Removes an item at the specified index.
     * @param index The index in the items list
     */
    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
        }
    }

    /**
     * Changes the quantity of an item at the specified index.
     * @param index The index in the items list
     * @param newQuantity The new quantity (must be >= 1)
     */
    public void changeQuantity(int index, int newQuantity) {
        if (index >= 0 && index < items.size() && newQuantity >= 1) {
            items.get(index).setQuantity(newQuantity);
        }
    }

    /**
     * Gets a TransactionItem at the specified index.
     */
    public TransactionItem getItem(int index) {
        if (index >= 0 && index < items.size()) {
            return items.get(index);
        }
        return null;
    }

    public void startTendering() {
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot tender empty transaction");
        }
        state = TransactionState.TENDERING;
    }

    /**
     * Checks if transaction is in shopping state.
     */
    public boolean isShopping() {
        return state == TransactionState.SHOPPING;
    }

    /**
     * Checks if transaction is in tendering state.
     */
    public boolean isTendering() {
        return state == TransactionState.TENDERING;
    }

    /**
     * Clears the transaction and returns to shopping state.
     */
    public void clearTransaction() {
        items.clear();
        payment = new Payment();
        state = TransactionState.SHOPPING;  // UPDATED
    }

    /**
     * Sets payment for this transaction.
     */
    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    /**
     * Checks if transaction has been paid.
     */
    public boolean isPaid() {
        return payment != null && payment.isCompleted();
    }

    @Override
    public String toString() {
        return String.format("Transaction{lines=%d, items=%d, subtotal=$%.2f, tax=$%.2f, total=$%.2f, paid=%s}",
                getLineCount(), getItemCount(), getSubtotal(), getTaxAmount(), getTotal(), isPaid());
    }
}