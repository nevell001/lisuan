package com.cashier.installer;

import com.cashier.constant.SystemPropertyKeys;
import com.cashier.util.DotEnv;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;

/**
 * 狸算(LiSuan)收银系统图形化安装程序
 */
public class Installer {
    private static final Logger LOGGER = Logger.getLogger(Installer.class.getName());
    private static final String APP_VERSION = "2.6.0";
    private static final String DB_NAME = "lisuan_system";
    private static final int MYSQL_READY_TIMEOUT_SECONDS = 60;
    private static final int MYSQL_READY_CHECK_INTERVAL_SECONDS = 2;
    private static final int COMMAND_CHECK_TIMEOUT_SECONDS = 10;
    private static final int INSTALL_COMMAND_TIMEOUT_SECONDS = 10 * 60;
    
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
    private String dbPassword = "";
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // UI look and feel 设置失败，使用默认设置
            LOGGER.log(Level.WARNING, "无法设置系统外观: {0}", e.getMessage());
        }
        new Installer().start();
    }
    
    public void start() {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("狸算(LiSuan)收银系统安装程序 v" + APP_VERSION);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
        
        JLabel titleLabel = new JLabel("狸算(LiSuan)收银系统安装程序", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Content area
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        // Use Chinese-compatible font
        logArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        logArea.setBackground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("安装日志"));
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        cancelButton = new JButton("取消");
        cancelButton.addActionListener(e -> exitInstaller());
        
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
            exitInstaller();
        }
    }
    
    private boolean checkCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            boolean completed = process.waitFor(COMMAND_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                process.waitFor(1, TimeUnit.SECONDS);
                return false;
            }
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
        JPasswordField passwordField = new JPasswordField(20);
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
                // 记录堆栈跟踪信息到日志
                LOGGER.log(Level.SEVERE, "安装失败", e);

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
        log("  注意: 首次登录请使用初始化时生成的随机密码并立即修改");
        log("");
        
        SwingUtilities.invokeLater(() -> {
            nextButton.setText("完成");
            nextButton.setEnabled(true);
            nextButton.addActionListener(e -> {
                JOptionPane.showMessageDialog(frame, 
                    "安装完成！\n\n请运行 start.bat 启动应用", 
                    "完成", JOptionPane.INFORMATION_MESSAGE);
                exitInstaller();
            });
        });
    }

    private void exitInstaller() {
        if (frame != null) {
            frame.dispose();
        }
        System.exit(0);
    }
    
    private void setupDatabase() throws Exception {
        if ("docker".equals(dbType)) {
            // Start Docker MySQL
            log("  启动 Docker MySQL...");
            executeCommand("docker-compose up -d mysql", new File("."));
            
            // Wait for MySQL to be ready
            log("  等待 MySQL 启动...");
            waitForDockerMysqlReady();
            
            // Import sample data
            log("  导入示例数据...");
            executeCommand("docker exec lisuan-mysql mysql -uroot -p" + dbPassword + " --default-character-set=utf8mb4 " + DB_NAME + " < docker/mysql-init/00-init-complete.sql", new File("."));
            
        } else {
            // Use local MySQL
            log("  配置本地 MySQL...");
            // Just update config, user needs to ensure MySQL is running
        }
    }

    private void waitForDockerMysqlReady() throws Exception {
        String command = "docker exec lisuan-mysql mysqladmin ping -uroot -p" + dbPassword + " --silent";
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(MYSQL_READY_TIMEOUT_SECONDS);

        while (System.nanoTime() < deadline) {
            if (commandSucceeds(command, new File("."))) {
                log("  MySQL 已就绪");
                return;
            }
            TimeUnit.SECONDS.sleep(MYSQL_READY_CHECK_INTERVAL_SECONDS);
        }

        throw new Exception("MySQL 在 " + MYSQL_READY_TIMEOUT_SECONDS + " 秒内未就绪，请检查 Docker 容器和数据库密码配置");
    }
    
    private void createConfigFiles() throws Exception {
        File configDir = new File("config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        // Create database.properties
        String dbUrl = String.format("jdbc:mysql://%s:%s/%s?sslMode=PREFERRED&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=UTF-8",
            dbHost, dbPort, DB_NAME);
        
        // 密码一律不写进 config/（发布门禁禁止 config/database.properties 出现 db.password），
        // 走环境变量 / .env；开发模式由下方写入 .env，生产模式由运维注入。
        String dbConfig = String.format(
            "# Database Configuration\n" +
            "# Production deployments should provide the password through CASHIER_DB_PASSWORD.\n" +
            "db.url=%s\n" +
            "db.username=%s\n" +
            "db.password=\n" +
            "db.pool.size=10\n" +
            "db.connection.timeout=30000\n" +
            "db.idle.timeout=600000\n" +
            "db.max.lifetime=1800000\n",
            dbUrl, dbUsername);
        
        Files.write(Paths.get("config/database.properties"), dbConfig.getBytes(StandardCharsets.UTF_8));
        
        log("  已创建 config/database.properties");
        storePasswordOutsideConfig();
    }

    /**
     * 开发模式把密码写进 {@code .env}（已 gitignore，启动脚本与应用都会读取）；
     * 生产模式保持不落盘，由运维通过环境变量注入。
     */
    private void storePasswordOutsideConfig() {
        if (isProductionEnvironment()) {
            log("  生产环境未写入数据库密码，请通过 CASHIER_DB_PASSWORD 环境变量提供");
            return;
        }
        if (dbPassword == null || dbPassword.isEmpty()) {
            return;
        }
        try {
            DotEnv.upsert(DotEnv.DB_PASSWORD_KEY, dbPassword);
            log("  开发环境数据库密码已写入 " + DotEnv.FILE_NAME + "（未写入 config/）");
        } catch (IOException e) {
            log("  [WARN] 写入 " + DotEnv.FILE_NAME + " 失败: " + e.getMessage());
        }
    }

    private boolean isProductionEnvironment() {
        return "production".equalsIgnoreCase(System.getenv("ENVIRONMENT"));
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
        java.nio.charset.Charset charset = isWindows()
                ? java.nio.charset.Charset.forName("GBK")
                : StandardCharsets.UTF_8;
        Thread outputReader = readProcessOutputAsync(process, charset);

        int exitCode = waitForProcess(process, command, INSTALL_COMMAND_TIMEOUT_SECONDS);
        outputReader.join(TimeUnit.SECONDS.toMillis(1));
        if (exitCode != 0) {
            throw new Exception("命令执行失败，退出码: " + exitCode);
        }
    }

    private Thread readProcessOutputAsync(Process process, java.nio.charset.Charset charset) {
        Thread outputReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), charset))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        log("    " + line);
                    }
                }
            } catch (IOException e) {
                log("    读取命令输出失败: " + e.getMessage());
            }
        }, "installer-command-output-reader");
        outputReader.setDaemon(true);
        outputReader.start();
        return outputReader;
    }

    private int waitForProcess(Process process, String command, int timeoutSeconds) throws InterruptedException {
        boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
            throw new InterruptedException("命令执行超时（" + timeoutSeconds + "秒）: " + command);
        }
        return process.exitValue();
    }

    private boolean commandSucceeds(String command, File directory) {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            if (isWindows()) {
                pb.command("cmd", "/c", command);
            } else {
                pb.command("sh", "-c", command);
            }
            pb.directory(directory);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            boolean completed = process.waitFor(MYSQL_READY_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "命令尚未成功: {0}", command);
            return false;
        }
    }
    
    private boolean isWindows() {
        return System.getProperty(SystemPropertyKeys.OS_NAME).toLowerCase().contains("win");
    }
}
