package com.cashier.service;

import com.cashier.dao.*;
import com.cashier.model.*;

import java.sql.SQLException;
import java.util.*;

/**
 * 数据服务
 * 提供与 DataManager 相同的接口，但使用 MySQL 数据库
 * 用于逐步从文件存储迁移到数据库存储
 */
public class DataService {

    /**
     * 加载库存数据
     */
    public static Map<String, Product> loadInventory() {
        try {
            List<Product> products = ProductDAO.findAll();
            Map<String, Product> inventory = new HashMap<>();
            for (Product product : products) {
                inventory.put(product.name, product);
            }
            return inventory;
        } catch (SQLException e) {
            System.err.println("加载商品数据失败: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    /**
     * 保存库存数据
     */
    public static void saveInventory(Map<String, Product> inventory) {
        try {
            // 批量删除所有商品
            List<Product> existing = ProductDAO.findAll();
            for (Product p : existing) {
                ProductDAO.delete(p.name);
            }

            // 批量插入新商品
            List<Product> products = new ArrayList<>(inventory.values());
            ProductDAO.batchInsert(products);
        } catch (SQLException e) {
            System.err.println("保存商品数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 加载用户数据
     */
    public static Map<String, User> loadUsers() {
        try {
            List<User> users = UserDAO.findAll();
            Map<String, User> userMap = new HashMap<>();
            for (User user : users) {
                userMap.put(user.username, user);
            }
            return userMap;
        } catch (SQLException e) {
            System.err.println("加载用户数据失败: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    /**
     * 保存用户数据
     */
    public static void saveUsers(Map<String, User> users) {
        try {
            List<User> userList = new ArrayList<>(users.values());
            UserDAO.batchInsert(userList);
        } catch (SQLException e) {
            System.err.println("保存用户数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 加载会员数据
     */
    public static Map<String, Member> loadMembers() {
        try {
            List<Member> members = MemberDAO.findAll();
            Map<String, Member> memberMap = new HashMap<>();
            for (Member member : members) {
                memberMap.put(member.phone, member);
            }
            return memberMap;
        } catch (SQLException e) {
            System.err.println("加载会员数据失败: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    /**
     * 保存会员数据
     */
    public static void saveMembers(Map<String, Member> members) {
        try {
            List<Member> memberList = new ArrayList<>(members.values());
            MemberDAO.batchInsert(memberList);
        } catch (SQLException e) {
            System.err.println("保存会员数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 加载交易数据
     */
    public static List<Transaction> loadTransactions() {
        try {
            return TransactionDAO.findAll();
        } catch (SQLException e) {
            System.err.println("加载交易数据失败: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 保存交易数据
     */
    public static void saveTransactions(List<Transaction> transactions) {
        try {
            TransactionDAO.batchInsert(transactions);
        } catch (SQLException e) {
            System.err.println("保存交易数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 加载设置数据（从文件，暂未迁移到数据库）
     */
    public static Map<String, String> loadSettings() {
        return com.cashier.model.DataManager.loadSettings();
    }

    /**
     * 保存设置数据（到文件，暂未迁移到数据库）
     */
    public static void saveSettings(double taxRate, int transactionCount) {
        com.cashier.model.DataManager.saveSettings(taxRate, transactionCount);
    }

    /**
     * 检查是否有活跃班次（从文件，暂未迁移）
     */
    public static boolean hasActiveShift() {
        return com.cashier.model.DataManager.hasActiveShift();
    }

    /**
     * 初始化（无操作，用于兼容 DataManager）
     */
    public static void initialize() {
        // 数据库已通过 DatabaseManager 初始化
    }
}
