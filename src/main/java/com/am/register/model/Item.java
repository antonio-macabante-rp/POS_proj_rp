package com.am.register.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "upc")

public class Item {
    private String upc;
    private String description;
    private double price;
    private String category;
    private boolean isPopular;

    /**
     * Constructor for backward compatibility (3 parameters).
     */
    public Item(String upc, String description, double price) {
        this.upc = upc;
        this.description = description;
        this.price = price;
        this.category = "OTHER";
        this.isPopular = false;
    }

    @Override
    public String toString() {
        return String.format("%s ($%.2f)", description, price);
    }
}
