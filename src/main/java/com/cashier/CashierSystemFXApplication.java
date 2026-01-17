package com.cashier;

import com.cashier.controller.LoginController;
import com.cashier.controller.MainController;
import com.cashier.constant.FXConstants;
import com.cashier.constant.SpacingConstants;
import com.cashier.model.DataManager;
import com.cashier.model.User;
import com.cashier.util.FXMLUtils;
import com.cashier.util.FXUtils;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * 收银系统 JavaFX 主应用类
 * 负责初始化 JavaFX 应用和加载主界面
 */
public class CashierSystemFXApplication extends Application {

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
        DataManager.initialize();

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
            System.err.println("加载登录界面失败: " + e.getMessage());
            e.printStackTrace();
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
            String currentTheme = DataManager.loadThemePreference();
            applyTheme(scene, currentTheme);

            // 设置场景
            primaryStage.setScene(scene);

        } catch (IOException e) {
            System.err.println("加载主界面失败: " + e.getMessage());
            e.printStackTrace();
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

        // 保存主题设置
        DataManager.saveThemePreference(themeName);
    }

    /**
     * 处理退出
     */
    private void handleExit() {
        // TODO: 检查是否有未保存的数据
        // TODO: 检查是否有进行中的班次

        // 保存数据
        // DataManager.saveAll();

        // 退出应用
        System.exit(0);
    }

    /**
     * 切换到主界面（登录成功后）
     * @param user 当前登录用户
     */
    public void switchToMainView(User user) {
        this.currentUser = user;

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
            String currentTheme = DataManager.loadThemePreference();
            applyTheme(scene, currentTheme);

            // 设置场景
            primaryStage.setScene(scene);

            // 更新窗口标题
            primaryStage.setTitle(APP_TITLE + " - " + user.name + " (" + user.getRoleDisplayName() + ")");

        } catch (IOException e) {
            System.err.println("加载主界面失败: " + e.getMessage());
            e.printStackTrace();
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
            System.err.println("加载登录界面失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取当前登录用户
     * @return 当前用户
     */
    public User getCurrentUser() {
        return currentUser;
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
        launch(args);
    }
}