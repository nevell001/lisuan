# Windows 本地 MySQL 安装指南

本指南适用于在 Windows 10 上为收银系统安装和配置 MySQL 8.0 数据库。

## 📋 目录

- [环境要求](#环境要求)
- [安装方式一：官方安装包（推荐）](#安装方式一官方安装包推荐)
- [安装方式二：使用 WAMP/XAMPP](#安装方式二使用-wampxampp)
- [配置数据库](#配置数据库)
- [配置应用连接](#配置应用连接)
- [测试连接](#测试连接)
- [常见问题](#常见问题)

## 环境要求

- **操作系统**: Windows 10 或更高版本
- **数据库版本**: MySQL 8.0 或更高版本
- **内存**: 最小 2GB（推荐 4GB+）
- **磁盘空间**: 最小 2GB 可用空间

## 安装方式一：官方安装包（推荐）

### 步骤 1: 下载 MySQL 安装包

1. 访问 MySQL 官网：https://dev.mysql.com/downloads/installer/
2. 选择 **Windows (x86, 64-bit), MSI Installer**
3. 下载安装程序（推荐 `mysql-installer-community-8.0.x.x.msi`）

### 步骤 2: 运行安装程序

1. 双击运行下载的 MSI 安装程序
2. 选择安装类型：
   - **Developer Default**（推荐）- 包含 MySQL Server、Workbench、Connector 等
   - **Server only** - 仅安装 MySQL Server
   - **Custom** - 自定义安装组件

### 步骤 3: 安装配置

1. **产品配置**：
   - 点击 "Next" 开始配置
   - 接受许可协议
   - 点击 "Execute" 安装组件

2. **Type and Networking**：
   - **Config Type**: Development Computer
   - **Port**: 3306（默认）
   - **Open Windows Firewall port for network access**: 勾选

3. **Authentication Method**：
   - 选择 **Use Strong Password Encryption**（推荐）
   - 或选择 **Legacy Authentication Method**（如果需要兼容旧版本）

4. **Accounts and Roles**：
   - 设置 **root** 用户密码
   - 使用密码管理器生成并保存独立的强随机密码，不要照抄文档示例
   - 可以添加其他用户（可选）

5. **Windows Service**：
   - **Configure MySQL Server as a Windows Service**: 勾选
   - **Windows Service Name**: MySQL80（默认）
   - **Start the MySQL Server at System Startup**: 勾选（推荐）
   - **Run Windows Service as**: Standard System Account（默认）

6. **Apply Configuration**：
   - 点击 "Execute" 应用配置
   - 等待配置完成

### 步骤 4: 完成安装

1. 点击 "Finish" 完成安装
2. MySQL Server 会自动启动
3. 可以使用 MySQL Workbench 连接数据库

## 安装方式二：使用 WAMP/XAMPP

### 使用 XAMPP

1. 下载 XAMPP：https://www.apachefriends.org/
2. 运行安装程序，选择安装路径（建议：`C:\xampp`）
3. 安装完成后，打开 XAMPP Control Panel
4. 点击 MySQL 旁边的 "Start" 按钮启动 MySQL
5. 默认配置：
   - 用户名：`root`
   - 密码：（空）
   - 端口：`3306`

### 使用 WampServer

1. 下载 WampServer：https://www.wampserver.com/
2. 运行安装程序
3. 安装完成后，系统托盘会出现 WampServer 图标
4. 左键点击图标 → MySQL → Service → Start/Resume Service
5. 默认配置：
   - 用户名：`root`
   - 密码：（空）
   - 端口：`3306`

## 配置数据库

### 方式一：使用命令行

1. 打开命令提示符（cmd）
2. 进入 MySQL bin 目录：
   ```cmd
   cd "C:\Program Files\MySQL\MySQL Server 8.0\bin"
   ```
3. 登录 MySQL：
   ```cmd
   mysql -u root -p
   ```
4. 输入 root 密码

5. 创建数据库和专用用户（用户名需与 `config\database.properties` 的 `db.username` 一致，默认 `lisuan`）：完整 SQL 见 [MYSQL_SETUP.md 的「配置数据库」](MYSQL_SETUP.md#配置数据库)，本文不再重复。

### 方式二：使用 MySQL Workbench

1. 打开 MySQL Workbench
2. 点击 "+" 创建新连接
3. 输入连接信息：
   - **Connection Name**: LiSuan System
   - **Hostname**: 127.0.0.1
   - **Port**: 3306
   - **Username**: root
   - **Password**: [输入 root 密码]
4. 点击 "Test Connection" 测试连接
5. 连接成功后，点击 "OK"
6. 打开连接，在 SQL 编辑器中执行建库建用户 SQL：见 [MYSQL_SETUP.md 的「配置数据库」](MYSQL_SETUP.md#配置数据库)（用户名 `lisuan`，需与 `db.username` 一致）。

### 方式三：使用 phpMyAdmin（如果已安装）

1. 打开浏览器，访问：http://localhost/phpmyadmin
2. 登录（默认用户名：root，密码根据安装设置）
3. 点击 "数据库" 标签
4. 创建新数据库：
   - 数据库名：`lisuan_system`
   - 排序规则：`utf8mb4_unicode_ci`
5. 点击 "创建"
6. 点击 "用户账户" 标签
7. 添加新用户：
   - 用户名：`lisuan`
   - 主机名：`任意主机 (%)`
   - 密码：使用密码管理器生成的应用专用强随机密码
8. 在 "数据库特定权限" 部分，选择 `lisuan_system` 数据库
9. 勾选 "检查全部"，点击 "执行"

## 配置应用连接

1. 打开项目目录中的 `config\database.properties` 文件（可从 `config\database.properties.example` 复制）
2. 确认用户名与前面创建的数据库用户一致：

```properties
# 数据库连接配置
db.url=jdbc:mysql://localhost:3306/lisuan_system?sslMode=PREFERRED&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
db.username=lisuan
# 推荐留空，并通过 CASHIER_DB_PASSWORD 环境变量提供
db.password=
```

3. 保存文件

> 不要使用 root 用户连接应用（不推荐）；即使临时使用，也必须同步改 `db.username`。
> 完整参数说明与连接池优化见 [MYSQL_SETUP.md 的「配置应用」](MYSQL_SETUP.md#配置应用)。

## 测试连接

### 使用 MySQL Workbench

1. 打开 MySQL Workbench
2. 使用 `lisuan` 用户创建连接
3. 测试连接是否成功

### 使用命令行

```cmd
cd "C:\Program Files\MySQL\MySQL Server 8.0\bin"
mysql -u lisuan -p lisuan_system
```

> 连接失败时先看下文常见问题，或 [MYSQL_SETUP.md 的「故障排查」](MYSQL_SETUP.md#故障排查)。

## 常见问题

### 问题 1: 无法连接到 MySQL

**错误信息**: `Communications link failure`

**解决方案**:

1. 检查 MySQL 服务是否运行：
   - 打开 "服务"（services.msc）
   - 找到 "MySQL80" 服务
   - 确保状态为 "正在运行"

2. 检查防火墙设置：
   - 打开 Windows Defender 防火墙
   - 允许 MySQL 端口 3306 通过

3. 检查端口是否被占用：
   ```cmd
   netstat -ano | findstr 3306
   ```

### 问题 2: 连接超时

**错误信息**: `Communications link failure` 或连接超时

**解决方案**:

修改 MySQL 配置文件 `my.ini`（默认位于 `C:\ProgramData\MySQL\MySQL Server 8.0\`）：

```ini
[mysqld]
wait_timeout = 28800
interactive_timeout = 28800
max_connections = 200
```

重启 MySQL 服务（services.msc → MySQL80 → 重启）。

> 若只是应用**冷启动首次建连慢**，优先调大 `config\database.properties` 的 `db.connection.timeout`，而不是改 my.ini。

### 更多问题

认证插件错误、中文字符乱码、权限不足等见 [MYSQL_SETUP.md 的「故障排查」](MYSQL_SETUP.md#故障排查)。

## 下一步

1. 完成 MySQL 安装和配置后，运行 `install.bat` 安装应用
2. 运行 `start.bat` 启动收银系统
3. 首次以空库启动时会先弹出**首次运行向导**，用于创建管理员账号（仓库不提供默认口令）；表结构由应用启动时自动创建/升级

## 相关文档

- [MySQL 官方文档](https://dev.mysql.com/doc/)
- [项目 README](../README.md)
- [数据库初始化文档](DATABASE_INIT.md)

## 技术支持

如有问题，请访问：
- 项目 Issue: https://gitee.com/nevell/hello/issues
- MySQL 官方文档: https://dev.mysql.com/doc/refman/8.0/en/
