package com.cashier.util;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Optional;

/**
 * JavaFX 工具类
 * 提供常用的 JavaFX 操作方法
 */
public class FXUtils {

    /**
     * 显示信息对话框
     * @param title 标题
     * @param message 消息
     */
    public static void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示警告对话框
     * @param title 标题
     * @param message 消息
     */
    public static void showWarningAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示错误对话框
     * @param title 标题
     * @param message 消息
     */
    public static void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示确认对话框
     * @param title 标题
     * @param message 消息
     * @return 如果用户点击确定返回 true，否则返回 false
     */
    public static boolean showConfirmAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * 显示错误对话框（仅消息）
     * @param message 消息
     */
    public static void showError(String message) {
        showErrorAlert("错误", message);
    }

    /**
     * 显示信息对话框（仅消息）
     * @param message 消息
     */
    public static void showInfo(String message) {
        showInfoAlert("信息", message);
    }

    /**
     * 淡入动画
     * @param node 目标节点
     * @param duration 动画时长（毫秒）
     */
    public static void fadeIn(Node node, int duration) {
        FadeTransition fade = new FadeTransition(Duration.millis(duration), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    /**
     * 淡出动画
     * @param node 目标节点
     * @param duration 动画时长（毫秒）
     */
    public static void fadeOut(Node node, int duration) {
        FadeTransition fade = new FadeTransition(Duration.millis(duration), node);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.play();
    }

    /**
     * 缩放动画
     * @param node 目标节点
     * @param fromX 起始 X 缩放
     * @param toX 结束 X 缩放
     * @param fromY 起始 Y 缩放
     * @param toY 结束 Y 缩放
     * @param duration 动画时长（毫秒）
     */
    public static void scale(Node node, double fromX, double toX, double fromY, double toY, int duration) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(duration), node);
        scale.setFromX(fromX);
        scale.setToX(toX);
        scale.setFromY(fromY);
        scale.setToY(toY);
        scale.play();
    }

    /**
     * 平移动画
     * @param node 目标节点
     * @param fromX 起始 X 位置
     * @param toX 结束 X 位置
     * @param fromY 起始 Y 位置
     * @param toY 结束 Y 位置
     * @param duration 动画时长（毫秒）
     */
    public static void translate(Node node, double fromX, double toX, double fromY, double toY, int duration) {
        TranslateTransition translate = new TranslateTransition(Duration.millis(duration), node);
        translate.setFromX(fromX);
        translate.setToX(toX);
        translate.setFromY(fromY);
        translate.setToY(toY);
        translate.play();
    }

    /**
     * 设置节点可见性并播放动画
     * @param node 目标节点
     * @param visible 是否可见
     */
    public static void setVisibleWithAnimation(Node node, boolean visible) {
        if (visible) {
            node.setVisible(true);
            node.setOpacity(0.0);
            fadeIn(node, 300);
        } else {
            fadeOut(node, 300);
            node.setVisible(false);
        }
    }

    /**
     * 加载图片
     * @param imagePath 图片路径（相对于 resources 目录）
     * @return Image 对象，如果加载失败返回 null
     */
    public static Image loadImage(String imagePath) {
        try {
            return new Image(imagePath);
        } catch (Exception e) {
            System.err.println("无法加载图片: " + imagePath);
            return null;
        }
    }

    /**
     * 创建 ImageView
     * @param imagePath 图片路径
     * @param fitWidth 适应宽度
     * @param fitHeight 适应高度
     * @param preserveRatio 是否保持宽高比
     * @return ImageView 对象
     */
    public static ImageView createImageView(String imagePath, double fitWidth, double fitHeight, boolean preserveRatio) {
        Image image = loadImage(imagePath);
        if (image == null) {
            return null;
        }

        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(fitWidth);
        imageView.setFitHeight(fitHeight);
        imageView.setPreserveRatio(preserveRatio);

        return imageView;
    }

    /**
     * 居中窗口
     * @param stage 目标窗口
     */
    public static void centerStage(Stage stage) {
        stage.centerOnScreen();
    }

    /**
     * 设置窗口最小尺寸
     * @param stage 目标窗口
     * @param minWidth 最小宽度
     * @param minHeight 最小高度
     */
    public static void setStageMinSize(Stage stage, double minWidth, double minHeight) {
        stage.setMinWidth(minWidth);
        stage.setMinHeight(minHeight);
    }

    /**
     * 设置窗口固定尺寸
     * @param stage 目标窗口
     * @param width 宽度
     * @param height 高度
     */
    public static void setStageFixedSize(Stage stage, double width, double height) {
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setResizable(false);
    }

    /**
     * 获取窗口中心坐标
     * @param stage 目标窗口
     * @return 中心坐标 [x, y]
     */
    public static double[] getStageCenter(Stage stage) {
        double x = stage.getX() + stage.getWidth() / 2;
        double y = stage.getY() + stage.getHeight() / 2;
        return new double[]{x, y};
    }

    /**
     * 应用 CSS 样式到节点
     * @param node 目标节点
     * @param cssFile CSS 文件路径
     */
    public static void applyStylesheet(javafx.scene.Parent node, String cssFile) {
        node.getStylesheets().add(cssFile);
    }

    /**
     * 移除节点的所有 CSS 样式
     * @param node 目标节点
     */
    public static void clearStylesheets(javafx.scene.Parent node) {
        node.getStylesheets().clear();
    }

    /**
     * 设置节点禁用状态
     * @param node 目标节点
     * @param disabled 是否禁用
     */
    public static void setDisabled(Node node, boolean disabled) {
        node.setDisable(disabled);
        node.setOpacity(disabled ? 0.5 : 1.0);
    }

    /**
     * 延迟执行任务
     * @param delay 延迟时间（毫秒）
     * @param task 要执行的任务
     */
    public static void delay(int delay, Runnable task) {
        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(delay);
                task.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * 在 JavaFX 应用线程中执行任务
     * @param task 要执行的任务
     */
    public static void runLater(Runnable task) {
        javafx.application.Platform.runLater(task);
    }

    /**
     * 检查是否在 JavaFX 应用线程中
     * @return 如果在应用线程中返回 true
     */
    public static boolean isFxApplicationThread() {
        return javafx.application.Platform.isFxApplicationThread();
    }
}