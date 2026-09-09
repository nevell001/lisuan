# 狸算 (LiSuan) 收銀系統

[簡體中文](./README.md) | [English](./README_en.md) | 繁體中文

狸算 (LiSuan) 收銀系統是一個基於 JavaFX 17 的桌面 POS 收銀系統，面向零售門市的收銀、商品、會員、採購、庫存、退貨、報表、用戶權限、資料備份和硬體接入等日常經營場景。

**當前版本**: v2.6.0 | **最新更新**: 2026-08-29 | **測試覆蓋**: 520 個測試用例

> 測試口徑：`mvn -q clean verify` 預設執行 520 個用例（含測試 + SpotBugs + JaCoCo 門檻）；
> `LoginControllerUITest`（17 個用例）需要真實顯示環境，在桌面環境用
> `mvn -Pui-tests -Dtest=LoginControllerUITest test` 顯式執行，全量共 537 個。

![Java](https://img.shields.io/badge/Java-17-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-17.0.12-blue)
![Maven](https://img.shields.io/badge/Maven-3.8+-red)
![MySQL](https://img.shields.io/badge/MySQL-8.4-blue)
![License](https://img.shields.io/badge/License-MulanPSL2-blue)

---

## 功能概覽

### 收銀管理
- 結帳收銀、購物車增刪改、商品搜尋、會員手機號碼識別。
- 支援現金、微信、支付寶、銀行卡等支付方式。
- 支付支援 `disabled` / `mock` / `production` 三種模式；mock 模式可在支付彈窗一鍵「模擬支付成功」，本地閉環驗證整條支付鏈路。
- 會員折扣、積分、餘額消費與交易紀錄聯動。
- 發票/收據列印、列印預覽、錢箱開啟、快速鍵說明。
- 交班管理，記錄班次銷售額和收銀員業績。
- 觸屏版：掃描/輸入精確命中商品自動加入購物車（含成功/未找到/庫存不足提示音），連續快速錄入。
- 觸屏版底部狀態列顯示「觸屏版POS」、班次、日期(含星期)與時間；退出交班流程與 PC 版一致（交班 / 取消 / 確定）。

### 商品與庫存
- 商品增刪改查，支援商品編碼、條碼、分類、單位、規格、售價、成本價和庫存資訊。
- 商品名稱唯一約束，條碼允許重複。
- 庫存預警、低庫存查詢、快速入庫。
- 庫存盤點、盤點明細、差異處理和盤點狀態追蹤。
- 商品 CSV 匯入、Excel/PDF 匯出。

### 會員與客戶
- 會員註冊、編輯、查詢、儲值和餘額管理。
- 消費積分累計，會員等級自動升級。
- 等級折扣規則：普通 10 折、銀卡 9.5 折、金卡 9 折、鑽石 8.5 折。
- 儲值紀錄查詢，會員消費與退貨流程聯動。

### 採購管理
- 供應商資料維護。
- 採購訂單建立、編輯、查詢和狀態流轉。
- 採購審批流程。
- 採購入庫和入庫歷史。
- 採購報表統計。

### 退貨管理
- 基於原交易建立退貨訂單。
- 退貨商品、退貨原因、商品狀態和退款方式記錄。
- 退貨審批流程。
- 審批通過後恢復庫存，並按現金、餘額或積分等方式處理退款。
- 退貨訂單與退貨報表支援查詢、篩選、匯出和列印。

### 報表與統計
- 交易紀錄查詢與今日交易統計。
- 數據統計視圖。
- 採購報表、庫存報表、利潤分析、退貨報表。
- 支援 Excel 和 PDF 匯出。
- 利潤分析包含銷售額、成本、毛利、毛利率、品類和日趨勢等視圖。

### 用戶、權限與審計
- 預設角色：管理員、收銀員、財務。
- BCrypt 密碼加密，支援密碼重設和首次登入改密。
- 用戶啟用、停用、刪除和角色管理。
- 管理員可查看審計日誌，覆蓋登入認證、交易、退貨、庫存、採購、會員、用戶和系統設定等操作。

### 資料備份與恢復
- 選單內建資料備份和資料恢復。
- SQL 備份預設產生到 `backups/sql`。
- 備份服務支援資料庫、設定、日誌、發票和業務資料檔案打包。
- 支援本地備份、自動備份設定、保留策略和備份清理。
- API 側保留雲端備份設定欄位，便於對接物件儲存。
- 恢復備份前必須輸入管理員密碼確認，防止未授權覆蓋資料。

### REST API 與同步
- 內建 Javalin API 服務，預設連接埠 `8080`。
- 當前註冊 90+ HTTP 路由和 WebSocket 同步端點。
- 覆蓋認證、商品、會員、交易、庫存、報表、設定、發票、用戶、列印、支付、備份、國際化和同步狀態。
- Token 身份驗證、角色授權、請求限流和安全回應標頭。
- 支付回調路由（`/api/payment/notify/*`）對外公開，安全由渠道驗簽（微信 RSA/AES-GCM、支付寶 RSA、mock 密鑰）保證。
- WebSocket 同步端點：`/ws/sync`。

### 硬體支援
- ESC/POS 熱敏列印、網路列印、列印佇列和列印歷史。
- 印表機發現、連線、狀態檢測、預設印表機設定。
- USB HID 條碼掃描槍接入和掃描焦點管理。
- 電子支付二維碼生成與支付狀態查詢。

### 國際化與主題
- 支援簡體中文、英文、繁體中文。
- 執行階段文案、彈窗、表單校驗、狀態列和報表頁面持續國際化。
- 貨幣顯示國際化。
- 主題支援：LiSuan、淺色、深色，並相容舊版 IntelliJ 主題偏好。
- 內建 Noto Sans CJK SC，優化跨平台中文顯示。
- 左側功能導覽圖示已按模組語意整理，避免重複。
- 觸屏版支援一鍵快速切換語言，設定全域生效。

---

## 快速開始

### 環境要求
- JDK 17 或更高版本
- Maven 3.8 或更高版本
- MySQL 8.4 或相容的 MySQL 8.0/8.3
- Docker Compose 可選，用於快速啟動 MySQL

### 預設帳戶
- 用戶名：`admin`
- 初始密碼：`admin123`
- 首次登入後建議立即修改密碼

### 啟動資料庫

```bash
docker compose up -d mysql
```

資料庫初始化指令碼位於 `docker/mysql-init/00-init-complete.sql`。預設設定檔為 `config/database.properties`，生產環境建議透過環境變數儲存密碼：

```bash
export CASHIER_DB_PASSWORD=your_password
```

### 環境變數設定（.env）

專案支援用根目錄的 `.env` 檔案集中管理安裝與啟動設定，避免把密碼、金鑰寫進設定檔或指令碼。

```bash
cp .env.example .env
```

`.env.example` 是隨倉庫維護的範本，`.env` 是本地實際設定（已被 `.gitignore` 忽略，請勿提交真實金鑰）。主要變數：

| 變數 | 說明 |
|------|------|
| `APP_VERSION` / `APP_NAME` | 應用版本號與品牌名（`install.sh` 打包用） |
| `ENVIRONMENT` | `development`（以 root 連庫）或 `production`（以專用用戶 lisuan 連庫） |
| `DB_TYPE` | 資料庫類型：`docker` / `local` / `none`，用於 `install.sh` 引導 |
| `MYSQL_CONTAINER_NAME` / `MYSQL_IMAGE` | Docker MySQL 容器與映像檔 |
| `MYSQL_ROOT_PASSWORD` / `CASHIER_DB_PASSWORD` / `MYSQL_USER` / `MYSQL_DATABASE` | Docker 資料庫初始化帳號（`CASHIER_DB_PASSWORD` 同時作為應用執行密碼） |
| `DB_HOST` / `DB_PORT` | 資料庫連線位址與連接埠 |
| `TZ` | 時區（預設 `Asia/Shanghai`） |
| `JVM_OPTS` | JVM 啟動參數（`config/jvm.config` 優先） |
| `TOKEN_SECRET` | API Token 金鑰，至少 32 位強隨機字串 |
| `CORS_ALLOWED_ORIGINS` | API 允許的跨域來源，生產禁止 `*` |
| `CASHIER_DB_PASSWORD` / `CASHER_DB_PASSWORD` | 應用資料庫使用者密碼（Docker 建庫建使用者與應用執行共用；`CASHER_DB_PASSWORD` 為舊拼寫相容） |

使用說明：

- `install.sh` / `install.bat` 會整體載入 `.env` 用於資料庫初始化、設定生成和打包；未定義的變數會走互動式引導。
- 偵測到 `.env`（或 `ENVIRONMENT=production`）時，`install.sh` 不會把密碼寫入 `config/database.properties`，執行密碼統一由 `CASHIER_DB_PASSWORD` 提供。
- `start.sh` 只定向讀取 `CASHIER_DB_PASSWORD` / `CASHER_DB_PASSWORD` 兩個密碼變數覆蓋資料庫密碼，不把 `TOKEN_SECRET`、`CORS_ALLOWED_ORIGINS` 等佔位設定帶入執行環境（API 側仍透過系統環境變數傳遞）。
- `DataConfig.bat` 同樣讀取 `.env` 產生資料庫設定。
- **安全提示**：首次使用務必替換 `MYSQL_ROOT_PASSWORD`、`CASHIER_DB_PASSWORD`、`TOKEN_SECRET` 為各自獨立的強隨機值，並限制 `CORS_ALLOWED_ORIGINS`。舊版 `.env` 中的 `MYSQL_PASSWORD` 已統一為 `CASHIER_DB_PASSWORD`（指令碼仍向後相容讀取舊變數）。

### 開發執行

```bash
mvn clean compile
mvn javafx:run
```

### 打包執行

```bash
mvn clean package
java -jar target/lisuan-fx-2.6.0-jar-with-dependencies.jar
```

也可以使用萬用字元啟動：

```bash
java -jar target/lisuan-fx-*-jar-with-dependencies.jar
```

### Windows

首次安裝推薦執行：

```bat
install.bat
```

日常啟動：

```bat
start.bat
start.bat --gui
```

資料庫設定工具：

```bat
DataConfig.bat
```

### Linux / macOS

```bash
chmod +x install.sh start.sh
./install.sh
./start.sh
```

`install.sh` 會檢查 Java/Maven、建置可執行 JAR，並引導設定 Docker、本地或遠端 MySQL。`start.sh` 會優先啟動帶依賴的可執行 JAR。

---

## 常用指令

```bash
# 編譯
mvn clean compile

# 執行測試
mvn test

# 跳過測試打包
mvn clean package -DskipTests

# 執行指定測試類別
mvn test -Dtest=ProductDAOTest

# 執行指定測試方法
mvn test -Dtest=PasswordUtilTest#testHashPassword

# 靜態品質檢查 (SpotBugs)
mvn -q -DskipTests spotbugs:check
```

---

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

---

## 會員等級

| 等級 | 積分範圍 | 折扣 |
|------|----------|------|
| 普通會員 | 0-999 | 無折扣，按 10.0 計算 |
| 銀卡會員 | 1000-4999 | 9.5 折 |
| 金卡會員 | 5000-9999 | 9 折 |
| 鑽石會員 | 10000+ | 8.5 折 |

折扣計算方式：`消費金額 * (會員折扣 / 10.0)`。

---

## REST API

API 服務預設執行在 `8080` 連接埠。生產環境啟動 API 前必須設定安全參數：

```bash
export TOKEN_SECRET=at_least_32_characters_secret
export CORS_ALLOWED_ORIGINS=https://your-domain.example
```

主要介面分組：

| 分組 | 路徑 | 說明 |
|------|------|------|
| 健康檢查 | `/api/health` | 服務與資料庫狀態 |
| 認證 | `/api/auth/*` | 登入、重新整理 Token、登出、當前用戶 |
| 商品 | `/api/products/*` | 商品 CRUD、低庫存 |
| 會員 | `/api/members/*` | 會員 CRUD、手機號碼查詢、儲值 |
| 交易 | `/api/transactions/*` | 交易建立、查詢、退款、今日統計 |
| 庫存 | `/api/inventory/*` | 庫存列表、預警、盤點、庫存更新 |
| 報表 | `/api/reports/*` | 日報、月報、熱銷商品、支付方式統計 |
| 設定 | `/api/settings/*` | 系統設定讀寫 |
| 發票 | `/api/invoices/*` | 發票建立、查詢、作廢、列印紀錄 |
| 用戶 | `/api/users/*` | 用戶管理 |
| 列印 | `/api/printers/*` | 印表機發現、連線、狀態、列印 |
| 支付 | `/api/payment/*` | 支付建立、狀態查詢、退款、設定 |
| 備份 | `/api/backup/*` | 備份執行、恢復、下載、設定 |
| 國際化 | `/api/i18n/*` | 語言、訊息和語言包查詢 |
| 同步 | `/ws/sync`, `/api/sync/status` | 多終端同步和線上狀態 |

除 `/api/health`、`/api/auth/login` 和支付回調 `/api/payment/notify/*` 外，API 預設需要認證
（支付回調由微信/支付寶伺服器直接呼叫，無法攜帶本系統 Token，安全性由渠道驗簽保證）。

---

## 資料庫

專案使用 MySQL，連線池為 HikariCP。關鍵資料表包括：

| 資料表 | 說明 |
|--------|------|
| `products` | 商品資訊，商品名稱唯一 |
| `members` | 會員資訊 |
| `transactions`, `transaction_items` | 交易與交易明細 |
| `return_orders`, `return_order_items` | 退貨訂單與退貨明細 |
| `purchase_orders`, `purchase_order_items` | 採購訂單與明細 |
| `purchase_approvals` | 採購審批紀錄 |
| `purchase_inbound`, `purchase_inbound_items` | 採購入庫與明細 |
| `suppliers` | 供應商 |
| `inventory_check`, `inventory_check_items` | 庫存盤點 |
| `users` | 用戶與角色 |
| `shifts` | 交班紀錄 |
| `invoices` | 發票 |
| `payment_orders`, `refund_records` | 支付訂單與退款紀錄 |
| `promotions` | 促銷規則 |
| `backup_records`, `backup_config` | 備份紀錄與備份設定 |
| `operation_logs` | 審計日誌 |
| `settings` | 系統設定 |

資料庫版本說明見 `docker/mysql-init/DATABASE_VERSIONS.md`。

---

## 專案結構

```text
src/main/java/com/cashier/
├── api/           # Javalin REST API、認證、同步
├── component/     # 可複用 JavaFX 元件
├── constant/      # 應用、資料庫、系統屬性等常數
├── controller/    # JavaFX 控制器
├── dao/           # 資料存取物件
├── i18n/          # 國際化管理和鍵名常數
├── model/         # 業務實體
├── notification/  # 通知系統
├── printer/       # 列印設備與範本
├── scanner/       # 掃描槍接入
├── service/       # 業務服務
└── util/          # 資料庫、匯入匯出、日誌、主題等工具
```

```text
src/main/resources/
├── com/cashier/view/      # FXML 頁面
├── com/cashier/i18n/      # 語言資源
├── css/                   # LiSuan、淺色、深色主題
├── fonts/                 # 內建字型
└── images/                # 應用圖示和圖片資源
```

## 相關文件

- [CLAUDE.md](CLAUDE.md) — 專案架構、遷移狀態與開發約定
- [docs/GO_LIVE_CHECKLIST.md](docs/GO_LIVE_CHECKLIST.md) — 上線走查清單
- [docs/CREDENTIALS_CHECKLIST.md](docs/CREDENTIALS_CHECKLIST.md) — 上線憑證準備清單（資料庫/API/支付/雲端備份）
- [docs/DATABASE_INIT.md](docs/DATABASE_INIT.md) — 資料庫初始化說明

---

## 程式碼品質與安全

### 安全措施
- **密碼儲存**: 使用 BCrypt 加密，禁止明文儲存
- **SQL 注入防護**: 全部使用 `PreparedStatement` 參數化查詢
- **隨機數生成**: 安全敏感場景使用 `SecureRandom`
- **資源管理**: JDBC 連線使用 try-with-resources 防止洩漏
- **登入防暴力**: 登入失敗按用戶累計並鎖定，成功/過期自動重置
- **恢復保護**: 資料恢復前校驗管理員密碼
- **支付安全**: 回調驗簽 + 金額校驗 + 終態保護（已退款/關閉訂單拒絕遲到回調，重複回調冪等）
- **API 認證**: Token 基礎認證，24 小時過期
- **角色授權**: 三級權限（管理員、收銀員、財務）
- **請求限流**: 每 IP 每分鐘最多 60 次請求
- **安全回應標頭**: X-Content-Type-Options, X-Frame-Options, X-XSS-Protection

### 程式碼品質
- **單元測試**: 520 個測試用例（`mvn -q clean verify`），覆蓋 DAO、Service、工具類、並發安全與 API（全量 537，含需顯示環境的 UI 測試）
- **靜態檢查**: SpotBugs 高風險缺陷門禁
- **覆蓋率門檻**: JaCoCo 行覆蓋率 ≥10%
- **i18n 門檻**: 強制三套語言包 key 一致、`I18nKeys` 常數齊全、原始碼 i18n 呼叫 key 齊全
- **並發安全測試**: 樂觀鎖防庫存超賣、防會員餘額超扣
- **依賴管理**: Maven Enforcer 插件確保依賴一致性
- **日誌規範**: 統一使用 SLF4J + LoggerFactoryUtil
- **程式碼規範**: 遵循 CLAUDE.md 中的行為指南

---

## 開發約定

- 日誌統一使用 `LoggerFactoryUtil.getLogger()`。
- DAO 使用 `PreparedStatement`，並繼承 `BaseDAO`。
- 跨 DAO 的業務交易統一透過 `DatabaseManager.executeBooleanTransaction()` 等交易入口處理。
- 新增介面優先使用 FXML + Controller，並補齊三套語言資源。
- 新增 i18n key 優先放入 `I18nKeys`，避免控制器裡散落字串。
- 新增非 i18n 常數優先收口到對應常數類別，避免重複字面量。
- 程式碼品質警告修復後至少執行編譯，涉及公共邏輯時補跑測試。

---

## 最近更新

### v2.6.0

#### 2026-08-29
- 列印設定接線：設定頁的「啟用列印 / 印表機名稱 / 紙張 / Logo」現對觸屏版結帳小票生效（未啟用則不出票）；設定頁新增「測試列印」按鈕
- 掃描槍測試：設定頁新增「掃描槍測試」輸入框，與設備一致校驗歸一化、空資料與 128 字元上限

#### 2026-08-28
- 設定安全：統一資料庫密碼變數為 `CASHIER_DB_PASSWORD`（相容舊拼寫 `CASHER_DB_PASSWORD` 與舊版 `MYSQL_PASSWORD`），設定檔不再保存明文密碼，執行密碼由 `.env`/環境變數注入
- Docker：初始化 SQL 不再寫死庫名，支援自訂 `MYSQL_DATABASE`；compose 變數化並掛載 `my.cnf`
- 腳本修復：install.sh JDBC characterEncoding 改為 UTF-8；release.bat 中文輸出與版本號門檻修復

#### 2026-08-23
- DAO 層全部完成實例化遷移（`XxxDAORefactored extends BaseDAO` + `DAOFactory`），移除靜態 DAO 與死碼
- 支付：支援 mock 模式「模擬支付成功」按鈕，介面內閉環測試支付；支付回調路由公開並由渠道驗簽保證安全
- 支付回調業務加固：金額校驗、重複回調冪等、已退款/關閉訂單拒絕遲到回調
- 恢復備份前必須輸入管理員密碼確認
- 觸屏版：掃描/輸入精確命中商品自動加購 + 成功/未找到/庫存不足提示音；退出交班流程（交班/取消/確定，交班後直接退出）；底部狀態列顯示「觸屏版POS」、班次與日期星期時間；支付彈窗增加模擬支付按鈕
- PC 版：主介面退出改為退出到登入介面（登入頁退出仍關閉應用）；結帳新增商品成功後清空搜尋欄
- 程式碼品質：修復支付回調被鑑權攔截、健康檢查連線洩漏、電子支付彈窗 NPE 等問題；i18n 完整性門檻、並發安全測試、token 過期測試
- 新增上線走查清單 `docs/GO_LIVE_CHECKLIST.md` 與憑證準備清單 `docs/CREDENTIALS_CHECKLIST.md`

#### 2026-07-24
- 觸屏版新增語言切換功能：工具列一鍵切換簡體中文/英文/繁體中文
- 語言偏好優化：同時保存用戶偏好和全域預設，新用戶自動使用系統語言
- 修復語言切換不生效問題：統一語言偏好儲存邏輯，優先讀取用戶偏好、全域預設、系統預設
- I18nManager 改進：使用語言標籤匹配替代 Locale 物件比較，提升跨平台相容性
- 程式碼品質統一：LanguagePreferenceDAO 日誌系統從 java.util.logging 遷移至 SLF4J
- 觸屏版新增混合支付提示：部分現金支付時強制繼續使用現金完成，避免記帳錯誤

### v2.5.9 (2026-07-19)
- 安全加固：支付訂單和退款記錄使用 `SecureRandom` 產生隨機碼
- 修復 FormValidator DISCOUNT 驗證規則邊界值問題（10.1 應無效）
- 修復 DatabaseManager JDBC 資源洩漏問題（ResultSet 未關閉）
- 依賴更新：Jackson 2.18.2, SLF4J 2.0.16, Mockito 5.15.2, JUnit 5.11.3, H2 2.3.232
- 測試增強：新增 FormValidatorTest (33 個測試) 和 LoginControllerUITest (17 個測試)
- 380 個測試用例全部通過，測試覆蓋率持續提升

### v2.5.9-maintenance (2026-07-05)
- 修復了 API Token 的安全測試失敗問題
- 優化了測試環境下的 Mockito 配置
- 強化了生產環境配置模板的安全提示
- 統一了資料庫初始化指令碼，支援更多功能模組的開箱即用
- 優化了 README 文檔結構，提升易讀性

### v2.5.9-maintenance (2026-07-04)
- 整理 `I18nKeys`，將常用國際化鍵名集中管理。
- 收口系統屬性、資料庫設定、資源包名、日期時間格式等非 i18n 常數。
- 修復多處程式碼品質警告，包括複雜度、重複字串、switch/default、資源關閉和靜態檢查問題。
- 更新左側功能導覽圖示，保證分組和功能入口圖示不重複。
- README 按當前功能、API、資料庫和維護流程重新整理。

### v2.5.9 (2026-06-20)
- 國際化完善：統一簡體中文、英文和繁體中文資源，補齊彈窗、狀態、日期、審批及表單校驗提示。
- 退貨流程優化：增加快捷日期篩選，修復查看原交易、列印單據、完成退貨和匯出提示的多語言顯示。
- 採購與盤點修復：修復入庫歷史列不顯示、盤點類型文字截斷及空表提示未跟隨應用語言的問題。
- 表單顯示修復：移除商品新增/編輯頁空錯誤標籤產生的紅色豎線，並在地化校驗錯誤。
- 字型與主題優化：預設使用 LiSuan 主題，繁體中文優先載入 Noto Sans CJK SC 字形並完善跨平台回退。
- 語言精簡：移除日語、韓語，僅保留簡體中文、英文和繁體中文。

### v2.5.7-l10n (2026-06-20)
- 語言包精簡為簡體中文、英文、繁體中文。
- 補齊缺失的國際化鍵值，減少頁面翻譯回退。
- 優化彈窗、二級頁面、狀態列、提示語等文案顯示。
- 調整部分頁面頂部標籤和輸入區域的佈局寬度。

### v2.5.7 (2026-06-12)
- 支援 development/production 環境區分。
- 開發環境使用 root，生產環境使用 lisuan 專用用戶。
- FormValidator 安全解析方法，防止 NumberFormatException。
- 安裝指令碼支援 `.env` 和自動環境檢測。
- Windows 設定工具支援環境變數。
- 指令碼顯示品牌統一為 LiSuan System。

### v2.5.6 (2026-06-10)
- 品牌名稱統一為「狸算 (LiSuan) 收銀系統」。
- 更新用戶介面、指令碼、小票列印、圖示和 Docker 容器品牌資訊。

### v2.5.5 (2026-06-09)
- 標籤頁寬度和關閉按鈕視覺優化。
- 新增字型大小調整功能。
- 修復 fcitx5 輸入法相容性。
- 優化繁體中文顯示和多平台字型回退鏈。

### v2.5.4 (2026-05-21)
- 新增 GUI 資料庫設定工具。
- 優化 Windows 分發包和安裝指令碼。

### v2.5.3 (2026-05-15)
- Windows 平台優化。
- 啟動體驗增強。
- 同步邏輯增強。
- DAO 重構完成。
- 修復資源清理和記憶體洩漏問題。

---

## 故障排除

**應用程式無法啟動**
- 檢查 JDK 是否為 17 或更高版本。
- 檢查 MySQL 是否正在執行。
- 檢查 `config/database.properties`。
- 查看 `logs/` 目錄下的日誌檔案。

**資料庫連線失敗**
- 確認資料庫位址、連接埠、庫名、用戶名和密碼。
- 如果使用環境變數密碼，確認 `CASHIER_DB_PASSWORD` 已設定。
- 若用 `.env` 管理設定，確認已執行 `cp .env.example .env` 且 `DB_HOST`/`DB_PORT`/`MYSQL_*` 與本地一致。
- Docker 環境可先執行 `docker compose up -d mysql`。

**API 服務無法啟動**
- 確認已設定至少 32 字元的 `TOKEN_SECRET`。
- 生產環境不要使用 `CORS_ALLOWED_ORIGINS=*`。
- 查看日誌中的連接埠佔用或設定錯誤。

**掃描槍無法工作**
- 確認掃描槍已連線並處於 USB HID 模式。
- 確認當前頁面焦點未被其他輸入控制項佔用。

**列印無回應**
- 檢查印表機連線、驅動和紙張狀態。
- 查看印表機狀態檢測和列印歷史。
- 網路列印需確認 IP、連接埠和防火牆設定。

**中文顯示異常**
- 優先確認內建 Noto Sans CJK SC 是否隨資源打包。
- Linux 環境可額外安裝中文字型包。

---

## 授權條款

木蘭寬鬆許可證 v2 (MulanPSL2)

---

**儲存庫**: https://gitee.com/nevell/lisuan.git

**問題回饋**: https://gitee.com/nevell/lisuan/issues
