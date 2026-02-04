package com.cashier.controller;

import com.cashier.service.DataService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * 系统设置控制器
 * 处理系统配置和设置
 */
public class SettingsController {

    @FXML
    private TabPane settingsTabPane;

    // 基本设置标签页
    @FXML
    private TextField storeNameField;

    @FXML
    private TextField storeAddressField;

    @FXML
    private TextField storePhoneField;

    @FXML
    private TextField taxRateField;

    @FXML
    private ComboBox<String> currencyComboBox;

    @FXML
    private ComboBox<String> languageComboBox;

    @FXML
    private ComboBox<String> themeComboBox;

    // 打印设置标签页
    @FXML
    private CheckBox enablePrintCheckBox;

    @FXML
    private TextField printerNameField;

    @FXML
    private ComboBox<String> paperSizeComboBox;

    @FXML
    private CheckBox printLogoCheckBox;

    @FXML
    private CheckBox printBarcodeCheckBox;

    // 备份设置标签页
    @FXML
    private CheckBox autoBackupCheckBox;

    @FXML
    private ComboBox<String> backupFrequencyComboBox;

    @FXML
    private TextField backupPathField;

    @FXML
    private Button backupNowButton;

    @FXML
    private Button restoreButton;

    // 安全设置标签页
    @FXML
    private CheckBox autoLogoutCheckBox;

    @FXML
    private Spinner<Integer> autoLogoutMinutesSpinner;

    @FXML
    private CheckBox passwordComplexityCheckBox;

    @FXML
    private Spinner<Integer> passwordMinLengthSpinner;

    @FXML
    private Spinner<Integer> passwordMaxAttemptsSpinner;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        System.out.println("SettingsController: 初始化系统设置...");
        
        // 初始化货币下拉框
        currencyComboBox.setItems(javafx.collections.FXCollections.observableArrayList(
            "CNY (人民币)",
            "USD (美元)",
            "EUR (欧元)"
        ));
        currencyComboBox.getSelectionModel().select(0);

        // 初始化语言下拉框
        languageComboBox.setItems(javafx.collections.FXCollections.observableArrayList(
            "简体中文",
            "English",
            "繁體中文"
        ));
        languageComboBox.getSelectionModel().select(0);

        // 初始化主题下拉框
        themeComboBox.setItems(javafx.collections.FXCollections.observableArrayList(
            "浅色主题",
            "深色主题",
            "IntelliJ主题"
        ));
        themeComboBox.getSelectionModel().select(0);

        // 初始化纸张大小下拉框
        paperSizeComboBox.setItems(javafx.collections.FXCollections.observableArrayList(
            "58mm (热敏纸)",
            "80mm (热敏纸)",
            "A4"
        ));
        paperSizeComboBox.getSelectionModel().select(0);

        // 初始化备份频率下拉框
        backupFrequencyComboBox.setItems(javafx.collections.FXCollections.observableArrayList(
            "每天",
            "每周",
            "每月"
        ));
        backupFrequencyComboBox.getSelectionModel().select(0);

