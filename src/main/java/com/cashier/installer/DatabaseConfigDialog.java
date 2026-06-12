package com.cashier.installer;

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
    private JFrame frame;
    private JComboBox<String> dbTypeCombo;
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
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(550, 450);
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
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
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

        // Host
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        mainPanel.add(new JLabel("Host:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        hostField = new JTextField("localhost", 20);
        mainPanel.add(hostField, gbc);

        // Port
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        mainPanel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        portField = new JTextField("3306", 20);
        mainPanel.add(portField, gbc);

        // Database Name
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        mainPanel.add(new JLabel("Database:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        dbNameField = new JTextField("lisuan_system", 20);
        mainPanel.add(dbNameField, gbc);

        // Username
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0;
        mainPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        userField = new JTextField("root", 20);
        mainPanel.add(userField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0;
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
            frame.dispose();
            System.exit(0);
        });
        testButton.addActionListener(e -> testConnection());
        saveButton.addActionListener(e -> saveAndStart());

        buttonPanel.add(cancelButton);
        buttonPanel.add(testButton);
        buttonPanel.add(saveButton);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        mainPanel.add(buttonPanel, gbc);

        // Info label
        JLabel infoLabel = new JLabel("<html><center>Configure LiSuan database connection.<br>For first-time setup, the database will be created automatically.</center></html>");
        infoLabel.setForeground(Color.GRAY);
        gbc.gridy = 8;
        mainPanel.add(infoLabel, gbc);

        frame.add(mainPanel);

        // Initialize with default selection
        updateFields();
    }

    private void updateFields() {
        String type = (String) dbTypeCombo.getSelectedItem();
        switch (type) {
            case "Local MySQL":
                hostField.setText("localhost");
                portField.setText("3306");
                dbNameField.setText("lisuan_system");
                userField.setText("root");
                passField.setText("");
                hostField.setEditable(false);
                portField.setEditable(false);
                break;
            case "Docker MySQL":
                hostField.setText("localhost");
                portField.setText("3306");
                dbNameField.setText("lisuan_system");
                userField.setText("root");
                passField.setText("RootPassword123!");
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
        Connection conn = null;
        Statement stmt = null;
        try {
            String dbUrl = String.format("jdbc:mysql://%s:%s/?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
                    host, port);
            conn = DriverManager.getConnection(dbUrl, user, pass);

            // Check if database exists
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + dbName + "'");

            StringBuilder message = new StringBuilder();
            message.append("Database connection successful!\n\n");

            if (rs.next()) {
                int tableCount = getTableCount(conn, dbName);
                message.append("Database '").append(dbName).append("' exists.");
                message.append("\nTables: ").append(tableCount);
            } else {
                message.append("Database '").append(dbName).append("' does not exist yet.\n");
                message.append("It will be created automatically when you save.");
            }
            rs.close();

            showMessage(message.toString(), JOptionPane.INFORMATION_MESSAGE);

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
        } finally {
            try { if (stmt != null) stmt.close(); } catch (Exception e) { /* ignore */ }
            try { if (conn != null) conn.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    private int getTableCount(Connection conn, String dbName) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '" + dbName + "'")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private void saveAndStart() {
        String host = hostField.getText().trim();
        String port = portField.getText().trim();
        String dbName = dbNameField.getText().trim();
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());

        if (host.isEmpty() || port.isEmpty() || user.isEmpty()) {
            showMessage("Please fill in Host, Port, and Username fields", JOptionPane.WARNING_MESSAGE);
            return;
        }

        saveButton.setEnabled(false);
        testButton.setEnabled(false);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Connect without specifying database first
            String dbUrl = String.format("jdbc:mysql://%s:%s/?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
                    host, port);

            // Check if database exists and create if needed
            boolean dbExists;
            boolean needsInit = false;

            try (Connection conn = DriverManager.getConnection(dbUrl, user, pass);
                 Statement stmt = conn.createStatement()) {

                ResultSet rs = stmt.executeQuery("SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + dbName + "'");
                dbExists = rs.next();
                rs.close();

                if (!dbExists) {
                    // Create database
                    stmt.execute("CREATE DATABASE " + dbName + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                    needsInit = true;
                } else {
                    // Check if tables exist
                    rs = stmt.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '" + dbName + "'");
                    if (rs.next() && rs.getInt(1) == 0) {
                        needsInit = true;
                    }
                    rs.close();
                }
            }

            // Import initialization script if needed
            if (needsInit) {
                showMessage("Database is empty. Initializing...", JOptionPane.INFORMATION_MESSAGE);
                if (!importInitScript(host, port, dbName, user, pass)) {
                    showMessage("Database created but initialization script failed.\nThe database may need manual setup.\n\nConfiguration has been saved.", JOptionPane.WARNING_MESSAGE);
                }
            }

            // Create config directory
            Path configDir = Paths.get("config");
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            // Write database.properties
            String fullDbUrl = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4",
                    host, port, dbName);

            String config = String.format(
                "# Database Configuration\n" +
                "# Generated by DatabaseConfigDialog\n" +
                "db.url=%s\n" +
                "db.username=%s\n" +
                "db.password=%s\n" +
                "db.pool.size=10\n" +
                "db.connection.timeout=30000\n" +
                "db.idle.timeout=600000\n" +
                "db.max.lifetime=1800000\n",
                fullDbUrl, user, pass);

            Files.write(configDir.resolve("database.properties"), config.getBytes(StandardCharsets.UTF_8));

            showMessage("Configuration saved successfully!\n\nDatabase '" + dbName + "' is ready.\n\nYou can now run start.bat to launch the application.", JOptionPane.INFORMATION_MESSAGE);

            // Ask if user wants to start the application
            int option = JOptionPane.showConfirmDialog(frame,
                "Configuration saved!\n\nDo you want to start the application now?",
                "Start Application",
                JOptionPane.YES_NO_OPTION);

            if (option == JOptionPane.YES_OPTION) {
                startApplication();
            }

            frame.dispose();
            System.exit(0);

        } catch (ClassNotFoundException e) {
            showMessage("MySQL JDBC driver not found!", JOptionPane.ERROR_MESSAGE);
            saveButton.setEnabled(true);
            testButton.setEnabled(true);
        } catch (Exception e) {
            String errorMsg = "Database operation failed!\n\n";
            if (e instanceof SQLException) {
                SQLException se = (SQLException) e;
                if (se.getMessage().contains("Communications link failure")) {
                    errorMsg += "Could not connect to MySQL server.\n";
                    errorMsg += "Please check:\n";
                    errorMsg += "- MySQL is running on " + host + ":" + port + "\n";
                    errorMsg += "- Firewall is not blocking the connection";
                } else if (se.getMessage().contains("Access denied")) {
                    errorMsg += "Authentication failed!\n";
                    errorMsg += "Please check your username and password.";
                } else {
                    errorMsg += "Error: " + se.getMessage();
                }
            } else {
                errorMsg += "Error: " + e.getMessage();
            }
            showMessage(errorMsg, JOptionPane.ERROR_MESSAGE);
            saveButton.setEnabled(true);
            testButton.setEnabled(true);
        }
    }

    private boolean importInitScript(String host, String port, String dbName, String user, String pass) {
        File initScript = new File("docker/mysql-init/00-init-complete.sql");
        if (!initScript.exists()) {
            System.err.println("[WARN] Init script not found: " + initScript.getAbsolutePath());
            return false;
        }

        try (Connection conn = DriverManager.getConnection(
                String.format("jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
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
                                System.err.println("[WARN] SQL execution failed (may be ok): " + e.getMessage());
                            }
                        }
                    }
                }
            }

            return true;
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to import init script: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void startApplication() {
        try {
            // Get the current working directory
            File currentDir = new File(System.getProperty("user.dir"));
            File targetDir = new File(currentDir, "target");

            // Find the JAR file in target directory
            File[] files = targetDir.listFiles((dir, name) -> name.endsWith("-jar-with-dependencies.jar"));

            if (files != null && files.length > 0) {
                // Try to run start.bat
                File startBat = new File(currentDir, "start.bat");
                if (startBat.exists()) {
                    Runtime.getRuntime().exec("cmd /c start /wait start.bat", null, currentDir);
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
