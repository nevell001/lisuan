package com.cashier;

import com.cashier.controller.LoginController;
import com.cashier.controller.MainController;
import com.cashier.controller.PosModeController;
import com.cashier.constant.FXConstants;
import com.cashier.constant.SpacingConstants;
import com.cashier.service.DataService;
import com.cashier.model.User;
import com.cashier.util.FXMLUtils;
import com.cashier.util.FXUtils;
import com.cashier.util.LoggerFactoryUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;

/**
 * 收银系统 JavaFX 主应用类
 * 负责初始化 JavaFX 应用和加载主界面
 */
public class CashierSystemFXApplication extends Application {

    private static final Logger logger = LoggerFactoryUtil.getLogger(CashierSystemFXApplication.class);
    private static final String APP_TITLE = "狸算(LiSuan)收银系统";
    private static final double WINDOW_WIDTH = 1300;
    private static final double WINDOW_HEIGHT = 800;

    private static CashierSystemFXApplication instance;

    private Stage primaryStage;
    private User currentUser;
    private Object currentController; // 当前活动的控制器，用于清理资源

    // 单实例控制
    private static final String APP_LOCK_FILE = System.getProperty("java.io.tmpdir") + java.io.File.separator + "cashier-system.lock";
    private static java.nio.channels.FileLock fileLock;
    private static java.nio.channels.FileChannel channel;

