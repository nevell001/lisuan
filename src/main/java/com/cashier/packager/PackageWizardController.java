package com.cashier.packager;

import com.cashier.util.LoggerFactoryUtil;
import javafx.animation.FadeTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 图形界面打包向导控制器
 * 提供可视化配置和打包功能
 */
public class PackageWizardController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(PackageWizardController.class);

    @FXML private VBox wizardContainer;
    @FXML private ProgressBar progressBar;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label statusLabel;
    @FXML private TextArea logTextArea;

    // 基本设置页
    @FXML private TextField appNameField;
    @FXML private TextField appVersionField;
    @FXML private TextField vendorField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField iconPathField;

    // 打包选项页
    @FXML private CheckBox embedJreCheckBox;
    @FXML private CheckBox createShortcutCheckBox;
    @FXML private CheckBox addMenuCheckBox;
    @FXML private CheckBox dirChooserCheckBox;
    @FXML private Spinner<Integer> memoryMinSpinner;
    @FXML private Spinner<Integer> memoryMaxSpinner;
    @FXML private TextField outputDirField;

    // 数据库配置页
    @FXML private ComboBox<String> dbTypeComboBox;
    @FXML private TextField dbHostField;
    @FXML private TextField dbPortField;
    @FXML private TextField dbNameField;
    @FXML private TextField dbUsernameField;
    @FXML private PasswordField dbPasswordField;
    @FXML private Button testConnectionButton;

    // 导航按钮
    @FXML private Button backButton;
    @FXML private Button nextButton;
    @FXML private Button cancelButton;
    @FXML private Button finishButton;

    // 步骤指示器
    @FXML private ToggleButton step1Button;
    @FXML private ToggleButton step2Button;
    @FXML private ToggleButton step3Button;
    @FXML private ToggleButton step4Button;

    // 步骤内容区域
    @FXML private VBox step1Content;
    @FXML private VBox step2Content;
    @FXML private VBox step3Content;
    @FXML private VBox step4Content;

    private int currentStep = 1;
    private static final int TOTAL_STEPS = 4;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @FXML
    private void initialize() {
        setupMemorySpinners();
        setupDbTypeComboBox();
        loadDefaultValues();
        updateNavigation();
    }

    private void setupMemorySpinners() {
        SpinnerValueFactory<Integer> minFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(256, 2048, 512, 128);
        SpinnerValueFactory<Integer> maxFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(512, 4096, 1024, 128);
        memoryMinSpinner.setValueFactory(minFactory);
        memoryMaxSpinner.setValueFactory(maxFactory);
    }

    private void setupDbTypeComboBox() {
        dbTypeComboBox.getItems().addAll("MySQL (Docker)", "MySQL (本地)", "MySQL (远程)");
        dbTypeComboBox.getSelectionModel().select(0);
        dbTypeComboBox.setOnAction(e -> handleDbTypeChange());
    }

    private void loadDefaultValues() {
        try {
            // 从 pom.xml 读取版本
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c",
                    "findstr \"<version>\" pom.xml | findstr /V \"javafx maven java mysql hikaricp poi pdfbox controlsfx fontawesomefx junit testfx h2 bcrypt logback jackson javalin slf4j plugin\"");
            pb.directory(new File("."));
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("<version>") && line.contains("</version>")) {
                    String version = line.substring(line.indexOf("<version>") + 9, line.indexOf("</version>"));
                    appVersionField.setText(version);
                    break;
                }
            }
        } catch (Exception e) {
            logger.warn("无法从 pom.xml 读取版本，使用默认值");
        }

        appNameField.setText("LiSuan");
        vendorField.setText("LiSuan");
        descriptionArea.setText("狸算(LiSuan)收银系统 - 现代化收银管理系统");
        embedJreCheckBox.setSelected(true);
        createShortcutCheckBox.setSelected(true);
        addMenuCheckBox.setSelected(true);
        dirChooserCheckBox.setSelected(true);

        // 默认输出目录
        outputDirField.setText(System.getProperty("user.dir") + "\\target\\dist");

        // 默认数据库配置
        dbNameField.setText("lisuan_system");
        dbUsernameField.setText("lisuan");
        dbHostField.setText("localhost");
        dbPortField.setText("3306");
    }

    @FXML
    private void handleDbTypeChange() {
        String selected = dbTypeComboBox.getSelectionModel().getSelectedItem();
        switch (selected) {
            case "MySQL (Docker)":
                dbHostField.setText("localhost");
                dbPortField.setText("3306");
                dbNameField.setText("lisuan_system");
                dbUsernameField.setText("lisuan");
                break;
            case "MySQL (本地)":
                dbHostField.setText("localhost");
                dbPortField.setText("3306");
                dbNameField.setText("lisuan_system");
                dbUsernameField.setText("root");
                break;
            case "MySQL (远程)":
                dbHostField.setText("");
                dbPortField.setText("3306");
                dbNameField.setText("lisuan_system");
                dbUsernameField.setText("");
                break;
        }
    }

    @FXML
    private void handleTestConnection() {
        testConnectionButton.setDisable(true);
        testConnectionButton.setText("测试中...");

        Task<Boolean> testTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                String host = dbHostField.getText().trim();
                String port = dbPortField.getText().trim();
                String database = dbNameField.getText().trim();
                String username = dbUsernameField.getText().trim();
                String password = dbPasswordField.getText().trim();

                // 加载 MySQL 驱动并测试连接
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=Asia/Shanghai";
                    try (java.sql.Connection conn = java.sql.DriverManager.getConnection(url, username, password)) {
                        return conn.isValid(5);
                    }
                } catch (ClassNotFoundException e) {
                    logger.error("MySQL JDBC 驱动未找到", e);
                    appendLog("错误: MySQL JDBC 驱动未找到，请确保项目已编译");
                    return false;
                }
            }

            @Override
            protected void succeeded() {
                testConnectionButton.setDisable(false);
                if (getValue()) {
                    testConnectionButton.setText("连接成功 ✓");
                    testConnectionButton.setStyle("-fx-text-fill: green;");
                    appendLog("数据库连接测试成功！");
                    showAlert(Alert.AlertType.INFORMATION, "连接成功", "数据库连接测试通过！");
                } else {
                    testConnectionButton.setText("连接失败 ✗");
                    testConnectionButton.setStyle("-fx-text-fill: red;");
                    showAlert(Alert.AlertType.ERROR, "连接失败", "无法连接到数据库，请检查配置。\n\n详情请查看日志。");
                }
            }

            @Override
            protected void failed() {
                testConnectionButton.setDisable(false);
                testConnectionButton.setText("连接失败 ✗");
                testConnectionButton.setStyle("-fx-text-fill: red;");
                logger.error("数据库连接测试失败", getException());
                appendLog("错误: " + getException().getMessage());
            }
        };

        executorService.submit(testTask);
    }

    @FXML
    private void handleBack() {
        if (currentStep > 1) {
            currentStep--;
            updateNavigation();
        }
    }

    @FXML
    private void handleNext() {
        if (currentStep < TOTAL_STEPS) {
            // 验证当前步骤
            if (!validateCurrentStep()) {
                return;
            }
            currentStep++;
            updateNavigation();
        }
    }

    @FXML
    private void handleFinish() {
        if (!validateCurrentStep()) {
            return;
        }

        // 确认对话框
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认打包");
        confirmAlert.setHeaderText("即将开始打包，这可能需要几分钟时间");
        confirmAlert.setContentText("打包选项：\n" +
                "• 应用名称: " + appNameField.getText() + "\n" +
                "• 版本: " + appVersionField.getText() + "\n" +
                "• 嵌入 JRE: " + (embedJreCheckBox.isSelected() ? "是" : "否") + "\n" +
                "• 输出目录: " + outputDirField.getText());

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            startPackaging();
        }
    }

    @FXML
    private void handleCancel() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("取消打包");
        alert.setHeaderText("确定要取消打包吗？");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            System.exit(0);
        }
    }

    @FXML
    private void handleBrowseIcon() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择应用图标");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("ICO 文件", "*.ico"),
                new FileChooser.ExtensionFilter("PNG 文件", "*.png"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
        );

        File file = fileChooser.showOpenDialog(iconPathField.getScene().getWindow());
        if (file != null) {
            iconPathField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleBrowseOutputDir() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("选择输出目录");

        File initialDir = new File(outputDirField.getText());
        if (initialDir.exists()) {
            directoryChooser.setInitialDirectory(initialDir);
        }

        File dir = directoryChooser.showDialog(outputDirField.getScene().getWindow());
        if (dir != null) {
            outputDirField.setText(dir.getAbsolutePath());
        }
    }

    private boolean validateCurrentStep() {
        switch (currentStep) {
            case 1:
                if (appNameField.getText().trim().isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "验证失败", "请输入应用名称");
                    return false;
                }
                if (appVersionField.getText().trim().isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "验证失败", "请输入版本号");
                    return false;
                }
                break;
            case 3:
                if (dbHostField.getText().trim().isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "验证失败", "请输入数据库主机");
                    return false;
                }
                if (dbNameField.getText().trim().isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "验证失败", "请输入数据库名称");
                    return false;
                }
                break;
            case 4:
                if (outputDirField.getText().trim().isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "验证失败", "请选择输出目录");
                    return false;
                }
                File outputDir = new File(outputDirField.getText());
                if (!outputDir.exists() && !outputDir.mkdirs()) {
                    showAlert(Alert.AlertType.ERROR, "验证失败", "无法创建输出目录");
                    return false;
                }
                break;
        }
        return true;
    }

    private void updateNavigation() {
        // 更新步骤按钮
        step1Button.setSelected(currentStep >= 1);
        step2Button.setSelected(currentStep >= 2);
        step3Button.setSelected(currentStep >= 3);
        step4Button.setSelected(currentStep >= 4);

        // 显示/隐藏步骤内容
        step1Content.setVisible(currentStep == 1);
        step1Content.setManaged(currentStep == 1);
        step2Content.setVisible(currentStep == 2);
        step2Content.setManaged(currentStep == 2);
        step3Content.setVisible(currentStep == 3);
        step3Content.setManaged(currentStep == 3);
        step4Content.setVisible(currentStep == 4);
        step4Content.setManaged(currentStep == 4);

        // 更新导航按钮
        backButton.setDisable(currentStep == 1);
        nextButton.setDisable(currentStep == TOTAL_STEPS);
        finishButton.setVisible(currentStep == TOTAL_STEPS);
        finishButton.setManaged(currentStep == TOTAL_STEPS);
    }

    private void startPackaging() {
        disableAllControls(true);
        progressBar.setProgress(0);
        progressIndicator.setVisible(true);
        logTextArea.clear();

        appendLog("========================================");
        appendLog("开始打包: " + appNameField.getText() + " " + appVersionField.getText());
        appendLog("========================================\n");

        Task<Void> packagingTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    // 步骤 1: 编译项目
                    updateProgress(0.1, 1.0);
                    updateMessage("正在编译项目...");
                    appendLog("[1/5] 正在编译项目...");

                    ProcessBuilder mvnCompile = new ProcessBuilder("mvn", "clean", "compile", "-q");
                    mvnCompile.directory(new File(System.getProperty("user.dir")));
                    mvnCompile.redirectErrorStream(true);

                    Process compileProcess = mvnCompile.start();
                    logProcessOutput(compileProcess);

                    if (compileProcess.waitFor() != 0) {
                        throw new RuntimeException("编译失败");
                    }
                    appendLog("✓ 编译完成\n");

                    // 步骤 2: 打包 JAR
                    updateProgress(0.3, 1.0);
                    updateMessage("正在打包 JAR...");
                    appendLog("[2/5] 正在打包 JAR...");

                    ProcessBuilder mvnPackage = new ProcessBuilder("mvn", "package", "-DskipTests", "-q");
                    mvnPackage.directory(new File(System.getProperty("user.dir")));
                    mvnPackage.redirectErrorStream(true);

                    Process packageProcess = mvnPackage.start();
                    logProcessOutput(packageProcess);

                    if (packageProcess.waitFor() != 0) {
                        throw new RuntimeException("打包失败");
                    }
                    appendLog("✓ JAR 打包完成\n");

                    // 步骤 3: 创建自定义 JRE（如果选择嵌入）
                    if (embedJreCheckBox.isSelected()) {
                        updateProgress(0.5, 1.0);
                        updateMessage("正在创建自定义 JRE...");
                        appendLog("[3/5] 正在创建自定义 JRE...");

                        createCustomJre();
                        appendLog("✓ 自定义 JRE 创建完成\n");
                    }

                    // 步骤 4: 保存数据库配置
                    updateProgress(0.7, 1.0);
                    updateMessage("正在保存数据库配置...");
                    appendLog("[4/5] 正在保存数据库配置...");

                    saveDatabaseConfig();
                    appendLog("✓ 数据库配置已保存\n");

                    // 步骤 5: 使用 jpackage 创建 EXE
                    updateProgress(0.8, 1.0);
                    updateMessage("正在创建 EXE 安装包...");
                    appendLog("[5/5] 正在创建 EXE 安装包...");

                    createExePackage();
                    appendLog("✓ EXE 安装包创建完成\n");

                    updateProgress(1.0, 1.0);
                    appendLog("========================================");
                    appendLog("打包完成！");
                    appendLog("输出位置: " + outputDirField.getText());
                    appendLog("========================================");

                    javafx.application.Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION, "打包成功",
                                "EXE 安装包已创建！\n\n" +
                                "输出位置: " + outputDirField.getText() + "\n\n" +
                                "是否打开输出目录？");

                        // 打开输出目录
                        try {
                            Runtime.getRuntime().exec("explorer " + outputDirField.getText());
                        } catch (IOException e) {
                            logger.error("无法打开输出目录", e);
                        }
                    });

                } catch (Exception e) {
                    logger.error("打包失败", e);
                    appendLog("\n✗ 打包失败: " + e.getMessage());

                    javafx.application.Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "打包失败",
                                "打包过程中发生错误:\n" + e.getMessage());
                    });
                    throw e;
                }

                return null;
            }

            @Override
            protected void failed() {
                javafx.application.Platform.runLater(() -> {
                    disableAllControls(false);
                    progressIndicator.setVisible(false);
                });
            }

            @Override
            protected void succeeded() {
                javafx.application.Platform.runLater(() -> {
                    disableAllControls(false);
                    progressIndicator.setVisible(false);
                });
            }
        };

        progressBar.progressProperty().bind(packagingTask.progressProperty());
        statusLabel.textProperty().bind(packagingTask.messageProperty());

        executorService.submit(packagingTask);
    }

    private void createCustomJre() throws Exception {
        String jreOutput = "target/custom-jre";
        File jreDir = new File(jreOutput);

        if (jreDir.exists()) {
            appendLog("删除旧的 JRE...");
            deleteDirectory(jreDir);
        }

        ProcessBuilder jlink = new ProcessBuilder(
                "jlink",
                "--add-modules", "java.base,java.sql,java.logging,java.naming,java.desktop,java.xml,java.net.http",
                "--add-modules", "javafx.controls,javafx.fxml,javafx.graphics",
                "--output", jreOutput,
                "--strip-native-commands",
                "--compress=2",
                "--no-man-pages",
                "--no-header-files"
        );

        jlink.redirectErrorStream(true);
        Process jlinkProcess = jlink.start();
        logProcessOutput(jlinkProcess);

        if (jlinkProcess.waitFor() != 0) {
            throw new RuntimeException("jlink 创建 JRE 失败");
        }
    }

    private void saveDatabaseConfig() throws Exception {
        File configDir = new File("config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File configFile = new File(configDir, "database.properties");
        String url = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4",
                dbHostField.getText().trim(),
                dbPortField.getText().trim(),
                dbNameField.getText().trim());

        Properties props = new Properties();
        props.setProperty("db.url", url);
        props.setProperty("db.username", dbUsernameField.getText().trim());
        props.setProperty("db.password", dbPasswordField.getText().trim());
        props.setProperty("db.pool.size", "10");
        props.setProperty("db.connection.timeout", "30000");
        props.setProperty("db.idle.timeout", "600000");
        props.setProperty("db.max.lifetime", "1800000");

        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "LiSuan 数据库配置 - 由打包向导生成");
        }

        appendLog("数据库配置已保存到: " + configFile.getAbsolutePath());
    }

    private void createExePackage() throws Exception {
        String version = appVersionField.getText().trim();
        String fatJar = "lisuan-fx-" + version + "-jar-with-dependencies.jar";
        String jrePath = embedJreCheckBox.isSelected() ? "target/custom-jre" : null;

        ProcessBuilder jpackage;
        if (embedJreCheckBox.isSelected()) {
            jpackage = new ProcessBuilder(
                    "jpackage",
                    "--type", "exe",
                    "--name", appNameField.getText().trim(),
                    "--app-version", version,
                    "--vendor", vendorField.getText().trim(),
                    "--description", descriptionArea.getText().trim(),
                    "--dest", outputDirField.getText().trim(),
                    "--input", "target",
                    "--runtime-image", jrePath,
                    "--main-jar", fatJar,
                    "--main-class", "com.cashier.CashierSystemFXApplication",
                    "--java-options", "-Xms" + memoryMinSpinner.getValue() + "m",
                    "--java-options", "-Xmx" + memoryMaxSpinner.getValue() + "m",
                    "--java-options", "-Dfile.encoding=UTF-8"
            );
        } else {
            jpackage = new ProcessBuilder(
                    "jpackage",
                    "--type", "exe",
                    "--name", appNameField.getText().trim(),
                    "--app-version", version,
                    "--vendor", vendorField.getText().trim(),
                    "--description", descriptionArea.getText().trim(),
                    "--dest", outputDirField.getText().trim(),
                    "--input", "target",
                    "--main-jar", fatJar,
                    "--main-class", "com.cashier.CashierSystemFXApplication",
                    "--java-options", "-Xms" + memoryMinSpinner.getValue() + "m",
                    "--java-options", "-Xmx" + memoryMaxSpinner.getValue() + "m",
                    "--java-options", "-Dfile.encoding=UTF-8"
            );
        }

        // 添加可选功能
        if (addMenuCheckBox.isSelected()) {
            jpackage.command().add("--win-menu");
            jpackage.command().add("--win-menu-group=" + appNameField.getText().trim());
        }
        if (createShortcutCheckBox.isSelected()) {
            jpackage.command().add("--win-shortcut");
        }
        if (dirChooserCheckBox.isSelected()) {
            jpackage.command().add("--win-dir-chooser");
        }

        // 添加图标
        String iconPath = iconPathField.getText().trim();
        if (!iconPath.isEmpty() && new File(iconPath).exists()) {
            jpackage.command().add("--icon=" + iconPath);
        } else {
            jpackage.command().add("--icon=src/main/resources/images/logos/app-icon.ico");
        }

        jpackage.command().add("--win-per-user-install");
        jpackage.command().add("false");

        jpackage.redirectErrorStream(true);
        Process jpackageProcess = jpackage.start();
        logProcessOutput(jpackageProcess);

        if (jpackageProcess.waitFor() != 0) {
            throw new RuntimeException("jpackage 打包失败");
        }
    }

    private void logProcessOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final String output = line;
                javafx.application.Platform.runLater(() -> appendLog(output));
            }
        }
    }

    private void deleteDirectory(File dir) throws IOException {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        Files.deleteIfExists(dir.toPath());
    }

    private void appendLog(String message) {
        logTextArea.appendText(message + "\n");
    }

    private void disableAllControls(boolean disable) {
        wizardContainer.setDisable(disable);
        backButton.setDisable(disable || currentStep == 1);
        nextButton.setDisable(disable || currentStep == TOTAL_STEPS);
        finishButton.setDisable(disable);
        cancelButton.setDisable(disable);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
