package com.cashier.controller;

import com.cashier.CashierSystemFXApplication;
import com.cashier.model.User;
import com.cashier.util.FXMLUtils;
import com.cashier.util.StatusBarManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * POS模式控制器
 * 专门为收银员设计的简化界面，只包含收银台和交接班功能
 */
public class PosModeController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(PosModeController.class);

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userRoleLabel;

    @FXML
    private Circle avatarCircle;

    @FXML
    private Label avatarText;

    @FXML
    private Button exitButton;

    @FXML
    private Button shiftButton;

    @FXML
    private VBox cartContainer;

    @FXML
    private Label dateLabel;

    @FXML
    private Label timeLabel;

    @FXML
    private Label statusLabel;

    private CashierSystemFXApplication application;
    private User currentUser;
    private CartController cartController;
    private Timeline timeTimeline;

    /**
     * 设置应用程序实例
     */
    public void setApplication(CashierSystemFXApplication application) {
        this.application = application;
    }

    /**
     * 设置当前用户
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;

        // 加载用户特定的语言偏好
        String userLanguage = com.cashier.service.DataService.loadLanguagePreference(user.username);
        com.cashier.i18n.I18nManager.getInstance().setLocale(userLanguage);

        // 更新用户信息显示
        userNameLabel.setText(user.name);
        userRoleLabel.setText(user.getRoleDisplayName());

        // 设置头像（显示用户名的首字母）
        if (user.name != null && !user.name.isEmpty()) {
            // 获取首字母
            String firstLetter = getFirstLetter(user.name);
            avatarText.setText(firstLetter);

            // 根据角色设置头像颜色
            String avatarColor;
            if ("admin".equals(user.role)) {
                avatarColor = "#FFC107"; // 管理员 - 黄色
            } else if ("finance".equals(user.role)) {
                avatarColor = "#9C27B0"; // 财务 - 紫色
            } else {
                avatarColor = "#FFC107"; // 收银员 - 黄色
            }
            avatarCircle.setStyle("-fx-fill: " + avatarColor + "; -fx-stroke: #FFFFFF; -fx-stroke-width: 2;");
        }

        logger.info("POS模式登录用户: {} ({})", user.name, user.getRoleDisplayName());
    }

    /**
     * 获取姓名的首字母
     * 对于英文：取第一个字母
     * 对于中文：取第一个字的拼音首字母
     */
    private String getFirstLetter(String name) {
        if (name == null || name.isEmpty()) {
            return "U";
        }

        char firstChar = name.charAt(0);
        
        // 如果是英文字母，直接返回
        if ((firstChar >= 'A' && firstChar <= 'Z') || (firstChar >= 'a' && firstChar <= 'z')) {
            return String.valueOf(firstChar).toUpperCase();
        }
        
        // 如果是中文，返回常用汉字的首字母映射
        // 这是一个简化版，实际应该使用完整的拼音库
        String pinyinFirstLetter = getPinyinFirstLetter(firstChar);
        return pinyinFirstLetter;
    }

    /**
     * 获取中文字符的拼音首字母
     * 这是一个简化的映射表，覆盖常见姓氏
     */
    private String getPinyinFirstLetter(char c) {
        // 常见中文姓氏拼音首字母映射
        if (c >= '赵' && c <= '座') {
            // 简化的范围判断，实际应该使用精确的拼音库
            if (c >= '赵' && c <= '邹') return "Z";
            if (c >= '张' && c <= '赵') return "Z";
            if (c >= '李' && c <= '路') return "L";
            if (c >= '王' && c <= '魏') return "W";
            if (c >= '刘' && c <= '罗') return "L";
            if (c >= '陈' && c <= '程') return "C";
            if (c >= '杨' && c <= '姚') return "Y";
            if (c >= '黄' && c <= '郝') return "H";
            if (c >= '赵' && c <= '邹') return "Z";
            if (c >= '周' && c <= '郑') return "Z";
            if (c >= '吴' && c <= '伍') return "W";
            if (c >= '徐' && c <= '许') return "X";
            if (c >= '孙' && c <= '苏') return "S";
            if (c >= '马' && c <= '麦') return "M";
            if (c >= '朱' && c <= '祝') return "Z";
            if (c >= '胡' && c <= '侯') return "H";
            if (c >= '郭' && c <= '顾') return "G";
            if (c >= '何' && c <= '贺') return "H";
            if (c >= '高' && c <= '葛') return "G";
            if (c >= '林' && c <= '刘') return "L";
            if (c >= '罗' && c <= '陆') return "L";
            if (c >= '梁' && c <= '廖') return "L";
            if (c >= '宋' && c <= '苏') return "S";
            if (c >= '郑' && c <= '郑') return "Z";
            if (c >= '谢' && c <= '薛') return "X";
            if (c >= '韩' && c <= '郝') return "H";
            if (c >= '唐' && c <= '汤') return "T";
            if (c >= '冯' && c <= '傅') return "F";
            if (c >= '于' && c <= '余') return "Y";
            if (c >= '董' && c <= '杜') return "D";
            if (c >= '萧' && c <= '萧') return "X";
            if (c >= '程' && c <= '崔') return "C";
            if (c >= '袁' && c <= '袁') return "Y";
            if (c >= '邓' && c <= '邓') return "D";
            if (c >= '许' && c <= '许') return "X";
            if (c >= '傅' && c <= '傅') return "F";
            if (c >= '沈' && c <= '沈') return "S";
            if (c >= '曾' && c <= '曾') return "Z";
            if (c >= '彭' && c <= '彭') return "P";
            if (c >= '吕' && c <= '吕') return "L";
            if (c >= '苏' && c <= '苏') return "S";
            if (c >= '卢' && c <= '陆') return "L";
            if (c >= '蒋' && c <= '蒋') return "J";
            if (c >= '蔡' && c <= '蔡') return "C";
            if (c >= '贾' && c <= '贾') return "J";
            if (c >= '丁' && c <= '丁') return "D";
            if (c >= '魏' && c <= '魏') return "W";
            if (c >= '薛' && c <= '薛') return "X";
            if (c >= '叶' && c <= '叶') return "Y";
            if (c >= '阎' && c <= '阎') return "Y";
            if (c >= '余' && c <= '余') return "Y";
            if (c >= '潘' && c <= '潘') return "P";
            if (c >= '杜' && c <= '杜') return "D";
            if (c >= '戴' && c <= '戴') return "D";
            if (c >= '夏' && c <= '夏') return "X";
            if (c >= '钟' && c <= '钟') return "Z";
            if (c >= '汪' && c <= '汪') return "W";
            if (c >= '田' && c <= '田') return "T";
            if (c >= '任' && c <= '任') return "R";
            if (c >= '姜' && c <= '姜') return "J";
            if (c >= '范' && c <= '范') return "F";
            if (c >= '方' && c <= '方') return "F";
            if (c >= '石' && c <= '石') return "S";
            if (c >= '姚' && c <= '姚') return "Y";
            if (c >= '谭' && c <= '谭') return "T";
            if (c >= '廖' && c <= '廖') return "L";
            if (c >= '邹' && c <= '邹') return "Z";
            if (c >= '熊' && c <= '熊') return "X";
            if (c >= '金' && c <= '金') return "J";
            if (c >= '陆' && c <= '陆') return "L";
            if (c >= '郝' && c <= '郝') return "H";
            if (c >= '孔' && c <= '孔') return "K";
            if (c >= '白' && c <= '白') return "B";
            if (c >= '崔' && c <= '崔') return "C";
            if (c >= '康' && c <= '康') return "K";
            if (c >= '毛' && c <= '毛') return "M";
            if (c >= '邱' && c <= '邱') return "Q";
            if (c >= '秦' && c <= '秦') return "Q";
            if (c >= '江' && c <= '江') return "J";
            if (c >= '史' && c <= '史') return "S";
            if (c >= '顾' && c <= '顾') return "G";
            if (c >= '侯' && c <= '侯') return "H";
            if (c >= '邵' && c <= '邵') return "S";
            if (c >= '孟' && c <= '孟') return "M";
            if (c >= '龙' && c <= '龙') return "L";
            if (c >= '万' && c <= '万') return "W";
            if (c >= '段' && c <= '段') return "D";
            if (c >= '雷' && c <= '雷') return "L";
            if (c >= '钱' && c <= '钱') return "Q";
            if (c >= '汤' && c <= '汤') return "T";
            if (c >= '尹' && c <= '尹') return "Y";
            if (c >= '黎' && c <= '黎') return "L";
            if (c >= '易' && c <= '易') return "Y";
            if (c >= '常' && c <= '常') return "C";
            if (c >= '武' && c <= '武') return "W";
            if (c >= '乔' && c <= '乔') return "Q";
            if (c >= '贺' && c <= '贺') return "H";
            if (c >= '赖' && c <= '赖') return "L";
            if (c >= '龚' && c <= '龚') return "G";
            if (c >= '文' && c <= '文') return "W";
        }
        
        // 默认返回用户名的第一个字符（对于不在映射表中的汉字）
        return String.valueOf(c);
    }

    /**
     * 初始化
     */
    @FXML
    private void initialize() {
        // 绑定状态栏
        statusLabel.textProperty().bind(StatusBarManager.statusProperty());

        // 更新状态
        StatusBarManager.updateStatus("就绪");
        updateDate();

        // 启动时间更新
        startTimeUpdate();

        // 加载收银台
        loadCartView();

        // 设置快捷键
        setupShortcuts();

        logger.info("POS模式控制器初始化完成");
    }

    /**
     * 加载收银台视图
     */
    private void loadCartView() {
        try {
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/CartView.fxml");
            VBox cartView = loader.load();

            // 获取CartController并设置当前用户
            cartController = loader.getController();
            if (cartController != null && currentUser != null) {
                cartController.setCurrentUser(currentUser);
                logger.debug("已将当前用户传递给CartController");
            }

            // 添加到容器
            cartContainer.getChildren().setAll(cartView);
            VBox.setMargin(cartView, new Insets(0));

            StatusBarManager.updateStatus("收银台已加载");

            // 加载完成后，自动聚焦到搜索框
            Platform.runLater(() -> {
                if (cartController != null) {
                    cartController.focusSearchField();
                }
            });

        } catch (IOException e) {
            logger.error("加载收银台失败", e);
            showError("加载收银台失败: " + e.getMessage());
        }
    }

    /**
     * 设置快捷键
     */
    private void setupShortcuts() {
        // 等待场景加载完成后设置快捷键
        Platform.runLater(() -> {
            if (avatarCircle.getScene() != null) {
                setupSceneShortcuts(avatarCircle.getScene());
            } else {
                // 如果场景还未加载，监听场景属性
                avatarCircle.sceneProperty().addListener((obs, oldScene, newScene) -> {
                    if (newScene != null) {
                        setupSceneShortcuts(newScene);
                    }
                });
            }
        });
    }

    /**
     * 为场景设置快捷键
     */
    private void setupSceneShortcuts(Scene scene) {
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            // F6 - 交接班
            if (event.getCode() == KeyCode.F6) {
                handleShift();
                event.consume();
            }
            // F8 - 结账（传递给CartController处理）
            else if (event.getCode() == KeyCode.F8) {
                // 让事件继续传递到CartController
                event.consume();
            }
            // ESC - 退出确认
            else if (event.getCode() == KeyCode.ESCAPE) {
                handleExit();
                event.consume();
            }
        });
    }

    /**
     * 启动时间更新
     */
    private void startTimeUpdate() {
        timeTimeline = new Timeline(new KeyFrame(
            Duration.seconds(1),
            event -> updateTime()
        ));
        timeTimeline.setCycleCount(Timeline.INDEFINITE);
        timeTimeline.play();
    }

    /**
     * 更新时间
     */
    private void updateTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        timeLabel.setText(timeFormat.format(new Date()));
    }

    /**
     * 更新日期
     */
    private void updateDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateLabel.setText(dateFormat.format(new Date()));
    }

    /**
     * 处理交接班
     */
    @FXML
    private void handleShift() {
        try {
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/ShiftView.fxml");
            VBox root = loader.load();

            ShiftController controller = loader.getController();
            controller.setCurrentUser(currentUser);

            Stage stage = new Stage();
            stage.setTitle("交接班");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            StatusBarManager.updateStatus("交接班操作完成");

        } catch (IOException e) {
            logger.error("加载交接班界面失败", e);
            showError("加载交接班界面失败: " + e.getMessage());
        }
    }

    /**
     * 处理退出登录
     */
    @FXML
    private void handleExit() {
        if (cartController != null) {
            // 检查购物车是否为空
            boolean cartEmpty = cartController.isCartEmpty();
            if (!cartEmpty) {
                // 购物车不为空，提示确认
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("确认退出");
                alert.setHeaderText("购物车不为空");
                alert.setContentText("当前购物车还有商品，确定要退出吗？未结账的商品将会丢失。");
                alert.showAndWait();
                // 无论选择什么都继续退出（因为收银员可能需要重新开始）
            }
        }

        // 停止时间更新
        if (timeTimeline != null) {
            timeTimeline.stop();
        }

        // 返回登录界面
        if (application != null) {
            application.logoutToLoginView();
        }
    }

    /**
     * 显示错误信息
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}