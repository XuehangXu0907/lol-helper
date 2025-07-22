package com.lol.championselector.manager;

import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.charset.StandardCharsets;

public class SystemTrayManager {
    private static final Logger logger = LoggerFactory.getLogger(SystemTrayManager.class);
    
    private SystemTray systemTray;
    private TrayIcon trayIcon;
    private Stage primaryStage;
    private boolean isSystemTraySupported;
    private Runnable onExitCallback;
    private Timeline trayIconMonitor;
    private AtomicBoolean isInitialized = new AtomicBoolean(false);
    private int initializationRetries = 0;
    private static final int MAX_RETRIES = 3;
    private boolean forceVisible = false;
    private boolean useChineseText = false; // 是否使用中文文本
    
    public SystemTrayManager(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.isSystemTraySupported = SystemTray.isSupported();
        
        if (!isSystemTraySupported) {
            logger.warn("System tray is not supported on this platform");
        }
    }
    
    public boolean isSupported() {
        return isSystemTraySupported;
    }
    
    public void initialize() {
        if (!isSystemTraySupported) {
            logger.warn("Cannot initialize system tray - not supported");
            return;
        }
        
        // Delay initialization to ensure UI is fully loaded
        Platform.runLater(() -> {
            try {
                initializeWithRetry();
            } catch (Exception e) {
                logger.error("Failed to initialize system tray after retries", e);
            }
        });
    }
    
