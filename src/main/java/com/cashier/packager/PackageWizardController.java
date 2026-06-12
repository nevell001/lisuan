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
    @FXML private CheckBox createExeInstallerCheckBox;
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

    /**
     * 查找 Maven 命令
     * 优先使用 PATH 中的 mvn.cmd (Windows) 或 mvn (Linux/Mac)
     */
    private String findMavenCommand() {
        String osName = System.getProperty("os.name").toLowerCase();
        String mavenCmd = osName.contains("win") ? "mvn.cmd" : "mvn";

        // 首先检查 PATH 中的 Maven
        try {
            ProcessBuilder pb = new ProcessBuilder(mavenCmd, "--version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            if (p.waitFor() == 0) {
                return mavenCmd;
            }
        } catch (Exception e) {
            // 继续尝试其他方法
        }

        // 检查常见 Maven 安装路径
        String[] mavenPaths = {
            System.getenv("MAVEN_HOME") + "\\bin\\mvn.cmd",
            System.getenv("MAVEN_HOME") + "/bin/mvn",
            "C:\\Program Files\\Apache Maven\\bin\\mvn.cmd",
            "C:\\Maven\\bin\\mvn.cmd",
            System.getProperty("user.home") + "\\.m2\\wrapper\\dists\\*\\*\\bin\\mvn.cmd"
        };

        for (String path : mavenPaths) {
            if (path != null && !path.contains("*")) {
                File f = new File(path);
                if (f.exists()) {
                    return path;
                }
            }
        }

        // 如果都找不到，返回默认命令名
        return mavenCmd;
    }

    /**
     * 查找 jpackage 命令
     * jpackage 通常在 JDK bin 目录中
     */
    private String findJpackageCommand() {
        // 首先检查 PATH 中的 jpackage
        try {
            ProcessBuilder pb = new ProcessBuilder("jpackage", "--version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            if (p.waitFor() == 0) {
                return "jpackage";
            }
        } catch (Exception e) {
            // 继续尝试其他方法
        }

        // 检查 JAVA_HOME 下的 bin 目录
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            String[] jpackagePaths = {
                javaHome + "\\bin\\jpackage.exe",
                javaHome + "/bin/jpackage",
                javaHome + "\\bin\\jpackage.cmd"
            };
            for (String path : jpackagePaths) {
                File f = new File(path);
                if (f.exists()) {
                    return path;
                }
            }
        }

        // 检查常见 JDK 安装路径
        String[] jdkPaths = {
            "C:\\Program Files\\Java\\jdk-*\\bin\\jpackage.exe",
            "C:\\Program Files\\Java\\jdk-*\\bin\\jpackage",
            "C:\\Java\\jdk-*\\bin\\jpackage.exe",
            System.getProperty("user.home") + "\\scoop\\apps\\openjdk*\\current\\bin\\jpackage.exe"
        };

        for (String path : jdkPaths) {
            if (path.contains("*")) {
                // 处理通配符
                File parentDir = new File(path).getParentFile();
                if (parentDir != null && parentDir.exists()) {
                    File[] matches = parentDir.listFiles((dir, name) ->
                        name.startsWith("jpackage") || name.startsWith("jpackage.exe"));
                    if (matches != null && matches.length > 0) {
                        return matches[0].getAbsolutePath();
                    }
                }
            } else {
                File f = new File(path);
                if (f.exists()) {
                    return path;
                }
            }
        }

        // 返回默认命令名
        return "jpackage";
    }

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

        // 默认图标路径
        String defaultIconPath = System.getProperty("user.dir") + "\\src\\main\\resources\\images\\logos\\app-icon.ico";
        File iconFile = new File(defaultIconPath);
        if (iconFile.exists()) {
            iconPathField.setText(defaultIconPath);
        }

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
                    String appName = appNameField.getText().trim();
                    String mavenCmd = findMavenCommand();
                    String version = appVersionField.getText().trim();
                    String fatJar = "target/" + "lisuan-fx-" + version + "-jar-with-dependencies.jar";
                    File jarFile = new File(fatJar);

                    // 检查 JAR 是否已存在，如果存在则跳过编译
                    if (jarFile.exists()) {
                        updateProgress(0.5, 1.0);
                        updateMessage("发现已编译的 JAR，跳过编译步骤...");
                        appendLog("发现已存在的 JAR 文件，跳过 Maven 编译\n");
                    } else {
                        // 步骤 1: 编译项目
                        updateProgress(0.1, 1.0);
                        updateMessage("正在编译项目...");
                        appendLog("[1/3] 正在编译项目...");

                        appendLog("使用 Maven: " + mavenCmd + "\n");

                        ProcessBuilder mvnCompile = new ProcessBuilder(mavenCmd, "compile", "-q");
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
                        appendLog("[2/3] 正在打包 JAR...");

                        ProcessBuilder mvnPackage = new ProcessBuilder(mavenCmd, "package", "-DskipTests", "-q");
                        mvnPackage.directory(new File(System.getProperty("user.dir")));
                        mvnPackage.redirectErrorStream(true);

                        Process packageProcess = mvnPackage.start();
                        logProcessOutput(packageProcess);

                        if (packageProcess.waitFor() != 0) {
                            throw new RuntimeException("打包失败");
                        }
                        appendLog("✓ JAR 打包完成\n");
                    }

                    // 步骤 3: 保存数据库配置
                    updateProgress(0.6, 1.0);
                    updateMessage("正在保存数据库配置...");
                    appendLog("[3/4] 正在保存数据库配置...");

                    saveDatabaseConfig();
                    appendLog("✓ 数据库配置已保存\n");

                    // 步骤 4: 创建分发包
                    updateProgress(0.8, 1.0);
                    updateMessage("正在创建分发包...");
                    appendLog("[4/4] 正在创建分发包...");

                    createExePackage();
                    appendLog("✓ 分发包创建完成\n");

                    updateProgress(1.0, 1.0);
                    appendLog("========================================");
                    appendLog("打包完成！");
                    appendLog("输出位置: " + outputDirField.getText());
                    appendLog("启动方式: 双击 " + appName + ".bat");
                    appendLog("========================================");

                    // 打开输出目录
                    try {
                        Runtime.getRuntime().exec("explorer " + outputDirField.getText());
                        appendLog("\n已打开输出目录");
                    } catch (IOException e) {
                        logger.error("无法打开输出目录", e);
                    }

                    javafx.application.Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION, "打包成功",
                                "分发包已创建！\n\n" +
                                "输出位置: " + outputDirField.getText() + "\\" + appName + "\n\n" +
                                "启动方式: 双击 " + appName + ".bat");
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
        String appName = appNameField.getText().trim();
        File outputDir = new File(outputDirField.getText().trim());

        appendLog("使用 PowerShell 打包方案\n");
        logger.info("Using PowerShell packaging");

        // 检查 package-simple.ps1 是否存在
        File ps1Script = new File("package-simple.ps1");
        if (!ps1Script.exists()) {
            String error = "找不到打包脚本: " + ps1Script.getAbsolutePath();
            appendLog(error + "\n");
            logger.error(error);
            throw new RuntimeException(error);
        }

        // 检查输出目录
        if (!outputDir.exists()) {
            outputDir.mkdirs();
            appendLog("创建输出目录: " + outputDir.getAbsolutePath() + "\n");
        }

        // 清理已存在的应用目录
        File existingAppDir = new File(outputDir, appName);
        if (existingAppDir.exists()) {
            appendLog("清理已存在的输出目录: " + existingAppDir.getAbsolutePath() + "\n");
            deleteDirectory(existingAppDir);
        }

        appendLog("启动 PowerShell 打包脚本...\n");

        // 构建 PowerShell 命令
        ProcessBuilder psBuilder = new ProcessBuilder(
            "powershell",
            "-ExecutionPolicy", "Bypass",
            "-File", ps1Script.getAbsolutePath()
        );

        // 设置环境变量，告诉脚本不要等待用户输入
        psBuilder.environment().put("FROM_JAVA", "1");

        psBuilder.directory(new File(System.getProperty("user.dir")));
        psBuilder.redirectErrorStream(true);

        appendLog("执行命令: powershell -ExecutionPolicy Bypass -File " + ps1Script.getName() + "\n");

        Process psProcess = psBuilder.start();

        // 捕获输出
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(psProcess.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                final String outputLine = line;
                javafx.application.Platform.runLater(() -> appendLog(outputLine));
            }
        }

        int exitCode = psProcess.waitFor();
        logger.info("PowerShell exit code: {}", exitCode);
        logger.info("PowerShell output:\n{}", output);

        if (exitCode != 0) {
            String error = "PowerShell 打包失败 (退出码: " + exitCode + ")";
            appendLog(error + "\n");
            if (!output.isEmpty()) {
                appendLog("错误输出:\n" + output.toString() + "\n");
            }
            throw new RuntimeException(error);
        }

        appendLog("✓ 打包完成\n");
        appendLog("输出位置: " + existingAppDir.getAbsolutePath() + "\n");
        appendLog("启动方式: 双击 " + appName + ".bat\n");
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
