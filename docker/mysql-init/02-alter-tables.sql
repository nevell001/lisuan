-- ============================================
-- 收银系统 MySQL 表结构升级脚本
-- ============================================
-- 此脚本用于为现有表添加 id 字段
-- 运行此脚本前请确保已备份数据库

-- 1. 为 members 表添加 id 字段（如果不存在）
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'members'
    AND COLUMN_NAME = 'id'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE members ADD COLUMN id INT AUTO_INCREMENT PRIMARY KEY FIRST',
    'SELECT "members.id column already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 为 categories 表添加 id 字段（如果不存在）
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'categories'
    AND COLUMN_NAME = 'id'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE categories ADD COLUMN id INT AUTO_INCREMENT PRIMARY KEY FIRST, MODIFY COLUMN name VARCHAR(50) UNIQUE NOT NULL',
    'SELECT "categories.id column already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 为 units 表添加 id 字段（如果不存在）
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'units'
    AND COLUMN_NAME = 'id'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE units ADD COLUMN id INT AUTO_INCREMENT PRIMARY KEY FIRST, MODIFY COLUMN name VARCHAR(50) UNIQUE NOT NULL',
    'SELECT "units.id column already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 创建主题偏好表（如果不存在）
SET @table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'theme_preferences'
);

SET @sql = IF(@table_exists = 0,
    'CREATE TABLE IF NOT EXISTS theme_preferences (
        username VARCHAR(50) PRIMARY KEY,
        theme_name VARCHAR(20) DEFAULT ''light'',
        updated_at BIGINT,
        INDEX idx_username (username),
        FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci',
    'SELECT "theme_preferences table already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT '=== 表结构升级完成 ===' AS status;