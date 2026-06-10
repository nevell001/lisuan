package com.cashier.installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

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
        frame.setSize(500, 400);
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
        mainPanel.add(new JLabel("Database Type:"), gbc);
        gbc.gridx = 1;
        dbTypeCombo = new JComboBox<>(new String[]{"Local MySQL", "Docker MySQL", "Remote MySQL"});
        dbTypeCombo.addActionListener(e -> updateFields());
        mainPanel.add(dbTypeCombo, gbc);

        // Host
        gbc.gridx = 0; gbc.gridy = 2;
        mainPanel.add(new JLabel("Host:"), gbc);
        gbc.gridx = 1;
        hostField = new JTextField("localhost", 20);
        mainPanel.add(hostField, gbc);

        // Port
        gbc.gridx = 0; gbc.gridy = 3;
        mainPanel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1;
        portField = new JTextField("3306", 20);
        mainPanel.add(portField, gbc);

        // Database Name
        gbc.gridx = 0; gbc.gridy = 4;
        mainPanel.add(new JLabel("Database:"), gbc);
        gbc.gridx = 1;
        dbNameField = new JTextField("cashier_system", 20);
        mainPanel.add(dbNameField, gbc);

        // Username
        gbc.gridx = 0; gbc.gridy = 5;
        mainPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        userField = new JTextField("root", 20);
        mainPanel.add(userField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 6;
        mainPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passField = new JPasswordField("RootPassword123!", 20);
        mainPanel.add(passField, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        testButton = new JButton("Test Connection");
        saveButton = new JButton("Save & Start");

        testButton.addActionListener(e -> testConnection());
        saveButton.addActionListener(e -> saveAndStart());

        buttonPanel.add(testButton);
        buttonPanel.add(saveButton);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        mainPanel.add(buttonPanel, gbc);

        // Info label
        JLabel infoLabel = new JLabel("<html><center>Configure LiSuan database connection.<br>For Docker MySQL, make sure Docker is running.</center></html>");
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
                dbNameField.setText("cashier_system");
                userField.setText("root");
                passField.setText("");
                hostField.setEditable(false);
                portField.setEditable(false);
                break;
            case "Docker MySQL":
                hostField.setText("localhost");
                portField.setText("3306");
                dbNameField.setText("cashier_system");
                userField.setText("root");
                passField.setText("RootPassword123!");
                hostField.setEditable(false);
                portField.setEditable(false);
                break;
            case "Remote MySQL":
                hostField.setText("");
                portField.setText("3306");
                dbNameField.setText("cashier_system");
                userField.setText("");
                passField.setText("");
                hostField.setEditable(true);
                portField.setEditable(true);
                break;
        }
    }

    private void testConnection() {
        String type = (String) dbTypeCombo.getSelectedItem();
        String host = hostField.getText().trim();
        String port = portField.getText().trim();
        String dbName = dbNameField.getText().trim();
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());

        if (host.isEmpty() || user.isEmpty()) {
            showMessage("Please fill in all required fields", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Try to load MySQL JDBC driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            showMessage("MySQL JDBC driver not found!\n\nMake sure you're running from the complete distribution package.", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Test actual database connection
        java.sql.Connection conn = null;
        java.sql.Statement stmt = null;
        try {
            String dbUrl = String.format("jdbc:mysql://%s:%s/?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
                    host, port);
            conn = java.sql.DriverManager.getConnection(dbUrl, user, pass);

            // Check if database exists
            stmt = conn.createStatement();
            java.sql.ResultSet rs = stmt.executeQuery("SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + dbName + "'");

            StringBuilder message = new StringBuilder();
            message.append("Database connection successful!\n\n");

            if (rs.next()) {
                message.append("Database '").append(dbName).append("' exists.");
            } else {
                message.append("Database '").append(dbName).append("' does not exist yet.\n");
                message.append("It will be created automatically when you save the configuration.");
            }
            rs.close();

            showMessage(message.toString(), JOptionPane.INFORMATION_MESSAGE);

        } catch (java.sql.SQLException e) {
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
            try { if (stmt != null) stmt.close(); } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
            try { if (conn != null) conn.close(); } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
        }
    }

    private void saveAndStart() {
        String type = (String) dbTypeCombo.getSelectedItem();
        String host = hostField.getText().trim();
        String port = portField.getText().trim();
        String dbName = dbNameField.getText().trim();
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());

        if (host.isEmpty() || port.isEmpty() || user.isEmpty()) {
            showMessage("Please fill in all required fields", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // First test the connection
        java.sql.Connection conn = null;
        java.sql.Statement stmt = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Connect without specifying database
            String dbUrl = String.format("jdbc:mysql://%s:%s/?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
                    host, port);
            conn = java.sql.DriverManager.getConnection(dbUrl, user, pass);
            stmt = conn.createStatement();

            // Check if database exists
            java.sql.ResultSet rs = stmt.executeQuery("SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + dbName + "'");
            boolean dbExists = rs.next();
            rs.close();

            // Create database if it doesn't exist
            if (!dbExists) {
                stmt.execute("CREATE DATABASE " + dbName + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            }

        } catch (ClassNotFoundException e) {
            showMessage("MySQL JDBC driver not found!", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (java.sql.SQLException e) {
            String errorMsg = "Database connection failed!\n\n";
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
            return;
        } finally {
            try { if (stmt != null) stmt.close(); } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
            try { if (conn != null) conn.close(); } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
        }

        // Create config directory
        Path configDir = Paths.get("config");
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            // Write database.properties
            String dbUrl = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4",
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
                dbUrl, user, pass);

            Files.write(configDir.resolve("database.properties"), config.getBytes(StandardCharsets.UTF_8));

            showMessage("Configuration saved successfully!\n\nDatabase '" + dbName + "' is ready.\n\nYou can now run the application.", JOptionPane.INFORMATION_MESSAGE);

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

        } catch (IOException e) {
            showMessage("Failed to save configuration: " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startApplication() {
        try {
            // Find the JAR file
            File jarFile = null;
            File currentDir = new File(".");
            File[] files = currentDir.listFiles((dir, name) -> name.endsWith("-jar-with-dependencies.jar"));

            if (files != null && files.length > 0) {
                jarFile = files[0];

                // Build JavaFX module path
                String m2Repo = System.getProperty("user.home") + "\\.m2\\repository\\org\\openjfx";
                String modulePath = String.format(
                    "%s\\javafx-base\\17.0.12;%s\\javafx-controls\\17.0.12;%s\\javafx-fxml\\17.0.12;%s\\javafx-graphics\\17.0.12",
                    m2Repo, m2Repo, m2Repo, m2Repo);

                // Build command
                ProcessBuilder pb = new ProcessBuilder(
                    "javaw",
                    "-Xms512m",
                    "-Xmx1024m",
                    "-Dfile.encoding=UTF-8",
                    "--module-path", modulePath,
                    "--add-modules", "javafx.controls,javafx.fxml",
                    "-jar", jarFile.getName()
                );
                pb.directory(new File("."));
                pb.start();

                showMessage("Application started!", JOptionPane.INFORMATION_MESSAGE);
            } else {
                showMessage("Could not find application JAR file.", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            showMessage("Failed to start application: " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showMessage(String message, int messageType) {
        JOptionPane.showMessageDialog(frame, message, "LiSuan", messageType);
    }
}
