package com.cashier.util;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

/**
 * 响应式布局工具类
 * 提供统一的布局配置，避免硬编码尺寸
 */
public class ResponsiveLayout {

    /**
     * 按钮尺寸预设
     */
    public static class ButtonSize {
        public static final double SMALL_MIN_WIDTH = 70;
        public static final double SMALL_MIN_HEIGHT = 28;

        public static final double NORMAL_MIN_WIDTH = 80;
        public static final double NORMAL_MIN_HEIGHT = 32;

        public static final double LARGE_MIN_WIDTH = 100;
        public static final double LARGE_MIN_HEIGHT = 36;

        public static final double EXTRA_LARGE_MIN_WIDTH = 120;
        public static final double EXTRA_LARGE_MIN_HEIGHT = 40;
    }

    /**
     * 输入框尺寸预设
     */
    public static class InputSize {
        public static final double SMALL_MIN_HEIGHT = 28;
        public static final double NORMAL_MIN_HEIGHT = 32;
        public static final double LARGE_MIN_HEIGHT = 36;

        public static final double SHORT_PREF_WIDTH = 150;
        public static final double NORMAL_PREF_WIDTH = 200;
        public static final double LONG_PREF_WIDTH = 300;
        public static final double EXTRA_LONG_PREF_WIDTH = 400;
    }

    /**
     * 表格尺寸预设
     */
    public static class TableSize {
        public static final double ROW_HEIGHT = 35;
        public static final double HEADER_HEIGHT = 40;
        public static final double MIN_WIDTH = 600;
        public static final double MIN_HEIGHT = 400;
    }

    /**
     * 对话框尺寸预设
     */
    public static class DialogSize {
        public static final double SMALL_WIDTH = 450;
        public static final double SMALL_HEIGHT = 350;

        public static final double NORMAL_WIDTH = 600;
        public static final double NORMAL_HEIGHT = 500;

        public static final double LARGE_WIDTH = 800;
        public static final double LARGE_HEIGHT = 600;

        public static final double EXTRA_LARGE_WIDTH = 1000;
        public static final double EXTRA_LARGE_HEIGHT = 700;
    }

    /**
     * 按钮尺寸枚举
     */
    public enum ButtonSizeEnum {
        SMALL(ButtonSize.SMALL_MIN_WIDTH, ButtonSize.SMALL_MIN_HEIGHT),
        NORMAL(ButtonSize.NORMAL_MIN_WIDTH, ButtonSize.NORMAL_MIN_HEIGHT),
        LARGE(ButtonSize.LARGE_MIN_WIDTH, ButtonSize.LARGE_MIN_HEIGHT),
        EXTRA_LARGE(ButtonSize.EXTRA_LARGE_MIN_WIDTH, ButtonSize.EXTRA_LARGE_MIN_HEIGHT);

        private final double minWidth;
        private final double minHeight;

        ButtonSizeEnum(double minWidth, double minHeight) {
            this.minWidth = minWidth;
            this.minHeight = minHeight;
        }

        public double getMinWidth() { return minWidth; }
        public double getMinHeight() { return minHeight; }
    }

    /**
     * 输入框尺寸枚举（高度）
     */
    public enum InputHeightSize {
        SMALL(InputSize.SMALL_MIN_HEIGHT),
        NORMAL(InputSize.NORMAL_MIN_HEIGHT),
        LARGE(InputSize.LARGE_MIN_HEIGHT);

        private final double minHeight;

        InputHeightSize(double minHeight) {
            this.minHeight = minHeight;
        }

        public double getMinHeight() { return minHeight; }
    }

    /**
     * 输入框尺寸枚举（宽度）
     */
    public enum InputWidthSize {
        SHORT(InputSize.SHORT_PREF_WIDTH),
        NORMAL(InputSize.NORMAL_PREF_WIDTH),
        LONG(InputSize.LONG_PREF_WIDTH),
        EXTRA_LONG(InputSize.EXTRA_LONG_PREF_WIDTH);

        private final double prefWidth;

        InputWidthSize(double prefWidth) {
            this.prefWidth = prefWidth;
        }

        public double getPrefWidth() { return prefWidth; }
    }

    /**
     * 配置标准按钮尺寸
     */
    public static void configureButton(Button button, ButtonSizeEnum size) {
        button.setMinWidth(size.getMinWidth());
        button.setMinHeight(size.getMinHeight());
    }

    /**
     * 配置标准输入框尺寸
     */
    public static void configureTextField(TextField field, InputHeightSize heightSize, InputWidthSize widthSize) {
        field.setMinHeight(heightSize.getMinHeight());
        field.setPrefWidth(widthSize.getPrefWidth());
    }

    /**
     * 配置 GridPane 列宽
     */
    public static void configureGridColumn(GridPane gridPane, int columnIndex, double percentWidth) {
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setPercentWidth(percentWidth);
        gridPane.getColumnConstraints().add(constraints);
    }

    /**
     * 配置 GridPane 行高
     */
    public static void configureGridRow(GridPane gridPane, int rowIndex, double percentHeight) {
        RowConstraints constraints = new RowConstraints();
        constraints.setPercentHeight(percentHeight);
        gridPane.getRowConstraints().add(constraints);
    }

    /**
     * 设置组件弹性增长
     */
    public static void setHGrow(javafx.scene.Node node, Priority priority) {
        GridPane.setHgrow(node, priority);
    }

    /**
     * 设置组件弹性增长
     */
    public static void setVGrow(javafx.scene.Node node, javafx.scene.layout.Priority priority) {
        GridPane.setVgrow(node, priority);
    }

    /**
     * 对话框尺寸枚举
     */
    public enum DialogSizeEnum {
        SMALL(DialogSize.SMALL_WIDTH, DialogSize.SMALL_HEIGHT),
        NORMAL(DialogSize.NORMAL_WIDTH, DialogSize.NORMAL_HEIGHT),
        LARGE(DialogSize.LARGE_WIDTH, DialogSize.LARGE_HEIGHT),
        EXTRA_LARGE(DialogSize.EXTRA_LARGE_WIDTH, DialogSize.EXTRA_LARGE_HEIGHT);

        private final double width;
        private final double height;

        DialogSizeEnum(double width, double height) {
            this.width = width;
            this.height = height;
        }

        public double getWidth() { return width; }
        public double getHeight() { return height; }
    }

    /**
     * 创建响应式列约束
     */
    public static ColumnConstraints createColumnConstraints(double minWidth, double prefWidth, double percentWidth, Priority hgrow) {
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setMinWidth(minWidth);
        constraints.setPrefWidth(prefWidth);
        constraints.setPercentWidth(percentWidth);
        constraints.setHgrow(hgrow);
        return constraints;
    }

    /**
     * 创建响应式行约束
     */
    public static RowConstraints createRowConstraints(double minHeight, double prefHeight, double percentHeight, Priority vgrow) {
        RowConstraints constraints = new RowConstraints();
        constraints.setMinHeight(minHeight);
        constraints.setPrefHeight(prefHeight);
        constraints.setPercentHeight(percentHeight);
        constraints.setVgrow(vgrow);
        return constraints;
    }
}
