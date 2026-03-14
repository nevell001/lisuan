package com.cashier.installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 收银系统图形化安装程序
 */
public class Installer {
    private static final String APP_VERSION = "2.4.5";
    private static final String DB_NAME = "cashier_system";
    
    private JFrame frame;
    private JTextArea logArea;
    private JButton nextButton;
    private JButton cancelButton;
    private int currentStep = 0;
    
    // Database configuration
    private String dbType = "docker"; // docker or local
    private String dbHost = "localhost";
    private String dbPort = "3306";
    private String dbUsername = "root";
    private String dbPassword = "RootPassword123!";
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // UI look and feel 设置失败，使用默认设置
            System.err.println("无法设置系统外观: " + e.getMessage());
        }
        new Installer().start();
    }
    
    public void start() {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("收银系统安装程序 v" + APP_VERSION);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(700, 500);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            
            createUI();
            frame.setVisible(true);
            
            checkEnvironment();
        });
    }
    
    private void createUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(70, 130, 180));
        headerPanel.setPreferredSize(new Dimension(700, 60));
        
        JLabel titleLabel = new JLabel("收银系统安装程序", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 20));
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Content area
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        // Use Chinese-compatible font
        logArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        logArea.setBackground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("安装日志"));
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        cancelButton = new JButton("取消");
        cancelButton.addActionListener(e -> System.exit(0));
        
        nextButton = new JButton("下一步");
        nextButton.addActionListener(this::handleNext);
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(nextButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        frame.getContentPane().add(mainPanel);
    }
    
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    private void handleNext(ActionEvent e) {
        if (currentStep == 0) {
            showDatabaseTypeDialog();
        } else if (currentStep == 1) {
            showDatabaseConfigDialog();
        } else if (currentStep == 2) {
            startInstallation();
        }
    }
    
    private void checkEnvironment() {
        log("正在检查系统环境...\n");
        
        boolean hasError = false;
        
        // Check Java
        String javaVersion = System.getProperty("java.version");
        if (javaVersion != null) {
            log("[✓] Java 版本: " + javaVersion);
            // Check if Java 17+
            String majorVersion = javaVersion.split("\\.")[0];
            try {
                int ver = Integer.parseInt(majorVersion);
                if (ver < 17) {
                    log("[✗] Java 版本过低，需要 JDK 17 或更高版本");
                    hasError = true;
                }
            } catch (NumberFormatException e) {
                // Version 1.8.x format
                if (javaVersion.startsWith("1.8")) {
                    log("[✗] Java 版本过低，需要 JDK 17 或更高版本");
                    hasError = true;
                }
            }
        } else {
            log("[✗] Java 未安装");
            hasError = true;
        }
        
        // Check Maven
        boolean hasMaven = checkCommand("mvn --version");
        if (hasMaven) {
            log("[✓] Maven 已安装");
        } else {
            log("[✗] Maven 未安装");
            hasError = true;
        }
        
        // Check Docker
        boolean hasDocker = checkCommand("docker --version");
        if (hasDocker) {
            log("[✓] Docker 已安装");
        } else {
            log("[⚠] Docker 未安装（可选，用于 MySQL 数据库）");
        }
        
        log("");
        
        if (hasError) {
            showMissingToolsDialog(hasMaven, hasDocker);
        } else {
            log("环境检查完成！所有必需工具已安装。");
            log("");
            log("点击 [下一步] 继续安装...");
            currentStep = 0;
        }
    }
    
    private void showMissingToolsDialog(boolean hasMaven, boolean hasDocker) {
        StringBuilder message = new StringBuilder();
        message.append("检测到缺少以下必需工具：\n\n");
        
        String javaVersion = System.getProperty("java.version");
        if (javaVersion == null || javaVersion.startsWith("1.8")) {
            message.append("• Java 17+\n");
            message.append("  下载地址: https://www.oracle.com/java/technologies/downloads/\n");
            message.append("  或使用 OpenJDK: https://adoptium.net/\n\n");
        }
        
        if (!hasMaven) {
            message.append("• Maven 3.8+\n");
            message.append("  下载地址: https://maven.apache.org/download.cgi\n");
            message.append("  或使用包管理器安装:\n");
            message.append("    Windows: choco install maven\n");
            message.append("    macOS: brew install maven\n");
            message.append("    Linux: sudo apt install maven\n\n");
        }
        
        if (!hasDocker) {
            message.append("• Docker Desktop（可选）\n");
            message.append("  下载地址: https://www.docker.com/products/docker-desktop\n\n");
        }
        
        message.append("请先安装缺少的工具，然后重新运行安装程序。");
        
        Object[] options = {"已安装完成，重新检查", "跳过检查（不推荐）", "取消"};
        int choice = JOptionPane.showOptionDialog(frame,
            message.toString(),
            "缺少必需工具",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[0]);
        
        if (choice == 0) {
            // Re-check
            log("重新检查环境...");
            logArea.setText("");
            checkEnvironment();
        } else if (choice == 1) {
            // Skip and continue
            log("[⚠] 已跳过环境检查，可能遇到问题...");
            log("");
            log("点击 [下一步] 继续安装...");
            currentStep = 0;
        } else {
            // Cancel
            System.exit(0);
        }
    }
    
    private boolean checkCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void showDatabaseTypeDialog() {
        String[] options = {"Docker MySQL（推荐）", "本地 MySQL", "跳过数据库配置"};
        int choice = JOptionPane.showOptionDialog(frame,
            "请选择数据库安装方式：\n\n" +
            "• Docker MySQL：自动安装和管理 MySQL（推荐）\n" +
            "• 本地 MySQL：使用已安装的 MySQL\n" +
            "• 跳过配置：稍后手动配置",
            "数据库安装方式",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);
        
        if (choice == 0) {
            dbType = "docker";
            log("已选择：Docker MySQL");
            showDatabaseConfigDialog();
        } else if (choice == 1) {
            dbType = "local";
            log("已选择：本地 MySQL");
            showDatabaseConfigDialog();
        } else if (choice == 2) {
            dbType = "skip";
            log("已选择：跳过数据库配置");
            startInstallation();
        } else {
            return;
        }
    }
    
    private void showDatabaseConfigDialog() {
        currentStep = 1;
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("数据库主机:"), gbc);
        gbc.gridx = 1;
        JTextField hostField = new JTextField("localhost", 20);
        panel.add(hostField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("数据库端口:"), gbc);
        gbc.gridx = 1;
        JTextField portField = new JTextField("3306", 20);
        panel.add(portField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("用户名:"), gbc);
        gbc.gridx = 1;
        JTextField usernameField = new JTextField("root", 20);
        panel.add(usernameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("密码:"), gbc);
        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField("RootPassword123!", 20);
        panel.add(passwordField, gbc);
        
        int result = JOptionPane.showConfirmDialog(frame, panel, 
            "数据库配置", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            dbHost = hostField.getText().trim();
            dbPort = portField.getText().trim();
            dbUsername = usernameField.getText().trim();
            dbPassword = new String(passwordField.getPassword());
            
            log("数据库配置：");
            log("  主机: " + dbHost);
            log("  端口: " + dbPort);
            log("  用户: " + dbUsername);
            
            currentStep = 2;
            startInstallation();
        }
    }
    
    private void startInstallation() {
        nextButton.setEnabled(false);
        cancelButton.setEnabled(false);
        
        log("");
        log("开始安装...");
        log("");
        
        new Thread(() -> {
            try {
                install();
            } catch (Exception e) {
                log("安装失败: " + e.getMessage());
                // 记录堆栈跟踪信息
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                log(sw.toString());

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame,
                        "安装失败：\n" + e.getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
                    nextButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                });
            }
        }).start();
    }
    
    private void install() throws Exception {
        // Step 1: Download dependencies
        log("[1/5] 下载 Maven 依赖...");
        executeCommand("mvn dependency:resolve", new File("."));
        log("✓ 依赖下载完成");
        
        // Step 2: Compile project
        log("[2/5] 编译项目...");
        executeCommand("mvn clean package -DskipTests", new File("."));
        log("✓ 项目编译完成");
        
        // Step 3: Setup database
        if (!"skip".equals(dbType)) {
            log("[3/5] 配置数据库...");
            setupDatabase();
            log("✓ 数据库配置完成");
        } else {
            log("[3/5] 跳过数据库配置");
        }
        
        // Step 4: Create configuration
        log("[4/5] 创建配置文件...");
        createConfigFiles();
        log("✓ 配置文件创建完成");
        
        // Step 5: Complete
        log("[5/5] 安装完成！");
        log("");
        log("========================================");
        log("安装成功完成！");
        log("========================================");
        log("");
        log("启动方式：");
        log("  • Windows: 双击 start.bat");
        log("  • Linux/Mac: 运行 ./start.sh");
        log("");
        log("默认登录：");
        log("  用户名: admin");
        log("  密码: admin123");
        log("");
        
        SwingUtilities.invokeLater(() -> {
            nextButton.setText("完成");
            nextButton.setEnabled(true);
            nextButton.addActionListener(e -> {
                JOptionPane.showMessageDialog(frame, 
                    "安装完成！\n\n请运行 start.bat 启动应用", 
                    "完成", JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);
            });
        });
    }
    
    private void setupDatabase() throws Exception {
        if ("docker".equals(dbType)) {
            // Start Docker MySQL
            log("  启动 Docker MySQL...");
            executeCommand("docker-compose up -d mysql", new File("."));
            
            // Wait for MySQL to be ready
            log("  等待 MySQL 启动...");
            Thread.sleep(10000);
            
            // Import sample data
            log("  导入示例数据...");
            executeCommand("docker exec cashier-mysql mysql -uroot -pRootPassword123! --default-character-set=utf8mb4 " + DB_NAME + " < docker/mysql-init/03-sample-data.sql", new File("."));
            
        } else {
            // Use local MySQL
            log("  配置本地 MySQL...");
            // Just update config, user needs to ensure MySQL is running
        }
    }
    
    private void createConfigFiles() throws Exception {
        File configDir = new File("config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        // Create database.properties
        String dbUrl = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=UTF-8",
            dbHost, dbPort, DB_NAME);
        
        String dbConfig = String.format(
            "# Database Configuration\n" +
            "db.url=%s\n" +
            "db.username=%s\n" +
            "db.password=%s\n" +
            "db.pool.size=10\n" +
            "db.connection.timeout=30000\n" +
            "db.idle.timeout=600000\n" +
            "db.max.lifetime=1800000\n",
            dbUrl, dbUsername, dbPassword);
        
        Files.write(Paths.get("config/database.properties"), dbConfig.getBytes(StandardCharsets.UTF_8));
        
        log("  已创建 config/database.properties");
    }
    
    private void executeCommand(String command, File directory) throws Exception {
        log("  执行: " + command);
        
        ProcessBuilder pb = new ProcessBuilder();
        if (isWindows()) {
            pb.command("cmd", "/c", command);
        } else {
            pb.command("sh", "-c", command);
        }
        pb.directory(directory);
        pb.redirectErrorStream(true);
        
        // Set console encoding for Windows
        if (isWindows()) {
            pb.environment().put("MAVEN_OPTS", "-Dfile.encoding=GBK");
        }
        
        Process process = pb.start();
        
        // Read output with appropriate encoding
        String charset = isWindows() ? "GBK" : "UTF-8";
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    log("    " + line);
                }
            }
        } catch (UnsupportedEncodingException e) {
            // Fallback to system default encoding
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        log("    " + line);
                    }
                }
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("命令执行失败，退出码: " + exitCode);
        }
    }
    
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}