package com.cashier.util;

import com.cashier.exception.DatabaseException;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 数据库连接诊断工具
 * 提供友好的数据库连接错误诊断和解决方案提示
 */
public class DatabaseConnectionHelper {

    private static final Logger logger = LoggerFactoryUtil.getLogger(DatabaseConnectionHelper.class);

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

        public static DiagnosticResult success() {
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
                "请按照以下步骤配置数据库：\n" +
                "1. 复制 config/database.properties.example 为 config/database.properties\n" +
                "2. 编辑 database.properties，设置正确的数据库连接信息\n" +
                "3. 确保 MySQL 服务正在运行\n\n" +
                "详细安装指南请参考：docs/WINDOWS_MYSQL_SETUP.md"
            );
        }

        // 加载配置
        Properties props = new Properties();
        String dbUrl = null;
        String dbUsername = null;
        String dbPassword = null;

        try (java.io.FileInputStream fis = new java.io.FileInputStream(configFile);
             java.io.InputStreamReader isr = new java.io.InputStreamReader(fis, "UTF-8")) {
            props.load(isr);
            dbUrl = props.getProperty("db.url");
            dbUsername = props.getProperty("db.username");

            // 优先从环境变量读取密码
            String envPassword = System.getenv("LISUAN_DB_PASSWORD");
            if (envPassword != null && !envPassword.isEmpty()) {
                dbPassword = envPassword;
            } else {
                dbPassword = props.getProperty("db.password");
            }
        } catch (Exception e) {
            return DiagnosticResult.failure(
                "读取配置文件失败：" + e.getMessage(),
                "请检查 config/database.properties 文件格式是否正确\n" +
                "确保文件使用 UTF-8 编码"
            );
        }

        // 验证配置完整性
        if (dbUrl == null || dbUrl.isEmpty()) {
            return DiagnosticResult.failure(
                "数据库 URL 未配置",
                "请在 config/database.properties 中设置 db.url 参数\n" +
                "示例：jdbc:mysql://localhost:3306/lisuan_system?useSSL=false&serverTimezone=Asia/Shanghai"
            );
        }
        if (dbUsername == null || dbUsername.isEmpty()) {
            return DiagnosticResult.failure(
                "数据库用户名未配置",
                "请在 config/database.properties 中设置 db.username 参数\n" +
                "示例：root 或 lisuan"
            );
        }
        if (dbPassword == null || dbPassword.isEmpty()) {
            return DiagnosticResult.failure(
                "数据库密码未配置",
                "请在 config/database.properties 中设置 db.password 参数\n" +
                "或者设置环境变量 LISUAN_DB_PASSWORD 来存储密码（更安全）\n" +
                "Windows: set LISUAN_DB_PASSWORD=YourPassword\n" +
                "Linux/Mac: export LISUAN_DB_PASSWORD=YourPassword"
            );
        }

        // 尝试连接数据库
        Connection testConn = null;
        try {
            // 尝试获取 JDBC 驱动
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                return DiagnosticResult.failure(
                    "MySQL JDBC 驱动未找到",
                    "请确保项目中包含 MySQL JDBC 驱动依赖\n" +
                    "如果使用 Maven，请检查 pom.xml 中是否有 mysql-connector-j 依赖"
                );
            }

            // 尝试建立连接
            testConn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);

            // 测试基本查询
            try (java.sql.Statement stmt = testConn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery("SELECT 1")) {
                if (rs.next()) {
                    return DiagnosticResult.success();
                }
            }

            return DiagnosticResult.success();

        } catch (SQLException e) {
            return analyzeSQLException(e, dbUrl, dbUsername);
        } finally {
            if (testConn != null) {
                try {
                    testConn.close();
                } catch (SQLException ex) {
                    // 忽略关闭错误
                }
            }
        }
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
                "请检查以下项目：\n" +
                "1. MySQL 服务是否正在运行\n" +
                "   - Windows: 在服务中查找 MySQL80 服务\n" +
                "   - 或使用命令：net start MySQL80\n" +
                "2. 主机名和端口是否正确\n" +
                "   - 当前配置：" + extractHostPort(dbUrl) + "\n" +
                "3. 防火墙是否阻止了连接\n" +
                "4. 如果使用 Docker，确保容器正在运行：docker ps"
            );
        }

        // Access denied - 用户名或密码错误
        if (errorMessage != null && errorMessage.contains("Access denied")) {
            return DiagnosticResult.failure(
                "数据库认证失败：用户名或密码错误",
                "请检查以下项目：\n" +
                "1. 用户名是否正确：当前配置为 " + dbUsername + "\n" +
                "2. 密码是否正确\n" +
                "3. 用户是否有访问 lisuan_system 数据库的权限\n\n" +
                "如果忘记密码，可以重置：\n" +
                "mysql -u root -p\n" +
                "ALTER USER '" + dbUsername + "'@'localhost' IDENTIFIED BY '新密码';"
            );
        }

        // Unknown database - 数据库不存在
        if (errorMessage != null && errorMessage.contains("Unknown database")) {
            return DiagnosticResult.failure(
                "数据库不存在：lisuan_system",
                "请创建数据库：\n" +
                "mysql -u root -p\n" +
                "CREATE DATABASE lisuan_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\n" +
                "GRANT ALL PRIVILEGES ON lisuan_system.* TO '" + dbUsername + "'@'localhost';\n" +
                "FLUSH PRIVILEGES;"
            );
        }

        // Server connection failure
        if (errorMessage != null && errorMessage.contains("Could not create connection to database server")) {
            return DiagnosticResult.failure(
                "无法创建数据库连接",
                "可能的原因：\n" +
                "1. MySQL 服务未启动\n" +
                "2. 主机名或端口配置错误\n" +
                "3. 网络连接问题\n" +
                "4. MySQL 最大连接数已达到限制\n\n" +
                "建议：检查 MySQL 服务状态和配置"
            );
        }

        // 通用错误
        return DiagnosticResult.failure(
            "数据库连接失败：" + errorMessage,
            "错误代码：" + errorCode + "\n" +
            "SQL 状态：" + e.getSQLState() + "\n\n" +
            "请检查：\n" +
            "1. MySQL 服务是否运行\n" +
            "2. 配置文件是否正确\n" +
            "3. 数据库用户权限\n\n" +
            "详细错误信息请查看日志文件：logs/app.log"
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
        if (e instanceof DatabaseException) {
            DatabaseException de = (DatabaseException) e;
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
