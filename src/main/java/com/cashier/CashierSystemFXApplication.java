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
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import org.slf4j.Logger;

/**
 * 收银系统 JavaFX 主应用类
 * 负责初始化 JavaFX 应用和加载主界面
 */
public class CashierSystemFXApplication extends Application {

    private static final Logger logger = LoggerFactoryUtil.getLogger(CashierSystemFXApplication.class);
    private static final String APP_TITLE = "收银系统";
    private static final double WINDOW_WIDTH = 1300;
    private static final double WINDOW_HEIGHT = 800;

    private static CashierSystemFXApplication instance;

    private Stage primaryStage;
    private User currentUser;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        instance = this;

        // 初始化数据管理器
        DataService.initialize();

        // 初始化缓存管理器
        com.cashier.util.CacheManager.initialize();

        // 预热缓存
        com.cashier.util.CacheManager.warmupCache();

        // 设置应用图标
        setupApplicationIcon();

        // 加载登录界面
        loadLoginScene();

        // 配置主窗口
        configurePrimaryStage();

        // 显示窗口
        primaryStage.show();
    }

    /**
     * 获取应用程序实例
     * @return 应用程序实例
     */
    public static CashierSystemFXApplication getInstance() {
        return instance;
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
            System.err.println("无法加载应用图标: " + e.getMessage());
        }
    }

    /**
     * 加载登录界面
     */
    private void loadLoginScene() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/LoginView.fxml"));
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
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/MainView.fxml"));
            Parent root = loader.load();

            // 获取控制器并设置应用程序引用
            MainController controller = loader.getController();
            controller.setApplication(this);

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
            
            logger.info("系统服务已关闭");
        } catch (Exception e) {
            logger.error("关闭系统服务时发生错误", e);
        }
    }    /**
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
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/PosModeView.fxml"));
            Parent root = loader.load();

            // 获取控制器并设置应用程序引用
            PosModeController controller = loader.getController();
            controller.setApplication(this);
            controller.setCurrentUser(user);

            // 创建场景
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

            // 应用主题
            String currentTheme = DataService.loadThemePreference();
            applyTheme(scene, currentTheme);

            // 设置场景
            primaryStage.setScene(scene);

            // 更新窗口标题
            primaryStage.setTitle(APP_TITLE + " - 收银台 - " + user.name);

            logger.info("用户 {} ({}) 进入POS模式", user.name, user.getRoleDisplayName());

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
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/MainView.fxml"));
            Parent root = loader.load();
    
            // 获取控制器并设置应用程序引用
            MainController controller = loader.getController();
            controller.setApplication(this);
            controller.setCurrentUser(user);
    
            // 创建场景
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
    
            // 应用主题
            String currentTheme = DataService.loadThemePreference();
            applyTheme(scene, currentTheme);
    
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
    
        } catch (IOException e) {            logger.error("加载主界面失败", e);
        }
    }

    /**
     * 返回登录界面（退出登录）
     */
    public void logoutToLoginView() {
        this.currentUser = null;

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/LoginView.fxml"));
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