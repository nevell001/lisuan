package com.cashier.controller;

import com.cashier.dao.MemberDAO;
import com.cashier.dao.ProductDAO;
import com.cashier.dao.PromotionDAO;
import com.cashier.dao.TransactionDAO;
import com.cashier.model.CartItem;
import com.cashier.model.Promotion;
import com.cashier.service.DataService;
import com.cashier.model.Member;
import com.cashier.model.Product;
import com.cashier.model.Transaction;
import com.cashier.model.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 购物车控制器
 * 处理购物车的增删改查和结算
 */
public class CartController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(CartController.class);

    // 音效文件路径（WAV 格式）
    private static final String SCAN_SUCCESS_SOUND = "/sounds/scan_success.wav";
    private static final String SCAN_ERROR_SOUND = "/sounds/scan_error.wav";
    private static final String SCAN_NOT_FOUND_SOUND = "/sounds/scan_not_found.wav";

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
    private Map<String, Product> inventory;
    private Map<String, CartItem> cartMap = new HashMap<>();
    private Member currentMember;
    private User currentUser;
    private String orderNumber;
    private double alreadyPaidAmount = 0.0; // 已支付金额
    private Promotion appliedPromotion; // 当前应用的促销

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
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            // F1 - 添加商品
            if (event.getCode() == javafx.scene.input.KeyCode.F1) {
                handleAddProduct();
                event.consume();
            }
            // Delete - 移除商品
            else if (event.getCode() == javafx.scene.input.KeyCode.DELETE) {
                handleRemoveProduct();
                event.consume();
            }
            // Ctrl+L - 清空购物车
            else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.L) {
                handleClearCart();
                event.consume();
            }
            // Ctrl+F - 搜索商品
            else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.F) {
                searchField.requestFocus();
                event.consume();
            }
            // Ctrl+M - 查询会员
            else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.M) {
                memberPhoneField.requestFocus();
                event.consume();
            }
            // F8 - 现金支付
            else if (event.getCode() == javafx.scene.input.KeyCode.F8) {
                handleCashPayment();
                event.consume();
            }
            // Ctrl+1 - 微信支付
            else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.DIGIT1) {
                handleWechatPayment();
                event.consume();
            }
            // Ctrl+2 - 支付宝支付
            else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.DIGIT2) {
                handleAlipayPayment();
                event.consume();
            }
            // Ctrl+3 - 银行卡支付
            else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.DIGIT3) {
                handleCardPayment();
                event.consume();
            }
            // Escape - 清空搜索框或会员手机号框
            else if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
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
            // Ctrl+/ - 显示快捷键帮助
            else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.SLASH) {
                showShortcutHelp();
                event.consume();
            }
            // 数字键 1-9 - 快速添加对应数量的商品（选中商品时）
            else if (event.getCode().isDigitKey() && !event.isControlDown() && !event.isAltDown()) {
                CartItem selected = cartTable.getSelectionModel().getSelectedItem();
                if (selected != null && !searchField.isFocused() && !memberPhoneField.isFocused()) {
                    int quantity = event.getText().charAt(0) - '0';
                    if (quantity >= 1 && quantity <= 9) {
                        updateCartItemQuantity(selected, quantity);
                        event.consume();
                    }
                }
            }
            // 数字键 0 - 设置数量为0（移除商品）
            else if (event.getCode() == javafx.scene.input.KeyCode.DIGIT0 && !event.isControlDown() && !event.isAltDown()) {
                CartItem selected = cartTable.getSelectionModel().getSelectedItem();
                if (selected != null && !searchField.isFocused() && !memberPhoneField.isFocused()) {
                    updateCartItemQuantity(selected, 0);
                    event.consume();
                }
            }
            // + 键 - 增加选中商品数量
            else if ((event.getCode() == javafx.scene.input.KeyCode.EQUALS || 
                      event.getCode() == javafx.scene.input.KeyCode.PLUS) && 
                      !event.isControlDown() && !event.isAltDown()) {
                CartItem selected = cartTable.getSelectionModel().getSelectedItem();
                if (selected != null && !searchField.isFocused() && !memberPhoneField.isFocused()) {
                    updateCartItemQuantity(selected, selected.quantity + 1);
                    event.consume();
                }
            }
            // - 键 - 减少选中商品数量
            else if ((event.getCode() == javafx.scene.input.KeyCode.MINUS || 
                      event.getCode() == javafx.scene.input.KeyCode.SUBTRACT) && 
                      !event.isControlDown() && !event.isAltDown()) {
                CartItem selected = cartTable.getSelectionModel().getSelectedItem();
                if (selected != null && !searchField.isFocused() && !memberPhoneField.isFocused() && selected.quantity > 1) {
                    updateCartItemQuantity(selected, selected.quantity - 1);
                    event.consume();
                }
            }
            // PageUp - 增加数量（一次增加5）
            else if (event.getCode() == javafx.scene.input.KeyCode.PAGE_UP && !event.isControlDown()) {
                CartItem selected = cartTable.getSelectionModel().getSelectedItem();
                if (selected != null && !searchField.isFocused() && !memberPhoneField.isFocused()) {
                    updateCartItemQuantity(selected, Math.min(selected.quantity + 5, selected.product.quantity));
                    event.consume();
                }
            }
            // PageDown - 减少数量（一次减少5）
            else if (event.getCode() == javafx.scene.input.KeyCode.PAGE_DOWN && !event.isControlDown()) {
                CartItem selected = cartTable.getSelectionModel().getSelectedItem();
                if (selected != null && !searchField.isFocused() && !memberPhoneField.isFocused() && selected.quantity > 5) {
                    updateCartItemQuantity(selected, selected.quantity - 5);
                    event.consume();
                }
            }
        });
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
            item.subtotal = item.product.price * item.quantity;
            cartList.set(cartList.indexOf(item), item); // 触发更新
        } else {
            showInfo("库存不足！当前库存: " + item.product.quantity);
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
     * 加载库存数据
     */
    private void loadInventory() {
        logger.info("CartController: 开始加载库存数据...");
        inventory = new HashMap<>();
        try {
            List<Product> products = ProductDAO.findAll();
            for (Product product : products) {
                inventory.put(product.name, product);
            }
        } catch (Exception e) {
            logger.error("从数据库加载商品失败", e);
            inventory = DataService.loadInventory();
        }
        logger.info("CartController: 加载了 {} 个商品", inventory.size());
        productList.setAll(inventory.values());
        updateCountLabel();
        logger.info("CartController: 库存数据加载完成");
    }

    /**
     * 更新商品数量标签
     */
    private void updateCountLabel() {
        countLabel.setText("商品数量: " + productList.size());
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
    private void handleAddProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            addToCart(selected, 1);
        }
    }

    /**
     * 添加商品到购物车（内部方法）
     * @param product 商品
     * @param quantity 数量
     */
    private void addToCart(Product product, int quantity) {
        // 检查是否有活跃班次
        if (!com.cashier.service.DataService.hasActiveShift()) {
            showError("当前没有开班，请先开班后再进行交易操作！");
            return;
        }

        if (quantity <= 0) {
            showError("添加数量必须大于0！");
            return;
        }

        // 从数据库获取最新库存数据，确保使用最新库存
        Product latestProduct = null;
        try {
            latestProduct = ProductDAO.findById(product.id);
            if (latestProduct != null) {
                // 更新内存中的库存数据
                inventory.put(product.name, latestProduct);
                product = latestProduct;
            }
        } catch (SQLException e) {
            logger.error("从数据库获取商品最新库存失败", e);
        }

        if (quantity > product.quantity) {
            showError("库存不足！当前库存: " + product.quantity);
            return;
        }

        CartItem cartItem = cartMap.get(product.name);
        if (cartItem != null) {
            // 商品已在购物车中，增加数量
            int newQuantity = cartItem.quantity + quantity;
            if (newQuantity > product.quantity) {
                showError("库存不足！最大可购买数量: " + product.quantity);
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
    }

    /**
     * 从购物车移除商品
     */
    @FXML
    private void handleRemoveProduct() {
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
    private void handleClearCart() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认清空");
        alert.setHeaderText(null);
        alert.setContentText("确定要清空购物车吗？");

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
    private void handleSearch() {
        String searchText = searchField.getText().trim();

        // 在搜索前刷新库存数据，确保使用最新数据
        refreshLatestInventory();

        if (searchText.isEmpty()) {
            productList.setAll(inventory.values());
            updateCountLabel();
            return;
        }

        // 搜索匹配的商品（支持名称和条形码）
        List<Product> matchedProducts = inventory.values().stream()
            .filter(p -> p.name.toLowerCase().contains(searchText.toLowerCase()) ||
                      p.barcode.toLowerCase().contains(searchText.toLowerCase()) ||
                      (p.productCode != null && p.productCode.toLowerCase().contains(searchText.toLowerCase())))
            .toList();

        if (matchedProducts.isEmpty()) {
            // 未找到商品
            playScanNotFoundSound();
            showScanMessage("未找到商品: " + searchText, false);
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
                showScanMessage("已添加: " + product.name, true);
                searchField.clear();
                searchField.requestFocus();
            } else {
                playScanErrorSound();
                showScanMessage("商品库存不足: " + product.name, false);
                searchField.clear();
                searchField.requestFocus();
            }
        } else {
            // 找到多个匹配商品，显示列表让用户选择
            productList.setAll(matchedProducts);
            updateCountLabel();
            playScanSuccessSound();
            showScanMessage("找到 " + matchedProducts.size() + " 个匹配商品，请选择", true);
        }
    }

    /**
     * 去结账
     */
    @FXML
    private void handleCheckout() {
        showError("请选择支付方式完成结账！");
    }

    /**
     * 搜索会员
     */
    @FXML
    private void handleSearchMember() {
        String phone = memberPhoneField.getText().trim();
        if (phone.isEmpty()) {
            currentMember = null;
            memberInfoLabel.setText("");
            updateStatistics();
            return;
        }

        Member member = null;
        try {
            member = MemberDAO.findByPhone(phone);
        } catch (Exception e) {
            logger.error("从数据库查找会员失败", e);
            Map<String, Member> members = DataService.loadMembers();
            member = members.get(phone);
        }

        if (member != null) {
            currentMember = member;
            memberInfoLabel.setText(String.format("会员: %s (余额: ¥%.2f, 积分: %d, 折扣: %.1f折)", 
                member.name, member.balance, (int)member.points, member.discount));
        } else {
            currentMember = null;
            memberInfoLabel.setText("未找到该会员");
        }

        updateStatistics();
    }

    /**
     * 现金支付
     */
    @FXML
    private void handleCashPayment() {
        if (cartList.isEmpty()) {
            showError("购物车为空，无法支付！");
            return;
        }

        // 检查是否有活跃班次
        if (!com.cashier.service.DataService.hasActiveShift()) {
            showError("当前没有开班，请先开班后再进行结算操作！");
            return;
        }

        double finalAmount = getFinalAmount();
        
        // 创建现金支付对话框
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("现金支付");
        dialog.setHeaderText(null);
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new javafx.geometry.Insets(25, 150, 15, 15));
        
        Label amountLabel = new Label(String.format("应付金额: ¥%.2f", finalAmount));
        amountLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #F44336;");
        
        Label paidLabel = new Label(String.format("已支付: ¥%.2f", alreadyPaidAmount));
        paidLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #4CAF50;");
        
        Label remainingLabel = new Label(String.format("还需支付: ¥%.2f", finalAmount - alreadyPaidAmount));
        remainingLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #F44336;");
        
        TextField receivedField = new TextField();
        receivedField.setPromptText("请输入本次支付金额");
        receivedField.setPrefHeight(45);
        receivedField.setStyle("-fx-font-size: 18px;");
        
        Label receivedLabel = new Label("本次支付: ");
        receivedLabel.setStyle("-fx-font-size: 18px;");
        
        Label changeLabel = new Label("找零: ¥0.00");
        changeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
        
        grid.add(amountLabel, 0, 0, 2, 1);
        grid.add(paidLabel, 0, 1, 2, 1);
        grid.add(remainingLabel, 0, 2, 2, 1);
        grid.add(receivedLabel, 0, 3);
        grid.add(receivedField, 1, 3);
        grid.add(changeLabel, 0, 4, 2, 1);
        
        // 快捷金额按钮
        Button btn100 = new Button("¥100");
        btn100.setPrefSize(100, 60);
        btn100.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        btn100.setOnAction(e -> {
            receivedField.setText("100");
            receivedField.requestFocus();
        });

        Button btn50 = new Button("¥50");
        btn50.setPrefSize(100, 60);
        btn50.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        btn50.setOnAction(e -> {
            receivedField.setText("50");
            receivedField.requestFocus();
        });

        Button btn20 = new Button("¥20");
        btn20.setPrefSize(100, 60);
        btn20.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        btn20.setOnAction(e -> {
            receivedField.setText("20");
            receivedField.requestFocus();
        });

        Button btn10 = new Button("¥10");
        btn10.setPrefSize(100, 60);
        btn10.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        btn10.setOnAction(e -> {
            receivedField.setText("10");
            receivedField.requestFocus();
        });

        Button btn5 = new Button("¥5");
        btn5.setPrefSize(100, 60);
        btn5.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        btn5.setOnAction(e -> {
            receivedField.setText("5");
            receivedField.requestFocus();
        });

        HBox quickButtons = new HBox(10, btn100, btn50, btn20, btn10, btn5);
        grid.add(quickButtons, 0, 5, 2, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType okButtonType = new ButtonType("确认", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);
        
        // 实时计算找零
        receivedField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double received = Double.parseDouble(newVal.trim());
                double totalPaid = alreadyPaidAmount + received;
                double remaining = finalAmount - totalPaid;
                if (remaining <= 0) {
                    changeLabel.setText(String.format("找零: ¥%.2f", Math.abs(remaining)));
                    changeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
                } else {
                    changeLabel.setText(String.format("还需: ¥%.2f", remaining));
                    changeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #F44336;");
                }
            } catch (NumberFormatException e) {
                double remaining = finalAmount - alreadyPaidAmount;
                if (remaining <= 0) {
                    changeLabel.setText("找零: ¥0.00");
                } else {
                    changeLabel.setText(String.format("还需: ¥%.2f", remaining));
                }
            }
        });
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                try {
                    double received = Double.parseDouble(receivedField.getText().trim());
                    if (received <= 0) {
                        showError("请输入有效的金额！");
                        return null;
                    }
                    return received;
                } catch (NumberFormatException e) {
                    showError("请输入有效的金额！");
                    return null;
                }
            }
            return null;
        });
        
        // 自动聚焦到输入框并设置按钮大小（合并后的处理）
        dialog.setOnShown(event -> {
            // 聚焦到输入框
            javafx.application.Platform.runLater(receivedField::requestFocus);
            
            // 设置确认按钮大小
            Button okButton = (Button) dialog.getDialogPane().lookupButton(okButtonType);
            if (okButton != null) {
                okButton.setPrefSize(120, 50);
                okButton.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            }
            Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
            if (cancelButton != null) {
                cancelButton.setPrefSize(120, 50);
                cancelButton.setStyle("-fx-font-size: 16px;");
            }
        });
        
        dialog.showAndWait().ifPresent(receivedAmount -> {
            double totalPaid = alreadyPaidAmount + receivedAmount;
            double remaining = finalAmount - totalPaid;
            
            if (remaining <= 0) {
                // 支付完成
                executePayment("现金", totalPaid, Math.abs(remaining));
                alreadyPaidAmount = 0.0; // 重置已支付金额
            } else {
                // 部分支付，继续
                alreadyPaidAmount = totalPaid;
                showInfo(String.format("已支付 ¥%.2f，还需支付 ¥%.2f", totalPaid, remaining));
                // 重新打开现金支付对话框
                handleCashPayment();
            }
        });
    }

    /**
     * 微信支付
     */
    @FXML
    private void handleWechatPayment() {
        handlePayment("微信");
    }

    /**
     * 支付宝支付
     */
    @FXML
    private void handleAlipayPayment() {
        handlePayment("支付宝");
    }

    /**
     * 银行卡支付
     */
    @FXML
    private void handleCardPayment() {
        handlePayment("银行卡");
    }

    /**
     * 执行支付
     * @param paymentMethod 支付方式
     * @param receivedAmount 实收金额（现金支付时使用）
     * @param changeAmount 找零金额（现金支付时使用）
     */
    private void executePayment(String paymentMethod, double receivedAmount, double changeAmount) {
        Connection conn = null;
        try {
            // 获取数据库连接并开始事务
            conn = com.cashier.util.DatabaseManager.getConnection();
            com.cashier.util.DatabaseManager.beginTransaction(conn);

            // 检查并扣减库存
            for (CartItem item : cartList) {
                Product product = inventory.get(item.product.name);
                if (product == null) {
                    throw new SQLException("商品不存在: " + item.product.name);
                }

                // 从数据库获取最新库存
                Product latestProduct = com.cashier.dao.ProductDAO.findById(product.id);
                if (latestProduct == null) {
                    throw new SQLException("商品不存在: " + item.product.name);
                }

                // 检查库存是否足够
                if (latestProduct.quantity < item.quantity) {
                    throw new SQLException("商品 " + item.product.name + " 库存不足！当前库存: " + latestProduct.quantity + ", 需要数量: " + item.quantity);
                }

                // 扣减库存
                product.quantity = latestProduct.quantity - item.quantity;
                product.version = latestProduct.version; // 用于乐观锁

                // 更新数据库库存
                if (!com.cashier.dao.ProductDAO.updateWithVersion(product)) {
                    throw new SQLException("商品 " + item.product.name + " 库存更新失败，可能已被其他操作修改");
                }

                // 更新内存中的库存
                inventory.put(item.product.name, product);
            }

            // 更新会员余额和积分
            if (currentMember != null) {
                double finalAmount = getFinalAmount();

                // 从数据库获取最新会员信息
                com.cashier.model.Member latestMember = com.cashier.dao.MemberDAO.findByPhone(currentMember.phone);
                if (latestMember == null) {
                    throw new SQLException("会员不存在: " + currentMember.phone);
                }

                // 检查余额是否足够
                if (latestMember.balance < finalAmount) {
                    throw new SQLException("会员余额不足！当前余额: " + latestMember.balance + ", 需要支付: " + finalAmount);
                }

                // 更新余额和积分
                currentMember.balance = latestMember.balance - finalAmount;
                currentMember.points = latestMember.points + (int)(finalAmount * 10);

                // 更新数据库
                if (!com.cashier.dao.MemberDAO.update(currentMember)) {
                    throw new SQLException("更新会员信息失败");
                }
            }

            // 创建交易记录
            Transaction transaction = createTransaction(paymentMethod, receivedAmount, changeAmount);
            saveTransactionWithConnection(conn, transaction);

            // 增加促销使用次数
            if (appliedPromotion != null) {
                try {
                    PromotionDAO.incrementUsage(appliedPromotion.id);
                    logger.info("购物车促销 {} 使用次数已增加", appliedPromotion.name);
                } catch (Exception e) {
                    logger.error("购物车增加促销使用次数失败", e);
                }
            }

            // 提交事务
            com.cashier.util.DatabaseManager.commitTransaction(conn);
            logger.info("交易成功完成，交易ID: {}", transaction.transactionId);

            // 显示成功消息
            showSuccess(paymentMethod, transaction, receivedAmount, changeAmount);

            // 清空购物车
            clear();

        } catch (SQLException e) {
            // 回滚事务
            if (conn != null) {
                com.cashier.util.DatabaseManager.rollbackTransaction(conn);
            }
            logger.error("交易失败: " + e.getMessage(), e);
            showError("交易失败: " + e.getMessage());
        } catch (Exception e) {
            // 回滚事务
            if (conn != null) {
                com.cashier.util.DatabaseManager.rollbackTransaction(conn);
            }
            logger.error("交易失败: " + e.getMessage(), e);
            showError("交易失败: " + e.getMessage());
        } finally {
            // 恢复自动提交
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("关闭数据库连接失败", e);
                }
            }
        }
    }

    /**
     * 处理支付
     * @param paymentMethod 支付方式
     */
    private void handlePayment(String paymentMethod) {
        if (cartList.isEmpty()) {
            showError("购物车为空，无法支付！");
            return;
        }

        // 检查是否有活跃班次
        if (!com.cashier.service.DataService.hasActiveShift()) {
            showError("当前没有开班，请先开班后再进行结算操作！");
            return;
        }

        // 确认支付
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认支付");
        alert.setHeaderText(null);
        alert.setContentText(String.format("确定要使用%s支付 ¥%.2f 吗？",
            paymentMethod, getFinalAmount()));

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            executePayment(paymentMethod, 0, 0);
        }
    }

    /**
     * 创建交易记录
     * @param paymentMethod 支付方式
     * @param receivedAmount 实收金额
     * @param changeAmount 找零金额
     * @return 交易记录
     */
    private Transaction createTransaction(String paymentMethod, double receivedAmount, double changeAmount) {
        // 生成订单号
        orderNumber = generateOrderNumber();

        Transaction transaction = new Transaction();
        transaction.transactionId = orderNumber;
        transaction.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
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
        
        transaction.totalAmount = getFinalAmount();  // 使用最终金额（包含会员折扣和促销优惠）
        // 实现税费计算：从系统设置中读取税率
        Map<String, String> settings = DataService.loadSettings();
        double taxRate = Double.parseDouble(settings.getOrDefault("taxRate", "0.0"));
        transaction.tax = transaction.totalAmount * taxRate / 100.0;
        transaction.finalAmount = getFinalAmount();
        transaction.paymentMethod = paymentMethod;
        
        if (currentMember != null) {
            transaction.memberPhone = currentMember.phone;
        }
        
        // 设置操作员信息为 NULL（CartController 无法获取当前用户）
        // TransactionDAO 会处理 NULL 值
        transaction.operatorUsername = null;
        transaction.operatorName = null;
        
        return transaction;
    }

    /**
     * 保存交易记录（使用指定的数据库连接）
     * @param conn 数据库连接
     * @param transaction 交易记录
     * @throws SQLException 如果保存失败
     */
    private void saveTransactionWithConnection(Connection conn, Transaction transaction) throws SQLException {
        TransactionDAO.insertWithConnection(conn, transaction);
    }

    /**
     * 保存交易记录
     * @param transaction 交易记录
     */
    private void saveTransaction(Transaction transaction) {
        try {
            // 保存到数据库
            TransactionDAO.insert(transaction);
        } catch (Exception e) {
            logger.error("保存交易到数据库失败", e);
            // 使用 DataService 保存交易记录
            try {
                List<Transaction> transactions = DataService.loadTransactions();
                transactions.add(transaction);
                DataService.saveTransactions(transactions);
            } catch (Exception ex) {
                showError("保存交易记录失败: " + ex.getMessage());
            }
        }
    }

    /**
     * 生成订单号
     * @return 订单号
     */
    private String generateOrderNumber() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        return "ORD" + sdf.format(new Date());
    }

    /**
     * 更新统计信息
     */
    private void updateStatistics() {
        int totalQuantity = 0;
        double totalAmount = 0.0;

        for (CartItem item : cartList) {
            totalQuantity += item.quantity;
            totalAmount += item.subtotal;
        }

// 计算折扣
        double discountRate = 1.0;  // 默认不打折
        if (currentMember != null) {
            discountRate = currentMember.discountRate / 10.0;  // 将0-10的折扣值转换为0-1的折扣率
        }

        // 计算促销优惠
        appliedPromotion = null;  // 重置当前应用的促销
        double promotionDiscount = 0.0;
        try {
            List<Promotion> promotions = PromotionDAO.findActive();
            logger.info("购物车加载到 {} 个活跃促销", promotions.size());
            
            for (Promotion promotion : promotions) {
                logger.info("购物车检查促销: {} (类型: {}, 门槛: {}, 优惠: {})", 
                    promotion.name, promotion.type, promotion.threshold, promotion.discount);
                
                double discount = promotion.calculateDiscount(totalAmount);
                logger.info("购物车促销 {} 的折扣金额: {}", promotion.name, discount);
                
                if (discount > promotionDiscount) {
                    promotionDiscount = discount;
                    appliedPromotion = promotion;  // 记录应用的促销
                    logger.info("购物车选择促销: {} (优惠金额: {})", promotion.name, discount);
                }
            }
            
            if (promotionDiscount > 0) {
                logger.info("购物车最终应用促销: {}，优惠金额: {}", 
                    appliedPromotion != null ? appliedPromotion.name : "无", promotionDiscount);
            }
        } catch (Exception e) {
            logger.error("购物车加载促销数据失败", e);
        }

        // 计算最终金额：先应用会员折扣，再减去促销优惠
        double amountAfterMemberDiscount = totalAmount * discountRate;
        double finalAmount = Math.max(0, amountAfterMemberDiscount - promotionDiscount);  // 应付金额不能为负数
        
        // 计算总优惠金额
        double discountAmount = totalAmount - finalAmount;  // 优惠金额 = 原价 - 应付金额

        totalQuantityLabel.setText(String.valueOf(totalQuantity));
        totalAmountLabel.setText(String.format("¥%.2f", totalAmount));
        memberDiscountLabel.setText(String.format("%.1f折", currentMember != null ? currentMember.discountRate : 10));
        
        // 显示优惠明细
        if (promotionDiscount > 0 && appliedPromotion != null) {
            discountLabel.setText(String.format("-¥%.2f (促销: %s - ¥%.2f)", 
                discountAmount, appliedPromotion.name, promotionDiscount));
        } else if (promotionDiscount > 0) {
            discountLabel.setText(String.format("-¥%.2f (促销: ¥%.2f)", discountAmount, promotionDiscount));
        } else {
            discountLabel.setText(String.format("-¥%.2f", discountAmount));
        }
        
        finalAmountLabel.setText(String.format("¥%.2f", finalAmount));
        
        // 更新支付按钮状态
        boolean hasItems = !cartList.isEmpty();
        cashButton.setDisable(!hasItems);
        wechatButton.setDisable(!hasItems);
        alipayButton.setDisable(!hasItems);
        cardButton.setDisable(!hasItems);
    }

    /**
     * 显示快捷键帮助
     */
    @FXML
    private void showShortcutHelp() {
        String shortcuts =
            "POS/结账页面快捷键:\n\n" +
            "商品操作:\n" +
            "F1 - 添加商品\n" +
            "Delete - 移除选中商品\n" +
            "Ctrl+L - 清空购物车\n" +
            "双击商品 - 快速添加到购物车\n\n" +
            "数量快捷键（选中商品时）:\n" +
            "数字键 1-9 - 快速设置数量\n" +
            "数字键 0 - 移除商品\n" +
            "+ / = - 增加数量（+1）\n" +
            "- / _ - 减少数量（-1）\n" +
            "PageUp - 增加数量（+5）\n" +
            "PageDown - 减少数量（-5）\n\n" +
            "搜索和查询:\n" +
            "Ctrl+F - 聚焦到搜索框\n" +
            "Enter - 执行搜索（在搜索框中）\n" +
            "Escape - 清空搜索（在搜索框中）\n\n" +
            "会员操作:\n" +
            "Ctrl+M - 聚焦到会员手机号框\n" +
            "Enter - 查询会员（在会员手机号框中）\n" +
            "Escape - 清空会员信息（在会员手机号框中）\n\n" +
            "支付方式:\n" +
            "F8 - 现金支付\n" +
            "Ctrl+1 - 微信支付\n" +
            "Ctrl+2 - 支付宝支付\n" +
            "Ctrl+3 - 银行卡支付";

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("快捷键帮助");
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
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示提示信息
     * @param message 提示消息
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 获取总金额
     * @return 总金额
     */
    private double getTotalAmount() {
        double total = 0.0;
        for (CartItem item : cartList) {
            total += item.subtotal;
        }
        return total;
    }

    /**
     * 获取最终金额
     * @return 最终金额（包含会员折扣和促销优惠）
     */
    private double getFinalAmount() {
        double totalAmount = getTotalAmount();
        
        // 计算会员折扣
        double discountRate = currentMember != null ? currentMember.discountRate / 10.0 : 1.0;
        double amountAfterMemberDiscount = totalAmount * discountRate;
        
        // 计算促销优惠
        double promotionDiscount = 0.0;
        try {
            List<Promotion> promotions = PromotionDAO.findActive();
            for (Promotion promotion : promotions) {
                double discount = promotion.calculateDiscount(totalAmount);
                if (discount > promotionDiscount) {
                    promotionDiscount = discount;
                }
            }
        } catch (Exception e) {
            logger.error("加载促销数据失败", e);
        }
        
        // 返回最终金额：会员折扣后减去促销优惠
        return Math.max(0, amountAfterMemberDiscount - promotionDiscount);
    }

    /**
     * 显示成功消息
     * @param paymentMethod 支付方式
     * @param transaction 交易记录
     * @param receivedAmount 实收金额
     * @param changeAmount 找零金额
     */
    private void showSuccess(String paymentMethod, Transaction transaction, double receivedAmount, double changeAmount) {
        String message = String.format(
            "支付成功！\n\n" +
            "订单号: %s\n" +
            "支付方式: %s\n" +
            "应付金额: ¥%.2f\n" +
            "商品数量: %d",
            transaction.transactionId,
            paymentMethod,
            getFinalAmount(),
            cartList.size()
        );
        
        // 如果是现金支付，显示实收和找零
        if ("现金".equals(paymentMethod)) {
            message += String.format(
                "\n实收金额: ¥%.2f\n" +
                "找零: ¥%.2f",
                receivedAmount,
                changeAmount
            );
        }
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("支付成功");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();

        // 支付成功后，焦点回到搜索框，方便继续扫描商品
        searchField.requestFocus();
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
        String originalStyle = table.getStyle();
        
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, event -> {
                table.setStyle("-fx-background-color: #4CAF50; -fx-opacity: 0.8;");
            }),
            new KeyFrame(Duration.millis(100), event -> {
                table.setStyle("-fx-background-color: #8BC34A; -fx-opacity: 0.9;");
            }),
            new KeyFrame(Duration.millis(200), event -> {
                table.setStyle(originalStyle);
            })
        );
        timeline.play();
    }

    /**
     * 显示扫描提示消息（在状态栏显示，不弹出提示框）
     * @param message 消息内容
     * @param success 是否成功
     */
    private void showScanMessage(String message, boolean success) {
        // 在状态栏显示消息，不弹出提示框
        com.cashier.util.StatusBarManager.updateStatus(message);
        logger.debug("扫描消息: {}", message);
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
        alreadyPaidAmount = 0.0;

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
            List<Product> products = ProductDAO.findAll();
            // 更新内存中的库存数据
            for (Product product : products) {
                inventory.put(product.name, product);
            }
            // 更新商品列表显示
            productList.setAll(inventory.values());
            logger.info("CartController: 库存数据刷新完成，共 {} 个商品", inventory.size());
        } catch (SQLException e) {
            logger.error("刷新库存数据失败", e);
        }
    }

    /**
     * 检查班次状态并提示
     */
    private void checkShiftStatus() {
        if (!com.cashier.service.DataService.hasActiveShift()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("提示");
            alert.setHeaderText(null);
            alert.setContentText("当前没有开班，请先切换到交班页面进行开班操作！");
            alert.showAndWait();
        }
    }

    /**
     * 检查购物车是否为空
     * @return 如果购物车为空返回true，否则返回false
     */
    public boolean isCartEmpty() {
        return cartList == null || cartList.isEmpty();
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
