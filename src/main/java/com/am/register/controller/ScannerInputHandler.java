package com.am.register.controller;

import com.am.register.model.InputSource;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Handles input from a barcode scanner globally.
 * Captures keyboard input application-wide without requiring focus.
 */
public class ScannerInputHandler {

    private final RegisterController controller;
    private final StringBuilder scanBuffer;
    private long lastKeyTime;
    private static final long SCAN_TIMEOUT_MS = 100; // Scanner types fast
    private boolean scanningEnabled = true;

    /**
     * Enables or disables scanner input.
     */
    public void setEnabled(boolean enabled) {
        this.scanningEnabled = enabled;
    }

    /**
     * Checks if scanning is enabled.
     */
    public boolean isEnabled() {
        return scanningEnabled;
    }

    /**
     * Creates a scanner input handler with global keyboard listener.
     *
     * @param controller The register controller to send scanned UPCs to
     */
    public ScannerInputHandler(RegisterController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("RegisterController cannot be null");
        }

        this.controller = controller;
        this.scanBuffer = new StringBuilder();
        this.lastKeyTime = System.currentTimeMillis();

        // Install global keyboard listener
        installGlobalKeyListener();
    }

    /**
     * Installs a global KeyEventDispatcher to capture all keyboard input.
     */
    private void installGlobalKeyListener() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(new KeyEventDispatcher() {
                    @Override
                    public boolean dispatchKeyEvent(KeyEvent e) {
                        // Only process KEY_TYPED events to get actual characters
                        if (e.getID() == KeyEvent.KEY_TYPED) {
                            return handleKeyTyped(e);
                        }
                        return false; // Allow event to propagate normally
                    }
                });

        System.out.println("✓ Global scanner input handler installed");
    }

    /**
     * Handles key typed events globally.
     */
    private boolean handleKeyTyped(KeyEvent e) {
        if (!scanningEnabled) {
            return false; // Don't process if disabled
        }

        char keyChar = e.getKeyChar();
        long currentTime = System.currentTimeMillis();

        // Check if this is part of a rapid scan sequence
        long timeSinceLastKey = currentTime - lastKeyTime;

        // If too much time passed, reset buffer (user typing vs scanner)
        if (timeSinceLastKey > SCAN_TIMEOUT_MS && scanBuffer.length() > 0) {
            scanBuffer.setLength(0);
        }

        lastKeyTime = currentTime;

        // Handle Enter key (end of scan)
        if (keyChar == '\n' || keyChar == '\r') {
            if (scanBuffer.length() > 0) {
                String scannedUPC = scanBuffer.toString().trim();
                scanBuffer.setLength(0);

                // Process the scanned UPC
                if (!scannedUPC.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        controller.processUPCScan(scannedUPC, InputSource.SCANNER);
                    });
                    return true; // Consume event
                }
            }
            return false; // Allow Enter to propagate if buffer empty
        }

        // Handle digits and letters (UPCs can have letters in some formats)
        if (Character.isLetterOrDigit(keyChar)) {
            scanBuffer.append(keyChar);

            // If scanning (rapid input), consume the event
            if (timeSinceLastKey < SCAN_TIMEOUT_MS) {
                return true; // Consume event to prevent typing in other fields
            }
        }

        return false; // Allow event to propagate normally
    }

    /**
     * Gets the current buffer content (for debugging).
     */
    public String getBufferContent() {
        return scanBuffer.toString();
    }

    /**
     * Clears the scan buffer.
     */
    public void clearBuffer() {
        scanBuffer.setLength(0);
    }
}