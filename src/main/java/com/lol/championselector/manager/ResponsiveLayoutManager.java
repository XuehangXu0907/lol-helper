package com.lol.championselector.manager;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ResponsiveLayoutManager {
    private static final Logger logger = LoggerFactory.getLogger(ResponsiveLayoutManager.class);
    
    private static final int MIN_COLUMNS = 3;
    private static final int MAX_COLUMNS = 12;
    private static final int BUTTON_WIDTH_WITH_SPACING = 130; // 120px button + 10px spacing
    private static final int WINDOW_MARGIN = 60; // scrollbar and margins
    
    public int calculateOptimalColumns(double windowWidth) {
        double availableWidth = windowWidth - WINDOW_MARGIN;
        int calculatedColumns = (int) (availableWidth / BUTTON_WIDTH_WITH_SPACING);
        int optimalColumns = Math.max(MIN_COLUMNS, Math.min(MAX_COLUMNS, calculatedColumns));
        
        logger.debug("Window width: {}, Available width: {}, Calculated columns: {}, Optimal columns: {}", 
                    windowWidth, availableWidth, calculatedColumns, optimalColumns);
        
        return optimalColumns;
    }
    
    public void rearrangeChampionGrid(GridPane gridPane, List<Node> championButtons, int newColumns) {
        if (gridPane == null || championButtons == null || championButtons.isEmpty()) {
            logger.warn("Invalid parameters for grid rearrangement");
            return;
        }
        
        if (newColumns <= 0) {
            logger.warn("Invalid column count: {}", newColumns);
            return;
        }
        
        logger.debug("Rearranging grid with {} champions into {} columns", championButtons.size(), newColumns);
        
        gridPane.getChildren().clear();
        
        for (int i = 0; i < championButtons.size(); i++) {
            int row = i / newColumns;
            int col = i % newColumns;
            
            Node championButton = championButtons.get(i);
            GridPane.setConstraints(championButton, col, row);
            gridPane.getChildren().add(championButton);
        }
        
        configureColumnConstraints(gridPane, newColumns);
        
        logger.debug("Grid rearranged successfully with {} rows and {} columns", 
                    (championButtons.size() + newColumns - 1) / newColumns, newColumns);
    }
    
    private void configureColumnConstraints(GridPane gridPane, int columnCount) {
        gridPane.getColumnConstraints().clear();
        
        double percentWidth = 100.0 / columnCount;
        
        for (int i = 0; i < columnCount; i++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setHgrow(Priority.ALWAYS);
            constraints.setPercentWidth(percentWidth);
            constraints.setFillWidth(true);
            gridPane.getColumnConstraints().add(constraints);
        }
        
        logger.debug("Configured {} column constraints with {}% width each", columnCount, percentWidth);
    }
    
    public boolean shouldRearrange(int currentColumns, int newColumns) {
        return currentColumns != newColumns;
    }
    
    public int getMinColumns() {
        return MIN_COLUMNS;
    }
    
    public int getMaxColumns() {
        return MAX_COLUMNS;
    }
    
    public int getButtonWidthWithSpacing() {
        return BUTTON_WIDTH_WITH_SPACING;
    }
    
    public int getWindowMargin() {
        return WINDOW_MARGIN;
    }
    
    public String getLayoutInfo(double windowWidth, int columns, int championCount) {
        int rows = (championCount + columns - 1) / columns;
        return String.format("窗口宽度: %.0fpx, 列数: %d, 行数: %d, 英雄数量: %d", 
                           windowWidth, columns, rows, championCount);
    }
    
    public void configureResponsiveLabels(List<Label> labels, double containerWidth) {
        if (labels == null || labels.isEmpty()) {
            return;
        }
        
        double availableWidth = containerWidth - WINDOW_MARGIN;
        
        for (Label label : labels) {
            configureResponsiveLabel(label, availableWidth);
        }
        
        logger.debug("Configured {} responsive labels for container width: {}", labels.size(), containerWidth);
    }
    
    public void configureResponsiveLabel(Label label, double availableWidth) {
        if (label == null) {
            return;
        }
        
        // Enable text wrapping for long text
        label.setWrapText(true);
        
        // Set maximum width based on available space
        double maxWidth = Math.max(100, availableWidth * 0.8); // Use 80% of available width
        label.setMaxWidth(maxWidth);
        
        // Allow the label to grow with its container
        label.maxWidthProperty().bind(label.getParent() instanceof Region ? 
            ((Region) label.getParent()).widthProperty().multiply(0.8) : 
            label.maxWidthProperty());
            
        logger.debug("Configured responsive label with max width: {}", maxWidth);
    }
    
    public double calculateOptimalTextWidth(String text, double containerWidth) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        double availableWidth = containerWidth - WINDOW_MARGIN;
        
        // Estimate text width (rough calculation)
        double estimatedCharWidth = 8; // pixels per character (approximate)
        double estimatedTextWidth = text.length() * estimatedCharWidth;
        
        // If text is too long, use word wrapping
        if (estimatedTextWidth > availableWidth * 0.8) {
            return availableWidth * 0.8;
        }
        
        return Math.min(estimatedTextWidth, availableWidth * 0.8);
    }
    
    public int calculateTextLines(String text, double maxWidth) {
        if (text == null || text.isEmpty()) {
            return 1;
        }
        
        double estimatedCharWidth = 8; // pixels per character (approximate)
        double textWidth = text.length() * estimatedCharWidth;
        
        return Math.max(1, (int) Math.ceil(textWidth / maxWidth));
    }
    
    public boolean isTextTruncated(String text, double availableWidth) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        double estimatedCharWidth = 8;
        double textWidth = text.length() * estimatedCharWidth;
        
        return textWidth > availableWidth;
    }
}