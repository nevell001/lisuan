-- 收银系统 v2.5.2 - 货币配置支持
-- 为 language_preferences 表添加货币字段

-- 修改 language_preferences 表，添加货币字段
ALTER TABLE language_preferences ADD COLUMN currency_code VARCHAR(10) DEFAULT 'CNY' COMMENT '货币代码 (CNY, USD, JPY, KRW, EUR)' AFTER language_tag;

-- 为现有记录设置默认货币
UPDATE language_preferences SET currency_code = 'CNY' WHERE currency_code IS NULL;

-- 验证修改
SELECT
    username,
    language_tag,
    currency_code,
    updated_at
FROM language_preferences
LIMIT 5;
