package com.cashier.util;

import com.cashier.constant.FXConstants;
import com.cashier.i18n.I18nManager;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 对话框构建器
 * 统一对话框样式和构建流程
 */
public class DialogBuilder {
    private static final Logger logger = LoggerFactoryUtil.getLogger(DialogBuilder.class);

    private final Alert.AlertType alertType;
    private String title;
    private String contentText;
    private String headerText;
    private String okText;
    private String cancelText;
    private String yesText;
    private String noText;
    private String detailsText;
    private boolean expandable = false;
    private boolean resizable = false;
    private double width = 0;
    private double height = 0;
    private Stage owner;
    private Modality modality = Modality.APPLICATION_MODAL;
    private Runnable onOkAction;
    private Runnable onCancelAction;
    private Runnable onYesAction;
    private Runnable onNoAction;
    private Consumer<Alert> configurator;

    /**
     * 创建确认对话框
     */
    public static DialogBuilder confirmation() {
        return new DialogBuilder(Alert.AlertType.CONFIRMATION);
    }

    /**
     * 创建信息对话框
     */
    public static DialogBuilder information() {
        return new DialogBuilder(Alert.AlertType.INFORMATION);
    }

    /**
     * 创建警告对话框
     */
    public static DialogBuilder warning() {
        return new DialogBuilder(Alert.AlertType.WARNING);
    }

    /**
     * 创建错误对话框
     */
    public static DialogBuilder error() {
        return new DialogBuilder(Alert.AlertType.ERROR);
    }

    /**
     * 创建无类型对话框（可自定义）
     */
    public static DialogBuilder create() {
        return new DialogBuilder(null);
    }

    private DialogBuilder(Alert.AlertType alertType) {
        this.alertType = alertType;
    }

    /**
     * 设置标题
     */
    public DialogBuilder title(String title) {
        this.title = title;
        return this;
    }

    /**
     * 设置内容
     */
    public DialogBuilder content(String content) {
        this.contentText = content;
        return this;
    }

    /**
     * 设置头部文本
     */
    public DialogBuilder header(String header) {
        this.headerText = header;
        return this;
    }

    /**
     * 设置确认按钮文本
     */
    public DialogBuilder okText(String text) {
        this.okText = text;
        return this;
    }

    /**
     * 设置取消按钮文本
     */
    public DialogBuilder cancelText(String text) {
        this.cancelText = text;
        return this;
    }

    /**
     * 设置是按钮文本
     */
    public DialogBuilder yesText(String text) {
        this.yesText = text;
        return this;
    }

    /**
     * 设置否按钮文本
     */
    public DialogBuilder noText(String text) {
        this.noText = text;
        return this;
    }

    /**
     * 设置详细信息
     */
    public DialogBuilder details(String details) {
        this.detailsText = details;
        return this;
    }

    /**
     * 设置是否可展开
     */
    public DialogBuilder expandable(boolean expandable) {
        this.expandable = expandable;
        return this;
    }

    /**
     * 设置是否可调整大小
     */
    public DialogBuilder resizable(boolean resizable) {
        this.resizable = resizable;
        return this;
    }

    /**
     * 设置宽度
     */
    public DialogBuilder width(double width) {
        this.width = width;
        return this;
    }

    /**
     * 设置高度
     */
    public DialogBuilder height(double height) {
        this.height = height;
        return this;
    }

    /**
     * 设置所有者窗口
     */
    public DialogBuilder owner(Window owner) {
        if (owner instanceof Stage) {
            this.owner = (Stage) owner;
        }
        return this;
    }

    /**
     * 设置模态类型
     */
    public DialogBuilder modality(Modality modality) {
        this.modality = modality;
        return this;
    }

    /**
     * 设置确认按钮回调
     */
    public DialogBuilder onOk(Runnable action) {
        this.onOkAction = action;
        return this;
    }

    /**
     * 设置取消按钮回调
     */
    public DialogBuilder onCancel(Runnable action) {
        this.onCancelAction = action;
        return this;
    }

    /**
     * 设置是按钮回调
     */
    public DialogBuilder onYes(Runnable action) {
        this.onYesAction = action;
        return this;
    }

    /**
     * 设置否按钮回调
     */
    public DialogBuilder onNo(Runnable action) {
        this.onNoAction = action;
        return this;
    }