    @Override
    public void init() throws Exception {
        notifyPreloader(new javafx.application.Preloader.ProgressNotification(0.1));

        // 加载自定义中文字体（在单例检查之前，确保字体可用）
        loadCustomFonts();

        // 单实例检查 - 防止应用多次启动导致数据冲突
        java.io.File lockFile = new java.io.File(APP_LOCK_FILE);
        try {
            channel = new java.io.RandomAccessFile(lockFile, "rw").getChannel();
            fileLock = channel.tryLock();
        } catch (java.io.IOException e) {
            logger.error("无法获取文件锁", e);
            throw e;
        }

        if (fileLock == null) {
            // 已有实例运行
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle(com.cashier.i18n.I18nManager.getInstance().get("app.single_instance.title"));
                alert.setHeaderText(null);
                alert.setContentText(com.cashier.i18n.I18nManager.getInstance().get("app.single_instance.message"));
                alert.showAndWait();
                Platform.exit();
                System.exit(0);
            });
            throw new Exception("Application already running");
        }

        notifyPreloader(new javafx.application.Preloader.ProgressNotification(0.2));
        logger.info("应用初始化完成，已获取单实例锁");
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        instance = this;

        notifyPreloader(new javafx.application.Preloader.ProgressNotification(0.3));

        // 检查数据库配置
        checkDatabaseConfiguration();

        // 立即设置应用图标（同步）
        setupApplicationIcon();

        // 加载语言偏好（同步，轻量级）
        String savedLanguage = DataService.loadLanguagePreference();
        com.cashier.i18n.I18nManager.getInstance().setLocale(savedLanguage);
        logger.info("应用启动 - 已加载语言偏好: {}, I18nManager 当前语言: {}", savedLanguage, com.cashier.i18n.I18nManager.getInstance().getCurrentLanguageTag());

        notifyPreloader(new javafx.application.Preloader.ProgressNotification(0.5));

        // 加载登录界面（同步）
        loadLoginScene();

        // 配置主窗口（同步）
        configurePrimaryStage();

        notifyPreloader(new javafx.application.Preloader.ProgressNotification(0.7));

        // 立即显示窗口 - 不等待后台初始化
        primaryStage.show();

        notifyPreloader(new javafx.application.Preloader.ProgressNotification(0.9));

        // 异步初始化后台服务 - 启动后立即执行
        CompletableFuture.runAsync(() -> {
            try {
                logger.debug("开始异步初始化后台服务...");
                long startTime = System.currentTimeMillis();

                // 初始化数据管理器（轻量级）
                DataService.initialize();

                // 初始化缓存管理器
                com.cashier.util.CacheManager.initialize();

                // 预热缓存（可能耗时较长）
                com.cashier.util.CacheManager.warmupCache();

                // 启动完成
                notifyPreloader(new javafx.application.Preloader.StateChangeNotification(
                    javafx.application.Preloader.StateChangeNotification.Type.BEFORE_START));

                long elapsed = System.currentTimeMillis() - startTime;
                logger.info("后台服务初始化完成，耗时: {}ms", elapsed);
            } catch (Exception e) {
                logger.error("后台服务初始化失败", e);
            }
        });

        // 延迟启动非关键后台服务（库存预警、备份、API）
        // 这些服务在用户登录后才会真正工作，延迟启动不影响用户体验
        java.util.Timer timer = new java.util.Timer(true); // 守护线程
        timer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                logger.debug("延迟启动非关键后台服务...");
            }
        }, 3000); // 3秒后启动
    }

    @Override
    public void stop() {
        // 释放单实例锁
        try {
            if (fileLock != null && fileLock.isValid()) {
                fileLock.release();
                logger.info("单实例锁已释放");
            }
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (java.io.IOException e) {
            logger.error("释放单实例锁时发生错误", e);
        }

        // 清理资源
        logger.info("应用程序已停止");
    }

    /**
     * 获取应用程序实例
     * @return 应用程序实例
     */
    public static CashierSystemFXApplication getInstance() {
        return instance;
    }

    /**
     * 加载自定义中文字体
     * 确保在所有平台上中文都能正确显示
     */
    private void loadCustomFonts() {
        try {
            // 加载 Noto Sans SC Regular 字体
            Font notoSansRegular = Font.loadFont(
                getClass().getResourceAsStream("/fonts/NotoSansSC-Regular.ttc"), 14
            );
            if (notoSansRegular != null) {
                logger.info("成功加载 Noto Sans SC Regular 字体: {}",
                    notoSansRegular.getName());
            } else {
                logger.warn("Noto Sans SC Regular 字体加载失败");
            }

            // 加载 Noto Sans SC Bold 字体
            Font notoSansBold = Font.loadFont(
                getClass().getResourceAsStream("/fonts/NotoSansSC-Bold.ttc"), 14
            );
            if (notoSansBold != null) {
                logger.info("成功加载 Noto Sans SC Bold 字体: {}",
                    notoSansBold.getName());
            } else {
                logger.warn("Noto Sans SC Bold 字体加载失败");
            }

            logger.debug("自定义字体加载完成");
        } catch (Exception e) {
            logger.error("加载自定义字体时发生错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 设置应用图标
     */
    private void setupApplicationIcon() {
        try {
            URL iconUrl = getClass().getResource("/images/logos/app-icon.png");
            if (iconUrl != null) {
                primaryStage.getIcons().add(new Image(iconUrl.toExternalForm()));
            }
        } catch (Exception e) {
            logger.warn("无法加载应用图标: {}", e.getMessage(), e);
        }
    }

    /**
     * 加载登录界面
     */
    private void loadLoginScene() {
        try {
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/LoginView.fxml");
            Parent root = loader.load();

            // 获取控制器并设置应用程序引用
            LoginController controller = loader.getController();
            controller.setApplication(this);

            // 创建场景
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

            // 应用默认主题
            applyTheme(scene, FXConstants.DEFAULT_THEME);

            // 设置场景
            primaryStage.setScene(scene);

        } catch (IOException e) {
            logger.error("加载登录界面失败", e);
            System.exit(1);
        }
    }

    /**
     * 加载主界面
     */
    private void loadMainScene() {
        try {
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/MainView.fxml");
            Parent root = loader.load();

            // 获取控制器并设置应用程序引用
            MainController controller = loader.getController();
            controller.setApplication(this);
            currentController = controller; // 保存控制器引用

            // 创建场景
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

            // 应用主题
            String currentTheme = DataService.loadThemePreference();
            applyTheme(scene, currentTheme);

            // 设置场景
            primaryStage.setScene(scene);

        } catch (IOException e) {
            logger.error("加载主界面失败", e);
        }
    }

    /**
     * 配置主窗口
     */
    private void configurePrimaryStage() {
        primaryStage.setTitle(APP_TITLE);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(600);
        primaryStage.centerOnScreen();

        // 窗口关闭事件处理
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            handleExit();
        });
    }

    /**
     * 应用主题
     * @param scene 场景
     * @param themeName 主题名称
     */
    public void applyTheme(Scene scene, String themeName) {
        if (scene == null) {
            return;
        }

        // 清除现有样式表
        scene.getStylesheets().clear();

        // 添加主样式表
        URL mainStylesheet = getClass().getResource("/css/styles.css");
        if (mainStylesheet != null) {
            scene.getStylesheets().add(mainStylesheet.toExternalForm());
        }

        // 添加主题样式表
        String themeCss = "/css/" + themeName + "-theme.css";
        URL themeStylesheet = getClass().getResource(themeCss);
        if (themeStylesheet != null) {
            scene.getStylesheets().add(themeStylesheet.toExternalForm());
        }

        // 保存主题设置（仅在用户登录后）
        if (currentUser != null) {
            DataService.saveThemePreference(currentUser.username, themeName);
        }
    }

    /**
     * 应用字号
     * @param scene 场景
     * @param fontSize 字号代码 (small, medium, large, extra-large)
     */
    public void applyFontSize(Scene scene, String fontSize) {
        if (scene == null) {
            return;
        }

        // 移除现有的字号样式类
        scene.getRoot().getStyleClass().removeAll("font-size-small", "font-size-medium", "font-size-large", "font-size-extra-large");

        // 添加新的字号样式类
        String styleClass = "font-size-" + fontSize;
        scene.getRoot().getStyleClass().add(styleClass);

        logger.info("应用字号: {}", fontSize);
    }

    /**
     * 处理退出
     */
    private void handleExit() {
        // 检查是否有进行中的班次
        if (DataService.hasActiveShift()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认退出");
            alert.setHeaderText(null);
            alert.setContentText("当前有活跃班次未交班！\n\n确定要退出系统吗？\n\n提示：建议先交班后再退出。");
    
            ButtonType yesButton = new ButtonType("先交班", ButtonBar.ButtonData.YES);
            ButtonType noButton = new ButtonType("直接退出", ButtonBar.ButtonData.NO);
            ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
    
            alert.getButtonTypes().setAll(yesButton, noButton, cancelButton);
    
            alert.showAndWait().ifPresent(buttonType -> {
                if (buttonType == yesButton) {
                    // 用户选择先交班，这里不执行任何操作
                    // 因为无法直接切换到交班页面，用户需要手动操作
                    logger.info("用户选择先交班");
                } else if (buttonType == noButton) {
                    // 用户选择直接退出
                    shutdown();
                    System.exit(0);
                }
                // 如果选择取消，不做任何操作
            });
        } else {
            // 没有活跃班次，直接退出
            if (FXUtils.showConfirmAlert("确认退出", "确定要退出系统吗？")) {
                shutdown();
                System.exit(0);
            }
        }
    }
    
    /**
     * 关闭系统服务
     */
    private void shutdown() {
        try {
            logger.info("正在关闭系统服务...");

            // 停止库存预警服务
            try {
                com.cashier.service.InventoryAlertService.getInstance().stop();
                logger.info("库存预警服务已停止");
            } catch (Exception e) {
                logger.error("停止库存预警服务时发生错误", e);
            }

            // 停止自动备份服务
            try {
                com.cashier.service.BackupService.getInstance().stop();
                logger.info("自动备份服务已停止");
            } catch (Exception e) {
                logger.error("停止自动备份服务时发生错误", e);
            }

            // 停止 REST API 服务器
            try {
                com.cashier.api.ApiServer.getInstance().stop();
                logger.info("REST API 服务器已停止");
            } catch (Exception e) {
                logger.error("停止 REST API 服务器时发生错误", e);
            }

            // 关闭通知管理器
            try {
                com.cashier.notification.NotificationManager.getInstance().shutdown();
                logger.info("通知管理器已关闭");
            } catch (Exception e) {
                logger.error("关闭通知管理器时发生错误", e);
            }

            // 关闭 UI 优化器
            try {
                com.cashier.util.UIOptimizer.shutdown();
                logger.info("UI 优化器已关闭");
            } catch (Exception e) {
                logger.error("关闭 UI 优化器时发生错误", e);
            }

            logger.info("系统服务已关闭");
        } catch (Exception e) {
            logger.error("关闭系统服务时发生错误", e);
        }
    }

    /**
     * 切换到主界面（登录成功后）
     * 根据用户角色选择进入主界面或POS模式
     * @param user 当前登录用户
     */
    public void switchToMainView(User user) {
        this.currentUser = user;

        // 根据角色决定进入哪个界面
        if ("cashier".equals(user.role)) {
            // 收银员进入POS模式（简化界面）
            switchToPosModeView(user);
        } else {
            // 管理员和财务进入完整主界面
            loadFullMainView(user);
        }
    }

    /**
     * 加载POS模式界面（专为收银员设计）
     * @param user 当前登录用户
     */
    private void switchToPosModeView(User user) {
        try {
            // 加载用户特定的语言偏好并应用到 I18nManager
            // 必须在加载 FXML 之前设置，确保使用正确的 ResourceBundle
            String userLanguage = DataService.loadLanguagePreference(user.username);
            com.cashier.i18n.I18nManager.getInstance().setLocale(userLanguage);
            logger.info("用户 {} 的语言偏好: {}, I18nManager 当前语言: {}",
                       user.username, userLanguage, com.cashier.i18n.I18nManager.getInstance().getCurrentLanguageTag());

            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/PosModeView.fxml");
            Parent root = loader.load();

            // 获取控制器并设置应用程序引用
            PosModeController controller = loader.getController();
            controller.setApplication(this);
            controller.setCurrentUser(user);
            currentController = controller; // 保存控制器引用

            // 创建场景
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

            // 应用主题
            String currentTheme = DataService.loadThemePreference();
            applyTheme(scene, currentTheme);

            // 应用字号
            String currentFontSize = DataService.loadFontSizePreference(user.username);
            applyFontSize(scene, currentFontSize);

            // 设置场景
            primaryStage.setScene(scene);

            // 更新窗口标题
            primaryStage.setTitle(APP_TITLE + " - 收银台 - " + user.name);

            logger.info("用户 {} ({}) 进入POS模式", user.name, user.getRoleDisplayName());

            // 启动库存预警服务
            try {
                com.cashier.service.InventoryAlertService.getInstance().start();
                logger.info("库存预警服务已启动");
            } catch (Exception e) {
                logger.error("启动库存预警服务失败", e);
            }

            // 启动自动备份服务
            try {
                com.cashier.service.BackupService.getInstance().start();
                logger.info("自动备份服务已启动");
            } catch (Exception e) {
                logger.error("启动自动备份服务失败", e);
            }

        } catch (IOException e) {
            logger.error("加载POS模式界面失败", e);
        }
    }

    /**
     * 加载完整主界面（管理员和财务）
     * @param user 当前登录用户
     */
    private void loadFullMainView(User user) {
        try {
            // 加载用户特定的语言偏好并应用到 I18nManager
            // 必须在加载 FXML 之前设置，确保使用正确的 ResourceBundle
            String userLanguage = DataService.loadLanguagePreference(user.username);
            com.cashier.i18n.I18nManager.getInstance().setLocale(userLanguage);
            logger.info("用户 {} 的语言偏好: {}, I18nManager 当前语言: {}",
                       user.username, userLanguage, com.cashier.i18n.I18nManager.getInstance().getCurrentLanguageTag());

            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/MainView.fxml");
            Parent root = loader.load();

            // 获取控制器并设置应用程序引用
            MainController controller = loader.getController();
            controller.setApplication(this);
            controller.setCurrentUser(user);
            currentController = controller; // 保存控制器引用

            // 创建场景
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

            // 应用主题
            String currentTheme = DataService.loadThemePreference();
            applyTheme(scene, currentTheme);

            // 应用字号
            String currentFontSize = DataService.loadFontSizePreference(user.username);
            applyFontSize(scene, currentFontSize);

            // 设置场景
            primaryStage.setScene(scene);

            // 更新窗口标题
            primaryStage.setTitle(APP_TITLE + " - " + user.name + " (" + user.getRoleDisplayName() + ")");

            logger.info("用户 {} ({}) 进入完整主界面", user.name, user.getRoleDisplayName());

            // 启动库存预警服务
            try {
                com.cashier.service.InventoryAlertService.getInstance().start();
                logger.info("库存预警服务已启动");
            } catch (Exception e) {
                logger.error("启动库存预警服务失败", e);
            }

            // 启动自动备份服务
            try {
                com.cashier.service.BackupService.getInstance().start();
                logger.info("自动备份服务已启动");
            } catch (Exception e) {
                logger.error("启动自动备份服务失败", e);
            }

            // 启动 REST API 服务器
            try {
                com.cashier.api.ApiServer apiServer = com.cashier.api.ApiServer.getInstance();
                int apiPort = com.cashier.api.ApiConfig.getPort();
                apiServer.start(apiPort);
                logger.info("REST API 服务器已启动，端口: {}", apiPort);
            } catch (Exception e) {
                logger.error("启动 REST API 服务器失败", e);
            }

        } catch (IOException e) {
            logger.error("加载主界面失败", e);
        }
    }

    /**
     * 返回登录界面（退出登录）
     */
    public void logoutToLoginView() {
        // 清理当前控制器资源
        try {
            if (currentController instanceof com.cashier.controller.MainController) {
                ((com.cashier.controller.MainController) currentController).cleanup();
            } else if (currentController instanceof com.cashier.controller.PosModeController) {
                ((com.cashier.controller.PosModeController) currentController).cleanup();
            }
        } catch (Exception e) {
            logger.error("清理控制器资源时发生错误", e);
        }

        // 停止库存预警服务
        try {
            com.cashier.service.InventoryAlertService.getInstance().stop();
            logger.info("库存预警服务已停止");
        } catch (Exception e) {
            logger.error("停止库存预警服务时发生错误", e);
        }

        // 停止自动备份服务
        try {
            com.cashier.service.BackupService.getInstance().stop();
            logger.info("自动备份服务已停止");
        } catch (Exception e) {
            logger.error("停止自动备份服务时发生错误", e);
        }

        // 停止 REST API 服务器
        try {
            com.cashier.api.ApiServer.getInstance().stop();
            logger.info("REST API 服务器已停止");
        } catch (Exception e) {
            logger.error("停止 REST API 服务器时发生错误", e);
        }

        // 关闭通知管理器
        try {
            com.cashier.notification.NotificationManager.getInstance().shutdown();
            logger.info("通知管理器已关闭");
        } catch (Exception e) {
            logger.error("关闭通知管理器时发生错误", e);
        }

        // 关闭 UI 优化器
        try {
            com.cashier.util.UIOptimizer.shutdown();
            logger.info("UI 优化器已关闭");
        } catch (Exception e) {
            logger.error("关闭 UI 优化器时发生错误", e);
        }

        this.currentUser = null;

        try {
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/LoginView.fxml");
            Parent root = loader.load();

            // 获取控制器并设置应用程序引用
            LoginController controller = loader.getController();
            controller.setApplication(this);

            // 创建场景
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

            // 应用默认主题
            applyTheme(scene, FXConstants.DEFAULT_THEME);

            // 设置场景
            primaryStage.setScene(scene);

            // 更新窗口标题
            primaryStage.setTitle(APP_TITLE);

        } catch (IOException e) {
            logger.error("加载登录界面失败", e);
        }
    }

    /**
     * 获取主窗口
     * @return 主窗口
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * 检查数据库配置
     * 如果配置不存在，显示配置向导
     */
    private void checkDatabaseConfiguration() {
        java.nio.file.Path configPath = java.nio.file.Paths.get("config", "database.properties");

        if (!java.nio.file.Files.exists(configPath)) {
            logger.info("数据库配置不存在，启动配置向导");

            // 使用 Swing 显示配置向导
            try {
                javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                logger.warn("无法设置系统外观", e);
            }

            try {
                javax.swing.SwingUtilities.invokeAndWait(() -> {
                    com.cashier.installer.DatabaseConfigDialog.main(new String[]{});
                });
            } catch (Exception e) {
                logger.error("配置向导执行失败", e);
            }

            // 配置完成后检查文件是否创建
            if (!java.nio.file.Files.exists(configPath)) {
                logger.warn("用户取消配置，退出应用");
                javafx.application.Platform.exit();
                System.exit(0);
            }

            logger.info("数据库配置完成");
        }
    }

    /**
     * 应用程序入口
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // Windows DPI 缩放支持
        // 确保应用程序在高 DPI 显示器上正确缩放
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            // 设置系统 DPI 感知
            System.setProperty("sun.java2d.dpiaware", "true");
            System.setProperty("sun.java2d.dpiaware", "true");
            System.setProperty("sun.java2d.win.uiScaleX", "1.0");
            System.setProperty("sun.java2d.win.uiScaleY", "1.0");
        }

        launch(args);
    }
}