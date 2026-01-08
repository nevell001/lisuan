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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 结账控制器
 * 处理结账和支付逻辑
 */
public class CheckoutController {

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
    private TextField memberPhoneField;

    @FXML
    private Label memberInfoLabel;

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
    private Button cashButton;

    @FXML
    private Button wechatButton;

    @FXML
    private Button alipayButton;

    @FXML
    private Button cardButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Button printButton;

    @FXML
    private Label orderNumberLabel;

    @FXML
    private Label cashierLabel;

    private ObservableList<CartItem> cartList;
    private List<CartItem> cartItems;
    private Member currentMember;
    private User currentUser;
    private String orderNumber;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 生成订单号
        orderNumber = generateOrderNumber();
        orderNumberLabel.setText("订单号: " + orderNumber);

        // 初始化购物车列表
        cartList = FXCollections.observableArrayList();
        cartTable.setItems(cartList);

        // 设置表格列
        setupTableColumns();

        // 初始化按钮状态
        updateButtonStates();
    }

    /**
     * 设置表格列
     */
    private void setupTableColumns() {
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
     * 生成订单号
     * @return 订单号
     */
    private String generateOrderNumber() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return "ORD" + sdf.format(new Date());
    }

    /**
     * 设置购物车商品
     * @param cartItems 购物车商品列表
     */
    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
        cartList.setAll(cartItems);
        updateStatistics();
    }

    /**
     * 设置当前用户
     * @param user 用户
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        cashierLabel.setText("收银员: " + user.name);
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
            memberInfoLabel.setText(String.format("会员: %s (余额: ¥%.2f, 积分: %d)", 
                member.name, member.balance, member.points));
        } else {
            currentMember = null;
            memberInfoLabel.setText("未找到该会员");
        }

        updateStatistics();
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
    }

    /**
     * 现金支付
     */
    @FXML
    private void handleCashPayment() {
        handlePayment("现金");
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
     * 处理支付
     * @param paymentMethod 支付方式
     */
    private void handlePayment(String paymentMethod) {
        if (cartList.isEmpty()) {
            showError("购物车为空，无法支付！");
            return;
        }

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
        Transaction transaction = createTransaction(paymentMethod);
        saveTransaction(transaction);

        // 显示成功消息
        showSuccess(paymentMethod, transaction);

        // 清除会员信息
        clear();

        // 启用打印按钮
        printButton.setDisable(false);
    }

    /**
     * 创建交易记录
     * @param paymentMethod 支付方式
     * @return 交易记录
     */
    private Transaction createTransaction(String paymentMethod) {
        Transaction transaction = new Transaction();
        transaction.transactionId = orderNumber;
        transaction.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        transaction.items = new ArrayList<>();
        
        for (CartItem item : cartList) {
            transaction.items.add(item.product);
        }
        
        transaction.totalAmount = getTotalAmount();
        transaction.tax = 0.0; // TODO: 实现税费计算
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
     * 取消
     */
    @FXML
    private void handleCancel() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认取消");
        alert.setHeaderText(null);
        alert.setContentText("确定要取消结账吗？");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            // TODO: 关闭结账界面
            Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
            infoAlert.setTitle("提示");
            infoAlert.setHeaderText(null);
            infoAlert.setContentText("结账已取消，购物车已清空。");
            infoAlert.showAndWait();
        }
    }

    /**
     * 打印小票
     */
    @FXML
    private void handlePrint() {
        // TODO: 实现打印功能
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("打印");
        alert.setHeaderText(null);
        alert.setContentText("打印功能正在开发中...");
        alert.showAndWait();
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        boolean hasItems = !cartList.isEmpty();
        cashButton.setDisable(!hasItems);
        wechatButton.setDisable(!hasItems);
        alipayButton.setDisable(!hasItems);
        cardButton.setDisable(!hasItems);
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
     */
    private void showSuccess(String paymentMethod, Transaction transaction) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("支付成功");
        alert.setHeaderText(null);
        alert.setContentText(String.format(
            "支付成功！\n\n" +
            "订单号: %s\n" +
            "支付方式: %s\n" +
            "支付金额: ¥%.2f\n" +
            "商品数量: %d",
            transaction.transactionId,
            paymentMethod,
            getFinalAmount(),
            cartList.size()
        ));
        alert.showAndWait();
    }

    /**
     * 清空结算信息
     */
    public void clear() {
        // 清除会员信息
        currentMember = null;
        memberPhoneField.clear();
        memberInfoLabel.setText("");
        
        // 清空购物车
        cartList.clear();
        updateStatistics();
        updateButtonStates();
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
}
