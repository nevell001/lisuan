import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatLaf;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class CashierSystemGUI extends JFrame {
    // 现代配色方案 - 优化版
    // 语义化颜色
    private static final Color PRIMARY_COLOR = new Color(63, 81, 181);       // 主色调 - 深靛蓝
    private static final Color PRIMARY_LIGHT = new Color(99, 125, 255);      // 主色调浅色
    private static final Color PRIMARY_DARK = new Color(30, 55, 153);        // 主色调深色

    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);       // 成功 - 绿色
    private static final Color SUCCESS_LIGHT = new Color(129, 199, 132);     // 成功浅色
    private static final Color SUCCESS_DARK = new Color(56, 142, 60);        // 成功深色

    private static final Color WARNING_COLOR = new Color(255, 152, 0);       // 警告 - 橙色
    private static final Color WARNING_LIGHT = new Color(255, 183, 77);      // 警告浅色
    private static final Color WARNING_DARK = new Color(230, 81, 0);         // 警告深色

    private static final Color DANGER_COLOR = new Color(244, 67, 54);        // 危险 - 红色
    private static final Color DANGER_LIGHT = new Color(239, 83, 80);        // 危险浅色
    private static final Color DANGER_DARK = new Color(198, 40, 40);         // 危险深色

    private static final Color INFO_COLOR = new Color(33, 150, 243);         // 信息 - 亮蓝
    private static final Color INFO_LIGHT = new Color(100, 181, 246);        // 信息浅色
    private static final Color INFO_DARK = new Color(13, 71, 161);           // 信息深色

    // 功能性颜色
    private static final Color PURPLE_COLOR = new Color(156, 39, 176);       // 紫色
    private static final Color PINK_COLOR = new Color(233, 30, 99);          // 粉色
    private static final Color CYAN_COLOR = new Color(0, 188, 212);          // 青色
    private static final Color TEAL_COLOR = new Color(0, 150, 136);          // 蓝绿色

    // 中性色系
    private static final Color GRAY_50 = new Color(250, 250, 250);
    private static final Color GRAY_100 = new Color(245, 245, 245);
    private static final Color GRAY_200 = new Color(238, 238, 238);
    private static final Color GRAY_300 = new Color(224, 224, 224);
    private static final Color GRAY_400 = new Color(189, 189, 189);
    private static final Color GRAY_500 = new Color(158, 158, 158);
    private static final Color GRAY_600 = new Color(117, 117, 117);
    private static final Color GRAY_700 = new Color(97, 97, 97);
    private static final Color GRAY_800 = new Color(66, 66, 66);
    private static final Color GRAY_900 = new Color(33, 33, 33);

    // 保留旧颜色常量以兼容（标记为废弃）
    @Deprecated
    private static final Color GRAY_COLOR = GRAY_500;

    // UI 背景和文本颜色
    private static final Color BACKGROUND_COLOR = new Color(248, 249, 250);  // 背景色 - 更柔和
    private static final Color CARD_BACKGROUND = new Color(255, 255, 255);   // 卡片背景 - 纯白
    private static final Color BORDER_COLOR = GRAY_300;                       // 边框色
    private static final Color TEXT_COLOR = GRAY_900;                         // 文本色 - 深灰
    private static final Color SECONDARY_TEXT = GRAY_600;                    // 次要文本
    private static final Color DISABLED_TEXT = GRAY_400;                     // 禁用文本

    // 特殊用途颜色
    private static final Color HOVER_COLOR = new Color(0, 0, 0, 8);          // 悬停效果
    private static final Color FOCUS_COLOR = new Color(63, 81, 181, 12);     // 焦点效果
    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 10);        // 阴影效果

    /**
     * 根据操作系统获取合适的中文字体
     * @param style 字体样式 (Font.PLAIN, Font.BOLD, Font.ITALIC)
     * @param size 字体大小
     * @return 支持中文的 Font 对象
     */
    private static Font getChineseFont(int style, int size) {
        String osName = System.getProperty("os.name", "").toLowerCase();

        // 按优先级尝试字体列表
        String[] preferredFonts = {
            // Windows 系统字体
            "Microsoft YaHei", "微软雅黑", "SimSun", "宋体", "SimHei", "黑体",

            // macOS 系统字体
            "PingFang SC", "PingFang TC", "Heiti SC", "STHeiti", "Hiragino Sans GB",

            // Linux 开源字体
            "Noto Sans CJK SC", "Noto Sans CJK TC", "Noto Sans CJK",
            "WenQuanYi Micro Hei", "WenQuanYi Zen Hei",
            "Source Han Sans CN", "Source Han Sans",
            "AR PL UMing CN", "AR PL UKai CN",
            "UMing CN", "UKai CN",

            // 通用后备字体
            "DejaVu Sans", "Liberation Sans", "Ubuntu", "Roboto",
            "Dialog", "SanSerif", "Serif", "Monospaced"
        };

        // 获取系统中所有可用的字体
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = ge.getAvailableFontFamilyNames();

        // 尝试找到第一个可用的字体
        for (String preferredFont : preferredFonts) {
            for (String available : availableFonts) {
                // 宽松匹配：忽略大小写和空格
                if (available.replaceAll("\\s+", "").equalsIgnoreCase(preferredFont.replaceAll("\\s+", ""))) {
                    Font font = new Font(available, style, size);
                    // 验证字体是否能显示中文
                    if (canDisplayChinese(font)) {
                        return font;
                    }
                }
            }
        }

        // 如果所有字体都不支持中文，使用系统默认字体
        return new Font(Font.SANS_SERIF, style, size);
    }

    /**
     * 检查字体是否能显示中文字符
     * @param font 要检查的字体
     * @return 如果字体能显示中文返回 true
     */
    private static boolean canDisplayChinese(Font font) {
        // 测试一些常用的中文字符
        String testChars = "中文字符测试收银系统";
        for (char c : testChars.toCharArray()) {
            if (!font.canDisplay(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取通用字体（用于数字、英文和中文）
     * @param style 字体样式
     * @param size 字体大小
     * @return Font 对象
     */
    private static Font getGeneralFont(int style, int size) {
        // 按优先级尝试字体列表（同时包含中英文字体）
        String[] preferredFonts = {
            // Windows 系统字体
            "Microsoft YaHei", "微软雅黑", "Arial", "Segoe UI", "Tahoma", "Verdana",
            "SimSun", "宋体", "SimHei", "黑体",

            // macOS 系统字体
            "PingFang SC", "SF Pro Text", "Helvetica Neue", "Helvetica", "Geneva",
            "Heiti SC", "STHeiti", "Hiragino Sans GB",

            // Linux 开源字体
            "Noto Sans CJK SC", "Noto Sans CJK TC", "Noto Sans CJK",
            "WenQuanYi Micro Hei", "WenQuanYi Zen Hei",
            "Source Han Sans CN", "Source Han Sans",
            "DejaVu Sans", "Liberation Sans", "Ubuntu", "Roboto",
            "Droid Sans", "Bitstream Vera Sans",

            // 通用后备字体
            "Dialog", "SanSerif", "SansSerif"
        };

        // 获取系统中所有可用的字体
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = ge.getAvailableFontFamilyNames();

        // 尝试找到第一个可用的字体
        for (String preferredFont : preferredFonts) {
            for (String available : availableFonts) {
                // 宽松匹配：忽略大小写和空格
                if (available.replaceAll("\\s+", "").equalsIgnoreCase(preferredFont.replaceAll("\\s+", ""))) {
                    Font font = new Font(available, style, size);
                    // 验证字体是否能显示中文
                    if (canDisplayChinese(font)) {
                        return font;
                    }
                }
            }
        }

        // 如果找不到，使用系统默认字体
        return new Font(Font.SANS_SERIF, style, size);
    }

    private List<Product> cart = new ArrayList<>();
    private Map<String, Product> inventory = new HashMap<>();
    private List<Transaction> transactions = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private Map<String, Member> members = new HashMap<>();
    private List<Promotion> promotions = new ArrayList<>();
    private List<RechargeRecord> rechargeRecords = new ArrayList<>();  // 充值记录
    private Map<String, User> users = new HashMap<>();  // 用户列表
    private List<OperationLog> operationLogs = new ArrayList<>();  // 操作日志
    private List<Shift> shifts = new ArrayList<>();  // 交接班记录
    private Shift currentShift = null;  // 当前班次
    private User currentUser = null;  // 当前登录用户
    private Member currentMember = null;  // 当前选择的会员
    private Promotion currentPromotion = null;  // 当前选择的促销
    private double taxRate = 0.0;
    private int transactionCount = 0;

    private JTable inventoryTable;
    private DefaultTableModel inventoryTableModel;
    private JTable cartTable;
    private DefaultTableModel cartTableModel;
    private JTable transactionTable;
    private DefaultTableModel transactionTableModel;
    private JLabel cartTotalLabel;
    private JLabel cartTypeCountLabel;
    private JLabel cartTotalQuantityLabel;
    private JLabel taxRateLabel;
    private JTextField searchField;
    private JComboBox<String> sortComboBox;
    private JLabel transactionTotalRevenueLabel;
    private JLabel transactionCountLabel;
    private JLabel transactionAvgRevenueLabel;
    private JLabel transactionTotalTaxLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 使用FlatLaf Light主题
                FlatLightLaf.setup();

                // 设置全局默认字体，确保中文正确显示
                Font defaultFont = getChineseFont(Font.PLAIN, 12);
                UIManager.put("Label.font", defaultFont);
                UIManager.put("Button.font", defaultFont);
                UIManager.put("TextField.font", defaultFont);
                UIManager.put("TextArea.font", defaultFont);
                UIManager.put("Table.font", defaultFont);
                UIManager.put("TableHeader.font", getChineseFont(Font.BOLD, 12));
                UIManager.put("ComboBox.font", defaultFont);
                UIManager.put("CheckBox.font", defaultFont);
                UIManager.put("RadioButton.font", defaultFont);
                UIManager.put("Panel.font", defaultFont);
                UIManager.put("TitledBorder.font", defaultFont);
                UIManager.put("ToolTip.font", defaultFont);
                UIManager.put("TabbedPane.font", getChineseFont(Font.BOLD, 12));
                UIManager.put("Menu.font", defaultFont);
                UIManager.put("MenuItem.font", defaultFont);

                // 设置对话框字体
                UIManager.put("OptionPane.font", defaultFont);
                UIManager.put("OptionPane.buttonFont", defaultFont);
                UIManager.put("OptionPane.messageFont", defaultFont);
                UIManager.put("OptionPane.buttonFont", defaultFont);

            } catch (Exception e) {
                e.printStackTrace();
            }

            // 先显示登录界面
            showLoginDialog();
        });
    }

    private static void showLoginDialog() {
        // 初始化数据管理器
        DataManager.initialize();

        // 加载用户数据
        Map<String, User> users = DataManager.loadUsers();

        JDialog loginDialog = new JDialog();
        loginDialog.setTitle("收银系统 - 用户登录");
        loginDialog.setSize(400, 300);
        loginDialog.setLocationRelativeTo(null);
        loginDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        loginDialog.setModal(true);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 标题
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("收银系统");
        titleLabel.setFont(getChineseFont(Font.BOLD, 24));
        titleLabel.setForeground(new Color(74, 144, 226));
        panel.add(titleLabel, gbc);

        // 用户名
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(new JLabel("用户名:"), gbc);

        gbc.gridx = 1;
        JTextField usernameField = new JTextField(20);
        usernameField.setFont(getChineseFont(Font.PLAIN, 13));
        MicroInteractions.addTextFieldEffects(usernameField);
        panel.add(usernameField, gbc);

        // 密码
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("密码:"), gbc);

        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setFont(getChineseFont(Font.PLAIN, 13));
        MicroInteractions.addPasswordFieldEffects(passwordField);
        panel.add(passwordField, gbc);

        // 按钮
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton loginButton = new JButton("登录");
        loginButton.setFont(getChineseFont(Font.PLAIN, 13));
        loginButton.setBackground(new Color(46, 204, 113));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        MicroInteractions.addButtonEffects(loginButton);

        JButton exitButton = new JButton("退出");
        exitButton.setFont(getChineseFont(Font.PLAIN, 13));
        exitButton.setBackground(new Color(231, 76, 60));
        exitButton.setForeground(Color.WHITE);
        exitButton.setFocusPainted(false);
        MicroInteractions.addButtonEffects(exitButton);

        buttonPanel.add(loginButton);
        buttonPanel.add(exitButton);
        panel.add(buttonPanel, gbc);

        loginDialog.add(panel);

        // 登录按钮事件
        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                ToastNotification.showError(loginDialog, "用户名和密码不能为空！");
                return;
            }

            User user = users.get(username);
            if (user == null) {
                ToastNotification.showError(loginDialog, "用户名不存在！");
                return;
            }

            if (!user.password.equals(password)) {
                ToastNotification.showError(loginDialog, "密码错误！");
                return;
            }

            if (!user.active) {
                ToastNotification.showError(loginDialog, "该账户已被禁用！");
                return;
            }

            // 更新最后登录时间
            user.lastLoginTime = new Date();
            DataManager.saveUsers(users);

            // 显示登录成功提示
            ToastNotification.showSuccess(loginDialog, "登录成功！欢迎 " + user.name);

            // 关闭登录对话框，显示主窗口
            loginDialog.dispose();
            CashierSystemGUI gui = new CashierSystemGUI(user);
            gui.setVisible(true);
        });

        // 退出按钮事件
        exitButton.addActionListener(e -> System.exit(0));

        // 按回车键登录
        loginDialog.getRootPane().setDefaultButton(loginButton);

        loginDialog.setVisible(true);
    }

    public CashierSystemGUI(User user) {
        this.currentUser = user;

        // 初始化数据管理器
        DataManager.initialize();

        setTitle("收银系统 - " + user.name + " (" + user.getRoleDisplayName() + ")");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1300, 800);
        setLocationRelativeTo(null);

        // 窗口关闭时自动保存
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveData();
                dispose();
                System.exit(0);
            }
        });

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(getChineseFont(Font.BOLD, 13));

        tabbedPane.addTab("库存管理", createInventoryPanel());
        tabbedPane.addTab("购物车 & 结账", createCartPanel());
        tabbedPane.addTab("交易记录", createTransactionPanel());
        tabbedPane.addTab("数据统计", createStatisticsPanel());
        tabbedPane.addTab("设置", createSettingsPanel());

        add(tabbedPane, BorderLayout.CENTER);

        // 创建菜单栏
        JMenuBar menuBar = new JMenuBar();
        
        // 系统菜单
        JMenu systemMenu = new JMenu("系统");
        systemMenu.setFont(getChineseFont(Font.PLAIN, 13));
        
        // 退出登录菜单项
        JMenuItem logoutMenuItem = new JMenuItem("退出登录");
        logoutMenuItem.setFont(getChineseFont(Font.PLAIN, 13));
        logoutMenuItem.addActionListener(e -> logout());
        systemMenu.add(logoutMenuItem);
        
        menuBar.add(systemMenu);
        
        // 管理菜单
        JMenu manageMenu = new JMenu("管理");
        manageMenu.setFont(getChineseFont(Font.PLAIN, 13));
        
        // 用户管理菜单项
        JMenuItem userManageMenuItem = new JMenuItem("用户管理");
        userManageMenuItem.setFont(getChineseFont(Font.PLAIN, 13));
        userManageMenuItem.addActionListener(e -> showUserManagementDialog());
        manageMenu.add(userManageMenuItem);
        
        // 交接班管理菜单项
        JMenuItem shiftManageMenuItem = new JMenuItem("交接班管理");
        shiftManageMenuItem.setFont(getChineseFont(Font.PLAIN, 13));
        shiftManageMenuItem.addActionListener(e -> showShiftManagementDialog());
        manageMenu.add(shiftManageMenuItem);
        
        menuBar.add(manageMenu);
        
        // 数据菜单
        JMenu dataMenu = new JMenu("数据");
        dataMenu.setFont(getChineseFont(Font.PLAIN, 13));
        
        // 备份数据菜单项
        JMenuItem backupDataMenuItem = new JMenuItem("备份数据");
        backupDataMenuItem.setFont(getChineseFont(Font.PLAIN, 13));
        backupDataMenuItem.addActionListener(e -> backupData());
        dataMenu.add(backupDataMenuItem);
        
        menuBar.add(dataMenu);
        
        setJMenuBar(menuBar);

        updateTaxRateLabel();

        // 添加键盘快捷键
        setupKeyboardShortcuts();

        // 加载数据（在窗口创建后加载，以便可以显示对话框）
        loadData();
    }

    // 检查是否有进行中的班次
    private boolean checkShiftRequired() {
        if (currentShift == null) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "您尚未开始班次，无法进行销售操作！\n\n" +
                "是否现在开始班次？",
                "未开始班次",
                JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                // 自动开始班次
                double currentRevenue = calculateTotalSales();
                int currentTransactionCount = transactions.size();
                
                String shiftId = String.format("S%06d", shifts.size() + 1);
                currentShift = new Shift(shiftId, currentUser.username, currentUser.name, new Date(), currentRevenue, currentTransactionCount);
                
                logOperation("自动开始班次", "班次ID: " + shiftId);
                JOptionPane.showMessageDialog(this, "班次开始成功！\n班次ID: " + shiftId + "\n开始时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(currentShift.startTime), "班次开始成功", JOptionPane.INFORMATION_MESSAGE);
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    // 检查是否有进行中的班次（不自动开始）
    private boolean isShiftActive() {
        return currentShift != null;
    }

    // 检查是否有进行中的班次（严格模式，不允许自动开始）
    private boolean checkShiftRequiredStrict() {
        if (currentShift == null) {
            JOptionPane.showMessageDialog(this,
                "您尚未开始班次，无法进行销售操作！\n\n" +
                "请先在\"设置\" -> \"交接班管理\"中开始班次。",
                "未开始班次",
                JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void setupKeyboardShortcuts() {
        // 输入映射
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        // F1 - 添加商品
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "addProduct");
        actionMap.put("addProduct", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAddProductDialog();
            }
        });

        // F2 - 补货
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "restock");
        actionMap.put("restock", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showRestockDialog();
            }
        });

        // F3 - 删除商品
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "deleteProduct");
        actionMap.put("deleteProduct", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedProduct();
            }
        });

        // F4 - 搜索
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "search");
        actionMap.put("search", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.requestFocus();
            }
        });

        // F5 - 刷新
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
        actionMap.put("refresh", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshInventoryTable();
            }
        });

        // F6 - 分类管理
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "categoryManage");
        actionMap.put("categoryManage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showCategoryManagementDialog();
            }
        });

        // F7 - 会员管理
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), "memberManage");
        actionMap.put("memberManage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showMemberManagementDialog(null);
            }
        });

        // F8 - 结账
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), "checkout");
        actionMap.put("checkout", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(CashierSystemGUI.this, 
                    "请在购物车面板进行结账操作！", 
                    "提示", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // Ctrl+N - 添加商品
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "ctrlAddProduct");
        actionMap.put("ctrlAddProduct", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAddProductDialog();
            }
        });

        // Ctrl+S - 保存数据
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "ctrlSave");
        actionMap.put("ctrlSave", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveData();
                JOptionPane.showMessageDialog(CashierSystemGUI.this, "数据保存成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // Ctrl+F - 搜索
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "ctrlSearch");
        actionMap.put("ctrlSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.requestFocus();
            }
        });

        // ESC - 清空搜索
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearSearch");
        actionMap.put("clearSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.setText("");
                refreshInventoryTable();
            }
        });

        // Delete - 删除选中项
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        actionMap.put("delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedProduct();
            }
        });

        // Ctrl+1 - 库存管理
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.CTRL_DOWN_MASK), "gotoInventory");
        actionMap.put("gotoInventory", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTabbedPane tabbedPane = (JTabbedPane) getContentPane().getComponent(0);
                tabbedPane.setSelectedIndex(0);
            }
        });

        // Ctrl+2 - 购物车
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.CTRL_DOWN_MASK), "gotoCart");
        actionMap.put("gotoCart", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTabbedPane tabbedPane = (JTabbedPane) getContentPane().getComponent(0);
                tabbedPane.setSelectedIndex(1);
            }
        });

        // Ctrl+3 - 交易记录
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.CTRL_DOWN_MASK), "gotoTransactions");
        actionMap.put("gotoTransactions", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTabbedPane tabbedPane = (JTabbedPane) getContentPane().getComponent(0);
                tabbedPane.setSelectedIndex(2);
            }
        });

        // Ctrl+4 - 设置
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_4, InputEvent.CTRL_DOWN_MASK), "gotoSettings");
        actionMap.put("gotoSettings", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTabbedPane tabbedPane = (JTabbedPane) getContentPane().getComponent(0);
                tabbedPane.setSelectedIndex(3);
            }
        });

        // F9 - 促销管理
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), "promotionManage");
        actionMap.put("promotionManage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPromotionManagementDialog();
            }
        });

        // F10 - 库存预警
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0), "stockWarning");
        actionMap.put("stockWarning", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showStockWarningDialog();
            }
        });

        // F11 - 数据备份
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "backupData");
        actionMap.put("backupData", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setSelectedFile(new File("backup_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())));

                int result = fileChooser.showSaveDialog(CashierSystemGUI.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    try {
                        String backupPath = fileChooser.getSelectedFile().getAbsolutePath();
                        DataManager.backupData(backupPath);
                        JOptionPane.showMessageDialog(CashierSystemGUI.this,
                            "数据备份成功！\n备份位置: " + backupPath,
                            "成功",
                            JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(CashierSystemGUI.this,
                            "数据备份失败: " + ex.getMessage(),
                            "错误",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        // F12 - 数据恢复
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), "restoreData");
        actionMap.put("restoreData", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                int result = fileChooser.showOpenDialog(CashierSystemGUI.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    try {
                        String backupPath = fileChooser.getSelectedFile().getAbsolutePath();
                        DataManager.restoreData(backupPath);
                        loadData();
                        refreshInventoryTable();
                        refreshTransactionTable(transactionTotalRevenueLabel, transactionCountLabel, transactionAvgRevenueLabel, transactionTotalTaxLabel);
                        JOptionPane.showMessageDialog(CashierSystemGUI.this,
                            "数据恢复成功！\n恢复位置: " + backupPath,
                            "成功",
                            JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(CashierSystemGUI.this,
                            "数据恢复失败: " + ex.getMessage(),
                            "错误",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        // Ctrl+D - 导出数据
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK), "exportData");
        actionMap.put("exportData", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportTransactions();
            }
        });

        // Ctrl+R - 刷新当前面板
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK), "refreshCurrent");
        actionMap.put("refreshCurrent", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTabbedPane tabbedPane = (JTabbedPane) getContentPane().getComponent(0);
                int selectedIndex = tabbedPane.getSelectedIndex();
                switch (selectedIndex) {
                    case 0: refreshInventoryTable(); break;
                    case 1: refreshCartTable(); break;
                    case 2: refreshTransactionTable(transactionTotalRevenueLabel, transactionCountLabel, transactionAvgRevenueLabel, transactionTotalTaxLabel); break;
                }
            }
        });

        // Ctrl+Q - 退出程序
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK), "exitApp");
        actionMap.put("exitApp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int confirm = JOptionPane.showConfirmDialog(CashierSystemGUI.this,
                    "确定要退出程序吗？",
                    "确认退出",
                    JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    saveData();
                    System.exit(0);
                }
            }
        });

        // Ctrl+A - 全选
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), "selectAll");
        actionMap.put("selectAll", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTabbedPane tabbedPane = (JTabbedPane) getContentPane().getComponent(0);
                int selectedIndex = tabbedPane.getSelectedIndex();
                if (selectedIndex == 0 && inventoryTable.isShowing()) {
                    inventoryTable.selectAll();
                } else if (selectedIndex == 1 && cartTable.isShowing()) {
                    cartTable.selectAll();
                } else if (selectedIndex == 2 && transactionTable.isShowing()) {
                    transactionTable.selectAll();
                }
            }
        });

        // Ctrl+E - 编辑选中项
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK), "editSelected");
        actionMap.put("editSelected", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTabbedPane tabbedPane = (JTabbedPane) getContentPane().getComponent(0);
                int selectedIndex = tabbedPane.getSelectedIndex();
                if (selectedIndex == 0 && inventoryTable.isShowing()) {
                    int row = inventoryTable.getSelectedRow();
                    if (row >= 0) {
                        String productName = (String) inventoryTableModel.getValueAt(row, 0);
                        Product product = inventory.get(productName);
                        if (product != null) {
                            showEditProductDialog(product);
                        }
                    }
                } else if (selectedIndex == 1 && cartTable.isShowing()) {
                    editCartItem();
                }
            }
        });

        // Ctrl+B - 批量操作
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK), "batchOperation");
        actionMap.put("batchOperation", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTabbedPane tabbedPane = (JTabbedPane) getContentPane().getComponent(0);
                int selectedIndex = tabbedPane.getSelectedIndex();
                if (selectedIndex == 0 && inventoryTable.isShowing()) {
                    batchRestockProducts();
                } else if (selectedIndex == 1 && cartTable.isShowing()) {
                    showBatchAddToCartDialog();
                }
            }
        });

        // Ctrl+M - 会员管理
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK), "memberManage");
        actionMap.put("memberManage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showMemberManagementDialog(null);
            }
        });

        // Ctrl+T - 交易统计
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK), "transactionStats");
        actionMap.put("transactionStats", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTabbedPane tabbedPane = (JTabbedPane) getContentPane().getComponent(0);
                tabbedPane.setSelectedIndex(2); // 跳转到交易记录页面
            }
        });

        // Enter - 确认/搜索
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enterAction");
        actionMap.put("enterAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (searchField.isFocusOwner()) {
                    searchInventory();
                }
            }
        });
    }

    private void loadData() {
        System.out.println("========== 开始加载数据 ==========");
        
        // 加载库存
        Map<String, Product> loadedInventory = DataManager.loadInventory();
        inventory.clear();
        inventory.putAll(loadedInventory);
        System.out.println("库存数据加载成功，共 " + inventory.size() + " 种商品");

        // 加载交易记录
        List<Transaction> loadedTransactions = DataManager.loadTransactions();
        transactions.clear();
        transactions.addAll(loadedTransactions);
        System.out.println("交易记录加载成功，共 " + transactions.size() + " 条记录");

        // 加载分类
        List<Category> loadedCategories = DataManager.loadCategories();
        categories.clear();
        categories.addAll(loadedCategories);
        System.out.println("分类数据加载成功，共 " + categories.size() + " 个分类");

        // 加载会员
        Map<String, Member> loadedMembers = DataManager.loadMembers();
        members.clear();
        members.putAll(loadedMembers);
        System.out.println("会员数据加载成功，共 " + members.size() + " 名会员");

        // 加载促销
        List<Promotion> loadedPromotions = DataManager.loadPromotions();
        promotions.clear();
        promotions.addAll(loadedPromotions);
        System.out.println("促销数据加载成功，共 " + promotions.size() + " 个促销");

        // 加载充值记录
        List<RechargeRecord> loadedRechargeRecords = DataManager.loadRechargeRecords();
        rechargeRecords.clear();
        rechargeRecords.addAll(loadedRechargeRecords);
        System.out.println("充值记录加载成功，共 " + rechargeRecords.size() + " 条记录");

        // 加载用户
        Map<String, User> loadedUsers = DataManager.loadUsers();
        users.clear();
        users.putAll(loadedUsers);
        System.out.println("用户数据加载成功，共 " + users.size() + " 名用户");

        // 加载操作日志
        List<OperationLog> loadedOperationLogs = DataManager.loadOperationLogs();
        operationLogs.clear();
        operationLogs.addAll(loadedOperationLogs);
        System.out.println("操作日志加载成功，共 " + operationLogs.size() + " 条记录");

        // 加载交接班记录
        List<Shift> loadedShifts = DataManager.loadShifts();
        shifts.clear();
        shifts.addAll(loadedShifts);
        System.out.println("交接班记录加载成功，共 " + shifts.size() + " 条记录");
        System.out.println("当前登录用户: " + currentUser.username);
        
        // 检查是否有未结束的班次（恢复该用户的班次）
        Shift unfinishedShift = shifts.stream()
            .filter(s -> s.username.equals(currentUser.username) && 
                        s.startTime.getTime() == s.endTime.getTime())
            .findFirst()
            .orElse(null);
        
        if (unfinishedShift != null) {
            System.out.println("找到未结束的班次: " + unfinishedShift.shiftId);
            System.out.println("startTime: " + unfinishedShift.startTime.getTime() + ", endTime: " + unfinishedShift.endTime.getTime());
            // 弹出对话框询问用户是否继续班次
            int confirm = JOptionPane.showConfirmDialog(this,
                "检测到您有未结束的班次！\n\n" +
                "班次ID: " + unfinishedShift.shiftId + "\n" +
                "开始时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(unfinishedShift.startTime) + "\n\n" +
                "是否继续该班次？",
                "未结束的班次",
                JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                currentShift = unfinishedShift;
                System.out.println("恢复用户 " + currentUser.name + " 的未结束班次: " + currentShift.shiftId);
            } else {
                currentShift = null;
                System.out.println("用户选择不恢复班次");
            }
        } else {
            System.out.println("未找到未结束的班次");
            System.out.println("所有班次信息:");
            for (Shift s : shifts) {
                System.out.println("  班次 " + s.shiftId + ": username=" + s.username + ", startTime=" + s.startTime.getTime() + ", endTime=" + s.endTime.getTime() + ", equal=" + (s.startTime.getTime() == s.endTime.getTime()));
            }
        }

        // 加载设置
        Map<String, String> settings = DataManager.loadSettings();
        if (settings.containsKey("taxRate")) {
            taxRate = Double.parseDouble(settings.get("taxRate"));
        }
        if (settings.containsKey("transactionCount")) {
            transactionCount = Integer.parseInt(settings.get("transactionCount"));
        }
        System.out.println("设置数据加载成功");
        System.out.println("========== 数据加载完成 ==========");
    }

    private void saveData() {
        // 保存库存
        DataManager.saveInventory(inventory);

        // 保存交易记录
        DataManager.saveTransactions(transactions);

        // 保存分类
        DataManager.saveCategories(categories);

        // 保存会员
        DataManager.saveMembers(members);

        // 保存促销
        DataManager.savePromotions(promotions);

        // 保存充值记录
        DataManager.saveRechargeRecords(rechargeRecords);

        // 保存用户
        DataManager.saveUsers(users);

        // 保存操作日志
        DataManager.saveOperationLogs(operationLogs);

        // 保存交接班记录
        // 如果有进行中的班次，确保它被包含在shifts列表中
        if (currentShift != null) {
            // 检查是否已经存在相同ID的班次
            boolean exists = shifts.stream().anyMatch(s -> s.shiftId.equals(currentShift.shiftId));
            if (!exists) {
                shifts.add(currentShift);
            }
        }
        DataManager.saveShifts(shifts);

        // 保存设置
        DataManager.saveSettings(taxRate, transactionCount);

        System.out.println("数据保存成功");
    }

    // 记录操作日志
    private void logOperation(String operation, String details) {
        if (currentUser == null) {
            return;
        }

        String logId = String.format("L%06d", operationLogs.size() + 1);
        OperationLog log = new OperationLog(logId, currentUser.username, operation, details);
        operationLogs.add(log);

        // 只保留最近1000条日志
        if (operationLogs.size() > 1000) {
            operationLogs = operationLogs.subList(operationLogs.size() - 1000, operationLogs.size());
        }

        saveData();
    }

    private JPanel createInventoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(SpacingConstants.PANEL_GAP, SpacingConstants.PANEL_GAP));
        panel.setBorder(SpacingConstants.getPadding(SpacingConstants.PANEL_PADDING,
                                                    SpacingConstants.PANEL_PADDING,
                                                    SpacingConstants.PANEL_PADDING,
                                                    SpacingConstants.PANEL_PADDING));

        // 顶部按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,
                                                      SpacingConstants.BUTTON_GAP,
                                                      SpacingConstants.BUTTON_GAP));

        JButton addButton = createStyledButton("添加商品", SUCCESS_COLOR);
        JButton restockButton = createStyledButton("补货", INFO_COLOR);
        JButton deleteButton = createStyledButton("删除商品", DANGER_COLOR);
        JButton categoryButton = createStyledButton("分类管理", PURPLE_COLOR);
        JButton promotionButton = createStyledButton("促销管理", INFO_COLOR);
        JButton stockWarningButton = createStyledButton("库存预警", WARNING_COLOR);

        buttonPanel.add(addButton);
        buttonPanel.add(restockButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(categoryButton);
        buttonPanel.add(promotionButton);
        buttonPanel.add(stockWarningButton);

        // 搜索和排序面板
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,
                                                       SpacingConstants.TOOLBAR_BUTTON_GAP,
                                                       SpacingConstants.TOOLBAR_BUTTON_GAP));
        searchField = new JTextField(15);
        searchField.setToolTipText("输入商品名称搜索");
        JButton searchButton = createStyledButton("搜索", PURPLE_COLOR);
        JButton clearSearchButton = createStyledButton("清除", GRAY_500);

        sortComboBox = new JComboBox<>(new String[]{"默认排序", "按名称", "按价格(低→高)", "按价格(高→低)", "按库存(多→少)"});
        sortComboBox.addActionListener(e -> sortInventory());

        searchPanel.add(new JLabel("搜索:"));
        MicroInteractions.addTextFieldEffects(searchField);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(clearSearchButton);
        searchPanel.add(new JLabel("排序:"));
        searchPanel.add(sortComboBox);

        // 顶部面板合并
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBorder(SpacingConstants.getVerticalGap(SpacingConstants.TABLE_TOOLBAR_GAP));

        topPanel.add(buttonPanel);
        topPanel.add(Box.createVerticalStrut(SpacingConstants.TABLE_TOOLBAR_GAP));
        topPanel.add(searchPanel);

        // 库存表格
        String[] columns = {"商品名称", "单价(元)", "库存", "最低库存", "分类", "预警状态"};
        inventoryTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        inventoryTable = new JTable();
        styleTable(inventoryTable, inventoryTableModel);

        // 添加右键菜单
        JPopupMenu popupMenu = new JPopupMenu();
        
        JMenuItem addMenuItem = new JMenuItem("添加商品");
        addMenuItem.addActionListener(e -> showAddProductDialog());
        popupMenu.add(addMenuItem);
        
        JMenuItem editMenuItem = new JMenuItem("补货");
        editMenuItem.addActionListener(e -> showRestockDialog());
        popupMenu.add(editMenuItem);
        
        JMenuItem deleteMenuItem = new JMenuItem("删除商品");
        deleteMenuItem.addActionListener(e -> deleteSelectedProduct());
        popupMenu.add(deleteMenuItem);
        
        popupMenu.addSeparator();
        
        JMenuItem batchDeleteMenuItem = new JMenuItem("批量删除");
        batchDeleteMenuItem.addActionListener(e -> batchDeleteProducts());
        popupMenu.add(batchDeleteMenuItem);
        
        JMenuItem batchRestockMenuItem = new JMenuItem("批量补货");
        batchRestockMenuItem.addActionListener(e -> batchRestockProducts());
        popupMenu.add(batchRestockMenuItem);
        
        popupMenu.addSeparator();
        
        JMenuItem refreshMenuItem = new JMenuItem("刷新");
        refreshMenuItem.addActionListener(e -> refreshInventoryTable());
        popupMenu.add(refreshMenuItem);
        
        JMenuItem categoryMenuItem = new JMenuItem("分类管理");
        categoryMenuItem.addActionListener(e -> showCategoryManagementDialog());
        popupMenu.add(categoryMenuItem);
        
        inventoryTable.setComponentPopupMenu(popupMenu);

        // 启用拖拽排序
        inventoryTable.setDragEnabled(true);
        inventoryTable.setDropMode(DropMode.INSERT_ROWS);
        inventoryTable.setTransferHandler(new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return COPY;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                JTable table = (JTable) c;
                int[] rows = table.getSelectedRows();
                return new StringSelection(table.getValueAt(rows[0], 0).toString());
            }

            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.stringFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                try {
                    String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    JTable table = (JTable) support.getComponent();
                    int sourceRow = Integer.parseInt(data);
                    int targetRow = table.rowAtPoint(support.getDropLocation().getDropPoint());

                    if (sourceRow >= 0 && targetRow >= 0 && sourceRow != targetRow) {
                        // 重新排序数据
                        List<Product> products = new ArrayList<>(inventory.values());
                        Product movedProduct = products.remove(sourceRow);
                        products.add(targetRow, movedProduct);

                        // 更新库存
                        inventory.clear();
                        for (Product p : products) {
                            inventory.put(p.name, p);
                        }

                        saveData();
                        refreshInventoryTable();
                        return true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });

        // 添加双击事件监听器
        inventoryTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // 双击
                    int row = inventoryTable.rowAtPoint(e.getPoint());
                    int column = inventoryTable.columnAtPoint(e.getPoint());
                    if (row >= 0 && column >= 0) {
                        // 只在双击商品名称列（第0列）时触发
                        if (column == 0) {
                            String productName = (String) inventoryTableModel.getValueAt(row, 0);
                            Product product = inventory.get(productName);
                            if (product != null) {
                                showEditProductDialog(product);
                            }
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(inventoryTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        // 库存统计标签
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel statsLabel = new JLabel("库存统计: ");
        statsPanel.add(statsLabel);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(statsPanel, BorderLayout.SOUTH);

        // 事件监听
        addButton.addActionListener(e -> showAddProductDialog());
        restockButton.addActionListener(e -> showRestockDialog());
        deleteButton.addActionListener(e -> deleteSelectedProduct());
        categoryButton.addActionListener(e -> showCategoryManagementDialog());
        promotionButton.addActionListener(e -> showPromotionManagementDialog());
        stockWarningButton.addActionListener(e -> showStockWarningDialog());
        searchButton.addActionListener(e -> searchInventory());
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            refreshInventoryTable();
        });
        searchField.addActionListener(e -> searchInventory());

        return panel;
    }

    private JPanel createCartPanel() {
        JPanel panel = new JPanel(new BorderLayout(SpacingConstants.PANEL_GAP, SpacingConstants.PANEL_GAP));
        panel.setBorder(SpacingConstants.getPadding(SpacingConstants.PANEL_PADDING + 5,
                                                    SpacingConstants.PANEL_PADDING + 5,
                                                    SpacingConstants.PANEL_PADDING + 5,
                                                    SpacingConstants.PANEL_PADDING + 5));
        panel.setBackground(new Color(240, 242, 245));

        // ========== 顶部操作区域 ==========
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setBackground(new Color(240, 242, 245));

        // 按钮工具栏
        JPanel toolbarPanel = new JPanel(new BorderLayout(SpacingConstants.TOOLBAR_BUTTON_GAP, 0));
        toolbarPanel.setBackground(new Color(255, 255, 255));
        toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            SpacingConstants.getPadding(SpacingConstants.TOOLBAR_PADDING_TOP,
                                        SpacingConstants.TOOLBAR_PADDING_LEFT,
                                        SpacingConstants.TOOLBAR_PADDING_BOTTOM,
                                        SpacingConstants.TOOLBAR_PADDING_RIGHT)
        ));

        // 左侧按钮组
        JPanel leftButtonGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        leftButtonGroup.setBackground(new Color(255, 255, 255));

        JButton addToCartButton = createStyledButton("添加商品", new Color(34, 197, 94));
        JButton batchAddButton = createStyledButton("批量添加", new Color(59, 130, 246));
        JButton repeatAddButton = createStyledButton("重复添加", new Color(139, 92, 246));
        JButton editButton = createStyledButton("编辑数量", new Color(59, 130, 246));
        JButton removeButton = createStyledButton("移除商品", new Color(239, 68, 68));
        JButton sortButton = createStyledButton("排序", new Color(245, 158, 11));
        JButton clearButton = createStyledButton("清空", new Color(107, 114, 128));
        JButton memberButton = createStyledButton("会员管理", new Color(139, 92, 246));

        leftButtonGroup.add(addToCartButton);
        leftButtonGroup.add(batchAddButton);
        leftButtonGroup.add(repeatAddButton);
        leftButtonGroup.add(editButton);
        leftButtonGroup.add(removeButton);
        leftButtonGroup.add(sortButton);
        leftButtonGroup.add(clearButton);
        leftButtonGroup.add(memberButton);

        // 右侧提示信息
        JPanel rightInfoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightInfoPanel.setBackground(new Color(255, 255, 255));
        
        JLabel shortcutHint = new JLabel("💡 快捷键: F1-添加 F2-编辑 F3-删除 Esc-清空");
        shortcutHint.setFont(getGeneralFont(Font.PLAIN, 11));
        shortcutHint.setForeground(new Color(107, 114, 128));
        
        rightInfoPanel.add(shortcutHint);

        toolbarPanel.add(leftButtonGroup, BorderLayout.WEST);
        toolbarPanel.add(rightInfoPanel, BorderLayout.EAST);

        // 快速收银面板
        JPanel quickCheckoutPanel = new JPanel(new BorderLayout(12, 0));
        quickCheckoutPanel.setBackground(new Color(255, 255, 255));
        quickCheckoutPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JLabel quickLabel = new JLabel("🔍 扫码/搜索商品:");
        quickLabel.setFont(getGeneralFont(Font.BOLD, 13));
        quickLabel.setForeground(new Color(34, 197, 94));
        
        JTextField quickInputField = new JTextField();
        quickInputField.setFont(getGeneralFont(Font.PLAIN, 14));
        quickInputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        quickInputField.setBackground(new Color(249, 250, 251));

        quickCheckoutPanel.add(quickLabel, BorderLayout.WEST);
        quickCheckoutPanel.add(quickInputField, BorderLayout.CENTER);

        // 快速收银事件监听器 - 支持扫码枪和手动输入
        quickInputField.addActionListener(e -> {
            String input = quickInputField.getText().trim();
            if (!input.isEmpty()) {
                quickAddToCart(input);
                quickInputField.setText(""); // 清空输入框
                quickInputField.requestFocus(); // 保持焦点，方便连续扫码
            }
        });

        // 支持扫码枪（通常以回车结尾）
        quickInputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String input = quickInputField.getText().trim();
                    if (!input.isEmpty()) {
                        quickAddToCart(input);
                        quickInputField.setText(""); // 清空输入框
                        quickInputField.requestFocus(); // 保持焦点，方便连续扫码
                    }
                }
            }
        });

        topContainer.add(toolbarPanel);
        topContainer.add(Box.createVerticalStrut(SpacingConstants.FORM_FIELD_GAP));
        topContainer.add(quickCheckoutPanel);

        panel.add(topContainer, BorderLayout.NORTH);

        // ========== 中间分割面板 ==========
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(850);
        splitPane.setResizeWeight(0.7);
        splitPane.setBorder(null);

        // ========== 左侧：购物车区域 ==========
        JPanel leftPanel = new JPanel(new BorderLayout(SpacingConstants.PANEL_GAP, SpacingConstants.PANEL_GAP));
        leftPanel.setBackground(new Color(240, 242, 245));

        // 购物车表格容器
        JPanel cartContainer = new JPanel(new BorderLayout(0, 0));
        cartContainer.setBackground(new Color(255, 255, 255));
        cartContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            SpacingConstants.getPadding(0, 0, 0, 0)
        ));

        // 表格标题栏
        JPanel cartHeader = new JPanel(new BorderLayout(SpacingConstants.TOOLBAR_BUTTON_GAP, 0));
        cartHeader.setBackground(PRIMARY_COLOR);
        cartHeader.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        JLabel cartTitle = new JLabel("🛒 购物车");
        cartTitle.setFont(getGeneralFont(Font.BOLD, 14));
        cartTitle.setForeground(Color.WHITE);

        JPanel cartStats = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        cartStats.setBackground(PRIMARY_COLOR);

        cartTypeCountLabel = new JLabel("0种");
        cartTypeCountLabel.setFont(getGeneralFont(Font.BOLD, 12));
        cartTypeCountLabel.setForeground(Color.WHITE);
        JLabel typeLabel = new JLabel("种商品");
        typeLabel.setFont(getGeneralFont(Font.PLAIN, 12));
        typeLabel.setForeground(GRAY_200);

        cartTotalQuantityLabel = new JLabel("0件");
        cartTotalQuantityLabel.setFont(getGeneralFont(Font.BOLD, 12));
        cartTotalQuantityLabel.setForeground(Color.WHITE);
        JLabel quantityLabel = new JLabel("总数");
        quantityLabel.setFont(getGeneralFont(Font.PLAIN, 12));
        quantityLabel.setForeground(GRAY_200);

        cartStats.add(cartTypeCountLabel);
        cartStats.add(typeLabel);
        cartStats.add(Box.createHorizontalStrut(20));
        cartStats.add(cartTotalQuantityLabel);
        cartStats.add(quantityLabel);

        cartHeader.add(cartTitle, BorderLayout.WEST);
        cartHeader.add(cartStats, BorderLayout.EAST);

        // 购物车表格
        String[] columns = {"商品名称", "单价", "数量", "小计"};
        cartTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        cartTable = new JTable();
        styleTable(cartTable, cartTableModel);
        cartTable.setRowHeight(32);

        JScrollPane scrollPane = new JScrollPane(cartTable);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);

        cartContainer.add(cartHeader, BorderLayout.NORTH);
        cartContainer.add(scrollPane, BorderLayout.CENTER);

        // 购物车底部合计
        JPanel cartFooter = new JPanel(new BorderLayout(15, 0));
        cartFooter.setBackground(new Color(255, 255, 255));
        cartFooter.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)));
        cartFooter.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JPanel footerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        footerLeft.setBackground(new Color(255, 255, 255));
        
        JLabel footerLabel = new JLabel("💰 购物车合计:");
        footerLabel.setFont(getGeneralFont(Font.BOLD, 13));
        footerLabel.setForeground(new Color(107, 114, 128));
        
        cartTotalLabel = new JLabel("¥0.00");
        cartTotalLabel.setFont(getGeneralFont(Font.BOLD, 20));
        cartTotalLabel.setForeground(new Color(239, 68, 68));

        footerLeft.add(footerLabel);
        footerLeft.add(cartTotalLabel);

        cartFooter.add(footerLeft, BorderLayout.WEST);

        leftPanel.add(cartContainer, BorderLayout.CENTER);
        leftPanel.add(cartFooter, BorderLayout.SOUTH);
        splitPane.setLeftComponent(leftPanel);

        // ========== 右侧：结账信息区域 ==========
        JPanel rightPanel = new JPanel(new BorderLayout(SpacingConstants.PANEL_GAP, SpacingConstants.PANEL_GAP));
        rightPanel.setBackground(new Color(240, 242, 245));

        // 会员信息卡片
        JPanel memberCard = new JPanel(new BorderLayout(SpacingConstants.CARD_GAP, SpacingConstants.CARD_GAP));
        memberCard.setBackground(new Color(255, 255, 255));
        memberCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            SpacingConstants.getPadding(SpacingConstants.CARD_PADDING,
                                        SpacingConstants.CARD_PADDING,
                                        SpacingConstants.CARD_PADDING,
                                        SpacingConstants.CARD_PADDING)
        ));

        JPanel memberHeader = new JPanel(new BorderLayout(0, 0));
        memberHeader.setBackground(new Color(255, 255, 255));
        
        JLabel memberTitle = new JLabel("👤 会员信息");
        memberTitle.setFont(getGeneralFont(Font.BOLD, 13));
        memberTitle.setForeground(new Color(59, 130, 246));

        memberHeader.add(memberTitle, BorderLayout.WEST);

        JPanel memberSelectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        memberSelectPanel.setBackground(new Color(255, 255, 255));

        JComboBox<String> memberComboBox = new JComboBox<>();
        memberComboBox.addItem("无会员");
        for (Member member : members.values()) {
            memberComboBox.addItem(member.phone + " - " + member.name + " (" + member.level + ")");
        }
        memberComboBox.setFont(getGeneralFont(Font.PLAIN, 12));
        memberComboBox.setBorder(BorderFactory.createLineBorder(new Color(209, 213, 219), 1));

        JButton memberManageButton = createStyledButton("管理", new Color(59, 130, 246));
        memberManageButton.setPreferredSize(new Dimension(70, 28));

        memberSelectPanel.add(memberComboBox);
        memberSelectPanel.add(memberManageButton);

        JLabel currentMemberLabel = new JLabel("当前: 无会员");
        currentMemberLabel.setFont(getGeneralFont(Font.PLAIN, 12));
        currentMemberLabel.setForeground(new Color(107, 114, 128));

        memberCard.add(memberHeader, BorderLayout.NORTH);
        memberCard.add(memberSelectPanel, BorderLayout.CENTER);
        memberCard.add(currentMemberLabel, BorderLayout.SOUTH);

        // 金额明细卡片
        JPanel amountCard = new JPanel(new BorderLayout(SpacingConstants.CARD_GAP, SpacingConstants.CARD_GAP));
        amountCard.setBackground(new Color(255, 255, 255));
        amountCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            SpacingConstants.getPadding(SpacingConstants.CARD_PADDING,
                                        SpacingConstants.CARD_PADDING,
                                        SpacingConstants.CARD_PADDING,
                                        SpacingConstants.CARD_PADDING)
        ));

        JPanel amountHeader = new JPanel(new BorderLayout(0, 0));
        amountHeader.setBackground(new Color(255, 255, 255));
        
        JLabel amountTitle = new JLabel("💵 金额明细");
        amountTitle.setFont(getGeneralFont(Font.BOLD, 13));
        amountTitle.setForeground(new Color(59, 130, 246));

        amountHeader.add(amountTitle, BorderLayout.WEST);

        JPanel amountPanel = new JPanel(new GridLayout(7, 2, 8, 10));
        amountPanel.setBackground(new Color(255, 255, 255));

        JLabel checkoutTypeCountLabel = new JLabel("商品种类");
        checkoutTypeCountLabel.setFont(getGeneralFont(Font.PLAIN, 12));
        checkoutTypeCountLabel.setForeground(new Color(107, 114, 128));
        JLabel checkoutTypeCountValue = new JLabel("0种");
        checkoutTypeCountValue.setFont(getGeneralFont(Font.BOLD, 12));
        checkoutTypeCountValue.setForeground(new Color(34, 197, 94));

        JLabel checkoutQuantityLabel = new JLabel("商品总数");
        checkoutQuantityLabel.setFont(getGeneralFont(Font.PLAIN, 12));
        checkoutQuantityLabel.setForeground(new Color(107, 114, 128));
        JLabel checkoutQuantityValue = new JLabel("0件");
        checkoutQuantityValue.setFont(getGeneralFont(Font.BOLD, 12));
        checkoutQuantityValue.setForeground(new Color(34, 197, 94));

        JLabel subtotalLabel = new JLabel("小计");
        subtotalLabel.setFont(getGeneralFont(Font.PLAIN, 12));
        subtotalLabel.setForeground(new Color(107, 114, 128));
        JLabel subtotalValue = new JLabel("¥0.00");
        subtotalValue.setFont(getGeneralFont(Font.BOLD, 12));
        subtotalValue.setForeground(new Color(59, 130, 246));

        JLabel discountLabel = new JLabel("会员折扣");
        discountLabel.setFont(getGeneralFont(Font.PLAIN, 12));
        discountLabel.setForeground(new Color(107, 114, 128));
        JLabel discountValue = new JLabel("-¥0.00");
        discountValue.setFont(getGeneralFont(Font.BOLD, 12));
        discountValue.setForeground(new Color(34, 197, 94));

        JLabel taxLabel = new JLabel("税费");
        taxLabel.setFont(getGeneralFont(Font.PLAIN, 12));
        taxLabel.setForeground(new Color(107, 114, 128));
        JLabel taxValue = new JLabel("¥0.00");
        taxValue.setFont(getGeneralFont(Font.BOLD, 12));
        taxValue.setForeground(new Color(245, 158, 11));

        JLabel totalLabel = new JLabel("应付金额");
        totalLabel.setFont(getGeneralFont(Font.BOLD, 12));
        totalLabel.setForeground(new Color(107, 114, 128));
        JLabel totalValue = new JLabel("¥0.00");
        totalValue.setFont(getGeneralFont(Font.BOLD, 16));
        totalValue.setForeground(new Color(239, 68, 68));

        JLabel changeLabel = new JLabel("找零");
        changeLabel.setFont(getGeneralFont(Font.PLAIN, 12));
        changeLabel.setForeground(new Color(107, 114, 128));
        JLabel changeValue = new JLabel("¥0.00");
        changeValue.setFont(getGeneralFont(Font.BOLD, 14));
        changeValue.setForeground(new Color(34, 197, 94));

        amountPanel.add(checkoutTypeCountLabel);
        amountPanel.add(checkoutTypeCountValue);
        amountPanel.add(checkoutQuantityLabel);
        amountPanel.add(checkoutQuantityValue);
        amountPanel.add(subtotalLabel);
        amountPanel.add(subtotalValue);
        amountPanel.add(discountLabel);
        amountPanel.add(discountValue);
        amountPanel.add(taxLabel);
        amountPanel.add(taxValue);
        amountPanel.add(totalLabel);
        amountPanel.add(totalValue);
        amountPanel.add(changeLabel);
        amountPanel.add(changeValue);

        amountCard.add(amountHeader, BorderLayout.NORTH);
        amountCard.add(amountPanel, BorderLayout.CENTER);

        // 收款卡片
        JPanel paymentCard = new JPanel(new BorderLayout(SpacingConstants.CARD_GAP, SpacingConstants.CARD_GAP));
        paymentCard.setBackground(new Color(255, 255, 255));
        paymentCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            SpacingConstants.getPadding(SpacingConstants.CARD_PADDING,
                                        SpacingConstants.CARD_PADDING,
                                        SpacingConstants.CARD_PADDING,
                                        SpacingConstants.CARD_PADDING)
        ));

        JPanel paymentHeader = new JPanel(new BorderLayout(0, 0));
        paymentHeader.setBackground(new Color(255, 255, 255));
        
        JLabel paymentTitle = new JLabel("💳 收款");
        paymentTitle.setFont(getGeneralFont(Font.BOLD, 13));
        paymentTitle.setForeground(new Color(59, 130, 246));

        paymentHeader.add(paymentTitle, BorderLayout.WEST);

        JPanel paymentInputPanel = new JPanel(new GridBagLayout());
        paymentInputPanel.setBackground(new Color(255, 255, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel paymentMethodLabel = new JLabel("支付方式:");
        paymentMethodLabel.setFont(getGeneralFont(Font.PLAIN, 12));
        paymentMethodLabel.setForeground(new Color(107, 114, 128));
        paymentInputPanel.add(paymentMethodLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JComboBox<String> paymentMethodComboBox = new JComboBox<>(new String[]{"现金", "微信支付", "支付宝", "银行卡", "组合支付"});
        paymentMethodComboBox.setFont(getGeneralFont(Font.PLAIN, 12));
        paymentMethodComboBox.setBorder(BorderFactory.createLineBorder(new Color(209, 213, 219), 1));
        paymentInputPanel.add(paymentMethodComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        JLabel receivedLabel = new JLabel("实收金额:");
        receivedLabel.setFont(getGeneralFont(Font.PLAIN, 12));
        receivedLabel.setForeground(new Color(107, 114, 128));
        paymentInputPanel.add(receivedLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField receivedField = new JTextField();
        receivedField.setFont(getGeneralFont(Font.PLAIN, 13));
        receivedField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        receivedField.setBackground(new Color(249, 250, 251));
        paymentInputPanel.add(receivedField, gbc);

        // 按钮面板
        JPanel paymentButtonPanel = new JPanel(new GridLayout(1, 3, 8, 0));
        paymentButtonPanel.setBackground(new Color(255, 255, 255));

        JButton checkoutButton = createStyledButton("确认结账", new Color(34, 197, 94));
        JButton refreshButton = createStyledButton("刷新", new Color(59, 130, 246));
        JButton changeDetailsButton = createStyledButton("找零详情", new Color(245, 158, 11));

        paymentButtonPanel.add(checkoutButton);
        paymentButtonPanel.add(refreshButton);
        paymentButtonPanel.add(changeDetailsButton);

        paymentCard.add(paymentHeader, BorderLayout.NORTH);
        paymentCard.add(paymentInputPanel, BorderLayout.CENTER);
        paymentCard.add(paymentButtonPanel, BorderLayout.SOUTH);

        // 右侧面板布局
        JPanel rightContainer = new JPanel();
        rightContainer.setLayout(new BoxLayout(rightContainer, BoxLayout.Y_AXIS));
        rightContainer.setBackground(new Color(240, 242, 245));

        rightContainer.add(memberCard);
        rightContainer.add(Box.createVerticalStrut(15));
        rightContainer.add(amountCard);
        rightContainer.add(Box.createVerticalStrut(15));
        rightContainer.add(paymentCard);

        rightPanel.add(rightContainer, BorderLayout.CENTER);
        splitPane.setRightComponent(rightPanel);

        panel.add(splitPane, BorderLayout.CENTER);

        // 事件监听
        addToCartButton.addActionListener(e -> showAddToCartDialog());
        batchAddButton.addActionListener(e -> showBatchAddToCartDialog());
        repeatAddButton.addActionListener(e -> repeatAddToCart());
        editButton.addActionListener(e -> editCartItem());
        removeButton.addActionListener(e -> removeCartItem());
        sortButton.addActionListener(e -> showSortCartDialog());
        clearButton.addActionListener(e -> clearCart());
        memberButton.addActionListener(e -> showMemberManagementDialog(memberComboBox));

        // 结账相关事件
        refreshButton.addActionListener(e -> {
            updateCartCheckoutInfo(checkoutTypeCountLabel, checkoutQuantityLabel, subtotalLabel, discountLabel, taxLabel, totalLabel, currentMemberLabel);
            changeLabel.setText("找零: ¥0.00");
            receivedField.setText("");
        });

        changeDetailsButton.addActionListener(e -> {
            try {
                double received = Double.parseDouble(receivedField.getText());
                // 计算应付金额
                double subtotal = 0;
                for (Product product : cart) {
                    subtotal += product.price * product.quantity;
                }
                double memberDiscountAmount = 0;
                if (currentMember != null) {
                    memberDiscountAmount = subtotal * (1 - currentMember.discount);
                }
                double afterMemberDiscount = subtotal - memberDiscountAmount;
                double promotionDiscountAmount = 0;
                if (currentPromotion != null) {
                    double promotionBaseAmount = ("满减".equals(currentPromotion.type)) ? subtotal : afterMemberDiscount;
                    promotionDiscountAmount = currentPromotion.calculateDiscount(promotionBaseAmount);
                }
                double discountAmount = memberDiscountAmount + promotionDiscountAmount;
                double total = subtotal - discountAmount;
                
                double change = received - total;
                if (change > 0) {
                    showChangeDetails(change);
                } else {
                    JOptionPane.showMessageDialog(this, "实收金额不足，无法计算找零！", "提示", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "请先输入实收金额！", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        checkoutButton.addActionListener(e -> {
            if (cart.isEmpty()) {
                JOptionPane.showMessageDialog(this, "购物车为空，无法结账！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            String paymentMethod = (String) paymentMethodComboBox.getSelectedItem();
            
            if ("组合支付".equals(paymentMethod)) {
                // 组合支付
                showCombinedPaymentDialog(totalLabel, changeLabel, receivedField, currentMemberLabel, memberComboBox);
            } else if ("现金".equals(paymentMethod)) {
                // 现金支付：需要输入实收金额
                try {
                    double received = Double.parseDouble(receivedField.getText());
                    performCheckout(checkoutTypeCountLabel, checkoutQuantityLabel, subtotalLabel, discountLabel, taxLabel, totalLabel, received, changeLabel, receivedField, currentMemberLabel, paymentMethod, memberComboBox);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "请输入有效的金额！", "错误", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // 微信支付、支付宝、银行卡：自动使用应付金额作为实收金额
                try {
                    // 从totalLabel获取应付金额并填充到receivedField
                    String totalText = totalLabel.getText().replace("应付金额: ¥", "");
                    double received = Double.parseDouble(totalText);
                    receivedField.setText(totalText);
                    performCheckout(checkoutTypeCountLabel, checkoutQuantityLabel, subtotalLabel, discountLabel, taxLabel, totalLabel, received, changeLabel, receivedField, currentMemberLabel, paymentMethod, memberComboBox);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "无法获取应付金额！", "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // 会员选择事件
        memberComboBox.addActionListener(e -> {
            int selectedIndex = memberComboBox.getSelectedIndex();
            if (selectedIndex == 0) {
                currentMember = null;
            } else {
                // 获取选中的会员
                Object selectedItem = memberComboBox.getSelectedItem();
                if (selectedItem != null) {
                    String selectedInfo = selectedItem.toString();
                    String phone = selectedInfo.split(" - ")[0];
                    currentMember = members.get(phone);
                } else {
                    currentMember = null;
                }
            }
            updateCartCheckoutInfo(checkoutTypeCountLabel, checkoutQuantityLabel, subtotalLabel, discountLabel, taxLabel, totalLabel, currentMemberLabel);
        });

        // 会员管理按钮事件
        memberManageButton.addActionListener(e -> showMemberManagementDialog(memberComboBox));

        // 初始化显示
        updateCartCheckoutInfo(checkoutTypeCountLabel, checkoutQuantityLabel, subtotalLabel, discountLabel, taxLabel, totalLabel, currentMemberLabel);

        return panel;
    }

    private void updateCartCheckoutInfo(JLabel checkoutTypeCountLabel, JLabel checkoutQuantityLabel, JLabel subtotalLabel, JLabel discountLabel, JLabel taxLabel, JLabel totalLabel, JLabel currentMemberLabel) {
        double subtotal = 0;
        int totalQuantity = 0;
        for (Product product : cart) {
            subtotal += product.price * product.quantity;
            totalQuantity += product.quantity;
        }

        // 计算会员折扣
        double discountAmount = 0;
        if (currentMember != null) {
            discountAmount = subtotal * (1 - currentMember.discount);
        }

        double discountedSubtotal = subtotal - discountAmount;
        double tax = discountedSubtotal * taxRate;
        double total = discountedSubtotal; // 应付金额 = 折扣后小计，税费由商家承担

        checkoutTypeCountLabel.setText("商品种类: " + cart.size() + "种");
        checkoutQuantityLabel.setText("商品总数: " + totalQuantity + "件");
        subtotalLabel.setText("小计: ¥" + String.format("%.2f", subtotal));
        discountLabel.setText("会员折扣: -¥" + String.format("%.2f", discountAmount) + (currentMember != null ? " (" + currentMember.level + " " + (currentMember.discount * 10) + "折)" : ""));
        taxLabel.setText("商家税费: ¥" + String.format("%.2f", tax) + " (商家承担)");
        totalLabel.setText("应付金额: ¥" + String.format("%.2f", total));

        // 更新当前会员标签
        if (currentMember != null) {
            currentMemberLabel.setText("当前: " + currentMember.name + " (" + currentMember.level + ") - 积分: " + String.format("%.0f", currentMember.points));
        } else {
            currentMemberLabel.setText("当前: 无会员");
        }
    }

    private void performCheckout(JLabel checkoutTypeCountLabel, JLabel checkoutQuantityLabel, JLabel subtotalLabel, JLabel discountLabel, JLabel taxLabel, JLabel totalLabel, double received, JLabel changeLabel, JTextField receivedField, JLabel currentMemberLabel, String paymentMethod, JComboBox<String> memberComboBox) {
        // 检查是否有进行中的班次
        if (!checkShiftRequiredStrict()) {
            return;
        }
        
        double subtotal = 0;
        for (Product product : cart) {
            subtotal += product.price * product.quantity;
        }

        // 计算会员折扣
        double memberDiscountAmount = 0;
        if (currentMember != null) {
            memberDiscountAmount = subtotal * (1 - currentMember.discount);
        }

        double afterMemberDiscount = subtotal - memberDiscountAmount;

        // 计算促销折扣（满减基于原始小计，打折基于会员折扣后金额）
        double promotionDiscountAmount = 0;
        String promotionInfo = "";
        if (currentPromotion != null) {
            // 满减基于原始小计计算，打折基于会员折扣后金额计算
            double promotionBaseAmount = ("满减".equals(currentPromotion.type)) ? subtotal : afterMemberDiscount;
            promotionDiscountAmount = currentPromotion.calculateDiscount(promotionBaseAmount);
            if (promotionDiscountAmount > 0) {
                currentPromotion.incrementUsage();
                String promotionType = "满减".equals(currentPromotion.type) ? "满减" : "打折";
                promotionInfo = "\n促销优惠: " + currentPromotion.name + " (" + promotionType + ") -¥" + String.format("%.2f", promotionDiscountAmount);
            }
        }

        double discountAmount = memberDiscountAmount + promotionDiscountAmount;
        double discountedSubtotal = subtotal - discountAmount;
        double tax = discountedSubtotal * taxRate;
        double total = discountedSubtotal; // 应付金额 = 折扣后小计，税费由商家承担

        // 对于微信支付、支付宝、银行卡，实收金额等于应付金额
        if ("微信支付".equals(paymentMethod) || "支付宝".equals(paymentMethod) || "银行卡".equals(paymentMethod)) {
            received = total;
        }

        if (received < total) {
            JOptionPane.showMessageDialog(this, "收款金额不足！还需: ¥" + String.format("%.2f", total - received), "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double change = received - total;
        changeLabel.setText("找零: ¥" + String.format("%.2f", change));

        // 更新库存
        for (Product cartProduct : cart) {
            Product inventoryProduct = inventory.get(cartProduct.name);
            if (inventoryProduct != null) {
                inventoryProduct.quantity -= cartProduct.quantity;
            }
        }

        // 记录交易
        transactionCount++;
        String transactionId = String.format("T%06d", transactionCount);
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        List<Product> itemsCopy = new ArrayList<>();
        for (Product p : cart) {
            itemsCopy.add(new Product(p.name, p.price, p.quantity));
        }

        Transaction transaction = new Transaction(transactionId, timestamp, itemsCopy, discountedSubtotal, tax, total, paymentMethod);
        transactions.add(transaction);

        // 更新会员积分
        if (currentMember != null) {
            currentMember.points += discountedSubtotal;  // 消费1元=1积分
            currentMember.updateLevel();  // 更新会员等级
            saveData();
        }

        saveData(); // 自动保存

        cart.clear();
        currentMember = null;  // 清空会员信息
        memberComboBox.setSelectedIndex(0);  // 重置会员选择下拉框到第一个选项
        refreshCartTable();
        refreshInventoryTable();
        updateCartCheckoutInfo(checkoutTypeCountLabel, checkoutQuantityLabel, subtotalLabel, discountLabel, taxLabel, totalLabel, currentMemberLabel);
        refreshTransactionTable(transactionTotalRevenueLabel, transactionCountLabel, transactionAvgRevenueLabel, transactionTotalTaxLabel);
        changeLabel.setText("找零: ¥0.00");
        receivedField.setText("");

        // 如果有会员，显示积分增加信息
        String memberInfo = "";
        if (currentMember != null) {
            memberInfo = "\n会员积分增加: " + String.format("%.0f", discountedSubtotal) + "\n当前积分: " + String.format("%.0f", currentMember.points) + "\n当前等级: " + currentMember.level;
        }

        // 支付方式信息
        String paymentMethodInfo = "\n支付方式: " + paymentMethod;

        // 显示交易成功对话框，包含打印小票选项
        showTransactionSuccessDialog(transactionId, total, received, change, tax, memberInfo, promotionInfo, itemsCopy, discountedSubtotal, discountAmount, paymentMethod);
    }

    private void showTransactionSuccessDialog(String transactionId, double total, double received, double change, double tax, String memberInfo, String promotionInfo, List<Product> items, double discountedSubtotal, double discountAmount, String paymentMethod) {
        JDialog successDialog = new JDialog(this, "交易成功", true);
        successDialog.setSize(400, 350);
        successDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(SpacingConstants.DIALOG_CONTENT_BUTTON_GAP,
                                                    SpacingConstants.DIALOG_CONTENT_BUTTON_GAP));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 交易信息
        JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setFont(getGeneralFont(Font.PLAIN, 13));
        String discountText = discountAmount > 0 ? "\n总优惠金额: ¥" + String.format("%.2f", discountAmount) : "";
        infoArea.setText(
            "交易成功！\n\n" +
            "应付金额: ¥" + String.format("%.2f", total) + "\n" +
            "实收金额: ¥" + String.format("%.2f", received) + "\n" +
            "找零金额: ¥" + String.format("%.2f", change) + "\n" +
            "支付方式: " + paymentMethod + "\n" +
            "商家税费: ¥" + String.format("%.2f", tax) + " (商家承担)\n" +
            "交易ID: " + transactionId +
            memberInfo +
            promotionInfo +
            discountText
        );

        JScrollPane scrollPane = new JScrollPane(infoArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton printButton = createStyledButton("打印小票", SUCCESS_COLOR);
        JButton previewButton = createStyledButton("预览小票", INFO_COLOR);
        JButton closeButton = createStyledButton("关闭", GRAY_500);

        buttonPanel.add(printButton);
        buttonPanel.add(previewButton);
        buttonPanel.add(closeButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        successDialog.add(panel);

        // 打印小票
        printButton.addActionListener(e -> {
            printReceipt(transactionId, total, received, change, tax, memberInfo, promotionInfo, items, discountedSubtotal, discountAmount, successDialog);
        });

        // 预览小票
        previewButton.addActionListener(e -> {
            showReceiptPreview(transactionId, total, received, change, tax, memberInfo, promotionInfo, items, discountedSubtotal, discountAmount);
        });

        // 关闭对话框
        closeButton.addActionListener(e -> successDialog.dispose());

        successDialog.setVisible(true);
    }

    private String generateReceiptContent(String transactionId, double total, double received, double change, double tax, String memberInfo, String promotionInfo, List<Product> items, double discountedSubtotal, double discountAmount) {
        StringBuilder receipt = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        receipt.append("========================================\n");
        receipt.append("            收银系统小票\n");
        receipt.append("========================================\n");
        receipt.append("交易时间: ").append(sdf.format(new Date())).append("\n");
        receipt.append("交易编号: ").append(transactionId).append("\n");
        receipt.append("----------------------------------------\n");

        // 商品清单
        receipt.append("商品名称      单价    数量   小计\n");
        receipt.append("----------------------------------------\n");
        for (Product product : items) {
            double subtotal = product.price * product.quantity;
            receipt.append(String.format("%-12s %6.2f %4d %8.2f\n",
                product.name, product.price, product.quantity, subtotal));
        }
        receipt.append("----------------------------------------\n");

        // 计算原始小计
        double originalSubtotal = 0;
        for (Product product : items) {
            originalSubtotal += product.price * product.quantity;
        }

        // 显示折扣信息
        receipt.append(String.format("原始小计: %31.2f\n", originalSubtotal));
        if (discountAmount > 0) {
            receipt.append(String.format("总优惠: %34.2f\n", discountAmount));

            // 显示会员折扣
            if (currentMember != null) {
                double memberDiscount = originalSubtotal * (1 - currentMember.discount);
                receipt.append(String.format("  会员折扣: %28.2f\n", memberDiscount));
            }

            // 显示促销折扣
            if (currentPromotion != null) {
                double promoDiscount = currentPromotion.calculateDiscount(originalSubtotal - (currentMember != null ? originalSubtotal * (1 - currentMember.discount) : 0));
                if (promoDiscount > 0) {
                    receipt.append(String.format("  促销折扣: %28.2f\n", promoDiscount));
                    receipt.append(String.format("    (%s)\n", currentPromotion.name));
                }
            }
        }

        receipt.append("========================================\n");
        receipt.append(String.format("应付金额: %31.2f\n", total));
        receipt.append(String.format("实收金额: %31.2f\n", received));
        receipt.append(String.format("找零金额: %31.2f\n", change));
        receipt.append(String.format("税费: %33.2f (商家承担)\n", tax));
        receipt.append("========================================\n");

        // 会员信息
        if (currentMember != null) {
            receipt.append("会员信息:\n");
            receipt.append("  会员: ").append(currentMember.name).append("\n");
            receipt.append("  等级: ").append(currentMember.level).append("\n");
            receipt.append("  积分: ").append(String.format("%.0f", currentMember.points)).append("\n");
            receipt.append("========================================\n");
        }

        receipt.append("            谢谢惠顾！\n");
        receipt.append("========================================\n");

        return receipt.toString();
    }

    private void printReceipt(String transactionId, double total, double received, double change, double tax, String memberInfo, String promotionInfo, List<Product> items, double discountedSubtotal, double discountAmount, JDialog previewDialog) {
        String receiptContent = generateReceiptContent(transactionId, total, received, change, tax, memberInfo, promotionInfo, items, discountedSubtotal, discountAmount);

        // 选择打印机
        java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
        job.setJobName("小票 - " + transactionId);

        if (job.printDialog()) {
            try {
                // 创建可打印对象
                java.awt.print.PageFormat pf = job.defaultPage();
                pf.setOrientation(java.awt.print.PageFormat.PORTRAIT);

                job.setPrintable(new java.awt.print.Printable() {
                    @Override
                    public int print(java.awt.Graphics graphics, java.awt.print.PageFormat pageFormat, int pageIndex) throws java.awt.print.PrinterException {
                        if (pageIndex > 0) {
                            return java.awt.print.Printable.NO_SUCH_PAGE;
                        }

                        java.awt.Graphics2D g2d = (java.awt.Graphics2D) graphics;
                        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                        // 设置字体
                        g2d.setFont(getChineseFont(Font.PLAIN, 10));

                        // 绘制小票内容
                        String[] lines = receiptContent.split("\n");
                        int y = 15;
                        for (String line : lines) {
                            g2d.drawString(line, 10, y);
                            y += 15;
                        }

                        return java.awt.print.Printable.PAGE_EXISTS;
                    }
                }, pf);

                job.print();
                JOptionPane.showMessageDialog(this, "小票打印成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                // 打印成功后自动关闭预览对话框
                if (previewDialog != null) {
                    previewDialog.dispose();
                }
            } catch (java.awt.print.PrinterException ex) {
                JOptionPane.showMessageDialog(this, "打印失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showReceiptPreview(String transactionId, double total, double received, double change, double tax, String memberInfo, String promotionInfo, List<Product> items, double discountedSubtotal, double discountAmount) {
        JDialog previewDialog = new JDialog(this, "小票预览 - " + transactionId, true);
        previewDialog.setSize(400, 500);
        previewDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 小票内容
        JTextArea receiptArea = new JTextArea();
        receiptArea.setEditable(false);
        receiptArea.setFont(getChineseFont(Font.PLAIN, 12));
        receiptArea.setText(generateReceiptContent(transactionId, total, received, change, tax, memberInfo, promotionInfo, items, discountedSubtotal, discountAmount));

        JScrollPane scrollPane = new JScrollPane(receiptArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton printButton = createStyledButton("打印", SUCCESS_COLOR);
        JButton saveButton = createStyledButton("保存", INFO_COLOR);
        JButton closeButton = createStyledButton("关闭", GRAY_500);

        buttonPanel.add(printButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(closeButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        previewDialog.add(panel);

        // 打印
        printButton.addActionListener(e -> {
            printReceipt(transactionId, total, received, change, tax, memberInfo, promotionInfo, items, discountedSubtotal, discountAmount, previewDialog);
        });

        // 保存小票
        saveButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("保存小票");
            fileChooser.setSelectedFile(new File("小票_" + transactionId + ".txt"));

            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                try {
                    String receiptContent = generateReceiptContent(transactionId, total, received, change, tax, memberInfo, promotionInfo, items, discountedSubtotal, discountAmount);
                    java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(fileChooser.getSelectedFile()));
                    writer.print(receiptContent);
                    writer.close();
                    JOptionPane.showMessageDialog(this, "小票保存成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "保存失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // 关闭
        closeButton.addActionListener(e -> previewDialog.dispose());

        previewDialog.setVisible(true);
    }

    private JPanel createTransactionPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(BACKGROUND_COLOR);

        // 顶部按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        JButton viewDetailButton = createStyledButton("👁️ 查看详情", new Color(52, 152, 219));
        JButton exportButton = createStyledButton("导出记录", new Color(155, 89, 182));
        JButton clearButton = createStyledButton("清空记录", new Color(231, 76, 60));

        buttonPanel.add(viewDetailButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(clearButton);

        // 交易记录表格
        String[] columns = {"交易ID", "时间", "商品数量", "小计(元)", "商家税费(元)", "实收金额(元)"};
        transactionTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        transactionTable = new JTable();
        styleTable(transactionTable, transactionTableModel);

        JScrollPane scrollPane = new JScrollPane(transactionTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        // 统计信息面板
JPanel statsPanel = new JPanel(new GridLayout(1, 4, 10, 5));
        statsPanel.setBorder(BorderFactory.createTitledBorder("交易统计"));

        transactionTotalRevenueLabel = new JLabel("总销售额: ¥0.00");
        transactionTotalRevenueLabel.setFont(getChineseFont(Font.BOLD, 12));
        transactionTotalRevenueLabel.setForeground(SUCCESS_COLOR);

        transactionCountLabel = new JLabel("交易次数: 0次");
        transactionCountLabel.setFont(getChineseFont(Font.BOLD, 12));
        transactionCountLabel.setForeground(INFO_COLOR);

        transactionAvgRevenueLabel = new JLabel("平均交易额: ¥0.00");
        transactionAvgRevenueLabel.setFont(getChineseFont(Font.BOLD, 12));
        transactionAvgRevenueLabel.setForeground(PURPLE_COLOR);

        transactionTotalTaxLabel = new JLabel("总税费: ¥0.00");

        statsPanel.add(transactionTotalRevenueLabel);
        statsPanel.add(transactionCountLabel);
        statsPanel.add(transactionAvgRevenueLabel);
        statsPanel.add(transactionTotalTaxLabel);

        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(statsPanel, BorderLayout.SOUTH);

        // 事件监听
        viewDetailButton.addActionListener(e -> {
            int selectedRow = transactionTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "请先选择一条交易记录！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            showTransactionDetail(selectedRow);
        });

        exportButton.addActionListener(e -> {
            if (transactions.isEmpty()) {
                JOptionPane.showMessageDialog(this, "暂无交易记录！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            exportTransactions();
        });

        clearButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "确定要清空所有交易记录吗？", "确认", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                transactions.clear();
                saveData(); // 自动保存
                refreshTransactionTable(transactionTotalRevenueLabel, transactionCountLabel, transactionAvgRevenueLabel, transactionTotalTaxLabel);
            }
        });

        refreshTransactionTable(transactionTotalRevenueLabel, transactionCountLabel, transactionAvgRevenueLabel, transactionTotalTaxLabel);

        return panel;
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        panel.setBackground(BACKGROUND_COLOR);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 税率设置
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel taxTitleLabel = new JLabel("税率设置:");
        taxTitleLabel.setFont(getChineseFont(Font.BOLD, 14));
        taxTitleLabel.setForeground(PRIMARY_COLOR);
        panel.add(taxTitleLabel, gbc);

        gbc.gridx = 1;
        taxRateLabel = new JLabel("当前税率: 0%");
        taxRateLabel.setFont(getChineseFont(Font.BOLD, 14));
        taxRateLabel.setForeground(SUCCESS_COLOR);
        panel.add(taxRateLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel newTaxLabel = new JLabel("设置新税率 (%):");
        newTaxLabel.setFont(getChineseFont(Font.BOLD, 13));
        newTaxLabel.setForeground(TEXT_COLOR);
        panel.add(newTaxLabel, gbc);

        gbc.gridx = 1;
        JTextField taxRateField = new JTextField(15);
        taxRateField.setFont(getChineseFont(Font.PLAIN, 13));
        taxRateField.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        panel.add(taxRateField, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        JButton setTaxButton = createStyledButton("保存设置", SUCCESS_COLOR);
        panel.add(setTaxButton, gbc);

        // 主题设置
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;

        JLabel themeTitleLabel = new JLabel("主题设置:");
        themeTitleLabel.setFont(getChineseFont(Font.BOLD, 14));
        themeTitleLabel.setForeground(PRIMARY_COLOR);
        panel.add(themeTitleLabel, gbc);

        gbc.gridx = 1;
        JComboBox<String> themeComboBox = new JComboBox<>(new String[]{
            "浅色主题 (Light)",
            "深色主题 (Dark)",
            "IntelliJ主题"
        });
        themeComboBox.setFont(getChineseFont(Font.PLAIN, 13));
        panel.add(themeComboBox, gbc);

        gbc.gridx = 1;
        gbc.gridy = 4;
        JButton applyThemeButton = createStyledButton("应用主题", INFO_COLOR);
        panel.add(applyThemeButton, gbc);

        // 备份和恢复按钮
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;

        JPanel backupRestorePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JButton backupButton = createStyledButton("备份数据", INFO_COLOR);
        JButton restoreButton = createStyledButton("恢复数据", PURPLE_COLOR);
        JLabel autoSaveLabel = new JLabel("自动保存: 已启用");
        autoSaveLabel.setFont(getChineseFont(Font.BOLD, 12));
        autoSaveLabel.setForeground(SUCCESS_COLOR);

        backupRestorePanel.add(backupButton);
        backupRestorePanel.add(restoreButton);
        backupRestorePanel.add(autoSaveLabel);

        panel.add(backupRestorePanel, gbc);

        // 交接班和用户管理按钮
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;

        JPanel managementPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JButton shiftButton = createStyledButton("交接班管理", new Color(241, 196, 15));
        JButton userManageButton = createStyledButton("用户管理", new Color(52, 152, 219));
        JButton logoutButton = createStyledButton("退出登录", DANGER_COLOR);

        managementPanel.add(shiftButton);
        managementPanel.add(userManageButton);
        managementPanel.add(logoutButton);

        panel.add(managementPanel, gbc);

        // 说明文本
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        JTextArea infoText = new JTextArea(
            "使用说明:\n" +
            "1. 在标签页之间切换以使用不同功能\n" +
            "2. 库存管理: 添加、补货、删除、搜索和排序商品\n" +
            "3. 购物车: 添加商品、编辑数量、移除商品\n" +
            "4. 结账: 查看清单、输入收款金额、确认结账\n" +
            "5. 交易记录: 查看历史交易记录、查看详情、导出记录\n" +
            "6. 设置: 配置税率、主题切换、数据备份和恢复\n\n" +
            "提示: 税费由商家承担，不计入消费者应付金额"
        );
        infoText.setEditable(false);
        infoText.setFont(getChineseFont(Font.PLAIN, 12));
        infoText.setBorder(BorderFactory.createTitledBorder("使用说明"));
        panel.add(infoText, gbc);

        // 事件监听
        shiftButton.addActionListener(e -> showShiftManagementDialog());
        userManageButton.addActionListener(e -> showUserManagementDialog());
        logoutButton.addActionListener(e -> logout());

        setTaxButton.addActionListener(e -> {
            try {
                double newRate = Double.parseDouble(taxRateField.getText());
                if (newRate < 0) {
                    JOptionPane.showMessageDialog(this, "税率不能为负数！", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                taxRate = newRate / 100.0;
                updateTaxRateLabel();
                saveData(); // 自动保存
                JOptionPane.showMessageDialog(this, "税率设置成功！当前税率: " + String.format("%.1f%%", taxRate * 100), "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "请输入有效的数字！", "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        applyThemeButton.addActionListener(e -> {
            int selectedTheme = themeComboBox.getSelectedIndex();
            try {
                switch (selectedTheme) {
                    case 0:
                        FlatLightLaf.setup();
                        break;
                    case 1:
                        FlatDarkLaf.setup();
                        break;
                    case 2:
                        FlatIntelliJLaf.setup();
                        break;
                }
                SwingUtilities.updateComponentTreeUI(this);
                JOptionPane.showMessageDialog(this, "主题切换成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "主题切换失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        backupButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("选择备份位置");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setSelectedFile(new File("backup_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())));

            int userSelection = fileChooser.showOpenDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                String backupPath = fileChooser.getSelectedFile().getAbsolutePath();
                try {
                    DataManager.backupData(backupPath);
                    JOptionPane.showMessageDialog(this,
                        "数据备份成功！\n备份位置: " + backupPath + "\n" +
                        "备份文件:\n" +
                        "  - " + backupPath + "/inventory.txt\n" +
                        "  - " + backupPath + "/transactions.txt\n" +
                        "  - " + backupPath + "/settings.txt",
                        "备份成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "备份失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });

        restoreButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "恢复数据将覆盖当前数据，确定要继续吗？\n\n" +
                "请选择之前备份的目录，该目录应包含以下文件：\n" +
                "  - inventory.txt\n" +
                "  - transactions.txt\n" +
                "  - settings.txt\n\n" +
                "建议先备份当前数据！",
                "确认恢复", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("选择备份目录");
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setAcceptAllFileFilterUsed(false);

                int userSelection = fileChooser.showOpenDialog(this);
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    String backupPath = selectedFile.getAbsolutePath();
                    
                    System.out.println("选择的备份路径: " + backupPath);
                    
                    // 检查备份目录是否存在
                    File backupDir = new File(backupPath);
                    if (!backupDir.exists()) {
                        JOptionPane.showMessageDialog(this, "备份目录不存在！", "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    // 检查是否是目录
                    if (!backupDir.isDirectory()) {
                        JOptionPane.showMessageDialog(this, "请选择备份目录，不是文件！", "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    // 检查备份文件是否存在
                    File inventoryBackup = new File(backupPath + "/inventory.txt");
                    File transactionsBackup = new File(backupPath + "/transactions.txt");
                    File settingsBackup = new File(backupPath + "/settings.txt");
                    
                    System.out.println("检查备份文件:");
                    System.out.println("  inventory.txt: " + inventoryBackup.exists());
                    System.out.println("  transactions.txt: " + transactionsBackup.exists());
                    System.out.println("  settings.txt: " + settingsBackup.exists());
                    
                    if (!inventoryBackup.exists() || !transactionsBackup.exists() || !settingsBackup.exists()) {
                        String missingFiles = "";
                        if (!inventoryBackup.exists()) missingFiles += "inventory.txt ";
                        if (!transactionsBackup.exists()) missingFiles += "transactions.txt ";
                        if (!settingsBackup.exists()) missingFiles += "settings.txt ";
                        JOptionPane.showMessageDialog(this, 
                            "备份文件不完整！\n缺少以下文件:\n  - " + missingFiles + "\n\n" +
                            "请确保选择了正确的备份目录。", 
                            "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    try {
                        System.out.println("开始恢复数据...");
                        DataManager.restoreData(backupPath);
                        System.out.println("数据恢复完成，开始加载...");

                        // 重新加载数据
                        loadData();
                        refreshInventoryTable();
                        refreshTransactionTable(transactionTotalRevenueLabel, transactionCountLabel, transactionAvgRevenueLabel, transactionTotalTaxLabel);
                        updateTaxRateLabel();

                        JOptionPane.showMessageDialog(this,
                            "数据恢复成功！\n\n" +
                            "恢复位置: " + backupPath + "\n" +
                            "库存: " + inventory.size() + " 种商品\n" +
                            "交易记录: " + transactions.size() + " 条\n" +
                            "税率: " + String.format("%.1f%%", taxRate * 100),
                            "恢复成功", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, 
                            "恢复失败：" + ex.getMessage() + "\n\n" +
                            "请检查备份文件是否完整且格式正确。", 
                            "错误", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                }
            }
        });

        return panel;
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(getChineseFont(Font.PLAIN, 13));
        button.setPreferredSize(new Dimension(120, 32));

        // 设置背景色和前景色，确保按钮可见
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);

        // 添加微交互效果
        MicroInteractions.addButtonEffects(button);

        return button;
    }

    private void styleTable(JTable table, DefaultTableModel model) {
        // 使用新的表格样式助手
        TableStyleHelper.styleTable(table, model);
    }

    private void showStockWarningDialog() {
        JDialog dialog = new JDialog(this, "库存预警", true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 获取所有需要预警的商品
        List<Product> warningProducts = new ArrayList<>();
        for (Product product : inventory.values()) {
            if (product.quantity < product.minStock) {
                warningProducts.add(product);
            }
        }

        // 创建表格
        String[] columns = {"商品名称", "当前库存", "最低库存", "缺货数量", "分类"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (Product product : warningProducts) {
            int shortage = product.minStock - product.quantity;
            model.addRow(new Object[]{
                product.name,
                product.quantity,
                product.minStock,
                shortage,
                product.category
            });
        }

        JTable table = new JTable(model);
        styleTable(table, model);

        // 设置行高
        table.setRowHeight(30);

        // 设置列宽
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        // 底部按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton restockButton = createStyledButton("批量补货", SUCCESS_COLOR);
        JButton closeButton = createStyledButton("关闭", GRAY_COLOR);

        buttonPanel.add(restockButton);
        buttonPanel.add(closeButton);

        // 统计信息
        JLabel statsLabel = new JLabel();
        if (warningProducts.isEmpty()) {
            statsLabel.setText("当前所有商品库存充足，无需补货！");
            statsLabel.setForeground(SUCCESS_COLOR);
        } else {
            statsLabel.setText("共有 " + warningProducts.size() + " 种商品需要补货，请及时处理！");
            statsLabel.setForeground(DANGER_COLOR);
        }
        statsLabel.setFont(getGeneralFont(Font.BOLD, 14));

        panel.add(statsLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(panel);

        // 批量补货按钮事件
        restockButton.addActionListener(e -> {
            if (warningProducts.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "没有需要补货的商品！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // 显示批量补货对话框
            showBatchRestockDialog(warningProducts);
            dialog.dispose();
        });

        // 关闭按钮事件
        closeButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void showBatchRestockDialog(List<Product> products) {
        JDialog dialog = new JDialog(this, "批量补货", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 创建表格
        String[] columns = {"商品名称", "当前库存", "补货数量"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        List<JTextField> quantityFields = new ArrayList<>();

        for (Product product : products) {
            model.addRow(new Object[]{product.name, product.quantity, ""});
            JTextField field = new JTextField(10);
            field.setFont(getGeneralFont(Font.PLAIN, 13));
            quantityFields.add(field);
        }

        JTable table = new JTable(model);
        styleTable(table, model);

        // 设置行高
        table.setRowHeight(30);

        // 自定义单元格编辑器
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(quantityFields.get(0)));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        // 底部按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton confirmButton = createStyledButton("确认补货", SUCCESS_COLOR);
        JButton cancelButton = createStyledButton("取消", GRAY_COLOR);

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(panel);

        // 确认补货按钮事件
        confirmButton.addActionListener(e -> {
            boolean hasError = false;
            StringBuilder errorMsg = new StringBuilder();

            for (int i = 0; i < products.size(); i++) {
                try {
                    String quantityText = (String) model.getValueAt(i, 2);
                    if (quantityText == null || quantityText.trim().isEmpty()) {
                        errorMsg.append(products.get(i).name).append("的补货数量不能为空！\n");
                        hasError = true;
                        continue;
                    }

                    int quantity = Integer.parseInt(quantityText.trim());
                    if (quantity <= 0) {
                        errorMsg.append(products.get(i).name).append("的补货数量必须大于0！\n");
                        hasError = true;
                        continue;
                    }

                    products.get(i).quantity += quantity;
                } catch (NumberFormatException ex) {
                    errorMsg.append(products.get(i).name).append("的补货数量格式错误！\n");
                    hasError = true;
                }
            }

            if (hasError) {
                JOptionPane.showMessageDialog(dialog, errorMsg.toString(), "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            saveData();
            refreshInventoryTable();
            dialog.dispose();
            JOptionPane.showMessageDialog(this, "批量补货成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
        });

        // 取消按钮事件
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void showAddProductDialog() {
        JDialog dialog = new JDialog(this, "添加商品", true);
        dialog.setSize(550, 600);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 必填字段
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("* 商品名称:"), gbc);

        gbc.gridx = 1;
        JTextField nameField = new JTextField(25);
        nameField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("* 条形码:"), gbc);

        gbc.gridx = 1;
        JPanel barcodePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JTextField barcodeField = new JTextField(20);
        barcodeField.setFont(getGeneralFont(Font.PLAIN, 13)); // 使用Arial字体，避免数字显示问题
        JButton generateBarcodeButton = new JButton("生成条形码");
        generateBarcodeButton.setFont(getGeneralFont(Font.PLAIN, 11));
        barcodePanel.add(barcodeField);
        barcodePanel.add(generateBarcodeButton);
        panel.add(barcodePanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("* 单价(元):"), gbc);

        gbc.gridx = 1;
        JTextField priceField = new JTextField(25);
        priceField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(priceField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("* 成本价(元):"), gbc);

        gbc.gridx = 1;
        JTextField costField = new JTextField(25);
        costField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(costField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("* 库存数量:"), gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.weightx = 1.0;
        JPanel quantityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JTextField quantityField = new JTextField(15);
        quantityField.setPreferredSize(new Dimension(100, 25));
        quantityField.setFont(getGeneralFont(Font.PLAIN, 13));
        JButton plusButton = new JButton("+");
        plusButton.setPreferredSize(new Dimension(30, 25));
        JButton minusButton = new JButton("-");
        minusButton.setPreferredSize(new Dimension(30, 25));
        quantityPanel.add(quantityField);
        quantityPanel.add(plusButton);
        quantityPanel.add(minusButton);
        panel.add(quantityPanel, gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(new JLabel("* 商品分类:"), gbc);

        gbc.gridx = 1;
        JComboBox<String> categoryComboBox = new JComboBox<>();
        for (Category category : categories) {
            categoryComboBox.addItem(category.name);
        }
        categoryComboBox.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(categoryComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        panel.add(new JLabel("单位:"), gbc);

        gbc.gridx = 1;
        JComboBox<String> unitComboBox = new JComboBox<>(new String[]{"个", "kg", "瓶", "盒", "包", "箱", "件", "套", "条", "双"});
        unitComboBox.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(unitComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        panel.add(new JLabel("品牌:"), gbc);

        gbc.gridx = 1;
        JTextField brandField = new JTextField(25);
        brandField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(brandField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        panel.add(new JLabel("供应商:"), gbc);

        gbc.gridx = 1;
        JTextField supplierField = new JTextField(25);
        supplierField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(supplierField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 9;
        panel.add(new JLabel("规格:"), gbc);

        gbc.gridx = 1;
        JTextField specField = new JTextField(25);
        specField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(specField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 10;
        panel.add(new JLabel("最低库存:"), gbc);

        gbc.gridx = 1;
        JTextField minStockField = new JTextField(25);
        minStockField.setText("10");
        minStockField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(minStockField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 11;
        panel.add(new JLabel("商品描述:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 11;
        gbc.gridheight = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        JTextArea descriptionArea = new JTextArea(3, 25);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(getGeneralFont(Font.PLAIN, 13));
        JScrollPane descScrollPane = new JScrollPane(descriptionArea);
        panel.add(descScrollPane, gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 13;
        gbc.gridheight = 1;
        gbc.weighty = 0.0;

        gbc.gridx = 0;
        gbc.gridy = 13;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        JButton confirmButton = createStyledButton("确定", SUCCESS_COLOR);
        JButton cancelButton = createStyledButton("取消", GRAY_COLOR);
        JButton clearButton = createStyledButton("清空", INFO_COLOR);

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(clearButton);

        panel.add(buttonPanel, gbc);

        // 提示标签
        gbc.gridx = 0;
        gbc.gridy = 14;
        gbc.gridwidth = 2;
        JLabel tipLabel = new JLabel("提示: 带 * 号的为必填项");
        tipLabel.setFont(getChineseFont(Font.PLAIN, 11));
        tipLabel.setForeground(GRAY_COLOR);
        panel.add(tipLabel, gbc);

        dialog.add(panel);

        // 生成条形码
        generateBarcodeButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                // 使用商品名称的前8位作为条形码
                String barcode = name.substring(0, Math.min(8, name.length())).toUpperCase();
                if (barcode.length() < 8) {
                    barcode = String.format("%08d", Integer.parseInt(barcode));
                }
                barcodeField.setText(barcode);
            } else {
                // 生成随机条形码
                barcodeField.setText(String.format("%08d", (int)(Math.random() * 100000000)));
            }
        });

        // 数量增减按钮
        plusButton.addActionListener(e -> {
            try {
                int quantity = Integer.parseInt(quantityField.getText());
                quantityField.setText(String.valueOf(quantity + 1));
            } catch (NumberFormatException ex) {
                quantityField.setText("1");
            }
        });

        minusButton.addActionListener(e -> {
            try {
                int quantity = Integer.parseInt(quantityField.getText());
                if (quantity > 1) {
                    quantityField.setText(String.valueOf(quantity - 1));
                }
            } catch (NumberFormatException ex) {
                quantityField.setText("1");
            }
        });

        // 清空按钮
        clearButton.addActionListener(e -> {
            nameField.setText("");
            barcodeField.setText("");
            priceField.setText("");
            costField.setText("");
            quantityField.setText("1");
            categoryComboBox.setSelectedIndex(0);
            unitComboBox.setSelectedIndex(0);
            brandField.setText("");
            supplierField.setText("");
            specField.setText("");
            minStockField.setText("10");
            descriptionArea.setText("");
        });

        // 确定按钮
        confirmButton.addActionListener(e -> {
            try {
                System.out.println("点击了添加商品确定按钮");
                // 验证必填字段
                String name = nameField.getText().trim();
                System.out.println("商品名称: " + name);
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "商品名称不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                    nameField.requestFocus();
                    return;
                }

                // 检查商品是否已存在
                if (inventory.containsKey(name)) {
                    // 显示已存在商品的信息
                    Product existingProduct = inventory.get(name);
                    String existingInfo = String.format("商品名称: %s\n价格: %.2f元\n库存: %d\n分类: %s",
                        existingProduct.name,
                        existingProduct.price,
                        existingProduct.quantity,
                        existingProduct.category);

                    int option = JOptionPane.showConfirmDialog(dialog,
                        "该商品已存在！\n\n" + existingInfo + "\n\n是否要修改该商品的信息？",
                        "商品已存在",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                    if (option == JOptionPane.YES_OPTION) {
                        // 用户选择修改现有商品
                        dialog.dispose();
                        showEditProductDialog(existingProduct);
                        return;
                    } else if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
                        // 用户选择取消，不做任何操作
                        return;
                    } else {
                        // 用户选择"否"，不允许添加重复商品
                        JOptionPane.showMessageDialog(dialog, "商品名称已存在，请使用不同的名称！", "错误", JOptionPane.ERROR_MESSAGE);
                        nameField.requestFocus();
                        return;
                    }
                }

                String barcode = barcodeField.getText().trim();
                System.out.println("条形码: " + barcode);
                if (barcode.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "条形码不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                    barcodeField.requestFocus();
                    return;
                }

                // 检查条形码是否重复
                for (Product product : inventory.values()) {
                    if (product.barcode != null && product.barcode.equals(barcode)) {
                        // 如果是同一个商品，跳过检查
                        if (inventory.containsKey(name) && product.name.equals(name)) {
                            continue;
                        }
                        JOptionPane.showMessageDialog(dialog, "该条形码已被其他商品使用！", "错误", JOptionPane.ERROR_MESSAGE);
                        barcodeField.requestFocus();
                        return;
                    }
                }

                double price = Double.parseDouble(priceField.getText());
                double cost = Double.parseDouble(costField.getText());
                int quantity = Integer.parseInt(quantityField.getText());
                String category = (String) categoryComboBox.getSelectedItem();
                String unit = (String) unitComboBox.getSelectedItem();
                String brand = brandField.getText().trim();
                String supplier = supplierField.getText().trim();
                String spec = specField.getText().trim();
                int minStock = Integer.parseInt(minStockField.getText());
                String description = descriptionArea.getText().trim();

                System.out.println("准备创建商品: " + name);

                // 验证数据
                if (price <= 0) {
                    JOptionPane.showMessageDialog(dialog, "单价必须大于0！", "错误", JOptionPane.ERROR_MESSAGE);
                    priceField.requestFocus();
                    return;
                }

                if (cost <= 0) {
                    JOptionPane.showMessageDialog(dialog, "成本价必须大于0！", "错误", JOptionPane.ERROR_MESSAGE);
                    costField.requestFocus();
                    return;
                }

                if (cost >= price) {
                    int confirm = JOptionPane.showConfirmDialog(dialog,
                        "成本价(" + cost + ")高于或等于售价(" + price + ")，这会导致亏损！\n是否继续？",
                        "警告", JOptionPane.YES_NO_OPTION);
                    if (confirm != JOptionPane.YES_OPTION) {
                        costField.requestFocus();
                        return;
                    }
                }

                if (quantity <= 0) {
                    JOptionPane.showMessageDialog(dialog, "库存数量必须大于0！", "错误", JOptionPane.ERROR_MESSAGE);
                    quantityField.requestFocus();
                    return;
                }

                if (minStock < 0) {
                    JOptionPane.showMessageDialog(dialog, "最低库存不能为负数！", "错误", JOptionPane.ERROR_MESSAGE);
                    minStockField.requestFocus();
                    return;
                }

                // 创建商品
                Product product = new Product(name, price, quantity, category);
                product.barcode = barcode;
                product.unit = unit;
                product.description = description;
                product.brand = brand;
                product.supplier = supplier;
                product.spec = spec;
                product.minStock = minStock;
                product.cost = cost;

                System.out.println("商品创建成功，准备保存");
                inventory.put(name, product);
                saveData();
                refreshInventoryTable();
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "商品添加成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                System.out.println("商品添加完成");
            } catch (NumberFormatException ex) {
                System.out.println("数字格式错误: " + ex.getMessage());
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "请输入有效的数字！\n错误详情: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                System.out.println("添加商品时出错: " + ex.getClass().getName());
                System.out.println("错误消息: " + ex.getMessage());
                ex.printStackTrace();
                String errorMsg = "添加商品时出错！\n";
                errorMsg += "错误类型: " + ex.getClass().getSimpleName() + "\n";
                if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
                    errorMsg += "错误详情: " + ex.getMessage() + "\n";
                }
                JOptionPane.showMessageDialog(dialog, errorMsg, "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        // 取消按钮
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void showEditProductDialog(Product product) {
        JDialog dialog = new JDialog(this, "修改商品信息", true);
        dialog.setSize(550, 600);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 必填字段
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("* 商品名称:"), gbc);

        gbc.gridx = 1;
        JTextField nameField = new JTextField(25);
        nameField.setText(product.name);
        nameField.setEditable(false); // 商品名称不可修改
        nameField.setBackground(new Color(240, 240, 240));
        nameField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("* 条形码:"), gbc);

        gbc.gridx = 1;
        JPanel barcodePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JTextField barcodeField = new JTextField(20);
        barcodeField.setText(product.barcode != null ? product.barcode : "");
        barcodeField.setEditable(false); // 条形码不可修改
        barcodeField.setBackground(new Color(240, 240, 240));
        barcodeField.setFont(getGeneralFont(Font.PLAIN, 13));
        barcodePanel.add(barcodeField);
        panel.add(barcodePanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("* 单价(元):"), gbc);

        gbc.gridx = 1;
        JTextField priceField = new JTextField(25);
        priceField.setText(String.format("%.2f", product.price));
        priceField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(priceField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("* 成本价(元):"), gbc);

        gbc.gridx = 1;
        JTextField costField = new JTextField(25);
        costField.setText(String.format("%.2f", product.cost));
        costField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(costField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("* 库存数量:"), gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.weightx = 1.0;
        JPanel quantityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JTextField quantityField = new JTextField(15);
        quantityField.setPreferredSize(new Dimension(100, 25));
        quantityField.setText(String.valueOf(product.quantity));
        quantityField.setFont(getGeneralFont(Font.PLAIN, 13));
        JButton plusButton = new JButton("+");
        plusButton.setPreferredSize(new Dimension(30, 25));
        JButton minusButton = new JButton("-");
        minusButton.setPreferredSize(new Dimension(30, 25));
        quantityPanel.add(quantityField);
        quantityPanel.add(plusButton);
        quantityPanel.add(minusButton);
        panel.add(quantityPanel, gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(new JLabel("* 商品分类:"), gbc);

        gbc.gridx = 1;
        JComboBox<String> categoryComboBox = new JComboBox<>();
        for (Category category : categories) {
            categoryComboBox.addItem(category.name);
        }
        if (product.category != null) {
            categoryComboBox.setSelectedItem(product.category);
        }
        categoryComboBox.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(categoryComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        panel.add(new JLabel("单位:"), gbc);

        gbc.gridx = 1;
        JComboBox<String> unitComboBox = new JComboBox<>(new String[]{"个", "kg", "瓶", "盒", "包", "箱", "件", "套", "条", "双"});
        if (product.unit != null) {
            unitComboBox.setSelectedItem(product.unit);
        }
        unitComboBox.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(unitComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        panel.add(new JLabel("品牌:"), gbc);

        gbc.gridx = 1;
        JTextField brandField = new JTextField(25);
        brandField.setText(product.brand != null ? product.brand : "");
        brandField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(brandField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        panel.add(new JLabel("供应商:"), gbc);

        gbc.gridx = 1;
        JTextField supplierField = new JTextField(25);
        supplierField.setText(product.supplier != null ? product.supplier : "");
        supplierField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(supplierField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 9;
        panel.add(new JLabel("规格:"), gbc);

        gbc.gridx = 1;
        JTextField specField = new JTextField(25);
        specField.setText(product.spec != null ? product.spec : "");
        specField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(specField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 10;
        panel.add(new JLabel("最低库存:"), gbc);

        gbc.gridx = 1;
        JTextField minStockField = new JTextField(25);
        minStockField.setText(String.valueOf(product.minStock));
        minStockField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(minStockField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 11;
        panel.add(new JLabel("商品描述:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 11;
        gbc.gridheight = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        JTextArea descriptionArea = new JTextArea(3, 25);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(getGeneralFont(Font.PLAIN, 13));
        descriptionArea.setText(product.description != null ? product.description : "");
        JScrollPane descScrollPane = new JScrollPane(descriptionArea);
        panel.add(descScrollPane, gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 13;
        gbc.gridheight = 1;
        gbc.weighty = 0.0;

        gbc.gridx = 0;
        gbc.gridy = 13;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        JButton confirmButton = createStyledButton("保存修改", SUCCESS_COLOR);
        JButton cancelButton = createStyledButton("取消", GRAY_COLOR);

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        panel.add(buttonPanel, gbc);

        // 提示标签
        gbc.gridx = 0;
        gbc.gridy = 14;
        gbc.gridwidth = 2;
        JLabel tipLabel = new JLabel("提示: 商品名称和条形码不可修改");
        tipLabel.setFont(getChineseFont(Font.PLAIN, 11));
        tipLabel.setForeground(GRAY_COLOR);
        panel.add(tipLabel, gbc);

        dialog.add(panel);

        // 数量增减按钮
        plusButton.addActionListener(e -> {
            try {
                int quantity = Integer.parseInt(quantityField.getText());
                quantityField.setText(String.valueOf(quantity + 1));
            } catch (NumberFormatException ex) {
                quantityField.setText("1");
            }
        });

        minusButton.addActionListener(e -> {
            try {
                int quantity = Integer.parseInt(quantityField.getText());
                if (quantity > 1) {
                    quantityField.setText(String.valueOf(quantity - 1));
                }
            } catch (NumberFormatException ex) {
                quantityField.setText("1");
            }
        });

        // 确定按钮
        confirmButton.addActionListener(e -> {
            try {
                System.out.println("点击了修改商品确定按钮");
                // 验证必填字段
                double price = Double.parseDouble(priceField.getText());
                double cost = Double.parseDouble(costField.getText());
                int quantity = Integer.parseInt(quantityField.getText());
                String category = (String) categoryComboBox.getSelectedItem();
                String unit = (String) unitComboBox.getSelectedItem();
                String brand = brandField.getText().trim();
                String supplier = supplierField.getText().trim();
                String spec = specField.getText().trim();
                int minStock = Integer.parseInt(minStockField.getText());
                String description = descriptionArea.getText().trim();

                System.out.println("准备更新商品: " + product.name);

                // 验证数据
                if (price <= 0) {
                    JOptionPane.showMessageDialog(dialog, "单价必须大于0！", "错误", JOptionPane.ERROR_MESSAGE);
                    priceField.requestFocus();
                    return;
                }

                if (cost <= 0) {
                    JOptionPane.showMessageDialog(dialog, "成本价必须大于0！", "错误", JOptionPane.ERROR_MESSAGE);
                    costField.requestFocus();
                    return;
                }

                if (cost >= price) {
                    int confirm = JOptionPane.showConfirmDialog(dialog,
                        "成本价(" + cost + ")高于或等于售价(" + price + ")，这会导致亏损！\n是否继续？",
                        "警告", JOptionPane.YES_NO_OPTION);
                    if (confirm != JOptionPane.YES_OPTION) {
                        costField.requestFocus();
                        return;
                    }
                }

                if (quantity <= 0) {
                    JOptionPane.showMessageDialog(dialog, "库存数量必须大于0！", "错误", JOptionPane.ERROR_MESSAGE);
                    quantityField.requestFocus();
                    return;
                }

                if (minStock < 0) {
                    JOptionPane.showMessageDialog(dialog, "最低库存不能为负数！", "错误", JOptionPane.ERROR_MESSAGE);
                    minStockField.requestFocus();
                    return;
                }

                // 更新商品信息
                product.price = price;
                product.quantity = quantity;
                product.category = category;
                product.unit = unit;
                product.description = description;
                product.brand = brand;
                product.supplier = supplier;
                product.spec = spec;
                product.minStock = minStock;
                product.cost = cost;

                System.out.println("商品信息更新成功，准备保存");
                saveData();
                refreshInventoryTable();
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "商品信息修改成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                System.out.println("商品信息修改完成");
            } catch (NumberFormatException ex) {
                System.out.println("数字格式错误: " + ex.getMessage());
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "请输入有效的数字！\n错误详情: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                System.out.println("修改商品时出错: " + ex.getClass().getName());
                System.out.println("错误消息: " + ex.getMessage());
                ex.printStackTrace();
                String errorMsg = "修改商品时出错！\n";
                errorMsg += "错误类型: " + ex.getClass().getSimpleName() + "\n";
                if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
                    errorMsg += "错误详情: " + ex.getMessage() + "\n";
                }
                JOptionPane.showMessageDialog(dialog, errorMsg, "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        // 取消按钮
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void showRestockDialog() {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请先选择要补货的商品！", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String productName = (String) inventoryTableModel.getValueAt(selectedRow, 0);
        Product product = inventory.get(productName);

        JDialog dialog = new JDialog(this, "补货 - " + productName, true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("当前库存: " + product.quantity), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("补货数量:"), gbc);

        gbc.gridx = 1;
        JTextField quantityField = new JTextField(20);
        panel.add(quantityField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        JButton confirmButton = createStyledButton("确定", new Color(46, 204, 113));
        JButton cancelButton = createStyledButton("取消", new Color(149, 165, 166));

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        panel.add(buttonPanel, gbc);

        dialog.add(panel);

        confirmButton.addActionListener(e -> {
            try {
                int addQuantity = Integer.parseInt(quantityField.getText());
                if (addQuantity <= 0) {
                    JOptionPane.showMessageDialog(dialog, "补货数量必须大于0！", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                product.quantity += addQuantity;
                refreshInventoryTable();
                saveData(); // 自动保存
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "补货成功！当前库存: " + product.quantity, "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "请输入有效的整数！", "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void deleteSelectedProduct() {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请先选择要删除的商品！", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String productName = (String) inventoryTableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
            "确定要删除商品 \"" + productName + "\" 吗？", "确认删除", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            inventory.remove(productName);
            refreshInventoryTable();
            saveData(); // 自动保存
            JOptionPane.showMessageDialog(this, "商品删除成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showAddToCartDialog() {
        // 检查是否有进行中的班次
        if (!checkShiftRequired()) {
            return;
        }
        
        if (inventory.isEmpty()) {
            JOptionPane.showMessageDialog(this, "库存为空，请先添加商品！", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(this, "添加到购物车", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("选择商品:"), gbc);

        gbc.gridx = 1;
        JComboBox<String> productComboBox = new JComboBox<>();
        for (Product product : inventory.values()) {
            if (product.quantity > 0) {
                productComboBox.addItem(product.name);
            }
        }

        if (productComboBox.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this, "没有可用的商品（库存为空）！", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        panel.add(productComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("购买数量:"), gbc);

        gbc.gridx = 1;
        JTextField quantityField = new JTextField(20);
        panel.add(quantityField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        JButton confirmButton = createStyledButton("确定", new Color(46, 204, 113));
        JButton cancelButton = createStyledButton("取消", new Color(149, 165, 166));

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        panel.add(buttonPanel, gbc);

        dialog.add(panel);

        confirmButton.addActionListener(e -> {
            try {
                String productName = (String) productComboBox.getSelectedItem();
                int quantity = Integer.parseInt(quantityField.getText());

                if (quantity <= 0) {
                    JOptionPane.showMessageDialog(dialog, "购买数量必须大于0！", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Product inventoryProduct = inventory.get(productName);
                if (quantity > inventoryProduct.quantity) {
                    JOptionPane.showMessageDialog(dialog, "库存不足！当前库存: " + inventoryProduct.quantity, "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                cart.add(new Product(productName, inventoryProduct.price, quantity));
                refreshCartTable();
                dialog.dispose();
                // 商品已添加到购物车，直接在购物车中显示
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "请输入有效的整数！", "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void editCartItem() {
        // 检查是否有进行中的班次
        if (!checkShiftRequiredStrict()) {
            return;
        }
        
        int selectedRow = cartTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请先选择要编辑的商品！", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String productName = (String) cartTableModel.getValueAt(selectedRow, 0);
        Product cartProduct = cart.get(selectedRow);
        Product inventoryProduct = inventory.get(productName);

        JDialog dialog = new JDialog(this, "✏️ 编辑数量 - " + productName, true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("当前数量: " + cartProduct.quantity), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("新数量:"), gbc);

        gbc.gridx = 1;
        JTextField quantityField = new JTextField(20);
        panel.add(quantityField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        JButton confirmButton = createStyledButton("确定", new Color(46, 204, 113));
        JButton cancelButton = createStyledButton("取消", new Color(149, 165, 166));

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        panel.add(buttonPanel, gbc);

        dialog.add(panel);

        confirmButton.addActionListener(e -> {
            try {
                int newQuantity = Integer.parseInt(quantityField.getText());
                if (newQuantity <= 0) {
                    JOptionPane.showMessageDialog(dialog, "数量必须大于0！", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (newQuantity > inventoryProduct.quantity) {
                    JOptionPane.showMessageDialog(dialog, "库存不足！当前库存: " + inventoryProduct.quantity, "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                cartProduct.quantity = newQuantity;
                refreshCartTable();
                dialog.dispose();
                // 数量已修改，直接在购物车中显示
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "请输入有效的整数！", "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void removeCartItem() {
        // 检查是否有进行中的班次
        if (!checkShiftRequiredStrict()) {
            return;
        }
        
        int selectedRow = cartTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请先选择要移除的商品！", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String productName = (String) cartTableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
            "确定要移除商品 \"" + productName + "\" 吗？", "确认移除", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            cart.remove(selectedRow);
            refreshCartTable();
            // 商品已移除，直接在购物车中显示
        }
    }

    private void clearCart() {
        // 检查是否有进行中的班次
        if (!checkShiftRequiredStrict()) {
            return;
        }
        
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "购物车已经是空的了！", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "确定要清空购物车吗？", "确认清空", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            cart.clear();
            refreshCartTable();
            // 购物车已清空，直接在购物车中显示
        }
    }

    private void refreshInventoryTable() {
        inventoryTableModel.setRowCount(0);
        for (Product product : inventory.values()) {
            String warningStatus = "";
            if (product.quantity <= 0) {
                warningStatus = "缺货";
            } else if (product.quantity < product.minStock) {
                warningStatus = "库存不足";
            } else {
                warningStatus = "正常";
            }
            inventoryTableModel.addRow(new Object[]{
                product.name, 
                String.format("%.2f", product.price), 
                product.quantity, 
                product.minStock,
                product.category, 
                warningStatus
            });
        }
    }

    private void searchInventory() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            refreshInventoryTable();
            return;
        }

        inventoryTableModel.setRowCount(0);
        String lowerKeyword = keyword.toLowerCase();

        for (Product product : inventory.values()) {
            boolean match = false;

            // 按商品名称搜索（模糊匹配）
            if (product.name.toLowerCase().contains(lowerKeyword)) {
                match = true;
            }

            // 按分类搜索
            if (!match && product.category.toLowerCase().contains(lowerKeyword)) {
                match = true;
            }

            // 按价格搜索（支持价格范围，如 "10-20"）
            if (!match && keyword.contains("-")) {
                try {
                    String[] range = keyword.split("-");
                    if (range.length == 2) {
                        double minPrice = Double.parseDouble(range[0].trim());
                        double maxPrice = Double.parseDouble(range[1].trim());
                        if (product.price >= minPrice && product.price <= maxPrice) {
                            match = true;
                        }
                    }
                } catch (NumberFormatException e) {
                    // 忽略价格范围解析错误
                }
            }

            // 按单个价格搜索
            if (!match) {
                try {
                    double searchPrice = Double.parseDouble(keyword);
                    if (Math.abs(product.price - searchPrice) < 0.01) {
                        match = true;
                    }
                } catch (NumberFormatException e) {
                    // 忽略价格解析错误
                }
            }

            if (match) {
                String status = product.quantity > 0 ? "有货" : "缺货";
                inventoryTableModel.addRow(new Object[]{
                    product.name, String.format("%.2f", product.price), product.quantity, product.category, status
                });
            }
        }
    }

    private void sortInventory() {
        int selectedIndex = sortComboBox.getSelectedIndex();
        List<Product> products = new ArrayList<>(inventory.values());

        switch (selectedIndex) {
            case 1: // 按名称
                products.sort((a, b) -> a.name.compareTo(b.name));
                break;
            case 2: // 按价格(低→高)
                products.sort((a, b) -> Double.compare(a.price, b.price));
                break;
            case 3: // 按价格(高→低)
                products.sort((a, b) -> Double.compare(b.price, a.price));
                break;
            case 4: // 按库存(多→少)
                products.sort((a, b) -> Integer.compare(b.quantity, a.quantity));
                break;
        }

        inventoryTableModel.setRowCount(0);
        for (Product product : products) {
            String status = product.quantity > 0 ? "有货" : "缺货";
            inventoryTableModel.addRow(new Object[]{
                product.name, String.format("%.2f", product.price), product.quantity, product.category, status
            });
        }
    }

    private void refreshCartTable() {
        cartTableModel.setRowCount(0);
        double total = 0;
        int totalQuantity = 0;
        for (Product product : cart) {
            double subtotal = product.price * product.quantity;
            total += subtotal;
            totalQuantity += product.quantity;
            cartTableModel.addRow(new Object[]{
                product.name, String.format("%.2f", product.price), product.quantity, String.format("%.2f", subtotal)
            });
        }

        // 更新购物车合计信息
        cartTypeCountLabel.setText("商品种类: " + cart.size() + "种");
        cartTotalQuantityLabel.setText("商品总数: " + totalQuantity + "件");
        cartTotalLabel.setText("总金额: ¥" + String.format("%.2f", total));
    }

    private void refreshTransactionTable(JLabel totalRevenueLabel, JLabel transactionCountLabel, JLabel avgRevenueLabel, JLabel totalTaxLabel) {
        transactionTableModel.setRowCount(0);
        double totalRevenue = 0;
        double totalTax = 0;
        for (Transaction transaction : transactions) {
            transactionTableModel.addRow(new Object[]{
                transaction.transactionId,
                transaction.timestamp,
                transaction.items.size(),
                String.format("%.2f", transaction.totalAmount),
                String.format("%.2f", transaction.tax),
                String.format("%.2f", transaction.finalAmount)
            });
            totalRevenue += transaction.finalAmount;
            totalTax += transaction.tax;
        }

        int count = transactions.size();
        double avgRevenue = count > 0 ? totalRevenue / count : 0;

        totalRevenueLabel.setText("总销售额: ¥" + String.format("%.2f", totalRevenue));
        transactionCountLabel.setText("交易次数: " + count + "次");
        avgRevenueLabel.setText("平均交易额: ¥" + String.format("%.2f", avgRevenue));
        totalTaxLabel.setText("总税费: ¥" + String.format("%.2f", totalTax));
    }

    private void showTransactionDetail(int selectedRow) {
        Transaction transaction = transactions.get(selectedRow);

        JDialog dialog = new JDialog(this, "交易详情 - " + transaction.transactionId, true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 交易信息
        JPanel infoPanel = new JPanel(new GridLayout(4, 2, 10, 5));
        infoPanel.setBorder(BorderFactory.createTitledBorder("交易信息"));

        infoPanel.add(new JLabel("交易ID:"));
        infoPanel.add(new JLabel(transaction.transactionId));
        infoPanel.add(new JLabel("交易时间:"));
        infoPanel.add(new JLabel(transaction.timestamp));
        infoPanel.add(new JLabel("小计金额:"));
        infoPanel.add(new JLabel("¥" + String.format("%.2f", transaction.totalAmount)));
        infoPanel.add(new JLabel("实收金额:"));
        infoPanel.add(new JLabel("¥" + String.format("%.2f", transaction.finalAmount)));

        // 商品清单
        JPanel itemsPanel = new JPanel(new BorderLayout());
        itemsPanel.setBorder(BorderFactory.createTitledBorder("商品清单"));

        String[] itemColumns = {"商品名称", "单价(元)", "数量", "小计(元)"};
        DefaultTableModel itemTableModel = new DefaultTableModel(itemColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (Product product : transaction.items) {
            double subtotal = product.price * product.quantity;
            itemTableModel.addRow(new Object[]{
                product.name,
                String.format("%.2f", product.price),
                product.quantity,
                String.format("%.2f", subtotal)
            });
        }

        JTable itemTable = new JTable(itemTableModel);
        itemTable.setRowHeight(25);
        JScrollPane itemScrollPane = new JScrollPane(itemTable);
        itemsPanel.add(itemScrollPane, BorderLayout.CENTER);

        // 合计信息
        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        summaryPanel.add(new JLabel("商家税费: ¥" + String.format("%.2f", transaction.tax) + " (商家承担)"));

        // 关闭按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton closeButton = createStyledButton("关闭", new Color(149, 165, 166));
        buttonPanel.add(closeButton);

        dialog.add(infoPanel, BorderLayout.NORTH);
        dialog.add(itemsPanel, BorderLayout.CENTER);
        dialog.add(summaryPanel, BorderLayout.SOUTH);

        // 使用单独的底部面板放置按钮
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(summaryPanel, BorderLayout.NORTH);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        closeButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void exportTransactions() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存交易记录");
        fileChooser.setSelectedFile(new java.io.File("交易记录_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToSave = fileChooser.getSelectedFile();

            try (java.io.PrintWriter writer = new java.io.PrintWriter(fileToSave, "UTF-8")) {
                writer.println("========================================");
                writer.println("           交易记录导出");
                writer.println("========================================");
                writer.println("导出时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                writer.println("总交易数: " + transactions.size());
                writer.println("========================================\n");

                for (Transaction transaction : transactions) {
                    writer.println("交易ID: " + transaction.transactionId);
                    writer.println("交易时间: " + transaction.timestamp);
                    writer.println("商品清单:");
                    for (Product product : transaction.items) {
                        writer.printf("  - %-20s x %-3d = ¥%.2f\n",
                            product.name, product.quantity, product.price * product.quantity);
                    }
                    writer.println("小计金额: ¥" + String.format("%.2f", transaction.totalAmount));
                    writer.println("商家税费: ¥" + String.format("%.2f", transaction.tax) + " (商家承担)");
                    writer.println("实收金额: ¥" + String.format("%.2f", transaction.finalAmount));
                    writer.println("----------------------------------------\n");
                }

                double totalRevenue = 0;
                double totalTax = 0;
                for (Transaction t : transactions) {
                    totalRevenue += t.finalAmount;
                    totalTax += t.tax;
                }

                writer.println("========================================");
                writer.println("            统计汇总");
                writer.println("========================================");
                writer.println("总销售额: ¥" + String.format("%.2f", totalRevenue));
                writer.println("总税费: ¥" + String.format("%.2f", totalTax));
                writer.println("平均交易额: ¥" + String.format("%.2f", totalRevenue / transactions.size()));
                writer.println("========================================");

                JOptionPane.showMessageDialog(this,
                    "交易记录导出成功！\n文件位置: " + fileToSave.getAbsolutePath(),
                    "导出成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "导出失败: " + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showCategoryManagementDialog() {
        JDialog dialog = new JDialog(this, "分类管理", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 分类列表
        String[] columns = {"分类名称", "描述"};
        DefaultTableModel categoryTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (Category category : categories) {
            categoryTableModel.addRow(new Object[]{category.name, category.description});
        }

        JTable categoryTable = new JTable(categoryTableModel);
        categoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoryTable.setRowHeight(25);

        JScrollPane scrollPane = new JScrollPane(categoryTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        JButton addButton = createStyledButton("添加分类", SUCCESS_COLOR);
        JButton editButton = createStyledButton("编辑分类", INFO_COLOR);
        JButton deleteButton = createStyledButton("删除分类", DANGER_COLOR);
        JButton closeButton = createStyledButton("关闭", GRAY_COLOR);

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(closeButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(panel);

        // 添加分类
        addButton.addActionListener(e -> {
            JDialog addDialog = new JDialog(dialog, "添加分类", true);
            addDialog.setSize(350, 200);
            addDialog.setLocationRelativeTo(dialog);

            JPanel addPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0;
            gbc.gridy = 0;
            addPanel.add(new JLabel("分类名称:"), gbc);

            gbc.gridx = 1;
            JTextField nameField = new JTextField(20);
            addPanel.add(nameField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            addPanel.add(new JLabel("描述:"), gbc);

            gbc.gridx = 1;
            JTextField descField = new JTextField(20);
            addPanel.add(descField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 2;
            JPanel confirmPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

            JButton confirmButton = createStyledButton("确定", SUCCESS_COLOR);
            JButton cancelButton = createStyledButton("取消", GRAY_COLOR);

            confirmPanel.add(confirmButton);
            confirmPanel.add(cancelButton);

            addPanel.add(confirmPanel, gbc);

            addDialog.add(addPanel);

            confirmButton.addActionListener(e1 -> {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(addDialog, "分类名称不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // 检查分类是否已存在
                for (Category cat : categories) {
                    if (cat.name.equals(name)) {
                        JOptionPane.showMessageDialog(addDialog, "该分类已存在！", "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                String description = descField.getText().trim();
                categories.add(new Category(name, description));
                saveData();

                // 刷新表格
                categoryTableModel.setRowCount(0);
                for (Category category : categories) {
                    categoryTableModel.addRow(new Object[]{category.name, category.description});
                }

                addDialog.dispose();
                JOptionPane.showMessageDialog(this, "分类添加成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            });

            cancelButton.addActionListener(e1 -> addDialog.dispose());

            addDialog.setVisible(true);
        });

        // 编辑分类
        editButton.addActionListener(e -> {
            int selectedRow = categoryTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog, "请先选择要编辑的分类！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            Category selectedCategory = categories.get(selectedRow);

            JDialog editDialog = new JDialog(dialog, "编辑分类", true);
            editDialog.setSize(350, 200);
            editDialog.setLocationRelativeTo(dialog);

            JPanel editPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0;
            gbc.gridy = 0;
            editPanel.add(new JLabel("分类名称:"), gbc);

            gbc.gridx = 1;
            JTextField nameField = new JTextField(20);
            nameField.setText(selectedCategory.name);
            editPanel.add(nameField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            editPanel.add(new JLabel("描述:"), gbc);

            gbc.gridx = 1;
            JTextField descField = new JTextField(20);
            descField.setText(selectedCategory.description);
            editPanel.add(descField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 2;
            JPanel confirmPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

            JButton confirmButton = createStyledButton("确定", SUCCESS_COLOR);
            JButton cancelButton = createStyledButton("取消", GRAY_COLOR);

            confirmPanel.add(confirmButton);
            confirmPanel.add(cancelButton);

            editPanel.add(confirmPanel, gbc);

            editDialog.add(editPanel);

            confirmButton.addActionListener(e1 -> {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(editDialog, "分类名称不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // 检查分类名称是否与其他分类重复
                for (int i = 0; i < categories.size(); i++) {
                    if (i != selectedRow && categories.get(i).name.equals(name)) {
                        JOptionPane.showMessageDialog(editDialog, "该分类名称已存在！", "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                selectedCategory.name = name;
                selectedCategory.description = descField.getText().trim();
                saveData();

                // 刷新表格
                categoryTableModel.setRowCount(0);
                for (Category category : categories) {
                    categoryTableModel.addRow(new Object[]{category.name, category.description});
                }

                // 刷新库存表格
                refreshInventoryTable();

                editDialog.dispose();
                JOptionPane.showMessageDialog(this, "分类修改成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            });

            cancelButton.addActionListener(e1 -> editDialog.dispose());

            editDialog.setVisible(true);
        });

        // 删除分类
        deleteButton.addActionListener(e -> {
            int selectedRow = categoryTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog, "请先选择要删除的分类！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            Category selectedCategory = categories.get(selectedRow);

            // 检查是否有商品使用该分类
            for (Product product : inventory.values()) {
                if (product.category.equals(selectedCategory.name)) {
                    JOptionPane.showMessageDialog(dialog, 
                        "该分类下还有商品，无法删除！\n请先修改或删除该分类下的商品。", 
                        "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            int confirm = JOptionPane.showConfirmDialog(dialog,
                "确定要删除分类 \"" + selectedCategory.name + "\" 吗？",
                "确认删除", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                categories.remove(selectedRow);
                saveData();

                // 刷新表格
                categoryTableModel.setRowCount(0);
                for (Category category : categories) {
                    categoryTableModel.addRow(new Object[]{category.name, category.description});
                }

                JOptionPane.showMessageDialog(this, "分类删除成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        closeButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void showMemberManagementDialog(JComboBox<String> memberComboBox) {
        JDialog dialog = new JDialog(this, "会员管理", true);
        dialog.setSize(1000, 450);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 会员列表
        String[] columns = {"手机号", "姓名", "积分", "等级", "折扣", "余额", "生日"};
        DefaultTableModel memberTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // 重新加载会员数据
        System.out.println("会员管理对话框打开时，当前会员数量: " + members.size());
        for (String phone : members.keySet()) {
            System.out.println("会员: " + phone + " - " + members.get(phone).name);
        }

        for (Member member : members.values()) {
            memberTableModel.addRow(new Object[]{
                member.phone,
                member.name,
                String.format("%.0f", member.points),
                member.level,
                String.format("%.1f折", member.discount * 10),
                String.format("%.2f", member.balance),
                member.birthday != null ? member.birthday : ""
            });
        }
        
        System.out.println("会员表格行数: " + memberTableModel.getRowCount());

        JTable memberTable = new JTable(memberTableModel);
        memberTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        memberTable.setRowHeight(25);

        JScrollPane scrollPane = new JScrollPane(memberTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 按钮面板 - 使用两行布局
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        
        // 第一行按钮
        JPanel firstRowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton addButton = createStyledButton("添加会员", SUCCESS_COLOR);
        JButton editButton = createStyledButton("编辑会员", INFO_COLOR);
        JButton rechargeButton = createStyledButton("会员充值", new Color(241, 196, 15));
        JButton recordButton = createStyledButton("消费记录", PURPLE_COLOR);
        
        firstRowPanel.add(addButton);
        firstRowPanel.add(editButton);
        firstRowPanel.add(rechargeButton);
        firstRowPanel.add(recordButton);
        
        // 第二行按钮
        JPanel secondRowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton statsButton = createStyledButton("报表统计", new Color(52, 152, 219));
        JButton deleteButton = createStyledButton("删除会员", DANGER_COLOR);
        JButton refreshButton = createStyledButton("刷新", new Color(59, 130, 246));
        JButton closeButton = createStyledButton("关闭", GRAY_COLOR);
        
        secondRowPanel.add(statsButton);
        secondRowPanel.add(deleteButton);
        secondRowPanel.add(refreshButton);
        secondRowPanel.add(closeButton);
        
        buttonPanel.add(firstRowPanel);
        buttonPanel.add(secondRowPanel);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(panel);

        // 添加会员
        addButton.addActionListener(e -> {
            JDialog addDialog = new JDialog(dialog, "添加会员", true);
            addDialog.setSize(400, 350);
            addDialog.setLocationRelativeTo(dialog);

            JPanel addPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0;
            gbc.gridy = 0;
            addPanel.add(new JLabel("手机号:"), gbc);

            gbc.gridx = 1;
            JTextField phoneField = new JTextField(20);
            phoneField.setFont(getGeneralFont(Font.PLAIN, 13));
            addPanel.add(phoneField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            addPanel.add(new JLabel("姓名:"), gbc);

            gbc.gridx = 1;
            JTextField nameField = new JTextField(20);
            nameField.setFont(getGeneralFont(Font.PLAIN, 13));
            addPanel.add(nameField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            addPanel.add(new JLabel("余额:"), gbc);

            gbc.gridx = 1;
            JTextField balanceField = new JTextField(20);
            balanceField.setText("0.00");
            balanceField.setFont(getGeneralFont(Font.PLAIN, 13));
            addPanel.add(balanceField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 3;
            addPanel.add(new JLabel("生日(MM-dd):"), gbc);

            gbc.gridx = 1;
            JTextField birthdayField = new JTextField(20);
            birthdayField.setFont(getGeneralFont(Font.PLAIN, 13));
            addPanel.add(birthdayField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 4;
            gbc.gridwidth = 2;
            JPanel confirmPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

            JButton confirmButton = createStyledButton("确定", SUCCESS_COLOR);
            JButton cancelButton = createStyledButton("取消", GRAY_COLOR);

            confirmPanel.add(confirmButton);
            confirmPanel.add(cancelButton);

            addPanel.add(confirmPanel, gbc);

            addDialog.add(addPanel);

            confirmButton.addActionListener(e1 -> {
                String phone = phoneField.getText().trim();
                String name = nameField.getText().trim();

                System.out.println("尝试添加会员 - 手机号: " + phone + ", 姓名: " + name);
                System.out.println("当前会员数量: " + members.size());
                System.out.println("手机号是否已存在: " + members.containsKey(phone));

                if (phone.isEmpty() || name.isEmpty()) {
                    JOptionPane.showMessageDialog(addDialog, "手机号和姓名不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (members.containsKey(phone)) {
                    Member existingMember = members.get(phone);
                    System.out.println("手机号已存在 - 会员信息: " + existingMember.name + ", " + existingMember.level);
                    JOptionPane.showMessageDialog(addDialog,
                        "该手机号已注册！\n\n已注册信息：\n姓名: " + existingMember.name +
                        "\n等级: " + existingMember.level +
                        "\n积分: " + String.format("%.0f", existingMember.points) +
                        "\n\n如需修改信息，请使用\"编辑会员\"功能。",
                        "手机号已注册",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }

                try {
                    double balance = Double.parseDouble(balanceField.getText().trim());
                    if (balance < 0) {
                        JOptionPane.showMessageDialog(addDialog, "余额不能为负数！", "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    String birthday = birthdayField.getText().trim();

                    Member member = new Member(phone, name, 0, "普通会员", 1.0, balance, birthday.isEmpty() ? null : birthday);
                    members.put(phone, member);
                    System.out.println("会员已添加到内存 - 当前会员数量: " + members.size());
                    
                    saveData();
                    System.out.println("数据已保存到文件");

                    // 刷新表格
                    memberTableModel.setRowCount(0);
                    System.out.println("开始刷新表格...");
                    for (Member m : members.values()) {
                        System.out.println("添加到表格: " + m.phone + " - " + m.name);
                        memberTableModel.addRow(new Object[]{
                            m.phone, m.name,
                            String.format("%.0f", m.points),
                            m.level,
                            String.format("%.1f折", m.discount * 10),
                            String.format("%.2f", m.balance),
                            m.birthday != null ? m.birthday : ""
                        });
                    }
                    System.out.println("表格刷新完成 - 行数: " + memberTableModel.getRowCount());

                    // 刷新会员选择下拉框
                    memberComboBox.removeAllItems();
                    memberComboBox.addItem("无会员");
                    for (Member m : members.values()) {
                        memberComboBox.addItem(m.phone + " - " + m.name + " (" + m.level + ")");
                    }

                    addDialog.dispose();
                    JOptionPane.showMessageDialog(this, "会员添加成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(addDialog, "请输入有效的数值！", "错误", JOptionPane.ERROR_MESSAGE);
                }
            });

            cancelButton.addActionListener(e1 -> addDialog.dispose());

            addDialog.setVisible(true);
        });

        // 编辑会员
        editButton.addActionListener(e -> {
            int selectedRow = memberTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog, "请先选择要编辑的会员！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String phone = (String) memberTableModel.getValueAt(selectedRow, 0);
            Member selectedMember = members.get(phone);

            JDialog editDialog = new JDialog(dialog, "编辑会员", true);
            editDialog.setSize(400, 350);
            editDialog.setLocationRelativeTo(dialog);

            JPanel editPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0;
            gbc.gridy = 0;
            editPanel.add(new JLabel("手机号:"), gbc);

            gbc.gridx = 1;
            JTextField phoneField = new JTextField(20);
            phoneField.setText(selectedMember.phone);
            phoneField.setEditable(false);
            editPanel.add(phoneField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            editPanel.add(new JLabel("姓名:"), gbc);

            gbc.gridx = 1;
            JTextField nameField = new JTextField(20);
            nameField.setText(selectedMember.name);
            nameField.setFont(getGeneralFont(Font.PLAIN, 13));
            editPanel.add(nameField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            editPanel.add(new JLabel("积分:"), gbc);

            gbc.gridx = 1;
            JTextField pointsField = new JTextField(20);
            pointsField.setText(String.format("%.0f", selectedMember.points));
            pointsField.setFont(getGeneralFont(Font.PLAIN, 13));
            editPanel.add(pointsField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 3;
            editPanel.add(new JLabel("余额:"), gbc);

            gbc.gridx = 1;
            JTextField balanceField = new JTextField(20);
            balanceField.setText(String.format("%.2f", selectedMember.balance));
            balanceField.setFont(getGeneralFont(Font.PLAIN, 13));
            editPanel.add(balanceField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 4;
            editPanel.add(new JLabel("生日(MM-dd):"), gbc);

            gbc.gridx = 1;
            JTextField birthdayField = new JTextField(20);
            birthdayField.setText(selectedMember.birthday != null ? selectedMember.birthday : "");
            birthdayField.setFont(getGeneralFont(Font.PLAIN, 13));
            editPanel.add(birthdayField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 5;
            gbc.gridwidth = 2;
            JPanel confirmPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

            JButton confirmButton = createStyledButton("确定", SUCCESS_COLOR);
            JButton cancelButton = createStyledButton("取消", GRAY_COLOR);

            confirmPanel.add(confirmButton);
            confirmPanel.add(cancelButton);

            editPanel.add(confirmPanel, gbc);

            editDialog.add(editPanel);

            confirmButton.addActionListener(e1 -> {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(editDialog, "姓名不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    double points = Double.parseDouble(pointsField.getText());
                    if (points < 0) {
                        JOptionPane.showMessageDialog(editDialog, "积分不能为负数！", "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    double balance = Double.parseDouble(balanceField.getText().trim());
                    if (balance < 0) {
                        JOptionPane.showMessageDialog(editDialog, "余额不能为负数！", "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    String birthday = birthdayField.getText().trim();

                    selectedMember.name = name;
                    selectedMember.points = points;
                    selectedMember.balance = balance;
                    selectedMember.birthday = birthday.isEmpty() ? null : birthday;
                    selectedMember.updateLevel();
                    saveData();

                    // 刷新表格
                    memberTableModel.setRowCount(0);
                    for (Member m : members.values()) {
                        memberTableModel.addRow(new Object[]{
                            m.phone, m.name,
                            String.format("%.0f", m.points),
                            m.level,
                            String.format("%.1f折", m.discount * 10),
                            String.format("%.2f", m.balance),
                            m.birthday != null ? m.birthday : ""
                        });
                    }

                    // 刷新会员选择下拉框
                    memberComboBox.removeAllItems();
                    memberComboBox.addItem("无会员");
                    for (Member m : members.values()) {
                        memberComboBox.addItem(m.phone + " - " + m.name + " (" + m.level + ")");
                    }

                    editDialog.dispose();
                    JOptionPane.showMessageDialog(this, "会员信息修改成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(editDialog, "请输入有效的数值！", "错误", JOptionPane.ERROR_MESSAGE);
                }
            });

            cancelButton.addActionListener(e1 -> editDialog.dispose());

            editDialog.setVisible(true);
        });

        // 删除会员
        deleteButton.addActionListener(e -> {
            int selectedRow = memberTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog, "请先选择要删除的会员！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String phone = (String) memberTableModel.getValueAt(selectedRow, 0);

            int confirm = JOptionPane.showConfirmDialog(dialog,
                "确定要删除会员 \"" + phone + "\" 吗？",
                "确认删除", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                members.remove(phone);
                saveData();

                // 刷新表格
                memberTableModel.setRowCount(0);
                for (Member member : members.values()) {
                    memberTableModel.addRow(new Object[]{
                        member.phone, member.name,
                        String.format("%.0f", member.points),
                        member.level,
                        String.format("%.1f折", member.discount * 10),
                        String.format("%.2f", member.balance),
                        member.birthday != null ? member.birthday : ""
                    });
                }

                // 刷新会员选择下拉框
                memberComboBox.removeAllItems();
                memberComboBox.addItem("无会员");
                for (Member m : members.values()) {
                    memberComboBox.addItem(m.phone + " - " + m.name + " (" + m.level + ")");
                }

                JOptionPane.showMessageDialog(this, "会员删除成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // 充值按钮
        rechargeButton.addActionListener(e -> {
            int selectedRow = memberTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "请先选择要充值的会员！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String phone = (String) memberTableModel.getValueAt(selectedRow, 0);
            Member member = members.get(phone);
            showRechargeDialog(member, memberTableModel, memberComboBox);
        });

        // 消费记录按钮
        recordButton.addActionListener(e -> {
            int selectedRow = memberTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "请先选择要查看消费记录的会员！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String phone = (String) memberTableModel.getValueAt(selectedRow, 0);
            Member member = members.get(phone);
            showMemberConsumptionRecords(member);
        });

        // 报表统计按钮
        statsButton.addActionListener(e -> {
            showMemberStatistics();
        });

        // 刷新按钮
        refreshButton.addActionListener(e -> {
            System.out.println("手动刷新会员列表");
            System.out.println("刷新前会员数量: " + members.size());
            for (String phone : members.keySet()) {
                System.out.println("  " + phone + " - " + members.get(phone).name);
            }
            
            // 重新加载会员数据
            Map<String, Member> loadedMembers = DataManager.loadMembers();
            members.clear();
            members.putAll(loadedMembers);
            System.out.println("重新加载后会员数量: " + members.size());
            
            // 刷新表格
            memberTableModel.setRowCount(0);
            for (Member m : members.values()) {
                memberTableModel.addRow(new Object[]{
                    m.phone, m.name,
                    String.format("%.0f", m.points),
                    m.level,
                    String.format("%.1f折", m.discount * 10),
                    String.format("%.2f", m.balance),
                    m.birthday != null ? m.birthday : ""
                });
            }
            System.out.println("表格刷新完成，行数: " + memberTableModel.getRowCount());
            
            // 刷新会员选择下拉框（如果传入的话）
            if (memberComboBox != null) {
                memberComboBox.removeAllItems();
                memberComboBox.addItem("无会员");
                for (Member m : members.values()) {
                    memberComboBox.addItem(m.phone + " - " + m.name + " (" + m.level + ")");
                }
            }
            
            JOptionPane.showMessageDialog(dialog, "会员列表已刷新！当前会员数: " + members.size(), "刷新成功", JOptionPane.INFORMATION_MESSAGE);
        });

        closeButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void updateTaxRateLabel() {
        taxRateLabel.setText("当前税率: " + String.format("%.1f%%", taxRate * 100));
    }

    private void batchDeleteProducts() {
        int[] selectedRows = inventoryTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "请先选择要删除的商品！", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "确定要删除选中的 " + selectedRows.length + " 个商品吗？",
            "确认删除", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            List<String> productsToDelete = new ArrayList<>();
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                String productName = (String) inventoryTableModel.getValueAt(selectedRows[i], 0);
                productsToDelete.add(productName);
            }

            for (String productName : productsToDelete) {
                inventory.remove(productName);
            }

            saveData();
            refreshInventoryTable();
            JOptionPane.showMessageDialog(this, "成功删除 " + productsToDelete.size() + " 个商品！", "成功", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void batchRestockProducts() {
        int[] selectedRows = inventoryTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "请先选择要补货的商品！", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // 创建补货对话框
        JDialog dialog = new JDialog(this, "批量补货", true);
        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("补货数量:"), gbc);

        gbc.gridx = 1;
        JTextField quantityField = new JTextField(15);
        panel.add(quantityField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        JButton confirmButton = createStyledButton("确定", SUCCESS_COLOR);
        JButton cancelButton = createStyledButton("取消", GRAY_COLOR);

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        panel.add(buttonPanel, gbc);

        dialog.add(panel);

        confirmButton.addActionListener(e -> {
            try {
                int quantity = Integer.parseInt(quantityField.getText());
                if (quantity <= 0) {
                    JOptionPane.showMessageDialog(dialog, "补货数量必须大于0！", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                for (int row : selectedRows) {
                    String productName = (String) inventoryTableModel.getValueAt(row, 0);
                    Product product = inventory.get(productName);
                    if (product != null) {
                        product.quantity += quantity;
                    }
                }

                saveData();
                refreshInventoryTable();
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "成功为 " + selectedRows.length + " 个商品补货 " + quantity + " 件！", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "请输入有效的整数！", "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void showPromotionManagementDialog() {
        JDialog dialog = new JDialog(this, "促销管理", true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 顶部按钮面板
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JButton addButton = createStyledButton("添加促销", SUCCESS_COLOR);
        JButton editButton = createStyledButton("编辑促销", INFO_COLOR);
        JButton deleteButton = createStyledButton("删除促销", DANGER_COLOR);
        JButton selectButton = createStyledButton("选择促销", WARNING_COLOR);
        JButton clearButton = createStyledButton("清除选择", GRAY_COLOR);

        topPanel.add(addButton);
        topPanel.add(editButton);
        topPanel.add(deleteButton);
        topPanel.add(selectButton);
        topPanel.add(clearButton);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // 促销列表
        String[] columns = {"促销名称", "类型", "门槛/条件", "折扣值", "状态", "有效期", "使用次数"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        styleTable(table, tableModel);

        refreshPromotionTable(tableModel);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // 当前选择的促销
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JLabel currentLabel = new JLabel("当前促销: ");
        JLabel currentPromotionLabel = new JLabel("无");
        currentPromotionLabel.setFont(getChineseFont(Font.BOLD, 12));
        currentPromotionLabel.setForeground(PRIMARY_COLOR);
        bottomPanel.add(currentLabel);
        bottomPanel.add(currentPromotionLabel);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);

        // 添加促销
        addButton.addActionListener(e -> {
            showAddPromotionDialog(tableModel);
            updateCurrentPromotionLabel(currentPromotionLabel);
        });

        // 编辑促销
        editButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog, "请先选择一个促销！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String promotionId = (String) tableModel.getValueAt(selectedRow, 0);
            Promotion promotion = getPromotionById(promotionId);
            if (promotion != null) {
                showEditPromotionDialog(promotion, tableModel);
                updateCurrentPromotionLabel(currentPromotionLabel);
            }
        });

        // 删除促销
        deleteButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog, "请先选择一个促销！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(dialog, "确定要删除选中的促销吗？", "确认删除", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                String promotionId = (String) tableModel.getValueAt(selectedRow, 0);
                promotions.removeIf(p -> p.id.equals(promotionId));
                if (currentPromotion != null && currentPromotion.id.equals(promotionId)) {
                    currentPromotion = null;
                }
                saveData();
                refreshPromotionTable(tableModel);
                updateCurrentPromotionLabel(currentPromotionLabel);
                JOptionPane.showMessageDialog(dialog, "促销删除成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // 选择促销
        selectButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog, "请先选择一个促销！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String promotionId = (String) tableModel.getValueAt(selectedRow, 0);
            Promotion promotion = getPromotionById(promotionId);
            if (promotion != null) {
                currentPromotion = promotion;
                updateCurrentPromotionLabel(currentPromotionLabel);
                JOptionPane.showMessageDialog(dialog, "已选择促销: " + promotion.name, "成功", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // 清除选择
        clearButton.addActionListener(e -> {
            currentPromotion = null;
            updateCurrentPromotionLabel(currentPromotionLabel);
            JOptionPane.showMessageDialog(dialog, "已清除促销选择", "提示", JOptionPane.INFORMATION_MESSAGE);
        });

        dialog.setVisible(true);
    }

    private void refreshPromotionTable(DefaultTableModel tableModel) {
        tableModel.setRowCount(0);
        for (Promotion promotion : promotions) {
            String status = promotion.enabled ? "启用" : "禁用";
            String validity = promotion.isValid() ? "有效" : "已过期";
            String usage = promotion.maxUsage == -1 ? "无限制" : promotion.usageCount + "/" + promotion.maxUsage;

            String condition = "";
            String discountValue = "";
            switch (promotion.type) {
                case "满减":
                    condition = "满" + String.format("%.2f", promotion.threshold) + "元";
                    discountValue = "减" + String.format("%.2f", promotion.discount) + "元";
                    break;
                case "打折":
                    condition = "满" + String.format("%.2f", promotion.threshold) + "元";
                    discountValue = String.format("%.0f", promotion.discount * 10) + "折";
                    break;
                case "优惠券":
                    condition = "无门槛";
                    discountValue = String.format("%.2f", promotion.discount) + "元";
                    break;
            }

            tableModel.addRow(new Object[]{
                promotion.id,
                promotion.name,
                promotion.type,
                condition,
                discountValue,
                status + "(" + validity + ")",
                usage
            });
        }
    }

    private Promotion getPromotionById(String id) {
        for (Promotion promotion : promotions) {
            if (promotion.id.equals(id)) {
                return promotion;
            }
        }
        return null;
    }

    private void updateCurrentPromotionLabel(JLabel label) {
        if (currentPromotion != null) {
            label.setText(currentPromotion.name + " (" + currentPromotion.type + ")");
            label.setForeground(SUCCESS_COLOR);
        } else {
            label.setText("无");
            label.setForeground(GRAY_COLOR);
        }
    }

    private void showAddPromotionDialog(DefaultTableModel tableModel) {
        JDialog dialog = new JDialog(this, "添加促销", true);
        dialog.setSize(450, 400);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("促销名称:"), gbc);

        gbc.gridx = 1;
        JTextField nameField = new JTextField(20);
        nameField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("促销类型:"), gbc);

        gbc.gridx = 1;
        JComboBox<String> typeComboBox = new JComboBox<>(new String[]{"满减", "打折", "优惠券"});
        typeComboBox.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(typeComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("门槛金额(元):"), gbc);

        gbc.gridx = 1;
        JTextField thresholdField = new JTextField(20);
        thresholdField.setText("0");
        thresholdField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(thresholdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("折扣值:"), gbc);

        gbc.gridx = 1;
        JTextField discountField = new JTextField(20);
        discountField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(discountField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("描述:"), gbc);

        gbc.gridx = 1;
        JTextField descriptionField = new JTextField(20);
        descriptionField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(descriptionField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(new JLabel("最大使用次数:"), gbc);

        gbc.gridx = 1;
        JTextField maxUsageField = new JTextField(20);
        maxUsageField.setText("-1");
        maxUsageField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(maxUsageField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        JButton confirmButton = createStyledButton("确定", SUCCESS_COLOR);
        JButton cancelButton = createStyledButton("取消", GRAY_COLOR);

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        panel.add(buttonPanel, gbc);

        // 提示标签
        gbc.gridx = 0;
        gbc.gridy = 7;
        JLabel tipLabel = new JLabel("提示: 满减和打折需要设置门槛金额，优惠券无门槛");
        tipLabel.setFont(getChineseFont(Font.PLAIN, 11));
        tipLabel.setForeground(GRAY_COLOR);
        panel.add(tipLabel, gbc);

        dialog.add(panel);

        // 类型变化时更新提示
        typeComboBox.addActionListener(e -> {
            String type = (String) typeComboBox.getSelectedItem();
            if ("优惠券".equals(type)) {
                thresholdField.setEnabled(false);
                thresholdField.setText("0");
                tipLabel.setText("提示: 优惠券面额为固定金额，无使用门槛");
            } else {
                thresholdField.setEnabled(true);
                if ("满减".equals(type)) {
                    tipLabel.setText("提示: 满X元减Y元");
                } else {
                    tipLabel.setText("提示: 满X元打Y折（如0.9表示9折）");
                }
            }
        });

        confirmButton.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "促销名称不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String type = (String) typeComboBox.getSelectedItem();
                double threshold = Double.parseDouble(thresholdField.getText());
                double discount = Double.parseDouble(discountField.getText());
                String description = descriptionField.getText().trim();
                int maxUsage = Integer.parseInt(maxUsageField.getText());

                if (discount <= 0) {
                    JOptionPane.showMessageDialog(dialog, "折扣值必须大于0！", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // 根据促销类型进行不同的验证
                if ("满减".equals(type)) {
                    if (threshold <= 0) {
                        JOptionPane.showMessageDialog(dialog, "门槛金额必须大于0！", "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (discount >= threshold) {
                        JOptionPane.showMessageDialog(dialog, "满减金额不能大于或等于门槛金额！", "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else if ("打折".equals(type)) {
                    if (threshold <= 0) {
                        JOptionPane.showMessageDialog(dialog, "门槛金额必须大于0！", "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (discount <= 0 || discount >= 1) {
                        JOptionPane.showMessageDialog(dialog, "折扣值必须在0和1之间（如0.9表示9折）！", "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else if ("优惠券".equals(type)) {
                    // 优惠券无门槛，门槛值必须为0
                    if (threshold != 0) {
                        JOptionPane.showMessageDialog(dialog, "优惠券的门槛金额必须为0！", "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                // 创建促销
                Promotion promotion = new Promotion();
                promotion.id = "P" + String.format("%06d", promotions.size() + 1);
                promotion.name = name;
                promotion.type = type;
                promotion.threshold = threshold;
                promotion.discount = discount;
                promotion.description = description;
                promotion.maxUsage = maxUsage;

                promotions.add(promotion);
                saveData();
                refreshPromotionTable(tableModel);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "促销添加成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "请输入有效的数字！", "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void showEditPromotionDialog(Promotion promotion, DefaultTableModel tableModel) {
        JDialog dialog = new JDialog(this, "编辑促销", true);
        dialog.setSize(450, 400);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("促销名称:"), gbc);

        gbc.gridx = 1;
        JTextField nameField = new JTextField(20);
        nameField.setText(promotion.name);
        nameField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("促销类型:"), gbc);

        gbc.gridx = 1;
        JComboBox<String> typeComboBox = new JComboBox<>(new String[]{"满减", "打折", "优惠券"});
        typeComboBox.setSelectedItem(promotion.type);
        typeComboBox.setEnabled(false); // 类型不可修改
        typeComboBox.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(typeComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("门槛金额(元):"), gbc);

        gbc.gridx = 1;
        JTextField thresholdField = new JTextField(20);
        thresholdField.setText(String.valueOf(promotion.threshold));
        thresholdField.setFont(getGeneralFont(Font.PLAIN, 13));
        if ("优惠券".equals(promotion.type)) {
            thresholdField.setEnabled(false);
        }
        panel.add(thresholdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("折扣值:"), gbc);

        gbc.gridx = 1;
        JTextField discountField = new JTextField(20);
        discountField.setText(String.valueOf(promotion.discount));
        discountField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(discountField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("描述:"), gbc);

        gbc.gridx = 1;
        JTextField descriptionField = new JTextField(20);
        descriptionField.setText(promotion.description);
        descriptionField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(descriptionField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(new JLabel("启用状态:"), gbc);

        gbc.gridx = 1;
        JCheckBox enabledCheckBox = new JCheckBox("启用");
        enabledCheckBox.setSelected(promotion.enabled);
        panel.add(enabledCheckBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        JButton confirmButton = createStyledButton("保存", SUCCESS_COLOR);
        JButton cancelButton = createStyledButton("取消", GRAY_COLOR);

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        panel.add(buttonPanel, gbc);

        dialog.add(panel);

        confirmButton.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "促销名称不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                double threshold = Double.parseDouble(thresholdField.getText());
                double discount = Double.parseDouble(discountField.getText());
                String description = descriptionField.getText().trim();

                if (discount <= 0) {
                    JOptionPane.showMessageDialog(dialog, "折扣值必须大于0！", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // 根据促销类型进行不同的验证
                if ("满减".equals(promotion.type)) {
                    if (threshold <= 0) {
                        JOptionPane.showMessageDialog(dialog, "门槛金额必须大于0！", "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (discount >= threshold) {
                        JOptionPane.showMessageDialog(dialog, "满减金额不能大于或等于门槛金额！", "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else if ("打折".equals(promotion.type)) {
                    if (threshold <= 0) {
                        JOptionPane.showMessageDialog(dialog, "门槛金额必须大于0！", "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (discount <= 0 || discount >= 1) {
                        JOptionPane.showMessageDialog(dialog, "折扣值必须在0和1之间（如0.9表示9折）！", "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else if ("优惠券".equals(promotion.type)) {
                    if (threshold != 0) {
                        JOptionPane.showMessageDialog(dialog, "优惠券的门槛金额必须为0！", "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                // 更新促销
                promotion.name = name;
                promotion.threshold = threshold;
                promotion.discount = discount;
                promotion.description = description;
                promotion.enabled = enabledCheckBox.isSelected();

                saveData();
                refreshPromotionTable(tableModel);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "促销更新成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "请输入有效的数字！", "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private JPanel createStatisticsPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 顶部统计卡片
        JPanel topPanel = new JPanel(new GridLayout(2, 3, 15, 15));

        // 销售统计卡片
        JPanel salesCard = createStatCard("销售统计", SUCCESS_COLOR);
        topPanel.add(salesCard);

        // 库存统计卡片
        JPanel inventoryCard = createStatCard("库存统计", INFO_COLOR);
        topPanel.add(inventoryCard);

        // 会员统计卡片
        JPanel memberCard = createStatCard("会员统计", PURPLE_COLOR);
        topPanel.add(memberCard);

        // 交易统计卡片
        JPanel transactionCard = createStatCard("交易统计", WARNING_COLOR);
        topPanel.add(transactionCard);

        // 促销统计卡片
        JPanel promotionCard = createStatCard("促销统计", DANGER_COLOR);
        topPanel.add(promotionCard);

        // 收入统计卡片
        JPanel revenueCard = createStatCard("收入统计", PRIMARY_COLOR);
        topPanel.add(revenueCard);

        panel.add(topPanel, BorderLayout.NORTH);

        // 底部按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        JButton salesReportButton = createStyledButton("销售报表", SUCCESS_COLOR);
        JButton inventoryReportButton = createStyledButton("库存报表", INFO_COLOR);
        JButton memberReportButton = createStyledButton("会员报表", PURPLE_COLOR);
        JButton refreshButton = createStyledButton("刷新数据", GRAY_COLOR);

        buttonPanel.add(salesReportButton);
        buttonPanel.add(inventoryReportButton);
        buttonPanel.add(memberReportButton);
        buttonPanel.add(refreshButton);

        panel.add(buttonPanel, BorderLayout.CENTER);

        // 初始化统计数据
        updateStatistics(salesCard, inventoryCard, memberCard, transactionCard, promotionCard, revenueCard);

        // 按钮事件
        salesReportButton.addActionListener(e -> showSalesReport());
        inventoryReportButton.addActionListener(e -> showInventoryReport());
        memberReportButton.addActionListener(e -> showMemberReport());
        refreshButton.addActionListener(e -> updateStatistics(salesCard, inventoryCard, memberCard, transactionCard, promotionCard, revenueCard));

        return panel;
    }

    private JPanel createStatCard(String title, Color color) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        card.setBackground(CARD_BACKGROUND);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(getChineseFont(Font.BOLD, 14));
        titleLabel.setForeground(PRIMARY_DARK);
        card.add(titleLabel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        contentPanel.setBackground(CARD_BACKGROUND);
        card.add(contentPanel, BorderLayout.CENTER);

        return card;
    }

    private void updateStatistics(JPanel salesCard, JPanel inventoryCard, JPanel memberCard,
                                  JPanel transactionCard, JPanel promotionCard, JPanel revenueCard) {
        // 清空旧数据
        clearStatCard(salesCard);
        clearStatCard(inventoryCard);
        clearStatCard(memberCard);
        clearStatCard(transactionCard);
        clearStatCard(promotionCard);
        clearStatCard(revenueCard);

        // 计算销售统计
        double todaySales = calculateTodaySales();
        double monthSales = calculateMonthSales();
        double totalSales = calculateTotalSales();
        int todayOrders = calculateTodayOrders();

        addStatItem(salesCard, "今日销售额: ¥" + String.format("%.2f", todaySales));
        addStatItem(salesCard, "本月销售额: ¥" + String.format("%.2f", monthSales));
        addStatItem(salesCard, "总销售额: ¥" + String.format("%.2f", totalSales));
        addStatItem(salesCard, "今日订单: " + todayOrders + "单");

        // 计算库存统计
        int totalProducts = inventory.size();
        int outOfStock = countOutOfStock();
        int lowStock = countLowStock();
        double inventoryValue = calculateInventoryValue();

        addStatItem(inventoryCard, "商品总数: " + totalProducts + "种");
        addStatItem(inventoryCard, "库存总值: ¥" + String.format("%.2f", inventoryValue));
        addStatItem(inventoryCard, "缺货商品: " + outOfStock + "种");
        addStatItem(inventoryCard, "低库存: " + lowStock + "种");

        // 计算会员统计
        int totalMembers = members.size();
        int activeMembers = countActiveMembers();
        int diamondMembers = countMembersByLevel("钻石");
        int goldMembers = countMembersByLevel("金卡");

        addStatItem(memberCard, "会员总数: " + totalMembers + "人");
        addStatItem(memberCard, "活跃会员: " + activeMembers + "人");
        addStatItem(memberCard, "钻石会员: " + diamondMembers + "人");
        addStatItem(memberCard, "金卡会员: " + goldMembers + "人");

        // 计算交易统计
        int totalTransactions = transactions.size();
        double avgOrderValue = totalTransactions > 0 ? totalSales / totalTransactions : 0;
        double taxAmount = calculateTotalTax();

        addStatItem(transactionCard, "总交易数: " + totalTransactions + "笔");
        addStatItem(transactionCard, "平均客单价: ¥" + String.format("%.2f", avgOrderValue));
        addStatItem(transactionCard, "总税费: ¥" + String.format("%.2f", taxAmount));
        addStatItem(transactionCard, "交易率: " + String.format("%.1f%%", calculateTransactionRate()));

        // 计算促销统计
        int totalPromotions = promotions.size();
        int activePromotions = countActivePromotions();
        int usedPromotions = countUsedPromotions();
        double promotionDiscount = calculateTotalPromotionDiscount();

        addStatItem(promotionCard, "促销规则: " + totalPromotions + "条");
        addStatItem(promotionCard, "生效促销: " + activePromotions + "条");
        addStatItem(promotionCard, "已使用: " + usedPromotions + "次");
        addStatItem(promotionCard, "优惠总额: ¥" + String.format("%.2f", promotionDiscount));

        // 计算收入统计
        double netRevenue = calculateNetRevenue();
        double memberRevenue = calculateMemberRevenue();
        double promotionRevenue = calculatePromotionRevenue();
        double avgDailyRevenue = calculateAvgDailyRevenue();

        addStatItem(revenueCard, "净收入: ¥" + String.format("%.2f", netRevenue));
        addStatItem(revenueCard, "会员收入: ¥" + String.format("%.2f", memberRevenue));
        addStatItem(revenueCard, "促销收入: ¥" + String.format("%.2f", promotionRevenue));
        addStatItem(revenueCard, "日均收入: ¥" + String.format("%.2f", avgDailyRevenue));
    }

    private void clearStatCard(JPanel card) {
        JPanel contentPanel = (JPanel) card.getComponent(1);
        contentPanel.removeAll();
    }

    private void addStatItem(JPanel card, String text) {
        JPanel contentPanel = (JPanel) card.getComponent(1);
        JLabel label = new JLabel(text);
        label.setFont(getGeneralFont(Font.PLAIN, 12));
        label.setForeground(TEXT_COLOR);
        contentPanel.add(label);
    }

    private double calculateTodaySales() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(new Date());

        double total = 0;
        for (Transaction t : transactions) {
            if (t.timestamp.startsWith(today)) {
                total += t.finalAmount;
            }
        }
        return total;
    }

    private double calculateMonthSales() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
        String currentMonth = sdf.format(new Date());

        double total = 0;
        for (Transaction t : transactions) {
            if (t.timestamp.startsWith(currentMonth)) {
                total += t.finalAmount;
            }
        }
        return total;
    }

    private double calculateTotalSales() {
        double total = 0;
        for (Transaction t : transactions) {
            total += t.finalAmount;
        }
        return total;
    }

    // 计算班次期间各种支付方式的收入
    private Map<String, Double> calculateShiftPaymentRevenue() {
        Map<String, Double> revenue = new HashMap<>();
        revenue.put("现金", 0.0);
        revenue.put("微信支付", 0.0);
        revenue.put("支付宝", 0.0);
        revenue.put("银行卡", 0.0);
        revenue.put("组合支付", 0.0);

        if (currentShift == null) {
            return revenue;
        }

        // 计算班次开始后的交易
        for (Transaction t : transactions) {
            if (t.timestamp.compareTo(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(currentShift.startTime)) >= 0) {
                if (t.paymentMethod != null && !t.paymentMethod.isEmpty()) {
                    if (t.paymentMethod.equals("现金")) {
                        revenue.put("现金", revenue.get("现金") + t.finalAmount);
                    } else if (t.paymentMethod.equals("微信支付")) {
                        revenue.put("微信支付", revenue.get("微信支付") + t.finalAmount);
                    } else if (t.paymentMethod.equals("支付宝")) {
                        revenue.put("支付宝", revenue.get("支付宝") + t.finalAmount);
                    } else if (t.paymentMethod.equals("银行卡")) {
                        revenue.put("银行卡", revenue.get("银行卡") + t.finalAmount);
                    } else if (t.paymentMethod.startsWith("组合支付")) {
                        revenue.put("组合支付", revenue.get("组合支付") + t.finalAmount);
                    }
                }
            }
        }

        return revenue;
    }

    private int calculateTodayOrders() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(new Date());

        int count = 0;
        for (Transaction t : transactions) {
            if (t.timestamp.startsWith(today)) {
                count++;
            }
        }
        return count;
    }

    private int countOutOfStock() {
        int count = 0;
        for (Product p : inventory.values()) {
            if (p.quantity <= 0) {
                count++;
            }
        }
        return count;
    }

    private int countLowStock() {
        int count = 0;
        for (Product p : inventory.values()) {
            if (p.quantity > 0 && p.quantity <= p.minStock) {
                count++;
            }
        }
        return count;
    }

    private double calculateInventoryValue() {
        double total = 0;
        for (Product p : inventory.values()) {
            total += p.price * p.quantity;
        }
        return total;
    }

    private int countActiveMembers() {
        int count = 0;
        for (Member m : members.values()) {
            if (m.points > 0) {
                count++;
            }
        }
        return count;
    }

    private int countMembersByLevel(String level) {
        int count = 0;
        for (Member m : members.values()) {
            if (m.level.equals(level)) {
                count++;
            }
        }
        return count;
    }

    private double calculateTotalTax() {
        double total = 0;
        for (Transaction t : transactions) {
            total += t.tax;
        }
        return total;
    }

    private double calculateTransactionRate() {
        if (inventory.isEmpty()) return 0;
        return (double) transactions.size() / inventory.size() * 100;
    }

    private int countActivePromotions() {
        int count = 0;
        for (Promotion p : promotions) {
            if (p.enabled && p.isValid()) {
                count++;
            }
        }
        return count;
    }

    private int countUsedPromotions() {
        int count = 0;
        for (Promotion p : promotions) {
            count += p.usageCount;
        }
        return count;
    }

    private double calculateTotalPromotionDiscount() {
        // 这里简化处理，实际需要从交易记录中计算
        return 0;
    }

    private double calculateNetRevenue() {
        return calculateTotalSales();
    }

    private double calculateMemberRevenue() {
        // 简化处理
        return calculateTotalSales() * 0.3;
    }

    private double calculatePromotionRevenue() {
        // 简化处理
        return calculateTotalSales() * 0.2;
    }

    private double calculateAvgDailyRevenue() {
        if (transactions.isEmpty()) return 0;
        return calculateTotalSales() / 30; // 假设30天
    }

    private void showSalesReport() {
        JDialog dialog = new JDialog(this, "销售报表", true);
        dialog.setSize(1100, 700);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 筛选条件面板
        JPanel filterPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 第一行：日期范围
        gbc.gridx = 0;
        gbc.gridy = 0;
        filterPanel.add(new JLabel("开始日期:"), gbc);

        gbc.gridx = 1;
        JTextField startDateField = new JTextField(12);
        startDateField.setToolTipText("格式: yyyy-MM-dd HH:mm:ss");
        filterPanel.add(startDateField, gbc);

        gbc.gridx = 2;
        filterPanel.add(new JLabel("结束日期:"), gbc);

        gbc.gridx = 3;
        JTextField endDateField = new JTextField(12);
        endDateField.setToolTipText("格式: yyyy-MM-dd HH:mm:ss");
        filterPanel.add(endDateField, gbc);

        // 第二行：金额范围
        gbc.gridx = 0;
        gbc.gridy = 1;
        filterPanel.add(new JLabel("最小金额:"), gbc);

        gbc.gridx = 1;
        JTextField minAmountField = new JTextField(12);
        minAmountField.setToolTipText("最小订单金额");
        filterPanel.add(minAmountField, gbc);

        gbc.gridx = 2;
        filterPanel.add(new JLabel("最大金额:"), gbc);

        gbc.gridx = 3;
        JTextField maxAmountField = new JTextField(12);
        maxAmountField.setToolTipText("最大订单金额");
        filterPanel.add(maxAmountField, gbc);

        // 第三行：商品名称
        gbc.gridx = 0;
        gbc.gridy = 2;
        filterPanel.add(new JLabel("商品名称:"), gbc);

        gbc.gridx = 1;
        JTextField productNameField = new JTextField(12);
        productNameField.setToolTipText("输入商品名称模糊搜索");
        filterPanel.add(productNameField, gbc);

        gbc.gridx = 2;
        filterPanel.add(new JLabel("会员手机:"), gbc);

        gbc.gridx = 3;
        JTextField memberPhoneField = new JTextField(12);
        memberPhoneField.setToolTipText("输入会员手机号");
        filterPanel.add(memberPhoneField, gbc);

        // 第四行：按钮
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 4;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        JButton queryButton = createStyledButton("查询", SUCCESS_COLOR);
        JButton resetButton = createStyledButton("重置", GRAY_COLOR);
        JButton exportButton = createStyledButton("导出Excel", INFO_COLOR);

        buttonPanel.add(queryButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(exportButton);

        filterPanel.add(buttonPanel, gbc);

        panel.add(filterPanel, BorderLayout.NORTH);

        // 表格
        String[] columns = {"交易ID", "时间", "金额", "税费", "实收", "商品数", "会员"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);
        styleTable(table, tableModel);

        // 设置行高
        table.setRowHeight(30);

        // 设置列宽
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        table.getColumnModel().getColumn(5).setPreferredWidth(60);
        table.getColumnModel().getColumn(6).setPreferredWidth(100);

        // 加载所有交易数据
        loadSalesData(tableModel, transactions);

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 底部统计
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        JLabel totalCountLabel = new JLabel("总交易数: 0笔");
        JLabel totalAmountLabel = new JLabel("总金额: ¥0.00");
        JLabel avgAmountLabel = new JLabel("平均金额: ¥0.00");

        statsPanel.add(totalCountLabel);
        statsPanel.add(totalAmountLabel);
        statsPanel.add(avgAmountLabel);

        panel.add(statsPanel, BorderLayout.SOUTH);

        dialog.add(panel);

        // 查询按钮事件
        queryButton.addActionListener(e -> {
            String startDate = startDateField.getText().trim();
            String endDate = endDateField.getText().trim();
            String minAmountText = minAmountField.getText().trim();
            String maxAmountText = maxAmountField.getText().trim();
            String productName = productNameField.getText().trim();
            String memberPhone = memberPhoneField.getText().trim();

            // 筛选交易
            List<Transaction> filteredTransactions = new ArrayList<>();
            for (Transaction t : transactions) {
                boolean match = true;

                // 日期范围筛选
                if (!startDate.isEmpty() && t.timestamp.compareTo(startDate) < 0) {
                    match = false;
                }
                if (!endDate.isEmpty() && t.timestamp.compareTo(endDate) > 0) {
                    match = false;
                }

                // 金额范围筛选
                try {
                    if (!minAmountText.isEmpty()) {
                        double minAmount = Double.parseDouble(minAmountText);
                        if (t.finalAmount < minAmount) {
                            match = false;
                        }
                    }
                    if (!maxAmountText.isEmpty()) {
                        double maxAmount = Double.parseDouble(maxAmountText);
                        if (t.finalAmount > maxAmount) {
                            match = false;
                        }
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "金额格式错误！", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // 商品名称筛选
                if (!productName.isEmpty()) {
                    boolean hasProduct = false;
                    for (Product p : t.items) {
                        if (p.name.toLowerCase().contains(productName.toLowerCase())) {
                            hasProduct = true;
                            break;
                        }
                    }
                    if (!hasProduct) {
                        match = false;
                    }
                }

                // 会员手机筛选（需要从交易记录中获取会员信息，这里暂时跳过）
                // TODO: 需要在Transaction类中添加会员信息字段

                if (match) {
                    filteredTransactions.add(t);
                }
            }

            // 加载筛选后的数据
            loadSalesData(tableModel, filteredTransactions);

            // 更新统计信息
            updateSalesStats(totalCountLabel, totalAmountLabel, avgAmountLabel, filteredTransactions);
        });

        // 重置按钮事件
        resetButton.addActionListener(e -> {
            startDateField.setText("");
            endDateField.setText("");
            minAmountField.setText("");
            maxAmountField.setText("");
            productNameField.setText("");
            memberPhoneField.setText("");

            loadSalesData(tableModel, transactions);
            updateSalesStats(totalCountLabel, totalAmountLabel, avgAmountLabel, transactions);
        });

        // 导出按钮事件
        exportButton.addActionListener(e -> {
            exportSalesData(tableModel);
        });

        // 初始化统计信息
        updateSalesStats(totalCountLabel, totalAmountLabel, avgAmountLabel, transactions);

        dialog.setVisible(true);
    }

    private void loadSalesData(DefaultTableModel tableModel, List<Transaction> transactionList) {
        tableModel.setRowCount(0);
        for (Transaction t : transactionList) {
            int itemCount = t.items.size();
            tableModel.addRow(new Object[]{
                t.transactionId,
                t.timestamp,
                String.format("%.2f", t.totalAmount),
                String.format("%.2f", t.tax),
                String.format("%.2f", t.finalAmount),
                itemCount,
                "-"  // TODO: 需要从交易记录中获取会员信息
            });
        }
    }

    private void updateSalesStats(JLabel totalCountLabel, JLabel totalAmountLabel, JLabel avgAmountLabel, List<Transaction> transactionList) {
        int totalCount = transactionList.size();
        double totalAmount = 0;

        for (Transaction t : transactionList) {
            totalAmount += t.finalAmount;
        }

        double avgAmount = totalCount > 0 ? totalAmount / totalCount : 0;

        totalCountLabel.setText("总交易数: " + totalCount + "笔");
        totalAmountLabel.setText("总金额: ¥" + String.format("%.2f", totalAmount));
        avgAmountLabel.setText("平均金额: ¥" + String.format("%.2f", avgAmount));
    }

    private void showInventoryReport() {
        JDialog dialog = new JDialog(this, "库存报表", true);
        dialog.setSize(1000, 600);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] columns = {"商品名称", "分类", "单价", "成本", "库存", "最低库存", "库存值", "状态"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);
        styleTable(table, tableModel);

        for (Product p : inventory.values()) {
            String status = "";
            if (p.quantity <= 0) {
                status = "缺货";
            } else if (p.quantity <= p.minStock) {
                status = "低库存";
            } else {
                status = "正常";
            }

            double stockValue = p.price * p.quantity;

            tableModel.addRow(new Object[]{
                p.name,
                p.category,
                String.format("%.2f", p.price),
                String.format("%.2f", p.cost),
                p.quantity,
                p.minStock,
                String.format("%.2f", stockValue),
                status
            });
        }

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 底部统计
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        int totalProducts = inventory.size();
        int outOfStock = countOutOfStock();
        int lowStock = countLowStock();
        double totalValue = calculateInventoryValue();

        statsPanel.add(new JLabel("商品总数: " + totalProducts + "种"));
        statsPanel.add(new JLabel("缺货: " + outOfStock + "种"));
        statsPanel.add(new JLabel("低库存: " + lowStock + "种"));
        statsPanel.add(new JLabel("库存总值: ¥" + String.format("%.2f", totalValue)));

        panel.add(statsPanel, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void showMemberReport() {
        JDialog dialog = new JDialog(this, "会员报表", true);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] columns = {"会员姓名", "手机号", "等级", "积分", "折扣", "状态"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);
        styleTable(table, tableModel);

        for (Member m : members.values()) {
            String status = m.points > 0 ? "活跃" : "未活跃";

            tableModel.addRow(new Object[]{
                m.name,
                m.phone,
                m.level,
                String.format("%.0f", m.points),
                String.format("%.1f折", m.discount * 10),
                status
            });
        }

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 底部统计
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        int totalMembers = members.size();
        int activeMembers = countActiveMembers();
        int diamondMembers = countMembersByLevel("钻石");
        int goldMembers = countMembersByLevel("金卡");

        statsPanel.add(new JLabel("会员总数: " + totalMembers + "人"));
        statsPanel.add(new JLabel("活跃会员: " + activeMembers + "人"));
        statsPanel.add(new JLabel("钻石会员: " + diamondMembers + "人"));
        statsPanel.add(new JLabel("金卡会员: " + goldMembers + "人"));

        panel.add(statsPanel, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    // 批量添加商品到购物车
    private void showBatchAddToCartDialog() {
        // 检查是否有进行中的班次
        if (!checkShiftRequiredStrict()) {
            return;
        }
        
        JDialog dialog = new JDialog(this, "批量添加商品", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 创建商品选择表格
        String[] columns = {"商品名称", "单价", "库存", "选择", "数量"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 3) return Boolean.class;
                if (column == 4) return Integer.class;
                return String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3 || column == 4;
            }
        };

        JTable table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(getGeneralFont(Font.PLAIN, 12));
        table.getTableHeader().setFont(getGeneralFont(Font.BOLD, 13));

        // 填充商品数据
        for (Product product : inventory.values()) {
            tableModel.addRow(new Object[]{
                product.name,
                String.format("%.2f", product.price),
                product.quantity,
                false,
                1
            });
        }

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton selectAllButton = createStyledButton("全选", INFO_COLOR);
        JButton deselectAllButton = createStyledButton("取消全选", GRAY_COLOR);
        JButton confirmButton = createStyledButton("确定添加", SUCCESS_COLOR);
        JButton cancelButton = createStyledButton("取消", DANGER_COLOR);

        buttonPanel.add(selectAllButton);
        buttonPanel.add(deselectAllButton);
        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(panel);

        // 全选
        selectAllButton.addActionListener(e -> {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                tableModel.setValueAt(true, i, 3);
            }
        });

        // 取消全选
        deselectAllButton.addActionListener(e -> {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                tableModel.setValueAt(false, i, 3);
            }
        });

        // 确定添加
        confirmButton.addActionListener(e -> {
            int addedCount = 0;
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                Boolean selected = (Boolean) tableModel.getValueAt(i, 3);
                if (selected) {
                    String productName = (String) tableModel.getValueAt(i, 0);
                    int quantity = (Integer) tableModel.getValueAt(i, 4);
                    Product product = inventory.get(productName);
                    
                    if (product != null && quantity > 0) {
                        // 检查库存
                        if (product.quantity < quantity) {
                            JOptionPane.showMessageDialog(dialog, 
                                "商品 " + productName + " 库存不足！\n当前库存: " + product.quantity + "，需要: " + quantity,
                                "库存不足", JOptionPane.WARNING_MESSAGE);
                            continue;
                        }
                        
                        // 添加到购物车
                        boolean found = false;
                        for (Product cartProduct : cart) {
                            if (cartProduct.name.equals(productName)) {
                                cartProduct.quantity += quantity;
                                found = true;
                                break;
                            }
                        }
                        
                        if (!found) {
                            Product newProduct = new Product(product.name, product.price, quantity, product.category);
                            cart.add(newProduct);
                        }
                        
                        addedCount++;
                    }
                }
            }
            
            if (addedCount > 0) {
                refreshCartTable();
                dialog.dispose();
                // 商品已添加到购物车，直接在购物车中显示
            } else {
                JOptionPane.showMessageDialog(dialog, "请选择要添加的商品！", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // 取消
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    // 快速重复添加（添加最近添加的商品）
    private void repeatAddToCart() {
        // 检查是否有进行中的班次
        if (!checkShiftRequiredStrict()) {
            return;
        }
        
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "购物车为空，无法重复添加！", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // 获取购物车中最后一个商品
        Product lastProduct = cart.get(cart.size() - 1);
        Product inventoryProduct = inventory.get(lastProduct.name);

        if (inventoryProduct == null || inventoryProduct.quantity <= 0) {
            JOptionPane.showMessageDialog(this, "商品 " + lastProduct.name + " 库存不足！", "库存不足", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 添加一件到购物车
        boolean found = false;
        for (Product cartProduct : cart) {
            if (cartProduct.name.equals(lastProduct.name)) {
                cartProduct.quantity++;
                found = true;
                break;
            }
        }

        if (!found) {
            Product newProduct = new Product(lastProduct.name, lastProduct.price, 1, lastProduct.category);
            cart.add(newProduct);
        }

        refreshCartTable();
        // 商品已添加到购物车，直接在购物车中显示
    }

    // 购物车排序
    private void showSortCartDialog() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "购物车为空，无需排序！", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] options = {"按名称排序", "按价格排序(低→高)", "按价格排序(高→低)", "按数量排序(多→少)", "按数量排序(少→多)", "按小计排序(高→低)"};
        int choice = JOptionPane.showOptionDialog(this,
            "请选择排序方式：",
            "购物车排序",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);

        if (choice >= 0 && choice < options.length) {
            switch (choice) {
                case 0: // 按名称排序
                    cart.sort((p1, p2) -> p1.name.compareTo(p2.name));
                    break;
                case 1: // 按价格排序(低→高)
                    cart.sort((p1, p2) -> Double.compare(p1.price, p2.price));
                    break;
                case 2: // 按价格排序(高→低)
                    cart.sort((p1, p2) -> Double.compare(p2.price, p1.price));
                    break;
                case 3: // 按数量排序(多→少)
                    cart.sort((p1, p2) -> Integer.compare(p2.quantity, p1.quantity));
                    break;
                case 4: // 按数量排序(少→多)
                    cart.sort((p1, p2) -> Integer.compare(p1.quantity, p2.quantity));
                    break;
                case 5: // 按小计排序(高→低)
                    cart.sort((p1, p2) -> Double.compare(p2.price * p2.quantity, p1.price * p1.quantity));
                    break;
            }
            refreshCartTable();
            JOptionPane.showMessageDialog(this, "购物车已按" + options[choice] + "！", "成功", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // 快速添加到购物车（支持扫码和搜索）
    private void quickAddToCart(String input) {
        // 检查是否有进行中的班次
        if (!checkShiftRequiredStrict()) {
            return;
        }
        
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        // 先尝试按条形码精确查找
        Product product = null;
        for (Product p : inventory.values()) {
            if (p.barcode != null && p.barcode.trim().equals(input.trim())) {
                product = p;
                break;
            }
        }

        // 如果条形码没找到，按名称模糊搜索
        if (product == null) {
            String lowerInput = input.toLowerCase().trim();
            for (Product p : inventory.values()) {
                if (p.name.toLowerCase().contains(lowerInput)) {
                    product = p;
                    break;
                }
            }
        }

        if (product == null) {
            JOptionPane.showMessageDialog(this, 
                "未找到商品：" + input + "！\n\n提示：\n1. 请确认条形码是否正确\n2. 可以输入商品名称的部分文字进行搜索", 
                "未找到商品", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (product.quantity <= 0) {
            JOptionPane.showMessageDialog(this, 
                "商品 " + product.name + " 库存不足！\n当前库存：0", 
                "库存不足", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 添加到购物车
        boolean found = false;
        for (Product cartProduct : cart) {
            if (cartProduct.name.equals(product.name)) {
                cartProduct.quantity++;
                found = true;
                break;
            }
        }

        if (!found) {
            Product newProduct = new Product(product.name, product.price, 1, product.category);
            cart.add(newProduct);
        }

        refreshCartTable();
        // 商品已添加到购物车，直接在购物车中显示
    }

    // 组合支付对话框
    private void showCombinedPaymentDialog(JLabel totalLabel, JLabel changeLabel, JTextField receivedField, JLabel currentMemberLabel, JComboBox<String> memberComboBox) {
        // 检查是否有进行中的班次
        if (!checkShiftRequiredStrict()) {
            return;
        }
        
        double total = 0;
        for (Product product : cart) {
            total += product.price * product.quantity;
        }
        
        // 计算会员折扣和促销折扣
        double memberDiscountAmount = 0;
        if (currentMember != null) {
            memberDiscountAmount = total * (1 - currentMember.discount);
        }
        
        double afterMemberDiscount = total - memberDiscountAmount;
        double promotionDiscountAmount = 0;
        if (currentPromotion != null) {
            double promotionBaseAmount = ("满减".equals(currentPromotion.type)) ? total : afterMemberDiscount;
            promotionDiscountAmount = currentPromotion.calculateDiscount(promotionBaseAmount);
        }
        
        double discountAmount = memberDiscountAmount + promotionDiscountAmount;
        double finalTotal = total - discountAmount;

        JDialog dialog = new JDialog(this, "组合支付", true);
        dialog.setSize(450, 400);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("应付金额: ¥" + String.format("%.2f", finalTotal)), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("现金:"), gbc);

        gbc.gridx = 1;
        JTextField cashField = new JTextField(10);
        cashField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(cashField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("微信支付:"), gbc);

        gbc.gridx = 1;
        JTextField wechatField = new JTextField(10);
        wechatField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(wechatField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("支付宝:"), gbc);

        gbc.gridx = 1;
        JTextField alipayField = new JTextField(10);
        alipayField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(alipayField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("银行卡:"), gbc);

        gbc.gridx = 1;
        JTextField cardField = new JTextField(10);
        cardField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(cardField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        JLabel totalPaidLabel = new JLabel("已支付: ¥0.00");
        totalPaidLabel.setFont(getGeneralFont(Font.BOLD, 14));
        totalPaidLabel.setForeground(DANGER_COLOR);
        panel.add(totalPaidLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton confirmButton = createStyledButton("确认支付", SUCCESS_COLOR);
        JButton cancelButton = createStyledButton("取消", GRAY_COLOR);

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        panel.add(buttonPanel, gbc);

        dialog.add(panel);

        // 计算总支付金额
        ActionListener calculateListener = e -> {
            double cash = 0, wechat = 0, alipay = 0, card = 0;
            try {
                if (!cashField.getText().isEmpty()) cash = Double.parseDouble(cashField.getText());
                if (!wechatField.getText().isEmpty()) wechat = Double.parseDouble(wechatField.getText());
                if (!alipayField.getText().isEmpty()) alipay = Double.parseDouble(alipayField.getText());
                if (!cardField.getText().isEmpty()) card = Double.parseDouble(cardField.getText());
            } catch (NumberFormatException ex) {
                // 忽略无效输入
            }
            double totalPaid = cash + wechat + alipay + card;
            totalPaidLabel.setText("已支付: ¥" + String.format("%.2f", totalPaid));
        };

        cashField.addActionListener(calculateListener);
        wechatField.addActionListener(calculateListener);
        alipayField.addActionListener(calculateListener);
        cardField.addActionListener(calculateListener);

        confirmButton.addActionListener(e -> {
            double cash = 0, wechat = 0, alipay = 0, card = 0;
            try {
                if (!cashField.getText().isEmpty()) cash = Double.parseDouble(cashField.getText());
                if (!wechatField.getText().isEmpty()) wechat = Double.parseDouble(wechatField.getText());
                if (!alipayField.getText().isEmpty()) alipay = Double.parseDouble(alipayField.getText());
                if (!cardField.getText().isEmpty()) card = Double.parseDouble(cardField.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "请输入有效的金额！", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double totalPaid = cash + wechat + alipay + card;
            if (totalPaid < finalTotal) {
                JOptionPane.showMessageDialog(dialog, "支付金额不足！还需: ¥" + String.format("%.2f", finalTotal - totalPaid), "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double change = totalPaid - finalTotal;
            dialog.dispose();

            // 执行结账
            performCheckout(
                new JLabel(), new JLabel(), new JLabel(), new JLabel(), 
                new JLabel(), totalLabel, totalPaid, changeLabel, receivedField, currentMemberLabel, 
                "组合支付 (现金:" + cash + ", 微信:" + wechat + ", 支付宝:" + alipay + ", 银行卡:" + card + ")",
                memberComboBox
            );
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    // 计算最优找零方案
    private Map<String, Integer> calculateOptimalChange(double changeAmount) {
        Map<String, Integer> change = new LinkedHashMap<>();
        double amount = changeAmount;

        // 人民币面额
        int[] denominations = {100, 50, 20, 10, 5, 1};
        double[] coinDenominations = {0.5, 0.1};

        // 计算纸币
        for (int denom : denominations) {
            if (amount >= denom) {
                int count = (int) (amount / denom);
                change.put(denom + "元", count);
                amount -= count * denom;
            }
        }

        // 计算硬币
        for (double denom : coinDenominations) {
            if (amount >= denom - 0.001) { // 考虑浮点数精度
                int count = (int) Math.round(amount / denom);
                change.put(denom + "元", count);
                amount -= count * denom;
            }
        }

        return change;
    }

    // 显示找零详情
    private void showChangeDetails(double changeAmount) {
        if (changeAmount <= 0) {
            JOptionPane.showMessageDialog(this, "无需找零！", "找零", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Map<String, Integer> change = calculateOptimalChange(changeAmount);
        StringBuilder sb = new StringBuilder();
        sb.append("找零总额: ¥").append(String.format("%.2f", changeAmount)).append("\n\n");
        sb.append("找零明细:\n");
        sb.append("--------------------------------\n");

        for (Map.Entry<String, Integer> entry : change.entrySet()) {
            sb.append(String.format("%s x %d张 = ¥%.2f\n", 
                entry.getKey(), 
                entry.getValue(), 
                Double.parseDouble(entry.getKey().replace("元", "")) * entry.getValue()));
        }

        sb.append("--------------------------------\n");

        JOptionPane.showMessageDialog(this, sb.toString(), "找零详情", JOptionPane.INFORMATION_MESSAGE);
    }

    // 导出销售数据为CSV
    private void exportSalesData(DefaultTableModel tableModel) {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "没有数据可导出！", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出销售数据");
        fileChooser.setSelectedFile(new File("销售报表_" + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()) + ".csv"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(fileChooser.getSelectedFile()));
                
                // 写入CSV头部
                writer.println("交易ID,交易时间,金额,税费,实收,商品数,会员");
                
                // 写入数据
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    writer.printf("%s,%s,%s,%s,%s,%s,%s\n",
                        tableModel.getValueAt(i, 0),
                        tableModel.getValueAt(i, 1),
                        tableModel.getValueAt(i, 2),
                        tableModel.getValueAt(i, 3),
                        tableModel.getValueAt(i, 4),
                        tableModel.getValueAt(i, 5),
                        tableModel.getValueAt(i, 6)
                    );
                }
                
                writer.close();
                JOptionPane.showMessageDialog(this, "销售数据导出成功！\n文件位置: " + fileChooser.getSelectedFile().getAbsolutePath(), "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "导出失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 导出库存数据为CSV
    private void exportInventoryData() {
        if (inventory.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有库存数据可导出！", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出库存数据");
        fileChooser.setSelectedFile(new File("库存数据_" + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()) + ".csv"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(fileChooser.getSelectedFile()));
                
                // 写入CSV头部
                writer.println("商品名称,单价,库存,分类,条形码,单位,品牌,供应商,规格,最低库存,成本价,库存值");
                
                // 写入数据
                for (Product product : inventory.values()) {
                    double stockValue = product.price * product.quantity;
                    writer.printf("%s,%.2f,%d,%s,%s,%s,%s,%s,%s,%d,%.2f,%.2f\n",
                        product.name,
                        product.price,
                        product.quantity,
                        product.category,
                        product.barcode,
                        product.unit,
                        product.brand,
                        product.supplier,
                        product.spec,
                        product.minStock,
                        product.cost,
                        stockValue
                    );
                }
                
                writer.close();
                JOptionPane.showMessageDialog(this, "库存数据导出成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "导出失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 导出会员数据为CSV
    private void exportMemberData() {
        if (members.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有会员数据可导出！", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出会员数据");
        fileChooser.setSelectedFile(new File("会员数据_" + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()) + ".csv"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(fileChooser.getSelectedFile()));
                
                // 写入CSV头部
                writer.println("手机号,姓名,积分,等级,折扣率,余额,生日");
                
                // 写入数据
                for (Member member : members.values()) {
                    writer.printf("%s,%s,%.2f,%s,%.2f,%.2f,%s\n",
                        member.phone,
                        member.name,
                        member.points,
                        member.level,
                        member.discount,
                        member.balance,
                        member.birthday
                    );
                }
                
                writer.close();
                JOptionPane.showMessageDialog(this, "会员数据导出成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "导出失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 会员充值对话框
    private void showRechargeDialog(Member member, DefaultTableModel memberTableModel, JComboBox<String> memberComboBox) {
        JDialog dialog = new JDialog(this, "会员充值 - " + member.name, true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("会员姓名:"), gbc);

        gbc.gridx = 1;
        JLabel nameLabel = new JLabel(member.name);
        nameLabel.setFont(getGeneralFont(Font.BOLD, 14));
        panel.add(nameLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("当前余额:"), gbc);

        gbc.gridx = 1;
        JLabel balanceLabel = new JLabel("¥" + String.format("%.2f", member.balance));
        balanceLabel.setFont(getGeneralFont(Font.BOLD, 14));
        balanceLabel.setForeground(DANGER_COLOR);
        panel.add(balanceLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("充值金额:"), gbc);

        gbc.gridx = 1;
        JTextField amountField = new JTextField(20);
        amountField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(amountField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("支付方式:"), gbc);

        gbc.gridx = 1;
        JComboBox<String> paymentMethodComboBox = new JComboBox<>(new String[]{"现金", "微信支付", "支付宝", "银行卡"});
        paymentMethodComboBox.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(paymentMethodComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        JButton confirmButton = createStyledButton("确认充值", SUCCESS_COLOR);
        JButton cancelButton = createStyledButton("取消", GRAY_COLOR);

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        panel.add(buttonPanel, gbc);

        dialog.add(panel);

        confirmButton.addActionListener(e -> {
            try {
                double amount = Double.parseDouble(amountField.getText().trim());
                if (amount <= 0) {
                    JOptionPane.showMessageDialog(dialog, "充值金额必须大于0！", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String paymentMethod = (String) paymentMethodComboBox.getSelectedItem();

                // 更新会员余额
                member.balance += amount;

                // 创建充值记录
                String recordId = "R" + String.format("%06d", rechargeRecords.size() + 1);
                RechargeRecord record = new RechargeRecord(recordId, member.phone, member.name, amount, paymentMethod, "系统");
                rechargeRecords.add(record);

                // 保存数据
                saveData();

                // 刷新显示
                balanceLabel.setText("¥" + String.format("%.2f", member.balance));

                JOptionPane.showMessageDialog(dialog, "充值成功！\n充值金额: ¥" + String.format("%.2f", amount) + "\n当前余额: ¥" + String.format("%.2f", member.balance), "成功", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "请输入有效的金额！", "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    // 查看会员消费记录
    private void showMemberConsumptionRecords(Member member) {
        JDialog dialog = new JDialog(this, "消费记录 - " + member.name, true);
        dialog.setSize(700, 500);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 查找该会员的所有交易记录
        List<Transaction> memberTransactions = new ArrayList<>();
        for (Transaction transaction : transactions) {
            // 检查交易中是否有会员消费
            for (Product product : transaction.items) {
                // 这里简化处理，实际应该记录每笔交易的会员信息
                // 暂时假设最近的交易是该会员的
                if (member.points > 0) {
                    memberTransactions.add(transaction);
                    break;
                }
            }
        }

        // 创建表格
        String[] columns = {"交易ID", "交易时间", "商品数量", "总金额", "税费"};
        DefaultTableModel recordTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (Transaction transaction : memberTransactions) {
            recordTableModel.addRow(new Object[]{
                transaction.transactionId,
                transaction.timestamp,
                transaction.items.size(),
                String.format("%.2f", transaction.totalAmount),
                String.format("%.2f", transaction.tax)
            });
        }

        JTable recordTable = new JTable(recordTableModel);
        recordTable.setRowHeight(25);
        recordTable.setFont(getGeneralFont(Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(recordTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 统计信息
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 15, 10));
        statsPanel.setBorder(BorderFactory.createTitledBorder("消费统计"));
        statsPanel.setBackground(CARD_BACKGROUND);

        double totalAmount = memberTransactions.stream().mapToDouble(t -> t.totalAmount).sum();
        int totalCount = memberTransactions.size();
        double avgAmount = totalCount > 0 ? totalAmount / totalCount : 0;

        statsPanel.add(new JLabel("总消费: ¥" + String.format("%.2f", totalAmount)));
        statsPanel.add(new JLabel("交易次数: " + totalCount + "次"));
        statsPanel.add(new JLabel("平均消费: ¥" + String.format("%.2f", avgAmount)));

        panel.add(statsPanel, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    // 会员报表统计
    private void showMemberStatistics() {
        JDialog dialog = new JDialog(this, "会员报表统计", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 创建统计卡片
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 15, 15));

        int totalMembers = members.size();
        int diamondMembers = (int) members.values().stream().filter(m -> "钻石".equals(m.level)).count();
        int goldMembers = (int) members.values().stream().filter(m -> "金卡".equals(m.level)).count();
        int silverMembers = (int) members.values().stream().filter(m -> "银卡".equals(m.level)).count();
        double totalBalance = members.values().stream().mapToDouble(m -> m.balance).sum();
        double totalPoints = members.values().stream().mapToDouble(m -> m.points).sum();
        double totalRecharge = rechargeRecords.stream().mapToDouble(r -> r.amount).sum();

        JPanel card1 = createStatCard("会员总数", totalMembers + "人", INFO_COLOR);
        JPanel card2 = createStatCard("会员总余额", "¥" + String.format("%.2f", totalBalance), DANGER_COLOR);
        JPanel card3 = createStatCard("会员总积分", String.format("%.0f", totalPoints), SUCCESS_COLOR);
        JPanel card4 = createStatCard("充值总额", "¥" + String.format("%.2f", totalRecharge), WARNING_COLOR);

        statsPanel.add(card1);
        statsPanel.add(card2);
        statsPanel.add(card3);
        statsPanel.add(card4);

        panel.add(statsPanel, BorderLayout.NORTH);

        // 会员等级分布
        JPanel levelPanel = new JPanel(new BorderLayout(10, 10));
        levelPanel.setBorder(BorderFactory.createTitledBorder("会员等级分布"));
        levelPanel.setBackground(CARD_BACKGROUND);

        String[] levelColumns = {"等级", "人数", "占比"};
        DefaultTableModel levelTableModel = new DefaultTableModel(levelColumns, 0);

        int[] levelCounts = {silverMembers, goldMembers, diamondMembers};
        String[] levelNames = {"银卡", "金卡", "钻石"};

        for (int i = 0; i < levelNames.length; i++) {
            double percentage = totalMembers > 0 ? (levelCounts[i] * 100.0 / totalMembers) : 0;
            levelTableModel.addRow(new Object[]{
                levelNames[i],
                levelCounts[i],
                String.format("%.1f%%", percentage)
            });
        }

        JTable levelTable = new JTable(levelTableModel);
        levelTable.setRowHeight(25);
        levelTable.setFont(getGeneralFont(Font.PLAIN, 12));

        JScrollPane levelScrollPane = new JScrollPane(levelTable);
        levelPanel.add(levelScrollPane, BorderLayout.CENTER);

        panel.add(levelPanel, BorderLayout.CENTER);

        JButton closeButton = createStyledButton("关闭", GRAY_COLOR);
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.add(closeButton);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    // 创建统计卡片
    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(CARD_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(getGeneralFont(Font.BOLD, 12));
        titleLabel.setForeground(color);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(getGeneralFont(Font.BOLD, 18));
        valueLabel.setForeground(TEXT_COLOR);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    // 交接班管理
    private void showShiftManagementDialog() {
        JDialog dialog = new JDialog(this, "交接班管理", true);
        dialog.setSize(1000, 600);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 顶部按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton startShiftButton = createStyledButton("开始班次", SUCCESS_COLOR);
        JButton endShiftButton = createStyledButton("结束班次", DANGER_COLOR);
        JButton viewShiftButton = createStyledButton("查看详情", INFO_COLOR);
        JButton exportButton = createStyledButton("导出记录", PURPLE_COLOR);

        buttonPanel.add(startShiftButton);
        buttonPanel.add(endShiftButton);
        buttonPanel.add(viewShiftButton);
        buttonPanel.add(exportButton);

        // 当前班次信息
        JLabel currentShiftLabel;
        if (currentShift != null) {
            currentShiftLabel = new JLabel("当前班次: " + currentShift.shiftId + " - " + currentUser.name + " (" + new SimpleDateFormat("HH:mm:ss").format(currentShift.startTime) + ")");
            currentShiftLabel.setForeground(SUCCESS_COLOR);
        } else {
            currentShiftLabel = new JLabel("当前班次: 未开始");
            currentShiftLabel.setForeground(PRIMARY_COLOR);
        }
        currentShiftLabel.setFont(getGeneralFont(Font.BOLD, 14));
        buttonPanel.add(currentShiftLabel);

        panel.add(buttonPanel, BorderLayout.NORTH);

        // 交接班记录表格
        String[] columns = {"班次ID", "操作员", "开始时间", "结束时间", "班次时长", "班次营业额", "交易数", "备注"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (Shift shift : shifts) {
            tableModel.addRow(new Object[]{
                shift.shiftId,
                shift.operatorName,
                shift.startTime,
                shift.endTime,
                shift.getDurationText(),
                String.format("%.2f", shift.shiftRevenue),
                shift.shiftTransactionCount,
                shift.notes
            });
        }

        JTable table = new JTable(tableModel);
        styleTable(table, tableModel);

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        dialog.add(panel);

        // 开始班次按钮事件
        startShiftButton.addActionListener(e -> {
            // 检查是否有当前用户的未结束班次
            Shift unfinishedShift = shifts.stream()
                .filter(s -> s.username.equals(currentUser.username) && s.startTime.equals(s.endTime))
                .findFirst()
                .orElse(null);
            
            if (unfinishedShift != null) {
                int confirm = JOptionPane.showConfirmDialog(dialog,
                    "检测到您有未结束的班次！\n\n" +
                    "班次ID: " + unfinishedShift.shiftId + "\n" +
                    "开始时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(unfinishedShift.startTime) + "\n\n" +
                    "是否恢复该班次？",
                    "未结束的班次",
                    JOptionPane.YES_NO_OPTION);
                
                if (confirm == JOptionPane.YES_OPTION) {
                    // 恢复班次
                    currentShift = unfinishedShift;
                    currentShiftLabel.setText("当前班次: " + currentShift.shiftId + " - " + currentUser.name + " (" + new SimpleDateFormat("HH:mm:ss").format(currentShift.startTime) + ")");
                    currentShiftLabel.setForeground(SUCCESS_COLOR);
                    logOperation("恢复班次", "班次ID: " + currentShift.shiftId);
                    JOptionPane.showMessageDialog(dialog, "班次已恢复！", "成功", JOptionPane.INFORMATION_MESSAGE);
                }
                return;
            }
            
            // 检查当前是否有其他用户的未结束班次
            if (currentShift != null) {
                JOptionPane.showMessageDialog(dialog, "当前已有班次在进行中，请先结束当前班次！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 计算当前营业额和交易数
            double currentRevenue = calculateTotalSales();
            int currentTransactionCount = transactions.size();

            String shiftId = String.format("S%06d", shifts.size() + 1);
            currentShift = new Shift(shiftId, currentUser.username, currentUser.name, new Date(), currentRevenue, currentTransactionCount);

            currentShiftLabel.setText("当前班次: " + shiftId + " - " + currentUser.name + " (" + new SimpleDateFormat("HH:mm:ss").format(currentShift.startTime) + ")");
            currentShiftLabel.setForeground(SUCCESS_COLOR);

            logOperation("开始班次", "班次ID: " + shiftId);
            JOptionPane.showMessageDialog(dialog, "班次开始成功！\n班次ID: " + shiftId + "\n开始时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(currentShift.startTime), "成功", JOptionPane.INFORMATION_MESSAGE);
        });

        // 结束班次按钮事件
        endShiftButton.addActionListener(e -> {
            if (currentShift == null) {
                JOptionPane.showMessageDialog(dialog, "当前没有进行中的班次！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(dialog,
                "确定要结束当前班次吗？\n班次ID: " + currentShift.shiftId,
                "确认结束", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                // 计算当前营业额和交易数
                double currentRevenue = calculateTotalSales();
                int currentTransactionCount = transactions.size();
                
                // 计算各种支付方式的收入
                Map<String, Double> paymentRevenue = calculateShiftPaymentRevenue();
                double cashRevenue = paymentRevenue.get("现金");
                double wechatRevenue = paymentRevenue.get("微信支付");
                double alipayRevenue = paymentRevenue.get("支付宝");
                double cardRevenue = paymentRevenue.get("银行卡");

                currentShift.endShift(currentRevenue, currentTransactionCount, cashRevenue, wechatRevenue, alipayRevenue, cardRevenue);
                shifts.add(currentShift);

                // 刷新表格
                tableModel.setRowCount(0);
                for (Shift shift : shifts) {
                    tableModel.addRow(new Object[]{
                        shift.shiftId,
                        shift.operatorName,
                        shift.startTime,
                        shift.endTime,
                        shift.getDurationText(),
                        String.format("%.2f", shift.shiftRevenue),
                        shift.shiftTransactionCount,
                        shift.notes
                    });
                }

                currentShift = null;
                currentShiftLabel.setText("当前班次: 未开始");
                currentShiftLabel.setForeground(PRIMARY_COLOR);

                saveData();
                logOperation("结束班次", "班次ID: " + currentShift.shiftId + ", 营业额: ¥" + String.format("%.2f", currentShift.shiftRevenue));
                
                // 显示详细的结班信息
                String shiftSummary = 
                    "班次结束成功！\n\n" +
                    "班次ID: " + currentShift.shiftId + "\n" +
                    "班次时长: " + currentShift.getDurationText() + "\n" +
                    "交易数量: " + currentShift.shiftTransactionCount + " 笔\n\n" +
                    "收入明细：\n" +
                    "现金收入: ¥" + String.format("%.2f", currentShift.cashRevenue) + "\n" +
                    "微信收入: ¥" + String.format("%.2f", currentShift.wechatRevenue) + "\n" +
                    "支付宝收入: ¥" + String.format("%.2f", currentShift.alipayRevenue) + "\n" +
                    "银行卡收入: ¥" + String.format("%.2f", currentShift.cardRevenue) + "\n" +
                    "组合支付: ¥" + String.format("%.2f", currentShift.shiftRevenue - currentShift.cashRevenue - currentShift.wechatRevenue - currentShift.alipayRevenue - currentShift.cardRevenue) + "\n" +
                    "合计收入: ¥" + String.format("%.2f", currentShift.shiftRevenue);
                
                JOptionPane.showMessageDialog(dialog, shiftSummary, "结班成功", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // 查看详情按钮事件
        viewShiftButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog, "请先选择要查看的班次！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String shiftId = (String) tableModel.getValueAt(selectedRow, 0);
            Shift selectedShift = shifts.stream()
                .filter(s -> s.shiftId.equals(shiftId))
                .findFirst()
                .orElse(null);

            if (selectedShift != null) {
                showShiftDetailDialog(selectedShift);
            }
        });

        // 导出按钮事件
        exportButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("导出交接班记录");
            fileChooser.setSelectedFile(new File("交接班记录_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv"));

            int userSelection = fileChooser.showSaveDialog(dialog);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                try {
                    PrintWriter writer = new PrintWriter(new FileWriter(fileChooser.getSelectedFile()));

                    writer.println("班次ID,操作员,开始时间,结束时间,班次时长,开机营业额,关机营业额,班次营业额,开机交易数,关机交易数,班次交易数,备注");

                    for (Shift shift : shifts) {
                        writer.printf("%s,%s,%s,%s,%s,%.2f,%.2f,%.2f,%d,%d,%d,%s\n",
                            shift.shiftId,
                            shift.operatorName,
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(shift.startTime),
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(shift.endTime),
                            shift.getDurationText(),
                            shift.openingRevenue,
                            shift.closingRevenue,
                            shift.shiftRevenue,
                            shift.openingTransactionCount,
                            shift.closingTransactionCount,
                            shift.shiftTransactionCount,
                            shift.notes
                        );
                    }

                    writer.close();
                    JOptionPane.showMessageDialog(dialog, "导出成功！\n文件位置: " + fileChooser.getSelectedFile().getAbsolutePath(), "成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "导出失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        dialog.setVisible(true);
    }

    // 显示班次详情
    private void showShiftDetailDialog(Shift shift) {
        JDialog dialog = new JDialog(this, "班次详情 - " + shift.shiftId, true);
        dialog.setSize(500, 550);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 班次信息
        JPanel infoPanel = new JPanel(new GridLayout(6, 2, 10, 5));
        infoPanel.setBorder(BorderFactory.createTitledBorder("班次信息"));

        infoPanel.add(new JLabel("班次ID:"));
        infoPanel.add(new JLabel(shift.shiftId));
        infoPanel.add(new JLabel("操作员:"));
        infoPanel.add(new JLabel(shift.operatorName));
        infoPanel.add(new JLabel("开始时间:"));
        infoPanel.add(new JLabel(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(shift.startTime)));
        infoPanel.add(new JLabel("结束时间:"));
        infoPanel.add(new JLabel(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(shift.endTime)));
        infoPanel.add(new JLabel("班次时长:"));
        infoPanel.add(new JLabel(shift.getDurationText()));
        infoPanel.add(new JLabel("备注:"));
        infoPanel.add(new JLabel(shift.notes.isEmpty() ? "无" : shift.notes));

        // 统计信息
        JPanel statsPanel = new JPanel(new GridLayout(6, 2, 10, 5));
        statsPanel.setBorder(BorderFactory.createTitledBorder("统计信息"));

        statsPanel.add(new JLabel("开机营业额:"));
        statsPanel.add(new JLabel("¥" + String.format("%.2f", shift.openingRevenue)));
        statsPanel.add(new JLabel("关机营业额:"));
        statsPanel.add(new JLabel("¥" + String.format("%.2f", shift.closingRevenue)));
        statsPanel.add(new JLabel("班次营业额:"));
        statsPanel.add(new JLabel("¥" + String.format("%.2f", shift.shiftRevenue)));
        statsPanel.add(new JLabel("交易数:"));
        statsPanel.add(new JLabel(String.valueOf(shift.shiftTransactionCount)));
        
        // 支付方式收入
        statsPanel.add(new JLabel("现金收入:"));
        statsPanel.add(new JLabel("¥" + String.format("%.2f", shift.cashRevenue)));
        statsPanel.add(new JLabel("微信收入:"));
        statsPanel.add(new JLabel("¥" + String.format("%.2f", shift.wechatRevenue)));
        statsPanel.add(new JLabel("支付宝收入:"));
        statsPanel.add(new JLabel("¥" + String.format("%.2f", shift.alipayRevenue)));
        statsPanel.add(new JLabel("银行卡收入:"));
        statsPanel.add(new JLabel("¥" + String.format("%.2f", shift.cardRevenue)));

        // 关闭按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton closeButton = createStyledButton("关闭", GRAY_COLOR);
        buttonPanel.add(closeButton);

        panel.add(infoPanel, BorderLayout.NORTH);
        panel.add(statsPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(panel);

        closeButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    // 用户管理
    private void showUserManagementDialog() {
        JDialog dialog = new JDialog(this, "用户管理", true);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 搜索面板
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        JLabel searchLabel = new JLabel("搜索:");
        searchLabel.setFont(getGeneralFont(Font.BOLD, 12));
        JTextField searchField = new JTextField(20);
        searchField.setFont(getGeneralFont(Font.PLAIN, 12));
        JButton searchButton = createStyledButton("搜索", new Color(59, 130, 246));
        JButton clearButton = createStyledButton("清除", new Color(149, 165, 166));
        
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(clearButton);

        // 用户列表
        String[] columns = {"用户名", "姓名", "角色", "创建时间", "最后登录", "状态"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (User user : users.values()) {
            tableModel.addRow(new Object[]{
                user.username,
                user.name,
                user.getRoleDisplayName(),
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(user.createTime),
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(user.lastLoginTime),
                user.active ? "正常" : "禁用"
            });
        }

        JTable table = new JTable(tableModel);
        styleTable(table, tableModel);

        JScrollPane scrollPane = new JScrollPane(table);
        
        // 中间面板：搜索面板 + 表格
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        panel.add(centerPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        JButton addButton = createStyledButton("添加用户", SUCCESS_COLOR);
        JButton editButton = createStyledButton("编辑用户", INFO_COLOR);
        JButton deleteButton = createStyledButton("删除用户", DANGER_COLOR);
        JButton changePasswordButton = createStyledButton("修改密码", WARNING_COLOR);
        JButton toggleStatusButton = createStyledButton("启用/禁用", new Color(52, 152, 219));
        JButton viewLogsButton = createStyledButton("操作日志", PURPLE_COLOR);
        JButton closeButton = createStyledButton("关闭", GRAY_COLOR);

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(changePasswordButton);
        buttonPanel.add(toggleStatusButton);
        buttonPanel.add(viewLogsButton);
        buttonPanel.add(closeButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(panel);

        // 搜索功能
        searchButton.addActionListener(e -> {
            String keyword = searchField.getText().trim().toLowerCase();
            if (keyword.isEmpty()) {
                // 显示所有用户
                tableModel.setRowCount(0);
                for (User user : users.values()) {
                    tableModel.addRow(new Object[]{
                        user.username,
                        user.name,
                        user.getRoleDisplayName(),
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(user.createTime),
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(user.lastLoginTime),
                        user.active ? "正常" : "禁用"
                    });
                }
            } else {
                // 搜索匹配的用户
                tableModel.setRowCount(0);
                for (User user : users.values()) {
                    if (user.username.toLowerCase().contains(keyword) ||
                        user.name.toLowerCase().contains(keyword) ||
                        user.getRoleDisplayName().toLowerCase().contains(keyword)) {
                        tableModel.addRow(new Object[]{
                            user.username,
                            user.name,
                            user.getRoleDisplayName(),
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(user.createTime),
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(user.lastLoginTime),
                            user.active ? "正常" : "禁用"
                        });
                    }
                }
            }
        });

        // 清除搜索
        clearButton.addActionListener(e -> {
            searchField.setText("");
            // 显示所有用户
            tableModel.setRowCount(0);
            for (User user : users.values()) {
                tableModel.addRow(new Object[]{
                    user.username,
                    user.name,
                    user.getRoleDisplayName(),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(user.createTime),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(user.lastLoginTime),
                    user.active ? "正常" : "禁用"
                });
            }
        });

        // 添加用户按钮事件
        addButton.addActionListener(e -> showAddUserDialog(dialog, tableModel));

        // 编辑用户按钮事件
        editButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog, "请先选择要编辑的用户！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String username = (String) tableModel.getValueAt(selectedRow, 0);
            User selectedUser = users.get(username);
            showEditUserDialog(dialog, tableModel, selectedUser);
        });

        // 删除用户按钮事件
        deleteButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog, "请先选择要删除的用户！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String username = (String) tableModel.getValueAt(selectedRow, 0);

            if (username.equals(currentUser.username)) {
                JOptionPane.showMessageDialog(dialog, "不能删除当前登录的用户！", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(dialog,
                "确定要删除用户 \"" + username + "\" 吗？",
                "确认删除", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                users.remove(username);
                saveData();

                // 刷新表格
                tableModel.setRowCount(0);
                for (User user : users.values()) {
                    tableModel.addRow(new Object[]{
                        user.username,
                        user.name,
                        user.getRoleDisplayName(),
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(user.createTime),
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(user.lastLoginTime),
                        user.active ? "正常" : "禁用"
                    });
                }

                logOperation("删除用户", "用户名: " + username);
                JOptionPane.showMessageDialog(dialog, "用户删除成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // 修改密码按钮事件
        changePasswordButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog, "请先选择要修改密码的用户！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String username = (String) tableModel.getValueAt(selectedRow, 0);
            User selectedUser = users.get(username);
            showChangePasswordDialog(dialog, selectedUser);
        });

        // 启用/禁用按钮事件
        toggleStatusButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog, "请先选择要操作的用户！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String username = (String) tableModel.getValueAt(selectedRow, 0);

            if (username.equals(currentUser.username)) {
                JOptionPane.showMessageDialog(dialog, "不能禁用当前登录的用户！", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            User selectedUser = users.get(username);
            selectedUser.active = !selectedUser.active;
            saveData();

            // 刷新表格
            tableModel.setRowCount(0);
            for (User user : users.values()) {
                tableModel.addRow(new Object[]{
                    user.username,
                    user.name,
                    user.getRoleDisplayName(),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(user.createTime),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(user.lastLoginTime),
                    user.active ? "正常" : "禁用"
                });
            }

            logOperation("修改用户状态", "用户名: " + username + ", 新状态: " + (selectedUser.active ? "正常" : "禁用"));
            JOptionPane.showMessageDialog(dialog, "用户状态修改成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
        });

        // 查看操作日志按钮事件
        viewLogsButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog, "请先选择要查看的用户！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String username = (String) tableModel.getValueAt(selectedRow, 0);
            showUserOperationLogsDialog(dialog, username);
        });

        // 关闭按钮事件
        closeButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    // 添加用户对话框
    private void showAddUserDialog(JDialog parentDialog, DefaultTableModel tableModel) {
        JDialog dialog = new JDialog(parentDialog, "添加用户", true);
        dialog.setSize(450, 400);
        dialog.setLocationRelativeTo(parentDialog);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("用户名:"), gbc);

        gbc.gridx = 1;
        JTextField usernameField = new JTextField(20);
        usernameField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("密码:"), gbc);

        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("确认密码:"), gbc);

        gbc.gridx = 1;
        JPasswordField confirmPasswordField = new JPasswordField(20);
        confirmPasswordField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(confirmPasswordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("姓名:"), gbc);

        gbc.gridx = 1;
        JTextField nameField = new JTextField(20);
        nameField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("角色:"), gbc);

        gbc.gridx = 1;
        JComboBox<String> roleComboBox = new JComboBox<>(new String[]{"管理员", "收银员", "财务"});
        roleComboBox.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(roleComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        JButton confirmButton = createStyledButton("确定", SUCCESS_COLOR);
        JButton cancelButton = createStyledButton("取消", GRAY_COLOR);

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        panel.add(buttonPanel, gbc);

        dialog.add(panel);

        confirmButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());
            String name = nameField.getText().trim();
            String roleText = (String) roleComboBox.getSelectedItem();

            // 验证用户名
            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "用户名不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (username.length() < 3) {
                JOptionPane.showMessageDialog(dialog, "用户名长度不能少于3个字符！", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 验证密码
            if (password.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "密码不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (password.length() < 6) {
                JOptionPane.showMessageDialog(dialog, "密码长度不能少于6个字符！", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(dialog, "两次输入的密码不一致！", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 验证姓名
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "姓名不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 检查用户名是否已存在
            if (users.containsKey(username)) {
                JOptionPane.showMessageDialog(dialog, "该用户名已存在！", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 转换角色
            String role;
            switch (roleText) {
                case "管理员": role = "admin"; break;
                case "收银员": role = "cashier"; break;
                case "财务": role = "finance"; break;
                default: role = "cashier";
            }

            // 创建用户
            User user = new User(username, password, name, role);
            users.put(username, user);
            saveData();

            // 刷新表格
            tableModel.setRowCount(0);
            for (User u : users.values()) {
                tableModel.addRow(new Object[]{
                    u.username,
                    u.name,
                    u.getRoleDisplayName(),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(u.createTime),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(u.lastLoginTime),
                    u.active ? "正常" : "禁用"
                });
            }

            logOperation("添加用户", "用户名: " + username + ", 角色: " + roleText);
            dialog.dispose();
            JOptionPane.showMessageDialog(parentDialog, "用户添加成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    // 编辑用户对话框
    private void showEditUserDialog(JDialog parentDialog, DefaultTableModel tableModel, User user) {
        JDialog dialog = new JDialog(parentDialog, "编辑用户", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(parentDialog);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("用户名:"), gbc);

        gbc.gridx = 1;
        JTextField usernameField = new JTextField(20);
        usernameField.setText(user.username);
        usernameField.setEditable(false);
        usernameField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("姓名:"), gbc);

        gbc.gridx = 1;
        JTextField nameField = new JTextField(20);
        nameField.setText(user.name);
        nameField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("角色:"), gbc);

        gbc.gridx = 1;
        JComboBox<String> roleComboBox = new JComboBox<>(new String[]{"管理员", "收银员", "财务"});
        roleComboBox.setFont(getGeneralFont(Font.PLAIN, 13));

        switch (user.role) {
            case "admin": roleComboBox.setSelectedIndex(0); break;
            case "cashier": roleComboBox.setSelectedIndex(1); break;
            case "finance": roleComboBox.setSelectedIndex(2); break;
        }

        panel.add(roleComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        JButton confirmButton = createStyledButton("确定", SUCCESS_COLOR);
        JButton cancelButton = createStyledButton("取消", GRAY_COLOR);

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        panel.add(buttonPanel, gbc);

        dialog.add(panel);

        confirmButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String roleText = (String) roleComboBox.getSelectedItem();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "姓名不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String role;
            switch (roleText) {
                case "管理员": role = "admin"; break;
                case "收银员": role = "cashier"; break;
                case "财务": role = "finance"; break;
                default: role = "cashier";
            }

            user.name = name;
            user.role = role;
            saveData();

            // 刷新表格
            tableModel.setRowCount(0);
            for (User u : users.values()) {
                tableModel.addRow(new Object[]{
                    u.username,
                    u.name,
                    u.getRoleDisplayName(),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(u.createTime),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(u.lastLoginTime),
                    u.active ? "正常" : "禁用"
                });
            }

            logOperation("编辑用户", "用户名: " + user.username + ", 新角色: " + roleText);
            dialog.dispose();
            JOptionPane.showMessageDialog(parentDialog, "用户信息修改成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    // 修改密码对话框
    private void showChangePasswordDialog(JDialog parentDialog, User user) {
        JDialog dialog = new JDialog(parentDialog, "修改密码", true);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(parentDialog);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("用户名:"), gbc);

        gbc.gridx = 1;
        JTextField usernameField = new JTextField(20);
        usernameField.setText(user.username);
        usernameField.setEditable(false);
        usernameField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("新密码:"), gbc);

        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("确认密码:"), gbc);

        gbc.gridx = 1;
        JPasswordField confirmPasswordField = new JPasswordField(20);
        confirmPasswordField.setFont(getGeneralFont(Font.PLAIN, 13));
        panel.add(confirmPasswordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        JButton confirmButton = createStyledButton("确定", SUCCESS_COLOR);
        JButton cancelButton = createStyledButton("取消", GRAY_COLOR);

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        panel.add(buttonPanel, gbc);

        dialog.add(panel);

        confirmButton.addActionListener(e -> {
            String password = new String(passwordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            // 验证密码
            if (password.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "密码不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (password.length() < 6) {
                JOptionPane.showMessageDialog(dialog, "密码长度不能少于6个字符！", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(dialog, "两次输入的密码不一致！", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            user.password = password;
            saveData();

            logOperation("修改密码", "用户名: " + user.username);
            dialog.dispose();
            JOptionPane.showMessageDialog(parentDialog, "密码修改成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    // 查看用户操作日志
    private void showUserOperationLogsDialog(JDialog parentDialog, String username) {
        JDialog dialog = new JDialog(parentDialog, "操作日志 - " + username, true);
        dialog.setSize(800, 500);
        dialog.setLocationRelativeTo(parentDialog);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 操作日志表格
        String[] columns = {"日志ID", "操作", "详情", "时间"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (OperationLog log : operationLogs) {
            if (log.username.equals(username)) {
                tableModel.addRow(new Object[]{
                    log.logId,
                    log.operation,
                    log.details,
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(log.timestamp)
                });
            }
        }

        JTable table = new JTable(tableModel);
        styleTable(table, tableModel);

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 关闭按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton closeButton = createStyledButton("关闭", GRAY_COLOR);
        buttonPanel.add(closeButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(panel);

        closeButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    // 备份数据
    private void backupData() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择备份文件保存位置");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".zip");
            }

            @Override
            public String getDescription() {
                return "ZIP 压缩文件 (*.zip)";
            }
        });
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File zipFile = fileChooser.getSelectedFile();
            if (!zipFile.getName().toLowerCase().endsWith(".zip")) {
                zipFile = new File(zipFile.getAbsolutePath() + ".zip");
            }
            
            try {
                // 创建ZIP文件
                java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new FileOutputStream(zipFile));
                File dataDir = new File("data");
                
                if (dataDir.exists() && dataDir.isDirectory()) {
                    backupDirectory(dataDir, dataDir.getName(), zos);
                }
                
                zos.close();
                
                logOperation("备份数据", "备份文件: " + zipFile.getAbsolutePath());
                JOptionPane.showMessageDialog(this, "数据备份成功！\n备份文件: " + zipFile.getAbsolutePath(), "备份成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "备份失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    // 递归备份目录
    private void backupDirectory(File dir, String basePath, java.util.zip.ZipOutputStream zos) throws IOException {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    backupDirectory(file, basePath + "/" + file.getName(), zos);
                } else {
                    java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(basePath + "/" + file.getName());
                    zos.putNextEntry(entry);
                    
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                    }
                    
                    zos.closeEntry();
                }
            }
        }
    }

    // 退出登录
    private void logout() {
        // 检查是否有进行中的班次
        if (currentShift != null) {
            int confirmShift = JOptionPane.showConfirmDialog(this,
                "您有进行中的班次未结束！\n\n" +
                "班次ID: " + currentShift.shiftId + "\n" +
                "开始时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(currentShift.startTime) + "\n\n" +
                "是否现在结束班次？",
                "未结束的班次",
                JOptionPane.YES_NO_CANCEL_OPTION);
            
            if (confirmShift == JOptionPane.YES_OPTION) {
                // 结束班次
                double currentRevenue = calculateTotalSales();
                int currentTransactionCount = transactions.size();
                
                // 计算各种支付方式的收入
                Map<String, Double> paymentRevenue = calculateShiftPaymentRevenue();
                double cashRevenue = paymentRevenue.get("现金");
                double wechatRevenue = paymentRevenue.get("微信支付");
                double alipayRevenue = paymentRevenue.get("支付宝");
                double cardRevenue = paymentRevenue.get("银行卡");
                
                currentShift.endShift(currentRevenue, currentTransactionCount, cashRevenue, wechatRevenue, alipayRevenue, cardRevenue);
                shifts.add(currentShift);
                
                saveData();
                logOperation("退出登录结束班次", "班次ID: " + currentShift.shiftId + ", 营业额: ¥" + String.format("%.2f", currentShift.shiftRevenue));
                
                currentShift = null;
            } else if (confirmShift == JOptionPane.CANCEL_OPTION) {
                // 取消退出
                return;
            }
            // 如果选择NO，继续退出但不结束班次
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "确定要退出登录吗？",
            "确认退出", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            saveData();
            dispose();
            showLoginDialog();
        }
    }
}