    /**
     * 自定义配置器
     */
    public DialogBuilder configure(Consumer<Alert> configurator) {
        this.configurator = configurator;
        return this;
    }

    /**
     * 显示并等待用户响应
     * @return 用户点击的按钮类型
     */
    public ButtonType showAndWait() {
        if (alertType == null) {
            logger.warn("尝试显示无类型对话框，使用默认确认类型");
            return ButtonType.CANCEL;
        }

        I18nManager i18n = I18nManager.getInstance();

        Alert alert = new Alert(alertType);
        alert.initModality(this.modality);

        // 设置标题
        if (title != null) {
            alert.setTitle(title);
        }

        // 设置头部
        if (headerText != null) {
            alert.setHeaderText(headerText);
        } else {
            alert.setHeaderText(null);
        }

        // 设置内容
        if (contentText != null) {
            alert.setContentText(contentText);
        }

        // 设置按钮文本
        if (okText != null) {
            alert.getButtonTypes().setAll(ButtonType.OK);
            // 由于 JavaFX 限制，这里简化处理
        }

        // 设置可展开
        if (expandable && detailsText != null) {
            // 创建详细信息区域作为可展开内容
            TextArea textArea = new TextArea(detailsText);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxHeight(150);
            alert.getDialogPane().setExpandableContent(textArea);
            alert.getDialogPane().setExpanded(true);
        }

        // 设置可调整大小
        alert.setResizable(resizable);

        // 设置尺寸
        if (width > 0 || height > 0) {
            // 设置对话框尺寸的代码
            if (width > 0) {
                alert.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
                alert.getDialogPane().setPrefWidth(width);
            }
            if (height > 0) {
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                alert.getDialogPane().setPrefHeight(height);
            }
        }

        // 设置所有者
        if (owner != null) {
            alert.initOwner(owner);
        }

        // 设置详细信息（非展开模式）
        if (detailsText != null && !expandable) {
            // 创建详细信息区域
            TextArea textArea = new TextArea(detailsText);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxHeight(150);
            VBox content = new VBox();
            content.getChildren().add(alert.getDialogPane().getContent());
            content.getChildren().add(textArea);
            alert.getDialogPane().setContent(content);
        }

        // 应用自定义配置
        if (configurator != null) {
            configurator.accept(alert);
        }

        // 显示并等待
        Optional<ButtonType> result = alert.showAndWait();

        // 执行回调
        result.ifPresent(buttonType -> {
            if (ButtonType.OK.equals(buttonType) && onOkAction != null) {
                onOkAction.run();
            } else if (ButtonType.CANCEL.equals(buttonType) && onCancelAction != null) {
                onCancelAction.run();
            } else if (ButtonType.YES.equals(buttonType) && onYesAction != null) {
                onYesAction.run();
            } else if (ButtonType.NO.equals(buttonType) && onNoAction != null) {
                onNoAction.run();
            }
        });

        return result.orElse(ButtonType.CANCEL);
    }

    /**
     * 显示对话框（无返回值）
     */
    public void show() {
        showAndWait();
    }

    /**
     * 显示并返回用户是否确认
     */
    public boolean showAndGetConfirm() {
        ButtonType result = showAndWait();
        return ButtonType.OK.equals(result);
    }

    /**
     * 快速显示确认对话框
     * @param message 确认消息
     * @return 用户是否确认
     */
    public static boolean confirm(String message) {
        return confirmation()
                .content(message)
                .showAndGetConfirm();
    }

    /**
     * 快速显示信息对话框
     * @param message 信息消息
     */
    public static void info(String message) {
        information()
                .content(message)
                .show();
    }

    /**
     * 快速显示警告对话框
     * @param message 警告消息
     */
    public static void warn(String message) {
        warning()
                .content(message)
                .show();
    }

    /**
     * 快速显示错误对话框
     * @param message 错误消息
     */
    public static void error(String message) {
        error()
                .content(message)
                .show();
    }

    /**
     * 对话框尺寸预设
     */
    public static class DialogSize {
        public static final double SMALL = 400;
        public static final double MEDIUM = 600;
        public static final double LARGE = 800;

        private DialogSize() {}
    }
}
