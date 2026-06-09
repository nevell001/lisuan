# 生产环境安全部署指南

本文档说明如何将收银系统安全地部署到生产环境。

---

## 📋 部署前安全检查清单

- [ ] 设置强随机 TOKEN_SECRET
- [ ] 配置 CORS_ALLOWED_ORIGINS
- [ ] 启用数据库 SSL 连接
- [ ] 配置 HTTPS 反向代理
- [ ] 更改所有默认密码
- [ ] 配置防火墙规则
- [ ] 设置定期备份

---

## 🔐 1. 环境变量配置

创建 `.env` 文件或设置系统环境变量：

```bash
# ============================================
# API 安全配置（必需）
# ============================================

# Token 密钥 - 必须使用强随机字符串
# 生成方法: openssl rand -base64 32
TOKEN_SECRET=YOUR_GENERATED_RANDOM_32_CHAR_SECRET_KEY_HERE

# CORS 允许的来源 - 限制哪些域名可以访问 API
# 多个域名用逗号分隔，不要使用 *
CORS_ALLOWED_ORIGINS=https://pos.yourdomain.com,https://admin.yourdomain.com

# ============================================
# 数据库配置
# ============================================

# 生产环境建议使用强密码
MYSQL_ROOT_PASSWORD=YOUR_STRONG_ROOT_PASSWORD
MYSQL_PASSWORD=YOUR_STRONG_APP_PASSWORD

# 启用数据库 SSL（推荐）
DB_USE_SSL=true
```

### 生成安全的 Token 密钥

```bash
# Linux/macOS
openssl rand -base64 32

# 或使用 Python
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

---

## 🌐 2. HTTPS 反向代理配置

### 使用 Nginx 配置 HTTPS

#### 安装 Nginx 和 Let's Encrypt

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install nginx certbot python3-certbot-nginx

# CentOS/RHEL
sudo yum install nginx certbot python3-certbot-nginx
```

#### 获取 SSL 证书

```bash
sudo certbot --nginx -d api.yourdomain.com
```

#### Nginx 配置文件

`/etc/nginx/sites-available/cashier-system`

```nginx
upstream cashier_backend {
    server 127.0.0.1:8080;
    # 多实例部署时添加更多服务器
    # server 127.0.0.1:8081;
    # server 127.0.0.1:8082;
}

server {
    listen 443 ssl http2;
    server_name api.yourdomain.com;

    # SSL 证书配置
    ssl_certificate /etc/letsencrypt/live/api.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.yourdomain.com/privkey.pem;

    # 现代 SSL 配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;

    # 安全响应头
    add_header Strict-Transport-Security "max-age=63072000; includeSubDomains" always;
    add_header X-Frame-Options DENY always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # API 路由
    location /api/ {
        proxy_pass http://cashier_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket 支持
    location /ws/ {
        proxy_pass http://cashier_backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # 静态资源
    location /static/ {
        alias /var/www/cashier/static/;
        expires 30d;
    }

    # 日志
    access_log /var/log/nginx/cashier_access.log;
    error_log /var/log/nginx/cashier_error.log;
}

# HTTP 重定向到 HTTPS
server {
    listen 80;
    server_name api.yourdomain.com;
    return 301 https://$server_name$request_uri;
}
```

#### 启用配置

```bash
sudo ln -s /etc/nginx/sites-available/cashier-system /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

---

## 🔒 3. 数据库安全配置

### 启用 MySQL SSL

#### 检查 MySQL SSL 状态

```sql
SHOW VARIABLES LIKE '%ssl%';
```

#### 配置 MySQL 强制 SSL

```sql
-- 为应用用户强制 SSL 连接
ALTER USER 'cashier'@'%' REQUIRE SSL;

-- 或要求特定证书
ALTER USER 'cashier'@'%' REQUIRE X509;
```

#### 应用端配置

`config/database.properties`

```properties
# 启用 SSL
db.url=jdbc:mysql://db.example.com:3306/cashier_system?useSSL=true&requireSSL=true&verifyServerCertificate=true

# 如使用自签名证书，需配置信任库
# db.url=jdbc:mysql://db.example.com:3306/cashier_system?useSSL=true&requireSSL=true&verifyServerCertificate=false&trustCertificateKeyStoreUrl=file:/path/to/keystore.jks&trustCertificateKeyStorePassword=keystore_password
```

---

## 🛡️ 4. 防火墙配置

### 使用 UFW (Ubuntu)

```bash
# 启用 UFW
sudo ufw enable

