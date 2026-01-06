import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * 自定义表格单元格渲染器
 * 实现隔行变色和选中行高亮效果
 */
public class StyledTableCellRenderer extends DefaultTableCellRenderer {

    private static final Color EVEN_ROW_COLOR = new Color(248, 250, 252);  // 偶数行背景色（浅蓝灰）
    private static final Color ODD_ROW_COLOR = new Color(255, 255, 255);   // 奇数行背景色（白色）
    private static final Color SELECTED_ROW_COLOR = new Color(63, 81, 181, 40);  // 选中行背景色（半透明深蓝）
    private static final Color SELECTED_TEXT_COLOR = new Color(63, 81, 181);     // 选中行文本色
    private static final Color HOVER_ROW_COLOR = new Color(63, 81, 181, 15);     // 悬停行背景色

    private int hoveredRow = -1;  // 当前悬停的行

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {

        Component component = super.getTableCellRendererComponent(
            table, value, isSelected, hasFocus, row, column
        );

        // 设置字体
        setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        // 设置背景色和前景色
        if (isSelected) {
            // 选中行
            setBackground(SELECTED_ROW_COLOR);
            setForeground(SELECTED_TEXT_COLOR);
            setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        } else if (row == hoveredRow) {
            // 悬停行
            setBackground(HOVER_ROW_COLOR);
            setForeground(new Color(66, 66, 66));
        } else if (row % 2 == 0) {
            // 偶数行
            setBackground(EVEN_ROW_COLOR);
            setForeground(new Color(66, 66, 66));
        } else {
            // 奇数行
            setBackground(ODD_ROW_COLOR);
            setForeground(new Color(66, 66, 66));
        }

        // 设置文本对齐方式
        if (value instanceof Number) {
            setHorizontalAlignment(JLabel.RIGHT);
        } else {
            setHorizontalAlignment(JLabel.LEFT);
        }

        return component;
    }

    /**
     * 设置当前悬停的行
     * @param row 行索引
     */
    public void setHoveredRow(int row) {
        this.hoveredRow = row;
    }

    /**
     * 获取当前悬停的行
     * @return 行索引
     */
    public int getHoveredRow() {
        return hoveredRow;
    }
}