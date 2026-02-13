package com.cashier.dao;

import com.cashier.model.User;
import com.cashier.util.DatabaseManager;
import com.cashier.util.DatabaseTestBase;
import com.cashier.util.PasswordUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserDAO 单元测试
 */
@DisplayName("用户数据访问对象测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserDAOTest extends DatabaseTestBase {

    private static User testUser;
    private static int insertedUserId;

    @BeforeAll
    @DisplayName("初始化测试环境")
    public static void setUpBeforeClass() throws SQLException {
        // 初始化测试数据库
        initTestDatabase();

        // 创建测试用户
        testUser = new User();
        testUser.username = "testuser_" + System.currentTimeMillis();
        testUser.password = PasswordUtil.hashPassword("testPassword123");
        testUser.name = "测试用户";
        testUser.role = "cashier";
        testUser.active = true;
        testUser.forcePasswordChange = false;
        testUser.createTime = new java.util.Date();
        testUser.lastLoginTime = new java.util.Date(0);
    }

    @BeforeEach
    @DisplayName("准备测试环境")
    public void setUp() {
        // 不需要清理数据，因为测试按顺序执行，依赖前面的测试结果
    }

    @Test
    @Order(1)
    @DisplayName("测试插入用户")
    public void testInsertUser() throws Exception {
        boolean result = UserDAO.insert(testUser);
        assertTrue(result);
        assertNotNull(testUser.id);
        assertTrue(testUser.id > 0);
        insertedUserId = testUser.id;
    }

    @Test
    @Order(2)
    @DisplayName("测试根据ID查找用户")
    public void testFindById() throws Exception {
        User found = UserDAO.findById(insertedUserId);
        assertNotNull(found);
        assertEquals(insertedUserId, found.id);
        assertEquals(testUser.username, found.username);
        assertEquals("测试用户", found.name);
    }

    @Test
    @Order(3)
    @DisplayName("测试根据用户名查找用户")
    public void testFindByUsername() throws Exception {
        User found = UserDAO.findByUsername(testUser.username);
        assertNotNull(found);
        assertEquals(testUser.username, found.username);
        assertEquals("测试用户", found.name);
        assertEquals("cashier", found.role);
    }

    @Test
    @Order(4)
    @DisplayName("测试查询所有用户")
    public void testFindAll() throws Exception {
        List<User> users = UserDAO.findAll();
        assertNotNull(users);
        assertTrue(users.size() > 0);

        // 验证测试用户在列表中
        boolean found = users.stream()
                .anyMatch(u -> u.username.equals(testUser.username));
        assertTrue(found);
    }

    @Test
    @Order(5)
    @DisplayName("测试更新用户")
    public void testUpdateUser() throws Exception {
        testUser.name = "更新后的测试用户";
        testUser.role = "finance";
        testUser.forcePasswordChange = true;

        boolean result = UserDAO.update(testUser);
        assertTrue(result);

        User updated = UserDAO.findById(insertedUserId);
        assertEquals("更新后的测试用户", updated.name);
        assertEquals("finance", updated.role);
        assertTrue(updated.forcePasswordChange);
    }

    @Test
    @Order(6)
    @DisplayName("测试更新最后登录时间")
    public void testUpdateLastLoginTime() throws Exception {
        long before = System.currentTimeMillis();
        UserDAO.updateLastLoginTimeByUsername(testUser.username);
        long after = System.currentTimeMillis();

        User updated = UserDAO.findByUsername(testUser.username);
        assertNotNull(updated.lastLoginTime);
        assertTrue(updated.lastLoginTime.getTime() >= before);
        assertTrue(updated.lastLoginTime.getTime() <= after);
    }

    @Test
    @Order(7)
    @DisplayName("测试验证用户密码")
    public void testVerifyPassword() throws Exception {
        User user = UserDAO.findByUsername(testUser.username);
        assertNotNull(user);

        boolean result = PasswordUtil.verifyPassword("testPassword123", user.password);
        assertTrue(result);

        // 错误密码
        result = PasswordUtil.verifyPassword("wrongPassword", user.password);
        assertFalse(result);
    }

    @Test
    @Order(8)
    @DisplayName("测试用户激活/停用")
    public void testSetActive() throws Exception {
        // 停用用户
        testUser.active = false;
        boolean result = UserDAO.update(testUser);
        assertTrue(result);

        User deactivated = UserDAO.findByUsername(testUser.username);
        assertFalse(deactivated.active);

        // 激活用户
        testUser.active = true;
        result = UserDAO.update(testUser);
        assertTrue(result);

        User activated = UserDAO.findByUsername(testUser.username);
        assertTrue(activated.active);
    }

    @Test
    @Order(10)
    @DisplayName("测试删除用户")
    public void testDeleteUser() throws Exception {
        boolean result = UserDAO.delete(insertedUserId);
        assertTrue(result);

        User deleted = UserDAO.findById(insertedUserId);
        assertNull(deleted);
    }

    @Test
    @DisplayName("测试查找不存在的用户")
    public void testFindNonExistentUser() throws Exception {
        User found = UserDAO.findById(999999);
        assertNull(found);

        found = UserDAO.findByUsername("nonexistent_user");
        assertNull(found);
    }

    @Test
    @DisplayName("测试批量插入用户")
    public void testBatchInsertUsers() throws Exception {
        List<User> users = List.of(
            createUser("batch1", "批量测试用户1", "admin"),
            createUser("batch2", "批量测试用户2", "cashier"),
            createUser("batch3", "批量测试用户3", "finance")
        );

        UserDAO.batchInsert(users);

        // 验证插入成功
        for (User u : users) {
            assertNotNull(u.id);
            assertTrue(u.id > 0);
        }

        // 清理测试数据
        for (User u : users) {
            UserDAO.delete(u.id);
        }
    }

    @Test
    @DisplayName("测试用户名唯一性")
    public void testUsernameUniqueness() throws Exception {
        User user1 = new User();
        user1.username = "duplicate_test";
        user1.password = PasswordUtil.hashPassword("password123");
        user1.name = "用户1";
        user1.role = "cashier";
        user1.active = true;
        user1.createTime = new java.util.Date();
        user1.lastLoginTime = new java.util.Date(0);

        UserDAO.insert(user1);

        User user2 = new User();
        user2.username = "duplicate_test"; // 相同的用户名
        user2.password = PasswordUtil.hashPassword("password456");
        user2.name = "用户2";
        user2.role = "cashier";
        user2.active = true;
        user2.createTime = new java.util.Date();
        user2.lastLoginTime = new java.util.Date(0);

        // 第二次插入应该失败（用户名重复）
        assertThrows(Exception.class, () -> UserDAO.insert(user2));

        // 清理
        UserDAO.delete(user1.id);
    }

    private User createUser(String username, String name, String role) {
        User user = new User();
        user.username = username + "_" + System.currentTimeMillis();
        user.password = PasswordUtil.hashPassword("password123");
        user.name = name;
        user.role = role;
        user.active = true;
        user.forcePasswordChange = false;
        user.createTime = new java.util.Date();
        user.lastLoginTime = new java.util.Date(0);
        return user;
    }

    @AfterAll
    @DisplayName("清理测试环境")
    public static void tearDownAfterClass() throws SQLException {
        DatabaseTestBase.clearTestData();
        DatabaseManager.clearTestConnection();
    }
}