    private void initializeWithRetry() {
        try {
            systemTray = SystemTray.getSystemTray();
            Image trayImage = loadTrayIcon();
            
            if (trayImage == null) {
                logger.error("Failed to load tray icon");
                if (initializationRetries < MAX_RETRIES) {
                    retryInitialization();
                }
                return;
            }
            
            PopupMenu popupMenu = createPopupMenu();
            trayIcon = new TrayIcon(trayImage, "LOL Helper", popupMenu);
            trayIcon.setImageAutoSize(true);
            
            // Force icon to be visible
            forceIconVisibility();
            
            trayIcon.addActionListener(e -> Platform.runLater(this::showWindow));
            
            systemTray.add(trayIcon);
            isInitialized.set(true);
            
            // Start monitoring tray icon visibility
            startTrayIconMonitoring();
            
            logger.info("System tray initialized successfully");
            
            // Show a test notification to verify the icon is working
            Platform.runLater(() -> {
                try {
                    Thread.sleep(500); // Wait a bit for the icon to be fully registered
                    showInfo("LOL Helper", "系统托盘图标已激活");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
            
        } catch (AWTException e) {
            logger.error("Failed to initialize system tray (attempt {})", initializationRetries + 1, e);
            if (initializationRetries < MAX_RETRIES) {
                retryInitialization();
            } else {
                isSystemTraySupported = false;
            }
        }
    }
    
    private void retryInitialization() {
        initializationRetries++;
        logger.info("Retrying system tray initialization (attempt {}/{})", initializationRetries + 1, MAX_RETRIES + 1);
        
        Platform.runLater(() -> {
            try {
                Thread.sleep(1000 * initializationRetries); // Increasing delay
                initializeWithRetry();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    private void forceIconVisibility() {
        if (trayIcon != null) {
            // Set tooltip to ensure icon is recognized by Windows - use simple English to avoid encoding issues
            trayIcon.setToolTip("LOL Helper - League of Legends Assistant");
            
            // Try to make icon more visible
            forceVisible = true;
            
            // Add mouse listeners to increase activity
            trayIcon.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        Platform.runLater(() -> showWindow());
                    }
                }
            });
        }
    }
    
    private void startTrayIconMonitoring() {
        if (trayIconMonitor != null) {
            trayIconMonitor.stop();
        }
        
        // Monitor tray icon every 10 seconds
        trayIconMonitor = new Timeline(new KeyFrame(Duration.seconds(10), e -> {
            checkTrayIconStatus();
        }));
        trayIconMonitor.setCycleCount(Timeline.INDEFINITE);
        trayIconMonitor.play();
    }
    
    private void checkTrayIconStatus() {
        if (!isInitialized.get() || trayIcon == null || systemTray == null) {
            return;
        }
        
        try {
            // Check if our icon is still in the system tray
            TrayIcon[] icons = systemTray.getTrayIcons();
            boolean found = false;
            for (TrayIcon icon : icons) {
                if (icon == trayIcon) {
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                logger.warn("Tray icon disappeared, attempting to re-add");
                try {
                    systemTray.add(trayIcon);
                    logger.info("Tray icon re-added successfully");
                    // 只是重新添加图标，不退出程序
                } catch (AWTException ex) {
                    logger.error("Failed to re-add tray icon", ex);
                    // 即使重新添加失败也不退出程序
                }
            }
        } catch (Exception ex) {
            logger.error("Error checking tray icon status", ex);
            // 检查错误不应该导致程序退出
        }
    }
    
    private Image loadTrayIcon() {
        try {
            // Get system DPI scaling factor
            double scaleFactor = getSystemScaleFactor();
            
            // Try multiple icon sizes for better Windows compatibility
            int[] preferredSizes = {16, 20, 24, 32};
            int targetSize = findBestIconSize(preferredSizes, scaleFactor);
            
            logger.debug("Target icon size: {}x{} (scale factor: {})", targetSize, targetSize, scaleFactor);
            
            // Try to load icon from multiple sources with size variants
            List<String> iconPaths = Arrays.asList(
                "/icon/tray-icon-" + targetSize + ".png",
                "/icon/tray-icon.png",
                "/icon.png",
                "/icon/app-icon.png",
                "/icon/logo.png"
            );
            
            BufferedImage image = null;
            String loadedPath = null;
            
            for (String path : iconPaths) {
                InputStream iconStream = getClass().getResourceAsStream(path);
                if (iconStream != null) {
                    try {
                        image = ImageIO.read(iconStream);
                        loadedPath = path;
                        logger.debug("Loaded tray icon from: {}", path);
                        break;
                    } catch (Exception e) {
                        logger.warn("Failed to read icon from path: {}", path, e);
                    } finally {
                        try {
                            iconStream.close();
                        } catch (Exception e) {
                            // Ignore close exception
                        }
                    }
                }
            }
            
            if (image == null) {
                logger.warn("No tray icon found, creating enhanced default icon with size: {}x{}", targetSize, targetSize);
                return createEnhancedDefaultIcon(targetSize);
            }
            
            // Process image for better tray icon compatibility
            Image processedImage = processIconForTray(image, targetSize);
            logger.debug("Tray icon processed from {} to {}x{}", loadedPath, targetSize, targetSize);
            return processedImage;
            
        } catch (Exception e) {
            logger.error("Failed to load tray icon from resources", e);
            return createEnhancedDefaultIcon(16);
        }
    }
    
    private int findBestIconSize(int[] sizes, double scaleFactor) {
        int targetSize = (int) (16 * scaleFactor);
        
        // Find the closest size from available options
        int bestSize = sizes[0];
        int minDiff = Math.abs(targetSize - bestSize);
        
        for (int size : sizes) {
            int diff = Math.abs(targetSize - size);
            if (diff < minDiff) {
                minDiff = diff;
                bestSize = size;
            }
        }
        
        // Ensure minimum size for visibility
        return Math.max(16, bestSize);
    }
    
    private Image processIconForTray(BufferedImage originalImage, int targetSize) {
        // Create a new image with proper format for system tray
        BufferedImage processedImage = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = processedImage.createGraphics();
        
        // Enable high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw the original image scaled to target size
        g2d.drawImage(originalImage, 0, 0, targetSize, targetSize, null);
        
        // Add a subtle border for better visibility on different backgrounds
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.setColor(new Color(0, 0, 0, 30)); // Semi-transparent black border
        g2d.drawRect(0, 0, targetSize - 1, targetSize - 1);
        
        g2d.dispose();
        return processedImage;
    }
    
    private Image createEnhancedDefaultIcon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // Enable anti-aliasing for better visual quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // Create a more distinctive icon design
        int margin = size / 8;
        int diameter = size - (margin * 2);
        
        // Choose colors based on system theme
        Color primaryColor, secondaryColor;
        if (isWindowsDarkTheme()) {
            primaryColor = Color.decode("#0078D4"); // Windows blue for dark theme
            secondaryColor = Color.decode("#106EBE");
        } else {
            primaryColor = Color.decode("#FF6B35"); // Orange for light theme
            secondaryColor = Color.decode("#F7931E");
        }
        
        // Background with gradient
        GradientPaint gradient = new GradientPaint(
            0, 0, primaryColor,
            0, size, secondaryColor
        );
        g2d.setPaint(gradient);
        g2d.fillOval(margin, margin, diameter, diameter);
        
        // Add outer ring for better visibility
        g2d.setStroke(new BasicStroke(Math.max(1, size/16f)));
        g2d.setColor(Color.WHITE);
        g2d.drawOval(margin, margin, diameter, diameter);
        
        // Draw "LOL" text or "L" depending on size
        g2d.setColor(Color.WHITE);
        String text = size >= 24 ? "LOL" : "L";
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(8, size/3));
        g2d.setFont(font);
        
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (size - fm.stringWidth(text)) / 2;
        int textY = (size - fm.getHeight()) / 2 + fm.getAscent();
        
        // Add text shadow for better readability
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.drawString(text, textX + 1, textY + 1);
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, textX, textY);
        
        g2d.dispose();
        logger.debug("Created enhanced default tray icon with size: {}x{}", size, size);
        return image;
    }
    
    private Image createDefaultIcon() {
        double scaleFactor = getSystemScaleFactor();
        int size = findBestIconSize(new int[]{16, 20, 24, 32}, scaleFactor);
        return createEnhancedDefaultIcon(size);
    }
    
    private PopupMenu createPopupMenu() {
        PopupMenu popupMenu = new PopupMenu();
        
        // 根据语言设置选择文本，但优先使用英文避免编码问题
        String showText = useChineseText ? "Show" : "Show Window";
        String hideText = useChineseText ? "Hide" : "Hide Window";
        String exitText = useChineseText ? "Exit" : "Exit";
        
        MenuItem showItem = new MenuItem(showText);
        showItem.addActionListener(e -> Platform.runLater(this::showWindow));
        popupMenu.add(showItem);
        
        MenuItem hideItem = new MenuItem(hideText);
        hideItem.addActionListener(e -> Platform.runLater(this::hideWindow));
        popupMenu.add(hideItem);
        
        popupMenu.addSeparator();
        
        MenuItem exitItem = new MenuItem(exitText);
        exitItem.addActionListener(e -> {
            // 显示确认对话框防止意外退出
            Platform.runLater(() -> {
                boolean confirmed = showExitConfirmation();
                if (confirmed) {
                    if (onExitCallback != null) {
                        onExitCallback.run();
                    } else {
                        destroy();
                        Platform.exit();
                        System.exit(0);
                    }
                }
            });
        });
        popupMenu.add(exitItem);
        
        return popupMenu;
    }
    
    public void showWindow() {
        if (primaryStage != null) {
            primaryStage.show();
            primaryStage.toFront();
            primaryStage.setIconified(false);
            logger.debug("Window shown from system tray");
        }
    }
    
    public void hideWindow() {
        if (primaryStage != null) {
            primaryStage.hide();
            logger.debug("Window hidden to system tray");
        }
    }
    
    public void setMinimizeToTray(boolean minimizeToTray) {
        if (primaryStage != null) {
            if (minimizeToTray) {
                primaryStage.iconifiedProperty().addListener((obs, wasIconified, isIconified) -> {
                    if (isIconified) {
                        hideWindow();
                    }
                });
            }
        }
    }
    
    public void showNotification(String title, String message, TrayIcon.MessageType messageType) {
        if (trayIcon != null) {
            // Ensure message is not too long and format it properly
            String formattedMessage = formatNotificationMessage(message);
            trayIcon.displayMessage(title, formattedMessage, messageType);
            logger.debug("Displayed notification: {} - {}", title, formattedMessage);
        } else {
            logger.warn("Cannot show notification - tray icon is null");
        }
    }
    
    public void showInfo(String title, String message) {
        showNotification(title, message, TrayIcon.MessageType.INFO);
    }
    
    public void showWarning(String title, String message) {
        showNotification(title, message, TrayIcon.MessageType.WARNING);
    }
    
    public void showError(String title, String message) {
        showNotification(title, message, TrayIcon.MessageType.ERROR);
    }
    
    public void setOnExitCallback(Runnable callback) {
        this.onExitCallback = callback;
    }
    
    public void destroy() {
        if (trayIconMonitor != null) {
            trayIconMonitor.stop();
            trayIconMonitor = null;
        }
        
        if (systemTray != null && trayIcon != null) {
            try {
                systemTray.remove(trayIcon);
                logger.info("System tray removed");
            } catch (Exception e) {
                logger.warn("Error removing tray icon", e);
            }
        }
        
        isInitialized.set(false);
    }
    
    public boolean isInitialized() {
        return isInitialized.get();
    }
    
    public void refreshTrayIcon() {
        if (!isSystemTraySupported || !isInitialized.get()) {
            return;
        }
        
        Platform.runLater(() -> {
            try {
                if (systemTray != null && trayIcon != null) {
                    systemTray.remove(trayIcon);
                    Thread.sleep(100);
                    systemTray.add(trayIcon);
                    logger.info("Tray icon refreshed");
                    showInfo("LOL Helper", "托盘图标已刷新");
                }
            } catch (Exception e) {
                logger.error("Failed to refresh tray icon", e);
            }
        });
    }
    
    private double getSystemScaleFactor() {
        try {
            // Get system DPI scaling factor
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            int dpi = toolkit.getScreenResolution();
            double scaleFactor = dpi / 96.0; // 96 DPI is the standard
            
            // Apply Windows-specific adjustments
            scaleFactor = adjustScaleFactorForWindows(scaleFactor);
            
            // Clamp scale factor to reasonable range
            scaleFactor = Math.max(1.0, Math.min(3.0, scaleFactor));
            
            logger.debug("System DPI: {}, Adjusted scale factor: {}", dpi, scaleFactor);
            return scaleFactor;
        } catch (Exception e) {
            logger.warn("Failed to get system scale factor, using default", e);
            return 1.0;
        }
    }
    
    private double adjustScaleFactorForWindows(double scaleFactor) {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            String osVersion = System.getProperty("os.version");
            
            if (osName.contains("windows")) {
                logger.debug("Windows detected: {} {}", osName, osVersion);
                
                // Windows 10/11 have better DPI handling
                if (osVersion.startsWith("10.") || osVersion.startsWith("11.")) {
                    // For Windows 10/11, use the scale factor as-is but ensure minimum visibility
                    return Math.max(1.25, scaleFactor);
                } else {
                    // For older Windows versions, be more conservative
                    return Math.max(1.0, Math.min(2.0, scaleFactor));
                }
            }
            
            return scaleFactor;
        } catch (Exception e) {
            logger.warn("Failed to detect Windows version for scale factor adjustment", e);
            return scaleFactor;
        }
    }
    
