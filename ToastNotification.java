import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Toast 通知系统
 * 提供优雅的非模态通知提示
 */
public class ToastNotification extends JWindow {

    private static final int TOAST_WIDTH = 350;
    private static final int TOAST_HEIGHT = 80;
    private static final int CORNER_RADIUS = 10;
    private static final int DURATION = 3000; // 显示时长（毫秒）

    private static List<ToastNotification> activeToasts = new ArrayList<>();
    private Timer timer;
    private float opacity = 0f;
    private int duration = DURATION;

    /**
     * Toast 类型枚举
     */
    public enum ToastType {
        SUCCESS(new Color(76, 175, 80), new Color(129, 199, 132), "✓"),
        WARNING(new Color(255, 152, 0), new Color(255, 183, 77), "⚠"),
        ERROR(new Color(244, 67, 54), new Color(239, 83, 80), "✕"),
        INFO(new Color(33, 150, 243), new Color(100, 181, 246), "ℹ");

        private final Color backgroundColor;
        private final Color iconColor;
        private final String iconText;

        ToastType(Color backgroundColor, Color iconColor, String iconText) {
            this.backgroundColor = backgroundColor;
            this.iconColor = iconColor;
            this.iconText = iconText;
        }
    }

    /**
     * 显示成功通知
     * @param parent 父窗口
     * @param message 通知消息
     */
    public static void showSuccess(Component parent, String message) {
        showToast(parent, message, ToastType.SUCCESS);
    }

    /**
     * 显示警告通知
     * @param parent 父窗口
     * @param message 通知消息
     */
    public static void showWarning(Component parent, String message) {
        showToast(parent, message, ToastType.WARNING);
    }

    /**
     * 显示错误通知
     * @param parent 父窗口
     * @param message 通知消息
     */
    public static void showError(Component parent, String message) {
        showToast(parent, message, ToastType.ERROR);
    }

    /**
     * 显示信息通知
     * @param parent 父窗口
     * @param message 通知消息
     */
    public static void showInfo(Component parent, String message) {
        showToast(parent, message, ToastType.INFO);
    }

    /**
     * 显示自定义类型的通知
     * @param parent 父窗口
     * @param message 通知消息
     * @param type 通知类型
     */
    public static void showToast(Component parent, String message, ToastType type) {
        SwingUtilities.invokeLater(() -> {
            ToastNotification toast = new ToastNotification(parent, message, type);
            toast.show();
        });
    }

    protected ToastNotification(Component parent, String message, ToastType type) {
        this.duration = DURATION;
        init(parent, message, type);
    }

    protected ToastNotification(Component parent, String message, ToastType type, int duration) {
        this.duration = duration;
        init(parent, message, type);
    }

    private void init(Component parent, String message, ToastType type) {
        setAlwaysOnTop(true);
        setFocusableWindowState(false);

        // 创建内容面板
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                     RenderingHints.VALUE_ANTIALIAS_ON);

                // 绘制圆角背景
                g2d.setColor(type.backgroundColor);
                g2d.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1,
                                                      getHeight() - 1, CORNER_RADIUS, CORNER_RADIUS));

                // 绘制阴影效果
                g2d.setColor(new Color(0, 0, 0, 20));
                g2d.fill(new RoundRectangle2D.Double(0, getHeight() - 5,
                                                      getWidth() - 1, 5, CORNER_RADIUS, CORNER_RADIUS));

                g2d.dispose();
            }
        };
        mainPanel.setLayout(new BorderLayout(10, 0));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // 创建图标标签
        JLabel iconLabel = new JLabel(type.iconText);
        iconLabel.setFont(new Font("Arial", Font.BOLD, 24));
        iconLabel.setForeground(Color.WHITE);
        iconLabel.setPreferredSize(new Dimension(40, 40));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);

        // 创建消息标签
        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        // 创建关闭按钮
        JButton closeButton = new JButton("×");
        closeButton.setFont(new Font("Arial", Font.BOLD, 18));
        closeButton.setForeground(Color.WHITE);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setPreferredSize(new Dimension(30, 30));
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dismiss());

        // 添加组件
        mainPanel.add(iconLabel, BorderLayout.WEST);
        mainPanel.add(messageLabel, BorderLayout.CENTER);
        mainPanel.add(closeButton, BorderLayout.EAST);

        // 添加鼠标点击关闭
        mainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dismiss();
            }
        });

        setContentPane(mainPanel);
        setSize(TOAST_WIDTH, TOAST_HEIGHT);

        // 定位 Toast
        setLocation(parent, this);
    }

    /**
     * 设置 Toast 位置（屏幕右下角）
     */
    private void setLocation(Component parent, ToastNotification toast) {
        GraphicsConfiguration gc = parent != null ?
            parent.getGraphicsConfiguration() :
            GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

        Rectangle screenBounds = gc.getBounds();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);

        int x = screenBounds.x + screenBounds.width - TOAST_WIDTH - 20;
        int y = screenBounds.y + screenBounds.height - screenInsets.bottom - TOAST_HEIGHT - 20;

        // 计算多个 Toast 的堆叠位置
        int offset = 0;
        for (ToastNotification other : activeToasts) {
            if (other.isVisible()) {
                offset += TOAST_HEIGHT + 10;
            }
        }

        setLocation(x, y - offset);
    }

    /**
     * 显示 Toast（带动画）
     */
    public void show() {
        activeToasts.add(this);
        setVisible(true);

        // 淡入动画
        Timer fadeInTimer = new Timer(16, e -> {
            opacity += 0.05f;
            if (opacity >= 1f) {
                opacity = 1f;
                ((Timer) e.getSource()).stop();
            }
            setOpacity(opacity);
        });
        fadeInTimer.start();

        // 自动关闭定时器
        timer = new Timer(duration, e -> dismiss());
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * 关闭 Toast（带动画）
     */
    public void dismiss() {
        if (timer != null) {
            timer.stop();
        }

        // 淡出动画
        Timer fadeOutTimer = new Timer(16, e -> {
            opacity -= 0.05f;
            if (opacity <= 0f) {
                opacity = 0f;
                ((Timer) e.getSource()).stop();
                dispose();
                activeToasts.remove(this);
            }
            setOpacity(opacity);
        });
        fadeOutTimer.start();
    }

    /**
     * 关闭所有活动的 Toast
     */
    public static void dismissAll() {
        List<ToastNotification> toasts = new ArrayList<>(activeToasts);
        for (ToastNotification toast : toasts) {
            toast.dismiss();
        }
    }
}

/**
 * Toast 通知管理器
 * 提供便捷的通知显示方法
 */
class ToastManager {

    /**
     * 显示成功通知
     */
    public static void success(Component parent, String message) {
        ToastNotification.showSuccess(parent, message);
    }

    /**
     * 显示警告通知
     */
    public static void warning(Component parent, String message) {
        ToastNotification.showWarning(parent, message);
    }

    /**
     * 显示错误通知
     */
    public static void error(Component parent, String message) {
        ToastNotification.showError(parent, message);
    }

    /**
     * 显示信息通知
     */
    public static void info(Component parent, String message) {
        ToastNotification.showInfo(parent, message);
    }

    /**
     * 显示自定义时长的通知
     */
    public static void show(Component parent, String message,
                           ToastNotification.ToastType type, int duration) {
        SwingUtilities.invokeLater(() -> {
            ToastNotification toast = new ToastNotification(parent, message, type, duration);
            toast.show();
        });
    }
}