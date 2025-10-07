package com.am.register.view;

import com.am.register.controller.RegisterController;
import com.am.register.model.Item;
import com.am.register.model.InputSource;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Panel displaying a 4x4 grid of item buttons for quick selection.
 * Supports pagination through multiple pages of items.
 */
public class ItemGridPanel extends JPanel {

    private static final int ITEMS_PER_PAGE = 16; // 4x4 grid
    private static final int GRID_ROWS = 4;
    private static final int GRID_COLS = 4;

    private final RegisterController controller;
    private final JButton[][] gridButtons;
    private final JButton nextButton;
    private final JButton prevButton;
    private final JLabel pageLabel;

    private List<Item> allItems;
    private int currentPage = 0;
    private int totalPages = 0;

    private JButton popularButton;
    private boolean showingPopular = false;

    /**
     * Creates the item grid panel.
     * @param controller The register controller
     */
    public ItemGridPanel(RegisterController controller) {
        this.controller = controller;
        this.gridButtons = new JButton[GRID_ROWS][GRID_COLS];

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = createTopPanel();
        JPanel gridPanel = createGridPanel();

        add(topPanel, BorderLayout.NORTH);
        add(gridPanel, BorderLayout.CENTER);

        // Store button references
        prevButton = (JButton) topPanel.getComponent(0);
        nextButton = (JButton) topPanel.getComponent(1);
        popularButton = (JButton) topPanel.getComponent(2);
        pageLabel = (JLabel) topPanel.getComponent(3);
    }

    /**
     * Creates the top panel with pagination controls.
     */
    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        topPanel.setBackground(Color.WHITE);

        JButton prevBtn = new JButton("◀ Prev");
        prevBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        prevBtn.addActionListener(e -> previousPage());

        JButton nextBtn = new JButton("Next ▶");
        nextBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        nextBtn.addActionListener(e -> nextPage());

        JButton popularBtn = new JButton("⭐ Popular");
        popularBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        popularBtn.setBackground(new Color(255, 215, 0)); // Gold
        popularBtn.addActionListener(e -> togglePopular());

        JLabel pageLabel = new JLabel("Page 1 of 1");
        pageLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        pageLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        topPanel.add(prevBtn);     // Index 0
        topPanel.add(nextBtn);     // Index 1
        topPanel.add(popularBtn);  // Index 2
        topPanel.add(pageLabel);   // Index 3

