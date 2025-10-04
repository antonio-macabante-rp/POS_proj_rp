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

    @Override
    public String toString() {
        return String.format("%s ($%.2f)", description, price);
    }
}
