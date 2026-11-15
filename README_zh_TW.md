# 狸算 (LiSuan) 收銀系統

[簡體中文](./README.md) | [English](./README_en.md) | 繁體中文

狸算 (LiSuan) 收銀系統是一個基於 JavaFX 17 的桌面 POS 收銀系統，涵蓋零售門市的收銀、商品、會員、採購、庫存、退貨、報表、用戶權限、資料備份和硬體接入等日常經營場景。

**當前版本**: v2.6.0 | **最新更新**: 2026-11-15

![Java](https://img.shields.io/badge/Java-17-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-17.0.12-blue)
![Maven](https://img.shields.io/badge/Maven-3.8+-red)
![MySQL](https://img.shields.io/badge/MySQL-8.4-blue)
![License](https://img.shields.io/badge/License-MulanPSL2-blue)

## 功能概覽

| 模組 | 能力 |
|------|------|
| 收銀 | 商品搜尋、購物車、會員手機號碼識別；現金/微信/支付寶/銀行卡支付；小票列印、錢箱、交班；支付支援 `disabled` / `mock` / `production`，mock 模式可一鍵模擬支付成功 |
| 觸屏版 | 掃描或輸入精確命中自動加購（含成功/未找到/庫存不足提示音）、一鍵切換語言、底部顯示班次與日期時間，交班流程與 PC 版一致 |
| 商品與庫存 | 商品增刪改查、名稱唯一（條碼可重複）、庫存預警與快速入庫、庫存盤點、CSV 匯入、Excel/PDF 匯出 |
| 會員 | 註冊、查詢、儲值、積分累計與等級自動升級（普通/銀卡/金卡/鑽石），消費與退貨聯動 |
| 採購 | 供應商、採購訂單與審批、採購入庫與入庫歷史、採購報表 |
| 退貨 | 基於原交易退貨，記錄退貨原因、商品狀態和退款方式；審批通過後恢復庫存並按現金、餘額或積分退款 |
| 報表 | 交易統計、採購/庫存/利潤/退貨報表，支援 Excel 與 PDF 匯出 |
| 用戶與審計 | 管理員/收銀員/財務三種角色、BCrypt 密碼、密碼重設與首次登入改密、審計日誌 |
| 備份與恢復 | 本地備份（資料庫/設定/日誌/發票/業務資料）、自動備份與保留策略；恢復前需管理員密碼確認；未接入的雲端備份介面回傳 501 |
| REST API | Javalin 服務預設 `8080`，90+ 路由與 `/ws/sync` WebSocket，Token 認證、角色授權、限流與安全回應標頭 |
| 硬體 | ESC/POS 熱感與網路列印、印表機發現與狀態檢測、USB HID 掃描槍、支付二維碼 |
| 國際化與主題 | 簡體中文/英文/繁體中文；LiSuan/淺色/深色主題；內建 Noto Sans CJK SC |

## 快速開始

### 環境要求

- JDK 17 或更高版本
- Maven 3.8 或更高版本
- MySQL 8.4（相容 8.0/8.3）
- Docker Compose 可選，用於快速啟動 MySQL

### 設定與啟動資料庫

複製 `.env.example` 為根目錄 `.env`，至少把 `MYSQL_ROOT_PASSWORD`、`CASHIER_DB_PASSWORD`、`TOKEN_SECRET` 改成各自獨立的強隨機值，然後啟動資料庫：

```bash
cp .env.example .env
docker compose up -d mysql
```

資料庫密碼的來源順序為 **行程環境變數 → 工作目錄 `.env` → `config/database.properties`**；設定檔中的 `db.password` 一律留空，密碼不寫入該檔案。初始化指令碼 `docker/mysql-init/00-init-complete.sql` 只在資料卷為空時執行一次，後續表結構由應用程式啟動時自動升級。

### 首次執行

空庫首次啟動會彈出**首次執行精靈**，由操作者當場建立管理員帳號並設定密碼。倉庫不提供任何預設口令。

### 執行

```bash
# 開發執行
mvn clean compile
mvn javafx:run

# 打包執行
mvn clean package
java -jar target/lisuan-fx-*-jar-with-dependencies.jar
```

Windows 首次安裝執行 `install.bat`，日常用 `start.bat`（或 `start.bat --gui`）啟動，資料庫設定工具為 `DataConfig.bat`。Linux/macOS 執行 `./install.sh`，之後用 `./start.sh` 啟動。

> 生產環境啟動 API 前必須設定至少 32 字元的 `TOKEN_SECRET`，並把 `CORS_ALLOWED_ORIGINS` 限制為實際網域（禁止 `*`）。

## 快速鍵

| 快速鍵 | 功能 |
|--------|------|
| `F1` | 新增商品到購物車 |
| `Delete` | 移除選取商品 |
| `Ctrl+L` | 清空購物車 |
| `F8` | 現金支付 |
| `Ctrl+1` | 微信支付 |
| `Ctrl+2` | 支付寶支付 |
| `Ctrl+3` | 銀行卡支付 |
| `Ctrl+F` | 聚焦搜尋框 |
| `Ctrl+M` | 聚焦會員手機號碼 |
| `Ctrl+/` | 顯示快速鍵說明 |

## 會員等級

| 等級 | 積分範圍 | 折扣 |
|------|----------|------|
| 普通會員 | 0-999 | 無折扣，按 10.0 計算 |
| 銀卡會員 | 1000-4999 | 9.5 折 |
| 金卡會員 | 5000-9999 | 9 折 |
| 鑽石會員 | 10000+ | 8.5 折 |

折扣計算方式：`消費金額 * (會員折扣 / 10.0)`。

## REST API

API 服務預設監聽 `8080`，目前註冊 90+ HTTP 路由和 WebSocket 同步端點 `/ws/sync`，涵蓋認證、商品、會員、交易、庫存、報表、設定、發票、用戶、列印、支付、備份、國際化和同步狀態。

除 `/api/health`、`/api/auth/login` 和支付回調 `/api/payment/notify/*` 外，API 預設需要認證。支付回調由渠道伺服器直接呼叫，無法攜帶本系統 Token，安全性由微信/支付寶驗簽保證。

## 安全要點

- 密碼使用 BCrypt 儲存，登入失敗按用戶累計並鎖定
- 所有 SQL 使用 `PreparedStatement`，安全敏感場景使用 `SecureRandom`
- 支付回調驗簽 + 金額校驗 + 終態保護（重複回調冪等）
- API Token 認證（24 小時過期）、三級角色授權、每 IP 每分鐘 60 次限流
- 庫存與會員餘額使用樂觀鎖，防止超賣和超額扣款

## 資料庫

專案使用 MySQL，連線池為 HikariCP。表結構由應用程式啟動時自動建立與升級，完整初始化指令碼見 `docker/mysql-init/00-init-complete.sql`，版本說明見 `docker/mysql-init/DATABASE_VERSIONS.md`。

## 相關文件

- [CLAUDE.md](CLAUDE.md) — 專案架構、遷移狀態與開發約定
- [AGENTS.md](AGENTS.md) — 開發速查與驗證命令
- [docs/GO_LIVE_CHECKLIST.md](docs/GO_LIVE_CHECKLIST.md) — 上線走查清單
- [docs/HARDWARE_ACCEPTANCE.md](docs/HARDWARE_ACCEPTANCE.md) — 硬體驗收測試表（印表機/掃描槍/錢箱/觸屏/客顯）
- [docs/CREDENTIALS_CHECKLIST.md](docs/CREDENTIALS_CHECKLIST.md) — 上線憑證準備清單（資料庫/API/支付/備份）
- [docs/DATABASE_INIT.md](docs/DATABASE_INIT.md) — 資料庫初始化說明
- [docker/README.md](docker/README.md) — Docker MySQL 部署與備份

## 最近更新

### v2.6.0

**2026-08-29**
- 列印設定接線：設定頁的「啟用列印 / 印表機名稱 / 紙張 / Logo」對觸屏版結帳小票生效（未啟用則不出票），並新增「測試列印」
- 設定頁新增「掃描槍測試」，與設備一致校驗歸一化、空資料和 128 字元上限

**2026-08-28**
- 資料庫密碼統一為 `CASHIER_DB_PASSWORD`（相容舊拼寫），設定檔不再保存明文密碼，改由 `.env` / 環境變數注入
- Docker 初始化 SQL 不再寫死庫名，支援自訂 `MYSQL_DATABASE`

**2026-08-23**
- 支付支援 mock 模式「模擬支付成功」，介面內即可閉環測試；回調增加金額校驗、重複回調冪等和終態保護
- 恢復備份前必須輸入管理員密碼確認
- 觸屏版：精確掃描自動加購與提示音、交班退出流程、底部狀態列、支付彈窗模擬支付；PC 版退出回到登入介面
- 修復支付回調被鑑權攔截、健康檢查連線洩漏、電子支付彈窗 NPE

**2026-07-24**
- 觸屏版支援工具列一鍵切換簡體中文/英文/繁體中文，語言偏好全域生效
- 修復語言切換不生效問題，新用戶自動使用系統語言
- 觸屏版部分現金支付時提示繼續用現金完成，避免記帳錯誤

更早版本的功能與修復記錄見 Git 提交歷史。

## 故障排除

**應用程式無法啟動**：確認 JDK 17+、MySQL 正在執行，並查看 `logs/` 下的日誌。

**資料庫連線失敗**：確認 `CASHIER_DB_PASSWORD` 已在環境變數或根目錄 `.env` 中提供（設定檔不保存密碼），且 `DB_HOST`/`DB_PORT`/`MYSQL_*` 與本地一致；Docker 環境可先執行 `docker compose up -d mysql`。

**API 服務無法啟動**：確認 `TOKEN_SECRET` 至少 32 字元、`CORS_ALLOWED_ORIGINS` 未使用 `*`，並檢查日誌中的連接埠佔用。

**掃描槍無回應**：確認掃描槍處於 USB HID 模式，且當前焦點未被其他輸入控制項佔用。

**列印無回應**：檢查印表機連線、驅動和紙張狀態；網路列印確認 IP、連接埠和防火牆設定。

**中文顯示異常**：確認內建 Noto Sans CJK SC 隨資源打包，Linux 環境可額外安裝中文字型包。

---

## 授權條款

木蘭寬鬆許可證 v2 (MulanPSL2)

**儲存庫**: https://gitee.com/nevell/lisuan.git

**問題回饋**: https://gitee.com/nevell/lisuan/issues
