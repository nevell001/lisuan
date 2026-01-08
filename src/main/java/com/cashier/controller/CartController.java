package com.cashier.controller;

import com.cashier.model.CartItem;
import com.cashier.model.DataManager;
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 购物车控制器
 * 处理购物车的增删改查和结算
 */
public class CartController {

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
    private Button checkoutButton;

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
    private String orderNumber;

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
        scene.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
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
        });
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
        System.out.println("CartController: 开始加载库存数据...");
        inventory = DataManager.loadInventory();
        System.out.println("CartController: 加载了 " + inventory.size() + " 个商品");
        productList.setAll(inventory.values());
        updateCountLabel();
        System.out.println("CartController: 库存数据加载完成");
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
        
        removeButton.setDisable(!hasCartSelection);
        addButton.setDisable(!hasProductSelection);
        clearButton.setDisable(cartList.isEmpty());
        checkoutButton.setDisable(cartList.isEmpty());
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
        if (quantity <= 0) {
            showError("添加数量必须大于0！");
            return;
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
        }
    }

    /**
     * 搜索商品
     */
    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().trim().toLowerCase();
        if (searchText.isEmpty()) {
            productList.setAll(inventory.values());
        } else {
            productList.setAll(inventory.values().stream()
                .filter(p -> p.name.toLowerCase().contains(searchText) || 
                          p.barcode.toLowerCase().contains(searchText))
                .toList());
        }
        updateCountLabel();
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

        Map<String, Member> members = DataManager.loadMembers();
        Member member = members.get(phone);

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

        double finalAmount = getFinalAmount();
        
        // 创建现金支付对话框
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("现金支付");
        dialog.setHeaderText(null);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        Label amountLabel = new Label(String.format("应付金额: ¥%.2f", finalAmount));
        amountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #F44336;");
        
        Label receivedLabel = new Label("实收金额:");
        TextField receivedField = new TextField();
        receivedField.setPromptText("请输入实收金额");
        
        Label changeLabel = new Label("找零: ¥0.00");
        changeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
        
        grid.add(amountLabel, 0, 0, 2, 1);
        grid.add(receivedLabel, 0, 1);
        grid.add(receivedField, 1, 1);
        grid.add(changeLabel, 0, 2, 2, 1);
        
        // 快捷金额按钮
        Button btn100 = new Button("¥100");
        btn100.setOnAction(e -> {
            receivedField.setText("100");
            receivedField.requestFocus();
        });
        
        Button btn50 = new Button("¥50");
        btn50.setOnAction(e -> {
            receivedField.setText("50");
            receivedField.requestFocus();
        });
        
        Button btn20 = new Button("¥20");
        btn20.setOnAction(e -> {
            receivedField.setText("20");
            receivedField.requestFocus();
        });
        
        HBox quickButtons = new HBox(5, btn100, btn50, btn20);
        grid.add(quickButtons, 0, 3, 2, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType okButtonType = new ButtonType("确认", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);
        
        // 实时计算找零
        receivedField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double received = Double.parseDouble(newVal.trim());
                double change = received - finalAmount;
                if (change >= 0) {
                    changeLabel.setText(String.format("找零: ¥%.2f", change));
                    changeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
                } else {
                    changeLabel.setText(String.format("还需: ¥%.2f", Math.abs(change)));
                    changeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #F44336;");
                }
            } catch (NumberFormatException e) {
                changeLabel.setText("找零: ¥0.00");
                changeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
            }
        });
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                try {
                    double received = Double.parseDouble(receivedField.getText().trim());
                    if (received < finalAmount) {
                        showError("实收金额不足！");
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
        
        dialog.showAndWait().ifPresent(receivedAmount -> {
            // 执行支付
            executePayment("现金", receivedAmount, receivedAmount - finalAmount);
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
        // 扣减库存
        Map<String, Product> inventory = DataManager.loadInventory();
        for (CartItem item : cartList) {
            Product product = inventory.get(item.product.name);
            if (product != null) {
                product.quantity -= item.quantity;
            }
        }
        DataManager.saveInventory(inventory);

        // 更新会员余额和积分
        if (currentMember != null) {
            Map<String, Member> members = DataManager.loadMembers();
            double finalAmount = getFinalAmount();
            currentMember.balance -= finalAmount;
            currentMember.points += (int)(finalAmount * 10); // 1元=10积分
            members.put(currentMember.phone, currentMember);
            DataManager.saveMembers(members);
        }

        // 创建交易记录
        Transaction transaction = createTransaction(paymentMethod, receivedAmount, changeAmount);
        saveTransaction(transaction);

        // 显示成功消息
        showSuccess(paymentMethod, transaction, receivedAmount, changeAmount);

        // 清空购物车
        clear();
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
        
        for (CartItem item : cartList) {
            transaction.items.add(item.product);
        }
        
        transaction.totalAmount = getTotalAmount();
        transaction.tax = 0.0; // TODO: 实现税费计算
        transaction.finalAmount = getFinalAmount();
        transaction.paymentMethod = paymentMethod;
        
        if (currentMember != null) {
            transaction.memberPhone = currentMember.phone;
        }
        
        return transaction;
    }

    /**
     * 保存交易记录
     * @param transaction 交易记录
     */
    private void saveTransaction(Transaction transaction) {
        try {
            List<Transaction> transactions = DataManager.loadTransactions();
            transactions.add(transaction);
            DataManager.saveTransactions(transactions);
        } catch (Exception e) {
            showError("保存交易记录失败: " + e.getMessage());
        }
    }

    /**
     * 生成订单号
     * @return 订单号
     */
    private String generateOrderNumber() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
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

        double finalAmount = totalAmount * discountRate;  // 应付金额 = 原价 * 折扣率
        double discountAmount = totalAmount - finalAmount;  // 优惠金额 = 原价 - 应付金额

        totalQuantityLabel.setText(String.valueOf(totalQuantity));
        totalAmountLabel.setText(String.format("¥%.2f", totalAmount));
        memberDiscountLabel.setText(String.format("%.1f折", currentMember != null ? currentMember.discountRate : 10));
        discountLabel.setText(String.format("-¥%.2f", discountAmount));
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
     * @return 最终金额
     */
    private double getFinalAmount() {
        double totalAmount = getTotalAmount();
        double discountRate = currentMember != null ? currentMember.discountRate / 10.0 : 1.0;
        return totalAmount * discountRate;
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
        
        // 清除会员信息
        currentMember = null;
        memberPhoneField.clear();
        memberInfoLabel.setText("");
        
        updateStatistics();
        updateButtonStates();
    }
}
