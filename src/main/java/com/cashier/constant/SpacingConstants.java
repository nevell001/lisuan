/**
 * 间距常量类
 * 定义统一的间距系统，确保整个应用的视觉一致性
 */
public class SpacingConstants {

    // 基础间距单位（8px 基准）
    private static final int BASE = 8;

    // ========== 基础间距 ==========

    /** 极小间距 - 4px */
    public static final int SPACING_XS = BASE / 2;

    /** 小间距 - 8px */
    public static final int SPACING_SM = BASE;

    /** 中间距 - 16px */
    public static final int SPACING_MD = BASE * 2;

    /** 大间距 - 24px */
    public static final int SPACING_LG = BASE * 3;

    /** 超大间距 - 32px */
    public static final int SPACING_XL = BASE * 4;

    /** 特大间距 - 48px */
    public static final int SPACING_XXL = BASE * 6;

    // ========== 组件间距 ==========

    /** 按钮内边距 - 上下 8px，左右 16px */
    public static final int BUTTON_PADDING_TOP = 8;
    public static final int BUTTON_PADDING_BOTTOM = 8;
    public static final int BUTTON_PADDING_LEFT = 16;
    public static final int BUTTON_PADDING_RIGHT = 16;

    /** 按钮之间的间距 - 8px */
    public static final int BUTTON_GAP = SPACING_SM;

    /** 输入框内边距 - 上下 10px，左右 12px */
    public static final int INPUT_PADDING_TOP = 10;
    public static final int INPUT_PADDING_BOTTOM = 10;
    public static final int INPUT_PADDING_LEFT = 12;
    public static final int INPUT_PADDING_RIGHT = 12;

    /** 输入框之间的间距 - 12px */
    public static final int INPUT_GAP = BASE + BASE / 2;

    /** 标签与输入框之间的间距 - 8px */
    public static final int LABEL_INPUT_GAP = SPACING_SM;

    // ========== 面板间距 ==========

    /** 面板外边距 - 16px */
    public static final int PANEL_MARGIN = SPACING_MD;

    /** 面板内边距 - 20px */
    public static final int PANEL_PADDING = 20;

    /** 面板之间的间距 - 16px */
    public static final int PANEL_GAP = SPACING_MD;

    /** 卡片内边距 - 24px */
    public static final int CARD_PADDING = SPACING_LG;

    /** 卡片之间的间距 - 16px */
    public static final int CARD_GAP = SPACING_MD;

    // ========== 表格间距 ==========

    /** 表格外边距 - 16px */
    public static final int TABLE_MARGIN = SPACING_MD;

    /** 表格单元格内边距 - 上下 8px，左右 12px */
    public static final int TABLE_CELL_PADDING_TOP = 8;
    public static final int TABLE_CELL_PADDING_BOTTOM = 8;
    public static final int TABLE_CELL_PADDING_LEFT = 12;
    public static final int TABLE_CELL_PADDING_RIGHT = 12;

    /** 表格行高 - 35px */
    public static final int TABLE_ROW_HEIGHT = 35;

    /** 表格与工具栏之间的间距 - 12px */
    public static final int TABLE_TOOLBAR_GAP = BASE + BASE / 2;

    // ========== 对话框间距 ==========

    /** 对话框外边距 - 24px */
    public static final int DIALOG_MARGIN = SPACING_LG;

    /** 对话框内边距 - 24px */
    public static final int DIALOG_PADDING = SPACING_LG;

    /** 对话框标题与内容之间的间距 - 16px */
    public static final int DIALOG_TITLE_CONTENT_GAP = SPACING_MD;

    /** 对话框内容与按钮之间的间距 - 24px */
    public static final int DIALOG_CONTENT_BUTTON_GAP = SPACING_LG;

    // ========== 标签页间距 ==========

    /** 标签页内容内边距 - 20px */
    public static final int TAB_CONTENT_PADDING = 20;

    /** 标签页之间的间距 - 4px */
    public static final int TAB_GAP = SPACING_XS;

    // ========== 菜单间距 ==========

    /** 菜单项内边距 - 上下 8px，左右 16px */
    public static final int MENU_ITEM_PADDING_TOP = 8;
    public static final int MENU_ITEM_PADDING_BOTTOM = 8;
    public static final int MENU_ITEM_PADDING_LEFT = 16;
    public static final int MENU_ITEM_PADDING_RIGHT = 16;

    /** 菜单项之间的间距 - 0px */
    public static final int MENU_ITEM_GAP = 0;

    // ========== 工具栏间距 ==========

    /** 工具栏内边距 - 上下 8px，左右 12px */
    public static final int TOOLBAR_PADDING_TOP = 8;
    public static final int TOOLBAR_PADDING_BOTTOM = 8;
    public static final int TOOLBAR_PADDING_LEFT = 12;
    public static final int TOOLBAR_PADDING_RIGHT = 12;

    /** 工具栏按钮之间的间距 - 8px */
    public static final int TOOLBAR_BUTTON_GAP = SPACING_SM;

    // ========== 表单间距 ==========

    /** 表单字段之间的间距 - 16px */
    public static final int FORM_FIELD_GAP = SPACING_MD;

    /** 表单组之间的间距 - 24px */
    public static final int FORM_GROUP_GAP = SPACING_LG;

    // ========== 辅助方法 ==========

    /**
     * 获取统一的内边距对象
     * @param top 上边距
     * @param left 左边距
     * @param bottom 下边距
     * @param right 右边距
     * @return EmptyBorder 对象
     */
    public static javax.swing.border.EmptyBorder getPadding(int top, int left, int bottom, int right) {
        return new javax.swing.border.EmptyBorder(top, left, bottom, right);
    }

    /**
     * 获取统一的外边距对象
     * @param margin 边距值
     * @return EmptyBorder 对象
     */
    public static javax.swing.border.EmptyBorder getMargin(int margin) {
        return new javax.swing.border.EmptyBorder(margin, margin, margin, margin);
    }

    /**
     * 获取水平间距对象
     * @param gap 间距值
     * @return EmptyBorder 对象（上下 0，左右 gap）
     */
    public static javax.swing.border.EmptyBorder getHorizontalGap(int gap) {
        return new javax.swing.border.EmptyBorder(0, gap, 0, gap);
    }

    /**
     * 获取垂直间距对象
     * @param gap 间距值
     * @return EmptyBorder 对象（上下 gap，左右 0）
     */
    public static javax.swing.border.EmptyBorder getVerticalGap(int gap) {
        return new javax.swing.border.EmptyBorder(gap, 0, gap, 0);
    }
}