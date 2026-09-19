package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.i18n.I18nManager;
import com.cashier.dao.DAOFactory;
import com.cashier.dao.HoldOrderDAORefactored;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.model.CartItem;
import com.cashier.model.Promotion;
import com.cashier.service.DataService;
import com.cashier.service.HoldOrderCodec;
import com.cashier.service.TransactionService;
import com.cashier.model.Member;
import com.cashier.model.Product;
import com.cashier.model.Transaction;
import com.cashier.model.User;
import com.cashier.model.PaymentOrder;
import com.cashier.scanner.FocusTarget;
import com.cashier.scanner.ScannerManager;
import com.cashier.service.PaymentService;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.ThemeUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.FormValidator;
import com.cashier.util.QrCodeImageUtil;
import com.cashier.util.UIOptimizer;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 购物车控制器
 * 处理购物车的增删改查和结算
 * 已重构为使用重构版 DAO
 */
public class CartController implements CartViewHost {
    private static final Logger logger = LoggerFactoryUtil.getLogger(CartController.class);
    private static final int FIRST_PAGE = 1;
    private static final int CART_PRODUCT_PAGE_SIZE = 500;

    // 音效文件路径（WAV 格式）
    private static final String SCAN_SUCCESS_SOUND = "/sounds/scan_success.wav";
    private static final String SCAN_ERROR_SOUND = "/sounds/scan_error.wav";
    private static final String SCAN_NOT_FOUND_SOUND = "/sounds/scan_not_found.wav";
    private static final String TEXT_SUCCESS_STYLE = "text-success";
    private static final String TEXT_DANGER_STYLE = "text-danger";
    private static final String QUICK_AMOUNT_BUTTON_CLASS = "title-md";
    private static final long DUPLICATE_SCAN_SUPPRESSION_MILLIS = 300;
    private enum ScanMessageLevel {
        SUCCESS,
        WARNING,
        ERROR
    }

    @FXML
    private TableView<CartItem> cartTable;

    @FXML
    private TableColumn<CartItem, String> nameColumn;

    @FXML
    private TableColumn<CartItem, String> priceColumn;

    @FXML
    private TableColumn<CartItem, String> quantityColumn;

    @FXML
    private TableColumn<CartItem, String> subtotalColumn;

    @FXML
    private TableView<Product> productTable;

    @FXML
    private TableColumn<Product, String> productNameColumn;

    @FXML
    private TableColumn<Product, String> productPriceColumn;

    @FXML
    private TableColumn<Product, String> productStockColumn;

    @FXML
    private TextField searchField;

    @FXML
    private Label countLabel;

    @FXML
    private Label totalQuantityLabel;

    @FXML
    private Label totalAmountLabel;

    @FXML
    private Label memberDiscountLabel;

    @FXML
    private Label discountLabel;

    @FXML
    private Label finalAmountLabel;

    @FXML
    private Button addButton;

    @FXML
    private Button removeButton;

    @FXML
    private Button clearButton;

    @FXML
    private TextField memberPhoneField;

    @FXML
    private Label memberInfoLabel;

    @FXML
    private Button cashButton;

    @FXML
    private Button wechatButton;

    @FXML
    private Button alipayButton;

    @FXML
    private Button cardButton;

    private ObservableList<CartItem> cartList;
    private ObservableList<Product> productList;
    private Map<String, Product> inventoryMap;
    private Map<String, CartItem> cartMap = new HashMap<>();
    private Member currentMember;
    private User currentUser;
    private BigDecimal alreadyPaidAmount = BigDecimal.ZERO; // 已支付金额
    private Promotion appliedPromotion; // 当前应用的促销
    private boolean paymentInProgress;
    private final ScannerManager scannerManager = ScannerManager.getInstance();
    private final FocusTarget scannerFocusTarget = createScannerFocusTarget();
    private boolean scannerFocusRegistered;

    // 记录挂到主 Scene 上的键盘过滤器引用，dispose 时必须移除，
    // 否则每次重开收银台都会叠加一个强引用旧控制器的永久过滤器（快捷键错乱 + 内存泄漏）
    private javafx.scene.Scene shortcutScene;
    private javafx.event.EventHandler<javafx.scene.input.KeyEvent> sceneKeyFilter;
    private String lastSuccessfulScanText;
    private long lastSuccessfulScanAt;
    private final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();
    private final HoldOrderDAORefactored holdOrderDAO = DAOFactory.getInstance().getHoldOrderDAO();
    private final I18nManager i18n = I18nManager.getInstance();

    // 商品列表查询（加载/搜索）的序号：只允许最新一次查询的结果刷新列表，丢弃过期响应
    private final AtomicLong productQuerySequence = new AtomicLong();

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 初始化购物车列表
        cartList = FXCollections.observableArrayList();
        cartTable.setItems(cartList);

        // 设置购物车表格列
        setupCartTableColumns();

        // 初始化商品列表
        productList = FXCollections.observableArrayList();
        productTable.setItems(productList);

        // 设置商品表格列
        setupProductTableColumns();

        // 加载库存数据
        loadInventory();

