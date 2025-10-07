package com.am.register.model;

import lombok.Data;

/**
 * Represents an item in a transaction with quantity.
 * Wraps an Item with quantity tracking and line total calculation.
 */
@Data
public class TransactionItem {

    private Item item;
    private int quantity;

    /**
     * Creates a transaction item with quantity of 1.
     */
    public TransactionItem(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.item = item;
        this.quantity = 1;
    }

    /**
     * Gets the line total (price × quantity).
     */
    public double getLineTotal() {
        return item.getPrice() * quantity;
    }

    /**
     * Increases quantity by 1.
     */
    public void incrementQuantity() {
        quantity++;
    }

    /**
     * Decreases quantity by 1.
     * Minimum quantity is 1 (use void to remove entirely).
     */
    public void decrementQuantity() {
        if (quantity > 1) {
            quantity--;
        }
    }

    /**
     * Sets a specific quantity.
     * @param quantity Must be >= 1
     */
    public void setQuantity(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        this.quantity = quantity;
    }

    /**
     * Gets the item's UPC (convenience method).
     */
    public String getUpc() {
        return item.getUpc();
    }

    /**
     * Gets the item's description (convenience method).
     */
    public String getDescription() {
        return item.getDescription();
    }

    /**
     * Gets the item's unit price (convenience method).
     */
    public double getUnitPrice() {
        return item.getPrice();
    }

    @Override
    public String toString() {
        return String.format("%dx %s @ $%.2f = $%.2f",
                quantity, item.getDescription(), item.getPrice(), getLineTotal());
    }
}