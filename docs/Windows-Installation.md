# Windows 安装指南

本指南介绍如何在 Windows 系统上安装和运行收银系统。

## 系统要求

- **操作系统**: Windows 10/11 (64位)
- **Java**: 不需要（已内嵌于安装包）
- **数据库**: MySQL 8.0+ 或 MariaDB 10.6+
- **内存**: 至少 2GB 可用内存
- **磁盘**: 至少 500MB 可用空间（安装包约 150MB）

---

## 安装步骤

### 第一步：安装 MySQL 数据库

收银系统需要 MySQL 数据库来存储数据。选择以下方式之一：

#### 方式 A：使用 MySQL Installer（推荐新手）

1. 访问 [MySQL 官网](https://dev.mysql.com/downloads/installer/)
2. 下载 MySQL Installer for Windows
3. 运行安装程序，选择 **Custom** 安装类型
4. 选择以下组件：
   - MySQL Server 8.0+
   - MySQL Workbench（可选，用于管理数据库）
5. 完成安装，设置 root 密码
6. 确保 MySQL 服务正在运行

#### 方式 B：使用便携版 MySQL（推荐高级用户）

1. 下载 [MySQL Portable Zip](https://dev.mysql.com/downloads/mysql/)
2. 解压到 `C:\mysql` 目录
3. 创建 `my.ini` 配置文件
4. 初始化数据库并启动服务

#### 方式 C：使用 Docker MySQL（推荐开发者）

```batch
# 安装 Docker Desktop 后运行
docker-compose up -d mysql
```

### 第二步：运行安装程序

1. 双击 `CashierSystem-2.5.5.exe` 安装程序
2. 点击"下一步"阅读许可协议
3. 选择安装位置（默认：`C:\Program Files\CashierSystem`）
4. 点击"安装"开始安装

安装完成后：
- ✅ 桌面创建快捷方式
- ✅ 开始菜单添加程序项
- ✅ 无需命令行启动

---

## 首次运行配置

### 数据库配置向导

首次运行收银系统时，会自动打开数据库配置向导：

1. **选择数据库类型**
   - **Local MySQL** - 本机安装的 MySQL
   - **Docker MySQL** - Docker 容器中的 MySQL
   - **Remote MySQL** - 远程 MySQL 服务器

2. **填写连接信息**
   - 主机：默认 `localhost`
   - 端口：默认 `3306`
   - 数据库名：默认 `cashier_system`
   - 用户名：默认 `root`
   - 密码：您设置的 MySQL 密码

3. **测试连接**
   - 点击"测试连接"按钮
   - 系统会自动创建数据库（如果不存在）

4. **保存配置**
   - 点击"保存并启动"
   - 应用程序将自动启动

---

## 常见问题

### MySQL 连接失败

**错误提示**: "Could not connect to MySQL server"

**解决方法**:
1. 检查 MySQL 服务是否运行
   ```batch
   net start MySQL80
   ```
2. 检查防火墙是否允许 3306 端口
3. 确认用户名和密码正确

### 数据库创建失败

**错误提示**: "Access denied for user"

**解决方法**:
1. 确认 MySQL 用户有创建数据库的权限
2. 或手动创建数据库：
   ```sql
   CREATE DATABASE cashier_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

### 应用启动失败

**错误提示**: "Application exited abnormally"

**解决方法**:
1. 检查日志文件：`logs/app.log`
2. 确认数据库配置正确：`config/database.properties`
3. 重新打开数据库配置向导

---

## 卸载

1. 通过 Windows 控制面板卸载程序
2. 或运行开始菜单中的"卸载"快捷方式

**注意**: 卸载程序不会删除以下内容：
- 数据库数据
- 配置文件
- 日志文件

如需完全删除，请手动删除 `C:\Users\[用户名]\.cashier` 目录。

---

## 高级配置

### 数据库连接池配置

编辑 `config/database.properties`：

```properties
# 连接池大小（默认 10）
db.pool.size=10

# 连接超时（毫秒）
db.connection.timeout=30000

# 空闲超时（毫秒）
db.idle.timeout=600000

# 最大生命周期（毫秒）
db.max.lifetime=1800000
```

### JVM 参数配置

编辑 `config/jvm.config`：

```
-Xms512m    # 最小堆内存
-Xmx1024m   # 最大堆内存
-Dfile.encoding=UTF-8
```

---

## 技术支持

遇到问题？获取帮助：

- **项目主页**: https://gitee.com/nevell/hello
- **问题反馈**: https://gitee.com/nevell/hello/issues
- **文档**: 参见项目 README.md

---

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 2.5.5 | 2026-06-10 | Windows 原生 EXE 安装包 |
| 2.5.4 | 2026-05-21 | Windows 分发包优化 |
| 2.5.0 | 2026-05-01 | REST API 和多终端支持 |
