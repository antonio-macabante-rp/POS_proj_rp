package com.am.register.util;

import org.h2.tools.Server;
import java.io.File;
import java.sql.SQLException;

/**
 * Manages the H2 database server lifecycle.
 * This is a utility class - use Main.java to run the application.
 */
public class H2ServerManager {

    private static Server tcpServer;
    private static Server webServer;

    /**
     * Starts the H2 TCP server and Web Console.
     * Creates database directory if it doesn't exist.
     */
    public static void startServer() {
        try {
            // Ensure database directory exists
            File dbDir = new File("./database");
            if (!dbDir.exists()) {
                dbDir.mkdirs();
                System.out.println("✓ Created database directory: ./database");
            }

            // Start TCP Server
            tcpServer = Server.createTcpServer(
                    "-tcp",
                    "-tcpAllowOthers",
                    "-tcpPort", "9092",
                    "-baseDir", "./database"
            ).start();

            System.out.println("✓ H2 TCP Server started on port 9092");

            // Start Web Console
            webServer = Server.createWebServer(
                    "-web",
                    "-webAllowOthers",
                    "-webPort", "8082"
            ).start();

            System.out.println("✓ H2 Web Console started at http://localhost:8082");

        } catch (SQLException e) {
            System.err.println("✗ Failed to start H2 server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Stops both the TCP server and Web Console.
     */
    public static void stopServer() {
        if (tcpServer != null && tcpServer.isRunning(false)) {
            tcpServer.stop();
            System.out.println("✓ H2 TCP Server stopped");
        }
        if (webServer != null && webServer.isRunning(false)) {
            webServer.stop();
            System.out.println("✓ H2 Web Console stopped");
        }
    }

    /**
     * Checks if the TCP server is currently running.
     * @return true if running, false otherwise
     */
    public static boolean isRunning() {
        return tcpServer != null && tcpServer.isRunning(false);
    }
}