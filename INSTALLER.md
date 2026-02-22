# 收银系统安装程序

## 图形化安装程序

本项目提供了一个简单的图形化安装程序，帮助您快速配置和安装收银系统。

### 使用方法

#### Windows
双击运行 `installer.bat`

#### Linux / macOS
运行 `./install.sh` 或使用以下命令：
```bash
mvn compile exec:java -Dexec.mainClass="com.cashier.installer.Installer"
```

### 安装步骤

1. **环境检查**
   - 自动检查 Java、Maven 和 Docker 环境
   - 显示检查结果

2. **选择数据库类型**
   - Docker MySQL（推荐）：自动安装和管理 MySQL
   - 本地 MySQL：使用已安装的 MySQL
   - 跳过配置：稍后手动配置

3. **配置数据库连接**
   - 输入数据库主机、端口、用户名和密码
   - 默认值已预填

4. **自动安装**
   - 下载 Maven 依赖
   - 编译项目
   - 配置数据库
   - 创建配置文件

5. **完成安装**
   - 显示安装完成信息
   - 提供启动方式和默认登录信息

### 默认登录

- 用户名: `admin`
- 密码: `admin123`

### 手动安装

如果您希望手动安装，可以参考以下步骤：

1. 确保已安装 Java 17+ 和 Maven 3.8+
2. 克隆或下载项目
3. 运行 `mvn clean package -DskipTests` 编译项目
4. 配置 `config/database.properties`
5. 运行 `start.bat`（Windows）或 `./start.sh`（Linux/Mac）

### 常见问题

#### Java 版本错误
确保安装了 Java 17 或更高版本：
```bash
java -version
```

#### Maven 未找到
请将 Maven 添加到系统 PATH，或使用 Maven Wrapper。

#### Docker 连接失败
确保 Docker Desktop 已安装并正在运行。

### 技术支持

如有问题，请查看日志或提交 Issue。