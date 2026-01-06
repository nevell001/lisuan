import javax.swing.*;
import java.awt.Component;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Toast 集成助手类
 * 提供便捷的方法将 JOptionPane 消息转换为 Toast 通知
 */
public class ToastIntegrationHelper {

    /**
     * 判断是否应该使用 Toast 通知
     * 简单的消息使用 Toast，复杂的确认对话框继续使用 JOptionPane
     */
    public static boolean shouldUseToast(String message, int messageType) {
        // 简短消息（单行或两行）使用 Toast
        if (message != null && message.length() < 100) {
            return true;
        }

        // 错误消息使用 Toast
        if (messageType == JOptionPane.ERROR_MESSAGE) {
            return true;
        }

        // 成功消息使用 Toast
        if (messageType == JOptionPane.INFORMATION_MESSAGE) {
            return true;
        }

        return false;
    }

    /**
     * 显示消息（自动选择 Toast 或 JOptionPane）
     */
    public static void showMessage(Component parent, String message, String title, int messageType) {
        if (shouldUseToast(message, messageType)) {
            switch (messageType) {
                case JOptionPane.ERROR_MESSAGE:
                    ToastNotification.showError(parent, message);
                    break;
                case JOptionPane.WARNING_MESSAGE:
                    ToastNotification.showWarning(parent, message);
                    break;
                case JOptionPane.INFORMATION_MESSAGE:
                    ToastNotification.showSuccess(parent, message);
                    break;
                default:
                    ToastNotification.showInfo(parent, message);
            }
        } else {
            // 复杂消息使用 JOptionPane
            JOptionPane.showMessageDialog(parent, message, title, messageType);
        }
    }

    /**
     * 显示成功消息
     */
    public static void showSuccess(Component parent, String message) {
        ToastNotification.showSuccess(parent, message);
    }

    /**
     * 显示错误消息
     */
    public static void showError(Component parent, String message) {
        ToastNotification.showError(parent, message);
    }

    /**
     * 显示警告消息
     */
    public static void showWarning(Component parent, String message) {
        ToastNotification.showWarning(parent, message);
    }

    /**
     * 显示信息消息
     */
    public static void showInfo(Component parent, String message) {
        ToastNotification.showInfo(parent, message);
    }

    /**
     * 从多行消息中提取第一行用于 Toast
     */
    public static String extractFirstLine(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        // 按换行符分割
        String[] lines = message.split("\n");
        if (lines.length > 0) {
            return lines[0];
        }

        return message;
    }

    /**
     * 从消息中提取关键信息用于 Toast
     * 例如："商品添加成功！\n商品名称: xxx" -> "商品添加成功"
     */
    public static String extractKeyMessage(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        // 匹配 "xxx成功！" 或 "xxx失败！" 模式
        Pattern pattern = Pattern.compile("^(.+[成功失败]+！)");
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            return matcher.group(1);
        }

        // 如果没有匹配，返回第一行
        return extractFirstLine(message);
    }
}
