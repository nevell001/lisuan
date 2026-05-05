-- ============================================
-- MySQL Root 用户远程连接权限配置
-- ============================================
-- ⚠️ 安全警告：请在部署前修改以下密码！
-- 此脚本在 MySQL 初始化时自动执行
-- 授予 root 用户从任何主机连接的权限

-- 授予 root 用户从任何主机连接的权限
-- 请将 'YOUR_ROOT_PASSWORD_HERE' 替换为您的实际密码
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY 'YOUR_ROOT_PASSWORD_HERE';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;

-- 确保本地 root 也有权限
GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost' WITH GRANT OPTION;

-- 刷新权限
FLUSH PRIVILEGES;

-- 验证权限
SELECT '=== Root 用户权限配置完成 ===' AS status;
SELECT '⚠️ 请确保已修改默认密码！' AS warning;
SELECT user, host FROM mysql.user WHERE user = 'root';