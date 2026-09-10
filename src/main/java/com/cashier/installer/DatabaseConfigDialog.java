package com.cashier.installer;

import com.cashier.constant.DatabaseConfigKeys;
import com.cashier.constant.SystemPropertyKeys;

import com.cashier.util.DotEnv;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;

/**
 * Database Configuration Dialog
 * Simple GUI for configuring database connection
 */
public class DatabaseConfigDialog {
    private static final Logger logger = LoggerFactoryUtil.getLogger(DatabaseConfigDialog.class);

    private JFrame frame;
    private JComboBox<String> dbTypeCombo;
    private JComboBox<String> envCombo;
    private JTextField hostField;
    private JTextField portField;
    private JTextField dbNameField;
    private JTextField userField;
    private JPasswordField passField;
    private JButton saveButton;
    private JButton testButton;
    private JButton cancelButton;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default look and feel
        }
        SwingUtilities.invokeLater(() -> new DatabaseConfigDialog().show());
    }

    public void show() {
        frame = new JFrame("LiSuan - Database Configuration");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(550, 480);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        createUI();
        frame.setVisible(true);
    }

    private void createUI() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel titleLabel = new JLabel("LiSuan - Database Configuration", SwingConstants.CENTER);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        // Database Type
        gbc.gridy = 1; gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(new JLabel("Database Type:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        dbTypeCombo = new JComboBox<>(new String[]{"Local MySQL", "Docker MySQL", "Remote MySQL"});
        dbTypeCombo.addActionListener(e -> updateFields());
        mainPanel.add(dbTypeCombo, gbc);

        // Environment (development -> root, production -> lisuan, 与 .env 的 ENVIRONMENT 对齐)
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        mainPanel.add(new JLabel("Environment:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        envCombo = new JComboBox<>(new String[]{"development", "production"});
        String currentEnv = System.getenv("ENVIRONMENT");
        if ("production".equalsIgnoreCase(currentEnv)) {
            envCombo.setSelectedItem("production");
        }
        envCombo.addActionListener(e -> updateFields());
        mainPanel.add(envCombo, gbc);

        // Host
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        mainPanel.add(new JLabel("Host:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        hostField = new JTextField("localhost", 20);
        mainPanel.add(hostField, gbc);

        // Port
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        mainPanel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        portField = new JTextField("3306", 20);
        mainPanel.add(portField, gbc);

        // Database Name
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0;
        mainPanel.add(new JLabel("Database:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        dbNameField = new JTextField("lisuan_system", 20);
        mainPanel.add(dbNameField, gbc);

        // Username
        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0;
        mainPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        userField = new JTextField("root", 20);
        mainPanel.add(userField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 0;
        mainPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        passField = new JPasswordField("", 20);
        mainPanel.add(passField, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        cancelButton = new JButton("Cancel");
        testButton = new JButton("Test Connection");
        saveButton = new JButton("Save & Start");

        cancelButton.addActionListener(e -> {
            exitDialog();
        });
        testButton.addActionListener(e -> testConnection());
        saveButton.addActionListener(e -> saveAndStart());

        buttonPanel.add(cancelButton);
        buttonPanel.add(testButton);
        buttonPanel.add(saveButton);

        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2;
        mainPanel.add(buttonPanel, gbc);

        // Info label
        JLabel infoLabel = new JLabel("<html><center>Configure LiSuan database connection.<br>For first-time setup, the database will be created automatically.</center></html>");
        infoLabel.setForeground(Color.GRAY);
        gbc.gridy = 9;
        mainPanel.add(infoLabel, gbc);

        frame.add(mainPanel);

        // Initialize with default selection
        updateFields();
    }

    private void updateFields() {
        String type = (String) dbTypeCombo.getSelectedItem();
        // 根据界面选择的环境决定默认用户（生产环境使用 lisuan，开发环境使用 root）
        boolean isProduction = "production".equals(envCombo.getSelectedItem());
        String defaultUser = isProduction ? "lisuan" : "root";
        String passwordVariable = isProduction ? "CASHIER_DB_PASSWORD" : "MYSQL_ROOT_PASSWORD";
        String defaultPassword = System.getenv(passwordVariable);
        if (defaultPassword == null) {
            defaultPassword = "";
        }

        switch (type) {
            case "Local MySQL":
                hostField.setText("localhost");
                portField.setText("3306");
                dbNameField.setText("lisuan_system");
                userField.setText(defaultUser);
                passField.setText("");
                hostField.setEditable(false);
                portField.setEditable(false);
                break;
            case "Docker MySQL":
                hostField.setText("localhost");
                portField.setText("3306");
                dbNameField.setText("lisuan_system");
                userField.setText(defaultUser);
                passField.setText(defaultPassword);
                hostField.setEditable(false);
                portField.setEditable(false);
                break;
            case "Remote MySQL":
                hostField.setText("");
                portField.setText("3306");
                dbNameField.setText("lisuan_system");
                userField.setText("");
                passField.setText("");
                hostField.setEditable(true);
                portField.setEditable(true);
                break;
        }
    }

    private void testConnection() {
        String host = hostField.getText().trim();
        String port = portField.getText().trim();
        String dbName = dbNameField.getText().trim();
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());

        if (host.isEmpty() || user.isEmpty()) {
            showMessage("Please fill in Host and Username fields", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Try to load MySQL JDBC driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            showMessage("MySQL JDBC driver not found!\n\nPlease ensure the application is built.", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Test actual database connection
        try {
            String dbUrl = String.format("jdbc:mysql://%s:%s/?sslMode=PREFERRED&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
                    host, port);
            StringBuilder message = new StringBuilder();
            message.append("Database connection successful!\n\n");
            try (Connection conn = DriverManager.getConnection(dbUrl, user, pass);
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?")) {
                stmt.setString(1, dbName);
                try (ResultSet rs = stmt.executeQuery()) {

                    if (rs.next()) {
                        int tableCount = getTableCount(conn, dbName);
                        message.append("Database '").append(dbName).append("' exists.");
                        message.append("\nTables: ").append(tableCount);
                    } else {
                        message.append("Database '").append(dbName).append("' does not exist yet.\n");
                        message.append("It will be created automatically when you save.");
                    }
                }

                showMessage(message.toString(), JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException e) {
            String errorMsg = "Connection failed!\n\n";
            if (e.getMessage().contains("Communications link failure")) {
                errorMsg += "Could not connect to MySQL server.\n";
                errorMsg += "Please check:\n";
                errorMsg += "- MySQL is running on " + host + ":" + port + "\n";
                errorMsg += "- Firewall is not blocking the connection";
            } else if (e.getMessage().contains("Access denied")) {
                errorMsg += "Authentication failed!\n";
                errorMsg += "Please check your username and password.";
            } else {
                errorMsg += "Error: " + e.getMessage();
            }
            showMessage(errorMsg, JOptionPane.ERROR_MESSAGE);
        }
    }

    private int getTableCount(Connection conn, String dbName) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?")) {
            stmt.setString(1, dbName);
            try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            }
        }
        return 0;
    }

    private static String validateDatabaseName(String dbName) {
        if (dbName == null || !dbName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Database name may only contain letters, numbers, and underscores");
        }
        return dbName;
    }

    private void saveAndStart() {
        DatabaseDialogInput input = readDialogInput();

        if (input.host().isEmpty() || input.port().isEmpty() || input.user().isEmpty()) {
            showMessage("Please fill in Host, Port, and Username fields", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            input = input.withDatabaseName(validateDatabaseName(input.dbName()));
        } catch (IllegalArgumentException e) {
            showMessage(e.getMessage(), JOptionPane.WARNING_MESSAGE);
            return;
        }

        saveButton.setEnabled(false);
        testButton.setEnabled(false);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            boolean needsInit = ensureDatabaseReady(input);
            initializeDatabaseIfNeeded(input, needsInit);
            writeDatabaseConfig(input);
            finishSuccessfulSave(input.dbName());

        } catch (ClassNotFoundException e) {
            showMessage("MySQL JDBC driver not found!", JOptionPane.ERROR_MESSAGE);
            saveButton.setEnabled(true);
            testButton.setEnabled(true);
        } catch (Exception e) {
            showMessage(buildDatabaseOperationError(e, input), JOptionPane.ERROR_MESSAGE);
            saveButton.setEnabled(true);
            testButton.setEnabled(true);
        }
    }

    private DatabaseDialogInput readDialogInput() {
        return new DatabaseDialogInput(
            hostField.getText().trim(),
            portField.getText().trim(),
            dbNameField.getText().trim(),
            userField.getText().trim(),
            new String(passField.getPassword())
        );
    }

    private boolean ensureDatabaseReady(DatabaseDialogInput input) throws SQLException {
        String dbUrl = String.format("jdbc:mysql://%s:%s/?sslMode=PREFERRED&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
                input.host(), input.port());

        try (Connection conn = DriverManager.getConnection(dbUrl, input.user(), input.pass())) {
            if (!databaseExists(conn, input.dbName())) {
                createDatabase(conn, input.dbName());
                return true;
            }
            return getTableCount(conn, input.dbName()) == 0;
        }
    }

    private boolean databaseExists(Connection conn, String dbName) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?")) {
            stmt.setString(1, dbName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void createDatabase(Connection conn, String dbName) throws SQLException {
        // dbName 已通过 validateDatabaseName 白名单校验，仅允许 [A-Za-z0-9_]
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE `" + dbName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
    }

    private void initializeDatabaseIfNeeded(DatabaseDialogInput input, boolean needsInit) {
        if (!needsInit) {
            return;
        }
        showMessage("Database is empty. Initializing...", JOptionPane.INFORMATION_MESSAGE);
        if (!importInitScript(input.host(), input.port(), input.dbName(), input.user(), input.pass())) {
            showMessage("Database created but initialization script failed.\nThe database may need manual setup.\n\nConfiguration has been saved.", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void writeDatabaseConfig(DatabaseDialogInput input) throws IOException {
        Path configDir = Paths.get("config");
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }

        String fullDbUrl = String.format("jdbc:mysql://%s:%s/%s?sslMode=PREFERRED&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=UTF-8",
                input.host(), input.port(), input.dbName());

        // 密码一律不写进 config/（发布门禁禁止 config/database.properties 出现 db.password），
        // 走环境变量 / .env；开发模式由下方写入 .env，生产模式由运维注入。
        String config = String.format(
            "# Database Configuration\n" +
            "# Generated by DatabaseConfigDialog\n" +
            "# Production deployments should provide the password through CASHIER_DB_PASSWORD.\n" +
            "db.url=%s\n" +
            "db.username=%s\n" +
            "db.password=\n" +
            "db.pool.size=10\n" +
            "db.connection.timeout=30000\n" +
            "db.idle.timeout=600000\n" +
            "db.max.lifetime=1800000\n",
            fullDbUrl, input.user());

        Files.write(configDir.resolve(DatabaseConfigKeys.DATABASE_PROPERTIES_FILE), config.getBytes(StandardCharsets.UTF_8));

        storePasswordOutsideConfig(input.pass());
    }

    /**
     * 开发模式把密码写进 {@code .env}（已 gitignore，启动脚本与应用都会读取）；
     * 生产模式保持不落盘，由运维通过环境变量注入。
     */
    private void storePasswordOutsideConfig(String password) {
        if (isProductionEnvironment() || password == null || password.isEmpty()) {
            return;
        }
        try {
            DotEnv.upsert(DotEnv.DB_PASSWORD_KEY, password);
            logger.info("数据库密码已写入 {}（未写入 config/）", DotEnv.FILE_NAME);
        } catch (IOException e) {
            logger.warn("写入 {} 失败: {}", DotEnv.FILE_NAME, e.getMessage(), e);
        }
    }

    private boolean isProductionEnvironment() {
        // 优先使用界面选择；未初始化时回退到环境变量（兼容 .env 加载场景）
        return envCombo != null
            ? "production".equals(envCombo.getSelectedItem())
            : "production".equalsIgnoreCase(System.getenv("ENVIRONMENT"));
    }

    private void finishSuccessfulSave(String dbName) {
        showMessage("Configuration saved successfully!\n\nDatabase '" + dbName + "' is ready.\n\nYou can now run start.bat to launch the application.", JOptionPane.INFORMATION_MESSAGE);

        int option = JOptionPane.showConfirmDialog(frame,
            "Configuration saved!\n\nDo you want to start the application now?",
            "Start Application",
            JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            startApplication();
        }

        exitDialog();
    }

    private void exitDialog() {
        if (frame != null) {
            frame.dispose();
        }
        System.exit(0);
    }

    private String buildDatabaseOperationError(Exception e, DatabaseDialogInput input) {
        String errorMsg = "Database operation failed!\n\n";
        if (e instanceof SQLException se) {
            if (se.getMessage().contains("Communications link failure")) {
                return errorMsg + "Could not connect to MySQL server.\n"
                    + "Please check:\n"
                    + "- MySQL is running on " + input.host() + ":" + input.port() + "\n"
                    + "- Firewall is not blocking the connection";
            }
            if (se.getMessage().contains("Access denied")) {
                return errorMsg + "Authentication failed!\n"
                    + "Please check your username and password.";
            }
            return errorMsg + "Error: " + se.getMessage();
        }
        return errorMsg + "Error: " + e.getMessage();
    }

    private record DatabaseDialogInput(String host, String port, String dbName, String user, String pass) {
        DatabaseDialogInput withDatabaseName(String validatedDbName) {
            return new DatabaseDialogInput(host, port, validatedDbName, user, pass);
        }
    }

    private boolean importInitScript(String host, String port, String dbName, String user, String pass) {
        File initScript = new File("docker/mysql-init/00-init-complete.sql");
        if (!initScript.exists()) {
            logger.warn("Init script not found: {}", initScript.getAbsolutePath());
            return false;
        }

        try (Connection conn = DriverManager.getConnection(
                String.format("jdbc:mysql://%s:%s/%s?sslMode=PREFERRED&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
                    host, port, dbName), user, pass)) {

            // Read and execute the script
            StringBuilder sqlBuffer = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(initScript, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    // Skip empty lines and comments
                    if (line.isEmpty() || line.startsWith("--") || line.startsWith("#")) {
                        continue;
                    }
                    sqlBuffer.append(line).append("\n");

                    // Execute on delimiter
                    if (line.endsWith(";")) {
                        String sql = sqlBuffer.toString();
                        sqlBuffer.setLength(0);
                        if (!sql.trim().isEmpty()) {
                            try (Statement stmt = conn.createStatement()) {
                                stmt.execute(sql);
                            } catch (SQLException e) {
                                // Some statements might fail if already exists, that's ok
                                logger.warn("SQL execution failed (may be ok): {}", e.getMessage());
                            }
                        }
                    }
                }
            }

            return true;
        } catch (Exception e) {
            logger.error("Failed to import init script: {}", e.getMessage(), e);
            return false;
        }
    }

    private void startApplication() {
        try {
            // Get the current working directory
            File currentDir = new File(System.getProperty(SystemPropertyKeys.USER_DIR));
            File targetDir = new File(currentDir, "target");

            // Find the JAR file in target directory
            File[] files = targetDir.listFiles((dir, name) -> name.endsWith("-jar-with-dependencies.jar"));

            if (files != null && files.length > 0) {
                // Try to run start.bat
                File startBat = new File(currentDir, "start.bat");
                if (startBat.exists()) {
                    new ProcessBuilder("cmd", "/c", "start", "", "start.bat", "--gui")
                            .directory(currentDir)
                            .start();
                    showMessage("Application starting...", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    showMessage("start.bat not found. Please run it manually.", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                showMessage("Could not find application JAR file.\n\nExpected location: " + targetDir.getAbsolutePath() + "\n\nPlease run: mvn clean package", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            showMessage("Failed to start application: " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showMessage(String message, int messageType) {
        JOptionPane.showMessageDialog(frame, message, "LiSuan Database Configuration", messageType);
    }
}
