# 收银系统 (Cashier System)

一个功能完整的收银系统，使用 Java Swing 开发，支持图形化界面（GUI）和命令行界面（CLI）两种模式。

![Java](https://img.shields.io/badge/Java-17-orange)
![License](https://img.shields.io/badge/License-MulanPSL2-blue)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey)

## ✨ 特性

### 🖥️ 图形化界面（GUI）
- 现代化的扁平化设计（基于 FlatLaf 3.5）
- 支持浅色、深色、IntelliJ 三种主题
- 完整的库存管理、购物车、结账功能
- 会员管理系统（积分、等级、折扣）
- 促销活动管理（满减、折扣）
- 交易记录和数据统计
- 交接班管理
- 用户权限管理
- 数据备份和恢复

### 💻 命令行界面（CLI）
- 基于控制台的收银功能
- 库存管理、购物车、结账、搜索
- 彩色终端输出支持

## 🚀 快速开始

### 环境要求

- **JDK**: Java 11 或更高版本（推荐 JDK 17）
- **操作系统**: Windows、macOS、Linux
- **内存**: 最小 512MB，推荐 1GB+
- **磁盘**: 最小 100MB 可用空间

### 安装

1. 克隆仓库
```bash
git clone https://gitee.com/nevell/hello.git
cd hello
```

2. 安装中文字体（Linux 系统必需）

**重要提示**：Linux 系统必须安装中文字体才能正确显示中文！

**自动安装（推荐）：**
```bash
./install_chinese_fonts.sh
```

**手动安装：**

Ubuntu/Debian:
```bash
sudo apt-get update
sudo apt-get install fonts-noto-cjk fonts-wqy-microhei
```

CentOS/RHEL:
```bash
sudo dnf install google-noto-sans-cjk-fonts wqy-microhei-fonts
```

Arch Linux:
```bash
sudo pacman -S noto-fonts-cjk wqy-microhei
```

验证字体安装：
```bash
fc-list :lang=zh
```

**Windows/macOS**：系统自带中文字体，无需额外安装。

3. 编译项目

**Windows:**
```cmd
compile_with_flatlaf.bat
```

**Mac/Linux:**
```bash
chmod +x compile_with_flatlaf.sh
./compile_with_flatlaf.sh
```

3. 运行程序

**Windows:**
```cmd
run_with_flatlaf.bat
```

**Mac/Linux:**
```bash
chmod +x run_with_flatlaf.sh
./run_with_flatlaf.sh
```

### 默认账户

系统首次运行时会自动创建默认管理员账户：

- **用户名**: `admin`
- **密码**: `admin123`
- **角色**: 管理员

## 📖 使用说明

### GUI 版本

图形化界面包含以下主要功能标签页：

1. **库存管理** - 商品添加、编辑、删除、补货、搜索
2. **购物车** - 商品添加、修改数量、删除、批量操作
3. **结账** - 支持多种支付方式、税率设置、促销应用
4. **会员管理** - 会员注册、积分、等级、折扣、余额充值
5. **交易记录** - 完整的交易历史记录和查询
6. **数据统计** - 销售额、交易量、平均客单价等统计
7. **交接班** - 班次开始/结束、交接班记录、收入统计
8. **用户管理** - 用户增删改查、权限管理
9. **系统设置** - 主题切换、税率配置、数据备份/恢复

### 快捷键

| 功能键 | 功能 |
|--------|------|
| F1 | 添加商品 |
| F2 | 补货 |
| F3 | 删除商品 |
| F4 | 搜索 |
| F5 | 刷新当前面板 |
| F6 | 分类管理 |
| F7 | 会员管理 |
| F8 | 结账 |
| F9 | 促销管理 |
| F10 | 库存预警 |
| F11 | 数据备份 |
| F12 | 数据恢复 |

| Ctrl 组合键 | 功能 |
|-------------|------|
| Ctrl+N | 添加商品 |
| Ctrl+S | 保存数据 |
| Ctrl+F | 搜索 |
| Ctrl+D | 导出数据 |
| Ctrl+R | 刷新当前面板 |
| Ctrl+Q | 退出程序 |
| Ctrl+A | 全选 |
| Ctrl+E | 编辑选中项 |
| Ctrl+B | 批量操作 |
| Ctrl+M | 会员管理 |
| Ctrl+T | 交易统计 |
| Ctrl+1~4 | 切换标签页 |

### CLI 版本

运行命令行版本：
```bash
java CashierSystem
```

按照屏幕提示进行操作即可。

## 📁 项目结构

```
hello/
├── 核心业务类
│   ├── CashierSystem.java           # 命令行版收银系统
│   ├── CashierSystemGUI.java        # GUI版收银系统（主程序）
│   ├── DataManager.java             # 数据管理类（文件读写）
│   ├── Product.java                 # 商品实体类
│   ├── Member.java                  # 会员实体类
│   ├── User.java                    # 用户实体类
│   ├── Transaction.java             # 交易实体类
│   ├── Category.java                # 分类实体类
│   ├── Promotion.java               # 促销实体类
│   ├── RechargeRecord.java          # 充值记录类
│   ├── OperationLog.java            # 操作日志类
│   └── Shift.java                   # 交接班记录类
│
├── 配置文件
│   ├── flatlaf-3.5.jar              # FlatLaf 界面框架
│   ├── compile_with_flatlaf.sh      # Mac/Linux 编译脚本
│   ├── run_with_flatlaf.sh          # Mac/Linux 运行脚本
│   ├── compile_with_flatlaf.bat     # Windows 编译脚本
│   └── run_with_flatlaf.bat         # Windows 运行脚本
│
├── data/                            # 数据目录（自动创建）
│   ├── inventory.txt                # 库存数据
│   ├── transactions.txt             # 交易记录
│   ├── members.txt                  # 会员数据
│   ├── users.txt                    # 用户数据
│   └── ...                          # 其他数据文件
│
└── 文档
    ├── README.md                    # 项目说明（本文件）
    ├── IFLOW.md                     # 详细项目文档
    ├── FLATLAF_README.md            # FlatLaf 使用说明
    └── LICENSE                      # 木兰宽松许可证 v2
```

## 🛠️ 开发

### 使用 IntelliJ IDEA

1. 打开 IntelliJ IDEA
2. 导入项目（选择 `hello.iml`）
3. 右键点击 `CashierSystemGUI.java`
4. 选择 "Run" 或 "Debug"

### 手动编译

```bash
# 编译所有 Java 文件（包含 FlatLaf）
javac -cp flatlaf-3.5.jar Category.java Product.java Transaction.java Member.java Promotion.java RechargeRecord.java User.java OperationLog.java Shift.java DataManager.java CashierSystemGUI.java

# 运行 GUI 版本
java -cp flatlaf-3.5.jar:. CashierSystemGUI

# 运行 CLI 版本
java CashierSystem
```

## 🎨 主题切换

系统支持三种主题：

- **浅色主题 (Light)**: 明亮清爽的界面
- **深色主题 (Dark)**: 护眼的深色界面
- **IntelliJ主题**: 类似IDE的专业风格

在系统设置中选择主题后，点击"应用主题"按钮即可生效。

## 🔒 权限系统

系统支持三种角色：

- **管理员 (admin)**: 完整的系统管理权限
- **收银员 (cashier)**: 日常收银操作权限
- **财务 (finance)**: 财务报表和数据统计权限

## 💡 特色功能

### 会员系统
- 会员等级自动升级（普通→银卡→金卡→钻石）
- 积分累计和折扣优惠
- 会员余额充值功能
- 生日特权检测

### 促销管理
- 支持多种促销类型（满减、折扣）
- 促销活动时间范围控制
- 促销启用/禁用管理

### 交接班管理
- 班次开始/结束记录
- 班次收入统计
- 多种支付方式收入统计（现金、微信、支付宝、银行卡）

### 数据安全
- 数据自动备份功能
- 操作日志完整记录
- 数据恢复支持

## 📝 许可证

本项目采用 **木兰宽松许可证 v2 (MulanPSL2)**

您可以根据木兰宽松许可证 v2 的条款和条件使用本软件。

获取木兰宽松许可证 v2 副本：
- 中文：http://license.coscl.org.cn/MulanPSL2

### FlatLaf 许可证

FlatLaf 使用 Apache 2.0 许可证
FlatLaf 官网: https://www.formdev.com/flatlaf/

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📧 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 Issue: https://gitee.com/nevell/hello/issues

## 🙏 致谢

- [FlatLaf](https://www.formdev.com/flatlaf/) - 现代化的 Swing Look and Feel
- [IntelliJ IDEA](https://www.jetbrains.com/idea/) - 强大的 Java IDE

---

**注意**: 本项目仅供学习和参考使用，不建议直接用于生产环境。如需用于生产环境，请进行充分的测试和安全加固。