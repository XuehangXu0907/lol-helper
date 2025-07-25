package com.lol.championselector.controller;

import com.lol.championselector.manager.LanguageManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;
import ch.qos.logback.classic.Level;

/**
 * 日志查看器弹窗控制器
 * 提供日志查看、搜索、筛选、导出等功能
 */
public class LogViewerDialogController implements Initializable {
    
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LogViewerDialogController.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // UI Components
    @FXML private Label dialogTitleLabel;
    @FXML private Label logStatsLabel;
    @FXML private Button closeButton;
    @FXML private TextField searchTextField;
    @FXML private Button searchButton;
    @FXML private Button clearSearchButton;
    @FXML private ComboBox<String> logLevelFilterComboBox;
    @FXML private Label runtimeLogLevelLabel;
    @FXML private ComboBox<String> runtimeLogLevelComboBox;
    @FXML private Button copyButton;
    @FXML private Button exportButton;
    @FXML private Button clearLogButton;
    @FXML private ScrollPane logScrollPane;
    @FXML private TextArea logTextArea;
    @FXML private CheckBox autoScrollCheckBox;
    @FXML private CheckBox wordWrapCheckBox;
    @FXML private Label statusLabel;
    @FXML private Button refreshButton;
    @FXML private Button okButton;
    
    // Data
    private final ConcurrentLinkedQueue<String> logBuffer = new ConcurrentLinkedQueue<>();
    private String allLogContent = "";
    private String filteredLogContent = "";
    private Pattern searchPattern = null;
    private String currentLevelFilter = "All";
    