# 允许 SSH
sudo ufw allow 22/tcp

# 允许 HTTPS
sudo ufw allow 443/tcp

# 拒绝直接访问应用端口
sudo ufw deny 8080/tcp

# 拒绝直接访问数据库
sudo ufw deny 3306/tcp

# 查看状态
sudo ufw status
```

### 使用 firewalld (CentOS/RHEL)

```bash
# 允许 HTTPS
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --permanent --add-service=http

# 拒绝应用端口
sudo firewall-cmd --permanent --remove-port=8080/tcp

# 重载配置
sudo firewall-cmd --reload
```

---

## 📦 5. Docker Compose 生产配置

`docker-compose.prod.yml`

```yaml
services:
  mysql:
    image: mysql:8.4
    container_name: cashier-mysql-prod
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    ports:
      - "127.0.0.1:3306:3306"  # 仅监听本地
    volumes:
      - cashier-mysql-data:/var/lib/mysql
      - ./docker/mysql-init:/docker-entrypoint-initdb.d:ro
      - ./docker/mysql-backup:/backup
    command: >
      --mysql-native-password=ON
      --ssl=ON
      --require-secure-transports=ON
      --bind-address=0.0.0.0
    networks:
      - cashier-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    image: cashier-system:latest
    container_name: cashier-app-prod
    restart: always
    environment:
      - TOKEN_SECRET=${TOKEN_SECRET}
      - CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS}
      - DB_HOST=mysql
      - DB_PORT=3306
    ports:
      - "127.0.0.1:8080:8080"  # 仅监听本地
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      - cashier-network

  nginx:
    image: nginx:alpine
    container_name: cashier-nginx-prod
    restart: always
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./certs:/etc/nginx/certs:ro
    depends_on:
      - app
    networks:
      - cashier-network

volumes:
  cashier-mysql-data:

networks:
  cashier-network:
    driver: bridge
```

---

## 🔍 6. 安全监控和日志

### 配置日志监控

```bash
# 安装 fail2ban 防止暴力攻击
sudo apt install fail2ban

# 配置 /etc/fail2ban/jail.local
[nginx-http-auth]
enabled = true
port = http,https
logpath = /var/log/nginx/error.log
```

### 定期检查日志

```bash
# 检查访问日志
sudo tail -f /var/log/nginx/cashier_access.log

# 检查错误日志
sudo tail -f /var/log/nginx/cashier_error.log
```

---

## 🔄 7. 证书自动续期

Let's Encrypt 证书有效期 90 天，设置自动续期：

```bash
# 测试续期
sudo certbot renew --dry-run

# 添加定时任务
sudo crontab -e

# 添加以下行（每天凌晨 2 点检查并续期）
0 2 * * * certbot renew --quiet --post-hook "systemctl reload nginx"
```

---

## 📋 8. 部署后验证清单

部署完成后，验证以下内容：

```bash
# 1. 检查 HTTPS 是否正常
curl -I https://api.yourdomain.com/api/v1/health
# 应返回 200 和安全响应头

# 2. 检查 HTTP 是否重定向到 HTTPS
curl -I http://api.yourdomain.com
# 应返回 301 重定向

# 3. 检查安全响应头
curl -I https://api.yourdomain.com/api/v1/health | grep -E "Strict-Transport-Security|X-Frame-Options|X-Content-Type-Options"

# 4. 检查数据库 SSL 连接
# 在应用日志中查找 SSL 连接信息

# 5. 检查 CORS 是否正确配置
# 从不允许的域名访问 API，应被拒绝
```

---

## 🚨 9. 安全事件响应

### 发现安全漏洞时

1. **立即隔离**：断开受影响系统的网络连接
2. **评估影响**：确定数据泄露范围
3. **修复漏洞**：应用安全补丁
4. **更改凭证**：重置所有可能的密码和密钥
5. **通知用户**：如涉及用户数据，按规定通知
6. **文档记录**：记录事件和响应过程

---

## 📞 10. 安全支持

如发现安全漏洞，请通过以下方式联系：

- 邮箱：security@yourdomain.com
- 或通过私有安全报告渠道提交

---

**文档版本：** v1.0  
**最后更新：** 2025-06-09
