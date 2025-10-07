package com.am.register.model;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents tax calculation breakdown by category.
 */
@Data
public class TaxBreakdown {

    // Tax rates by category
    public static final double TAX_RATE_TOBACCO = 0.20;   // 20%
    public static final double TAX_RATE_ALCOHOL = 0.15;   // 15%
    public static final double TAX_RATE_DEFAULT = 0.07;   // 7%

    private Map<String, CategoryTax> categoryTaxes = new HashMap<>();
    private double totalTax = 0.0;

    /**
     * Represents tax for a specific category.
     */
    @Data
    public static class CategoryTax {
        private String category;
        private double subtotal;
        private double taxRate;
        private double taxAmount;

        public CategoryTax(String category, double subtotal, double taxRate) {
            this.category = category;
            this.subtotal = subtotal;
            this.taxRate = taxRate;
            this.taxAmount = subtotal * taxRate;
        }

        public String getFormattedRate() {
            return String.format("%.0f%%", taxRate * 100);
        }
    }

    /**
     * Gets tax rate for a category.
     */
    public static double getTaxRateForCategory(String category) {
        if ("TOBACCO".equals(category)) {
            return TAX_RATE_TOBACCO;
        } else if ("ALCOHOL".equals(category)) {
            return TAX_RATE_ALCOHOL;
        } else {
            return TAX_RATE_DEFAULT;
        }
    }

    /**
     * Adds category tax to breakdown.
     */
    public void addCategoryTax(String category, double subtotal, double taxRate) {
        CategoryTax catTax = new CategoryTax(category, subtotal, taxRate);
        categoryTaxes.put(category, catTax);
        totalTax += catTax.getTaxAmount();
    }

    /**
     * Checks if there are multiple tax categories.
     */
    public boolean hasMultipleTaxRates() {
        return categoryTaxes.size() > 1 ||
                categoryTaxes.values().stream()
                        .anyMatch(ct -> ct.getTaxRate() != TAX_RATE_DEFAULT);
    }
}