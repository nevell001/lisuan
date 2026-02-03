-- ============================================
-- 修复 MySQL root 密码并创建 cashier 用户
-- ============================================

-- 1. 为 root 用户设置密码（使用 mysql_native_password）
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'RootPassword123!';
ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY 'RootPassword123!';

-- 2. 创建 cashier 用户
DROP USER IF EXISTS 'cashier'@'%';
CREATE USER 'cashier'@'%' IDENTIFIED WITH mysql_native_password BY 'YourStrongPassword123!';

-- 3. 授予权限
GRANT ALL PRIVILEGES ON cashier_system.* TO 'cashier'@'%';
GRANT ALL PRIVILEGES ON cashier_system.* TO 'cashier'@'localhost';

-- 4. 刷新权限
FLUSH PRIVILEGES;

-- 5. 验证用户
SELECT '=== 用户创建完成 ===' AS status;
SELECT user, host, plugin FROM mysql.user WHERE user IN ('root', 'cashier');
