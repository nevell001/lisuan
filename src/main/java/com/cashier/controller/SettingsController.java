package com.cashier.controller;

import com.cashier.service.DataService;
import com.cashier.i18n.I18nManager;
import com.cashier.util.FormValidator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 系统设置控制器
 * 处理系统配置和设置
 */
public class SettingsController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(SettingsController.class);

    private com.cashier.model.User currentUser;

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
    private ComboBox<String> languageComboBox;

    @FXML
    private ComboBox<String> currencyComboBox;

    @FXML
    private ComboBox<String> themeComboBox;

    @FXML
    private ComboBox<String> fontSizeComboBox;

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
    private TextField logoPathField;

    @FXML
    private ImageView logoPreviewImage;

    @FXML
    private Label logoPreviewPlaceholder;

    @FXML
    private Label logoInfoLabel;

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

    // 数据导入标签页
    @FXML
    private ProgressBar importProgressBar;

    @FXML
    private Label importStatusLabel;

    @FXML
    private TextField csvFilePathField;

    @FXML
    private Button importFromCsvButton;

    @FXML
    private CheckBox skipDuplicatesCheckBox;

    @FXML
    private Label totalProcessedLabel;

    @FXML
    private Label successCountLabel;

    @FXML
    private Label skippedCountLabel;

    @FXML
    private Label errorCountLabel;

    @FXML
    private VBox importMessagesArea;

    private com.cashier.util.ProductDataImporter dataImporter;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        logger.info("SettingsController: 初始化系统设置...");

        // 初始化语言下拉框
        languageComboBox.setItems(javafx.collections.FXCollections.observableArrayList(
            "简体中文",
            "繁體中文",
            "English",
            "日本語",
            "한국어"
        ));
        languageComboBox.getSelectionModel().select(0);

        // 初始化货币下拉框
        currencyComboBox.setItems(javafx.collections.FXCollections.observableArrayList(
            "¥ 人民币 (CNY)",
            "$ 美元 (USD)",
            "¥ 日元 (JPY)",
            "₩ 韩元 (KRW)",
            "€ 欧元 (EUR)"
        ));
        currencyComboBox.getSelectionModel().select(0);

        // 初始化主题下拉框
        themeComboBox.setItems(javafx.collections.FXCollections.observableArrayList(
            "浅色主题",
            "深色主题",
            "IntelliJ主题"
        ));
        themeComboBox.getSelectionModel().select(0);

        // 初始化字号下拉框
        I18nManager i18n = I18nManager.getInstance();
        fontSizeComboBox.setItems(javafx.collections.FXCollections.observableArrayList(
            i18n.get("settings.font_size_small"),
            i18n.get("settings.font_size_medium"),
            i18n.get("settings.font_size_large"),
            i18n.get("settings.font_size_extra_large")
        ));
        fontSizeComboBox.getSelectionModel().select(1); // 默认选中中等

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

        // 初始化数据导入工具
        dataImporter = new com.cashier.util.ProductDataImporter();

        // 加载设置
        loadSettings();

        logger.info("SettingsController: 系统设置初始化完成");
    }

    /**
     * 设置当前用户
     * @param user 当前用户
     */
    public void setCurrentUser(com.cashier.model.User user) {
        this.currentUser = user;
        // 用户设置后重新加载设置
        if (user != null) {
            loadSettings();
        }
    }

    /**
     * 加载设置
     */
    private void loadSettings() {
        logger.info("SettingsController: 开始加载设置...");

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

        // 加载 Logo 路径
        String logoPath = settings.getOrDefault("logoPath", "");
        logoPathField.setText(logoPath);
        if (!logoPath.isEmpty()) {
            File logoFile = new File(logoPath);
            if (logoFile.exists()) {
                loadLogoPreview(logoFile);
            }
        }

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

        // 加载语言偏好 - 从数据库加载当前用户的语言偏好
        String username = (currentUser != null) ? currentUser.username : "default";
        String savedLanguage = DataService.loadLanguagePreference(username);
        String savedLanguageName = convertLanguageTagToName(savedLanguage);
        languageComboBox.getSelectionModel().select(savedLanguageName);

        // 加载字号偏好 - 从数据库加载当前用户的字号偏好
        String savedFontSize = DataService.loadFontSizePreference(username);
        String savedFontSizeName = convertFontSizeCodeToName(savedFontSize);
        fontSizeComboBox.getSelectionModel().select(savedFontSizeName);

        // 记录初始加载时的语言，用于检测变化
        initialLanguage = savedLanguage;

        // 加载货币偏好 - 从数据库加载当前用户的货币偏好
        try {
            String savedCurrency = com.cashier.dao.LanguagePreferenceDAO.getCurrencyPreference(username);
            String savedCurrencyName = convertCurrencyCodeToName(savedCurrency);
            currencyComboBox.getSelectionModel().select(savedCurrencyName);
            initialCurrency = savedCurrency;
        } catch (Exception e) {
            logger.warn("加载货币偏好失败: {}", e.getMessage());
            currencyComboBox.getSelectionModel().select(0); // 默认人民币
            initialCurrency = "CNY";
        }

        // 初始化 I18nManager 的语言
        applyLanguageSetting(savedLanguage);

        logger.info("SettingsController: 设置加载完成，当前主题: {}, 当前语言: {}, 当前货币: {}, 用户: {}",
                savedThemeCode, savedLanguage, initialCurrency, username);
    }

    /** 初始加载时的语言（用于检测变化） */
    private String initialLanguage = null;

    /** 初始加载时的货币（用于检测变化） */
    private String initialCurrency = null;

    /**
     * 处理保存基本设置
     */
    @FXML
    public void handleSaveBasicSettings() {
        if (validateBasicSettings()) {
            // 检查语言是否变化（与初始加载时的语言对比）
            String selectedLanguage = languageComboBox.getSelectionModel().getSelectedItem();
            String newLanguageTag = selectedLanguage != null ? convertLanguageNameToTag(selectedLanguage) : "zh-CN";
            boolean languageChanged = !newLanguageTag.equals(initialLanguage);

            // 检查货币是否变化
            String selectedCurrency = currencyComboBox.getSelectionModel().getSelectedItem();
            String newCurrencyCode = selectedCurrency != null ? convertCurrencyNameToCode(selectedCurrency) : "CNY";
            boolean currencyChanged = !newCurrencyCode.equals(initialCurrency);

            saveSettings();

            // 应用主题设置
            String selectedTheme = themeComboBox.getSelectionModel().getSelectedItem();
            if (selectedTheme != null) {
                String themeCode = convertThemeNameToCode(selectedTheme);
                applyThemeToCurrentScene(themeCode);
            }

            // 应用语言设置
            if (selectedLanguage != null) {
                applyLanguageSetting(newLanguageTag);
            }

            // 应用字号设置
            String selectedFontSize = fontSizeComboBox.getSelectionModel().getSelectedItem();
            if (selectedFontSize != null) {
                String fontSizeCode = convertFontSizeNameToCode(selectedFontSize);
                applyFontSizeToCurrentScene(fontSizeCode);
            }

            // 应用货币设置
            if (selectedCurrency != null && currencyChanged) {
                try {
                    String username = (currentUser != null) ? currentUser.username : "default";
                    com.cashier.dao.LanguagePreferenceDAO.setCurrencyPreference(username, newCurrencyCode);
                    com.cashier.util.CurrencyUtil.setCurrency(newCurrencyCode);
                    logger.info("货币已更新为: {}", newCurrencyCode);
                } catch (Exception e) {
                    logger.error("保存货币偏好失败: {}", e.getMessage(), e);
                }
            }

            if (languageChanged) {
                // 语言已更改，提示用户重启
                showLanguageRestartDialog();
            } else {
                showSuccess("基本设置保存成功！");
            }
        }
    }

    /**
     * 显示语言重启对话框
     */
    private void showLanguageRestartDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        I18nManager i18n = I18nManager.getInstance();
        alert.setTitle(i18n.get("settings.language") + " " + i18n.get("message.save.success").split(" / ")[0]);
        alert.setHeaderText(null);
        alert.setContentText(i18n.get("message.restart.required"));

        ButtonType restartButton = new ButtonType(i18n.get("message.restart.now"), ButtonBar.ButtonData.OK_DONE);
        ButtonType laterButton = new ButtonType(i18n.get("message.restart.later"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(restartButton, laterButton);

        logger.info("显示语言重启对话框，等待用户选择...");

        Optional<ButtonType> result = alert.showAndWait();
        logger.info("用户选择: {}", result.map(bt -> bt.getText()).orElse("无"));

        result.ifPresent(buttonType -> {
            if (buttonType.equals(restartButton)) {
                // 重启应用
                logger.info("用户选择立即重启，开始重启应用...");
                restartApplication();
            } else {
                logger.info("用户选择稍后重启");
            }
        });
    }

    /**
     * 重启应用程序
     */
    private void restartApplication() {
        try {
            logger.info("正在重启应用...");
            com.cashier.CashierSystemFXApplication app = com.cashier.CashierSystemFXApplication.getInstance();
            if (app == null) {
                logger.error("无法获取应用实例");
                showError("重启应用失败：无法获取应用实例");
                return;
            }
            logger.info("调用 logoutToLoginView 返回登录界面...");
            app.logoutToLoginView(); // 回到登录界面，语言已更改
            logger.info("重启应用完成");
        } catch (Exception e) {
            logger.error("重启应用失败", e);
            showError("重启应用失败: " + e.getMessage());
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
     * 将语言名称转换为语言标签
     * @param languageName 语言名称
     * @return 语言标签
     */
    private String convertLanguageNameToTag(String languageName) {
        if (languageName == null) {
            return "zh-CN";
        }
        switch (languageName) {
            case "简体中文":
                return "zh-CN";
            case "繁體中文":
                return "zh-TW";
            case "English":
                return "en";
            case "日本語":
                return "ja";
            case "한국어":
                return "ko";
            default:
                return "zh-CN";
        }
    }

    /**
     * 将语言标签转换为语言名称
     * @param languageTag 语言标签
     * @return 语言名称
     */
    private String convertLanguageTagToName(String languageTag) {
        if (languageTag == null) {
            return "简体中文";
        }
        switch (languageTag) {
            case "zh-CN":
                return "简体中文";
            case "zh-TW":
                return "繁體中文";
            case "en":
                return "English";
            case "ja":
                return "日本語";
            case "ko":
                return "한국어";
            default:
                return "简体中文";
        }
    }

    /**
     * 将字号代码转换为字号名称
     * @param fontSizeCode 字号代码
     * @return 字号名称
     */
    private String convertFontSizeCodeToName(String fontSizeCode) {
        if (fontSizeCode == null) {
            return I18nManager.getInstance().get("settings.font_size_medium");
        }
        I18nManager i18n = I18nManager.getInstance();
        switch (fontSizeCode) {
            case "small":
                return i18n.get("settings.font_size_small");
            case "medium":
                return i18n.get("settings.font_size_medium");
            case "large":
                return i18n.get("settings.font_size_large");
            case "extra-large":
                return i18n.get("settings.font_size_extra_large");
            default:
                return i18n.get("settings.font_size_medium");
        }
    }

    /**
     * 将字号名称转换为字号代码
     * @param fontSizeName 字号名称
     * @return 字号代码
     */
    private String convertFontSizeNameToCode(String fontSizeName) {
        if (fontSizeName == null) {
            return "medium";
        }
        I18nManager i18n = I18nManager.getInstance();
        String small = i18n.get("settings.font_size_small");
        String medium = i18n.get("settings.font_size_medium");
        String large = i18n.get("settings.font_size_large");
        String extraLarge = i18n.get("settings.font_size_extra_large");

        if (fontSizeName.equals(small)) {
            return "small";
        } else if (fontSizeName.equals(medium)) {
            return "medium";
        } else if (fontSizeName.equals(large)) {
            return "large";
        } else if (fontSizeName.equals(extraLarge)) {
            return "extra-large";
        }
        return "medium";
    }

    /**
     * 货币代码转显示名称
     */
    private String convertCurrencyCodeToName(String currencyCode) {
        if (currencyCode == null) {
            return "¥ 人民币 (CNY)";
        }
        switch (currencyCode) {
            case "CNY":
                return "¥ 人民币 (CNY)";
            case "USD":
                return "$ 美元 (USD)";
            case "JPY":
                return "¥ 日元 (JPY)";
            case "KRW":
                return "₩ 韩元 (KRW)";
            case "EUR":
                return "€ 欧元 (EUR)";
            default:
                return "¥ 人民币 (CNY)";
        }
    }

    /**
     * 货币显示名称转代码
     */
    private String convertCurrencyNameToCode(String currencyName) {
        if (currencyName == null) {
            return "CNY";
        }
        if (currencyName.contains("CNY") || currencyName.contains("人民币")) {
            return "CNY";
        } else if (currencyName.contains("USD") || currencyName.contains("美元")) {
            return "USD";
        } else if (currencyName.contains("JPY") || currencyName.contains("日元")) {
            return "JPY";
        } else if (currencyName.contains("KRW") || currencyName.contains("韩元")) {
            return "KRW";
        } else if (currencyName.contains("EUR") || currencyName.contains("欧元")) {
            return "EUR";
        }
        return "CNY";
    }

    /**
     * 应用语言设置
     * @param languageTag 语言标签
     */
    private void applyLanguageSetting(String languageTag) {
        try {
            com.cashier.i18n.I18nManager.getInstance().setLocale(languageTag);
            logger.info("语言已切换到: {}, I18nManager 当前语言标签: {}", languageTag, com.cashier.i18n.I18nManager.getInstance().getCurrentLanguageTag());
        } catch (Exception e) {
            logger.error("语言切换失败: {}", languageTag, e);
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
     * 应用字号到当前场景
     * @param fontSizeCode 字号代码
     */
    private void applyFontSizeToCurrentScene(String fontSizeCode) {
        if (fontSizeComboBox.getScene() != null) {
            javafx.application.Platform.runLater(() -> {
                com.cashier.CashierSystemFXApplication app = com.cashier.CashierSystemFXApplication.getInstance();
                if (app != null) {
                    app.applyFontSize(fontSizeComboBox.getScene(), fontSizeCode);
                }
            });
        }
    }

    /**
     * 处理保存打印设置
     */
    @FXML
    public void handleSavePrintSettings() {
        saveSettings();
        showSuccess("打印设置保存成功！");
    }

    /**
     * 处理保存备份设置
     */
    @FXML
    public void handleSaveBackupSettings() {
        saveSettings();
        showSuccess("备份设置保存成功！");
    }

    /**
     * 处理保存安全设置
     */
    @FXML
    public void handleSaveSecuritySettings() {
        saveSettings();
        showSuccess("安全设置保存成功！");
    }

    /**
     * 处理浏览备份路径
     */
    @FXML
    public void handleBrowseBackupPath() {
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
     * 处理选择 Logo 图片
     */
    @FXML
    public void handleSelectLogo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择 Logo 图片");

        // 设置文件过滤器
        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
            "图片文件 (*.png, *.jpg, *.jpeg, *.gif, *.bmp)",
            "*.png", "*.PNG", "*.jpg", "*.JPG", "*.jpeg", "*.JPEG", "*.gif", "*.GIF", "*.bmp", "*.BMP"
        );
        fileChooser.getExtensionFilters().add(imageFilter);

        // 设置初始目录
        String currentPath = logoPathField.getText().trim();
        if (!currentPath.isEmpty()) {
            File currentFile = new File(currentPath);
            if (currentFile.exists() && currentFile.getParentFile() != null) {
                fileChooser.setInitialDirectory(currentFile.getParentFile());
            }
        }

        File selectedFile = fileChooser.showOpenDialog(logoPathField.getScene().getWindow());
        if (selectedFile != null) {
            // 复制 Logo 到项目目录
            copyLogoToProject(selectedFile);
        }
    }

    /**
     * 处理清除 Logo
     */
    @FXML
    public void handleClearLogo() {
        logoPathField.clear();
        logoPreviewImage.setImage(null);
        logoPreviewPlaceholder.setVisible(true);
        logoInfoLabel.setText("建议尺寸: 200x200 像素");
        printLogoCheckBox.setSelected(false);
    }

    /**
     * 复制 Logo 到项目目录
     */
    private void copyLogoToProject(File sourceFile) {
        try {
            // 创建 logo 目录（如果不存在）
            File logoDir = new File("logos");
            if (!logoDir.exists()) {
                logoDir.mkdirs();
            }

            // 目标文件路径
            String extension = getFileExtension(sourceFile.getName());
            String targetFileName = "store_logo" + extension;
            File targetFile = new File(logoDir, targetFileName);

            // 复制文件
            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 更新 UI
            String relativePath = "logos/" + targetFileName;
            logoPathField.setText(relativePath);
            loadLogoPreview(targetFile);

            showSuccess("Logo 已添加到项目");

        } catch (Exception e) {
            logger.error("复制 Logo 文件失败", e);
            showError("添加 Logo 失败: " + e.getMessage());
        }
    }

    /**
     * 加载 Logo 预览
     */
    private void loadLogoPreview(File logoFile) {
        if (logoFile != null && logoFile.exists()) {
            try {
                Image logoImage = new Image(logoFile.toURI().toString());
                logoPreviewImage.setImage(logoImage);
                logoPreviewPlaceholder.setVisible(false);

                // 更新信息标签
                int width = (int) logoImage.getWidth();
                int height = (int) logoImage.getHeight();
                logoInfoLabel.setText(String.format("当前尺寸: %dx%d 像素", width, height));

            } catch (Exception e) {
                logger.error("加载 Logo 预览失败", e);
                logoPreviewPlaceholder.setVisible(true);
            }
        } else {
            logoPreviewImage.setImage(null);
            logoPreviewPlaceholder.setVisible(true);
            logoInfoLabel.setText("建议尺寸: 200x200 像素");
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex);
        }
        return ".png"; // 默认扩展名
    }

    /**
     * 处理立即备份
     */
    @FXML
    public void handleBackupNow() {
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
            File[] sqlFiles = backupDir.listFiles((dir, name) -> name.startsWith("lisuan_system_") && name.endsWith(".sql"));
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
    public void handleRestore() {
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
        
        File[] sqlFiles = backupDir.listFiles((dir, name) -> name.startsWith("lisuan_system_") && name.endsWith(".sql"));
        
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
    public void handleResetAll() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18nManager.getInstance().get("common.confirm"));
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
            double taxRate = FormValidator.parseDouble(taxRateField.getText().trim());
            if (taxRate < 0 || taxRate > 1) {
                errorMessage += "税率必须在0到1之间！\n";
            }
        } catch (IllegalArgumentException e) {
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
        String selectedLanguage = languageComboBox.getSelectionModel().getSelectedItem();
        settings.put("language", selectedLanguage != null ? selectedLanguage : "简体中文");
        String selectedTheme = themeComboBox.getSelectionModel().getSelectedItem();
        settings.put("theme", selectedTheme != null ? selectedTheme : "浅色主题");
        String selectedCurrency = currencyComboBox.getSelectionModel().getSelectedItem();
        settings.put("currency", selectedCurrency != null ? selectedCurrency : "¥ 人民币 (CNY)");

        // 打印设置
        settings.put("enablePrint", String.valueOf(enablePrintCheckBox.isSelected()));
        settings.put("printerName", printerNameField.getText().trim());
        String selectedPaperSize = paperSizeComboBox.getSelectionModel().getSelectedItem();
        settings.put("paperSize", selectedPaperSize != null ? selectedPaperSize : "58mm (热敏纸)");
        settings.put("printLogo", String.valueOf(printLogoCheckBox.isSelected()));
        settings.put("logoPath", logoPathField.getText().trim());
        settings.put("printBarcode", String.valueOf(printBarcodeCheckBox.isSelected()));

        // 备份设置
        settings.put("autoBackup", String.valueOf(autoBackupCheckBox.isSelected()));
        String selectedBackupFreq = backupFrequencyComboBox.getSelectionModel().getSelectedItem();
        settings.put("backupFrequency", selectedBackupFreq != null ? selectedBackupFreq : "每天");
        settings.put("backupPath", backupPathField.getText().trim());

        // 安全设置
        settings.put("autoLogout", String.valueOf(autoLogoutCheckBox.isSelected()));
        settings.put("autoLogoutMinutes", String.valueOf(autoLogoutMinutesSpinner.getValue()));
        settings.put("passwordComplexity", String.valueOf(passwordComplexityCheckBox.isSelected()));
        settings.put("passwordMinLength", String.valueOf(passwordMinLengthSpinner.getValue()));
        settings.put("passwordMaxAttempts", String.valueOf(passwordMaxAttemptsSpinner.getValue()));

        // 保存所有设置到数据库
        DataService.saveSettings(settings);

        // 保存主题偏好（单独存储到主题偏好表）
        String themeName = settings.getOrDefault("theme", "浅色主题");
        String themeCode = convertThemeNameToCode(themeName);
        DataService.saveThemePreference(themeCode);

        // 保存语言偏好 - 保存到当前用户
        String languageName = settings.getOrDefault("language", "简体中文");
        String languageTag = convertLanguageNameToTag(languageName);
        String username = (currentUser != null) ? currentUser.username : "default";
        DataService.saveLanguagePreference(username, languageTag);

        // 保存字号偏好 - 保存到当前用户
        String fontSizeName = fontSizeComboBox.getSelectionModel().getSelectedItem();
        String fontSizeCode = convertFontSizeNameToCode(fontSizeName);
        DataService.saveFontSizePreference(username, fontSizeCode);

        logger.info("SettingsController: 设置保存成功，主题: {}, 语言: {}, 字号: {}, 用户: {}", themeCode, languageTag, fontSizeCode, username);
    }
    /**
     * 显示成功消息
     * @param message 消息内容
     */
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18nManager.getInstance().get("label.success"));
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
        alert.setTitle(I18nManager.getInstance().get("label.error"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 打开探数API网站
     */
    @FXML
    public void handleOpenTanshuApi() {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("https://www.tanshuapi.com/market/detail-77"));
        } catch (Exception e) {
            logger.error("打开网页失败", e);
            showError("打开网页失败: " + e.getMessage());
        }
    }

    /**
     * 打开聚合数据网站
     */
    @FXML
    public void handleOpenJuheApi() {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("https://www.juhe.cn/docs/api/id/489"));
        } catch (Exception e) {
            logger.error("打开网页失败", e);
            showError("打开网页失败: " + e.getMessage());
        }
    }

    /**
     * 打开天聚数据网站
     */
    @FXML
    public void handleOpenTianapiApi() {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("https://www.tianapi.com/apiview/138"));
        } catch (Exception e) {
            logger.error("打开网页失败", e);
            showError("打开网页失败: " + e.getMessage());
        }
    }

    /**
     * 浏览 CSV 文件
     */
    @FXML
    public void handleBrowseCsvFile() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("选择 CSV 文件");
        fileChooser.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter("CSV 文件", "*.csv")
        );
        fileChooser.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter("所有文件", "*.*")
        );
        
        File selectedFile = fileChooser.showOpenDialog(csvFilePathField.getScene().getWindow());
        if (selectedFile != null) {
            csvFilePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    /**
     * 从 CSV 文件导入数据
     */
    @FXML
    public void handleImportFromCSV() {
        String filePath = csvFilePathField.getText().trim();
        
        if (filePath.isEmpty()) {
            showError("请选择 CSV 文件");
            return;
        }

        if (dataImporter == null) {
            dataImporter = new com.cashier.util.ProductDataImporter();
        }

        // 重置统计
        dataImporter.resetStatistics();
        updateImportStatistics();
        clearImportMessages();

        // 显示进度条
        importProgressBar.setVisible(true);
        importProgressBar.setProgress(0);
        importStatusLabel.setText("正在导入 CSV 文件...");

        // 异步导入
        new Thread(() -> {
            try {
                Map<String, Object> result = dataImporter.importFromCSV(filePath);
                
                javafx.application.Platform.runLater(() -> {
                    updateImportStatistics();
                    
                    @SuppressWarnings("unchecked")
                    List<String> messages = (List<String>) result.get("messages");
                    
                    if (messages != null) {
                        for (String message : messages) {
                            addImportMessage(message);
                        }
                    }
                    
                    if ((Boolean) result.get("success")) {
                        importProgressBar.setProgress(1);
                        importStatusLabel.setText("导入完成！");
                        showSuccess("CSV 文件导入完成！");
                    } else {
                        importProgressBar.setProgress(1);
                        importStatusLabel.setText("导入失败");
                        showError("CSV 文件导入失败: " + result.get("error"));
                    }
                    
                    // 延迟隐藏进度条
                    javafx.application.Platform.runLater(() -> {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        importProgressBar.setVisible(false);
                    });
                });
            } catch (Exception e) {
                logger.error("从 CSV 导入数据失败", e);
                javafx.application.Platform.runLater(() -> {
                    importProgressBar.setVisible(false);
                    importStatusLabel.setText("导入失败");
                    showError("从 CSV 导入数据失败: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 更新导入统计
     */
    private void updateImportStatistics() {
        if (dataImporter == null) return;
        
        Map<String, Integer> stats = dataImporter.getStatistics();
        totalProcessedLabel.setText(String.valueOf(stats.get("totalProcessed")));
        successCountLabel.setText(String.valueOf(stats.get("successCount")));
        skippedCountLabel.setText(String.valueOf(stats.get("skippedCount")));
        errorCountLabel.setText(String.valueOf(stats.get("errorCount")));
    }

    /**
     * 添加导入消息
     */
    private void addImportMessage(String message) {
        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-font-size: 12px; -fx-wrap-text: true;");
        importMessagesArea.getChildren().add(messageLabel);
    }

    /**
     * 清除导入消息
     */
    private void clearImportMessages() {
        importMessagesArea.getChildren().clear();
        Label logLabel = new Label("导入日志:");
        logLabel.setStyle("-fx-font-weight: bold;");
        importMessagesArea.getChildren().add(logLabel);
    }
}