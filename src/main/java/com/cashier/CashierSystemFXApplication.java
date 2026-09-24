package com.cashier;

import com.cashier.constant.DatabaseConfigKeys;
import com.cashier.constant.SystemPropertyKeys;

import com.cashier.controller.LoginController;
import com.cashier.controller.MainController;
import com.cashier.constant.FXConstants;
import com.cashier.i18n.I18nKeys;
import com.cashier.i18n.I18nManager;
import com.cashier.service.DataService;
import com.cashier.model.User;
import com.cashier.util.FXMLUtils;
import com.cashier.util.FXUtils;
import com.cashier.util.LoggerFactoryUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
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
    /** 启动窗口显示后，延后这么多毫秒再跑重量级初始化，确保窗口已经绘制出来 */
    private static final double SPLASH_FIRST_FRAME_DELAY_MS = 60;

    /** 数据库阶段所在后台线程名（门禁据此断言该阶段不在 FX 线程上跑） */
    private static final String STARTUP_DATABASE_THREAD = "startup-database";
    /** 数据库阶段最多等多久：冷启动首次连接实测可达 8 秒以上，这里给足余量；超时明确报错而不是无限卡住 */
    private static final long STARTUP_DATABASE_TIMEOUT_MS = 60_000;
    /** 等待数据库期间刷新"已等待 N 秒"的间隔 */
    private static final double STARTUP_STATUS_REFRESH_SECONDS = 1;

    /**
     * 界面字体族名：必须与 {@code css/*.css} 里 {@code -fx-font-family} 的**首项**一致，
     * 门禁见 {@code ThemeStylePolicyTest.uiFontStacksStartWithTheBundledFamily}。
     *
     * <p>JavaFX 只使用候选列表的第一个族名（实测 17.0.12 不会遍历回退列表），而 Windows/macOS
     * 默认都不安装这个族名，随包内置的 .ttc 是唯一来源——所以它对不上时界面不会报错，只会静默
     * 换成平台默认字体。</p>
     */
    static final String UI_FONT_FAMILY = "Noto Sans CJK SC";

    /** 内置界面字体是否注册成功；失败时界面会回退到平台默认字体 */
    private boolean uiFontReady = true;

    private static CashierSystemFXApplication instance;

    private Stage primaryStage;
    private User currentUser;
    private Object currentController; // 当前活动的控制器，用于清理资源

    // 单实例控制
    private static final String APP_LOCK_FILE = System.getProperty("java.io.tmpdir") + java.io.File.separator + "lisuan.lock";
    private static java.nio.channels.FileLock fileLock;
    private static java.nio.channels.FileChannel channel;

    @Override
    public void init() throws Exception {
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
            });
            throw new Exception("Application already running");
        }

        logger.info("应用初始化完成，已获取单实例锁");
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        instance = this;

        // 尽早注册全局弹窗主题化钩子；主场景就绪后，所有新弹窗自动继承主题
        com.cashier.util.ThemeUtils.installGlobalDialogTheming();

        // 先显示轻量启动窗口，再执行重量级初始化（连库、建表、加载 FXML）。
        // 必须延后一帧再跑初始化：同步初始化会占满 FX 线程，窗口根本来不及绘制。
        final SplashWindow splash = new SplashWindow();
        splash.show();

        PauseTransition defer = new PauseTransition(Duration.millis(SPLASH_FIRST_FRAME_DELAY_MS));
        defer.setOnFinished(event -> {
            try {
                initializeApplication(splash);
            } catch (Throwable t) {
                // 数据库静态初始化失败抛的是 ExceptionInInitializerError（Error 而非 Exception），
                // 只 catch Exception 会让启动失败完全静默（javaw/双击启动时用户什么都看不到）
                logger.error("应用初始化失败", t);
                splash.close();
                // 动画/布局处理期间不允许 showAndWait，必须回到事件循环后再弹窗
                Platform.runLater(() -> showStartupFailure(t));
            }
            // 正常路径由 completeStartup 负责关掉启动窗口：initializeApplication 现在是异步的
        });
        defer.play();
    }

    /** 数据库阶段的结果；{@code null} 结果表示用户在配置向导里取消了配置 */
    private record StartupDatabase(String languageTag) {
    }

    /**
     * 启动第一阶段：把数据库相关的重活交给后台线程，FX 线程只负责刷新启动画面。
     *
     * <p>此前配置向导、连接池建连、建表迁移、语言偏好读取都在 FX 线程上同步执行，
     * 后果有两个：① 启动画面被占死、进度文案根本刷不出来；② 冷启动首连要好几秒
     * （本机实测 8.5s），连接一旦被挂住就会无限期停在启动画面上、既无提示也无超时。</p>
     */
    private void initializeApplication(SplashWindow splash) {
        splash.updateProgress(0.2, "正在连接数据库...");
        long startedAt = System.currentTimeMillis();
        logger.info("启动阶段: 正在连接数据库（等待上限 {} 秒）", STARTUP_DATABASE_TIMEOUT_MS / 1000);

        CompletableFuture<StartupDatabase> databasePhase = new CompletableFuture<>();
        Thread worker = new Thread(() -> {
            try {
                databasePhase.complete(initializeDatabasePhase());
            } catch (Throwable t) {
                databasePhase.completeExceptionally(t);
            }
        }, STARTUP_DATABASE_THREAD);
        // 守护线程：即使它仍卡在 socket 上，也不会阻止进程退出
        worker.setDaemon(true);
        worker.start();

        // 完成即回到 FX 线程继续；不轮询、不阻塞，启动画面才能重绘
        databasePhase.whenComplete((result, error) ->
            Platform.runLater(() -> completeStartup(splash, result, error, startedAt)));

        // 看门狗：只负责"等太久"的提示与超时判定
        long[] lastLoggedSecond = {0};
        Timeline watchdog = new Timeline(new KeyFrame(
            Duration.seconds(STARTUP_STATUS_REFRESH_SECONDS), event -> {
                if (databasePhase.isDone()) {
                    return;
                }
                long waited = System.currentTimeMillis() - startedAt;
                if (waited >= STARTUP_DATABASE_TIMEOUT_MS) {
                    logger.error("启动阶段: 等待数据库超过 {} 秒，放弃启动", STARTUP_DATABASE_TIMEOUT_MS / 1000);
                    databasePhase.completeExceptionally(new IOException(
                        "连接数据库超过 " + (STARTUP_DATABASE_TIMEOUT_MS / 1000) + " 秒仍未完成（已等待 "
                            + (waited / 1000) + " 秒）。请检查 MySQL 是否已启动、"
                            + "config/database.properties 的主机/端口是否正确；"
                            + "若数据库响应较慢，可调大 db.connection.timeout"));
                    return;
                }
                long seconds = waited / 1000;
                // 每 5 秒在日志里留一行：出问题时只看日志就知道启动卡在哪一步（此前是彻底静默）
                if (seconds >= 5 && seconds % 5 == 0 && seconds != lastLoggedSecond[0]) {
                    lastLoggedSecond[0] = seconds;
                    logger.info("启动阶段: 仍在等待数据库（已 {} 秒）", seconds);
                }
                splash.updateProgress(0.2, "正在连接数据库...（已等待 " + seconds + " 秒）");
            }));
        watchdog.setCycleCount(Animation.INDEFINITE);
        watchdog.play();
        databasePhase.whenComplete((result, error) -> Platform.runLater(watchdog::stop));
    }

    /**
     * 数据库阶段（在 {@value #STARTUP_DATABASE_THREAD} 线程执行）。
     *
     * <p>首次触碰 {@code DatabaseManager} 会在这里建连接池、建表并跑迁移，是最耗时的一段。</p>
     *
     * @return 结果；返回 {@code null} 表示用户在配置向导里取消了配置
     */
    private StartupDatabase initializeDatabasePhase() throws Exception {
        // 检查数据库配置：缺失时弹配置向导，用户取消则不再继续
        // （向导自身按独立工具的语义退出进程，这里只需等它结束）
        if (!checkDatabaseConfiguration()) {
            return null;
        }

        // 支付渠道必须在收银界面加载前完成配置，未配置渠道保持禁用。
        com.cashier.service.PaymentService.init();

        return new StartupDatabase(DataService.loadLanguagePreference());
    }

    /**
     * 数据库阶段结束后的收口：区分"失败 / 用户取消 / 成功"，再回到 FX 线程做界面部分。
     */
    private void completeStartup(SplashWindow splash, StartupDatabase database, Throwable error, long startedAt) {
        if (error != null) {
            logger.error("应用初始化失败", error);
            splash.close();
            showStartupFailure(error);
            return;
        }
        if (database == null) {
            logger.warn("数据库配置未完成，应用退出");
            splash.close();
            Platform.exit();
            return;
        }

        logger.info("启动阶段: 数据库就绪，耗时 {}ms", System.currentTimeMillis() - startedAt);
        try {
            finishStartup(splash, database.languageTag());
        } catch (Throwable t) {
            logger.error("应用初始化失败", t);
            splash.close();
            showStartupFailure(t);
        }
    }

    /**
     * 启动第二阶段（FX 线程）：应用图标、语言、登录界面、主窗口与后台服务。
     */
    private void finishStartup(SplashWindow splash, String savedLanguage) {
        splash.updateProgress(0.5, "正在加载界面...");

        // 立即设置应用图标
        setupApplicationIcon();

        I18nManager.getInstance().setLocale(savedLanguage);
        logger.info("应用启动 - 已加载语言偏好: {}, I18nManager 当前语言: {}",
            savedLanguage, I18nManager.getInstance().getCurrentLanguageTag());

        // 加载登录界面
        loadLoginScene();

        // 配置主窗口
        configurePrimaryStage();

        splash.updateProgress(0.85, "正在启动服务...");

        // 立即显示窗口 - 不等待后台初始化
        primaryStage.show();

        if (!uiFontReady) {
            // 动画/布局处理期间不允许 showAndWait，回到事件循环后再弹
            Platform.runLater(this::warnUiFontMissing);
        }

        splash.updateProgress(1.0, "即将完成...");
        splash.close();

        // 异步初始化后台服务 - 启动后立即执行
        startBackgroundServices();
    }

    /**
     * 初始化失败时给出可见反馈并退出，避免只留一个空启动窗口。
     */
    private void showStartupFailure(Throwable t) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("启动失败");
        alert.setHeaderText(null);
        alert.setContentText("应用初始化失败：" + t.getMessage());
        alert.showAndWait();
        Platform.exit();
    }

    private void startBackgroundServices() {
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
        shutdown();

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
     * 获取主窗口当前场景（用于给对话框复制主题样式）。
     */
    public Scene getPrimaryScene() {
        return primaryStage != null ? primaryStage.getScene() : null;
    }

    /**
     * 获取当前登录用户
     * @return 当前登录用户，未登录时返回 null
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * 加载自定义中文字体
     * 确保在所有平台上中文都能正确显示
     */
    private void loadCustomFonts() {
        boolean regular;
        boolean bold;
        try {
            regular = loadFontCollection("/fonts/NotoSansSC-Regular.ttc", "Regular");
            bold = loadFontCollection("/fonts/NotoSansSC-Bold.ttc", "Bold");
        } catch (Exception e) {
            logger.error("加载自定义字体时发生错误: {}", e.getMessage(), e);
            regular = false;
            bold = false;
        }
        uiFontReady = regular && bold;

        if (uiFontReady) {
            logger.debug("自定义字体加载完成");
            return;
        }

        logger.error("内置界面字体 {} 未注册成功：JavaFX 只使用 -fx-font-family 的第一个族名，"
            + "界面将静默回退到平台默认字体（Windows 上是 Microsoft YaHei UI），"
            + "在缺少中文字体的系统上会显示成方框。"
            + "请确认安装包完整（src/main/resources/fonts/*.ttc 未被裁剪）", UI_FONT_FAMILY);
    }

    private boolean loadFontCollection(String resourcePath, String styleName) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                logger.error("未找到界面字体资源 {}（{}），安装包可能被裁剪", resourcePath, styleName);
                return false;
            }

            Font[] loadedFonts = Font.loadFonts(inputStream, 14);
            boolean loaded = loadedFonts != null && java.util.Arrays.stream(loadedFonts)
                .anyMatch(font -> UI_FONT_FAMILY.equals(font.getFamily()));
            if (loaded) {
                logger.info("成功加载 {} {} 字体", UI_FONT_FAMILY, styleName);
            } else {
                logger.error("{} 未提供族名 {}（实际族名: {}）", resourcePath, UI_FONT_FAMILY,
                    loadedFonts == null ? "null" : java.util.Arrays.stream(loadedFonts)
                        .map(Font::getFamily).distinct().collect(java.util.stream.Collectors.joining(", ")));
            }
            return loaded;
        }
    }

    /**
     * 内置界面字体缺失时给出可见提示：字体是"静默降级"，只在日志里报错等于没报。
     */
    private void warnUiFontMissing() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("界面字体缺失");
        alert.setHeaderText(null);
        alert.setContentText("未能加载随包内置的界面字体 \"" + UI_FONT_FAMILY + "\"。\n\n"
            + "界面已回退到系统默认字体，中文或符号可能显示异常。\n"
            + "请确认安装包完整（fonts/*.ttc 未被裁剪）。");
        alert.showAndWait();
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
            String username = currentUser != null ? currentUser.username : "default";
            String currentTheme = DataService.loadThemePreference(username);
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

        // 添加主题样式表，兼容旧版 IntelliJ 主题偏好
        String normalizedThemeName = "intellij".equals(themeName) ? "lisuan" : themeName;
        String themeCss = "/css/" + normalizedThemeName + "-theme.css";
        URL themeStylesheet = getClass().getResource(themeCss);
        if (themeStylesheet != null) {
            scene.getStylesheets().add(themeStylesheet.toExternalForm());
        }

        // 保存主题设置（仅在用户登录后）
        if (currentUser != null) {
            DataService.saveThemePreference(currentUser.username, normalizedThemeName);
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
        requestExit();
    }

    /**
     * 请求退出应用，统一执行退出确认和 JavaFX 生命周期清理。
     */
    public void requestExit() {
        boolean hasActiveShift = DataService.hasActiveShift();
        User user = getCurrentUser();

        if (!hasActiveShift) {
            // 没有活跃班次，直接确认退出
            if (FXUtils.showConfirmAlert(
                I18nManager.getInstance().get("app.exit_confirm_title"),
                I18nManager.getInstance().get("app.exit_confirm_message"))) {
                if (user == null) {
                    // 登录界面：确认后真正退出应用
                    exitApplication();
                } else {
                    // 已登录：与触屏版一致，退出到登录界面而非关闭应用
                    logoutToLoginView();
                }
            }
            return;
        }

        // 有活跃班次：按是否已登录区分
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18nManager.getInstance().get("app.exit_confirm_title"));
        alert.setHeaderText(null);

        if (user == null) {
            // 未登录（登录界面）：无法在此交班，不提供"先交班"，仅"直接退出/取消"
            alert.setContentText(I18nManager.getInstance().get("app.exit_active_shift_message"));
            ButtonType noButton = new ButtonType(
                I18nManager.getInstance().get("app.exit_direct"), ButtonBar.ButtonData.NO);
            ButtonType cancelButton = new ButtonType(
                I18nManager.getInstance().get(I18nKeys.Common.CANCEL), ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(noButton, cancelButton);
            alert.showAndWait().ifPresent(bt -> {
                if (bt == noButton) {
                    // 登录界面无法交班，直接退出应用
                    exitApplication();
                }
            });
        } else {
            // 已登录：三选，"先交班"取消退出并引导前往交接班入口（不再空操作）
            alert.setContentText(I18nManager.getInstance().get("app.exit_active_shift_confirm"));
            ButtonType yesButton = new ButtonType(
                I18nManager.getInstance().get("app.exit_shift_first"), ButtonBar.ButtonData.YES);
            ButtonType noButton = new ButtonType(
                I18nManager.getInstance().get("app.exit_direct"), ButtonBar.ButtonData.NO);
            ButtonType cancelButton = new ButtonType(
                I18nManager.getInstance().get(I18nKeys.Common.CANCEL), ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(yesButton, noButton, cancelButton);
            alert.showAndWait().ifPresent(buttonType -> {
                if (buttonType == yesButton) {
                    Alert tip = new Alert(Alert.AlertType.INFORMATION);
                    tip.setTitle(I18nManager.getInstance().get("app.go_shift_title"));
                    tip.setHeaderText(null);
                    tip.setContentText(I18nManager.getInstance().get("app.go_shift_message"));
                    tip.showAndWait();
                    logger.info("用户选择先交班，已引导前往交接班入口");
                } else if (buttonType == noButton) {
                    logoutToLoginView();
                }
            });
        }
    }

    public void exitApplication() {
        javafx.application.Platform.exit();
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
     * 加载POS模式界面（专为收银员设计的触屏版）
     * @param user 当前登录用户
     */
    public void switchToPosModeView(User user) {
        try {
            // 加载用户特定的语言偏好并应用到 I18nManager
            // 必须在加载 FXML 之前设置，确保使用正确的 ResourceBundle
            String userLanguage = DataService.loadLanguagePreference(user.username);
            com.cashier.i18n.I18nManager.getInstance().setLocale(userLanguage);
            logger.info("用户 {} 的语言偏好: {}, I18nManager 当前语言: {}",
                       user.username, userLanguage, com.cashier.i18n.I18nManager.getInstance().getCurrentLanguageTag());

            // 收银员使用新的触屏版收银界面
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/TouchCartView.fxml");
            Parent root = loader.load();

            // 获取控制器并设置应用程序引用
            com.cashier.controller.TouchCartController controller = loader.getController();
            controller.setApplication(this);
            controller.setCurrentUser(user);
            currentController = controller; // 保存控制器引用

            // 创建场景
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

            // 应用主题
            String currentTheme = DataService.loadThemePreference(user.username);
            applyTheme(scene, currentTheme);

            // 应用字号
            String currentFontSize = DataService.loadFontSizePreference(user.username);
            applyFontSize(scene, currentFontSize);

            // 设置场景
            primaryStage.setScene(scene);

            // 更新窗口标题
            primaryStage.setTitle(APP_TITLE + " - 触屏收银台 - " + user.name);

            logger.info("用户 {} ({}) 进入触屏收银台", user.name, user.getRoleDisplayName());

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
            logger.error("加载触屏收银台界面失败", e);
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
            String currentTheme = DataService.loadThemePreference(user.username);
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
                if (com.cashier.api.ApiConfig.isEnabled()) {
                    com.cashier.api.ApiServer apiServer = com.cashier.api.ApiServer.getInstance();
                    int apiPort = com.cashier.api.ApiConfig.getPort();
                    apiServer.start(apiPort);
                    logger.info("REST API 服务器已启动，端口: {}", apiPort);
                } else {
                    logger.info("REST API 服务器已禁用");
                }
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
            } else if (currentController instanceof com.cashier.controller.TouchCartController) {
                ((com.cashier.controller.TouchCartController) currentController).cleanup();
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
     * 检查数据库配置：文件缺失时弹出配置向导并等待其关闭。
     *
     * @return true 表示配置就绪、可以继续启动；false 表示用户取消了配置
     */
    private boolean checkDatabaseConfiguration() {
        java.nio.file.Path configPath = java.nio.file.Paths.get("config", DatabaseConfigKeys.DATABASE_PROPERTIES_FILE);
        if (java.nio.file.Files.exists(configPath)) {
            return true;
        }

        logger.info("数据库配置不存在，启动配置向导");

        // 使用 Swing 显示配置向导
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.warn("无法设置系统外观", e);
        }

        // 必须等向导真正关闭：向导是异步显示的，提前返回会把"还没配"误判成"用户取消配置"，
        // 接着继续初始化会抛 ExceptionInInitializerError，并顺手写下一份空密码的配置文件模板
        boolean configured = com.cashier.installer.DatabaseConfigDialog.showAndWait();
        if (!configured) {
            logger.warn("配置向导已关闭，但 config/{} 仍不存在", DatabaseConfigKeys.DATABASE_PROPERTIES_FILE);
            return false;
        }
        logger.info("数据库配置完成");
        return true;
    }

    /**
     * 应用程序入口
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // Windows DPI 缩放支持：声明进程 DPI 感知，缩放交给系统处理。
        // 注意不要设 sun.java2d.win.uiScaleX/Y=1.0——那是把缩放强行按回 100%，
        // 高 DPI 屏上 Swing 弹窗（如数据库配置向导）会明显偏小。
        if (System.getProperty(SystemPropertyKeys.OS_NAME, "").toLowerCase().contains("win")) {
            System.setProperty("sun.java2d.dpiaware", "true");
        }

        launch(args);
    }
}
