package com.am.register.util;

import com.am.register.controller.RegisterController;
import com.am.register.util.ConsoleJournal;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler that automatically cleans up old suspended transactions.
 * Runs daily at midnight to remove transactions older than 7 days.
 */
public class SuspensionCleanupScheduler {
    private final RegisterController controller;
    private final ScheduledExecutorService scheduler;
    private final int retentionDays;
    private LocalDateTime lastCleanupDate;

    /**
     * Creates a new cleanup scheduler
     * @param controller The register controller
     * @param retentionDays Number of days to keep suspended transactions (default: 7)
     */
    public SuspensionCleanupScheduler(RegisterController controller, int retentionDays) {
        this.controller = controller;
        this.retentionDays = retentionDays;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.lastCleanupDate = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
    }

    /**
     * Starts the cleanup scheduler.
     * Checks every hour if it's past midnight and runs cleanup if needed.
     */
    public void start() {
        // Schedule cleanup check every hour
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkAndCleanup();
            } catch (Exception e) {
                System.err.println("Error during suspension cleanup: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.HOURS); // Check every hour

        System.out.println("Suspension cleanup scheduler started (retention: " + retentionDays + " days)");
    }

    /**
     * Checks if we've crossed midnight and performs cleanup if needed
     */
    private void checkAndCleanup() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentDate = now.truncatedTo(ChronoUnit.DAYS);

        // If we've crossed into a new day since last cleanup
        if (currentDate.isAfter(lastCleanupDate)) {
            performCleanup();
            lastCleanupDate = currentDate;
        }
    }

    /**
     * Performs the actual cleanup of old suspended transactions
     */
    private void performCleanup() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        int removedCount = controller.cleanupOldSuspensions(cutoffDate);

        // Log the cleanup result
        ConsoleJournal.logSuspensionCleanup(removedCount);
    }

    /**
     * Stops the cleanup scheduler gracefully
     */
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
                System.out.println("Suspension cleanup scheduler stopped");
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Manual trigger for cleanup (useful for testing)
     */
    public void forceCleanup() {
        performCleanup();
    }
}