        return topPanel;
    }

    /**
     * Creates the 4x4 grid of item buttons.
     */
    private JPanel createGridPanel() {
        JPanel gridPanel = new JPanel(new GridLayout(GRID_ROWS, GRID_COLS, 5, 5));
        gridPanel.setBackground(Color.WHITE);
        gridPanel.setBorder(BorderFactory.createTitledBorder("Quick Item Selection"));

        // Create 16 buttons
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                JButton button = createItemButton(row, col);
                gridButtons[row][col] = button;
                gridPanel.add(button);
            }
        }

        return gridPanel;
    }

    /**
     * Creates a single item button.
     */
    private JButton createItemButton(int row, int col) {
        JButton button = new JButton();
        button.setFont(new Font("SansSerif", Font.BOLD, 11));
        button.setPreferredSize(new Dimension(120, 80));
        button.setFocusPainted(false);

        // Style
        button.setBackground(new Color(230, 230, 250)); // Light purple
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // Initially disabled (no item loaded yet)
        button.setEnabled(false);
        button.setText("Empty");

        // Add click handler
        final int buttonIndex = row * GRID_COLS + col;
        button.addActionListener(e -> handleItemButtonClick(buttonIndex));

        return button;
    }

    /**
     * Loads items from the controller and displays first page.
     * @param items List of all items from database
     */
    public void loadItems(List<Item> items) {
        this.allItems = items;

        if (items == null || items.isEmpty()) {
            totalPages = 0;
            currentPage = 0;
            updatePageDisplay();
            return;
        }

        // Calculate total pages
        totalPages = (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE);
        currentPage = 0;

        updatePageDisplay();
    }

    /**
     * Updates the grid display with current page items.
     */
    private void updatePageDisplay() {
        if (allItems == null || allItems.isEmpty()) {
            // Clear all buttons
            for (int i = 0; i < GRID_ROWS * GRID_COLS; i++) {
                int row = i / GRID_COLS;
                int col = i % GRID_COLS;
                gridButtons[row][col].setText("Empty");
                gridButtons[row][col].setEnabled(false);
            }
            pageLabel.setText("No items");
            nextButton.setEnabled(false);
            prevButton.setEnabled(false);
            return;
        }

        // Calculate start and end indices for current page
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allItems.size());

        // Update each button
        for (int i = 0; i < GRID_ROWS * GRID_COLS; i++) {
            int row = i / GRID_COLS;
            int col = i % GRID_COLS;
            int itemIndex = startIndex + i;

            if (itemIndex < endIndex) {
                // Button has an item
                Item item = allItems.get(itemIndex);
                String buttonText = String.format(
                        "<html><center>%s<br>$%.2f</center></html>",
                        truncateText(item.getDescription(), 20),
                        item.getPrice()
                );
                gridButtons[row][col].setText(buttonText);
                gridButtons[row][col].setEnabled(true);
                gridButtons[row][col].setBackground(new Color(144, 238, 144)); // Light green
            } else {
                // No item for this button
                gridButtons[row][col].setText("Empty");
                gridButtons[row][col].setEnabled(false);
                gridButtons[row][col].setBackground(new Color(230, 230, 230)); // Gray
            }
        }

        // Update pagination controls
        pageLabel.setText(String.format("Page %d of %d", currentPage + 1, totalPages));
        prevButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage < totalPages - 1);
    }

    /**
     * Toggles between showing all items and popular items only.
     */
    private void togglePopular() {
        if (showingPopular) {
            // Switch back to all items
            List<Item> allItems = controller.getAllItems();
            loadItems(allItems);
            popularButton.setText("⭐ Popular");
            popularButton.setBackground(new Color(255, 215, 0)); // Gold
            showingPopular = false;
        } else {
            // Switch to popular items
            List<Item> popularItems = controller.getPopularItems();
            loadItems(popularItems);
            popularButton.setText("📋 All Items");
            popularButton.setBackground(new Color(200, 200, 200)); // Gray
            showingPopular = true;
        }
    }

    /**
     * Truncates text to specified length with ellipsis.
     */
    private String truncateText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Handles item button click.
     */
    private void handleItemButtonClick(int buttonIndex) {
        int itemIndex = currentPage * ITEMS_PER_PAGE + buttonIndex;

        if (itemIndex < allItems.size()) {
            Item item = allItems.get(itemIndex);
            // Add item to transaction via controller
            controller.processUPCScan(item.getUpc(), com.am.register.model.InputSource.QUICK_ADD);
        }
    }

    /**
     * Goes to next page of items.
     */
    private void nextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            updatePageDisplay();
        }
    }

    /**
     * Goes to previous page of items.
     */
    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            updatePageDisplay();
        }
    }

    /**
     * Gets the top panel (for adding scanner input later).
     */
    public JPanel getTopPanel() {
        return (JPanel) getComponent(0);
    }

    /**
     * Enables or disables the entire grid.
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        // Disable all grid buttons
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                if (gridButtons[row][col] != null) {
                    gridButtons[row][col].setEnabled(enabled &&
                            !gridButtons[row][col].getText().equals("Empty"));
                }
            }
        }

        // Disable navigation buttons
        nextButton.setEnabled(enabled && currentPage < totalPages - 1);
        prevButton.setEnabled(enabled && currentPage > 0);
        popularButton.setEnabled(enabled);

        // Visual feedback
        if (!enabled) {
            setBackground(Color.LIGHT_GRAY);
        } else {
            setBackground(Color.WHITE);
        }
    }
}