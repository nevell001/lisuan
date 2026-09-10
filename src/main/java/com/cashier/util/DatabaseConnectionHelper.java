package com.cashier.util;

import com.cashier.constant.DatabaseConfigKeys;

import com.cashier.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.nio.charset.StandardCharsets;

/**
 * 数据库连接诊断工具
 * 提供友好的数据库连接错误诊断和解决方案提示
 */
public class DatabaseConnectionHelper {

    /**
     * 数据库连接诊断结果
     */
    public static class DiagnosticResult {
        public final boolean success;
        public final String errorMessage;
        public final String solution;

        public DiagnosticResult(boolean success, String errorMessage, String solution) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.solution = solution;
        }

        public static DiagnosticResult ok() {
            return new DiagnosticResult(true, null, null);
        }

        public static DiagnosticResult failure(String errorMessage, String solution) {
            return new DiagnosticResult(false, errorMessage, solution);
        }

        public String getFullMessage() {
            if (success) {
                return "数据库连接成功！";
            }
            return "错误：" + errorMessage + "\n\n解决方案：\n" + solution;
        }
    }

    /**
     * 诊断数据库连接问题
     * @return 诊断结果
     */
    public static DiagnosticResult diagnoseConnection() {
        // 检查配置文件是否存在
        java.io.File configFile = new java.io.File("config/database.properties");
        if (!configFile.exists()) {
            return DiagnosticResult.failure(
                "数据库配置文件不存在",
                """
                请按照以下步骤配置数据库：
                1. 复制 config/database.properties.example 为 config/database.properties
                2. 编辑 database.properties，设置正确的数据库连接信息
                3. 确保 MySQL 服务正在运行

                详细安装指南请参考：docs/WINDOWS_MYSQL_SETUP.md
                """
            );
        }

        DbConnectionConfig config;
        try {
            config = loadConnectionConfig(configFile);
        } catch (Exception e) {
            return DiagnosticResult.failure(
                "读取配置文件失败：" + e.getMessage(),
                """
                请检查 config/database.properties 文件格式是否正确
                确保文件使用 UTF-8 编码
                """
            );
        }

        DiagnosticResult validationResult = validateConnectionConfig(config);
        if (!validationResult.success) {
            return validationResult;
        }

        // 尝试连接数据库
        try {
            ensureMysqlDriverAvailable();

            // 测试基本查询
            try (Connection testConn = DriverManager.getConnection(config.url(), config.username(), config.password());
                 java.sql.Statement stmt = testConn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery("SELECT 1")) {
                if (rs.next()) {
                    return DiagnosticResult.ok();
                }
            }

            return DiagnosticResult.ok();

        } catch (ClassNotFoundException e) {
            return DiagnosticResult.failure(
                "MySQL JDBC 驱动未找到",
                "请确保项目中包含 MySQL JDBC 驱动依赖\n" +
                "如果使用 Maven，请检查 pom.xml 中是否有 mysql-connector-j 依赖"
            );
        } catch (SQLException e) {
            return analyzeSQLException(e, config.url(), config.username());
        }
    }

    private static DbConnectionConfig loadConnectionConfig(java.io.File configFile) throws java.io.IOException {
        Properties props = new Properties();
           try (java.io.FileInputStream fis = new java.io.FileInputStream(configFile);
               java.io.InputStreamReader isr = new java.io.InputStreamReader(fis, StandardCharsets.UTF_8)) {
            props.load(isr);
        }

        // L-2: 优先使用 CASHIER_DB_PASSWORD，向后兼容旧拼写 CASHER_DB_PASSWORD；
        // 两个变量都是"环境变量 → .env"的取值顺序
        String password = DotEnv.get(DotEnv.DB_PASSWORD_KEY);
        if (password == null || password.isEmpty()) {
            password = DotEnv.get("CASHER_DB_PASSWORD");
        }
        if (password == null || password.isEmpty()) {
            password = props.getProperty(DatabaseConfigKeys.PASSWORD);
        }
        return new DbConnectionConfig(props.getProperty(DatabaseConfigKeys.URL), props.getProperty(DatabaseConfigKeys.USERNAME), password);
    }

    private static DiagnosticResult validateConnectionConfig(DbConnectionConfig config) {
        if (config.url() == null || config.url().isEmpty()) {
            return DiagnosticResult.failure(
                "数据库 URL 未配置",
                "请在 config/database.properties 中设置 db.url 参数\n" +
                "示例：jdbc:mysql://localhost:3306/lisuan_system?sslMode=PREFERRED&serverTimezone=Asia/Shanghai"
            );
        }
        if (config.username() == null || config.username().isEmpty()) {
            return DiagnosticResult.failure(
                "数据库用户名未配置",
                "请在 config/database.properties 中设置 db.username 参数\n" +
                "示例：root 或 lisuan"
            );
        }
        if (config.password() == null || config.password().isEmpty()) {
            return DiagnosticResult.failure(
                "数据库密码未配置",
                "请在 config/database.properties 中设置 db.password 参数\n" +
                "或者设置环境变量 CASHIER_DB_PASSWORD 来存储密码（更安全）\n" +
                "Windows: set CASHIER_DB_PASSWORD=YourPassword\n" +
                "Linux/Mac: export CASHIER_DB_PASSWORD=YourPassword"
            );
        }
        return DiagnosticResult.ok();
    }

    private static void ensureMysqlDriverAvailable() throws ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
    }

    private record DbConnectionConfig(String url, String username, String password) {
    }

    /**
     * 分析 SQL 异常并提供友好的错误信息和解决方案
     */
    private static DiagnosticResult analyzeSQLException(SQLException e, String dbUrl, String dbUsername) {
        String errorMessage = e.getMessage();
        int errorCode = e.getErrorCode();

        // Communications link failure - 无法连接到 MySQL 服务器
        if (errorMessage != null && errorMessage.contains("Communications link failure")) {
            return DiagnosticResult.failure(
                "无法连接到 MySQL 服务器",
                "请检查以下项目：\n"
                + "1. MySQL 服务是否正在运行\n"
                + "   - Windows: 在服务中查找 MySQL80 服务\n"
                + "   - 或使用命令：net start MySQL80\n"
                + "2. 主机名和端口是否正确\n"
                + "   - 当前配置：" + extractHostPort(dbUrl) + "\n"
                + "3. 防火墙是否阻止了连接\n"
                + "4. 如果使用 Docker，确保容器正在运行：docker ps"
            );
        }

        // Access denied - 用户名或密码错误
        if (errorMessage != null && errorMessage.contains("Access denied")) {
            return DiagnosticResult.failure(
                "数据库认证失败：用户名或密码错误",
                "请检查以下项目：\n"
                + "1. 用户名是否正确（请检查 config/database.properties 中的配置）\n"
                + "2. 密码是否正确\n"
                + "3. 用户是否有访问 lisuan_system 数据库的权限\n\n"
                + "如果忘记密码，可以重置：\n"
                + "mysql -u root -p\n"
                + "ALTER USER '你的用户名'@'%' IDENTIFIED BY '新密码';\n"
                + "GRANT ALL PRIVILEGES ON lisuan_system.* TO '你的用户名'@'%';\n"
                + "FLUSH PRIVILEGES;"
            );
        }

        // Unknown database - 数据库不存在
        if (errorMessage != null && errorMessage.contains("Unknown database")) {
            return DiagnosticResult.failure(
                "数据库不存在：lisuan_system",
                "请创建数据库：\n"
                + "mysql -u root -p\n"
                + "CREATE DATABASE lisuan_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\n"
                + "GRANT ALL PRIVILEGES ON lisuan_system.* TO '你的用户名'@'localhost';\n"
                + "FLUSH PRIVILEGES;"
            );
        }

        // Server connection failure
        if (errorMessage != null && errorMessage.contains("Could not create connection to database server")) {
            return DiagnosticResult.failure(
                "无法创建数据库连接",
                """
                可能的原因：
                1. MySQL 服务未启动
                2. 主机名或端口配置错误
                3. 网络连接问题
                4. MySQL 最大连接数已达到限制

                建议：检查 MySQL 服务状态和配置
                """
            );
        }

        // 通用错误
        return DiagnosticResult.failure(
            "数据库连接失败：" + errorMessage,
            "错误代码：" + errorCode + "\n"
            + "SQL 状态：" + e.getSQLState() + "\n\n"
            + "请检查：\n"
            + "1. MySQL 服务是否运行\n"
            + "2. 配置文件是否正确\n"
            + "3. 数据库用户权限\n\n"
            + "详细错误信息请查看日志文件：logs/app.log"
        );
    }

    /**
     * 从 JDBC URL 提取主机名和端口
     */
    private static String extractHostPort(String url) {
        try {
            // jdbc:mysql://localhost:3306/dbname
            int start = url.indexOf("://") + 3;
            int slash = url.indexOf("/", start);
            if (start > 0 && slash > start) {
                return url.substring(start, slash);
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return "未知";
    }

    /**
     * 测试数据库连接
     * @return 如果连接成功返回 true，否则返回 false
     */
    public static boolean testConnection() {
        return diagnoseConnection().success;
    }

    /**
     * 获取友好的错误消息
     * @param e 异常
     * @return 友好的错误消息
     */
    public static String getFriendlyErrorMessage(Throwable e) {
        if (e instanceof DatabaseException de) {
            switch (de.getDbErrorType()) {
                case CONNECTION_FAILED:
                    return "数据库连接失败\n\n" +
                           "请检查：\n" +
                           "1. MySQL 服务是否正在运行\n" +
                           "2. config/database.properties 配置是否正确\n" +
                           "3. 数据库用户名和密码是否正确\n\n" +
                           "详细诊断：请使用工具中的'测试连接'功能";
                default:
                    return e.getMessage();
            }
        }
        return e.getMessage();
    }
}