        // 设置表格选择模式
        cartTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        productTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // 添加表格选择监听
        cartTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> updateButtonStates()
        );
        productTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> updateButtonStates()
        );

        // 双击商品添加到购物车
        productTable.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Product product = row.getItem();
                    if (product != null && product.quantity > 0) {
                        addToCart(product, 1);
                    }
                }
            });
            return row;
        });

        // 搜索框 Enter 键监听
        searchField.setOnAction(event -> handleSearch());

        // 会员手机号框 Enter 键监听
        memberPhoneField.setOnAction(event -> handleSearchMember());

        // 注册扫码焦点目标，扫码枪输入直接进入收银台商品搜索
        registerScannerFocusTarget();

        // 设置全局快捷键
        setupShortcuts();

        // 更新统计信息
        updateStatistics();

        // 检查并提示开班状态
        javafx.application.Platform.runLater(this::checkShiftStatus);

        // 自动聚焦到搜索框，方便直接扫描商品
        javafx.application.Platform.runLater(() -> {
            searchField.requestFocus();
        });
    }

    /**
     * 设置快捷键
     */
    private void setupShortcuts() {
        // 等待场景加载完成后设置快捷键
        javafx.application.Platform.runLater(() -> {
            if (cartTable.getScene() != null) {
                setupSceneShortcuts(cartTable.getScene());
            } else {
                // 如果场景还未加载，监听场景属性
                cartTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
                    if (newScene != null) {
                        setupSceneShortcuts(newScene);
                    }
                });
            }
        });
    }

    /**
     * 为场景设置快捷键
     * @param scene 场景
     */
    private void setupSceneShortcuts(javafx.scene.Scene scene) {
        // 同一控制器重复挂载同一场景时跳过，避免叠加重复过滤器
        if (sceneKeyFilter != null && shortcutScene == scene) {
            return;
        }
        shortcutScene = scene;
        sceneKeyFilter = event -> {
            handleCartCommandShortcut(event);
            if (!event.isConsumed()) {
                handleCartFieldShortcut(event);
            }
            if (!event.isConsumed()) {
                handleCartQuantityShortcut(event);
            }
        };
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, sceneKeyFilter);
    }

    private void handleCartCommandShortcut(javafx.scene.input.KeyEvent event) {
        if (event.isControlDown()) {
            handleCartControlShortcut(event);
        }

        if (event.isConsumed()) {
            return;
        }

        handleCartFunctionShortcut(event);
    }

    private void handleCartControlShortcut(javafx.scene.input.KeyEvent event) {
        switch (event.getCode()) {
            case L -> consumeShortcut(event, this::handleClearCart);
            case F -> consumeShortcut(event, searchField::requestFocus);
            case M -> consumeShortcut(event, memberPhoneField::requestFocus);
            case DIGIT1 -> consumeShortcut(event, this::handleWechatPayment);
            case DIGIT2 -> consumeShortcut(event, this::handleAlipayPayment);
            case DIGIT3 -> consumeShortcut(event, this::handleCardPayment);
            case SLASH -> consumeShortcut(event, this::showShortcutHelp);
            default -> {
            }
        }
    }

    private void handleCartFunctionShortcut(javafx.scene.input.KeyEvent event) {
        switch (event.getCode()) {
            case F1 -> consumeShortcut(event, this::handleAddProduct);
            case F2 -> consumeShortcut(event, this::handleHoldOrder);
            case F3 -> consumeShortcut(event, this::handleResumeOrder);
            case F4 -> consumeShortcut(event, this::handleClearCart);
            case DELETE -> consumeShortcut(event, this::handleRemoveProduct);
            case F8 -> consumeShortcut(event, this::handleCashPayment);
            default -> {
            }
        }
    }

    private void handleCartFieldShortcut(javafx.scene.input.KeyEvent event) {
        if (event.getCode() != javafx.scene.input.KeyCode.ESCAPE) {
            return;
        }

        if (searchField.isFocused()) {
            searchField.clear();
            handleSearch();
            event.consume();
        } else if (memberPhoneField.isFocused()) {
            memberPhoneField.clear();
            handleSearchMember();
            event.consume();
        }
    }

    private void handleCartQuantityShortcut(javafx.scene.input.KeyEvent event) {
        if (event.isControlDown() || event.isAltDown()) {
            return;
        }

        CartItem selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected == null || searchField.isFocused() || memberPhoneField.isFocused()) {
            return;
        }

        switch (event.getCode()) {
            case DIGIT0 -> consumeShortcut(event, () -> updateCartItemQuantity(selected, 0));
            case EQUALS, PLUS -> consumeShortcut(event, () -> updateCartItemQuantity(selected, selected.quantity + 1));
            case MINUS, SUBTRACT -> {
                if (selected.quantity > 1) {
                    consumeShortcut(event, () -> updateCartItemQuantity(selected, selected.quantity - 1));
                }
            }
            case PAGE_UP -> consumeShortcut(event,
                () -> updateCartItemQuantity(selected, Math.min(selected.quantity + 5, selected.product.quantity)));
            case PAGE_DOWN -> {
                if (selected.quantity > 5) {
                    consumeShortcut(event, () -> updateCartItemQuantity(selected, selected.quantity - 5));
                }
            }
            default -> handleDigitQuantityShortcut(event, selected);
        }
    }

    private void handleDigitQuantityShortcut(javafx.scene.input.KeyEvent event, CartItem selected) {
        if (!event.getCode().isDigitKey() || event.getText().isEmpty()) {
            return;
        }

        int quantity = event.getText().charAt(0) - '0';
        if (quantity >= 1 && quantity <= 9) {
            updateCartItemQuantity(selected, quantity);
            event.consume();
        }
    }

    private void consumeShortcut(javafx.scene.input.KeyEvent event, Runnable action) {
        action.run();
        event.consume();
    }
    
    /**
     * 更新购物车商品数量
     * @param item 购物车商品
     * @param newQuantity 新数量
     */
    private void updateCartItemQuantity(CartItem item, int newQuantity) {
        if (newQuantity <= 0) {
            // 数量为0，移除商品
            cartList.remove(item);
            cartMap.remove(item.product.name);
        } else if (newQuantity <= item.product.quantity) {
            // 检查库存
            item.quantity = newQuantity;
            item.updateSubtotal();
            cartList.set(cartList.indexOf(item), item); // 触发更新
        } else {
            showInfo(I18nManager.getInstance().get("runtime.low_stock_current", item.product.quantity));
        }
        updateStatistics();
    }

    /**
     * 设置购物车表格列
     */
    private void setupCartTableColumns() {
        nameColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().product.name));
        priceColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%.2f", cellData.getValue().product.price)));
        quantityColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.valueOf(cellData.getValue().quantity)));
        subtotalColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%.2f", cellData.getValue().subtotal)));
    }

    /**
     * 设置商品表格列
     */
    private void setupProductTableColumns() {
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        productPriceColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%.2f", cellData.getValue().price)));
        productStockColumn.setCellValueFactory(cellData -> {
            Product p = cellData.getValue();
            String stockText = String.valueOf(p.quantity);
            if (p.quantity <= 0) {
                return new SimpleStringProperty(stockText + " (缺货)");
            } else if (p.quantity < p.minStock) {
                return new SimpleStringProperty(stockText + " (不足)");
            }
            return new SimpleStringProperty(stockText);
        });
    }

    /**
     * 加载库存数据（后台查询，完成后回到 JavaFX 线程刷新列表）。
     */
    private void loadInventory() {
        logger.info("CartController: 开始加载库存数据...");
        // 先占位，避免异步结果返回前被其它路径读到 null
        inventoryMap = new HashMap<>();
        long sequence = productQuerySequence.incrementAndGet();
        UIOptimizer.runInBackground(
            () -> productDAO.findAll(FIRST_PAGE, CART_PRODUCT_PAGE_SIZE).getData(),
            products -> {
                if (sequence != productQuerySequence.get()) {
                    return; // 已有更新的查询，丢弃过期结果
                }
                for (Product product : products) {
                    inventoryMap.put(product.name, product);
                }
                logger.info("CartController: 加载了 {} 个商品", inventoryMap.size());
                productList.setAll(inventoryMap.values());
                updateCountLabel();
            },
            e -> {
                logger.error("从数据库加载商品失败", e);
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
            });
    }

    private List<Product> searchProducts(String searchText) throws SQLException {
        if (searchText == null || searchText.trim().isEmpty()) {
            return productDAO.findAll(FIRST_PAGE, CART_PRODUCT_PAGE_SIZE).getData();
        }
        return productDAO.search(searchText.trim(), FIRST_PAGE, CART_PRODUCT_PAGE_SIZE).getData();
    }

    private void replaceVisibleProducts(List<Product> products) {
        inventoryMap.clear();
        for (Product product : products) {
            inventoryMap.put(product.name, product);
        }
        productList.setAll(products);
        updateCountLabel();
    }

    /**
     * 更新商品数量标签
     */
    private void updateCountLabel() {
        countLabel.setText(i18n.get("cart.product_count", productList.size()));
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        boolean hasCartSelection = !cartTable.getSelectionModel().getSelectedItems().isEmpty();
        boolean hasProductSelection = !productTable.getSelectionModel().getSelectedItems().isEmpty();
        
        // 移除按钮（如果存在）
        if (removeButton != null) {
            removeButton.setDisable(!hasCartSelection);
        }
        
        // 添加按钮（如果存在）
        if (addButton != null) {
            addButton.setDisable(!hasProductSelection);
        }
        
        // 清空按钮
        if (clearButton != null) {
            clearButton.setDisable(cartList.isEmpty());
        }
    }

    /**
     * 添加商品到购物车
     */
    @FXML
    public void handleAddProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            addToCart(selected, 1);
        }
    }

    /**
     * 添加商品到购物车（内部方法）。
     *
     * <p>班次状态与最新库存各是一次数据库往返，统一放后台取，回到 FX 线程后再校验入车，
     * 避免扫码/双击/回车时界面被阻塞。</p>
     * @param product 商品
     * @param quantity 数量
     */
    private void addToCart(Product product, int quantity) {
        UIOptimizer.runInBackground(
            () -> {
                // 从数据库获取最新库存数据，确保使用最新库存
                Product latestProduct = null;
                try {
                    latestProduct = productDAO.findById(product.id);
                } catch (SQLException e) {
                    logger.error("从数据库获取商品最新库存失败", e);
                }
                return new CartAddContext(com.cashier.service.DataService.hasActiveShift(), latestProduct);
            },
            context -> applyAddToCart(product, quantity, context),
            e -> logger.error("添加商品到购物车失败", e));
    }

    /**
     * 在 JavaFX 线程校验后写入购物车；校验顺序（班次 → 数量 → 库存）与后台化之前一致。
     */
    private void applyAddToCart(Product product, int quantity, CartAddContext context) {
        // 检查是否有活跃班次
        if (!context.activeShift()) {
            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.no_active_shift_transaction"));
            return;
        }

        if (quantity <= 0) {
            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.quantity_positive"));
            return;
        }

        if (context.latestProduct() != null) {
            // 更新内存中的库存数据
            inventoryMap.put(product.name, context.latestProduct());
            product = context.latestProduct();
        }

        if (quantity > product.quantity) {
            showError(I18nManager.getInstance().get("runtime.low_stock_current", product.quantity));
            return;
        }

        CartItem cartItem = cartMap.get(product.name);
        if (cartItem != null) {
            // 商品已在购物车中，增加数量
            int newQuantity = cartItem.quantity + quantity;
            if (newQuantity > product.quantity) {
                showError(I18nManager.getInstance().get("runtime.low_stock_max", product.quantity));
                return;
            }
            cartItem.setQuantity(newQuantity);
            // 先移除再添加来触发TableView刷新
            cartList.remove(cartItem);
            cartList.add(cartItem);
        } else {
            // 商品不在购物车中，添加新项
            cartItem = new CartItem(product, quantity);
            cartMap.put(product.name, cartItem);
            cartList.add(cartItem);
        }

        updateStatistics();
        updateButtonStates();
        selectCartItem(product.name);
        // 添加成功后清空搜索栏（扫码/双击/回车/按钮路径统一）；添加失败（库存不足等）保留输入
        if (searchField != null) {
            searchField.clear();
        }
    }

    /** 添加商品到购物车前，后台取回的班次状态与最新库存。 */
    private record CartAddContext(boolean activeShift, Product latestProduct) {}

    private boolean addScannedProductToCart(String scanText) {
        String normalizedScanText = scanText != null ? scanText.trim() : "";
        if (normalizedScanText.isEmpty()) {
            return false;
        }

        if (paymentInProgress) {
            playScanErrorSound();
            showScanMessage(i18n.get("cart.scan.payment_in_progress"), ScanMessageLevel.ERROR);
            return false;
        }

        if (isDuplicateSuccessfulScan(normalizedScanText)) {
            searchField.clear();
            searchField.requestFocus();
            showScanMessage(i18n.get("cart.scan.duplicate_ignored", normalizedScanText), ScanMessageLevel.WARNING);
            return false;
        }

        List<Product> exactMatches;
        try {
            exactMatches = findExactScanMatches(normalizedScanText);
        } catch (SQLException e) {
            logger.error("查询扫码商品失败: {}", normalizedScanText, e);
            playScanErrorSound();
            showScanMessage(i18n.get("cart.scan.not_found", normalizedScanText), ScanMessageLevel.ERROR);
            return false;
        }

        if (exactMatches.isEmpty()) {
            playScanNotFoundSound();
            showScanMessage(i18n.get("cart.scan.not_found", normalizedScanText), ScanMessageLevel.ERROR);
            return false;
        }

        if (exactMatches.size() > 1) {
            replaceVisibleProducts(exactMatches);
            playScanErrorSound();
            showScanMessage(i18n.get("cart.scan.multiple_matches", exactMatches.size()), ScanMessageLevel.WARNING);
            return false;
        }

        Product product = exactMatches.get(0);
        if (product.quantity <= 0) {
            playScanErrorSound();
            showScanMessage(i18n.get("cart.scan.out_of_stock", product.name), ScanMessageLevel.ERROR);
            return false;
        }

        int beforeQuantity = getCartQuantity(product.name);
        addToCart(product, 1);
        int afterQuantity = getCartQuantity(product.name);
        if (afterQuantity <= beforeQuantity) {
            playScanErrorSound();
            return false;
        }

        playScanSuccessSound();
        flashTable(cartTable);
        rememberSuccessfulScan(normalizedScanText);
        showScanMessage(i18n.get("cart.scan.added", product.name), ScanMessageLevel.SUCCESS);
        searchField.clear();
        searchField.requestFocus();
        return true;
    }

    private boolean isDuplicateSuccessfulScan(String scanText) {
        long now = System.currentTimeMillis();
        return lastSuccessfulScanText != null
            && lastSuccessfulScanText.equalsIgnoreCase(scanText)
            && now - lastSuccessfulScanAt <= DUPLICATE_SCAN_SUPPRESSION_MILLIS;
    }

    private void rememberSuccessfulScan(String scanText) {
        lastSuccessfulScanText = scanText;
        lastSuccessfulScanAt = System.currentTimeMillis();
    }

    private boolean matchesExactScanCode(Product product, String scanText) {
        return (product.barcode != null && product.barcode.equalsIgnoreCase(scanText))
            || (product.productCode != null && product.productCode.equalsIgnoreCase(scanText));
    }

    private int getCartQuantity(String productName) {
        CartItem item = cartMap.get(productName);
        return item != null ? item.quantity : 0;
    }

    private void selectCartItem(String productName) {
        for (int i = 0; i < cartList.size(); i++) {
            CartItem item = cartList.get(i);
            if (item.product != null && item.product.name.equals(productName)) {
                cartTable.getSelectionModel().clearAndSelect(i);
                cartTable.scrollTo(i);
                return;
            }
        }
    }

    /**
     * 从购物车移除商品
     */
    @FXML
    public void handleRemoveProduct() {
        CartItem selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            cartMap.remove(selected.product.name);
            cartList.remove(selected);
            updateStatistics();
            updateButtonStates();
        }
    }

    /**
     * 清空购物车
     */
    @FXML
    public void handleClearCart() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18nManager.getInstance().get(I18nKeys.Common.CONFIRM));
        alert.setHeaderText(null);
        alert.setContentText(com.cashier.i18n.I18nManager.getInstance().get("runtime.clear_cart_confirm"));

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            cartMap.clear();
            cartList.clear();
            updateStatistics();
            updateButtonStates();
            // 清空购物车后，焦点回到搜索框，方便继续扫描
            searchField.requestFocus();
        }
    }

    /**
     * 搜索商品
     */
    @FXML
    public void handleSearch() {
        String searchText = searchField.getText().trim();
        long sequence = productQuerySequence.incrementAndGet();
        // 查询放后台：扫描枪/回车触发时，FX 线程不得被数据库往返阻塞
        UIOptimizer.runInBackground(
            () -> searchProducts(searchText),
            matchedProducts -> {
                if (sequence != productQuerySequence.get()) {
                    return; // 快速连续扫码时丢弃过期结果，避免旧结果覆盖新结果
                }
                applySearchResult(searchText, matchedProducts);
            },
            e -> {
                logger.error("搜索商品失败", e);
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
            });
    }

    /**
     * 在 JavaFX 线程应用搜索结果：
     * 空关键字恢复全部商品；唯一匹配直接入车；多匹配展示列表供选择。
     */
    private void applySearchResult(String searchText, List<Product> matchedProducts) {
        if (searchText.isEmpty()) {
            replaceVisibleProducts(matchedProducts);
            return;
        }

        if (matchedProducts.isEmpty()) {
            // 未找到商品
            playScanNotFoundSound();
            showScanMessage(i18n.get("cart.scan.not_found", searchText), ScanMessageLevel.ERROR);
            searchField.clear();
            searchField.requestFocus();
            return;
        }

        if (matchedProducts.size() == 1) {
            // 找到唯一商品，自动添加到购物车
            Product product = matchedProducts.get(0);
            if (product.quantity > 0) {
                addToCart(product, 1);
                playScanSuccessSound();
                flashTable(cartTable);
                showScanMessage(i18n.get("cart.scan.added", product.name), ScanMessageLevel.SUCCESS);
                searchField.clear();
                searchField.requestFocus();
            } else {
                playScanErrorSound();
                showScanMessage(i18n.get("cart.scan.out_of_stock", product.name), ScanMessageLevel.ERROR);
                searchField.clear();
                searchField.requestFocus();
            }
            return;
        }

        // 找到多个匹配商品，显示列表让用户选择
        replaceVisibleProducts(matchedProducts);
        playScanSuccessSound();
        showScanMessage(i18n.get("cart.scan.multiple_matches", matchedProducts.size()), ScanMessageLevel.WARNING);
    }

    /**
     * 去结账
     */
    @FXML
    public void handleCheckout() {
        showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.select_payment_method"));
    }

    /**
     * 搜索会员（查询放后台，避免回车/按钮触发时阻塞界面）
     */
    @FXML
    public void handleSearchMember() {
        String phone = memberPhoneField.getText().trim();
        if (phone.isEmpty()) {
            currentMember = null;
            memberInfoLabel.setText("");
            updateStatistics();
            return;
        }

        UIOptimizer.runInBackground(
            () -> DAOFactory.getInstance().getMemberDAO().findByPhone(phone),
            this::applyMemberSearchResult,
            e -> {
                logger.error("从数据库查找会员失败", e);
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
            });
    }

    /**
     * 在 JavaFX 线程展示会员查询结果
     */
    private void applyMemberSearchResult(Member member) {
        if (member != null) {
            currentMember = member;
            memberInfoLabel.setText(I18nManager.getInstance().get("runtime.member_summary_discount",
                    member.name, CurrencyUtil.format(member.balance.doubleValue()),
                    member.getPoints().intValue(), member.discount));
        } else {
            currentMember = null;
            memberInfoLabel.setText(com.cashier.i18n.I18nManager.getInstance().get("runtime.member_not_found"));
        }

        updateStatistics();
    }

    /**
     * 现金支付
     */
    @FXML
    public void handleCashPayment() {
        if (cartList.isEmpty()) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.CART_EMPTY_PAYMENT));
            return;
        }

        if (!com.cashier.service.DataService.hasActiveShift()) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.NO_ACTIVE_SHIFT));
            return;
        }

        BigDecimal finalAmount = getFinalAmount();

        Dialog<BigDecimal> dialog = new Dialog<>();
        dialog.setTitle(com.cashier.i18n.I18nManager.getInstance().get("statistics.cash_payment"));
        dialog.setHeaderText(null);

        CashPaymentForm form = createCashPaymentForm(finalAmount, alreadyPaidAmount);

        String symbol = CurrencyUtil.getSymbol();
        Button btn100 = createQuickAmountButton(symbol, "100", form.receivedField);
        Button btn50 = createQuickAmountButton(symbol, "50", form.receivedField);
        Button btn20 = createQuickAmountButton(symbol, "20", form.receivedField);
        Button btn10 = createQuickAmountButton(symbol, "10", form.receivedField);
        Button btn5 = createQuickAmountButton(symbol, "5", form.receivedField);

        HBox quickButtons = new HBox(10, btn100, btn50, btn20, btn10, btn5);
        form.grid.add(quickButtons, 0, 5, 2, 1);

        dialog.getDialogPane().setContent(form.grid);
        ThemeUtils.applyDialogTheme(dialog.getDialogPane());

        ButtonType okButtonType = new ButtonType(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Dialog.CONFIRM), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        form.receivedField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                BigDecimal received = new BigDecimal(newVal.trim());
                BigDecimal totalPaid = alreadyPaidAmount.add(received);
                BigDecimal remaining = finalAmount.subtract(totalPaid);
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    form.changeLabel.setText(I18nManager.getInstance().get(I18nKeys.Runtime.CHANGE_AMOUNT, CurrencyUtil.format(remaining.abs().doubleValue())));
                    form.changeLabel.getStyleClass().removeAll(TEXT_SUCCESS_STYLE, TEXT_DANGER_STYLE);
                    form.changeLabel.getStyleClass().add(TEXT_SUCCESS_STYLE);
                } else {
                    form.changeLabel.setText(I18nManager.getInstance().get("runtime.remaining_amount", CurrencyUtil.format(remaining.doubleValue())));
                    form.changeLabel.getStyleClass().removeAll(TEXT_SUCCESS_STYLE, TEXT_DANGER_STYLE);
                    form.changeLabel.getStyleClass().add(TEXT_DANGER_STYLE);
                }
            } catch (NumberFormatException e) {
                BigDecimal remaining = finalAmount.subtract(alreadyPaidAmount);
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    form.changeLabel.setText(I18nManager.getInstance().get(I18nKeys.Runtime.CHANGE_AMOUNT, CurrencyUtil.format(0)));
                } else {
                    form.changeLabel.setText(I18nManager.getInstance().get("runtime.remaining_amount", CurrencyUtil.format(remaining.doubleValue())));
                }
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                try {
                    BigDecimal received = new BigDecimal(form.receivedField.getText().trim());
                    if (received.compareTo(BigDecimal.ZERO) <= 0) {
                        showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.invalid_amount"));
                        return null;
                    }
                    return received;
                } catch (NumberFormatException e) {
                    showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.invalid_amount"));
                    return null;
                }
            }
            return null;
        });

        dialog.setOnShown(event -> {
            javafx.application.Platform.runLater(form.receivedField::requestFocus);

            Button okButton = (Button) dialog.getDialogPane().lookupButton(okButtonType);
            if (okButton != null) {
                okButton.setPrefSize(120, 50);
                okButton.getStyleClass().add("title-sm");
            }
            Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
            if (cancelButton != null) {
                cancelButton.setPrefSize(120, 50);
                cancelButton.getStyleClass().add("fs-16");
            }
        });

        dialog.showAndWait().ifPresentOrElse(
            receivedAmount -> handleCashPaymentResult(receivedAmount, finalAmount),
            this::handleCashPaymentCancelled);
    }

    /** 现金支付表单控件集合 */
    private static class CashPaymentForm {
        final GridPane grid;
        final TextField receivedField;
        final Label changeLabel;

        CashPaymentForm(GridPane grid, TextField receivedField, Label changeLabel) {
            this.grid = grid;
            this.receivedField = receivedField;
            this.changeLabel = changeLabel;
        }
    }

    private CashPaymentForm createCashPaymentForm(BigDecimal finalAmount, BigDecimal alreadyPaidAmount) {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new javafx.geometry.Insets(25, 150, 15, 15));

        Label amountLabel = new Label(I18nManager.getInstance().get("runtime.amount_due", CurrencyUtil.format(finalAmount.doubleValue())));
        amountLabel.getStyleClass().add(TEXT_DANGER_STYLE);
        amountLabel.getStyleClass().add("title-2xl");

        Label paidLabel = new Label(I18nManager.getInstance().get("runtime.amount_paid", CurrencyUtil.format(alreadyPaidAmount.doubleValue())));
        paidLabel.getStyleClass().add(TEXT_SUCCESS_STYLE);
        paidLabel.getStyleClass().add("fs-18");

        BigDecimal initialRemaining = finalAmount.subtract(alreadyPaidAmount);
        Label remainingLabel = new Label(I18nManager.getInstance().get("runtime.amount_remaining", CurrencyUtil.format(initialRemaining.doubleValue())));
        remainingLabel.getStyleClass().add(TEXT_DANGER_STYLE);
        remainingLabel.getStyleClass().add("title-lg");

        TextField receivedField = new TextField();
        receivedField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.payment_amount_hint"));
        receivedField.setPrefHeight(45);
        receivedField.getStyleClass().add("fs-18");

        Label receivedLabel = new Label(I18nManager.getInstance().get("runtime.payment_this_time"));
        receivedLabel.getStyleClass().add("fs-18");

        Label changeLabel = new Label(I18nManager.getInstance().get(I18nKeys.Runtime.CHANGE_AMOUNT, CurrencyUtil.format(0)));
        changeLabel.getStyleClass().add(TEXT_SUCCESS_STYLE);
        changeLabel.getStyleClass().add(QUICK_AMOUNT_BUTTON_CLASS);

        grid.add(amountLabel, 0, 0, 2, 1);
        grid.add(paidLabel, 0, 1, 2, 1);
        grid.add(remainingLabel, 0, 2, 2, 1);
        grid.add(receivedLabel, 0, 3);
        grid.add(receivedField, 1, 3);
        grid.add(changeLabel, 0, 4, 2, 1);
        return new CashPaymentForm(grid, receivedField, changeLabel);
    }

    private Button createQuickAmountButton(String symbol, String amount, TextField receivedField) {
        Button button = new Button(symbol + amount);
        button.setPrefSize(100, 60);
        button.getStyleClass().add(QUICK_AMOUNT_BUTTON_CLASS);
        button.setOnAction(e -> {
            receivedField.setText(amount);
            receivedField.requestFocus();
        });
        return button;
    }

    private void handleCashPaymentResult(BigDecimal receivedAmount, BigDecimal finalAmount) {
        BigDecimal totalPaid = alreadyPaidAmount.add(receivedAmount);
        BigDecimal remaining = finalAmount.subtract(totalPaid);

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            executePayment("现金", totalPaid, remaining.abs());
            alreadyPaidAmount = BigDecimal.ZERO;
        } else {
            alreadyPaidAmount = totalPaid;
            showInfo(I18nManager.getInstance().get("runtime.partial_payment",
                CurrencyUtil.format(totalPaid.doubleValue()), CurrencyUtil.format(remaining.doubleValue())));
            handleCashPayment();
        }
    }

    private void handleCashPaymentCancelled() {
        // 用户点击取消或关闭对话框，若有部分支付未完成则重置
        if (alreadyPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            alreadyPaidAmount = BigDecimal.ZERO;
        }
    }

    /**
     * 微信支付
     */
    @FXML
    public void handleWechatPayment() {
        startElectronicPayment(PaymentOrder.PaymentChannel.WECHAT, "微信");
    }

    /**
     * 支付宝支付
     */
    @FXML
    public void handleAlipayPayment() {
        startElectronicPayment(PaymentOrder.PaymentChannel.ALIPAY, "支付宝");
    }

    /**
     * 银行卡支付
     */
    @FXML
    public void handleCardPayment() {
        handlePayment("银行卡");
    }

    /**
     * 执行支付
     * @param paymentMethod 支付方式
     * @param receivedAmount 实收金额（现金支付时使用）
     * @param changeAmount 找零金额（现金支付时使用）
     */
    private void executePayment(String paymentMethod, BigDecimal receivedAmount, BigDecimal changeAmount) {
        try {
            Transaction transaction = createTransaction(paymentMethod);
            completeTransaction(transaction, paymentMethod, receivedAmount, changeAmount);
        } catch (Exception e) {
            logger.error("交易失败: " + e.getMessage(), e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED) + ": " + e.getMessage());
        }
    }

    private void completeTransaction(Transaction transaction, String paymentMethod,
                                     BigDecimal receivedAmount, BigDecimal changeAmount) {
        // 交易事务含逐商品乐观锁往返+会员扣减+明细落库+同步广播，放到 daemon 线程执行；
        // 期间用 paymentInProgress 锁住购物车/搜索/支付按钮，防止并发篡改与重复结账。
        if (paymentInProgress) {
            return;
        }
        setPaymentInProgress(true);

        Thread worker = new Thread(() -> {
            try {
                TransactionService.TransactionResult result = TransactionService.executeTransaction(
                    cartList,
                    currentMember,
                    transaction,
                    inventoryMap,
                    appliedPromotion
                );

                if (!result.isSuccess() || result.getTransaction() == null) {
                    final String message = result.getMessage();
                    javafx.application.Platform.runLater(() -> {
                        setPaymentInProgress(false);
                        showError(message != null
                            ? message
                            : com.cashier.i18n.I18nManager.getInstance().get("runtime.transaction_failed"));
                    });
                    return;
                }

                final com.cashier.model.Transaction settled = result.getTransaction();
                logger.info("交易成功完成，交易ID: {}", settled.transactionId);
                javafx.application.Platform.runLater(() -> {
                    setPaymentInProgress(false);
                    showSuccess(paymentMethod, settled, receivedAmount.doubleValue(), changeAmount.doubleValue());
                    clear();
                });
            } catch (Exception e) {
                logger.error("交易失败: " + e.getMessage(), e);
                javafx.application.Platform.runLater(() -> {
                    setPaymentInProgress(false);
                    showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED) + ": " + e.getMessage());
                });
            }
        }, "cart-settle");
        worker.setDaemon(true);
        worker.start();
    }

    private void startElectronicPayment(PaymentOrder.PaymentChannel channel, String paymentMethod) {
        if (paymentInProgress) return;
        if (cartList.isEmpty()) {
            showError(i18n.get(I18nKeys.Runtime.CART_EMPTY_PAYMENT));
            return;
        }
        if (!DataService.hasActiveShift()) {
            showError(i18n.get(I18nKeys.Runtime.NO_ACTIVE_SHIFT));
            return;
        }
        if (!PaymentService.isChannelAvailable(channel)) {
            showError(i18n.get("payment.channel.unavailable") + ": "
                + PaymentService.getChannelUnavailableReason(channel));
            return;
        }

        // 检查是否有部分现金支付未完成
        if (alreadyPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal finalAmount = getFinalAmount();
            BigDecimal remaining = finalAmount.subtract(alreadyPaidAmount);
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(i18n.get(I18nKeys.Common.WARNING));
            alert.setHeaderText(i18n.get("payment.mixed_payment_warning"));
            alert.setContentText(i18n.get("payment.mixed_payment_detail",
                CurrencyUtil.format(alreadyPaidAmount.doubleValue()),
                CurrencyUtil.format(remaining.doubleValue()),
                CurrencyUtil.format(finalAmount.doubleValue())));
            ButtonType continueButtonType = new ButtonType(i18n.get("payment.continue_anyway"), ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButtonType = new ButtonType(i18n.get(I18nKeys.Common.CANCEL), ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(continueButtonType, cancelButtonType);

            alert.showAndWait().ifPresent(buttonType -> {
                if (buttonType == cancelButtonType) {
                    // 用户取消，重置部分支付
                    alreadyPaidAmount = BigDecimal.ZERO;
                }
            });
        }

        try {
            Transaction transaction = createTransaction(paymentMethod);
            String terminalId = currentUser != null ? currentUser.username : "desktop";
            PaymentOrder paymentOrder = PaymentService.createPaymentOrder(
                transaction.transactionId, transaction.finalAmount, channel, terminalId);
            showElectronicPaymentDialog(paymentOrder, transaction, paymentMethod);
        } catch (Exception e) {
            logger.error("创建电子支付订单失败", e);
            showError(i18n.get("payment.create.failed") + ": " + e.getMessage());
        }
    }

    private void showElectronicPaymentDialog(PaymentOrder paymentOrder, Transaction transaction,
                                             String paymentMethod) throws Exception {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(i18n.get("payment.scan.title"));
        dialog.setHeaderText(i18n.get("payment.scan.header",
            localizePaymentMethod(paymentMethod), CurrencyUtil.format(transaction.finalAmount.doubleValue())));
        if (cartTable.getScene() != null) {
            dialog.initOwner(cartTable.getScene().getWindow());
            dialog.getDialogPane().getStylesheets().addAll(cartTable.getScene().getStylesheets());
        }

        javafx.scene.image.ImageView qrView = new javafx.scene.image.ImageView(
            QrCodeImageUtil.create(paymentOrder.qrCodeContent, 260));
        Label status = new Label(i18n.get("payment.waiting"));
        status.getStyleClass().add("payment-status-label");
        Label orderLabel = new Label(paymentOrder.merchantOrderNo);
        orderLabel.getStyleClass().add("text-muted");
        VBox content = new VBox(12, qrView, status, orderLabel);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.setPadding(new javafx.geometry.Insets(16));
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
            if (!queryRunning.compareAndSet(false, true)) return;
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
                if (latest == null) return;
                status.setText(latest.status.getDisplayName());
                if (latest.status == PaymentOrder.PaymentStatus.SUCCESS
                        && settled.compareAndSet(false, true)) {
                    poller.stop();
                    dialog.close();
                    setPaymentInProgress(false);
                    completeTransaction(transaction, paymentMethod, BigDecimal.ZERO, BigDecimal.ZERO);
                } else if (latest.status.isFinal() && latest.status != PaymentOrder.PaymentStatus.SUCCESS) {
                    poller.stop();
                    dialog.close();
                    showError(i18n.get("payment.not_completed") + ": " + latest.status.getDisplayName());
                }
            }));
        }));
        poller.setCycleCount(Timeline.INDEFINITE);

        dialog.setOnHidden(event -> {
            poller.stop();
            setPaymentInProgress(false);
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

        setPaymentInProgress(true);
        poller.play();
        dialog.show();
    }

    /** mock 模式下触发一次模拟支付回调，轮询会自动发现支付成功并完成交易 */
    private void simulateMockPayment(PaymentOrder paymentOrder, Label status, Button simulateBtn) {
        simulateBtn.setDisable(true);
        try {
            Map<String, String> notify = new java.util.HashMap<>();
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

    private void setPaymentInProgress(boolean inProgress) {
        paymentInProgress = inProgress;
        cartTable.setDisable(inProgress);
        productTable.setDisable(inProgress);
        searchField.setDisable(inProgress);
        memberPhoneField.setDisable(inProgress);
        // CartView.fxml 未提供 addButton/removeButton 节点（遗留字段），必须判空
        if (addButton != null) {
            addButton.setDisable(inProgress);
        }
        if (removeButton != null) {
            removeButton.setDisable(inProgress);
        }
        clearButton.setDisable(inProgress);
        updateStatistics();
    }

    /**
     * 处理支付
     * @param paymentMethod 支付方式
     */
    public void handlePayment(String paymentMethod) {
        if (cartList.isEmpty()) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.CART_EMPTY_PAYMENT));
            return;
        }

        // 检查是否有活跃班次
        if (!com.cashier.service.DataService.hasActiveShift()) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.NO_ACTIVE_SHIFT));
            return;
        }

        // 确认支付
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18nManager.getInstance().get(I18nKeys.Common.CONFIRM));
        alert.setHeaderText(null);
        alert.setContentText(I18nManager.getInstance().get("runtime.payment_confirm",
                localizePaymentMethod(paymentMethod), CurrencyUtil.format(getFinalAmount().doubleValue())));

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            executePayment(paymentMethod, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    /**
     * 创建交易记录
     * @param paymentMethod 支付方式
     * @param receivedAmount 实收金额
     * @param changeAmount 找零金额
     * @return 交易记录
     */
    private Transaction createTransaction(String paymentMethod) {
        // 生成订单号
        String orderNumber = generateOrderNumber();

        Transaction transaction = new Transaction();
        transaction.transactionId = orderNumber;
        transaction.timestamp = com.cashier.util.DateTimeFormats.formatStandard(LocalDateTime.now(ZoneId.systemDefault()));
        transaction.items = new ArrayList<>();
        
        // 合并相同商品的记录
        Map<Integer, Product> productMap = new java.util.LinkedHashMap<>();
        
        for (CartItem item : cartList) {
            Product product = item.product;
            if (productMap.containsKey(product.id)) {
                // 商品已存在，累加数量
                Product existing = productMap.get(product.id);
                existing.quantity += item.quantity;
            } else {
                // 商品不存在，添加新记录
                Product newProduct = new Product();
                newProduct.id = product.id;
                newProduct.productCode = product.productCode;
                newProduct.barcode = product.barcode;
                newProduct.name = product.name;
                newProduct.price = product.price;
                newProduct.quantity = item.quantity;
                newProduct.category = product.category;
                newProduct.unit = product.unit;
                newProduct.cost = product.cost;
                productMap.put(product.id, newProduct);
            }
        }
        
        // 将合并后的商品列表添加到交易中
        transaction.items.addAll(productMap.values());
        
        // total_amount = 明细原价合计（与触屏收银台、REST API 同一口径）：
        // 写成折后金额会让 total_amount - final_amount 恒为 0，小票/详情的"商品总额"与"实付"也无法体现优惠
        transaction.totalAmount = TransactionService.calculateTotalAmount(cartList);
        transaction.finalAmount = getFinalAmount();
        // 税额按**实付金额**计（业务确认）：税率以小数形式配置（设置界面校验区间 0.0-1.0），
        // 税是价内税，不参与应付金额计算，只随交易记录展示/打印
        transaction.tax = TransactionService.calculateTax(transaction.finalAmount);
        transaction.paymentMethod = paymentMethod;
        
        if (currentMember != null) {
            transaction.memberPhone = currentMember.phone;
        }
        
        if (currentUser != null) {
            transaction.operatorUsername = currentUser.username;
            transaction.operatorName = currentUser.name;
        }
        
        return transaction;
    }

    /**
     * 生成订单号
     * @return 订单号
     */
    private String generateOrderNumber() {
        String ts = com.cashier.util.DateTimeFormats.COMPACT_DATE_TIME_MILLIS.format(LocalDateTime.now(ZoneId.systemDefault()));
        return "ORD" + ts;
    }

    /**
     * 更新统计信息
     */
    private void updateStatistics() {
        CartTotals totals = calculateCartTotals();
        PromotionSelection promotionSelection = selectBestPromotion(totals.totalAmount());
        appliedPromotion = promotionSelection.promotion();

        BigDecimal finalAmount = getFinalAmount();
        BigDecimal discountAmount = totals.totalAmount().subtract(finalAmount);

        updateStatisticsLabels(totals, discountAmount, finalAmount, promotionSelection);
        updatePaymentButtons();
    }

    private CartTotals calculateCartTotals() {
        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cartList) {
            totalQuantity += item.quantity;
            totalAmount = totalAmount.add(item.subtotal);
        }
        return new CartTotals(totalQuantity, totalAmount);
    }

    private PromotionSelection selectBestPromotion(BigDecimal totalAmount) {
        Promotion bestPromotion = null;
        BigDecimal promotionDiscount = BigDecimal.ZERO;
        try {
            List<Promotion> promotions = DAOFactory.getInstance().getPromotionDAO().findActive();
            logger.info("购物车加载到 {} 个活跃促销", promotions.size());

            for (Promotion promotion : promotions) {
                logger.info("购物车检查促销: {} (类型: {}, 门槛: {}, 优惠: {})",
                    promotion.name, promotion.type, promotion.threshold, promotion.discount);

                BigDecimal discount = promotion.calculateDiscount(totalAmount);
                logger.info("购物车促销 {} 的折扣金额: {}", promotion.name, discount);

                if (discount.compareTo(promotionDiscount) > 0) {
                    promotionDiscount = discount;
                    bestPromotion = promotion;
                    logger.info("购物车选择促销: {} (优惠金额: {})", promotion.name, discount);
                }
            }

            if (promotionDiscount.compareTo(BigDecimal.ZERO) > 0) {
                logger.info("购物车最终应用促销: {}，优惠金额: {}",
                    bestPromotion != null ? bestPromotion.name : "无", promotionDiscount);
            }
        } catch (Exception e) {
            logger.error("购物车加载促销数据失败", e);
        }
        return new PromotionSelection(bestPromotion, promotionDiscount);
    }

    private void updateStatisticsLabels(
            CartTotals totals,
            BigDecimal discountAmount,
            BigDecimal finalAmount,
            PromotionSelection promotionSelection) {
        totalQuantityLabel.setText(String.valueOf(totals.totalQuantity()));
        totalAmountLabel.setText(CurrencyUtil.format(totals.totalAmount().doubleValue()));
        memberDiscountLabel.setText(I18nManager.getInstance().get("runtime.discount_rate",
                currentMember != null ? currentMember.getDiscountRate().doubleValue() : 10.0));

        updateDiscountLabel(discountAmount, promotionSelection);
        finalAmountLabel.setText(CurrencyUtil.format(finalAmount.doubleValue()));
    }

    private void updateDiscountLabel(BigDecimal discountAmount, PromotionSelection promotionSelection) {
        BigDecimal promotionDiscount = promotionSelection.discount();
        Promotion selectedPromotion = promotionSelection.promotion();
        if (promotionDiscount.compareTo(BigDecimal.ZERO) > 0 && selectedPromotion != null) {
            discountLabel.setText(I18nManager.getInstance().get("runtime.promotion_discount_period",
                    CurrencyUtil.format(discountAmount.doubleValue()), selectedPromotion.name,
                    CurrencyUtil.format(promotionDiscount.doubleValue())));
        } else if (promotionDiscount.compareTo(BigDecimal.ZERO) > 0) {
            discountLabel.setText(I18nManager.getInstance().get("runtime.promotion_discount",
                    CurrencyUtil.format(discountAmount.doubleValue()), CurrencyUtil.format(promotionDiscount.doubleValue())));
        } else {
            discountLabel.setText(I18nManager.getInstance().get("runtime.discount_display",
                    CurrencyUtil.format(discountAmount.doubleValue())));
        }
    }

    private void updatePaymentButtons() {
        // 更新支付按钮状态
        boolean hasItems = !cartList.isEmpty();
        cashButton.setDisable(!hasItems || paymentInProgress);
        wechatButton.setDisable(!hasItems || paymentInProgress
            || !PaymentService.isChannelAvailable(PaymentOrder.PaymentChannel.WECHAT));
        alipayButton.setDisable(!hasItems || paymentInProgress
            || !PaymentService.isChannelAvailable(PaymentOrder.PaymentChannel.ALIPAY));
        cardButton.setDisable(!hasItems || paymentInProgress);
    }

    private record CartTotals(int totalQuantity, BigDecimal totalAmount) {
    }

    private record PromotionSelection(Promotion promotion, BigDecimal discount) {
    }

    /**
     * 显示快捷键帮助
     */
    @FXML
    private void showShortcutHelp() {
        String shortcuts =
            i18n.get("shortcut.help.pos_title") + ":\n\n" +
            i18n.get("shortcut.help.category_product") + ":\n" +
            i18n.get("shortcut.help.f1_add") + "\n" +
            i18n.get("shortcut.help.delete_remove") + "\n" +
            i18n.get("shortcut.help.ctrl_l_clear") + "\n" +
            i18n.get("shortcut.help.double_click_add") + "\n\n" +
            i18n.get("shortcut.help.category_quantity") + ":\n" +
            i18n.get("shortcut.help.num_1_9") + "\n" +
            i18n.get("shortcut.help.num_0_remove") + "\n" +
            i18n.get("shortcut.help.plus_inc") + "\n" +
            i18n.get("shortcut.help.minus_desc") + "\n" +
            i18n.get("shortcut.help.pageup_inc") + "\n" +
            i18n.get("shortcut.help.pagedown_desc") + "\n\n" +
            i18n.get("shortcut.help.category_search") + ":\n" +
            i18n.get("shortcut.help.ctrl_f_search") + "\n" +
            i18n.get("shortcut.help.enter_search") + "\n" +
            i18n.get("shortcut.help.esc_clear_search") + "\n\n" +
            i18n.get("shortcut.help.category_member") + ":\n" +
            i18n.get("shortcut.help.ctrl_m_member") + "\n" +
            i18n.get("shortcut.help.enter_member") + "\n" +
            i18n.get("shortcut.help.esc_clear_member") + "\n\n" +
            i18n.get("shortcut.help.category_payment") + ":\n" +
            i18n.get("shortcut.help.f8_cash") + "\n" +
            i18n.get("shortcut.help.ctrl1_wechat") + "\n" +
            i18n.get("shortcut.help.ctrl2_alipay") + "\n" +
            i18n.get("shortcut.help.ctrl3_card");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(i18n.get("shortcut.help.title"));
        alert.setHeaderText(null);
        alert.setContentText(shortcuts);
        alert.getDialogPane().setPrefWidth(500);
        alert.showAndWait();
    }

    /**
     * 显示错误信息
     * @param message 错误消息
     */
    private void showError(String message) {
        com.cashier.util.FXUtils.showError(message);
    }

    /**
     * 显示提示信息
     * @param message 提示消息
     */
    private void showInfo(String message) {
        com.cashier.util.StatusBarManager.updateStatus(message);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18nManager.getInstance().get(I18nKeys.Common.TIP));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 获取最终金额（会员折扣 + 促销优惠），保留 2 位小数。
     *
     * <p>与触屏收银台共用 {@link TransactionService} 的口径。此前这里自己算且不做四舍五入，
     * 而界面标签按 2 位小数显示，会出现"标签显示 ¥1.80、实际应付 1.8050，收银员按显示金额
     * 收款却被判为金额不足"的问题。</p>
     *
     * @return 最终应付金额
     */
    private BigDecimal getFinalAmount() {
        return TransactionService.calculateFinalAmount(cartList, currentMember, appliedPromotion);
    }

    /**
     * 显示成功消息
     * @param paymentMethod 支付方式
     * @param transaction 交易记录
     * @param receivedAmount 实收金额
     * @param changeAmount 找零金额
     */
    private void showSuccess(String paymentMethod, Transaction transaction, double receivedAmount, double changeAmount) {
        I18nManager i18n = I18nManager.getInstance();
        String message = i18n.get("payment.success.details",
            transaction.transactionId,
            localizePaymentMethod(paymentMethod),
            CurrencyUtil.format(getFinalAmount().doubleValue()),
            cartList.size());

        // 如果是现金支付，显示实收和找零
        if ("现金".equals(paymentMethod)) {
            message += i18n.get("payment.success.cash_details",
                CurrencyUtil.format(receivedAmount), CurrencyUtil.format(changeAmount));
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(i18n.get(I18nKeys.Label.SUCCESS));
        alert.setHeaderText(i18n.get("payment.success.header"));
        alert.setContentText(message);
        alert.showAndWait();

        // 支付成功后，焦点回到搜索框，方便继续扫描商品
        searchField.requestFocus();
    }

    private String localizePaymentMethod(String paymentMethod) {
        String key = switch (paymentMethod) {
            case "现金" -> I18nKeys.Runtime.PAYMENT_CASH;
            case "微信" -> I18nKeys.Runtime.PAYMENT_WECHAT;
            case "支付宝" -> I18nKeys.Runtime.PAYMENT_ALIPAY;
            case "银行卡" -> I18nKeys.Runtime.PAYMENT_CARD;
            default -> null;
        };
        return key == null ? paymentMethod : I18nManager.getInstance().get(key);
    }

    /**
     * 播放扫描成功音效
     */
    private void playScanSuccessSound() {
        try {
            javafx.scene.media.Media sound = new javafx.scene.media.Media(
                getClass().getResource(SCAN_SUCCESS_SOUND).toString()
            );
            javafx.scene.media.MediaPlayer mediaPlayer = new javafx.scene.media.MediaPlayer(sound);
            mediaPlayer.play();
            logger.debug("播放扫描成功音效");
        } catch (Exception e) {
            logger.debug("播放扫描成功音效失败（音效文件可能不存在）: {}", e.getMessage());
        }
    }

    /**
     * 播放扫描错误音效
     */
    private void playScanErrorSound() {
        try {
            javafx.scene.media.Media sound = new javafx.scene.media.Media(
                getClass().getResource(SCAN_ERROR_SOUND).toString()
            );
            javafx.scene.media.MediaPlayer mediaPlayer = new javafx.scene.media.MediaPlayer(sound);
            mediaPlayer.play();
            logger.debug("播放扫描错误音效");
        } catch (Exception e) {
            logger.debug("播放扫描错误音效失败（音效文件可能不存在）: {}", e.getMessage());
        }
    }

    /**
     * 播放扫描未找到音效
     */
    private void playScanNotFoundSound() {
        try {
            javafx.scene.media.Media sound = new javafx.scene.media.Media(
                getClass().getResource(SCAN_NOT_FOUND_SOUND).toString()
            );
            javafx.scene.media.MediaPlayer mediaPlayer = new javafx.scene.media.MediaPlayer(sound);
            mediaPlayer.play();
            logger.debug("播放扫描未找到音效");
        } catch (Exception e) {
            logger.debug("播放扫描未找到音效失败（音效文件可能不存在）: {}", e.getMessage());
        }
    }

    /**
     * 添加视觉闪烁效果
     * @param table 要闪烁的表格
     */
    private void flashTable(TableView<?> table) {
        table.getStyleClass().remove("scan-success-flash");
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, event -> {
                table.getStyleClass().add("scan-success-flash");
            }),
            new KeyFrame(Duration.millis(200), event -> {
                table.getStyleClass().remove("scan-success-flash");
            })
        );
        timeline.play();
    }

    /**
     * 显示扫描提示消息（在状态栏显示，不弹出提示框）
     * @param message 消息内容
     * @param success 是否成功
     */
    private void showScanMessage(String message, ScanMessageLevel level) {
        // 在状态栏显示消息，不弹出提示框
        switch (level) {
            case SUCCESS -> com.cashier.util.StatusBarManager.updateSuccess(message);
            case WARNING -> com.cashier.util.StatusBarManager.updateWarning(message);
            case ERROR -> com.cashier.util.StatusBarManager.updateError(message);
            default -> logger.warn("未知扫码消息级别: {}", level);
        }
        logger.debug("扫描消息: {}", message);
    }

    private void registerScannerFocusTarget() {
        if (!scannerFocusRegistered) {
            scannerManager.getFocusManager().registerFocusTarget(scannerFocusTarget);
            scannerFocusRegistered = true;
            logger.debug("已注册收银台扫码焦点目标");
        }
    }

    public void dispose() {
        if (scannerFocusRegistered) {
            scannerManager.getFocusManager().unregisterFocusTarget(scannerFocusTarget);
            scannerFocusRegistered = false;
            logger.debug("已注销收银台扫码焦点目标");
        }

        // 从主场景移除键盘过滤器，避免旧控制器残留并继续 consume 快捷键
        if (shortcutScene != null && sceneKeyFilter != null) {
            shortcutScene.removeEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, sceneKeyFilter);
            logger.debug("已移除收银台场景键盘过滤器");
        }
        shortcutScene = null;
        sceneKeyFilter = null;
    }

    private FocusTarget createScannerFocusTarget() {
        return new FocusTarget() {
            @Override
            public String getName() {
                return "cart-search";
            }

            @Override
            public void gainFocus() {
                javafx.application.Platform.runLater(() -> {
                    if (searchField != null && !searchField.isDisabled()) {
                        searchField.requestFocus();
                    }
                });
            }

            @Override
            public void loseFocus() {
            }

            @Override
            public boolean canReceiveFocus() {
                return searchField != null && !searchField.isDisabled() && !paymentInProgress;
            }

            @Override
            public boolean isScanTarget() {
                return true;
            }

            @Override
            public void onKeyboardInput(String input) {
            }

            @Override
            public void onScanInput(String input) {
                javafx.application.Platform.runLater(() -> {
                    if (searchField != null) {
                        searchField.setText(input);
                        searchField.positionCaret(searchField.getText().length());
                    }
                });
            }

            @Override
            public void onScanComplete(String input) {
                javafx.application.Platform.runLater(() -> addScannedProductToCart(input));
            }
        };
    }

    /**
     * 刷新购物车
     */
    public void refreshCart() {
        loadInventory();
        updateStatistics();
    }

    /**
     * 获取购物车列表
     * @return 购物车列表
     */
    public List<CartItem> getCartItems() {
        return new ArrayList<>(cartList);
    }

    /**
     * 获取总数量
     * @return 总数量
     */
    public int getTotalQuantity() {
        int total = 0;
        for (CartItem item : cartList) {
            total += item.quantity;
        }
        return total;
    }

    /**
     * 清空购物车
     */
    public void clear() {
        cartMap.clear();
        cartList.clear();

        // 重置已支付金额
        alreadyPaidAmount = BigDecimal.ZERO;

        // 清除会员信息
        currentMember = null;
        memberPhoneField.clear();
        memberInfoLabel.setText("");

        updateStatistics();
        updateButtonStates();

        // 清空后，焦点回到搜索框，方便继续扫描商品
        searchField.requestFocus();
    }

    /**
     * 刷新最新的库存数据
     * 从数据库重新加载库存数据，确保使用最新数据
     */
    private void refreshLatestInventory() {
        logger.info("CartController: 刷新库存数据...");
        try {
            replaceVisibleProducts(searchProducts(searchField.getText()));
            logger.info("CartController: 库存数据刷新完成，共 {} 个商品", inventoryMap.size());
        } catch (SQLException e) {
            logger.error("刷新库存数据失败", e);
        }
    }

    private List<Product> findExactScanMatches(String scanText) throws SQLException {
        List<Product> matches = new ArrayList<>();
        Product barcodeMatch = productDAO.findByBarcode(scanText);
        if (barcodeMatch != null) {
            matches.add(barcodeMatch);
        }

        Product codeMatch = productDAO.findByProductCode(scanText);
        if (codeMatch != null && matches.stream().noneMatch(product -> product.id == codeMatch.id)) {
            matches.add(codeMatch);
        }

        Product nameMatch = productDAO.findByName(scanText);
        if (nameMatch != null && matches.stream().noneMatch(product -> product.id == nameMatch.id)) {
            matches.add(nameMatch);
        }
        return matches;
    }

    /**
     * 检查班次状态并提示
     */
    private void checkShiftStatus() {
        // 班次查询也走后台：它此前在 FX 线程查库后再弹窗
        UIOptimizer.runInBackground(
            DataService::hasActiveShift,
            active -> {
                if (!active) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle(I18nManager.getInstance().get(I18nKeys.Common.TIP));
                    alert.setHeaderText(null);
                    alert.setContentText(I18nManager.getInstance().get("runtime.start_shift_required"));
                    alert.showAndWait();
                }
                focusSearchField();
            },
            e -> {
                logger.error("检查班次状态失败", e);
                focusSearchField();
            });
    }

    /**
     * 检查购物车是否为空
     * @return 如果购物车为空返回true，否则返回false
     */
    public boolean isCartEmpty() {
        return cartList == null || cartList.isEmpty();
    }

    // ========== 挂单功能 ==========

    /**
     * 处理挂单 (F2)
     */
    public void handleHoldOrder() {
        if (isCartEmpty()) {
            showInfo(I18nManager.getInstance().get("cart.hold.empty_cart"));
            return;
        }

        try {
            // 创建挂单对象
            com.cashier.model.HoldOrder holdOrder = new com.cashier.model.HoldOrder();
            holdOrder.orderNumber = com.cashier.model.HoldOrder.generateOrderNumber();
            holdOrder.userId = currentUser != null ? currentUser.id : 0;

            // 会员信息
            if (currentMember != null) {
                holdOrder.memberId = currentMember.id;
                holdOrder.memberName = currentMember.name;
                holdOrder.memberPhone = currentMember.phone;
            }

            // 金额信息
            holdOrder.totalAmount = calculateHoldOrderTotal();
            holdOrder.discountAmount = calculateHoldOrderDiscount();
            holdOrder.finalAmount = calculateHoldOrderFinal();
            holdOrder.itemCount = cartList.size();

            // 序列化购物车项目
            holdOrder.itemsJson = HoldOrderCodec.serialize(cartList);

            // 保存到数据库
            holdOrderDAO.insert(holdOrder);

            // 清空购物车
            handleClearCart();

            showInfo(I18nManager.getInstance().get("cart.hold.success", holdOrder.orderNumber));

            logger.info("挂单成功: {}", holdOrder.orderNumber);

        } catch (SQLException e) {
            logger.error("挂单失败", e);
            showError(I18nManager.getInstance().get("cart.hold.error") + ": " + e.getMessage());
        }
    }

    /**
     * 处理恢复挂单 (F3)
     */
    public void handleResumeOrder() {
        try {
            // 获取当前用户的活跃挂单列表
            int userId = currentUser != null ? currentUser.id : 0;
            List<com.cashier.model.HoldOrder> holdOrders =
                userId > 0 ? holdOrderDAO.findActiveByUserId(userId)
                           : holdOrderDAO.findAllActive();

            if (holdOrders.isEmpty()) {
                showInfo(I18nManager.getInstance().get("cart.hold.no_orders"));
                return;
            }

            // 显示挂单选择对话框
            showHoldOrderSelectionDialog(holdOrders);

        } catch (SQLException e) {
            logger.error("获取挂单列表失败", e);
            showError(I18nManager.getInstance().get("cart.hold.load_error") + ": " + e.getMessage());
        }
    }

    /**
     * 显示挂单选择对话框
     */
    private void showHoldOrderSelectionDialog(List<com.cashier.model.HoldOrder> holdOrders) {
        // 使用ListView选择对话框
        showHoldOrderListViewDialog(holdOrders);
    }

    /**
     * 显示挂单列表视图对话框
     */
    private void showHoldOrderListViewDialog(List<com.cashier.model.HoldOrder> holdOrders) {
        javafx.scene.control.Dialog<com.cashier.model.HoldOrder> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle(I18nManager.getInstance().get("cart.hold.resume_title"));
        dialog.setHeaderText(I18nManager.getInstance().get("cart.hold.resume_header"));

        // 设置按钮
        dialog.getDialogPane().getButtonTypes().addAll(
            javafx.scene.control.ButtonType.CANCEL,
            new javafx.scene.control.ButtonType("确定", javafx.scene.control.ButtonBar.ButtonData.OK_DONE)
        );

        // 创建ListView
        javafx.scene.control.ListView<com.cashier.model.HoldOrder> listView = new javafx.scene.control.ListView<>();
        listView.getItems().addAll(holdOrders);

        // 设置单元格工厂
        listView.setCellFactory(param -> new javafx.scene.control.ListCell<com.cashier.model.HoldOrder>() {
            @Override
            protected void updateItem(com.cashier.model.HoldOrder order, boolean empty) {
                super.updateItem(order, empty);
                if (empty || order == null) {
                    setText(null);
                } else {
                    setText(I18nManager.getInstance().get("runtime.held_order_item",
                            order.orderNumber, order.holdDate,
                            order.memberName != null ? order.memberName : I18nManager.getInstance().get("runtime.non_member"),
                            CurrencyUtil.format(order.finalAmount.doubleValue()), order.itemCount));
                }
            }
        });

        listView.getSelectionModel().selectFirst();

        // 设置对话框内容
        dialog.getDialogPane().setContent(listView);

        // 转换结果
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == javafx.scene.control.ButtonType.OK) {
                return listView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(order -> {
            if (order != null) {
                resumeOrder(order);
            }
        });
    }

    /**
     * 恢复挂单到购物车。
     *
     * <p>挂单明细要逐条查库（行数多时是 N 次往返），解析与查库全部放后台，
     * 结果回 FX 线程落地，避免恢复大额挂单时界面卡死。</p>
     */
    private void resumeOrder(com.cashier.model.HoldOrder order) {
        UIOptimizer.runInBackground(
            () -> {
                List<CartItem> items = HoldOrderCodec.parse(order.itemsJson, productDAO);
                Member member = null;
                if (order.memberId != null) {
                    try {
                        member = DAOFactory.getInstance().getMemberDAO().findById(order.memberId);
                    } catch (SQLException e) {
                        logger.warn("恢复会员信息失败: {}", e.getMessage());
                    }
                }
                // 标记挂单已恢复
                holdOrderDAO.updateStatus(order.id, 1);
                return new ResumedOrder(items, member);
            },
            resumed -> {
                // 清空当前购物车并填入恢复结果
                cartList.clear();
                cartMap.clear();
                for (CartItem item : resumed.items()) {
                    cartList.add(item);
                    cartMap.put(item.product.name, item);
                }

                currentMember = resumed.member();
                if (currentMember != null) {
                    memberPhoneField.setText(currentMember.phone);
                    memberInfoLabel.setText(currentMember.name + " - " +
                        String.format("%.1f折", currentMember.discount));
                }

                updateStatistics();
                cartTable.setItems(cartList);
                showInfo(I18nManager.getInstance().get("cart.hold.resume_success", order.orderNumber));
                logger.info("恢复挂单成功: {}", order.orderNumber);
            },
            e -> {
                logger.error("恢复挂单失败", e);
                showError(I18nManager.getInstance().get("cart.hold.resume_error") + ": " + e.getMessage());
            });
    }

    /** 恢复挂单的结果：后台解析/查库完成后一次性带回 FX 线程。 */
    private record ResumedOrder(List<CartItem> items, Member member) {}



    /**
     * 获取总金额
     */
    private java.math.BigDecimal calculateHoldOrderTotal() {
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (CartItem item : cartList) {
            total = total.add(item.subtotal);
        }
        return total;
    }

    /**
     * 获取折扣金额
     */
    private java.math.BigDecimal calculateHoldOrderDiscount() {
        java.math.BigDecimal total = calculateHoldOrderTotal();
        java.math.BigDecimal discount = java.math.BigDecimal.ZERO;

        if (currentMember != null) {
            // discount是折扣（如9.5表示95折），计算折扣金额
            double discountRate = currentMember.discount.doubleValue() / 10.0;
            discount = total.multiply(java.math.BigDecimal.valueOf(1 - discountRate));
        }

        return discount;
    }

    /**
     * 获取最终金额
     */
    private java.math.BigDecimal calculateHoldOrderFinal() {
        return calculateHoldOrderTotal().subtract(calculateHoldOrderDiscount());
    }

    /**
     * 设置当前用户
     * @param user 当前登录用户
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        logger.debug("已设置当前用户: {} ({})", user.name, user.getRoleDisplayName());
    }

    /**
     * 聚焦到搜索框
     */
    public void focusSearchField() {
        if (searchField != null) {
            searchField.requestFocus();
            logger.debug("已聚焦到搜索框");
        }
    }
}
