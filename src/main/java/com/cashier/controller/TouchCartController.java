package com.cashier.controller;

import com.cashier.CashierSystemFXApplication;
import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.i18n.I18nManager;
import com.cashier.model.CartItem;
import com.cashier.model.Category;
import com.cashier.model.Member;
import com.cashier.model.Product;
import com.cashier.model.Promotion;
import com.cashier.model.Transaction;
import com.cashier.model.User;
import com.cashier.printer.PrintUtil;
import com.cashier.printer.PrinterManager;
import com.cashier.service.TransactionService;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.DateTimeFormats;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.StatusBarManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.shape.Circle;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import javafx.scene.control.Toggle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.cashier.model.PaymentOrder;
import com.cashier.model.HoldOrder;
import com.cashier.model.Shift;
import com.cashier.dao.HoldOrderDAORefactored;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import com.cashier.service.PaymentService;
import com.cashier.util.QrCodeImageUtil;
import com.cashier.util.ThemeUtils;
import com.cashier.util.UIOptimizer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 触屏版收银控制器
 *
 * <p>三栏布局(分类导航 + 商品卡片 + 购物车/摘要/支付)的触屏收银台。
 * 自管 {@link ObservableList}&lt;{@link CartItem}&gt; 与库存快照 {@code inventoryMap}，
 * 直接复用主项目 DAO/Service 层完成核心收银闭环。</p>
 *
 * <p>首版范围:分类、商品卡片、购物车增删改、4 种支付、会员查询折扣、结账、打印。
 * 不含挂单、促销、交接班(交接班由 PosModeView 底栏处理)。</p>
 */
public class TouchCartController implements CartViewHost {
    private static final Logger logger = LoggerFactoryUtil.getLogger(TouchCartController.class);

