package com.am.register.database;

import com.am.register.model.Item;

import java.sql.*;

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
                    "PRICE DECIMAL(10,2) NOT NULL)";

    private static final String INSERT_ITEM_SQL =
            "INSERT INTO ITEMS (UPC, DESCRIPTION, PRICE) VALUES (?, ?, ?)";

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
            statement.execute(CREATE_TABLE_SQL);
            System.out.println("✓ ITEMS table ready");
            return true;

        } catch (SQLException e) {
            System.err.println("✗ Failed to create table: " + e.getMessage());
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

            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            // Silently fail on duplicate keys (expected when re-running)
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
                // Item found - create and populate Item object
                Item item = new Item();
                item.setUpc(rs.getString("UPC"));
                item.setDescription(rs.getString("DESCRIPTION"));
                item.setPrice(rs.getDouble("PRICE"));
                return item;
            } else {
                // Item not found
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
}