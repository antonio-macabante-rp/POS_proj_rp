package com.am.register.database;

import java.util.ArrayList;
import java.util.List;
import com.am.register.model.Item;
import com.am.register.model.SuspendedTransaction;
import com.am.register.model.Transaction;
import com.am.register.model.TransactionItem;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages all H2 database operations for the register system.
 * Uses H2 in server mode for IntelliJ Database tool compatibility.
 */
public class DatabaseManager {

    // Database connection details - UPDATED URL
    private static final String JDBC_URL = "jdbc:h2:tcp://localhost:9092/register";
    private static final String JDBC_USER = "sa";
    private static final String JDBC_PASSWORD = "";

    // SQL statements
    private static final String CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS ITEMS (" +
                    "UPC VARCHAR(20) PRIMARY KEY, " +
                    "DESCRIPTION VARCHAR(255) NOT NULL, " +
                    "PRICE DECIMAL(10,2) NOT NULL, " +
                    "CATEGORY VARCHAR(50) DEFAULT 'OTHER', " +
                    "IS_POPULAR BOOLEAN DEFAULT FALSE)";

    private static final String CREATE_TRANSACTIONS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS TRANSACTIONS (" +
                    "TRANSACTION_ID VARCHAR(50) PRIMARY KEY, " +
                    "TIMESTAMP TIMESTAMP NOT NULL, " +
                    "SUBTOTAL DECIMAL(10,2) NOT NULL, " +
                    "TAX DECIMAL(10,2) NOT NULL, " +
                    "TOTAL DECIMAL(10,2) NOT NULL, " +
                    "PAYMENT_METHOD VARCHAR(20) NOT NULL)";

    private static final String CREATE_TRANSACTION_ITEMS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS TRANSACTION_ITEMS (" +
                    "ID INT AUTO_INCREMENT PRIMARY KEY, " +
                    "TRANSACTION_ID VARCHAR(50) NOT NULL, " +
                    "UPC VARCHAR(20) NOT NULL, " +
                    "DESCRIPTION VARCHAR(255) NOT NULL, " +
                    "QUANTITY INT NOT NULL, " +
                    "UNIT_PRICE DECIMAL(10,2) NOT NULL, " +
                    "LINE_TOTAL DECIMAL(10,2) NOT NULL, " +
                    "FOREIGN KEY (TRANSACTION_ID) REFERENCES TRANSACTIONS(TRANSACTION_ID), " +
                    "FOREIGN KEY (UPC) REFERENCES ITEMS(UPC))";

    private static final String CREATE_SUSPENDED_TRANSACTIONS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS SUSPENDED_TRANSACTIONS (" +
                    "SUSPENSION_ID VARCHAR(50) PRIMARY KEY, " +
                    "SUSPENDED_AT TIMESTAMP NOT NULL, " +
                    "SUSPENSION_DATE DATE NOT NULL, " +
                    "TRANSACTION_STATE VARCHAR(20) NOT NULL, " +
                    "SUBTOTAL DECIMAL(10,2) NOT NULL, " +
                    "TAX DECIMAL(10,2) NOT NULL, " +
                    "TOTAL DECIMAL(10,2) NOT NULL, " +
                    "ITEM_COUNT INT NOT NULL, " +
                    "ITEMS_JSON TEXT NOT NULL, " +
                    "NOTE VARCHAR(255))";

    private static final String INSERT_ITEM_SQL =
            "INSERT INTO ITEMS (UPC, DESCRIPTION, PRICE, CATEGORY, IS_POPULAR) VALUES (?, ?, ?, ?, ?)";

    private static final String SELECT_ITEM_BY_UPC_SQL =
            "SELECT * FROM ITEMS WHERE UPC = ?";

    private static final String DELETE_ALL_ITEMS_SQL =
            "DELETE FROM ITEMS";

    private Connection connection;

    /**
     * Establishes connection to the H2 database server.
     * Note: H2 server must be running before calling this method.
     *
     * @return true if connection successful, false otherwise
     */
    public boolean connect() {
        try {
            Class.forName("org.h2.Driver");
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);

            System.out.println("✓ Database connection established (Server Mode)");
            System.out.println("  Connection URL: " + JDBC_URL);
            System.out.println("  Database file: ~/register.mv.db");

            return true;

        } catch (ClassNotFoundException e) {
            System.err.println("✗ H2 Driver not found: " + e.getMessage());
            return false;
        } catch (SQLException e) {
            System.err.println("✗ Database connection failed: " + e.getMessage());
            System.err.println("  Make sure H2 server is running!");
            System.err.println("  Expected URL: " + JDBC_URL);
            return false;
        }
    }

    /**
     * Creates the ITEMS table if it doesn't already exist.
     *
     * @return true if table created/exists, false otherwise
     */
    public boolean createTables() {
        try (Statement statement = connection.createStatement()) {
            // Create ITEMS table
            statement.execute(CREATE_TABLE_SQL);
            System.out.println("✓ ITEMS table ready");

            // Create TRANSACTIONS table
            statement.execute(CREATE_TRANSACTIONS_TABLE_SQL);
            System.out.println("✓ TRANSACTIONS table ready");

            // Create TRANSACTION_ITEMS table
            statement.execute(CREATE_TRANSACTION_ITEMS_TABLE_SQL);
            System.out.println("✓ TRANSACTION_ITEMS table ready");

            statement.execute(CREATE_SUSPENDED_TRANSACTIONS_TABLE_SQL);  // NEW
            System.out.println("✓ SUSPENDED_TRANSACTIONS table ready");

            return true;

        } catch (SQLException e) {
            System.err.println("✗ Failed to create tables: " + e.getMessage());
            return false;
        }
    }

    /**
     * Inserts an item into the database.
     * If an item with the same UPC exists, it will fail (UPC is primary key).
     *
     * @param item The item to insert
     * @return true if insert successful, false otherwise
     */
    public boolean insertItem(Item item) {
        if (item == null) {
            System.err.println("✗ Cannot insert null item");
            return false;
        }

        try (PreparedStatement pstmt = connection.prepareStatement(INSERT_ITEM_SQL)) {
            pstmt.setString(1, item.getUpc());
            pstmt.setString(2, item.getDescription());
            pstmt.setDouble(3, item.getPrice());
            pstmt.setString(4, item.getCategory() != null ? item.getCategory() : "OTHER");
            pstmt.setBoolean(5, item.isPopular());

            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            if (!e.getMessage().contains("Unique index or primary key violation")) {
                System.err.println("✗ Failed to insert item " + item.getUpc() + ": " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Retrieves an item from the database by its UPC.
     *
     * @param upc The UPC to search for
     * @return Item object if found, null if not found
     */
    public Item getItemByUPC(String upc) {
        if (upc == null || upc.trim().isEmpty()) {
            return null;
        }

        try (PreparedStatement pstmt = connection.prepareStatement(SELECT_ITEM_BY_UPC_SQL)) {
            pstmt.setString(1, upc);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Item item = new Item();
                item.setUpc(rs.getString("UPC"));
                item.setDescription(rs.getString("DESCRIPTION"));
                item.setPrice(rs.getDouble("PRICE"));

                // Handle columns that might not exist in old schema
                try {
                    item.setCategory(rs.getString("CATEGORY"));
                    item.setPopular(rs.getBoolean("IS_POPULAR"));
                } catch (SQLException e) {
                    // Old schema - set defaults
                    item.setCategory("OTHER");
                    item.setPopular(false);
                }

                return item;
            } else {
                return null;
            }

        } catch (SQLException e) {
            System.err.println("✗ Failed to query item " + upc + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Deletes all items from the database.
     * Useful for testing and resetting the database.
     *
     * @return true if successful, false otherwise
     */
    public boolean clearAllItems() {
        try (Statement statement = connection.createStatement()) {
            int rowsDeleted = statement.executeUpdate(DELETE_ALL_ITEMS_SQL);
            System.out.println("✓ Cleared " + rowsDeleted + " items from database");
            return true;

        } catch (SQLException e) {
            System.err.println("✗ Failed to clear items: " + e.getMessage());
            return false;
        }
    }

    /**
     * Counts total number of items in the database.
     * Useful for verification after parsing.
     *
     * @return Number of items, or -1 if error
     */
    public int getItemCount() {
        try (Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM ITEMS");
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;

        } catch (SQLException e) {
            System.err.println("✗ Failed to count items: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Closes the database connection.
     * Should be called when the application shuts down.
     */
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✓ Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("✗ Error closing connection: " + e.getMessage());
        }
    }

    /**
     * Checks if the database connection is active.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Retrieves all items from the database.
     * Used for populating the item grid.
     *
     * @return List of all items, or empty list if error
     */
    public List<Item> getAllItems() {
        List<Item> items = new ArrayList<>();

        String sql = "SELECT * FROM ITEMS ORDER BY DESCRIPTION";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Item item = new Item();
                item.setUpc(rs.getString("UPC"));
                item.setDescription(rs.getString("DESCRIPTION"));
                item.setPrice(rs.getDouble("PRICE"));

                try {
                    item.setCategory(rs.getString("CATEGORY"));
                    item.setPopular(rs.getBoolean("IS_POPULAR"));
                } catch (SQLException e) {
                    item.setCategory("OTHER");
                    item.setPopular(false);
                }

                items.add(item);
            }

            System.out.println("✓ Retrieved " + items.size() + " items for grid");

        } catch (SQLException e) {
            System.err.println("✗ Failed to retrieve items: " + e.getMessage());
        }

        return items;
    }

    /**
     * Retrieves only popular items from the database.
     * @return List of popular items
     */
    public List<Item> getPopularItems() {
        List<Item> items = new ArrayList<>();

        String sql = "SELECT * FROM ITEMS WHERE IS_POPULAR = TRUE ORDER BY DESCRIPTION";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Item item = new Item();
                item.setUpc(rs.getString("UPC"));
                item.setDescription(rs.getString("DESCRIPTION"));
                item.setPrice(rs.getDouble("PRICE"));
                item.setCategory(rs.getString("CATEGORY"));
                item.setPopular(rs.getBoolean("IS_POPULAR"));
                items.add(item);
            }

            System.out.println("✓ Retrieved " + items.size() + " popular items");

        } catch (SQLException e) {
            System.err.println("✗ Failed to retrieve popular items: " + e.getMessage());
        }

        return items;
    }

    /**
     * Saves a completed transaction to the database.
     */
    public boolean saveTransaction(Transaction transaction, String receiptNumber) {
        if (transaction == null || !transaction.isPaid()) {
            System.err.println("✗ Cannot save unpaid or null transaction");
            return false;
        }

        try {
            // Insert into TRANSACTIONS table
            String txSql = "INSERT INTO TRANSACTIONS " +
                    "(TRANSACTION_ID, TIMESTAMP, SUBTOTAL, TAX, TOTAL, PAYMENT_METHOD) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = connection.prepareStatement(txSql)) {
                pstmt.setString(1, receiptNumber);
                pstmt.setTimestamp(2, java.sql.Timestamp.valueOf(transaction.getPayment().getPaymentTime()));
                pstmt.setDouble(3, transaction.getSubtotal());
                pstmt.setDouble(4, transaction.getTaxAmount());
                pstmt.setDouble(5, transaction.getTotal());
                pstmt.setString(6, transaction.getPayment().getMethod().name());

                pstmt.executeUpdate();
            }

            // Insert items into TRANSACTION_ITEMS table
            String itemSql = "INSERT INTO TRANSACTION_ITEMS " +
                    "(TRANSACTION_ID, UPC, DESCRIPTION, QUANTITY, UNIT_PRICE, LINE_TOTAL) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = connection.prepareStatement(itemSql)) {
                for (TransactionItem txItem : transaction.getItems()) {
                    pstmt.setString(1, receiptNumber);
                    pstmt.setString(2, txItem.getUpc());
                    pstmt.setString(3, txItem.getDescription());
                    pstmt.setInt(4, txItem.getQuantity());
                    pstmt.setDouble(5, txItem.getUnitPrice());
                    pstmt.setDouble(6, txItem.getLineTotal());

                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            System.out.println("✓ Transaction saved: " + receiptNumber);
            return true;

        } catch (SQLException e) {
            System.err.println("✗ Failed to save transaction: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets sales data for the last N days.
     * Returns map of UPC to total quantity sold.
     */
    public java.util.Map<String, Integer> getSalesDataForDays(int days) {
        java.util.Map<String, Integer> salesMap = new java.util.HashMap<>();

        String sql = "SELECT UPC, SUM(QUANTITY) AS TOTAL_SOLD " +
                "FROM TRANSACTION_ITEMS TI " +
                "JOIN TRANSACTIONS T ON TI.TRANSACTION_ID = T.TRANSACTION_ID " +
                "WHERE T.TIMESTAMP >= DATEADD('DAY', ?, CURRENT_TIMESTAMP()) " +
                "GROUP BY UPC " +
                "ORDER BY TOTAL_SOLD DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, -days);  // Negative for past days

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String upc = rs.getString("UPC");
                int totalSold = rs.getInt("TOTAL_SOLD");
                salesMap.put(upc, totalSold);
            }

            System.out.println("✓ Retrieved sales data for last " + days + " days: " + salesMap.size() + " items");

        } catch (SQLException e) {
            System.err.println("✗ Failed to get sales data: " + e.getMessage());
        }

        return salesMap;
    }

    /**
     * Updates popular items based on sales data.
     * Marks top N items as popular.
     */
    public boolean updatePopularItems(int topN) {
        System.out.println("\n=== UPDATING POPULAR ITEMS ===");

        try {
            // First, clear all popular flags
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("UPDATE ITEMS SET IS_POPULAR = FALSE");
                System.out.println("✓ Cleared existing popular flags");
            }

            // Get top selling items from last 30 days
            String sql = "SELECT UPC, SUM(QUANTITY) AS TOTAL_SOLD " +
                    "FROM TRANSACTION_ITEMS TI " +
                    "JOIN TRANSACTIONS T ON TI.TRANSACTION_ID = T.TRANSACTION_ID " +
                    "WHERE T.TIMESTAMP >= DATEADD('DAY', -30, CURRENT_TIMESTAMP()) " +
                    "GROUP BY UPC " +
                    "ORDER BY TOTAL_SOLD DESC " +
                    "LIMIT ?";

            List<String> topUPCs = new ArrayList<>();

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, topN);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    topUPCs.add(rs.getString("UPC"));
                }
            }

            if (topUPCs.isEmpty()) {
                System.out.println("⚠ No sales data yet - popular items not updated");
                return true; // Not an error, just no data
            }

            // Mark top items as popular
            String updateSql = "UPDATE ITEMS SET IS_POPULAR = TRUE WHERE UPC = ?";

            try (PreparedStatement pstmt = connection.prepareStatement(updateSql)) {
                for (String upc : topUPCs) {
                    pstmt.setString(1, upc);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            System.out.println("✓ Marked " + topUPCs.size() + " items as popular (based on 30-day sales)");
            System.out.println();

            return true;

        } catch (SQLException e) {
            System.err.println("✗ Failed to update popular items: " + e.getMessage());
            return false;
        }
    }

    /**
     * Saves a suspended transaction to the database.
     */
    public boolean saveSuspendedTransaction(SuspendedTransaction suspension) {
        String sql = "INSERT INTO SUSPENDED_TRANSACTIONS " +
                "(SUSPENSION_ID, SUSPENDED_AT, SUSPENSION_DATE, TRANSACTION_STATE, " +
                "SUBTOTAL, TAX, TOTAL, ITEM_COUNT, ITEMS_JSON, NOTE) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, suspension.getSuspensionId());
            pstmt.setTimestamp(2, java.sql.Timestamp.valueOf(suspension.getSuspendedAt()));
            pstmt.setDate(3, java.sql.Date.valueOf(suspension.getSuspendedAt().toLocalDate()));
            pstmt.setString(4, suspension.getTransactionState());
            pstmt.setDouble(5, suspension.getSubtotal());
            pstmt.setDouble(6, suspension.getTax());
            pstmt.setDouble(7, suspension.getTotal());
            pstmt.setInt(8, suspension.getItemCount());
            pstmt.setString(9, suspension.getItemsJson());
            pstmt.setString(10, suspension.getNote());

            pstmt.executeUpdate();
            System.out.println("✓ Suspended transaction saved: " + suspension.getSuspensionId());
            return true;

        } catch (SQLException e) {
            System.err.println("✗ Failed to save suspension: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves all suspended transactions.
     */
    public List<SuspendedTransaction> getAllSuspendedTransactions() {
        List<SuspendedTransaction> suspensions = new ArrayList<>();

        String sql = "SELECT * FROM SUSPENDED_TRANSACTIONS ORDER BY SUSPENDED_AT DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                SuspendedTransaction suspension = new SuspendedTransaction();
                suspension.setSuspensionId(rs.getString("SUSPENSION_ID"));
                suspension.setSuspendedAt(rs.getTimestamp("SUSPENDED_AT").toLocalDateTime());
                suspension.setTransactionState(rs.getString("TRANSACTION_STATE"));
                suspension.setSubtotal(rs.getDouble("SUBTOTAL"));
                suspension.setTax(rs.getDouble("TAX"));
                suspension.setTotal(rs.getDouble("TOTAL"));
                suspension.setItemCount(rs.getInt("ITEM_COUNT"));
                suspension.setItemsJson(rs.getString("ITEMS_JSON"));
                suspension.setNote(rs.getString("NOTE"));

                suspensions.add(suspension);
            }

            System.out.println("✓ Retrieved " + suspensions.size() + " suspended transactions");

        } catch (SQLException e) {
            System.err.println("✗ Failed to retrieve suspensions: " + e.getMessage());
        }

        return suspensions;
    }

    /**
     * Deletes a suspended transaction by ID.
     */
    public boolean deleteSuspendedTransaction(String suspensionId) {
        String sql = "DELETE FROM SUSPENDED_TRANSACTIONS WHERE SUSPENSION_ID = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, suspensionId);
            int deleted = pstmt.executeUpdate();

            if (deleted > 0) {
                System.out.println("✓ Deleted suspended transaction: " + suspensionId);
                return true;
            } else {
                System.out.println("⚠ Suspension not found: " + suspensionId);
                return false;
            }

        } catch (SQLException e) {
            System.err.println("✗ Failed to delete suspension: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes all suspended transactions from previous days.
     * Used for daily cleanup.
     */
    public int cleanupExpiredSuspensions() {
        String sql = "DELETE FROM SUSPENDED_TRANSACTIONS WHERE SUSPENSION_DATE < CURRENT_DATE";

        try (Statement stmt = connection.createStatement()) {
            int deleted = stmt.executeUpdate(sql);

            if (deleted > 0) {
                System.out.println("✓ Cleaned up " + deleted + " expired suspended transactions");
            } else {
                System.out.println("✓ No expired suspensions to clean up");
            }

            return deleted;

        } catch (SQLException e) {
            System.err.println("✗ Failed to cleanup suspensions: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Gets count of suspended transactions.
     */
    public int getSuspendedTransactionCount() {
        String sql = "SELECT COUNT(*) FROM SUSPENDED_TRANSACTIONS";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;

        } catch (SQLException e) {
            System.err.println("✗ Failed to count suspensions: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Gets the next suspension sequence number for today.
     */
    public int getNextSuspensionSequence() {
        String sql = "SELECT COUNT(*) FROM SUSPENDED_TRANSACTIONS " +
                "WHERE SUSPENSION_DATE = CURRENT_DATE";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1) + 1;  // Next sequence number
            }
            return 1;

        } catch (SQLException e) {
            System.err.println("✗ Failed to get suspension sequence: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Gets the active database connection.
     * Used for migrations and advanced operations.
     * @return The connection
     */
    public Connection getConnection() {
        return connection;
    }
}