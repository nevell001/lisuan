-- ============================================
-- v2.5.5 字号偏好设置表
-- ============================================

SET @table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'font_size_preferences'
);

SET @sql = IF(@table_exists = 0,
    'CREATE TABLE IF NOT EXISTS font_size_preferences (
        username VARCHAR(50) PRIMARY KEY,
        font_size VARCHAR(20) DEFAULT ''medium'',
        updated_at BIGINT,
        INDEX idx_username (username),
        FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci',
    'SELECT "font_size_preferences table already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 插入默认字号偏好（为已存在的用户）
INSERT IGNORE INTO font_size_preferences (username, font_size, updated_at)
SELECT username, 'medium', UNIX_TIMESTAMP() * 1000
FROM users
WHERE username NOT IN (SELECT username FROM font_size_preferences);
