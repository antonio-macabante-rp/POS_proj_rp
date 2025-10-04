package com.am.register.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class Transaction {

    @Getter
    private List<Item> scannedItems = new ArrayList<>();

    public void addItem(Item item) {
        if (item != null) {
            scannedItems.add(item);
        }
    }

    public double getSubtotal() {
        double total = 0.0;
        for (Item item : scannedItems) {
            total += item.getPrice();
        }
        return total;
    }

    public int getItemCount() {
        return scannedItems.size();
    }

    public void clearTransaction() {
        scannedItems.clear();
    }

    @Override
    public String toString() {
        return String.format("Transaction{items=%d, subtotal=$%.2f}",
                getItemCount(), getSubtotal());
    }
}