        // 初始化自动登出时间
        SpinnerValueFactory<Integer> logoutMinutesFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 120, 30);
        autoLogoutMinutesSpinner.setValueFactory(logoutMinutesFactory);

        // 初始化密码最小长度
        SpinnerValueFactory<Integer> passwordMinLengthFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(4, 20, 6);
        passwordMinLengthSpinner.setValueFactory(passwordMinLengthFactory);

        // 初始化密码最大尝试次数
        SpinnerValueFactory<Integer> passwordMaxAttemptsFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(3, 10, 5);
        passwordMaxAttemptsSpinner.setValueFactory(passwordMaxAttemptsFactory);

        // 加载设置
        loadSettings();
        
        System.out.println("SettingsController: 系统设置初始化完成");
    }

    /**
     * 加载设置
     */
    private void loadSettings() {
        System.out.println("SettingsController: 开始加载设置...");
        
        Map<String, String> settings = DataService.loadSettings();
        
        // 加载基本设置
        storeNameField.setText(settings.getOrDefault("storeName", ""));
        storeAddressField.setText(settings.getOrDefault("storeAddress", ""));
        storePhoneField.setText(settings.getOrDefault("storePhone", ""));
        taxRateField.setText(settings.getOrDefault("taxRate", "0.0"));
        
        // 加载打印设置
        enablePrintCheckBox.setSelected(Boolean.parseBoolean(settings.getOrDefault("enablePrint", "false")));
        printerNameField.setText(settings.getOrDefault("printerName", ""));
        printLogoCheckBox.setSelected(Boolean.parseBoolean(settings.getOrDefault("printLogo", "true")));
        printBarcodeCheckBox.setSelected(Boolean.parseBoolean(settings.getOrDefault("printBarcode", "true")));
        
        // 加载备份设置
        autoBackupCheckBox.setSelected(Boolean.parseBoolean(settings.getOrDefault("autoBackup", "false")));
        backupPathField.setText(settings.getOrDefault("backupPath", ""));
        
        // 加载安全设置
        autoLogoutCheckBox.setSelected(Boolean.parseBoolean(settings.getOrDefault("autoLogout", "true")));
        passwordComplexityCheckBox.setSelected(Boolean.parseBoolean(settings.getOrDefault("passwordComplexity", "true")));
        
        // 加载主题偏好
        String savedThemeCode = DataService.loadThemePreference();
        String savedThemeName = convertThemeCodeToName(savedThemeCode);
        themeComboBox.getSelectionModel().select(savedThemeName);
        
        System.out.println("SettingsController: 设置加载完成，当前主题: " + savedThemeCode);
    }

    /**
     * 处理保存基本设置
     */
    @FXML
    private void handleSaveBasicSettings() {
        if (validateBasicSettings()) {
            saveSettings();
            
            // 应用主题设置
            String selectedTheme = themeComboBox.getSelectionModel().getSelectedItem();
            if (selectedTheme != null) {
                String themeCode = convertThemeNameToCode(selectedTheme);
                applyThemeToCurrentScene(themeCode);
            }
            
            showSuccess("基本设置保存成功！");
        }
    }

    /**
     * 将中文主题名称转换为英文主题代码
     * @param themeName 中文主题名称
     * @return 英文主题代码
     */
    private String convertThemeNameToCode(String themeName) {
        if (themeName == null) {
            return "light";
        }
        switch (themeName) {
            case "浅色主题":
                return "light";
            case "深色主题":
                return "dark";
            case "IntelliJ主题":
                return "intellij";
            default:
                return "light";
        }
    }

    /**
     * 将英文主题代码转换为中文主题名称
     * @param themeCode 英文主题代码
     * @return 中文主题名称
     */
    private String convertThemeCodeToName(String themeCode) {
        if (themeCode == null) {
            return "浅色主题";
        }
        switch (themeCode) {
            case "light":
                return "浅色主题";
            case "dark":
                return "深色主题";
            case "intellij":
                return "IntelliJ主题";
            default:
                return "浅色主题";
        }
    }

    /**
     * 应用主题到当前场景
     * @param themeCode 主题代码
     */
    private void applyThemeToCurrentScene(String themeCode) {
        if (themeComboBox.getScene() != null) {
            javafx.application.Platform.runLater(() -> {
                com.cashier.CashierSystemFXApplication app = com.cashier.CashierSystemFXApplication.getInstance();
                if (app != null) {
                    app.applyTheme(themeComboBox.getScene(), themeCode);
                }
            });
        }
    }

    /**
     * 处理保存打印设置
     */
    @FXML
    private void handleSavePrintSettings() {
        saveSettings();
        showSuccess("打印设置保存成功！");
    }

    /**
     * 处理保存备份设置
     */
    @FXML
    private void handleSaveBackupSettings() {
        saveSettings();
        showSuccess("备份设置保存成功！");
    }

    /**
     * 处理保存安全设置
     */
    @FXML
    private void handleSaveSecuritySettings() {
        saveSettings();
        showSuccess("安全设置保存成功！");
    }

    /**
     * 处理浏览备份路径
     */
    @FXML
    private void handleBrowseBackupPath() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("选择备份目录");
        
        // 设置初始目录
        String currentPath = backupPathField.getText().trim();
        if (!currentPath.isEmpty()) {
            File initialDir = new File(currentPath);
            if (initialDir.exists()) {
                directoryChooser.setInitialDirectory(initialDir);
            }
        }
        
        File selectedDirectory = directoryChooser.showDialog(backupPathField.getScene().getWindow());
        if (selectedDirectory != null) {
            backupPathField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    /**
     * 处理立即备份
     */
    @FXML
    private void handleBackupNow() {
        try {
            // 获取用户选择的备份路径，如果为空则使用项目根目录
            String backupBasePath = backupPathField.getText().trim();
            if (backupBasePath.isEmpty()) {
                backupBasePath = System.getProperty("user.dir");
            }
            
            // 确保备份路径存在
            File backupDir = new File(backupBasePath);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            // 备份数据库（会在备份目录中创建带时间戳的 .sql 文件）
            DataService.backupData(backupBasePath);
            
            // 获取最新的备份文件名
            File[] sqlFiles = backupDir.listFiles((dir, name) -> name.startsWith("cashier_system_") && name.endsWith(".sql"));
            if (sqlFiles != null && sqlFiles.length > 0) {
                java.util.Arrays.sort(sqlFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                showSuccess("数据备份成功！\n备份文件: " + sqlFiles[0].getName());
            } else {
                showSuccess("数据备份成功！");
            }
        } catch (Exception e) {
            showError("数据备份失败: " + e.getMessage());
        }
    }

    /**
     * 处理恢复数据
     */
    @FXML
    private void handleRestore() {
        // 获取用户选择的备份路径，如果为空则使用项目根目录
        final String backupBasePath;
        String path = backupPathField.getText().trim();
        if (path.isEmpty()) {
            backupBasePath = System.getProperty("user.dir");
        } else {
            backupBasePath = path;
        }
        
        // 列出可用的备份文件
        File backupDir = new File(backupBasePath);
        if (!backupDir.exists()) {
            showError("备份路径不存在！\n路径: " + backupBasePath);
            return;
        }
        
        File[] sqlFiles = backupDir.listFiles((dir, name) -> name.startsWith("cashier_system_") && name.endsWith(".sql"));
        
        if (sqlFiles == null || sqlFiles.length == 0) {
            showError("未找到任何备份文件！\n路径: " + backupBasePath + "\n请先进行数据备份。");
            return;
        }
        
        // 按修改时间排序，最新的在前
        java.util.Arrays.sort(sqlFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        
        // 创建选择对话框
        ChoiceDialog<String> dialog = new ChoiceDialog<>();
        dialog.setTitle("选择备份");
        dialog.setHeaderText("请选择要恢复的备份：");
        dialog.setContentText("可用备份：");
        
        // 添加备份选项
        ObservableList<String> options = FXCollections.observableArrayList();
        for (File file : sqlFiles) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timeStr = sdf.format(new Date(file.lastModified()));
            options.add(file.getName() + " (" + timeStr + ")");
        }
        dialog.getItems().addAll(options);
        
        dialog.showAndWait().ifPresent(selected -> {
            // 提取备份文件名
            String backupFileName = selected.split(" \\(")[0];
            File backupFile = new File(backupBasePath, backupFileName);
            
            try {
                // 确认恢复
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("确认恢复");
                confirmAlert.setHeaderText(null);
                confirmAlert.setContentText("确定要从以下备份恢复数据吗？\n备份文件: " + backupFileName + "\n\n恢复数据将覆盖当前数据，确定要继续吗？");
                
                if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    DataService.restoreData(backupFile.getAbsolutePath());
                    showSuccess("数据恢复成功！\n请重新登录以加载最新数据。");
                    
                    // 重新加载数据
                    loadSettings();
                }
            } catch (Exception e) {
                showError("数据恢复失败: " + e.getMessage());
            }
        });
    }

    /**
     * 处理重置所有设置
     */
    @FXML
    private void handleResetAll() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认重置");
        alert.setHeaderText(null);
        alert.setContentText("确定要重置所有设置为默认值吗？");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            // 清空所有字段
            storeNameField.clear();
            storeAddressField.clear();
            storePhoneField.clear();
            taxRateField.setText("0.0");
            printerNameField.clear();
            backupPathField.clear();
            
            // 重置为默认值
            enablePrintCheckBox.setSelected(false);
            autoBackupCheckBox.setSelected(false);
            autoLogoutCheckBox.setSelected(true);
            passwordComplexityCheckBox.setSelected(true);
            
            showSuccess("所有设置已重置为默认值！");
        }
    }

    /**
     * 验证基本设置
     * @return 如果验证通过返回true，否则返回false
     */
    private boolean validateBasicSettings() {
        String errorMessage = "";

        // 验证税率
        try {
            double taxRate = Double.parseDouble(taxRateField.getText().trim());
            if (taxRate < 0 || taxRate > 1) {
                errorMessage += "税率必须在0到1之间！\n";
            }
        } catch (NumberFormatException e) {
            errorMessage += "税率格式不正确！\n";
        }

        if (!errorMessage.isEmpty()) {
            showError(errorMessage);
            return false;
        }

        return true;
    }

    /**
     * 保存设置
     */
    private void saveSettings() {
        Map<String, String> settings = new java.util.HashMap<>();
        
        // 基本设置
        settings.put("storeName", storeNameField.getText().trim());
        settings.put("storeAddress", storeAddressField.getText().trim());
        settings.put("storePhone", storePhoneField.getText().trim());
        settings.put("taxRate", taxRateField.getText().trim());
        settings.put("currency", currencyComboBox.getSelectionModel().getSelectedItem());
        settings.put("language", languageComboBox.getSelectionModel().getSelectedItem());
        settings.put("theme", themeComboBox.getSelectionModel().getSelectedItem());
        
        // 打印设置
        settings.put("enablePrint", String.valueOf(enablePrintCheckBox.isSelected()));
        settings.put("printerName", printerNameField.getText().trim());
        settings.put("paperSize", paperSizeComboBox.getSelectionModel().getSelectedItem());
        settings.put("printLogo", String.valueOf(printLogoCheckBox.isSelected()));
        settings.put("printBarcode", String.valueOf(printBarcodeCheckBox.isSelected()));
        
        // 备份设置
        settings.put("autoBackup", String.valueOf(autoBackupCheckBox.isSelected()));
        settings.put("backupFrequency", backupFrequencyComboBox.getSelectionModel().getSelectedItem());
        settings.put("backupPath", backupPathField.getText().trim());
        
        // 安全设置
        settings.put("autoLogout", String.valueOf(autoLogoutCheckBox.isSelected()));
        settings.put("autoLogoutMinutes", String.valueOf(autoLogoutMinutesSpinner.getValue()));
        settings.put("passwordComplexity", String.valueOf(passwordComplexityCheckBox.isSelected()));
        settings.put("passwordMinLength", String.valueOf(passwordMinLengthSpinner.getValue()));
        settings.put("passwordMaxAttempts", String.valueOf(passwordMaxAttemptsSpinner.getValue()));
        
        // 保存到文件
        DataService.saveSettings(
            Double.parseDouble(settings.getOrDefault("taxRate", "0.0")),
            0
        );
        
        // 保存主题偏好
        String themeName = settings.getOrDefault("theme", "浅色主题");
        String themeCode = convertThemeNameToCode(themeName);
        DataService.saveThemePreference(themeCode);
        
        System.out.println("SettingsController: 设置保存成功，主题: " + themeCode);
    }

    /**
     * 显示成功消息
     * @param message 消息内容
     */
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("成功");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示错误信息
     * @param message 错误消息
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
