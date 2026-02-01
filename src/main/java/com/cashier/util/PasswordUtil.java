package com.cashier.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * 密码加密工具类
 * 使用 BCrypt 算法进行密码加密
 */
public class PasswordUtil {

    /**
     * 加密密码
     * @param plainPassword 明文密码
     * @return 加密后的密码
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.withDefaults().hashToString(12, plainPassword.toCharArray());
    }

    /**
     * 验证密码
     * @param plainPassword 明文密码
     * @param hashedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        try {
            // 检查是否是 BCrypt 加密的密码（以 $2a$、$2b$、$2y$ 开头）
            if (hashedPassword != null && hashedPassword.startsWith("$2")) {
                return BCrypt.verifyer().verify(plainPassword.toCharArray(), hashedPassword).verified;
            }
            // 如果不是加密密码，直接比较（向后兼容旧数据）
            return plainPassword.equals(hashedPassword);
        } catch (Exception e) {
            // 如果验证失败，尝试直接比较（向后兼容）
            return plainPassword.equals(hashedPassword);
        }
    }

    /**
     * 检查密码是否是已加密的格式
     * @param password 密码
     * @return 是否是加密格式
     */
    public static boolean isHashed(String password) {
        return password != null && password.startsWith("$2");
    }
}
