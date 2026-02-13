package com.cashier.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PasswordUtil 单元测试
 */
@DisplayName("密码加密工具类测试")
public class PasswordUtilTest {

    @Test
    @DisplayName("测试密码哈希功能")
    public void testHashPassword() {
        String plainPassword = "testPassword123";
        String hashedPassword = PasswordUtil.hashPassword(plainPassword);

        // 验证哈希不为空
        assertNotNull(hashedPassword);
        // 验证哈希以 $2 开头（BCrypt 格式）
        assertTrue(hashedPassword.startsWith("$2"));
        // 验证哈希长度足够
        assertTrue(hashedPassword.length() >= 60);
    }

    @Test
    @DisplayName("测试密码验证功能 - 正确密码")
    public void testVerifyPasswordCorrect() {
        String plainPassword = "correctPassword123";
        String hashedPassword = PasswordUtil.hashPassword(plainPassword);

        assertTrue(PasswordUtil.verifyPassword(plainPassword, hashedPassword));
    }

    @Test
    @DisplayName("测试密码验证功能 - 错误密码")
    public void testVerifyPasswordIncorrect() {
        String plainPassword = "correctPassword123";
        String wrongPassword = "wrongPassword456";
        String hashedPassword = PasswordUtil.hashPassword(plainPassword);

        assertFalse(PasswordUtil.verifyPassword(wrongPassword, hashedPassword));
    }

    @Test
    @DisplayName("测试密码验证功能 - 相同密码不同哈希")
    public void testSamePasswordDifferentHash() {
        String plainPassword = "testPassword123";
        String hash1 = PasswordUtil.hashPassword(plainPassword);
        String hash2 = PasswordUtil.hashPassword(plainPassword);

        // BCrypt 使用随机盐，相同的密码会产生不同的哈希值
        assertNotEquals(hash1, hash2);
        // 但两个哈希都应该能验证相同的密码
        assertTrue(PasswordUtil.verifyPassword(plainPassword, hash1));
        assertTrue(PasswordUtil.verifyPassword(plainPassword, hash2));
    }

    @Test
    @DisplayName("测试空密码处理")
    public void testEmptyPassword() {
        String emptyPassword = "";
        String hashedPassword = PasswordUtil.hashPassword(emptyPassword);

        assertNotNull(hashedPassword);
        assertTrue(PasswordUtil.verifyPassword(emptyPassword, hashedPassword));
    }

    @Test
    @DisplayName("测试特殊字符密码")
    public void testSpecialCharactersPassword() {
        String specialPassword = "p@ssw0rd!#$%^&*()_+-=[]{}|;':\",./<>?";
        String hashedPassword = PasswordUtil.hashPassword(specialPassword);

        assertTrue(PasswordUtil.verifyPassword(specialPassword, hashedPassword));
    }

    @Test
    @DisplayName("测试中文字符密码")
    public void testChineseCharactersPassword() {
        String chinesePassword = "密码123测试";
        String hashedPassword = PasswordUtil.hashPassword(chinesePassword);

        assertTrue(PasswordUtil.verifyPassword(chinesePassword, hashedPassword));
    }

    @Test
    @DisplayName("测试 isHashed 方法")
    public void testIsHashed() {
        String plainPassword = "testPassword";
        String hashedPassword = PasswordUtil.hashPassword(plainPassword);

        assertFalse(PasswordUtil.isHashed(plainPassword));
        assertTrue(PasswordUtil.isHashed(hashedPassword));
    }

    @Test
    @DisplayName("测试向后兼容 - 非加密密码直接比较")
    public void testBackwardCompatibility() {
        String plainPassword = "plainPassword123";
        String nonHashedPassword = "plainPassword123";

        // 非加密格式的密码应该能直接比较
        assertTrue(PasswordUtil.verifyPassword(plainPassword, nonHashedPassword));
    }

    @Test
    @DisplayName("测试长密码")
    public void testLongPassword() {
        // BCrypt 支持最长72字节的密码（注意是字节不是字符）
        String longPassword = "a".repeat(60) + "123"; // 63字符，63字节
        String hashedPassword = PasswordUtil.hashPassword(longPassword);

        assertTrue(PasswordUtil.verifyPassword(longPassword, hashedPassword));
    }
}