    /** 小票打印串行执行器（daemon）：网络打印可能阻塞数秒，且多笔交易打印不得交叠 */
    private static final java.util.concurrent.ExecutorService RECEIPT_PRINTER =
        java.util.concurrent.Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "tpos-receipt-printer");
            thread.setDaemon(true);
            return thread;
        });
    private static final I18nManager i18n = I18nManager.getInstance();
    private static final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();
    private static final HoldOrderDAORefactored holdOrderDAO = DAOFactory.getInstance().getHoldOrderDAO();
    private static final String SCAN_SUCCESS_SOUND = "/sounds/scan_success.wav";
    private static final String SCAN_ERROR_SOUND = "/sounds/scan_error.wav";
    private static final String SCAN_NOT_FOUND_SOUND = "/sounds/scan_not_found.wav";

    @FXML private VBox categoryBox;
    @FXML private TextField searchField;
    @FXML private FlowPane productGrid;
    @FXML private Button clearBtn;
    @FXML private VBox cartList;
    @FXML private TextField memberPhoneField;
    @FXML private Label memberInfoLabel;
    @FXML private Label totalQtyLabel;
    @FXML private Label totalAmountLabel;
    @FXML private Label discountLabel;
    @FXML private Label finalAmountLabel;

    // 顶部工具栏
    @FXML private Label storeNameLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Circle avatarCircle;
    @FXML private Label avatarText;
    @FXML private Button exitButton;

    // 购物车
    @FXML private Label cartCountLabel;

    // 底部状态栏
    @FXML private Label dateLabel;
    @FXML private Label timeLabel;
    @FXML private Button shiftButton;
    @FXML private Label shiftInfoLabel;

    private CashierSystemFXApplication application;
    private User currentUser;

    /** 时钟更新定时器 */
    private javafx.animation.Timeline clockTimeline;

    private final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    /** 库存快照,key = Product.name({@code TransactionService.executeTransaction} 契约要求) */
    private final Map<String, Product> inventoryMap = new HashMap<>();
    private Member currentMember;
    /** 当前选中的最优促销,与标准收银台口径一致 */
    private Promotion appliedPromotion;
    /** 当前选中的分类名,null 表示"全部" */
    private String currentCategoryName = null;
    /** 当前搜索关键字,null 或空表示无搜索 */
    private String currentKeyword = null;
    /** 支付进行中标志,防止重复结算 */
    private boolean paymentInProgress = false;
    /** 现金部分支付累计金额 */
    private BigDecimal cashReceivedAmount = BigDecimal.ZERO;
    /** 扫码/输入停顿后检查“未找到商品”的延迟任务 */
    private PauseTransition notFoundHint;
    /** 搜索输入防抖：停止连续输入 300ms 后才执行精确匹配/查询，避免逐键 3+1 次 DB */
    private PauseTransition searchDebounce;
    /** 商品列表加载序号：只允许最新一次加载结果刷新网格，避免过期结果覆盖 */
    private final AtomicLong productQuerySequence = new AtomicLong();

    @FXML
    private void initialize() {
        logger.info("触屏版收银视图初始化");
        startClock();
        // 分类加载完成后默认选中「热销推荐」，由选中事件驱动首次商品加载（见 loadCategories）
        loadCategories();
        refreshCartView();
        updateSummary();
        setupShortcuts();
        updateShiftInfo();
        // 输入时实时精确匹配：完整条码/名称/编号命中唯一商品立即自动加购（无需回车）。
        // 增加防抖：连续键入/扫码期间只调度一次，停顿 300ms 后执行，避免每键 3+1 次 DB 查询
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String keyword = newVal == null ? null : newVal.trim();
            scheduleSearchAction(keyword);
        });
    }

    /** 搜索防抖调度：停止输入 300ms 后执行一次查询/加购动作 */
    private void scheduleSearchAction(String keyword) {
        if (searchDebounce != null) {
            searchDebounce.stop();
        }
        searchDebounce = new PauseTransition(Duration.millis(300));
        searchDebounce.setOnFinished(event -> performSearch(keyword));
        searchDebounce.play();
    }

    /** 执行搜索：命中唯一商品立即加购；否则按关键字刷新商品列表 */
    private void performSearch(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            currentKeyword = null;
            loadProducts(currentCategoryName);
            return;
        }
        // 精确匹配最多 3 次查库，同样放后台，避免键入/扫码时卡住界面
        UIOptimizer.runInBackground(
            () -> findExactProduct(keyword),
            exact -> {
                if (exact != null) {
                    addToCart(exact); // 成功后自动清空搜索栏
                    return;
                }
                scheduleNotFoundHint(keyword);
                currentKeyword = keyword;
                loadProducts(currentCategoryName);
            },
            e -> {
                logger.error("实时精确匹配商品失败", e);
                currentKeyword = keyword;
                loadProducts(currentCategoryName);
            });
    }

    // ===== 时钟更新 =====

    private void startClock() {
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateDateTime()));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
        updateDateTime(); // 立即更新一次
    }

    /** 清理资源（登出/切换视图时调用），停止时钟动画与搜索防抖，防止泄漏 */
    public void cleanup() {
        if (clockTimeline != null) {
            clockTimeline.stop();
            clockTimeline = null;
        }
        if (searchDebounce != null) {
            searchDebounce.stop();
            searchDebounce = null;
        }
    }

    private void updateDateTime() {
        LocalDateTime now = LocalDateTime.now();
        if (dateLabel != null) {
            dateLabel.setText(now.format(DateTimeFormats.FULL_DATE));
        }
        if (timeLabel != null) {
            timeLabel.setText(now.format(DateTimeFormats.TIME));
        }
    }

    /** 更新右下角班次信息（与 PC 版 MainView 一致） */
    private void updateShiftInfo() {
        if (shiftInfoLabel == null) {
            return;
        }
        // 班次查询放后台，避免初始化时在 FX 线程查库
        UIOptimizer.runInBackground(
            () -> DAOFactory.getInstance().getShiftDAO().findActiveShift(),
            activeShift -> {
                if (activeShift != null) {
                    String startTime = LocalDateTime.ofInstant(activeShift.startTime, ZoneId.systemDefault())
                        .format(DateTimeFormats.TIME_HOUR_MINUTE);
                    shiftInfoLabel.setText(i18n.get("runtime.shift_summary",
                        activeShift.shiftId, activeShift.operatorName, startTime));
                } else {
                    shiftInfoLabel.setText(i18n.get("status.shift_not_started"));
                }
            },
            e -> {
                logger.error("更新班次信息失败", e);
                shiftInfoLabel.setText(i18n.get("runtime.shift_unknown"));
            });
    }

    // ===== 按钮事件 =====

    @FXML
    private void handleLanguageSwitch() {
        logger.info("语言切换按钮被点击");
        showLanguageSelectionDialog();
    }

    @FXML
    private void handleExit() {
        // 触屏版退出确认：大按钮 + 三选（先交班 / 取消 / 确认退出）。
        // 三个按钮放在显式 HBox 中统一排版，确保无论字号/间距都完整显示。
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(i18n.get("common.confirm"));
        dialog.setHeaderText(null);
        dialog.getDialogPane().getStyleClass().add("fs-18");

        String message = isCartEmpty()
            ? i18n.get("tpos.exit_confirm")
            : i18n.get("runtime.cart_exit_confirm");

        Button shiftFirstBtn = new Button(i18n.get("tpos.exit_shift_first"));
        Button cancelBtn = new Button(i18n.get("common.cancel"));
        Button confirmBtn = new Button(i18n.get("tpos.exit_confirm_btn"));
        for (Button b : new Button[]{shiftFirstBtn, cancelBtn, confirmBtn}) {
            b.setPrefSize(180, 56);
            b.getStyleClass().add("title-md");
        }
        shiftFirstBtn.setCancelButton(false);
        // 只设置结果并让对话框正常关闭返回；交班窗口在 showAndWait 返回后打开，
        // 避免在对话框模态事件循环里嵌套新窗口导致对话框残留
        shiftFirstBtn.setOnAction(e -> dialog.setResult("shift"));
        cancelBtn.setOnAction(e -> dialog.setResult("cancel"));
        confirmBtn.setOnAction(e -> dialog.setResult("exit"));

        HBox buttons = new HBox(16, shiftFirstBtn, cancelBtn, confirmBtn);
        buttons.setAlignment(javafx.geometry.Pos.CENTER);
        buttons.setPrefWidth(3 * 180 + 2 * 16);
        buttons.setMinWidth(3 * 180 + 2 * 16);
        buttons.setMaxWidth(3 * 180 + 2 * 16);

        VBox content = new VBox(20, message(message), buttons);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.setPadding(new Insets(20, 30, 20, 30));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().clear();
        dialog.getDialogPane().setPrefWidth(3 * 180 + 2 * 16 + 100);

        ThemeUtils.applyDialogTheme(dialog.getDialogPane());
        if (productGrid.getScene() != null) {
            dialog.initOwner(productGrid.getScene().getWindow());
        }

        String result = dialog.showAndWait().orElse("cancel");
        if ("shift".equals(result)) {
            // 对话框已完全关闭，打开交接班窗口（退出模式），交班完成后直接退出
            com.cashier.controller.ShiftController shiftController = openShiftDialog();
            if (shiftController != null && shiftController.isShiftEnded()) {
                StatusBarManager.updateSuccess("交接班完成，正在退出…");
                if (application != null) {
                    application.logoutToLoginView();
                }
            }
        } else if ("exit".equals(result) && application != null) {
            application.logoutToLoginView();
        }
    }

    private Label message(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("fs-18");
        return label;
    }

    /**
     * 打开交接班弹窗（模态），返回 ShiftController 供调用方判断交班状态。
     *
     * @return ShiftController 实例，加载失败时返回 null
     */
    /**
     * 打开交接班弹窗（模态），返回 ShiftController 供调用方判断交班状态。
     *
     * @return ShiftController 实例，加载失败时返回 null
     */
    private com.cashier.controller.ShiftController openShiftDialog() {
        logger.info("交接班按钮被点击");
        try {
            javafx.fxml.FXMLLoader loader = com.cashier.util.FXMLUtils.loadFXMLLoader("/com/cashier/view/ShiftView.fxml");
            VBox root = loader.load();
            logger.info("ShiftView 加载成功");

            com.cashier.controller.ShiftController controller = loader.getController();
            controller.setCurrentUser(currentUser);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle(i18n.get("runtime.shift_handover"));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            // Scene 显式尺寸：new Scene(root) 不带尺寸会 sizeToScene，把 VBox.vgrow=ALWAYS 的表格区
            // 算成 0，弹窗只剩标题栏。显式 1100x750 让内容区有空间渲染。
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 1100, 750);
            // 复制样式表以确保主题一致
            if (searchField.getScene() != null) {
                scene.getStylesheets().addAll(searchField.getScene().getStylesheets());
            }
            stage.setScene(scene);
            stage.showAndWait();

            return controller;

        } catch (java.io.IOException e) {
            logger.error("加载交接班界面失败", e);
            StatusBarManager.updateError(i18n.get("label.error") + ": " + e.getMessage());
            return null;
        }
    }

    @FXML
    private void handleShift() {
        com.cashier.controller.ShiftController controller = openShiftDialog();
        if (controller != null) {
            StatusBarManager.updateSuccess("交接班操作完成");
            updateShiftInfo();
        }
    }

    // ===== 快捷键 =====

    private void setupShortcuts() {
        Platform.runLater(() -> {
            Scene scene = searchField.getScene();
            if (scene != null) {
                bindShortcuts(scene);
                // 默认聚焦搜索框
                focusSearchField();
            } else {
                searchField.sceneProperty().addListener((obs, o, n) -> {
                    if (n != null) {
                        bindShortcuts(n);
                        // 默认聚焦搜索框
                        focusSearchField();
                    }
                });
            }
        });
    }

    /**
     * 绑定快捷键。对齐非触屏版 CartController 约定（F8 现金、Ctrl+1/2/3 支付、Ctrl+F/L/M 等），
     * 与 UI 按钮文案 "(F8)"、"(Ctrl+1)" 保持一致。addEventFilter(capture 阶段)优先处理。
     *
     * 快捷键表：F2 挂单 / F3 取单 / F6 交接班 / F8 现金 /
     * Ctrl+F 搜索 / Ctrl+L 清空 / Ctrl+M 会员 / Ctrl+1 微信 / Ctrl+2 支付宝 / Ctrl+3 银行卡 /
     * Delete 删末项 / Enter 搜索 / Esc 清搜索或退出
     */
    private void bindShortcuts(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();
            boolean ctrl = event.isControlDown();

            // F2 - 挂单 / F3 - 取单
            if (code == KeyCode.F2) {
                event.consume();
                handleHoldOrder();
            } else if (code == KeyCode.F3) {
                event.consume();
                handleRecallOrder();
            // F6 - 交接班
            } else if (code == KeyCode.F6) {
                event.consume();
                handleShift();
            // F8 - 现金支付（对齐 CartController 与 UI "现金支付 (F8)" 文案）
            } else if (code == KeyCode.F8) {
                event.consume();
                handleCashPayment();
            // Ctrl+F - 搜索 / Ctrl+L - 清空 / Ctrl+M - 会员手机号
            } else if (ctrl && code == KeyCode.F) {
                event.consume();
                focusSearchField();
                searchField.selectAll();
            } else if (ctrl && code == KeyCode.L) {
                event.consume();
                handleClear();
            } else if (ctrl && code == KeyCode.M) {
                event.consume();
                if (memberPhoneField != null) {
                    memberPhoneField.requestFocus();
                }
            // Ctrl+1/2/3 - 微信/支付宝/银行卡（对齐 UI 文案）
            } else if (ctrl && code == KeyCode.DIGIT1) {
                event.consume();
                handleWechatPayment();
            } else if (ctrl && code == KeyCode.DIGIT2) {
                event.consume();
                handleAlipayPayment();
            } else if (ctrl && code == KeyCode.DIGIT3) {
                event.consume();
                handleCardPayment();
            // Delete - 删除购物车最后一项
            } else if (code == KeyCode.DELETE) {
                event.consume();
                if (!cartItems.isEmpty()) {
                    removeItem(cartItems.get(cartItems.size() - 1));
                }
            // Enter - 搜索框回车
            } else if (code == KeyCode.ENTER && event.getSource() == searchField) {
                event.consume();
                handleSearchAction();
            // F1 / Ctrl+/ - 快捷键帮助
            } else if (code == KeyCode.F1 || (ctrl && code == KeyCode.SLASH)) {
                event.consume();
                showShortcutHelp();
            // Esc - 清搜索或退出
            } else if (code == KeyCode.ESCAPE) {
                event.consume();
                handleEscape();
            }
        });
    }

    public void setApplication(CashierSystemFXApplication application) {
        this.application = application;
    }

    // ===== CartViewHost 契约 =====
    @Override
    public void setCurrentUser(User user) {
        this.currentUser = user;
        updateUserInfo();
    }

    private void updateUserInfo() {
        // 加载店铺名称
        try {
            Map<String, String> settings = com.cashier.service.DataService.loadSettings();
            String storeName = settings.getOrDefault("storeName", "便利店");
            if (storeNameLabel != null) {
                storeNameLabel.setText(storeName);
            }
        } catch (Exception e) {
            logger.warn("加载店铺名称失败", e);
        }

        if (currentUser == null) {
            return;
        }
        if (userNameLabel != null) {
            userNameLabel.setText(currentUser.name);
        }
        if (userRoleLabel != null) {
            userRoleLabel.setText(currentUser.getRoleDisplayName());
        }
        if (avatarText != null && currentUser.name != null && !currentUser.name.isEmpty()) {
            avatarText.setText(currentUser.name.substring(0, 1).toUpperCase());
        }
    }

    @Override
    public boolean isCartEmpty() {
        return cartItems == null || cartItems.isEmpty();
    }

    @Override
    public void focusSearchField() {
        if (searchField != null) {
            searchField.requestFocus();
        }
    }

    // ===== 分类导航 =====

    private static final String HOT_CATEGORY_KEY = "hot";
    private static final String ALL_CATEGORY_KEY = null;
    /** 触屏商品搜索返回上限，避免全表加载 */
    private static final int SEARCH_LIMIT = 500;

    private void loadCategories() {
        // 分类查询放后台，完成后在 FX 线程构建按钮并默认选中「热销推荐」
        UIOptimizer.runInBackground(
            () -> DAOFactory.getInstance().getCategoryDAO().findAll(),
            cats -> {
                logger.info("加载分类完成,共{}个分类", cats.size());
                categoryBox.getChildren().clear();
                ToggleGroup group = new ToggleGroup();

                // 热销推荐 - 置顶
                ToggleButton hotBtn = buildCategoryButton("● " + i18n.get("tpos.hot_products"), HOT_CATEGORY_KEY, group);
                categoryBox.getChildren().add(hotBtn);
                logger.info("已添加'热销推荐'分类按钮");

                // 全部商品
                ToggleButton allBtn = buildCategoryButton(i18n.get("tpos.all_categories"), ALL_CATEGORY_KEY, group);
                categoryBox.getChildren().add(allBtn);
                logger.info("已添加'全部商品'分类按钮");

                for (Category c : cats) {
                    categoryBox.getChildren().add(buildCategoryButton(c.name, c.name, group));
                    logger.debug("添加分类按钮: {}", c.name);
                }
                group.selectedToggleProperty().addListener((obs, o, n) -> {
                    if (n != null) {
                        onCategorySelected((String) n.getUserData());
                    }
                });
                if (!group.getToggles().isEmpty()) {
                    group.selectToggle(group.getToggles().get(0)); // 默认选中热销
                }
            },
            e -> {
                logger.error("加载分类失败", e);
                StatusBarManager.updateError(i18n.get("label.error") + ": " + e.getMessage());
                // 兜底：分类拿不到时至少展示全部商品，避免商品区空白
                loadProducts(ALL_CATEGORY_KEY);
            });
    }

    private ToggleButton buildCategoryButton(String label, String categoryName, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(label);
        btn.getStyleClass().add("tpos-category-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setUserData(categoryName); // HOT_CATEGORY_KEY = 热销, null = 全部, 其他 = 分类名
        btn.setToggleGroup(group);
        return btn;
    }

    private void onCategorySelected(String categoryName) {
        this.currentCategoryName = categoryName;
        this.currentKeyword = null;
        if (searchField != null) {
            searchField.clear();
        }
        loadProducts(categoryName);
    }

    // ===== 商品加载与卡片 =====

    private void loadProducts(String categoryName) {
        // 关键字在 FX 线程读取后固定下来，避免后台线程读到变化的字段
        final String keyword = currentKeyword;
        long sequence = productQuerySequence.incrementAndGet();
        UIOptimizer.runInBackground(
            () -> {
                if (keyword != null && !keyword.isBlank()) {
                    // SQL 关键词搜索（带 LIMIT），避免全表加载后在内存过滤导致卡顿
                    return productDAO.search(keyword, 1, SEARCH_LIMIT).getData();
                }
                if (HOT_CATEGORY_KEY.equals(categoryName)) {
                    // 热销推荐：混合模式（手动标记 + 销量统计）
                    return loadHotProductsHybrid();
                }
                if (ALL_CATEGORY_KEY == categoryName) {
                    return productDAO.findAll();
                }
                return productDAO.findByCategory(categoryName);
            },
            products -> {
                if (sequence != productQuerySequence.get()) {
                    return; // 已有更新的加载请求，丢弃过期结果
                }
                // 覆盖更新库存快照(保留不在当前列表中的购物车商品条目)
                for (Product p : products) {
                    inventoryMap.put(p.name, p);
                }
                refreshProductGrid(products);
            },
            e -> {
                logger.error("加载商品失败", e);
                StatusBarManager.updateError(i18n.get("label.error") + ": " + e.getMessage());
                refreshProductGrid(new ArrayList<>());
            });
    }

    /**
     * 加载热销商品（混合模式）
     * 优先显示手动标记的热销商品，不足时补充销量高的商品
     */
    private List<Product> loadHotProductsHybrid() {
        try {
            // 1. 获取手动标记的热销商品
            List<Product> manualHot = productDAO.findHotProducts();
            logger.info("手动标记热销商品: {}个", manualHot.size());

            // 2. 如果不足 12 个，补充销量高的商品（最近30天）
            final int TARGET_COUNT = 12;
            List<Product> topSelling = manualHot.size() < TARGET_COUNT
                ? productDAO.findTopSellingProducts(30, TARGET_COUNT * 2)
                : List.of();

            List<Product> hotProducts = mergeHotProducts(manualHot, topSelling, TARGET_COUNT);
            logger.info("loadHotProductsHybrid 返回: {}个商品", hotProducts.size());
            return hotProducts;
        } catch (SQLException e) {
            logger.error("加载热销商品失败", e);
            StatusBarManager.updateError(i18n.get("label.error") + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 合并“热销推荐”列表：手动标记的排在前面且不截断，不足 {@code target} 时用销量榜按商品 ID 去重补足。
     *
     * <p>纯函数（不碰界面、不查库），触屏收银台“热销推荐”的取数规则可直接单测。</p>
     */
    static List<Product> mergeHotProducts(List<Product> manualHot, List<Product> topSelling, int target) {
        List<Product> merged = new ArrayList<>(manualHot);
        if (merged.size() >= target) {
            return merged;
        }
        for (Product candidate : topSelling) {
            if (merged.size() >= target) {
                break;
            }
            boolean exists = merged.stream().anyMatch(h -> h.id == candidate.id);
            if (!exists) {
                merged.add(candidate);
            }
        }
        return merged;
    }

    private List<Product> filterByKeyword(List<Product> all, String keyword) {
        String k = keyword.trim().toLowerCase();
        if (k.isEmpty()) {
            return all;
        }
        List<Product> result = new ArrayList<>();
        for (Product p : all) {
            if (containsIgnoreCase(p.name, k)
                || containsIgnoreCase(p.barcode, k)
                || containsIgnoreCase(p.productCode, k)) {
                result.add(p);
            }
        }
        return result;
    }

    private static boolean containsIgnoreCase(String text, String keyword) {
        return text != null && text.toLowerCase().contains(keyword);
    }

    private void refreshProductGrid(List<Product> products) {
        logger.info("刷新商品网格: 商品数量={}", products.size());
        for (Product p : products) {
            logger.info("  添加商品卡片: {} (ID: {})", p.name, p.id);
        }
        productGrid.getChildren().clear();
        for (Product p : products) {
            productGrid.getChildren().add(buildProductCard(p));
        }
    }

    /** 商品卡片:首字符色块 + 名称 + 库存 + 价格(无图标字段,用首字符代替) */
    private VBox buildProductCard(Product p) {
        VBox card = new VBox(6);
        card.getStyleClass().add("tpos-product-card");
        card.setUserData(p);

        StackPane initialPane = new StackPane();
        initialPane.getStyleClass().add("tpos-product-card-initial");
        String firstChar = (p.name != null && !p.name.isEmpty()) ? p.name.substring(0, 1) : "?";
        Label initial = new Label(firstChar);
        initial.getStyleClass().add("tpos-product-card-initial-text"); initial.setStyle("-fx-font-size: 26; -fx-font-weight: bold;");
        initialPane.getChildren().add(initial);

        Label name = new Label(p.name);
        name.getStyleClass().add("tpos-product-card-name");

        Label meta = new Label(i18n.get("tpos.stock_label", p.quantity));
        meta.getStyleClass().add("tpos-product-card-meta");

        String unitSuffix = (p.unit != null && !p.unit.isEmpty()) ? "/" + p.unit : "";
        Label price = new Label(CurrencyUtil.format(p.getPrice().doubleValue()) + unitSuffix);
        price.getStyleClass().add("tpos-product-card-price");

        card.getChildren().addAll(initialPane, name, meta, price);

        if (p.quantity <= 0) {
            card.getStyleClass().add("tpos-product-card--out");
        } else {
            card.setOnMouseClicked(e -> addToCart(p));
        }
        return card;
    }

    // ===== 购物车操作 =====

    /** 支付进行中时阻止修改购物车（电子支付二维码非模态，期间篡改会导致交易错乱） */
    private boolean blockIfPaymentInProgress() {
        if (paymentInProgress) {
            warn(i18n.get("tpos.payment_in_progress"));
            return true;
        }
        return false;
    }

    private void addToCart(Product product) {
        if (blockIfPaymentInProgress()) return;
        if (product == null) {
            return;
        }
        // 刷新最新库存是数据库往返，放后台；回到 FX 线程后再校验入车
        // （避免使用陈旧快照：切分类/多终端变动后前端校验会误导）
        UIOptimizer.runInBackground(
            () -> {
                try {
                    return productDAO.findById(product.id);
                } catch (java.sql.SQLException ex) {
                    logger.warn("刷新商品库存失败 (ID:{}): {}", product.id, ex.getMessage());
                    return null;
                }
            },
            fresh -> applyAddToCart(product, fresh),
            ex -> logger.warn("刷新商品库存失败 (ID:{}): {}", product.id, ex.getMessage()));
    }

    /** 在 JavaFX 线程用最新库存校验后写入购物车。 */
    private void applyAddToCart(Product product, Product fresh) {
        if (fresh != null) {
            inventoryMap.put(product.name, fresh);
        } else {
            inventoryMap.putIfAbsent(product.name, product);
        }
        int stock = currentStock(product, inventoryMap);
        CartItem existing = findCartItem(cartItems, product.id);
        int inCart = existing != null ? existing.quantity : 0;
        if (inCart + 1 > stock) {
            playScanErrorSound();
            warn(i18n.get("tpos.out_of_stock_warn", product.name));
            return;
        }
        if (existing != null) {
            existing.addQuantity(1);
        } else {
            cartItems.add(new CartItem(product, 1));
        }
        refreshCartView();
        updateSummary();
        // 添加成功后清空搜索栏，便于连续扫码/输入下一件商品；添加失败（库存不足等）保留输入
        if (searchField != null) {
            searchField.clear();
            searchField.requestFocus();
        }
        StatusBarManager.updateSuccess(i18n.get("tpos.scan_added", product.name));
        playScanSuccessSound();
    }

    private void incrementQty(CartItem item) {
        if (blockIfPaymentInProgress()) return;
        int stock = currentStock(item.product, inventoryMap);
        if (item.quantity + 1 > stock) {
            warn(i18n.get("tpos.out_of_stock_warn", item.product.name));
            return;
        }
        item.addQuantity(1);
        refreshCartView();
        updateSummary();
    }

    private void decrementQty(CartItem item) {
        if (blockIfPaymentInProgress()) return;
        if (item.quantity <= 1) {
            cartItems.remove(item);
        } else {
            item.addQuantity(-1);
        }
        refreshCartView();
        updateSummary();
    }

    private void removeItem(CartItem item) {
        if (blockIfPaymentInProgress()) return;
        cartItems.remove(item);
        refreshCartView();
        updateSummary();
    }

    /** 按商品 ID 定位购物车行（改名前后的同一商品仍算同一行）。 */
    static CartItem findCartItem(List<CartItem> items, int productId) {
        for (CartItem item : items) {
            if (item.product.id == productId) {
                return item;
            }
        }
        return null;
    }

    /** 取商品可用库存：优先用库存快照里的最新数量，快照里没有才回退用商品自带数量。 */
    static int currentStock(Product product, Map<String, Product> inventory) {
        Product inv = inventory.get(product.name);
        return inv != null ? inv.quantity : product.quantity;
    }

    private void refreshCartView() {
        cartList.getChildren().clear();
        if (cartItems.isEmpty()) {
            Label empty = new Label(i18n.get("cart.empty"));
            empty.getStyleClass().add("tpos-cart-empty");
            cartList.getChildren().add(empty);
            return;
        }
        for (CartItem item : cartItems) {
            cartList.getChildren().add(buildCartRow(item));
        }
    }

    private HBox buildCartRow(CartItem item) {
        HBox row = new HBox(10);
        row.getStyleClass().add("tpos-cart-row");
        row.setAlignment(Pos.CENTER_LEFT);

        // 左侧：商品信息
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        info.getStyleClass().add("tpos-cart-row-content");

        // 第一行：商品名称
        Label name = new Label(item.product.name);
        name.getStyleClass().add("tpos-cart-row-name");
        name.setMaxWidth(200);

        // 第二行：单价 × 数量
        String unitSuffix = (item.product.unit != null && !item.product.unit.isEmpty())
            ? "/" + item.product.unit : "";
        Label priceQty = new Label(String.format("%s × %d%s",
            CurrencyUtil.format(item.product.getPrice().doubleValue()), item.quantity, unitSuffix));
        priceQty.getStyleClass().add("tpos-cart-row-price-qty");

        info.getChildren().addAll(name, priceQty);

        // 右侧：小计 + 控制按钮
        VBox right = new VBox(6);
        right.setAlignment(Pos.TOP_RIGHT);

        // 小计金额
        Label subtotal = new Label(CurrencyUtil.format(item.subtotal.doubleValue()));
        subtotal.getStyleClass().add("tpos-cart-row-subtotal");

        // 控制按钮行
        HBox ctrl = new HBox(6);
        ctrl.getStyleClass().add("tpos-cart-row-ctrl");
        ctrl.setAlignment(Pos.CENTER_RIGHT);

        Button minus = new Button("−");
        minus.getStyleClass().add("tpos-qty-minus");
        minus.setOnAction(e -> decrementQty(item));

        Label qty = new Label(String.valueOf(item.quantity));
        qty.getStyleClass().add("tpos-qty-val");

        Button plus = new Button("+");
        plus.getStyleClass().add("tpos-qty-plus");
        plus.setOnAction(e -> incrementQty(item));

        Button remove = new Button("×");
        remove.getStyleClass().add("tpos-remove-btn");
        remove.setOnAction(e -> removeItem(item));

        ctrl.getChildren().addAll(minus, qty, plus, remove);
        right.getChildren().addAll(subtotal, ctrl);

        row.getChildren().addAll(info, right);
        return row;
    }

    private void updateSummary() {
        int count = cartItems.size();
        int qty = cartItems.stream().mapToInt(i -> i.quantity).sum();
        BigDecimal total = TransactionService.calculateTotalAmount(cartItems);
        BigDecimal finalAmt = getPayableAmount();
        BigDecimal discount = total.subtract(finalAmt);

        // 更新购物车数量
        if (cartCountLabel != null) {
            cartCountLabel.setText("(" + count + ")");
        }
        totalQtyLabel.setText(String.valueOf(qty));
        totalAmountLabel.setText(CurrencyUtil.format(total.doubleValue()));
        discountLabel.setText(discount.compareTo(BigDecimal.ZERO) > 0
            ? "-" + CurrencyUtil.format(discount.doubleValue())
            : CurrencyUtil.format(BigDecimal.ZERO.doubleValue()));
        finalAmountLabel.setText(CurrencyUtil.format(finalAmt.doubleValue()));
    }

    /**
     * 计算当前应付金额（会员折扣 + 最优促销优惠）。
     * 每次调用都重新选取促销（阈值/优惠按商品原价总额计算），使界面显示、挂单与结账落库口径一致。
     */
    private BigDecimal getPayableAmount() {
        appliedPromotion = TransactionService.selectBestPromotion(
            TransactionService.calculateTotalAmount(cartItems));
        return TransactionService.calculateFinalAmount(cartItems, currentMember, appliedPromotion);
    }

    // ===== 搜索 / 清空 / 会员 =====

    @FXML
    private void handleSearch() {
        currentKeyword = searchField.getText();
        loadProducts(currentCategoryName);
    }

    @FXML
    private void handleClear() {
        if (blockIfPaymentInProgress()) return;
        if (cartItems.isEmpty()) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            i18n.get("tpos.clear_confirm"), ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            cartItems.clear();
            currentMember = null;
            cashReceivedAmount = BigDecimal.ZERO; // 重置现金累计金额
            if (memberPhoneField != null) {
                memberPhoneField.clear();
            }
            if (memberInfoLabel != null) {
                memberInfoLabel.setText("");
            }
            refreshCartView();
            updateSummary();
        }
    }

    /** F8 - 挂单：保存当前购物车到数据库，清空界面 */
    private void handleHoldOrder() {
        if (blockIfPaymentInProgress()) return;
        if (cartItems.isEmpty()) {
            warn(i18n.get("cart.hold.empty_cart"));
            return;
        }
        try {
            HoldOrder holdOrder = new HoldOrder();
            holdOrder.orderNumber = HoldOrder.generateOrderNumber();
            holdOrder.userId = currentUser != null ? currentUser.id : 0;
            if (currentMember != null) {
                holdOrder.memberId = currentMember.id;
                holdOrder.memberName = currentMember.name;
                holdOrder.memberPhone = currentMember.phone;
            }
            BigDecimal total = TransactionService.calculateTotalAmount(cartItems);
            BigDecimal finalAmt = getPayableAmount();
            holdOrder.totalAmount = total;
            holdOrder.discountAmount = total.subtract(finalAmt);
            holdOrder.finalAmount = finalAmt;
            holdOrder.itemCount = cartItems.size();
            holdOrder.itemsJson = serializeCartItems(cartItems);
            holdOrderDAO.insert(holdOrder);

            clearCartForHold();
            showInfo(i18n.get("cart.hold.success", holdOrder.orderNumber));
            logger.info("挂单成功: {}", holdOrder.orderNumber);
        } catch (SQLException e) {
            logger.error("挂单失败", e);
            warn(i18n.get("cart.hold.error") + ": " + e.getMessage());
        }
    }

    /** F3 - 取单：列出当前用户挂单，选择恢复 */
    private void handleRecallOrder() {
        if (blockIfPaymentInProgress()) return;
        try {
            int userId = currentUser != null ? currentUser.id : 0;
            List<HoldOrder> holdOrders = userId > 0
                ? holdOrderDAO.findActiveByUserId(userId)
                : holdOrderDAO.findAllActive();
            if (holdOrders.isEmpty()) {
                showInfo(i18n.get("cart.hold.no_orders"));
                return;
            }
            showHoldOrderSelectionDialog(holdOrders);
        } catch (SQLException e) {
            logger.error("获取挂单列表失败", e);
            warn(i18n.get("cart.hold.load_error") + ": " + e.getMessage());
        }
    }

    /** 挂单选择对话框 */
    private void showHoldOrderSelectionDialog(List<HoldOrder> holdOrders) {
        Dialog<HoldOrder> dialog = new Dialog<>();
        dialog.setTitle(i18n.get("cart.hold.resume_title"));
        dialog.setHeaderText(i18n.get("cart.hold.resume_header"));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        ListView<HoldOrder> listView = new ListView<>();
        listView.getItems().addAll(holdOrders);
        listView.setCellFactory(param -> new ListCell<HoldOrder>() {
            @Override
            protected void updateItem(HoldOrder order, boolean empty) {
                super.updateItem(order, empty);
                if (empty || order == null) {
                    setText(null);
                } else {
                    String member = order.memberName != null ? order.memberName : i18n.get("runtime.non_member");
                    setText(i18n.get("runtime.held_order_item", order.orderNumber, order.holdDate,
                        member, CurrencyUtil.format(order.finalAmount.doubleValue()), order.itemCount));
                }
            }
        });
        listView.getSelectionModel().selectFirst();
        dialog.getDialogPane().setContent(listView);
        if (productGrid.getScene() != null) {
            dialog.initOwner(productGrid.getScene().getWindow());
            dialog.getDialogPane().getStylesheets().addAll(productGrid.getScene().getStylesheets());
        }
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return listView.getSelectionModel().getSelectedItem();
            }
            return null;
        });
        dialog.showAndWait().ifPresent(order -> {
            if (order != null) {
                resumeHoldOrder(order);
            }
        });
    }

    /** 恢复挂单到购物车（解析与逐条查库放后台，结果回 FX 线程落地） */
    private void resumeHoldOrder(HoldOrder order) {
        // 当前购物车非空时确认是否覆盖
        if (!cartItems.isEmpty()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                i18n.get("tpos.hold.overwrite_confirm"), ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
                return; // 用户取消，挂单保持不变
            }
        }

        UIOptimizer.runInBackground(
            () -> {
                List<CartItem> items = parseHoldCartItems(order.itemsJson);
                Member member = null;
                if (order.memberId != null) {
                    try {
                        member = DAOFactory.getInstance().getMemberDAO().findById(order.memberId);
                    } catch (SQLException e) {
                        logger.warn("恢复会员信息失败: {}", e.getMessage());
                    }
                }
                holdOrderDAO.updateStatus(order.id, 1);
                return new ResumedHoldOrder(items, member);
            },
            resumed -> {
                cartItems.clear();
                currentMember = resumed.member();
                cashReceivedAmount = BigDecimal.ZERO;
                cartItems.addAll(resumed.items());

                if (currentMember != null) {
                    if (memberPhoneField != null) {
                        memberPhoneField.setText(currentMember.phone);
                    }
                    if (memberInfoLabel != null) {
                        String discountStr = currentMember.getDiscount().stripTrailingZeros().toPlainString();
                        memberInfoLabel.setText(i18n.get("tpos.member_info",
                            currentMember.name, currentMember.level, discountStr));
                    }
                }
                for (CartItem ci : cartItems) {
                    inventoryMap.putIfAbsent(ci.product.name, ci.product);
                }
                refreshCartView();
                updateSummary();
                showInfo(i18n.get("cart.hold.resume_success", order.orderNumber));
                logger.info("恢复挂单成功: {}", order.orderNumber);
            },
            e -> {
                logger.error("恢复挂单失败", e);
                warn(i18n.get("cart.hold.resume_error") + ": " + e.getMessage());
            });
    }

    /** 恢复挂单的结果：后台解析/查库完成后一次性带回 FX 线程。 */
    private record ResumedHoldOrder(List<CartItem> items, Member member) {}

    /** 挂单后清空购物车（保留分类/商品显示） */
    private void clearCartForHold() {
        cartItems.clear();
        currentMember = null;
        cashReceivedAmount = BigDecimal.ZERO;
        if (memberPhoneField != null) {
            memberPhoneField.clear();
        }
        if (memberInfoLabel != null) {
            memberInfoLabel.setText("");
        }
        refreshCartView();
        updateSummary();
    }

    /** 序列化购物车为 JSON（与 CartController 格式一致，便于互通） */
    static String serializeCartItems(List<CartItem> items) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            CartItem item = items.get(i);
            if (i > 0) json.append(",");
            json.append("{\"productId\":").append(item.product.id)
                .append(",\"quantity\":").append(item.quantity).append("}");
        }
        json.append("]");
        return json.toString();
    }

    /** 从 JSON 反序列化恢复购物车 */
    /**
     * 解析挂单明细并逐条查商品。
     *
     * <p>纯数据操作，不触碰 UI 状态，可安全在后台线程调用。</p>
     */
    /**
     * 解析挂单明细并逐条查商品。
     *
     * <p>纯数据操作，不触碰 UI 状态，可安全在后台线程调用；商品已被删除或字段损坏的行跳过，
     * 单行失败不影响其余明细。</p>
     */
    static List<CartItem> parseHoldCartItems(String json) {
        List<CartItem> result = new ArrayList<>();
        if (json == null || json.isEmpty()) return result;
        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) return result;
        String body = json.substring(1, json.length() - 1);
        if (body.isEmpty()) return result;
        String[] items = body.split("\\},\\{");
        for (String item : items) {
            item = item.replace("{", "").replace("}", "");
            String[] fields = item.split(",");
            int productId = 0;
            int quantity = 1;
            for (String field : fields) {
                String[] kv = field.split(":");
                if (kv.length == 2) {
                    String key = kv[0].replace("\"", "").trim();
                    String value = kv[1].trim();
                    try {
                        if ("productId".equals(key)) {
                            productId = Integer.parseInt(value);
                        } else if ("quantity".equals(key)) {
                            quantity = Integer.parseInt(value);
                        }
                    } catch (NumberFormatException e) {
                        logger.debug("解析挂单项字段失败", e);
                    }
                }
            }
            try {
                Product product = productDAO.findById(productId);
                if (product != null) {
                    result.add(new CartItem(product, quantity));
                }
            } catch (SQLException e) {
                logger.warn("恢复商品失败 (ID:{}): {}", productId, e.getMessage());
            }
        }
        return result;
    }

    /** 信息提示（同步状态栏） */
    private void showInfo(String msg) {
        StatusBarManager.updateSuccess(msg);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /** ESC - 取消当前操作 */
    private void handleEscape() {
        // 如果搜索框有焦点，清空搜索或失去焦点
        if (searchField != null && searchField.isFocused()) {
            if (!searchField.getText().isEmpty()) {
                searchField.clear();
                if (currentCategoryName != null) {
                    loadProducts(currentCategoryName);
                }
            } else {
                // 搜索框为空时，失去焦点
                searchField.getParent().requestFocus();
            }
        } else if (memberPhoneField != null && memberPhoneField.isFocused()) {
            // 会员手机号框有焦点，清空内容
            memberPhoneField.clear();
            currentMember = null;
            if (memberInfoLabel != null) {
                memberInfoLabel.setText("");
            }
            updateSummary();
        } else {
            // 其他情况：仅当购物车为空时才退出
            if (cartItems.isEmpty()) {
                handleExit();
            }
        }
    }

    /** F1 / Ctrl+/ - 快捷键帮助说明 */
    private void showShortcutHelp() {
        String shortcuts =
            i18n.get("shortcut.help.tpos_title") + ":\n\n" +
            i18n.get("shortcut.help.category_hold") + ":\n" +
            i18n.get("shortcut.help.f2_hold") + "\n" +
            i18n.get("shortcut.help.f3_resume") + "\n" +
            i18n.get("shortcut.help.delete_last") + "\n" +
            i18n.get("shortcut.help.ctrl_l_clear") + "\n\n" +
            i18n.get("shortcut.help.category_payment") + ":\n" +
            i18n.get("shortcut.help.f8_cash") + "\n" +
            i18n.get("shortcut.help.ctrl1_wechat") + "\n" +
            i18n.get("shortcut.help.ctrl2_alipay") + "\n" +
            i18n.get("shortcut.help.ctrl3_card") + "\n\n" +
            i18n.get("shortcut.help.category_search") + ":\n" +
            i18n.get("shortcut.help.ctrl_f_search") + "\n" +
            i18n.get("shortcut.help.ctrl_m_member") + "\n" +
            i18n.get("shortcut.help.enter_search") + "\n\n" +
            i18n.get("shortcut.help.category_other") + ":\n" +
            i18n.get("shortcut.help.ctrl6_shift") + "\n" +
            i18n.get("shortcut.help.f1_help") + "\n" +
            i18n.get("shortcut.help.esc_clear_exit");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(i18n.get("shortcut.help.title"));
        alert.setHeaderText(null);
        alert.setContentText(shortcuts);
        alert.getDialogPane().setPrefWidth(420);
        if (productGrid != null && productGrid.getScene() != null) {
            alert.initOwner(productGrid.getScene().getWindow());
        }
        alert.showAndWait();
    }

    /** ENTER - 搜索框回车处理 */
    private void handleSearchAction() {
        String keyword = searchField.getText();
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        String trimmed = keyword.trim();
        // 条码/名称/编号精确命中：直接加入购物车（成功后自动清空搜索栏，便于连续录入）
        // 匹配与模糊查询都放后台，避免回车时 FX 线程查库
        UIOptimizer.runInBackground(
            () -> {
                Product product = findExactProduct(trimmed);
                if (product == null) {
                    // 精确未命中：若模糊搜索结果恰好唯一，也视为"确定商品"直接加入
                    List<Product> searched = productDAO.search(trimmed, 1, SEARCH_LIMIT).getData();
                    if (searched.size() == 1) {
                        return searched.get(0);
                    }
                    logger.info("搜索未命中可直加商品: keyword={}, 候选={}", trimmed, searched.size());
                }
                return product;
            },
            product -> {
                if (product != null) {
                    addToCart(product);
                    return;
                }
                // 未精确命中：刷新网格供选择
                currentKeyword = keyword;
                if (currentCategoryName != null) {
                    loadProducts(currentCategoryName);
                }
            },
            e -> {
                logger.error("精确匹配商品失败", e);
                currentKeyword = keyword;
                if (currentCategoryName != null) {
                    loadProducts(currentCategoryName);
                }
            });
    }

    /** 按条码 → 名称 → 商品编号精确匹配商品，未命中返回 null */
    private Product findExactProduct(String keyword) throws SQLException {
        Product product = productDAO.findByBarcode(keyword);
        if (product == null) {
            product = productDAO.findByName(keyword);
        }
        if (product == null) {
            product = productDAO.findByProductCode(keyword);
        }
        return product;
    }

    /** 输入停顿后若仍未命中任何商品，提示“未找到”（避免逐字符误报） */
    private void scheduleNotFoundHint(String keyword) {
        if (notFoundHint != null) {
            notFoundHint.stop();
        }
        notFoundHint = new PauseTransition(Duration.millis(400));
        notFoundHint.setOnFinished(e -> {
            String current = searchField.getText();
            if (current == null || current.isBlank() || !current.trim().equals(keyword)) {
                return; // 输入已变化或已清空
            }
            String trimmed = current.trim();
            // 命中检查也放后台，避免输入停顿后 FX 线程查库
            UIOptimizer.runInBackground(
                () -> findExactProduct(trimmed),
                product -> {
                    if (product == null) {
                        playScanNotFoundSound();
                        warn(i18n.get("tpos.scan_not_found", trimmed));
                    }
                },
                ex -> logger.error("检查未找到商品失败", ex));
        });
        notFoundHint.play();
    }

    /** 播放扫码成功短促提示音 */
    private void playScanSuccessSound() {
        playSound(SCAN_SUCCESS_SOUND);
    }

    /** 播放扫码错误提示音（库存不足等） */
    private void playScanErrorSound() {
        playSound(SCAN_ERROR_SOUND);
    }

    /** 播放未找到商品提示音 */
    private void playScanNotFoundSound() {
        playSound(SCAN_NOT_FOUND_SOUND);
    }

    private void playSound(String resource) {
        try {
            javafx.scene.media.Media sound = new javafx.scene.media.Media(
                getClass().getResource(resource).toString()
            );
            javafx.scene.media.MediaPlayer mediaPlayer = new javafx.scene.media.MediaPlayer(sound);
            mediaPlayer.play();
            logger.debug("播放扫码提示音: {}", resource);
        } catch (Exception e) {
            logger.debug("播放扫码提示音失败（音效文件可能不存在）: {}", e.getMessage());
        }
    }

    @FXML
    private void handleSearchMember() {
        String phone = memberPhoneField.getText();
        if (phone == null || phone.trim().isEmpty()) {
            currentMember = null;
            memberInfoLabel.setText("");
            updateSummary();
            return;
        }
        // 会员查询放后台，避免回车/按钮触发时阻塞触摸界面
        String lookupPhone = phone.trim();
        UIOptimizer.runInBackground(
            () -> DAOFactory.getInstance().getMemberDAO().findByPhone(lookupPhone),
            this::applyMemberSearchResult,
            e -> {
                logger.error("查询会员失败", e);
                StatusBarManager.updateError(i18n.get("label.error") + ": " + e.getMessage());
            });
    }

    /** 在 JavaFX 线程展示会员查询结果。 */
    private void applyMemberSearchResult(Member m) {
        if (m == null) {
            currentMember = null;
            memberInfoLabel.setText(i18n.get("tpos.member_not_found"));
            warn(i18n.get("tpos.member_not_found"));
        } else {
            currentMember = m;
            String discountStr = m.getDiscount().stripTrailingZeros().toPlainString();
            memberInfoLabel.setText(i18n.get("tpos.member_info", m.name, m.level, discountStr));
        }
        updateSummary();
    }

    // ===== 支付前置校验 =====

    private boolean preCheck() {
        if (paymentInProgress) {
            return false;
        }
        if (cartItems.isEmpty()) {
            warn(i18n.get("runtime.cart_empty_payment"));
            return false;
        }
        if (!com.cashier.service.DataService.hasActiveShift()) {
            warn(i18n.get("runtime.no_active_shift"));
            return false;
        }
        return true;
    }

    // ===== 现金支付（支持部分支付）=====

    @FXML
    private void handleCashPayment() {
        if (!preCheck()) {
            return;
        }
        final BigDecimal finalAmount = getPayableAmount();
        final BigDecimal remainingAmount = finalAmount.subtract(cashReceivedAmount);

        Dialog<BigDecimal> dialog = new Dialog<>();
        dialog.setTitle(i18n.get("cart.cash_payment"));
        dialog.setHeaderText(null);

        // 同步主界面样式
        if (productGrid.getScene() != null) {
            dialog.initOwner(productGrid.getScene().getWindow());
            dialog.getDialogPane().getStylesheets().addAll(productGrid.getScene().getStylesheets());
        }

        VBox content = new VBox(12);
        content.setPadding(new Insets(12, 20, 16, 20));
        content.setPrefWidth(520);

        TextField receivedField = createCashInputField();
        GridPane denomGrid = createCashDenominationGrid(receivedField, finalAmount);
        Label statusLabel = createCashStatusLabel(receivedField, finalAmount);
        Button continueBtn = createCashConfirmButton();

        // 组装内容
        content.getChildren().add(createCashDueBox(finalAmount));
        HBox partialBox = createCashPartialBox(remainingAmount);
        if (partialBox != null) {
            content.getChildren().add(partialBox);
        }
        content.getChildren().addAll(
            createCashSectionTitle("收款金额"), receivedField,
            createCashSectionTitle("快捷金额"), denomGrid,
            statusLabel, continueBtn);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().clear();
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);

        // 默认聚焦到金额输入框
        dialog.setOnShown(e -> Platform.runLater(() -> receivedField.requestFocus()));

        // -- ESC / Enter 键盘支持 --
        // ESC 关闭对话框（取消）
        dialog.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                dialog.setResult(null);
                dialog.close();
            }
        });
        // 输入框内 Enter 确认收款
        receivedField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();
                continueBtn.fire();
            }
        });

        // 确认收款：仅解析金额→设结果→关闭对话框；支付/递归放到 showAndWait().ifPresent，
        // 避免在对话框事件处理内嵌套 showAndWait（部分支付后第二次输入界面会卡死）。
        continueBtn.setOnAction(e -> {
            try {
                BigDecimal thisPayment = new BigDecimal(receivedField.getText().trim());
                if (thisPayment.compareTo(BigDecimal.ZERO) <= 0) {
                    return;
                }
                dialog.setResult(thisPayment);
                dialog.close();
            } catch (NumberFormatException ex) {
                warn("请输入有效的金额");
            }
        });

        dialog.showAndWait().ifPresent(thisPayment ->
            handleCashPaymentResult(thisPayment, finalAmount));
    }

    /** 创建应付金额展示行 */
    private HBox createCashDueBox(BigDecimal finalAmount) {
        Label dueTitleLabel = new Label("应付金额");
        dueTitleLabel.getStyleClass().add("cash-section-title");

        Label dueLabel = new Label(CurrencyUtil.format(finalAmount.doubleValue()));
        dueLabel.getStyleClass().add("cash-due-highlight");

        HBox dueBox = new HBox(8, dueTitleLabel, dueLabel);
        dueBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return dueBox;
    }

    /** 创建部分付款的“已付/还需”展示行，未发生部分付款时返回 null */
    private HBox createCashPartialBox(BigDecimal remainingAmount) {
        if (cashReceivedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        Label paidLabel = new Label("已付 " + CurrencyUtil.format(cashReceivedAmount.doubleValue()));
        paidLabel.getStyleClass().add("cash-paid-label");
        Label sep = new Label("  |  ");
        sep.getStyleClass().add("cash-separator");
        Label remainLabel = new Label("还需 " + CurrencyUtil.format(remainingAmount.doubleValue()));
        remainLabel.getStyleClass().add("cash-remain-label");

        HBox partialBox = new HBox(4, paidLabel, sep, remainLabel);
        partialBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return partialBox;
    }

    /** 创建现金支付弹窗的分节标题 */
    private Label createCashSectionTitle(String text) {
        Label title = new Label(text);
        title.getStyleClass().add("cash-section-title");
        return title;
    }

    /** 创建收款金额输入框 */
    private TextField createCashInputField() {
        TextField receivedField = new TextField();
        receivedField.setPromptText("请输入收款金额");
        receivedField.setPrefHeight(56);
        receivedField.setMaxWidth(Double.MAX_VALUE);
        receivedField.getStyleClass().add("cash-input-field");
        return receivedField;
    }

    /** 创建快捷面额按钮区（含精确金额/清除按钮） */
    private GridPane createCashDenominationGrid(TextField receivedField, BigDecimal finalAmount) {
        String symbol = CurrencyUtil.getSymbol();
        int[] amounts = {100, 50, 20, 10, 5, 1};
        GridPane denomGrid = new GridPane();
        denomGrid.setHgap(10);
        denomGrid.setVgap(10);
        denomGrid.setAlignment(javafx.geometry.Pos.CENTER);

        for (int i = 0; i < amounts.length; i++) {
            Button b = new Button(symbol + amounts[i]);
            b.setPrefSize(110, 62);
            b.getStyleClass().add("cash-denom-btn");
            final int amt = amounts[i];
            b.setOnAction(e -> {
                receivedField.setText(String.valueOf(amt));
                receivedField.requestFocus();
            });
            denomGrid.add(b, i % 4, i / 4);
        }

        Button exactBtn = new Button("精确金额");
        exactBtn.setPrefSize(110, 62);
        exactBtn.getStyleClass().add("cash-exact-btn");
        exactBtn.setOnAction(e -> {
            receivedField.setText(finalAmount.toPlainString());
            receivedField.requestFocus();
        });
        denomGrid.add(exactBtn, 2, 1);

        Button clearBtn = new Button("清除 C");
        clearBtn.setPrefSize(110, 62);
        clearBtn.getStyleClass().add("cash-clear-btn");
        clearBtn.setOnAction(e -> {
            receivedField.clear();
            receivedField.requestFocus();
        });
        denomGrid.add(clearBtn, 3, 1);

        return denomGrid;
    }

    /** 创建状态标签并绑定输入监听（找零/还需提示） */
    private Label createCashStatusLabel(TextField receivedField, BigDecimal finalAmount) {
        Label statusLabel = new Label("请输入收款金额");
        statusLabel.getStyleClass().add("cash-status-default");
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setAlignment(javafx.geometry.Pos.CENTER);
        statusLabel.setPrefHeight(40);

        receivedField.textProperty().addListener((o, ov, nv) -> {
            try {
                BigDecimal thisPayment = new BigDecimal(nv.trim());
                BigDecimal totalAfterThis = cashReceivedAmount.add(thisPayment);
                BigDecimal diff = totalAfterThis.subtract(finalAmount);

                if (thisPayment.compareTo(BigDecimal.ZERO) <= 0) {
                    statusLabel.setText("请输入收款金额");
                    statusLabel.getStyleClass().setAll("cash-status-default");
                } else if (totalAfterThis.compareTo(finalAmount) < 0) {
                    BigDecimal stillNeed = finalAmount.subtract(totalAfterThis);
                    statusLabel.setText("还需支付 " + CurrencyUtil.format(stillNeed.doubleValue()));
                    statusLabel.getStyleClass().setAll("cash-status-warn");
                } else {
                    statusLabel.setText("找零 " + CurrencyUtil.format(diff.doubleValue()));
                    statusLabel.getStyleClass().setAll("cash-status-change");
                }
            } catch (NumberFormatException e) {
                statusLabel.setText("请输入收款金额");
                statusLabel.getStyleClass().setAll("cash-status-default");
            }
        });
        return statusLabel;
    }

    /** 创建确认收款按钮 */
    private Button createCashConfirmButton() {
        Button continueBtn = new Button("确认收款 (Enter)");
        continueBtn.setDefaultButton(true);
        continueBtn.setPrefHeight(52);
        continueBtn.setMaxWidth(Double.MAX_VALUE);
        continueBtn.getStyleClass().add("cash-confirm-btn");
        return continueBtn;
    }

    /** 处理现金支付结果：付清则完成交易，未付清则提示并重新打开 */
    private void handleCashPaymentResult(BigDecimal thisPayment, BigDecimal finalAmount) {
        // 此处已脱离对话框事件循环，不再嵌套 showAndWait
        CashProgress progress = applyCashPayment(cashReceivedAmount, thisPayment, finalAmount);
        cashReceivedAmount = progress.received();
        if (progress.settled()) {
            executePayment("现金", cashReceivedAmount, progress.change());
            cashReceivedAmount = BigDecimal.ZERO; // 重置
        } else {
            // 未付清：提示并重新打开（递归在前一个 showAndWait 返回后，不会卡死）
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setHeaderText(null);
            info.setContentText("收款成功！还需: " + CurrencyUtil.format(progress.stillNeed().doubleValue()));
            info.showAndWait();
            handleCashPayment();
        }
    }

    /** 现金支付累计后的结果：新累计收款、找零、尚需金额、是否已付清。 */
    record CashProgress(BigDecimal received, BigDecimal change, BigDecimal stillNeed, boolean settled) {}

    /**
     * 累计一笔现金收款并判定是否付清（触屏收银台支持分次收现）。
     *
     * <p>纯函数：付清时仍返回累计收款额，由调用方负责执行支付后清零。</p>
     */
    static CashProgress applyCashPayment(BigDecimal received, BigDecimal thisPayment, BigDecimal finalAmount) {
        BigDecimal total = received.add(thisPayment);
        if (total.compareTo(finalAmount) >= 0) {
            return new CashProgress(total, total.subtract(finalAmount), BigDecimal.ZERO, true);
        }
        return new CashProgress(total, BigDecimal.ZERO, finalAmount.subtract(total), false);
    }

    // ===== 银行卡支付 =====

    @FXML
    private void handleCardPayment() {
        handleGenericPayment("银行卡");
    }

    private void handleGenericPayment(String paymentMethod) {
        if (!preCheck()) {
            return;
        }
        // 已部分现金支付时强制用现金完成，避免混合支付导致已收现金未记账
        if (cashReceivedAmount.compareTo(BigDecimal.ZERO) > 0) {
            warn(i18n.get("tpos.cash_partial_cash_only"));
            return;
        }
        BigDecimal finalAmount = getPayableAmount();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText(null);
        alert.setContentText(i18n.get("runtime.payment_confirm",
            paymentMethod, CurrencyUtil.format(finalAmount.doubleValue())));
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            executePayment(paymentMethod, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    // ===== 结账大按钮(默认走现金支付流程) =====

    // ===== 电子支付(微信/支付宝) =====

    @FXML
    private void handleWechatPayment() {
        startElectronicPayment(PaymentOrder.PaymentChannel.WECHAT, "微信");
    }

    @FXML
    private void handleAlipayPayment() {
        startElectronicPayment(PaymentOrder.PaymentChannel.ALIPAY, "支付宝");
    }

    private void startElectronicPayment(PaymentOrder.PaymentChannel channel, String paymentMethod) {
        if (!preCheck()) {
            return;
        }
        // 已部分现金支付时强制用现金完成，避免混合支付导致已收现金未记账
        if (cashReceivedAmount.compareTo(BigDecimal.ZERO) > 0) {
            warn(i18n.get("tpos.cash_partial_cash_only"));
            return;
        }
        if (!PaymentService.isChannelAvailable(channel)) {
            warn(i18n.get("payment.channel.unavailable") + ": "
                + PaymentService.getChannelUnavailableReason(channel));
            return;
        }
        try {
            Transaction transaction = createTransaction(paymentMethod);
            String terminalId = currentUser != null ? currentUser.username : "desktop";
            PaymentOrder paymentOrder = PaymentService.createPaymentOrder(
                transaction.transactionId, transaction.finalAmount, channel, terminalId);
            showElectronicPaymentDialog(paymentOrder, transaction, paymentMethod);
        } catch (Exception e) {
            logger.error("创建电子支付订单失败", e);
            warn(i18n.get("payment.create.failed") + ": " + e.getMessage());
        }
    }

    /**
     * 电子支付二维码对话框 + 异步轮询(照搬 CartController 范式,确保不阻塞 UI 线程)。
     */
    private void showElectronicPaymentDialog(PaymentOrder paymentOrder, Transaction transaction,
                                             String paymentMethod) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(i18n.get("payment.scan.title"));
        dialog.setHeaderText(i18n.get("payment.scan.header",
            paymentMethod, CurrencyUtil.format(transaction.finalAmount.doubleValue())));
        if (productGrid.getScene() != null) {
            dialog.initOwner(productGrid.getScene().getWindow());
            dialog.getDialogPane().getStylesheets().addAll(productGrid.getScene().getStylesheets());
        }

        ImageView qrView;
        try {
            qrView = new ImageView(QrCodeImageUtil.create(paymentOrder.qrCodeContent, 260));
        } catch (com.google.zxing.WriterException e) {
            logger.error("生成二维码失败", e);
            warn(i18n.get("payment.qr.generate.failed") + ": " + e.getMessage());
            return;
        }
        Label status = new Label(i18n.get("payment.waiting"));
        Label orderLabel = new Label(paymentOrder.merchantOrderNo);
        orderLabel.getStyleClass().addAll("tpos-muted", "text-sm");
        VBox content = new VBox(12, qrView, status, orderLabel);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(16));
        if ("mock".equals(PaymentService.getConfig().mode)) {
            Button simulateBtn = new Button(i18n.get("payment.mock.simulate"));
            simulateBtn.getStyleClass().addAll("primary-button", "button-normal");
            simulateBtn.setOnAction(e -> simulateMockPayment(paymentOrder, status, simulateBtn));
            content.getChildren().add(simulateBtn);
        }
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        AtomicBoolean settled = new AtomicBoolean(false);
        AtomicBoolean queryRunning = new AtomicBoolean(false);
        Timeline poller = new Timeline();
        poller.getKeyFrames().add(new KeyFrame(Duration.seconds(2), event -> {
            if (!queryRunning.compareAndSet(false, true)) {
                return;
            }
            CompletableFuture.supplyAsync(() -> {
                try {
                    return PaymentService.queryPaymentStatus(paymentOrder.paymentId);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).whenComplete((latest, error) -> javafx.application.Platform.runLater(() -> {
                queryRunning.set(false);
                if (error != null) {
                    status.setText(i18n.get("payment.query.retrying"));
                    return;
                }
                if (latest == null) {
                    return;
                }
                status.setText(latest.status.getDisplayName());
                if (latest.status == PaymentOrder.PaymentStatus.SUCCESS
                        && settled.compareAndSet(false, true)) {
                    poller.stop();
                    dialog.close();
                    paymentInProgress = false;
                    completeTransaction(transaction, paymentMethod, BigDecimal.ZERO, BigDecimal.ZERO);
                } else if (latest.status.isFinal() && latest.status != PaymentOrder.PaymentStatus.SUCCESS) {
                    poller.stop();
                    dialog.close();
                    paymentInProgress = false;
                    warn(i18n.get("payment.not_completed") + ": " + latest.status.getDisplayName());
                }
            }));
        }));
        poller.setCycleCount(Timeline.INDEFINITE);

        dialog.setOnHidden(event -> {
            poller.stop();
            paymentInProgress = false;
            if (!settled.get()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        PaymentService.cancelPaymentOrder(paymentOrder.paymentId);
                    } catch (SQLException e) {
                        logger.warn("取消支付订单失败: {}", paymentOrder.paymentId, e);
                    }
                });
            }
        });

        paymentInProgress = true;
        poller.play();
        dialog.show();
    }

    /** mock 模式下触发一次模拟支付回调，轮询会自动发现支付成功并完成交易 */
    private void simulateMockPayment(PaymentOrder paymentOrder, Label status, Button simulateBtn) {
        simulateBtn.setDisable(true);
        try {
            java.util.Map<String, String> notify = new java.util.HashMap<>();
            notify.put("out_trade_no", paymentOrder.merchantOrderNo);
            notify.put("trade_status", "SUCCESS");
            notify.put("total_amount", paymentOrder.amount.toPlainString());
            notify.put("transaction_id", "MOCK_" + paymentOrder.paymentId);
            notify.put("mock_signature", PaymentService.getConfig().mockCallbackSecret);
            if (PaymentService.handlePaymentNotify(paymentOrder.channel, notify)) {
                status.setText(i18n.get("payment.mock.simulated"));
            } else {
                status.setText(i18n.get("payment.mock.simulate_failed"));
                simulateBtn.setDisable(false);
            }
        } catch (Exception ex) {
            logger.error("模拟支付回调失败", ex);
            status.setText(i18n.get("payment.mock.simulate_failed"));
            simulateBtn.setDisable(false);
        }
    }

    // ===== 结账事务 =====

    private void executePayment(String paymentMethod, BigDecimal receivedAmount, BigDecimal changeAmount) {
        try {
            Transaction transaction = createTransaction(paymentMethod);
            completeTransaction(transaction, paymentMethod, receivedAmount, changeAmount);
        } catch (Exception e) {
            logger.error("交易失败", e);
            warn(i18n.get("runtime.transaction_failed") + ": " + e.getMessage());
        }
    }

    private Transaction createTransaction(String paymentMethod) {
        Transaction tx = new Transaction();
        tx.transactionId = TransactionService.generateOrderNumber();
        tx.timestamp = DateTimeFormats.formatStandard(LocalDateTime.now(ZoneId.systemDefault()));
        tx.items = new ArrayList<>();

        // 合并同 id 商品
        Map<Integer, Product> productMap = new LinkedHashMap<>();
        for (CartItem item : cartItems) {
            Product src = item.product;
            Product existing = productMap.get(src.id);
            if (existing != null) {
                existing.quantity += item.quantity;
            } else {
                Product np = new Product();
                np.id = src.id;
                np.productCode = src.productCode;
                np.barcode = src.barcode;
                np.name = src.name;
                np.price = src.price;
                np.quantity = item.quantity;
                np.category = src.category;
                np.unit = src.unit;
                np.cost = src.cost;
                productMap.put(src.id, np);
            }
        }
        tx.items.addAll(productMap.values());

        tx.totalAmount = TransactionService.calculateTotalAmount(cartItems);
        tx.tax = TransactionService.calculateTax(tx.totalAmount);
        tx.finalAmount = getPayableAmount();
        tx.paymentMethod = paymentMethod;
        if (currentMember != null) {
            tx.memberPhone = currentMember.phone;
        }
        if (currentUser != null) {
            tx.operatorUsername = currentUser.username;
            tx.operatorName = currentUser.name;
        }
        return tx;
    }

    private void completeTransaction(Transaction transaction, String paymentMethod,
                                     BigDecimal receivedAmount, BigDecimal changeAmount) {
        // 交易事务（乐观锁往返+会员/明细落库+同步广播）与回执打印可能耗时数秒，
        // 放到 daemon 线程执行；期间 paymentInProgress 阻止购物车/商品操作与重复结账。
        if (paymentInProgress) {
            return;
        }
        paymentInProgress = true;
        // 在切换到工作线程前快照，保证结账落库与界面显示用的是同一个促销
        final Promotion promotionToApply = appliedPromotion;

        Thread worker = new Thread(() -> {
            try {
                // 兜底:保证购物车所有商品都在 inventoryMap,避免 executeTransaction 内 inventory.get(name) 返回 null
                for (CartItem ci : cartItems) {
                    inventoryMap.computeIfAbsent(ci.product.name, n -> {
                        try {
                            return productDAO.findById(ci.product.id);
                        } catch (SQLException ex) {
                            logger.warn("结账前补查库存失败: {}", ci.product.name, ex);
                            return null;
                        }
                    });
                }

                TransactionService.TransactionResult result = TransactionService.executeTransaction(
                    cartItems, currentMember, transaction, inventoryMap, promotionToApply);

                if (!result.isSuccess() || result.getTransaction() == null) {
                    final String message = result.getMessage();
                    javafx.application.Platform.runLater(() -> {
                        paymentInProgress = false;
                        warn(message != null ? message : i18n.get("runtime.transaction_failed"));
                    });
                    return;
                }

                final Transaction settled = result.getTransaction();
                logger.info("触屏版交易成功,交易ID: {}", settled.transactionId);

                // 在购物车被清空前，先在后台准备好小票快照（settings/明细读取都在 worker 内完成）
                final ReceiptData receipt = createReceiptData(settled, paymentMethod, receivedAmount, changeAmount);

                javafx.application.Platform.runLater(() -> {
                    paymentInProgress = false;
                    showPaymentSuccess(paymentMethod, changeAmount);
                    resetAfterPayment();
                });

                // 小票打印放到独立串行线程，不阻塞 FX，也不影响下一笔交易的收银
                if (receipt != null) {
                    RECEIPT_PRINTER.submit(() -> printReceiptInBackground(settled, receipt));
                }
            } catch (Exception e) {
                logger.error("交易失败", e);
                javafx.application.Platform.runLater(() -> {
                    paymentInProgress = false;
                    warn(i18n.get("runtime.transaction_failed") + ": " + e.getMessage());
                });
            }
        }, "touch-cart-settle");
        worker.setDaemon(true);
        worker.start();
    }

    /** 小票打印所需数据（在结算 worker 内、购物车被清空前一次性快照） */
    private static final class ReceiptData {
        final String storeName;
        final String cashierName;
        final String itemsText;
        final int totalQuantity;
        final double totalAmount;
        final double discountAmount;
        final double finalAmount;
        final double paidAmount;
        final double changeAmount;
        final String paymentMethod;
        final String memberInfo;
        final boolean printLogo;
        final String printerName;
        final String paperSize;

        ReceiptData(String storeName, String cashierName, String itemsText, int totalQuantity,
                    double totalAmount, double discountAmount, double finalAmount, double paidAmount,
                    double changeAmount, String paymentMethod, String memberInfo, boolean printLogo,
                    String printerName, String paperSize) {
            this.storeName = storeName;
            this.cashierName = cashierName;
            this.itemsText = itemsText;
            this.totalQuantity = totalQuantity;
            this.totalAmount = totalAmount;
            this.discountAmount = discountAmount;
            this.finalAmount = finalAmount;
            this.paidAmount = paidAmount;
            this.changeAmount = changeAmount;
            this.paymentMethod = paymentMethod;
            this.memberInfo = memberInfo;
            this.printLogo = printLogo;
            this.printerName = printerName;
            this.paperSize = paperSize;
        }
    }

    /** 在结算 worker 内构建打印快照；打印功能未启用时返回 null */
    private ReceiptData createReceiptData(Transaction tx, String paymentMethod,
                                          BigDecimal received, BigDecimal change) {
        Map<String, String> settings = com.cashier.service.DataService.loadSettings();
        if (!Boolean.parseBoolean(settings.getOrDefault("enablePrint", "false"))) {
            logger.info("打印功能未启用（enablePrint=false），跳过小票打印");
            return null;
        }

        StringBuilder items = new StringBuilder();
        int totalQty = 0;
        for (CartItem ci : cartItems) {
            items.append(ci.product.name)
                .append(" x").append(ci.quantity)
                .append("  ").append(String.format("%.2f", ci.subtotal.doubleValue()))
                .append("\n");
            totalQty += ci.quantity;
        }
        BigDecimal total = TransactionService.calculateTotalAmount(cartItems);
        BigDecimal discount = total.subtract(tx.finalAmount);
        String memberInfo = currentMember != null
            ? (currentMember.name + "(" + currentMember.phone + ") " + currentMember.level) : null;
        String cashierName = currentUser != null ? currentUser.name : "";

        return new ReceiptData(
            settings.getOrDefault("storeName", "狸算收银"),
            cashierName,
            items.toString(),
            totalQty,
            total.doubleValue(),
            discount.doubleValue(),
            tx.finalAmount.doubleValue(),
            received.doubleValue(),
            change.doubleValue(),
            paymentMethod,
            memberInfo,
            Boolean.parseBoolean(settings.getOrDefault("printLogo", "true")),
            settings.getOrDefault("printerName", "").trim(),
            settings.getOrDefault("paperSize", ""));
    }

    /** 在串行打印线程执行实际打印（含网络打印机连接/IO 超时，不占用 FX 线程） */
    private void printReceiptInBackground(Transaction tx, ReceiptData data) {
        try {
            if (!data.printerName.isEmpty()) {
                boolean selected = PrinterManager.getInstance().setDefaultPrinterByName(data.printerName);
                if (!selected) {
                    logger.warn("设置中的打印机名称未匹配到已注册设备: {}", data.printerName);
                }
            }
            PrinterManager.getInstance().applyPaperSize(data.paperSize);
            boolean ok = PrintUtil.printReceipt(
                tx.transactionId, data.storeName, data.cashierName, data.itemsText, data.totalQuantity,
                data.totalAmount, data.discountAmount, data.finalAmount, data.paidAmount,
                data.changeAmount, data.paymentMethod, data.memberInfo, data.printLogo);
            if (!ok) {
                logger.info("小票打印未完成(可能未连接打印机),交易仍已成功: {}", tx.transactionId);
            }
        } catch (Exception e) {
            // 打印失败不应影响已成功的交易
            logger.error("后台小票打印失败: {}", tx.transactionId, e);
        }
    }

    private void showPaymentSuccess(String paymentMethod, BigDecimal change) {
        String changeText = change.compareTo(BigDecimal.ZERO) > 0
            ? "  " + i18n.get("runtime.change_amount", CurrencyUtil.format(change.doubleValue())) : "";
        String msg = i18n.get("service.transaction_success") + changeText;
        StatusBarManager.updateSuccess(msg);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void resetAfterPayment() {
        cartItems.clear();
        currentMember = null;
        cashReceivedAmount = BigDecimal.ZERO; // 重置现金累计金额
        if (memberPhoneField != null) {
            memberPhoneField.clear();
        }
        if (memberInfoLabel != null) {
            memberInfoLabel.setText("");
        }
        refreshCartView();
        updateSummary();
        updateShiftInfo();
        loadProducts(currentCategoryName); // 刷新库存显示(库存已扣减)
    }

    /**
     * 显示语言选择对话框
     */
    private void showLanguageSelectionDialog() {
        // 创建自定义对话框
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(i18n.get("settings.language"));
        dialog.setHeaderText(i18n.get("tpos.language.select"));

        // 设置对话框样式
        dialog.getDialogPane().getStyleClass().add("fs-16");

        // 创建语言选项
        ToggleGroup languageGroup = new ToggleGroup();

        RadioButton chineseRadio = new RadioButton("简体中文");
        chineseRadio.setUserData("zh-CN");
        chineseRadio.setToggleGroup(languageGroup);
        chineseRadio.getStyleClass().addAll("fs-18", "p-8");

        RadioButton traditionalRadio = new RadioButton("繁體中文");
        traditionalRadio.setUserData("zh-TW");
        traditionalRadio.setToggleGroup(languageGroup);
        traditionalRadio.getStyleClass().addAll("fs-18", "p-8");

        RadioButton englishRadio = new RadioButton("English");
        englishRadio.setUserData("en");
        englishRadio.setToggleGroup(languageGroup);
        englishRadio.getStyleClass().addAll("fs-18", "p-8");

        // 选中当前语言
        String currentLanguage = I18nManager.getInstance().getCurrentLanguageTag();
        for (Toggle toggle : languageGroup.getToggles()) {
            RadioButton radio = (RadioButton) toggle;
            if (radio.getUserData().equals(currentLanguage)) {
                radio.setSelected(true);
                break;
            }
        }

        // 垂直布局
        VBox vbox = new VBox(12, chineseRadio, traditionalRadio, englishRadio);
        vbox.getStyleClass().add("p-16");
        vbox.setPadding(new Insets(20, 20, 20, 20));

        dialog.getDialogPane().setContent(vbox);

        // 设置按钮
        ButtonType confirmBtn = new ButtonType(i18n.get("common.confirm"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType(i18n.get("common.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(confirmBtn, cancelBtn);

        // 触屏化：放大按钮
        dialog.setOnShown(e -> {
            for (ButtonType bt : dialog.getDialogPane().getButtonTypes()) {
                Button b = (Button) dialog.getDialogPane().lookupButton(bt);
                if (b != null) {
                    b.setPrefSize(140, 50);
                    b.getStyleClass().add("title-sm");
                }
            }
        });

        ThemeUtils.applyDialogTheme(dialog.getDialogPane());
        // 显示对话框并处理结果
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmBtn) {
                RadioButton selected = (RadioButton) languageGroup.getSelectedToggle();
                if (selected != null) {
                    return (String) selected.getUserData();
                }
            }
            return null;
        });

        java.util.Optional<String> result = dialog.showAndWait();
        result.ifPresent(languageTag -> switchLanguage(languageTag));
    }

    /**
     * 切换语言并刷新界面
     */
    private void switchLanguage(String languageTag) {
        try {
            // 保存语言偏好
            String username = (currentUser != null) ? currentUser.username : "default";
            com.cashier.service.DataService.saveLanguagePreference(username, languageTag);
            com.cashier.service.DataService.saveLanguagePreference("default", languageTag);
            logger.info("语言已切换: username={}, languageTag={}", username, languageTag);

            // 更新 I18nManager
            I18nManager.getInstance().setLocale(languageTag);

            // 显示成功提示
            showInfo(i18n.get("tpos.language.changed"));

            // 延迟刷新界面，确保提示被看到
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
            pause.setOnFinished(e -> {
                // 重新加载当前视图
                if (application != null && currentUser != null) {
                    application.switchToPosModeView(currentUser);
                }
            });
            pause.play();

        } catch (Exception e) {
            logger.error("语言切换失败", e);
            StatusBarManager.updateError(i18n.get("label.error") + ": " + e.getMessage());
        }
    }

    // ===== 工具 =====

    private void warn(String msg) {
        StatusBarManager.updateWarning(msg);
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