    // External reference
    private TextArea externalLogTextArea;
    private LanguageManager languageManager;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupComponents();
        setupEventHandlers();
        updateLogStats();
        optimizeScrollSpeed();
        initializeRuntimeLogLevel();
    }
    
    /**
     * 设置外部日志文本区域引用
     */
    public void setExternalLogTextArea(TextArea externalLogTextArea) {
        this.externalLogTextArea = externalLogTextArea;
        if (externalLogTextArea != null) {
            // 同步现有日志内容
            Platform.runLater(() -> {
                String existingContent = externalLogTextArea.getText();
                if (!existingContent.isEmpty()) {
                    logTextArea.setText(existingContent);
                    allLogContent = existingContent;
                    updateLogStats();
                    scrollToBottom();
                }
            });
        }
    }
    
    /**
     * 设置语言管理器
     */
    public void setLanguageManager(LanguageManager languageManager) {
        this.languageManager = languageManager;
        updateLanguage();
    }
    
    /**
     * 添加日志消息
     */
    public void appendLogMessage(String message) {
        Platform.runLater(() -> {
            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
            String formattedMessage = String.format("[%s] %s\n", timestamp, message);
            
            logBuffer.offer(formattedMessage);
            allLogContent += formattedMessage;
            
            applyFilters();
            updateLogStats();
            
            if (autoScrollCheckBox.isSelected()) {
                scrollToBottom();
            }
        });
    }
    
    private void setupComponents() {
        // 设置文本区域属性
        logTextArea.setEditable(false);
        wordWrapCheckBox.setSelected(false);
        autoScrollCheckBox.setSelected(true);
        
        // 设置日志级别过滤器选项和默认值
        logLevelFilterComboBox.getItems().clear();
        logLevelFilterComboBox.getItems().addAll(
            languageManager != null ? languageManager.getString("dialog.logViewer.allLevels") : "全部", 
            "DEBUG", "INFO", "WARN", "ERROR");
        logLevelFilterComboBox.setValue(
            languageManager != null ? languageManager.getString("dialog.logViewer.allLevels") : "全部");
        
        // 设置搜索字段回车事件
        searchTextField.setOnAction(e -> onSearchClicked());
        
        // 设置运行时日志级别默认值
        if (runtimeLogLevelComboBox != null) {
            runtimeLogLevelComboBox.setValue("INFO");
        }
    }
    
    private void setupEventHandlers() {
        // 监听自动换行复选框
        wordWrapCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            logTextArea.setWrapText(newVal);
        });
        
        // 监听搜索文本变化
        searchTextField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.trim().isEmpty()) {
                searchPattern = null;
                applyFilters();
            }
        });
    }
    
    private void optimizeScrollSpeed() {
        if (logScrollPane != null) {
            logger.debug("Setting up scroll speed optimization for log viewer");
            
            // Set faster scroll speed with fixed pixel increment
            logScrollPane.setOnScroll(event -> {
                if (event.getDeltaY() != 0) {
                    // Use fixed pixel increment for more predictable scrolling
                    double scrollPixels = event.getDeltaY() > 0 ? -120 : 120; // 120 pixels per scroll
                    double contentHeight = logScrollPane.getContent().getBoundsInLocal().getHeight();
                    double viewportHeight = logScrollPane.getViewportBounds().getHeight();
                    
                    if (contentHeight > viewportHeight) {
                        double currentVvalue = logScrollPane.getVvalue();
                        double scrollableHeight = contentHeight - viewportHeight;
                        double scrollAmount = scrollPixels / scrollableHeight;
                        double newVvalue = currentVvalue + scrollAmount;
                        
                        // Clamp to valid range [0, 1]
                        newVvalue = Math.max(0, Math.min(1, newVvalue));
                        
                        logScrollPane.setVvalue(newVvalue);
                    }
                    
                    event.consume();
                }
            });
            
            logger.debug("Scroll speed optimization configured for log viewer");
        }
    }
    
    @FXML
    private void onCloseClicked() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
    
    @FXML
    private void onSearchClicked() {
        String searchText = searchTextField.getText().trim();
        if (searchText.isEmpty()) {
            searchPattern = null;
        } else {
            try {
                searchPattern = Pattern.compile(Pattern.quote(searchText), Pattern.CASE_INSENSITIVE);
            } catch (Exception e) {
                logger.warn("Invalid search pattern: {}", searchText, e);
                showAlert(
                    languageManager != null ? languageManager.getString("dialog.logViewer.searchError") : "搜索错误",
                    languageManager != null ? languageManager.getString("dialog.logViewer.searchErrorMessage") : "无效的搜索内容");
                return;
            }
        }
        applyFilters();
    }
    
    @FXML
    private void onClearSearchClicked() {
        searchTextField.clear();
        searchPattern = null;
        applyFilters();
    }
    
    @FXML
    private void onLogLevelFilterChanged() {
        currentLevelFilter = logLevelFilterComboBox.getValue();
        if (currentLevelFilter == null) {
            currentLevelFilter = languageManager != null ? languageManager.getString("dialog.logViewer.allLevels") : "全部";
        }
        applyFilters();
    }
    
    @FXML
    private void onCopyClicked() {
        String selectedText = logTextArea.getSelectedText();
        if (selectedText.isEmpty()) {
            selectedText = logTextArea.getText();
        }
        
        if (!selectedText.isEmpty()) {
            ClipboardContent content = new ClipboardContent();
            content.putString(selectedText);
            Clipboard.getSystemClipboard().setContent(content);
            
            statusLabel.setText(languageManager != null ? languageManager.getString("dialog.logViewer.copiedToClipboard") : "已复制到剪贴板");
            // 3秒后恢复状态文本
            Platform.runLater(() -> {
                try {
                    Thread.sleep(3000);
                    Platform.runLater(() -> statusLabel.setText(languageManager != null ? languageManager.getString("dialog.logViewer.realTimeDisplay") : "实时显示"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }
    
    @FXML
    private void onExportClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(languageManager != null ? languageManager.getString("dialog.logViewer.exportLog") : "导出日志文件");
        fileChooser.setInitialFileName("log_export_" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(languageManager != null ? languageManager.getString("dialog.logViewer.textFile") : "文本文件", "*.txt"));
        
        Stage stage = (Stage) exportButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(filteredLogContent.isEmpty() ? allLogContent : filteredLogContent);
                statusLabel.setText((languageManager != null ? languageManager.getString("dialog.logViewer.logExported") : "日志已导出至: ") + file.getName());
                
                // 3秒后恢复状态文本
                Platform.runLater(() -> {
                    try {
                        Thread.sleep(3000);
                        Platform.runLater(() -> statusLabel.setText(languageManager != null ? languageManager.getString("dialog.logViewer.realTimeDisplay") : "实时显示"));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (IOException e) {
                logger.error("Failed to export log", e);
                showAlert(
                    languageManager != null ? languageManager.getString("dialog.logViewer.exportError") : "导出失败", 
                    (languageManager != null ? languageManager.getString("dialog.logViewer.exportErrorMessage") : "无法保存日志文件: ") + e.getMessage());
            }
        }
    }
    
    @FXML
    private void onClearLogClicked() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(languageManager != null ? languageManager.getString("dialog.logViewer.confirmClear") : "确认清空");
        alert.setHeaderText(languageManager != null ? languageManager.getString("dialog.logViewer.clearLogTitle") : "清空日志");
        alert.setContentText(languageManager != null ? languageManager.getString("dialog.logViewer.clearLogMessage") : "确定要清空所有日志记录吗？此操作不可恢复。");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                clearAllLogs();
            }
        });
    }
    
    @FXML
    private void onAutoScrollToggled() {
        if (autoScrollCheckBox.isSelected()) {
            scrollToBottom();
        }
    }
    
    @FXML
    private void onWordWrapToggled() {
        // Event handler is set in setupEventHandlers()
    }
    
    @FXML
    private void onRefreshClicked() {
        if (externalLogTextArea != null) {
            String externalContent = externalLogTextArea.getText();
            if (!externalContent.equals(allLogContent)) {
                allLogContent = externalContent;
                applyFilters();
                updateLogStats();
                statusLabel.setText(languageManager != null ? languageManager.getString("dialog.logViewer.refreshed") : "已刷新");
                
                // 2秒后恢复状态文本
                Platform.runLater(() -> {
                    try {
                        Thread.sleep(2000);
                        Platform.runLater(() -> statusLabel.setText(languageManager != null ? languageManager.getString("dialog.logViewer.realTimeDisplay") : "实时显示"));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }
    }
    
    @FXML
    private void onOkClicked() {
        onCloseClicked();
    }
    
    @FXML
    private void onRuntimeLogLevelChanged() {
        String selectedLevel = runtimeLogLevelComboBox.getValue();
        if (selectedLevel != null) {
            setRuntimeLogLevel(selectedLevel);
            String message = languageManager != null ? 
                languageManager.getString("dialog.logViewer.logLevelChanged") + " " + selectedLevel :
                "日志级别已设置为: " + selectedLevel;
            statusLabel.setText(message);
            
            // 3秒后恢复状态文本
            Platform.runLater(() -> {
                try {
                    Thread.sleep(3000);
                    Platform.runLater(() -> statusLabel.setText(languageManager != null ? 
                        languageManager.getString("dialog.logViewer.realTimeDisplay") : "实时显示"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }
    
    private void applyFilters() {
        String content = allLogContent;
        
        // 应用日志级别过滤
        String allLevelsText = languageManager != null ? languageManager.getString("dialog.logViewer.allLevels") : "全部";
        if (!allLevelsText.equals(currentLevelFilter)) {
            StringBuilder filtered = new StringBuilder();
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.contains(currentLevelFilter)) {
                    filtered.append(line).append("\n");
                }
            }
            content = filtered.toString();
        }
        
        // 应用搜索过滤
        if (searchPattern != null) {
            StringBuilder filtered = new StringBuilder();
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (searchPattern.matcher(line).find()) {
                    filtered.append(line).append("\n");
                }
            }
            content = filtered.toString();
        }
        
        filteredLogContent = content;
        logTextArea.setText(content);
        
        if (autoScrollCheckBox.isSelected()) {
            scrollToBottom();
        }
    }
    
    private void clearAllLogs() {
        logBuffer.clear();
        allLogContent = "";
        filteredLogContent = "";
        logTextArea.clear();
        
        // 同时清空外部日志区域
        if (externalLogTextArea != null) {
            externalLogTextArea.clear();
        }
        
        updateLogStats();
        statusLabel.setText(languageManager != null ? languageManager.getString("dialog.logViewer.logCleared") : "日志已清空");
    }
    
    private void scrollToBottom() {
        Platform.runLater(() -> {
            logTextArea.setScrollTop(Double.MAX_VALUE);
            logScrollPane.setVvalue(1.0);
        });
    }
    
    private void updateLogStats() {
        String content = filteredLogContent.isEmpty() ? allLogContent : filteredLogContent;
        int lineCount = content.isEmpty() ? 0 : content.split("\n").length;
        
        String allLevelsText = languageManager != null ? languageManager.getString("dialog.logViewer.allLevels") : "全部";
        if (searchPattern != null || !allLevelsText.equals(currentLevelFilter)) {
            int totalLines = allLogContent.isEmpty() ? 0 : allLogContent.split("\n").length;
            String displayFormat = languageManager != null ? languageManager.getString("dialog.logViewer.displayEntries") : "显示 %d 条，共 %d 条日志记录";
            logStatsLabel.setText(String.format(displayFormat, lineCount, totalLines));
        } else {
            String totalFormat = languageManager != null ? languageManager.getString("dialog.logViewer.totalEntries") : "共 %d 条日志记录";
            logStatsLabel.setText(String.format(totalFormat, lineCount));
        }
    }
    
    private void updateLanguage() {
        if (languageManager != null) {
            dialogTitleLabel.setText(languageManager.getString("dialog.logViewer.title"));
            
            // 更新ComboBox选项
            String currentSelection = logLevelFilterComboBox.getValue();
            logLevelFilterComboBox.getItems().clear();
            logLevelFilterComboBox.getItems().addAll(
                languageManager.getString("dialog.logViewer.allLevels"), 
                "DEBUG", "INFO", "WARN", "ERROR");
            
            // 保持当前选择或设置默认值
            if (currentSelection != null) {
                logLevelFilterComboBox.setValue(currentSelection);
            } else {
                logLevelFilterComboBox.setValue(languageManager.getString("dialog.logViewer.allLevels"));
            }
            
            // 更新运行时日志级别标签
            if (runtimeLogLevelLabel != null) {
                runtimeLogLevelLabel.setText(languageManager.getString("dialog.logViewer.runtimeLogLevel"));
            }
            
            // 更新状态标签
            if (statusLabel.getText().isEmpty() || statusLabel.getText().equals("实时显示") || statusLabel.getText().equals("Real-time display")) {
                statusLabel.setText(languageManager.getString("dialog.logViewer.realTimeDisplay"));
            }
            
            // 刷新统计信息
            updateLogStats();
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void initializeRuntimeLogLevel() {
        if (runtimeLogLevelComboBox != null) {
            // 获取当前日志级别
            org.slf4j.Logger rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            if (rootLogger instanceof ch.qos.logback.classic.Logger) {
                ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger) rootLogger;
                Level currentLevel = logbackLogger.getLevel();
                if (currentLevel != null) {
                    runtimeLogLevelComboBox.setValue(currentLevel.toString());
                }
            }
        }
    }
    
    private void setRuntimeLogLevel(String levelStr) {
        try {
            Level level = Level.toLevel(levelStr);
            org.slf4j.Logger rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            if (rootLogger instanceof ch.qos.logback.classic.Logger) {
                ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger) rootLogger;
                logbackLogger.setLevel(level);
                logger.info("运行时日志级别已设置为: {}", level);
            }
        } catch (Exception e) {
            logger.error("设置日志级别失败: {}", levelStr, e);
        }
    }
}