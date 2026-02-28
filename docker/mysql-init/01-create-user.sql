-- ============================================
-- 收银系统 MySQL 初始化脚本
-- ============================================

-- 1. 确保 root 用户可以从任何主机连接
-- 注意：Docker 容器中的 MySQL 需要从宿主机（如 WSL 172.18.0.1）连接
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY 'RootPassword123!';
CREATE USER IF NOT EXISTS 'root'@'172.18.0.1' IDENTIFIED BY 'RootPassword123!';
CREATE USER IF NOT EXISTS 'root'@'192.168.6.141' IDENTIFIED BY 'RootPassword123!';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
GRANT ALL PRIVILEGES ON *.* TO 'root'@'172.18.0.1' WITH GRANT OPTION;
GRANT ALL PRIVILEGES ON *.* TO 'root'@'192.168.6.141' WITH GRANT OPTION;
GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost' WITH GRANT OPTION;

-- 2. 创建专用用户（如果不存在）
-- 注意：通过 docker-compose.yml 环境变量创建的用户可能权限不足
-- 这个脚本确保用户有完整的权限

CREATE USER IF NOT EXISTS 'cashier'@'%' IDENTIFIED BY 'YourStrongPassword123!';
CREATE USER IF NOT EXISTS 'cashier'@'localhost' IDENTIFIED BY 'YourStrongPassword123!';

-- 3. 授予所有权限
GRANT ALL PRIVILEGES ON cashier_system.* TO 'cashier'@'%';
GRANT ALL PRIVILEGES ON cashier_system.* TO 'cashier'@'localhost';

-- 3. 允许用户创建其他用户（可选，用于某些功能）
-- GRANT CREATE USER ON *.* TO 'cashier'@'%';

-- 4. 刷新权限
FLUSH PRIVILEGES;

-- 5. 创建初始数据（如果需要）

-- 插入默认管理员用户（密码: admin123，需要 BCrypt 加密）
-- 注意：密码已在应用启动时创建
-- INSERT INTO users (username, password, name, role, active, last_login, created_at)
-- VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 'admin', 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000);

-- 6. 显示创建的用户
SELECT '=== MySQL 用户创建完成 ===' AS status;
SELECT user, host FROM mysql.user WHERE user IN ('root', 'cashier');

-- 7. 显示数据库信息
SELECT '=== 数据库信息 ===' AS status;
SHOW DATABASES LIKE 'cashier%';
