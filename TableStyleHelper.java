import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 表格样式助手类
 * 提供统一的表格样式设置，包括隔行变色、选中高亮、悬停效果等
 */
public class TableStyleHelper {

    // 颜色常量
    private static final Color HEADER_BACKGROUND = new Color(63, 81, 181);      // 表头背景色（深靛蓝）
    private static final Color HEADER_FOREGROUND = Color.WHITE;                  // 表头前景色（白色）
    private static final Color BORDER_COLOR = new Color(224, 224, 224);          // 边框颜色
    private static final Color GRID_COLOR = new Color(224, 224, 224);            // 网格线颜色
    private static final Color CARD_BACKGROUND = new Color(255, 255, 255);       // 卡片背景色
    private static final Color TEXT_COLOR = new Color(66, 66, 66);               // 文本颜色

    // 尺寸常量
    private static final int ROW_HEIGHT = 35;           // 行高
    private static final int HEADER_HEIGHT = 40;        // 表头高度

    /**
     * 应用现代化表格样式
     * @param table 目标表格
     * @param model 表格模型
     */
    public static void styleTable(JTable table, DefaultTableModel model) {
        // 设置模型
        table.setModel(model);

        // 设置行高
        table.setRowHeight(ROW_HEIGHT);
        table.setRowMargin(0);

        // 设置选择模式（支持多选）
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // 设置网格颜色
        table.setGridColor(GRID_COLOR);
        table.setShowGrid(true);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);  // 只显示水平线，更现代

        // 设置背景和前景色
        table.setBackground(CARD_BACKGROUND);
        table.setForeground(TEXT_COLOR);

        // 设置字体
        table.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));

        // 设置选择样式
        table.setSelectionBackground(new Color(63, 81, 181, 40));
        table.setSelectionForeground(new Color(63, 81, 181));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 设置表头样式
        styleTableHeader(table.getTableHeader());

        // 应用自定义单元格渲染器（隔行变色）
        applyStyledRenderer(table);

        // 添加鼠标悬停效果
        addHoverEffect(table);

        // 设置边距
        table.setIntercellSpacing(new Dimension(0, 0));

        // 设置表格边框
        table.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

    /**
     * 设置表头样式
     * @param header 表头
     */
    private static void styleTableHeader(JTableHeader header) {
        header.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        header.setBackground(HEADER_BACKGROUND);
        header.setForeground(HEADER_FOREGROUND);
        header.setOpaque(true);
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);
        header.setPreferredSize(new Dimension(header.getWidth(), HEADER_HEIGHT));

        // 自定义表头渲染器
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component component = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column
                );

                setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
                setBackground(HEADER_BACKGROUND);
                setForeground(HEADER_FOREGROUND);
                setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                    BorderFactory.createEmptyBorder(0, 12, 0, 12)
                ));
                setHorizontalAlignment(JLabel.LEFT);
                setVerticalAlignment(JLabel.CENTER);

                return component;
            }
        });
    }

    /**
     * 应用自定义单元格渲染器
     * @param table 目标表格
     */
    private static void applyStyledRenderer(JTable table) {
        StyledTableCellRenderer renderer = new StyledTableCellRenderer();

        // 为所有列设置渲染器
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    /**
     * 添加鼠标悬停效果
     * @param table 目标表格
     */
    private static void addHoverEffect(JTable table) {
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());

                if (row >= 0 && column >= 0) {
                    // 更新所有渲染器的悬停行
                    for (int i = 0; i < table.getColumnCount(); i++) {
                        DefaultTableCellRenderer renderer =
                            (DefaultTableCellRenderer) table.getColumnModel().getColumn(i).getCellRenderer();
                        if (renderer instanceof StyledTableCellRenderer) {
                            ((StyledTableCellRenderer) renderer).setHoveredRow(row);
                        }
                    }
                    table.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // 清除悬停效果
                for (int i = 0; i < table.getColumnCount(); i++) {
                    DefaultTableCellRenderer renderer =
                        (DefaultTableCellRenderer) table.getColumnModel().getColumn(i).getCellRenderer();
                    if (renderer instanceof StyledTableCellRenderer) {
                        ((StyledTableCellRenderer) renderer).setHoveredRow(-1);
                    }
                }
                table.repaint();
            }
        });
    }

    /**
     * 应用紧凑型表格样式（行高较小）
     * @param table 目标表格
     * @param model 表格模型
     */
    public static void styleCompactTable(JTable table, DefaultTableModel model) {
        styleTable(table, model);
        table.setRowHeight(28);  // 较小的行高
    }

    /**
     * 应用宽松型表格样式（行高较大）
     * @param table 目标表格
     * @param model 表格模型
     */
    public static void styleSpaciousTable(JTable table, DefaultTableModel model) {
        styleTable(table, model);
        table.setRowHeight(42);  // 较大的行高
    }
}