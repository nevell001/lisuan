# 收银系统安装指南

## 快速开始

### Windows 用户

1. **下载分发包**
   - 获取 `CashierSystem-v{version}.zip`
   - 解压到任意目录

2. **配置数据库**
   - 双击运行 `Database Config.bat`
   - 选择数据库类型：
     - **Local MySQL** - 本地安装的 MySQL
     - **Docker MySQL** - Docker 容器（推荐）
     - **Remote MySQL** - 远程 MySQL 服务器
   - 点击 "Test Connection" 测试连接
   - 点击 "Save & Start" 保存配置

3. **启动应用**
   - 配置完成后选择 "Yes" 自动启动
   - 或双击 `Quick Start.bat` 手动启动

### Linux/macOS 用户

1. **安装依赖**
   ```bash
   # 安装 Java 17+
   sudo apt install openjdk-17-jdk  # Ubuntu/Debian
   brew install openjdk@17          # macOS
   
   # 安装 Maven
   sudo apt install maven            # Ubuntu/Debian
   brew install maven                # macOS
   ```

2. **编译项目**
   ```bash
   mvn clean package -DskipTests
   ```

3. **配置数据库**
   ```bash
   # 创建配置文件
   mkdir -p config
   cp config/database.properties.example config/database.properties
   
   # 编辑数据库配置
   nano config/database.properties
   ```

4. **启动应用**
   ```bash
   ./start.sh
   ```

## 数据库配置

### 选项 1: Docker MySQL（推荐）

```bash
# 启动 MySQL 容器
docker compose up -d mysql

# 查看容器状态
docker ps
```

默认配置：
- 主机: `localhost:3306`
- 数据库: `cashier_system`
- 用户名: `root`
- 密码: `RootPassword123!`

### 选项 2: 本地 MySQL

1. 安装 MySQL 8.0+
2. 创建数据库：
   ```sql
   CREATE DATABASE cashier_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. 创建用户并授权：
   ```sql
   CREATE USER 'cashier'@'localhost' IDENTIFIED BY 'YourPassword123!';
   GRANT ALL PRIVILEGES ON cashier_system.* TO 'cashier'@'localhost';
   FLUSH PRIVILEGES;
   ```

## 默认登录

- **用户名**: `admin`
- **初始密码**: 首次启动时自动生成（查看 `logs/app.log`）

## 开发者指南

### 构建分发包

```bash
# Windows
package.bat

# Linux/macOS
mvn clean package -DskipTests
mkdir -p dist/CashierSystem
cp target/cashier-system-fx-*-jar-with-dependencies.jar dist/CashierSystem/
cp start.bat dist/CashierSystem/  # 或 start.sh for Linux
```

### 运行图形化配置工具

```bash
# 从源码运行
mvn compile exec:java -Dexec.mainClass="com.cashier.installer.DatabaseConfigDialog"
```

## 常见问题

### Q: 提示"数据源未初始化或已关闭"
**A**: 数据库连接失败，请检查：
1. MySQL 服务是否运行
2. `config/database.properties` 配置是否正确
3. 数据库是否已创建

### Q: Java 版本错误
**A**: 确保安装了 Java 17 或更高版本：
```bash
java -version
```

### Q: 找不到 JavaFX 运行时
**A**: 使用 `start.bat` 脚本启动，它会自动配置 JavaFX 模块路径。

### Q: 端口 3306 已被占用
**A**: 
- Windows: `netstat -ano | findstr :3306` 查找占用进程
- Linux: `lsof -i :3306` 查找占用进程
- 或修改 `docker-compose.yml` 中的端口映射

## 目录结构

```
CashierSystem/
├── cashier-system-fx-{version}-jar-with-dependencies.jar  # 应用 JAR
├── Database Config.bat                                      # 数据库配置工具
├── Quick Start.bat                                          # 快速启动
├── start.bat                                                # 完整启动脚本
├── config/                                                  # 配置目录
│   ├── database.properties                                  # 数据库配置
│   └── jvm.config                                           # JVM 参数（可选）
├── data/                                                    # 数据目录
├── logs/                                                    # 日志目录
└── lib/                                                     # GUI 配置工具类
```

## 系统要求

- **Java**: JDK 17 或更高版本
- **内存**: 最低 512MB，推荐 1GB
- **磁盘**: 最低 100MB 可用空间
- **数据库**: MySQL 8.0+ 或兼容的数据库

## 技术支持

如有问题，请：
1. 查看 `logs/app.log` 获取详细错误信息
2. 检查 [README.md](README.md) 了解项目详情
3. 提交 Issue 到项目仓库
