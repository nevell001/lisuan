package com.cashier.constant;

import javafx.scene.paint.Color;

/**
 * JavaFX 常量类
 * 定义应用程序中使用的颜色、尺寸、字体等常量
 */
public class FXConstants {

    // ========== 主题常量 ==========

    /** 默认主题 */
    public static final String DEFAULT_THEME = "light";

    /** 浅色主题 */
    public static final String LIGHT_THEME = "light";

    /** 深色主题 */
    public static final String DARK_THEME = "dark";

    /** IntelliJ 主题 */
    public static final String INTELLIJ_THEME = "intellij";

    // ========== 颜色常量 - 语义化颜色 ==========

    // 主色调
    public static final Color PRIMARY_COLOR = Color.rgb(63, 81, 181);
    public static final Color PRIMARY_LIGHT = Color.rgb(99, 125, 255);
    public static final Color PRIMARY_DARK = Color.rgb(30, 55, 153);

    // 成功色
    public static final Color SUCCESS_COLOR = Color.rgb(76, 175, 80);
    public static final Color SUCCESS_LIGHT = Color.rgb(129, 199, 132);
    public static final Color SUCCESS_DARK = Color.rgb(56, 142, 60);

    // 警告色
    public static final Color WARNING_COLOR = Color.rgb(255, 152, 0);
    public static final Color WARNING_LIGHT = Color.rgb(255, 183, 77);
    public static final Color WARNING_DARK = Color.rgb(230, 81, 0);

    // 危险色
    public static final Color DANGER_COLOR = Color.rgb(244, 67, 54);
    public static final Color DANGER_LIGHT = Color.rgb(239, 83, 80);
    public static final Color DANGER_DARK = Color.rgb(198, 40, 40);

    // 信息色
    public static final Color INFO_COLOR = Color.rgb(33, 150, 243);
    public static final Color INFO_LIGHT = Color.rgb(100, 181, 246);
    public static final Color INFO_DARK = Color.rgb(13, 71, 161);

    // 功能性颜色
    public static final Color PURPLE_COLOR = Color.rgb(156, 39, 176);
    public static final Color PINK_COLOR = Color.rgb(233, 30, 99);
    public static final Color CYAN_COLOR = Color.rgb(0, 188, 212);
    public static final Color TEAL_COLOR = Color.rgb(0, 150, 136);

    // 中性色系
    public static final Color GRAY_50 = Color.rgb(250, 250, 250);
    public static final Color GRAY_100 = Color.rgb(245, 245, 245);
    public static final Color GRAY_200 = Color.rgb(238, 238, 238);
    public static final Color GRAY_300 = Color.rgb(224, 224, 224);
    public static final Color GRAY_400 = Color.rgb(189, 189, 189);
    public static final Color GRAY_500 = Color.rgb(158, 158, 158);
    public static final Color GRAY_600 = Color.rgb(117, 117, 117);
    public static final Color GRAY_700 = Color.rgb(97, 97, 97);
    public static final Color GRAY_800 = Color.rgb(66, 66, 66);
    public static final Color GRAY_900 = Color.rgb(33, 33, 33);

    // UI 背景和文本颜色
    public static final Color BACKGROUND_COLOR = Color.rgb(248, 249, 250);
    public static final Color CARD_BACKGROUND = Color.rgb(255, 255, 255);
    public static final Color BORDER_COLOR = GRAY_300;
    public static final Color TEXT_COLOR = GRAY_900;
    public static final Color SECONDARY_TEXT = GRAY_600;
    public static final Color DISABLED_TEXT = GRAY_400;

    // ========== 尺寸常量 ==========

    // 窗口尺寸
    public static final double WINDOW_WIDTH = 1300;
    public static final double WINDOW_HEIGHT = 800;
    public static final double MIN_WINDOW_WIDTH = 1000;
    public static final double MIN_WINDOW_HEIGHT = 600;

    // 按钮尺寸
    public static final double BUTTON_MIN_WIDTH = 80;
    public static final double BUTTON_MIN_HEIGHT = 32;
    public static final double BUTTON_PREF_WIDTH = 120;

    // 输入框尺寸
    public static final double TEXT_FIELD_MIN_HEIGHT = 32;
    public static final double TEXT_FIELD_PREF_HEIGHT = 36;

    // 表格尺寸
    public static final double TABLE_ROW_HEIGHT = 35;
    public static final double TABLE_HEADER_HEIGHT = 40;

    // 对话框尺寸
    public static final double DIALOG_MIN_WIDTH = 400;
    public static final double DIALOG_MIN_HEIGHT = 300;

    // ========== 字体常量 ==========

    /** 主字体名称 */
    public static final String PRIMARY_FONT_FAMILY = "Microsoft YaHei";

    /** 默认字体大小 */
    public static final double FONT_SIZE_NORMAL = 14;

    /** 大字体大小 */
    public static final double FONT_SIZE_LARGE = 16;

    /** 小字体大小 */
    public static final double FONT_SIZE_SMALL = 12;

    /** 标题字体大小 */
    public static final double FONT_SIZE_TITLE = 20;

    /** 头部字体大小 */
    public static final double FONT_SIZE_HEADING = 18;

    // ========== 动画常量 ==========

    /** 短动画时长（毫秒） */
    public static final int ANIMATION_DURATION_SHORT = 150;

    /** 标准动画时长（毫秒） */
    public static final int ANIMATION_DURATION_NORMAL = 300;

    /** 长动画时长（毫秒） */
    public static final int ANIMATION_DURATION_LONG = 500;

    // ========== Toast 通知常量 ==========

    /** Toast 显示时长（毫秒） */
    public static final int TOAST_DURATION = 3000;

    /** Toast 宽度 */
    public static final double TOAST_WIDTH = 350;

    /** Toast 高度 */
    public static final double TOAST_HEIGHT = 80;

    /** Toast 圆角半径 */
    public static final double TOAST_CORNER_RADIUS = 10;

    // ========== 边框半径常量 ==========

    /** 小圆角 */
    public static final double CORNER_RADIUS_SMALL = 4;

    /** 标准圆角 */
    public static final double CORNER_RADIUS_NORMAL = 8;

    /** 大圆角 */
    public static final double CORNER_RADIUS_LARGE = 12;

    // ========== 阴影常量 ==========

    /** 轻微阴影 */
    public static final String SHADOW_LIGHT = "0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.24)";

    /** 标准阴影 */
    public static final String SHADOW_NORMAL = "0 3px 6px rgba(0,0,0,0.16), 0 3px 6px rgba(0,0,0,0.23)";

    /** 深度阴影 */
    public static final String SHADOW_DEEP = "0 10px 20px rgba(0,0,0,0.19), 0 6px 6px rgba(0,0,0,0.23)";

    // ========== 其他常量 ==========

    /** 应用标题 */
    public static final String APP_TITLE = "收银系统";

    /** 应用版本 */
    public static final String APP_VERSION = "2.2.1";

    /** 数据目录名称 */
    public static final String DATA_DIR_NAME = "data";

    // ========== 辅助方法 ==========

    /**
     * 获取颜色字符串（用于 CSS）
     * @param color JavaFX Color 对象
     * @return CSS 颜色字符串
     */
    public static String toCssColor(Color color) {
        return String.format("rgba(%d, %d, %d, %.2f)",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255),
            color.getOpacity());
    }

    /**
     * 获取十六进制颜色字符串
     * @param color JavaFX Color 对象
     * @return 十六进制颜色字符串
     */
    public static String toHexColor(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }
}