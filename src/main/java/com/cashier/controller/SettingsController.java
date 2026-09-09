package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.constant.FXConstants;
import com.cashier.dao.DAOFactory;
import com.cashier.service.DataService;
import com.cashier.service.PaymentService;
import com.cashier.i18n.I18nManager;
import com.cashier.model.User;
import com.cashier.printer.EscPosUtils;
import com.cashier.printer.NetworkPrinterDevice;
import com.cashier.printer.PrintTask;
import com.cashier.printer.PrinterDevice;
import com.cashier.printer.PrinterManager;
import com.cashier.scanner.ScanValidation;
import com.cashier.scanner.USBHIDScannerDevice;
import com.cashier.util.PasswordUtil;
import com.cashier.util.FormValidator;
import com.cashier.util.DatabaseManager;
import com.cashier.util.DateTimeFormats;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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

    // 防止重复点击"立即备份"造成并发 dump
    private volatile boolean backupInProgress;

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
    private Button testPrintButton;

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

    @FXML
    private TextField scannerTestField;

    @FXML
    private Label scannerTestResultLabel;

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

    // 支付设置标签页
    @FXML
    private ComboBox<String> paymentModeComboBox;

    @FXML
    private CheckBox paymentMockEnabledCheckBox;

    @FXML
    private TextField paymentMockSecretField;

    @FXML
    private TextField paymentOrderExpireMinutesField;

    @FXML
    private TextField paymentNotifyUrlField;

    @FXML
    private CheckBox wechatEnabledCheckBox;

    @FXML
    private TextField wechatAppIdField;

    @FXML
    private TextField wechatMchIdField;

    @FXML
    private PasswordField wechatApiKeyField;

    @FXML
    private TextField wechatCertPathField;

    @FXML
    private TextField wechatPrivateKeyPathField;

    @FXML
    private TextField wechatMerchantSerialNoField;

    @FXML
    private CheckBox alipayEnabledCheckBox;

    @FXML
    private TextField alipayAppIdField;

    @FXML
    private PasswordField alipayPrivateKeyField;

    @FXML
    private CheckBox showAlipayPrivateKeyCheckBox;

    @FXML
    private TextArea alipayPublicKeyArea;

    @FXML
    private TextField alipayCertPathField;

    @FXML
    private TextField alipayGatewayField;

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
        I18nManager i18n = I18nManager.getInstance();

        // 初始化语言下拉框
        languageComboBox.setItems(javafx.collections.FXCollections.observableArrayList(
            "简体中文",
            "繁體中文",
            "English"
        ));
        languageComboBox.getSelectionModel().select(0);

        // 初始化货币下拉框
        currencyComboBox.setItems(javafx.collections.FXCollections.observableArrayList(
            i18n.get(I18nKeys.Currency.CNY),
            i18n.get("currency.usd"),
            i18n.get("currency.jpy"),
            i18n.get("currency.krw"),
            i18n.get("currency.eur")
        ));
        currencyComboBox.getSelectionModel().select(0);

        // 初始化主题下拉框
        themeComboBox.setItems(javafx.collections.FXCollections.observableArrayList(
            i18n.get(I18nKeys.Menu.Theme.LIGHT),
            i18n.get(I18nKeys.Menu.Theme.DARK),
            i18n.get(I18nKeys.Menu.Theme.LISUAN)
        ));
        themeComboBox.getSelectionModel().select(i18n.get(I18nKeys.Menu.Theme.LISUAN));

        // 初始化字号下拉框
        fontSizeComboBox.setItems(javafx.collections.FXCollections.observableArrayList(
            i18n.get(I18nKeys.Settings.FONT_SIZE_SMALL),
            i18n.get(I18nKeys.Settings.FONT_SIZE_MEDIUM),
            i18n.get(I18nKeys.Settings.FONT_SIZE_LARGE),
            i18n.get(I18nKeys.Settings.FONT_SIZE_EXTRA_LARGE)
        ));
        fontSizeComboBox.getSelectionModel().select(1); // 默认选中中等

        // 初始化纸张大小下拉框
        paperSizeComboBox.setItems(javafx.collections.FXCollections.observableArrayList(
            i18n.get("settings.paper_58mm"),
            i18n.get("settings.paper_80mm"),
            "A4"
        ));
        paperSizeComboBox.getSelectionModel().select(0);

        // 初始化备份频率下拉框
        backupFrequencyComboBox.setItems(javafx.collections.FXCollections.observableArrayList(
            i18n.get("settings.backup_daily"),
            i18n.get("settings.backup_weekly"),
            i18n.get("settings.backup_monthly")
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

        // 初始化支付模式下拉框
        paymentModeComboBox.setItems(javafx.collections.FXCollections.observableArrayList(
            "disabled",
            "mock",
            "production"
        ));
        paymentModeComboBox.getSelectionModel().select("disabled");

        // 初始化数据导入工具
        dataImporter = new com.cashier.util.ProductDataImporter();

        logger.info("SettingsController: 系统设置初始化完成");
    }

    /**
     * 设置当前用户
     * @param user 当前用户
     */
    public void setCurrentUser(com.cashier.model.User user) {
        this.currentUser = user;
        loadSettings();
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
        String savedPaperSize = settings.getOrDefault("paperSize", "");
        if (!savedPaperSize.isEmpty() && paperSizeComboBox.getItems().contains(savedPaperSize)) {
            paperSizeComboBox.setValue(savedPaperSize);
        }
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
        backupPathField.setText(DataService.resolveSqlBackupPath(settings.get("backupPath")));

        // 加载安全设置
        autoLogoutCheckBox.setSelected(Boolean.parseBoolean(settings.getOrDefault("autoLogout", "true")));
        passwordComplexityCheckBox.setSelected(Boolean.parseBoolean(settings.getOrDefault("passwordComplexity", "true")));

        // 加载主题偏好
        String username = (currentUser != null) ? currentUser.username : "default";
        String savedThemeCode = DataService.loadThemePreference(username);
        String savedThemeName = convertThemeCodeToName(savedThemeCode);
        themeComboBox.getSelectionModel().select(savedThemeName);

        // 加载语言偏好 - 从数据库加载当前用户的语言偏好
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
            String savedCurrency = com.cashier.dao.DAOFactory.getInstance().getLanguagePreferenceDAO().getCurrencyPreference(username);
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

        // 加载支付配置
        loadPaymentSettings();

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
                // 保存语言偏好到 language_preferences 表
                if (languageChanged) {
                    String username = (currentUser != null) ? currentUser.username : "default";
                    // 保存用户特定偏好
                    com.cashier.service.DataService.saveLanguagePreference(username, newLanguageTag);
                    // 同时更新全局默认（"default"用户），确保新用户使用系统默认语言
                    com.cashier.service.DataService.saveLanguagePreference("default", newLanguageTag);
                    logger.info("语言偏好已保存: username={}, languageTag={}, 全局默认已更新", username, newLanguageTag);
                }
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
                    com.cashier.dao.DAOFactory.getInstance().getLanguagePreferenceDAO().setCurrencyPreference(username, newCurrencyCode);
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
                showSuccess(com.cashier.i18n.I18nManager.getInstance().get("runtime.settings_basic_saved"));
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
                showError(I18nManager.getInstance().get("runtime.restart_unavailable"));
                return;
            }
            logger.info("调用 logoutToLoginView 返回登录界面...");
            app.logoutToLoginView(); // 回到登录界面，语言已更改
            logger.info("重启应用完成");
        } catch (Exception e) {
            logger.error("重启应用失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED));
        }
    }

    /**
     * 将中文主题名称转换为英文主题代码
     * @param themeName 中文主题名称
     * @return 英文主题代码
     */
    private String convertThemeNameToCode(String themeName) {
        if (themeName == null) {
            return FXConstants.DEFAULT_THEME;
        }
        I18nManager i18n = I18nManager.getInstance();
        if (themeName.equals(i18n.get(I18nKeys.Menu.Theme.LIGHT)) || "浅色主题".equals(themeName)) return "light";
        if (themeName.equals(i18n.get(I18nKeys.Menu.Theme.DARK)) || "深色主题".equals(themeName)) return "dark";
        if (themeName.equals(i18n.get(I18nKeys.Menu.Theme.LISUAN)) || "LiSuan主题".equals(themeName) || "IntelliJ主题".equals(themeName)) return "lisuan";
        return FXConstants.DEFAULT_THEME;
    }

    /**
     * 将英文主题代码转换为中文主题名称
     * @param themeCode 英文主题代码
     * @return 中文主题名称
     */
    private String convertThemeCodeToName(String themeCode) {
        if (themeCode == null) {
            return I18nManager.getInstance().get(I18nKeys.Menu.Theme.LISUAN);
        }
        switch (themeCode) {
            case "light":
                return I18nManager.getInstance().get(I18nKeys.Menu.Theme.LIGHT);
            case "dark":
                return I18nManager.getInstance().get(I18nKeys.Menu.Theme.DARK);
            case "lisuan":
            case "intellij":
                return I18nManager.getInstance().get(I18nKeys.Menu.Theme.LISUAN);
            default:
                return I18nManager.getInstance().get(I18nKeys.Menu.Theme.LISUAN);
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
            return I18nManager.getInstance().get(I18nKeys.Settings.FONT_SIZE_MEDIUM);
        }
        I18nManager i18n = I18nManager.getInstance();
        switch (fontSizeCode) {
            case "small":
                return i18n.get(I18nKeys.Settings.FONT_SIZE_SMALL);
            case "medium":
                return i18n.get(I18nKeys.Settings.FONT_SIZE_MEDIUM);
            case "large":
                return i18n.get(I18nKeys.Settings.FONT_SIZE_LARGE);
            case "extra-large":
                return i18n.get(I18nKeys.Settings.FONT_SIZE_EXTRA_LARGE);
            default:
                return i18n.get(I18nKeys.Settings.FONT_SIZE_MEDIUM);
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
        String small = i18n.get(I18nKeys.Settings.FONT_SIZE_SMALL);
        String medium = i18n.get(I18nKeys.Settings.FONT_SIZE_MEDIUM);
        String large = i18n.get(I18nKeys.Settings.FONT_SIZE_LARGE);
        String extraLarge = i18n.get(I18nKeys.Settings.FONT_SIZE_EXTRA_LARGE);

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
        I18nManager i18n = I18nManager.getInstance();
        if (currencyCode == null) {
            return i18n.get(I18nKeys.Currency.CNY);
        }
        switch (currencyCode) {
            case "CNY":
                return i18n.get(I18nKeys.Currency.CNY);
            case "USD":
                return i18n.get("currency.usd");
            case "JPY":
                return i18n.get("currency.jpy");
            case "KRW":
                return i18n.get("currency.krw");
            case "EUR":
                return i18n.get("currency.eur");
            default:
                return i18n.get(I18nKeys.Currency.CNY);
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
        showSuccess(com.cashier.i18n.I18nManager.getInstance().get("runtime.settings_print_saved"));
    }

    /**
     * 处理保存备份设置
     */
    @FXML
    public void handleSaveBackupSettings() {
        saveSettings();
        showSuccess(com.cashier.i18n.I18nManager.getInstance().get("runtime.settings_backup_saved"));
    }

    /**
     * 处理保存安全设置
     */
    @FXML
    public void handleSaveSecuritySettings() {
        saveSettings();
        showSuccess(com.cashier.i18n.I18nManager.getInstance().get("runtime.settings_security_saved"));
    }

    /**
     * 处理保存支付设置
     */
    @FXML
    public void handleSavePaymentSettings() {
        try {
            PaymentService.PaymentConfig paymentConfig = buildPaymentConfigFromForm();
            PaymentService.saveConfig(paymentConfig);
            String username = (currentUser != null) ? currentUser.username : "default";
            com.cashier.service.AuditService.success(username, "SETTINGS", "PAYMENT_SETTINGS_UPDATED",
                "支付模式=" + paymentConfig.mode
                    + ", 微信启用=" + paymentConfig.wechatEnabled
                    + ", 支付宝启用=" + paymentConfig.alipayEnabled,
                1);
            showSuccess(I18nManager.getInstance().get("runtime.settings_payment_saved"));
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            logger.error("保存支付配置失败", e);
            showError(I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED));
        }
    }

    /**
     * H-20: 切换支付宝私钥显示/隐藏（需二次确认）
     */
    @FXML
    public void handleTogglePrivateKeyVisibility() {
        if (showAlipayPrivateKeyCheckBox.isSelected()) {
            String keyValue = alipayPrivateKeyField.getText();
            if (keyValue == null || keyValue.trim().isEmpty()) {
                showAlipayPrivateKeyCheckBox.setSelected(false);
                return;
            }
            // 二次确认后显示明文
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(I18nManager.getInstance().get(I18nKeys.Common.WARNING));
            alert.setHeaderText(com.cashier.i18n.I18nManager.getInstance().get("runtime.private_key_visibility_warning"));
            alert.setContentText(keyValue);
            alert.showAndWait();
            // 查看后自动取消勾选，恢复掩码状态
            showAlipayPrivateKeyCheckBox.setSelected(false);
        }
    }

    /**
     * 处理浏览备份路径
     */
    @FXML
    public void handleBrowseBackupPath() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.choose_backup_directory"));

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
        fileChooser.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.choose_logo"));

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
        logoInfoLabel.setText(com.cashier.i18n.I18nManager.getInstance().get("runtime.logo_size_hint"));
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

            showSuccess(com.cashier.i18n.I18nManager.getInstance().get("runtime.logo_added"));

        } catch (Exception e) {
            logger.error("复制 Logo 文件失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.SAVE_DATA));
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
                logoInfoLabel.setText(I18nManager.getInstance().get("runtime.logo_current_size", width, height));

            } catch (Exception e) {
                logger.error("加载 Logo 预览失败", e);
                logoPreviewPlaceholder.setVisible(true);
            }
        } else {
            logoPreviewImage.setImage(null);
            logoPreviewPlaceholder.setVisible(true);
            logoInfoLabel.setText(com.cashier.i18n.I18nManager.getInstance().get("runtime.logo_size_hint"));
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
        if (backupInProgress) {
            showError("备份正在进行中，请稍候…");
            return;
        }
        try {
            // 获取用户选择的备份路径，如果为空则使用默认 SQL 备份目录
            final String backupBasePath = DataService.resolveSqlBackupPath(backupPathField.getText());
            
            // 确保备份路径存在（快速文件操作留在 FX 线程）
            File backupDir = new File(backupBasePath);
            if (!backupDir.exists() && !backupDir.mkdirs()) {
                showError(I18nManager.getInstance().get("runtime.backup_path_create_failed", backupBasePath));
                return;
            }
            
            backupInProgress = true;
            com.cashier.util.StatusBarManager.updateWarning("正在备份…请稍候，完成后会有提示");
            // 整库 dump 可能耗时较长，放到 daemon 线程执行，避免冻结 UI
            Thread worker = new Thread(() -> {
                try {
                    DataService.backupData(backupBasePath);

                    // 获取最新的备份文件名
                    File[] sqlFiles = new File(backupBasePath)
                        .listFiles((dir, name) -> isCurrentDatabaseBackupFile(name));
                    String newestName = null;
                    if (sqlFiles != null && sqlFiles.length > 0) {
                        java.util.Arrays.sort(sqlFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                        newestName = sqlFiles[0].getName();
                    }
                    final String backupFileName = newestName;

                    javafx.application.Platform.runLater(() -> {
                        backupInProgress = false;
                        if (backupFileName != null) {
                            showSuccess(I18nManager.getInstance().get("runtime.backup_file_success", backupFileName));
                        } else {
                            showSuccess(com.cashier.i18n.I18nManager.getInstance().get("runtime.backup_success"));
                        }
                    });
                } catch (Exception e) {
                    logger.error("立即备份失败", e);
                    javafx.application.Platform.runLater(() -> {
                        backupInProgress = false;
                        showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED));
                    });
                }
            }, "settings-backup");
            worker.setDaemon(true);
            worker.start();
        } catch (Exception e) {
            backupInProgress = false;
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED));
        }
    }

    /**
     * 处理恢复数据
     */
    @FXML
    public void handleRestore() {
        // 获取用户选择的备份路径，如果为空则使用默认 SQL 备份目录
        final String backupBasePath = DataService.resolveSqlBackupPath(backupPathField.getText());
        
        // 列出可用的备份文件
        File backupDir = new File(backupBasePath);
        if (!backupDir.exists()) {
            showError(I18nManager.getInstance().get("runtime.backup_path_missing", backupBasePath));
            return;
        }
        
        File[] sqlFiles = backupDir.listFiles((dir, name) -> isCurrentDatabaseBackupFile(name));
        
        if (sqlFiles == null || sqlFiles.length == 0) {
            showError(I18nManager.getInstance().get("runtime.backup_not_found", backupBasePath));
            return;
        }
        
        // 按修改时间排序，最新的在前
        java.util.Arrays.sort(sqlFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        
        // 创建选择对话框
        ChoiceDialog<String> dialog = new ChoiceDialog<>();
        dialog.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.choose_backup"));
        dialog.setHeaderText(com.cashier.i18n.I18nManager.getInstance().get("runtime.choose_backup_header"));
        dialog.setContentText(com.cashier.i18n.I18nManager.getInstance().get("runtime.available_backups"));
        
        // 添加备份选项
        ObservableList<String> options = FXCollections.observableArrayList();
        for (File file : sqlFiles) {
            String timeStr = java.time.Instant.ofEpochMilli(file.lastModified())
                .atZone(java.time.ZoneId.systemDefault())
                .format(com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME);
            options.add(file.getName() + " (" + timeStr + ")");
        }
        dialog.getItems().addAll(options);
        
        java.util.Optional<String> selectedBackup = dialog.showAndWait();
        if (selectedBackup.isEmpty()) {
            com.cashier.util.StatusBarManager.updateWarning(
                I18nManager.getInstance().get(I18nKeys.Status.CANCELLED));
            return;
        }

        selectedBackup.ifPresent(selected -> {
            // 提取备份文件名
            String backupFileName = selected.split(" \\(")[0];
            File backupFile = new File(backupBasePath, backupFileName);
            
            try {
                // 确认恢复
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.confirm_restore"));
                confirmAlert.setHeaderText(null);
                confirmAlert.setContentText(I18nManager.getInstance().get("runtime.restore_confirm", backupFileName));
                
                if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    // 恢复前必须验证管理员密码，防止未授权覆盖数据。
                    // 弹窗只负责收集输入（快）；BCrypt 验证放到恢复 worker 后台执行，避免冻结 UI。
                    Optional<String> entered = askRestoreAdminPassword();
                    if (entered.isEmpty()) {
                        com.cashier.util.StatusBarManager.updateWarning(
                            I18nManager.getInstance().get(I18nKeys.Status.CANCELLED));
                        return;
                    }
                    final String password = entered.get();
                    final String restoreFilePath = backupFile.getAbsolutePath();
                    com.cashier.util.StatusBarManager.updateWarning("正在恢复数据…请稍候，完成后会有提示");
                    // 整库恢复可能耗时较长，放到 daemon 线程执行，避免冻结 UI
                    Thread worker = new Thread(() -> {
                        try {
                            User admin = DAOFactory.getInstance().getUserDAO().findByUsername("admin");
                            if (admin == null || admin.password == null || admin.password.isBlank()) {
                                javafx.application.Platform.runLater(() ->
                                    showError(I18nManager.getInstance().get("runtime.restore_admin_not_configured")));
                                return;
                            }
                            if (!PasswordUtil.verifyPassword(password, admin.password)) {
                                javafx.application.Platform.runLater(() ->
                                    showError(I18nManager.getInstance().get("runtime.password_incorrect")));
                                return;
                            }

                            DataService.restoreData(restoreFilePath);
                            javafx.application.Platform.runLater(() -> {
                                showSuccess(com.cashier.i18n.I18nManager.getInstance().get("runtime.restore_success"));
                                // 重新加载数据
                                loadSettings();
                            });
                        } catch (Exception e) {
                            logger.error("恢复数据失败", e);
                            javafx.application.Platform.runLater(() ->
                                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED)));
                        }
                    }, "settings-restore");
                    worker.setDaemon(true);
                    worker.start();
                } else {
                    com.cashier.util.StatusBarManager.updateWarning(
                        I18nManager.getInstance().get(I18nKeys.Status.CANCELLED));
                }
            } catch (Exception e) {
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED));
            }
        });
    }

    /**
     * 弹出管理员密码输入框，仅负责收集输入（BCrypt 校验由调用方在后台线程执行）。
     * @return 确定时返回输入的密码（可能为空串）；取消时返回 empty
     */
    private Optional<String> askRestoreAdminPassword() {
        javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle(I18nManager.getInstance().get("runtime.confirm_restore"));
        dialog.setHeaderText(I18nManager.getInstance().get("runtime.restore_admin_password_hint"));

        javafx.scene.control.PasswordField passwordField = new javafx.scene.control.PasswordField();
        passwordField.setPromptText(I18nManager.getInstance().get("runtime.password"));

        VBox content = new VBox(10, passwordField);
        dialog.getDialogPane().setContent(content);

        ButtonType okType = new ButtonType(
            I18nManager.getInstance().get(I18nKeys.Common.CONFIRM), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType(
            I18nManager.getInstance().get(I18nKeys.Common.CANCEL), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, cancelType);
        dialog.setResultConverter(btn -> btn == okType ? passwordField.getText() : null);

        return dialog.showAndWait();
    }

    /**
     * 处理重置所有设置
     */
    @FXML
    public void handleResetAll() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18nManager.getInstance().get(I18nKeys.Common.CONFIRM));
        alert.setHeaderText(null);
        alert.setContentText(com.cashier.i18n.I18nManager.getInstance().get("runtime.settings_reset_confirm"));

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            // 清空所有字段
            storeNameField.clear();
            storeAddressField.clear();
            storePhoneField.clear();
            taxRateField.setText("0.0");
            printerNameField.clear();
            backupPathField.setText(DataService.DEFAULT_SQL_BACKUP_PATH);
            
            // 重置为默认值
            enablePrintCheckBox.setSelected(false);
            autoBackupCheckBox.setSelected(false);
            autoLogoutCheckBox.setSelected(true);
            passwordComplexityCheckBox.setSelected(true);
            
            showSuccess(com.cashier.i18n.I18nManager.getInstance().get("runtime.settings_reset_success"));
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

    private void loadPaymentSettings() {
        PaymentService.PaymentConfig paymentConfig = PaymentService.getConfig();
        if (paymentConfig == null) {
            paymentConfig = new PaymentService.PaymentConfig();
        }

        paymentModeComboBox.getSelectionModel().select(defaultText(paymentConfig.mode, "disabled"));
        paymentMockEnabledCheckBox.setSelected(paymentConfig.mockEnabled);
        paymentMockSecretField.setText(defaultText(paymentConfig.mockCallbackSecret, ""));
        paymentOrderExpireMinutesField.setText(String.valueOf(paymentConfig.orderExpireMinutes));
        paymentNotifyUrlField.setText(defaultText(paymentConfig.notifyUrl, ""));

        wechatEnabledCheckBox.setSelected(paymentConfig.wechatEnabled);
        wechatAppIdField.setText(defaultText(paymentConfig.wechatAppId, ""));
        wechatMchIdField.setText(defaultText(paymentConfig.wechatMchId, ""));
        wechatApiKeyField.setText(defaultText(paymentConfig.wechatApiKey, ""));
        wechatCertPathField.setText(defaultText(paymentConfig.wechatCertPath, ""));
        wechatPrivateKeyPathField.setText(defaultText(paymentConfig.wechatPrivateKeyPath, ""));
        wechatMerchantSerialNoField.setText(defaultText(paymentConfig.wechatMerchantSerialNo, ""));

        alipayEnabledCheckBox.setSelected(paymentConfig.alipayEnabled);
        alipayAppIdField.setText(defaultText(paymentConfig.alipayAppId, ""));
        alipayPrivateKeyField.setText(defaultText(paymentConfig.alipayPrivateKey, ""));
        alipayPublicKeyArea.setText(defaultText(paymentConfig.alipayPublicKey, ""));
        alipayCertPathField.setText(defaultText(paymentConfig.alipayCertPath, ""));
        alipayGatewayField.setText(defaultText(paymentConfig.alipayGateway, ""));
    }

    private PaymentService.PaymentConfig buildPaymentConfigFromForm() {
        PaymentService.PaymentConfig paymentConfig = new PaymentService.PaymentConfig();
        String selectedMode = paymentModeComboBox.getSelectionModel().getSelectedItem();
        paymentConfig.mode = defaultText(selectedMode, "disabled").trim().toLowerCase();
        if (!List.of("disabled", "mock", "production").contains(paymentConfig.mode)) {
            throw new IllegalArgumentException(I18nManager.getInstance().get("runtime.payment_mode_invalid"));
        }

        try {
            paymentConfig.orderExpireMinutes = Integer.parseInt(paymentOrderExpireMinutesField.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(I18nManager.getInstance().get("runtime.payment_expire_invalid"));
        }
        if (paymentConfig.orderExpireMinutes < 1 || paymentConfig.orderExpireMinutes > 120) {
            throw new IllegalArgumentException(I18nManager.getInstance().get("runtime.payment_expire_invalid"));
        }

        paymentConfig.mockEnabled = paymentMockEnabledCheckBox.isSelected();
        paymentConfig.mockCallbackSecret = paymentMockSecretField.getText().trim();
        paymentConfig.notifyUrl = paymentNotifyUrlField.getText().trim();

        paymentConfig.wechatEnabled = wechatEnabledCheckBox.isSelected();
        paymentConfig.wechatAppId = wechatAppIdField.getText().trim();
        paymentConfig.wechatMchId = wechatMchIdField.getText().trim();
        paymentConfig.wechatApiKey = wechatApiKeyField.getText().trim();
        paymentConfig.wechatCertPath = wechatCertPathField.getText().trim();
        paymentConfig.wechatPrivateKeyPath = wechatPrivateKeyPathField.getText().trim();
        paymentConfig.wechatMerchantSerialNo = wechatMerchantSerialNoField.getText().trim();

        paymentConfig.alipayEnabled = alipayEnabledCheckBox.isSelected();
        paymentConfig.alipayAppId = alipayAppIdField.getText().trim();
        paymentConfig.alipayPrivateKey = alipayPrivateKeyField.getText().trim();
        paymentConfig.alipayPublicKey = alipayPublicKeyArea.getText().trim();
        paymentConfig.alipayCertPath = alipayCertPathField.getText().trim();
        paymentConfig.alipayGateway = alipayGatewayField.getText().trim();
        return paymentConfig;
    }

    private String defaultText(String value, String defaultValue) {
        return value == null ? defaultValue : value;
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
        settings.put("theme", selectedTheme != null ? selectedTheme : I18nManager.getInstance().get(I18nKeys.Menu.Theme.LISUAN));
        String selectedCurrency = currencyComboBox.getSelectionModel().getSelectedItem();
        settings.put("currency", selectedCurrency != null ? selectedCurrency : I18nManager.getInstance().get(I18nKeys.Currency.CNY));

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
        try {
            DataService.saveSettings(settings);
        } catch (SQLException e) {
            logger.error("保存设置数据失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.save_failed"));
            return;
        }

        // 保存主题偏好（单独存储到主题偏好表）
        String themeName = settings.getOrDefault("theme", I18nManager.getInstance().get(I18nKeys.Menu.Theme.LISUAN));
        String themeCode = convertThemeNameToCode(themeName);
        String username = (currentUser != null) ? currentUser.username : "default";
        DataService.saveThemePreference(username, themeCode);

        // 保存语言偏好 - 保存到当前用户
        String languageName = settings.getOrDefault("language", "简体中文");
        String languageTag = convertLanguageNameToTag(languageName);
        DataService.saveLanguagePreference(username, languageTag);

        // 保存字号偏好 - 保存到当前用户
        String fontSizeName = fontSizeComboBox.getSelectionModel().getSelectedItem();
        String fontSizeCode = convertFontSizeNameToCode(fontSizeName);
        DataService.saveFontSizePreference(username, fontSizeCode);

        logger.info("SettingsController: 设置保存成功，主题: {}, 语言: {}, 字号: {}, 用户: {}", themeCode, languageTag, fontSizeCode, username);
        com.cashier.service.AuditService.success(username, "SETTINGS", "SETTINGS_UPDATED",
            "主题=" + themeCode + ", 语言=" + languageTag + ", 字号=" + fontSizeCode,
            settings.size());
    }

    /**
     * 测试打印：向当前默认打印机输出测试页
     */
    @FXML
    public void handleTestPrint() {
        PrinterManager manager = PrinterManager.getInstance();
        PrinterDevice device = manager.getDefaultPrinter();
        if (device == null) {
            java.util.List<PrinterDevice> devices = manager.getAllDevices();
            if (devices.size() == 1) {
                device = devices.get(0);
            }
        }
        if (device == null || !device.isConnected()) {
            showError(I18nManager.getInstance().get("runtime.print_no_printer"));
            return;
        }

        int width = 48;
        StringBuilder content = new StringBuilder();
        content.append(EscPosUtils.createSeparator(width, '-')).append("\n");
        content.append(EscPosUtils.centerText(I18nManager.getInstance().get("settings.test_print"), width)).append("\n");
        content.append(EscPosUtils.createSeparator(width, '-')).append("\n");
        content.append("设备名称: ").append(device.getDeviceName()).append("\n");
        content.append("设备ID: ").append(device.getDeviceId()).append("\n");
        content.append("设备类型: ").append(device.getDeviceType().getDisplayName()).append("\n");
        if (device instanceof NetworkPrinterDevice) {
            NetworkPrinterDevice netPrinter = (NetworkPrinterDevice) device;
            content.append("IP地址: ").append(netPrinter.getHostAddress()).append("\n");
            content.append("端口: ").append(netPrinter.getPort()).append("\n");
        }
        content.append("打印时间: ")
            .append(DateTimeFormats.formatStandard(LocalDateTime.now(ZoneId.systemDefault()))).append("\n");
        content.append(EscPosUtils.createSeparator(width, '-')).append("\n");
        content.append("\n\n");

        // 网络打印机连接/IO 可能阻塞数秒（每台 5s 超时），放到 daemon 线程执行，避免冻结设置页
        final PrintTask task = PrintTask.createTestTask(content.toString());
        testPrintButton.setDisable(true);
        Thread worker = new Thread(() -> {
            boolean ok;
            try {
                ok = manager.print(task);
            } catch (Exception e) {
                logger.error("测试打印异常", e);
                ok = false;
            }
            final boolean success = ok;
            javafx.application.Platform.runLater(() -> {
                testPrintButton.setDisable(false);
                if (success) {
                    showSuccess(I18nManager.getInstance().get("runtime.print_success"));
                } else {
                    showError(I18nManager.getInstance().get("runtime.print_failed"));
                }
            });
        }, "settings-test-print");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * 扫码枪测试：使用与设备一致的校验规则验证扫码数据
     */
    @FXML
    public void handleScannerTest() {
        ScanValidation validation = USBHIDScannerDevice.validateScanData(
            scannerTestField.getText(), USBHIDScannerDevice.DEFAULT_MAX_SCAN_LENGTH);
        I18nManager i18n = I18nManager.getInstance();

        scannerTestResultLabel.getStyleClass().removeAll("text-success", "text-danger", "text-muted");
        if (validation.isAccepted()) {
            scannerTestResultLabel.setText(i18n.get(I18nKeys.Settings.SCANNER_TEST_OK,
                validation.getNormalizedData(), validation.getNormalizedData().length()));
            scannerTestResultLabel.getStyleClass().add("text-success");
        } else if (ScanValidation.ERROR_TOO_LONG.equals(validation.getErrorCode())) {
            scannerTestResultLabel.setText(i18n.get(I18nKeys.Settings.SCANNER_TEST_TOO_LONG,
                validation.getNormalizedData().length(), USBHIDScannerDevice.DEFAULT_MAX_SCAN_LENGTH));
            scannerTestResultLabel.getStyleClass().add("text-danger");
        } else {
            scannerTestResultLabel.setText(i18n.get(I18nKeys.Settings.SCANNER_TEST_EMPTY));
            scannerTestResultLabel.getStyleClass().add("text-danger");
        }
        scannerTestField.selectAll();
    }

    /**
     * 显示成功消息
     * @param message 消息内容
     */
    private void showSuccess(String message) {
        com.cashier.util.StatusBarManager.updateSuccess(message);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18nManager.getInstance().get(I18nKeys.Label.SUCCESS));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示错误信息
     * @param message 错误消息
     */
    private void showError(String message) {
        com.cashier.util.FXUtils.showError(message);
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
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED));
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
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED));
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
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED));
        }
    }

    /**
     * 浏览 CSV 文件
     */
    @FXML
    public void handleBrowseCsvFile() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.choose_csv"));
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
            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.csv_select"));
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
        importStatusLabel.setText(com.cashier.i18n.I18nManager.getInstance().get("runtime.csv_importing"));

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
                        importStatusLabel.setText(com.cashier.i18n.I18nManager.getInstance().get("runtime.import_complete"));
                        showSuccess(com.cashier.i18n.I18nManager.getInstance().get("runtime.csv_import_success"));
                    } else {
                        importProgressBar.setProgress(1);
                        importStatusLabel.setText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.IMPORT_DATA));
                        showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED));
                    }
                    
                    // 延迟隐藏进度条，避免阻塞 JavaFX UI 线程。
                    PauseTransition hideProgressDelay = new PauseTransition(Duration.seconds(2));
                    hideProgressDelay.setOnFinished(event -> importProgressBar.setVisible(false));
                    hideProgressDelay.play();
                });
            } catch (Exception e) {
                logger.error("从 CSV 导入数据失败", e);
                javafx.application.Platform.runLater(() -> {
                    importProgressBar.setVisible(false);
                    importStatusLabel.setText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.IMPORT_DATA));
                    showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED));
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
        Label logLabel = new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.import_log"));
        logLabel.getStyleClass().add("font-bold");
        importMessagesArea.getChildren().add(logLabel);
    }

    private boolean isCurrentDatabaseBackupFile(String fileName) {
        return fileName != null
            && fileName.startsWith(DatabaseManager.getBackupFilePrefix() + "_")
            && fileName.endsWith(".sql");
    }
}
