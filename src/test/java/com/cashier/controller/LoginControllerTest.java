package com.cashier.controller;

import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoginController 单元测试
 */
// @ExtendWith(ApplicationExtension.class)
// @DisplayName("登录控制器测试")
public class LoginControllerTest extends DatabaseTestBase {

    private TextField usernameField;
    private PasswordField passwordField;
    private Button loginButton;

    // @Start
    // public void start(Stage stage) throws Exception {
    //     // 初始化测试数据库
    //     initTestDatabase();

    //     // 加载登录界面
    //     FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cashier/view/LoginView.fxml"));
    //     AnchorPane root = loader.load();

    //     // 获取控制器
    //     LoginController controller = loader.getController();

    //     // 设置舞台
    //     Scene scene = new Scene(root, 1300, 800);
    //     stage.setScene(scene);
    //     stage.setTitle("收银系统 - 登录");
    //     stage.show();

    //     // 获取UI组件
    //     usernameField = (TextField) scene.lookup("#usernameField");
    //     passwordField = (PasswordField) scene.lookup("#passwordField");
    //     loginButton = (Button) scene.lookup("#loginButton");
    // }

    // @BeforeEach
    // public void beforeEach() {
    //     // 清空输入框
    //     usernameField.clear();
    //     passwordField.clear();
    // }

    // @Test
    // @DisplayName("测试登录成功")
    // public void testLoginSuccess(FxRobot robot) {
    //     // 输入正确的用户名和密码
    //     robot.clickOn(usernameField).write("admin");
    //     robot.clickOn(passwordField).write("admin123");
    //     robot.clickOn(loginButton);

    //     // 等待登录完成
    //     try {
    //         Thread.sleep(1000);
    //     } catch (InterruptedException e) {
    //         e.printStackTrace();
    //     }

    //     // 登录成功后应该关闭登录窗口并打开主窗口
    //     // 这里我们只测试登录过程，不测试主窗口的打开
    //     // 因为主窗口的打开需要更多的测试设置
    // }

    // @Test
    // @DisplayName("测试登录失败 - 空用户名")
    // public void testLoginFailureEmptyUsername(FxRobot robot) {
    //     // 只输入密码，不输入用户名
    //     robot.clickOn(passwordField).write("admin123");
    //     robot.clickOn(loginButton);

    //     // 等待错误提示
    //     try {
    //         Thread.sleep(500);
    //     } catch (InterruptedException e) {
    //         e.printStackTrace();
    //     }

    //     // 应该显示错误提示
    //     // 这里可以使用 TestFX 的方法来验证错误提示
    // }

    // @Test
    // @DisplayName("测试登录失败 - 空密码")
    // public void testLoginFailureEmptyPassword(FxRobot robot) {
    //     // 只输入用户名，不输入密码
    //     robot.clickOn(usernameField).write("admin");
    //     robot.clickOn(loginButton);

    //     // 等待错误提示
    //     try {
    //         Thread.sleep(500);
    //     } catch (InterruptedException e) {
    //         e.printStackTrace();
    //     }

    //     // 应该显示错误提示
    // }

    // @Test
    // @DisplayName("测试登录失败 - 错误密码")
    // public void testLoginFailureWrongPassword(FxRobot robot) {
    //     // 输入正确的用户名和错误的密码
    //     robot.clickOn(usernameField).write("admin");
    //     robot.clickOn(passwordField).write("wrongpassword");
    //     robot.clickOn(loginButton);

    //     // 等待错误提示
    //     try {
    //         Thread.sleep(500);
    //     } catch (InterruptedException e) {
    //         e.printStackTrace();
    //     }

    //     // 应该显示错误提示
    // }

    // @Test
    // @DisplayName("测试登录失败 - 不存在的用户")
    // public void testLoginFailureNonExistentUser(FxRobot robot) {
    //     // 输入不存在的用户名
    //     robot.clickOn(usernameField).write("nonexistent");
    //     robot.clickOn(passwordField).write("admin123");
    //     robot.clickOn(loginButton);

    //     // 等待错误提示
    //     try {
    //         Thread.sleep(500);
    //     } catch (InterruptedException e) {
    //         e.printStackTrace();
    //     }

    //     // 应该显示错误提示
    // }
}
