package com.am.register.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Handles database schema migrations.
 * Adds new columns to existing tables without losing data.
 */
public class DatabaseMigration {

    private final Connection connection;

    public DatabaseMigration(Connection connection) {
        this.connection = connection;
    }

    /**
     * Checks if a column exists in a table.
     */
    private boolean columnExists(String tableName, String columnName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        ResultSet rs = metadata.getColumns(null, null, tableName, columnName);
        boolean exists = rs.next();
        rs.close();
        return exists;
    }

    /**
     * Migrates ITEMS table to include category and popularity columns.
     */
    public void migrateToVersion2() {
        System.out.println("\n=== DATABASE MIGRATION v1.0 → v2.0 ===");

        try (Statement stmt = connection.createStatement()) {

            // Check and add CATEGORY column
            if (!columnExists("ITEMS", "CATEGORY")) {
                System.out.println("Adding CATEGORY column...");
                stmt.execute("ALTER TABLE ITEMS ADD COLUMN CATEGORY VARCHAR(50) DEFAULT 'OTHER'");
                System.out.println("✓ CATEGORY column added");
            } else {
                System.out.println("✓ CATEGORY column already exists");
            }

            // Check and add IS_POPULAR column
            if (!columnExists("ITEMS", "IS_POPULAR")) {
                System.out.println("Adding IS_POPULAR column...");
                stmt.execute("ALTER TABLE ITEMS ADD COLUMN IS_POPULAR BOOLEAN DEFAULT FALSE");
                System.out.println("✓ IS_POPULAR column added");
            } else {
                System.out.println("✓ IS_POPULAR column already exists");
            }

            System.out.println("\n✓ Migration complete");
            System.out.println("  Existing data preserved");
            System.out.println("  New columns added with default values\n");

        } catch (Exception e) {
            System.err.println("✗ Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}