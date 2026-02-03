package com.cashier;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestMySQLConnection {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/cashier_system?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
        String username = "cashier";
        String password = "YourStrongPassword123!";

        try {
            System.out.println("正在连接到 MySQL...");
            Connection conn = DriverManager.getConnection(url, username, password);
            System.out.println("✓ MySQL 连接成功！");

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT USER() as user, DATABASE() as db, VERSION() as version");
            if (rs.next()) {
                System.out.println("  当前用户: " + rs.getString("user"));
                System.out.println("  当前数据库: " + rs.getString("db"));
                System.out.println("  MySQL 版本: " + rs.getString("version"));
            }

            rs.close();
            stmt.close();
            conn.close();

            System.out.println("\n✓ 测试完成！");

        } catch (Exception e) {
            System.err.println("✗ 连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
