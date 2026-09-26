#!/bin/bash
# 狸算(LiSuan) 生产发布候选验证脚本
# 该脚本执行完整的测试、静态扫描、打包和验证流程

set -e

echo "===================================================="
echo "      狸算(LiSuan) 生产发布候选验证开始"
echo "===================================================="

# 0. 版本一致性门禁
echo "[0/3] 正在校验版本一致性..."
VERSION_POM=$(grep -m 1 "<version>" pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
VERSION_JAVA=$(grep "APP_VERSION =" src/main/java/com/cashier/constant/AppConstants.java | sed 's/.*"\(.*\)".*/\1/')
VERSION_INSTALLER=$(grep "private static final String APP_VERSION" src/main/java/com/cashier/installer/Installer.java | sed 's/.*"\(.*\)".*/\1/')
VERSION_ENV=$(grep -m 1 "^APP_VERSION=" .env.example | cut -d= -f2- | tr -d '\r')

# 四处必须一致（pom.xml / AppConstants / 安装器 / .env.example）；
# 同一个不变量由 src/test/java/com/cashier/constant/VersionConsistencyTest 在 CI 里守着
for pair in "pom.xml:$VERSION_POM" "AppConstants.java:$VERSION_JAVA" \
            "installer/Installer.java:$VERSION_INSTALLER" ".env.example:$VERSION_ENV"; do
    name="${pair%%:*}"
    value="${pair#*:}"
    if [ "$value" != "$VERSION_POM" ]; then
        echo "✗ 错误：版本号不一致！"
        echo "  pom.xml: $VERSION_POM"
        echo "  $name: $value"
        exit 1
    fi
done
echo "✓ 版本号一致 ($VERSION_POM，四处已比对)"

# 从 .env 定向读取数据库密码，与 start.sh 保持一致（仅限密码变量）
if [ -f ".env" ]; then
    load_db_password_from_env() {
        local key="$1" value
        value=$(grep -E "^${key}=" .env | head -1 | cut -d= -f2- | sed 's/^[[:space:]]*"//;s/"[[:space:]]*$//')
        if [ -z "${!key:-}" ] && [ -n "$value" ]; then
            export "$key=$value"
        fi
    }
    load_db_password_from_env CASHIER_DB_PASSWORD
    load_db_password_from_env CASHER_DB_PASSWORD

    # 兼容旧版 .env：MYSQL_PASSWORD 已统一为 CASHIER_DB_PASSWORD
    if [ -z "${CASHIER_DB_PASSWORD:-}" ] && [ -z "${CASHER_DB_PASSWORD:-}" ]; then
        MYSQL_PASSWORD_VALUE=$(grep -E "^MYSQL_PASSWORD=" .env | head -1 | cut -d= -f2- | sed 's/^[[:space:]]*"//;s/"[[:space:]]*$//')
        if [ -n "$MYSQL_PASSWORD_VALUE" ]; then
            export CASHIER_DB_PASSWORD="$MYSQL_PASSWORD_VALUE"
        fi
    fi
fi

# 1. 完整验证门禁：单元测试 + SpotBugs + JaCoCo 覆盖率 + 打包
echo "[1/3] 正在运行完整验证门禁 (mvn clean verify)..."
mvn clean verify -DskipTests=false
echo "✓ 完整验证通过（测试/SpotBugs/覆盖率/打包）"

# 2. 环境与配置验证
echo "[2/3] 验证发布配置..."

# 检查 JAR 文件
if [ -f "target/lisuan-fx-${VERSION_POM}-jar-with-dependencies.jar" ]; then
    echo "✓ 可执行 JAR 已生成"
else
    echo "✗ 找不到可执行 JAR"
    exit 1
fi

# 数据库生产配置门禁
DB_CONFIG="config/database.properties"
if [ ! -f "$DB_CONFIG" ]; then
    echo "✗ 找不到数据库配置文件: $DB_CONFIG"
    exit 1
fi

DB_URL=$(grep '^db.url=' "$DB_CONFIG" | cut -d= -f2-)
DB_PASSWORD=$(grep '^db.password=' "$DB_CONFIG" | cut -d= -f2-)

if echo "$DB_URL" | grep -E 'useSSL=false|sslMode=DISABLED' > /dev/null 2>&1; then
    echo "✗ 数据库连接禁用了 SSL，请改用 sslMode=PREFERRED/REQUIRED/VERIFY_CA/VERIFY_IDENTITY"
    exit 1
fi

if [ -n "$DB_PASSWORD" ]; then
    echo "✗ config/database.properties 不应保存数据库密码，请改用 CASHIER_DB_PASSWORD 环境变量"
    exit 1
fi

if [ -z "${CASHIER_DB_PASSWORD:-}" ] && [ -z "${CASHER_DB_PASSWORD:-}" ]; then
    echo "✗ 未设置 CASHIER_DB_PASSWORD，生产发布必须通过环境变量提供数据库密码"
    exit 1
fi
echo "✓ 数据库密码与 SSL 配置通过"

# API 生产配置门禁：API 未开启时允许发布；开启后必须使用强密钥和限定 CORS。
API_CONFIG="config/api.properties"
if [ -f "$API_CONFIG" ] && grep -q '^api.enabled=true' "$API_CONFIG"; then
    TOKEN_SECRET_VALUE="${TOKEN_SECRET:-$(grep '^token.secret=' "$API_CONFIG" | cut -d= -f2-)}"
    CORS_VALUE="${CORS_ALLOWED_ORIGINS:-$(grep '^cors.allowed.origins=' "$API_CONFIG" | cut -d= -f2-)}"

    if [ "${#TOKEN_SECRET_VALUE}" -lt 32 ] \
        || [ "$TOKEN_SECRET_VALUE" = "default_secret_key" ] \
        || echo "$TOKEN_SECRET_VALUE" | grep -E 'REPLACE_|change_this|your_secret' > /dev/null 2>&1; then
        echo "✗ API 已开启，但 TOKEN_SECRET 不安全"
        exit 1
    fi

    if [ -z "$CORS_VALUE" ] || echo "$CORS_VALUE" | grep '\*' > /dev/null 2>&1; then
        echo "✗ API 已开启，但 CORS_ALLOWED_ORIGINS/cors.allowed.origins 未限制为具体来源"
        exit 1
    fi
    echo "✓ API 安全配置通过"
else
    echo "✓ API 未开启，跳过 API 密钥与 CORS 发布门禁"
fi

# 支付配置门禁：生产发布禁止 mock 模式；production 模式必须已配置真实凭据（无占位符）。
if [ -f "config/payment.properties" ]; then
    PAYMENT_MODE=$(grep '^payment.mode=' config/payment.properties | cut -d= -f2- | tr '[:upper:]' '[:lower:]' | tr -d ' ')
    if [ "$PAYMENT_MODE" = "mock" ]; then
        echo "✗ 生产发布禁止使用 mock 支付模式！请配置真实支付通道或保持 disabled"
        exit 1
    fi
    if [ "$PAYMENT_MODE" = "production" ]; then
        if grep -E 'YOUR_|REPLACE_WITH|CHANGE_ME|change_me|your-' config/payment.properties > /dev/null 2>&1; then
            echo "✗ production 支付模式仍包含占位凭据（YOUR_/REPLACE_WITH/CHANGE_ME），请填写真实商户配置"
            exit 1
        fi
        echo "✓ 支付配置使用 production 模式且未发现占位凭据（真实通道回调需另行验证）"
    fi
    if [ "$PAYMENT_MODE" = "disabled" ]; then
        echo "⚠ 电子支付当前为 disabled；如生产需要微信/支付宝收款，请先配置真实支付通道"
    fi
fi

# 检查敏感信息（二次确认）
if grep -r -E "RootPassword123!|db.password=.+[^[:space:]]" config/ src/main/resources/ \
    --exclude='database.properties.template' \
    --exclude='*.example' > /dev/null 2>&1; then
    echo "✗ 警告：在代码或配置中发现疑似泄露的本地密码！请清理后再发布。"
    exit 1
else
    echo "✓ 未发现明显的本地泄露密码"
fi

# 检查 .env 是否被误包含在 target 中（如果有）
if [ -f "target/classes/.env" ] || [ -f "target/classes/.env.example" ]; then
    echo "✗ 警告：.env 文件被误包含在打包资源中！"
    exit 1
fi

echo "===================================================="
echo "      狸算(LiSuan) 发布验证全部通过！"
echo "      发布版本: ${VERSION_POM}"
echo "===================================================="
