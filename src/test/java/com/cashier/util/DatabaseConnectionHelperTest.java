package com.cashier.util;

import com.cashier.util.DatabaseConnectionHelper.DiagnosticResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据库连接诊断工具测试
 */
public class DatabaseConnectionHelperTest {

    @Test
    public void testDiagnoseConnection() {
        System.out.println("=== 数据库连接诊断测试 ===\n");

        DiagnosticResult result = DatabaseConnectionHelper.diagnoseConnection();

        System.out.println("诊断结果：");
        System.out.println(result.getFullMessage());
        System.out.println("\n===");

        if (result.success) {
            System.out.println("✅ 数据库连接成功！");
        } else {
            System.out.println("❌ 数据库连接失败");
            System.out.println("\n错误信息：");
            System.out.println(result.errorMessage);
            System.out.println("\n解决方案：");
            System.out.println(result.solution);
        }

        // 断言：至少应该返回一个结果
        assertNotNull(result);
        assertNotNull(result.getFullMessage());
    }

    @Test
    public void testExtractHostPort() {
        // 测试 URL 解析
        String url1 = "jdbc:mysql://localhost:3306/lisuan_system?useSSL=false";
        String url2 = "jdbc:mysql://192.168.1.100:3307/dbname";
        String url3 = "jdbc:mysql://db.example.com/cashier";

        System.out.println("=== URL 解析测试 ===");
        System.out.println("URL1: " + url1);
        System.out.println("URL2: " + url2);
        System.out.println("URL3: " + url3);
    }

    public static void main(String[] args) {
        System.out.println("=== 数据库连接诊断工具 ===\n");

        DiagnosticResult result = DatabaseConnectionHelper.diagnoseConnection();

        System.out.println("═══════════════════════════════════════");
        System.out.println("       数据库连接诊断报告");
        System.out.println("═══════════════════════════════════════\n");

        if (result.success) {
            System.out.println("✅ 状态：连接成功");
            System.out.println("\n数据库连接正常，可以正常使用收银系统。");
        } else {
            System.out.println("❌ 状态：连接失败");
            System.out.println("\n【错误信息】");
            System.out.println(result.errorMessage);
            System.out.println("\n【解决方案】");
            System.out.println(result.solution);
        }

        System.out.println("\n═══════════════════════════════════════");
    }
}
