package com.am.register.controller;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Handles input from a barcode scanner.
 * Scanners act as keyboard input devices, sending digits followed by Enter.
 * This handler captures that input and passes it to the controller.
 */
public class ScannerInputHandler {

    private final RegisterController controller;
    private final JTextField scannerInputField;

    /**
     * Creates a scanner input handler.
     *
     * @param controller The register controller to send scanned UPCs to
     */
    public ScannerInputHandler(RegisterController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("RegisterController cannot be null");
        }

        this.controller = controller;

        // Create hidden text field to capture scanner input
        this.scannerInputField = new JTextField();

        // Add action listener for Enter key (end of scan)
        this.scannerInputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleScannerInput();
            }
        });
    }

    /**
     * Gets the JTextField component for scanner input.
     * This should be added to your UI and kept focused.
     *
     * @return The scanner input field
     */
    public JTextField getScannerInputField() {
        return scannerInputField;
    }

    /**
     * Handles the completed scanner input.
     * Called when Enter key is pressed (scanner finishes sending UPC).
     */
    private void handleScannerInput() {
        // Get the scanned UPC
        String upc = scannerInputField.getText().trim();

        // Clear field for next scan
        scannerInputField.setText("");

        // Pass to controller if not empty
        if (!upc.isEmpty()) {
            controller.processUPCScan(upc);
        }

        // Ensure field keeps focus for next scan
        scannerInputField.requestFocusInWindow();
    }

    /**
     * Manually triggers a UPC scan (for testing without physical scanner).
     *
     * @param upc The UPC to simulate scanning
     */
    public void simulateScan(String upc) {
        scannerInputField.setText(upc);
        handleScannerInput();
    }
}