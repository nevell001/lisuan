import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * 微交互效果工具类
 * 提供按钮悬停/点击动画、输入框焦点效果等微交互
 */
public class MicroInteractions {

    /**

         * 为按钮添加基本效果（不包含变色）

         * @param button 目标按钮

         */

        public static void addButtonEffects(JButton button) {

            // 按钮效果已禁用（变色功能已取消）

            // 保留此方法以维持代码兼容性

        }

    /**
     * 为文本框添加焦点动画效果
     * @param textField 目标文本框
     */
    public static void addTextFieldEffects(JTextField textField) {
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        textField.addFocusListener(new FocusAdapter() {
            private Timer timer;
            private float alpha = 0f;

            @Override
            public void focusGained(FocusEvent e) {
                startAnimation(0f, 1f, 300);
            }

            @Override
            public void focusLost(FocusEvent e) {
                startAnimation(alpha, 0f, 300);
            }

            private void startAnimation(float startAlpha, float endAlpha, int duration) {
                if (timer != null) {
                    timer.stop();
                }

                final float start = startAlpha;
                final float end = endAlpha;
                final long startTime = System.currentTimeMillis();

                timer = new Timer(16, e -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    float progress = Math.min(1f, (float) elapsed / duration);
                    alpha = start + (end - start) * easeInOutCubic(progress);

                    // 更新边框颜色
                    Color focusColor = new Color(63, 81, 181);
                    Color normalColor = new Color(200, 200, 200);
                    Color borderColor = interpolateColor(normalColor, focusColor, alpha);

                    textField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(borderColor, 1),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12)
                    ));

                    // 添加发光效果
                    if (alpha > 0.5f) {
                        textField.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(
                                    new Color(63, 81, 181, (int) (alpha * 50)), 2
                                ),
                                BorderFactory.createLineBorder(borderColor, 1)
                            ),
                            BorderFactory.createEmptyBorder(8, 12, 8, 12)
                        ));
                    }

                    if (progress >= 1f) {
                        timer.stop();
                    }
                });
                timer.start();
            }
        });
    }

    /**
     * 为密码框添加焦点动画效果
     * @param passwordField 目标密码框
     */
    public static void addPasswordFieldEffects(JPasswordField passwordField) {
        addTextFieldEffects(passwordField);
    }

    /**
     * 为文本区域添加焦点动画效果
     * @param textArea 目标文本区域
     */
    public static void addTextAreaEffects(JTextArea textArea) {
        textArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        textArea.addFocusListener(new FocusAdapter() {
            private Timer timer;
            private float alpha = 0f;

            @Override
            public void focusGained(FocusEvent e) {
                startAnimation(0f, 1f, 300);
            }

            @Override
            public void focusLost(FocusEvent e) {
                startAnimation(alpha, 0f, 300);
            }

            private void startAnimation(float startAlpha, float endAlpha, int duration) {
                if (timer != null) {
                    timer.stop();
                }

                final float start = startAlpha;
                final float end = endAlpha;
                final long startTime = System.currentTimeMillis();

                timer = new Timer(16, e -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    float progress = Math.min(1f, (float) elapsed / duration);
                    alpha = start + (end - start) * easeInOutCubic(progress);

                    Color focusColor = new Color(63, 81, 181);
                    Color normalColor = new Color(200, 200, 200);
                    Color borderColor = interpolateColor(normalColor, focusColor, alpha);

                    textArea.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(borderColor, 1),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12)
                    ));

                    if (progress >= 1f) {
                        timer.stop();
                    }
                });
                timer.start();
            }
        });
    }

    /**
     * 缓动函数 - Ease In Out Cubic
     * @param t 进度值 (0-1)
     * @return 缓动后的值
     */
    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3f) / 2f;
    }

    /**
     * 混合两种颜色
     * @param color1 颜色1
     * @param color2 颜色2
     * @param ratio 比例 (0-1)
     * @return 混合后的颜色
     */
    private static Color blendColors(Color color1, Color color2, float ratio) {
        int r = (int) (color1.getRed() * (1 - ratio) + color2.getRed() * ratio);
        int g = (int) (color1.getGreen() * (1 - ratio) + color2.getGreen() * ratio);
        int b = (int) (color1.getBlue() * (1 - ratio) + color2.getBlue() * ratio);
        return new Color(r, g, b);
    }

    /**
     * 在两种颜色之间插值
     * @param color1 起始颜色
     * @param color2 结束颜色
     * @param ratio 插值比例 (0-1)
     * @return 插值后的颜色
     */
    private static Color interpolateColor(Color color1, Color color2, float ratio) {
        int r = (int) (color1.getRed() + (color2.getRed() - color1.getRed()) * ratio);
        int g = (int) (color1.getGreen() + (color2.getGreen() - color1.getGreen()) * ratio);
        int b = (int) (color1.getBlue() + (color2.getBlue() - color1.getBlue()) * ratio);
        return new Color(r, g, b);
    }

    /**
     * 创建带有圆角的按钮
     * @param text 按钮文本
     * @param backgroundColor 背景色
     * @param textColor 文本颜色
     * @param arc 圆角弧度
     * @return 圆角按钮
     */
    public static JButton createRoundedButton(String text, Color backgroundColor,
                                              Color textColor, int arc) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                     RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isArmed()) {
                    g2d.setColor(backgroundColor.darker());
                } else if (getModel().isRollover()) {
                    g2d.setColor(backgroundColor.brighter());
                } else {
                    g2d.setColor(backgroundColor);
                }

                g2d.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1,
                                                      getHeight() - 1, arc, arc));
                g2d.dispose();

                super.paintComponent(g);
            }
        };

        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setForeground(textColor);
        button.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        addButtonEffects(button);

        return button;
    }
}