    private boolean isWindowsDarkTheme() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (!os.contains("windows")) {
                return false;
            }
            
            // Try to detect Windows dark theme
            // This is a simplified detection - in a real application you might use JNA
            String theme = System.getProperty("awt.useSystemAAFontSettings");
            logger.debug("System theme hint: {}", theme);
            
            // For now, assume light theme - this could be enhanced with registry reading
            return false;
        } catch (Exception e) {
            logger.debug("Failed to detect Windows theme", e);
            return false;
        }
    }
    
    private String formatNotificationMessage(String message) {
        if (message == null) {
            return "";
        }
        
        // Limit message length to prevent truncation
        int maxLength = 150; // Reduced for better Windows compatibility
        if (message.length() > maxLength) {
            return message.substring(0, maxLength - 3) + "...";
        }
        
        // Ensure proper line breaks for better display
        return message.replace("，", "，\n").replace("。", "。\n").trim();
    }
    
    public String getTrayIconStatus() {
        if (!isSystemTraySupported) {
            return "系统不支持托盘功能";
        }
        if (!isInitialized.get()) {
            return "托盘图标未初始化";
        }
        if (trayIcon == null) {
            return "托盘图标对象为空";
        }
        if (systemTray == null) {
            return "系统托盘对象为空";
        }
        
        try {
            TrayIcon[] icons = systemTray.getTrayIcons();
            for (TrayIcon icon : icons) {
                if (icon == trayIcon) {
                    return "托盘图标正常运行 (共" + icons.length + "个图标)";
                }
            }
            return "托盘图标已丢失 (系统中共" + icons.length + "个图标)";
        } catch (Exception e) {
            return "检查托盘状态时出错: " + e.getMessage();
        }
    }
    
    public String getSystemInfo() {
        try {
            String osName = System.getProperty("os.name");
            String osVersion = System.getProperty("os.version");
            String javaVersion = System.getProperty("java.version");
            
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            int dpi = toolkit.getScreenResolution();
            double scaleFactor = getSystemScaleFactor();
            
            return String.format(
                "OS: %s %s, Java: %s, DPI: %d, 缩放: %.1fx, 托盘支持: %s",
                osName, osVersion, javaVersion, dpi, scaleFactor,
                SystemTray.isSupported() ? "是" : "否"
            );
        } catch (Exception e) {
            return "获取系统信息失败: " + e.getMessage();
        }
    }
    
    // 设置托盘菜单语言
    public void setLanguage(boolean useChinese) {
        this.useChineseText = useChinese;
        // 如果托盘已初始化，重新创建菜单
        if (isInitialized.get() && trayIcon != null) {
            Platform.runLater(() -> {
                PopupMenu newMenu = createPopupMenu();
                trayIcon.setPopupMenu(newMenu);
                logger.debug("Tray menu language updated to: {}", useChinese ? "Chinese" : "English");
            });
        }
    }
    
    private boolean showExitConfirmation() {
        try {
            // 使用AWT的确认对话框（因为JavaFX可能不可用）
            int result = java.awt.Dialog.class.isAssignableFrom(java.awt.Dialog.class) ? 
                showAwtConfirmDialog() : true ? 1 : 0;
            return result == 0; // 0 = Yes, 1 = No
        } catch (Exception e) {
            logger.error("Failed to show exit confirmation dialog", e);
            // 如果对话框失败，默认不退出（更安全）
            return false;
        }
    }
    
    private int showAwtConfirmDialog() {
        // 使用简单的方法确认退出
        // 这里简化处理，实际中可以使用JOptionPane或其他方式
        logger.info("Exit confirmation requested");
        return 0; // 直接返回确认，可以根据需要修改
    }
}