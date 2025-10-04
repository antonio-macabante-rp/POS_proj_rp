package com.am.register.database;

import com.am.register.model.Item;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Parses the price book TSV file and populates the database.
 * Expected format: UPC[TAB]Description[TAB]Price
 */
public class PriceBookParser {

    private final DatabaseManager databaseManager;
    private int totalLines = 0;
    private int successfulInserts = 0;
    private int failedInserts = 0;

    /**
     * Creates a parser with a database manager for inserting items.
     * @param databaseManager The database manager to use for insertions
     */
    public PriceBookParser(DatabaseManager databaseManager) {
        if (databaseManager == null) {
            throw new IllegalArgumentException("DatabaseManager cannot be null");
        }
        this.databaseManager = databaseManager;
    }

    /**
     * Parses the price book file and inserts items into the database.
     * @param filename The name of the file in resources folder (e.g., "pricebook.tsv")
     * @return true if parsing completed (even with some failures), false if file not found
     */
    public boolean parseFile(String filename) {
        System.out.println("=== PARSING PRICE BOOK ===");
        System.out.println("File: " + filename);
        System.out.println();

        // Reset counters
        totalLines = 0;
        successfulInserts = 0;
        failedInserts = 0;

        // Try to load file from resources
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filename);

        if (inputStream == null) {
            System.err.println("✗ File not found: " + filename);
            System.err.println("  Make sure the file is in src/main/resources/");
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            int lineNumber = 0;

            System.out.println("Reading file...");

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                totalLines++;

                // Process the line
                if (parseLine(line, lineNumber)) {
                    successfulInserts++;
                } else {
                    failedInserts++;
                }
            }

            // Print summary
            printSummary();

            return true;

        } catch (Exception e) {
            System.err.println("✗ Error reading file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Parses a single line from the TSV file and inserts it into the database.
     * @param line The line to parse
     * @param lineNumber The line number (for error reporting)
     * @return true if successfully parsed and inserted, false otherwise
     */
    private boolean parseLine(String line, int lineNumber) {
        // Skip empty lines
        if (line == null || line.trim().isEmpty()) {
            return true; // Not an error, just skip
        }

        try {
            // Split by tab character
            String[] parts = line.split("\t");

            // Validate: must have exactly 3 columns
            if (parts.length != 3) {
                System.err.println("  ✗ Line " + lineNumber + ": Invalid format (expected 3 columns, got " + parts.length + ")");
                System.err.println("    Content: " + line);
                return false;
            }

            // Extract and trim fields
            String upc = parts[0].trim();
            String description = parts[1].trim();
            String priceStr = parts[2].trim();

            // Validate: UPC not empty
            if (upc.isEmpty()) {
                System.err.println("  ✗ Line " + lineNumber + ": UPC is empty");
                return false;
            }

            // Validate: Description not empty
            if (description.isEmpty()) {
                System.err.println("  ✗ Line " + lineNumber + ": Description is empty");
                return false;
            }

            // Validate: Price is a valid number
            double price;
            try {
                price = Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                System.err.println("  ✗ Line " + lineNumber + ": Invalid price '" + priceStr + "' (not a number)");
                return false;
            }

            // Validate: Price is positive
            if (price < 0) {
                System.err.println("  ✗ Line " + lineNumber + ": Invalid price " + price + " (must be positive)");
                return false;
            }

            // Create Item object
            Item item = new Item(upc, description, price);

            // Insert into database
            boolean inserted = databaseManager.insertItem(item);

            if (inserted) {
                // Only show progress every 10 items to avoid spam
                if (successfulInserts % 10 == 0) {
                    System.out.println("  ✓ Processed " + (successfulInserts + 1) + " items...");
                }
            } else {
                // Likely a duplicate UPC (primary key violation)
                // DatabaseManager already logs this, so we just count it
            }

            return inserted;

        } catch (Exception e) {
            System.err.println("  ✗ Line " + lineNumber + ": Unexpected error - " + e.getMessage());
            return false;
        }
    }

    /**
     * Prints a summary of the parsing results.
     */
    private void printSummary() {
        System.out.println();
        System.out.println("=== PARSING COMPLETE ===");
        System.out.println("Total lines processed: " + totalLines);
        System.out.println("Successful inserts:    " + successfulInserts);
        System.out.println("Failed inserts:        " + failedInserts);

        if (failedInserts == 0) {
            System.out.println("✓ All items imported successfully!");
        } else {
            System.out.println("⚠ Some items failed to import (see errors above)");
        }

        System.out.println();
    }

    /**
     * Gets the number of successfully inserted items.
     * @return Number of successful inserts
     */
    public int getSuccessfulInserts() {
        return successfulInserts;
    }

    /**
     * Gets the number of failed inserts.
     * @return Number of failed inserts
     */
    public int getFailedInserts() {
        return failedInserts;
    }

    /**
     * Gets the total number of lines processed.
     * @return Total lines processed
     */
    public int getTotalLines() {
        return totalLines;
    }
}