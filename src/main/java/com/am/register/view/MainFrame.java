package com.am.register.view;

import com.am.register.controller.RegisterController;
import com.am.register.controller.ScannerInputHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Main application window for the register system.
 * Contains the display panel and scanner input field.
 */
public class MainFrame extends JFrame {

    private final DisplayPanel displayPanel;
    private final JTextField scannerInputField;
    private final RegisterController controller;

    /**
     * Creates the main application frame.
     * @param scannerHandler The scanner input handler for capturing scans
     * @param controller The register controller for handling actions
     */
    public MainFrame(ScannerInputHandler scannerHandler, RegisterController controller) {
        this.controller = controller;

        // Window properties
        setTitle("Mock Register System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center on screen
        setResizable(false);

        // Create display panel
        displayPanel = new DisplayPanel();

        // Get scanner input field from handler
        scannerInputField = scannerHandler.getScannerInputField();

        // Set up layout
        setupLayout();

        // Set up keyboard shortcuts
        setupKeyboardShortcuts();

        // Wire clear button to controller
        displayPanel.setClearButtonListener(e -> controller.startNewTransaction());

        // Ensure scanner field has focus
        scannerInputField.requestFocusInWindow();
    }

    /**
     * Sets up the window layout.
     */
    private void setupLayout() {
        // Use BorderLayout for main frame
        setLayout(new BorderLayout());

        // Add display panel to center
        add(displayPanel, BorderLayout.CENTER);

        // Add scanner input field at bottom
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Scanner Input (or type UPC + Enter)"));
        inputPanel.add(scannerInputField, BorderLayout.CENTER);

        // Add info label
        JLabel infoLabel = new JLabel("F1 = Clear Transaction | F2 = Focus Scanner Input");
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        inputPanel.add(infoLabel, BorderLayout.SOUTH);

        add(inputPanel, BorderLayout.SOUTH);
    }

    /**
     * Sets up keyboard shortcuts for the application.
     */
    private void setupKeyboardShortcuts() {
        // Get the root pane's input and action maps
        JRootPane rootPane = getRootPane();

        // F1 - Clear Transaction
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "clearTransaction");
        rootPane.getActionMap().put("clearTransaction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.startNewTransaction();
            }
        });

        // F2 - Focus Scanner Input
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "focusScanner");
        rootPane.getActionMap().put("focusScanner", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ensureScannerFocus();
            }
        });
    }

    /**
     * Gets the display panel.
     * Used by controller to update the display.
     * @return The display panel
     */
    public DisplayPanel getDisplayPanel() {
        return displayPanel;
    }

    /**
     * Ensures the scanner input field has focus.
     * Called after showing dialogs or other focus-stealing operations.
     */
    public void ensureScannerFocus() {
        scannerInputField.requestFocusInWindow();
    }
}