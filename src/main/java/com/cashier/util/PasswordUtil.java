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
     * @throws IllegalArgumentException 如果密码格式不正确
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (hashedPassword == null || !hashedPassword.startsWith("$2")) {
            throw new IllegalArgumentException("密码格式不正确，必须使用BCrypt加密");
        }
        try {
            return BCrypt.verifyer().verify(plainPassword.toCharArray(), hashedPassword).verified;
        } catch (Exception e) {
            throw new IllegalArgumentException("密码验证失败", e);
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
