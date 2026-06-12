package com.cashier.controller;

import com.cashier.i18n.I18nManager;
import com.cashier.dao.PromotionDAO;
import com.cashier.model.CartItem;
import com.cashier.model.Promotion;
import com.cashier.service.DataService;
import com.cashier.util.CurrencyUtil;
import com.cashier.model.Member;
import com.cashier.model.Product;
import com.cashier.model.Transaction;
import com.cashier.model.User;
import com.cashier.util.DialogBuilder;
import com.cashier.util.FXUtils;
import com.cashier.util.ReceiptPrinter;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.FormValidator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 结账控制器
 * 处理结账和支付逻辑
 */
public class CheckoutController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(CheckoutController.class);

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
    private TextField couponCodeField;

    @FXML
    private Button verifyCouponButton;

    @FXML
    private Button clearCouponButton;

    @FXML
    private Label couponInfoLabel;

    @FXML
    private Label finalAmountLabel;

    private ObservableList<CartItem> cartList;
    private List<CartItem> cartItems;
    private Member currentMember;
    private User currentUser;
    private String orderNumber;
    private Transaction lastTransaction; // 保存最后完成的交易信息
    private Promotion appliedPromotion; // 当前应用的促销
    private Promotion appliedCoupon; // 当前应用的优惠券

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

        // 会员手机号框 Enter 键监听
        memberPhoneField.setOnAction(event -> handleSearchMember());

        // 设置全局快捷键
        setupShortcuts();
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
            // F8 - 现金支付
            if (event.getCode() == javafx.scene.input.KeyCode.F8) {
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
            // Ctrl+M - 查询会员
            else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.M) {
                memberPhoneField.requestFocus();
                event.consume();
            }
            // Escape - 清空会员信息或取消结账
            else if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                if (memberPhoneField.isFocused()) {
                    memberPhoneField.clear();
                    handleSearchMember();
                    event.consume();
                } else {
                    handleCancel();
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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
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
    public void handleSearchMember() {
        String phone = memberPhoneField.getText().trim();
        if (phone.isEmpty()) {
            currentMember = null;
            memberInfoLabel.setText("");
            updateStatistics();
            return;
        }

        Map<String, Member> members = DataService.loadMembers();
        Member member = members.get(phone);

        if (member != null) {
            currentMember = member;
            memberInfoLabel.setText(String.format("会员: %s (余额: ¥%.2f, 积分: %d)", 
                member.name, member.getBalance(), member.getPoints().intValue()));
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
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cartList) {
            totalQuantity += item.quantity;
            totalAmount = totalAmount.add(item.subtotal);
        }

        // 计算会员折扣
        BigDecimal discountRate = BigDecimal.ONE;  // 默认不打折
        if (currentMember != null) {
            discountRate = currentMember.getDiscountRate().divide(BigDecimal.TEN);  // 将0-10的折扣值转换为0-1的折扣率
        }

        // 计算促销优惠
        appliedPromotion = null;  // 重置当前应用的促销
        BigDecimal promotionDiscount = BigDecimal.ZERO;
        try {
            // 如果用户手动选择了优惠券，使用优惠券
            if (appliedCoupon != null) {
                appliedPromotion = appliedCoupon;
                promotionDiscount = appliedCoupon.calculateDiscount(totalAmount);
                logger.info("使用优惠券: {}，优惠金额: ¥{}", appliedCoupon.name, promotionDiscount);
            } else {
                // 否则自动计算最佳促销（不包括优惠券）
                List<Promotion> promotions = PromotionDAO.findActive();
                logger.info("加载到 {} 个活跃促销", promotions.size());

                for (Promotion promotion : promotions) {
                    // 跳过优惠券类型，只处理满减和打折
                    if ("优惠券".equals(promotion.type)) {
                        continue;
                    }

                    logger.info("检查促销: {} (类型: {}, 门槛: {}, 优惠: {}, 启用: {}, 有效期: {}-{})",
                        promotion.name, promotion.type, promotion.threshold, promotion.discount,
                        promotion.enabled, promotion.startDate, promotion.endDate);

                    BigDecimal discount = promotion.calculateDiscount(totalAmount);
                    logger.info("促销 {} 的折扣金额: {}", promotion.name, discount);

                    if (discount.compareTo(promotionDiscount) > 0) {
                        promotionDiscount = discount;
                        appliedPromotion = promotion;  // 记录应用的促销
                        logger.info("选择促销: {} (优惠金额: {})", promotion.name, discount);
                    }
                }

                if (promotionDiscount.compareTo(BigDecimal.ZERO) > 0) {
                    logger.info("最终应用促销: {}，优惠金额: {}",
                        appliedPromotion != null ? appliedPromotion.name : "无", promotionDiscount);
                }
            }
        } catch (Exception e) {
            logger.error("加载促销数据失败", e);
        }

        // 计算最终金额：先应用会员折扣，再减去促销优惠
        BigDecimal amountAfterMemberDiscount = totalAmount.multiply(discountRate);
        BigDecimal finalAmount = amountAfterMemberDiscount.subtract(promotionDiscount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        // 计算总优惠金额
        BigDecimal totalDiscountAmount = totalAmount.subtract(finalAmount);  // 优惠金额 = 原价 - 应付金额

        totalQuantityLabel.setText(String.valueOf(totalQuantity));
        totalAmountLabel.setText(CurrencyUtil.format(totalAmount.doubleValue()));
        memberDiscountLabel.setText(String.format("%.1f折", currentMember != null ? currentMember.getDiscountRate() : BigDecimal.TEN));

        // 显示优惠明细
        if (promotionDiscount.compareTo(BigDecimal.ZERO) > 0 && appliedPromotion != null) {
            String promotionType = "优惠券".equals(appliedPromotion.type) ? "优惠券" : "促销";
            discountLabel.setText(String.format("-%s (%s: %s - %s)",
                CurrencyUtil.format(totalDiscountAmount.doubleValue()),
                promotionType, appliedPromotion.name,
                CurrencyUtil.format(promotionDiscount.doubleValue())));
        } else if (promotionDiscount.compareTo(BigDecimal.ZERO) > 0) {
            discountLabel.setText(String.format("-%s (促销: %s)",
                CurrencyUtil.format(totalDiscountAmount.doubleValue()),
                CurrencyUtil.format(promotionDiscount.doubleValue())));
        } else {
            discountLabel.setText(CurrencyUtil.format(totalDiscountAmount.doubleValue()));
            discountLabel.setText("-" + discountLabel.getText());
        }

        finalAmountLabel.setText(CurrencyUtil.format(finalAmount.doubleValue()));
    }

    /**
     * 现金支付
     */
    @FXML
    public void handleCashPayment() {
        handlePayment("现金");
    }

    /**
     * 微信支付
     */
    @FXML
    public void handleWechatPayment() {
        handlePayment("微信");
    }

    /**
     * 支付宝支付
     */
    @FXML
    public void handleAlipayPayment() {
        handlePayment("支付宝");
    }

    /**
     * 银行卡支付
     */
    @FXML
    public void handleCardPayment() {
        handlePayment("银行卡");
    }

    /**
     * 处理支付
     * @param paymentMethod 支付方式
     */
    public void handlePayment(String paymentMethod) {
        if (cartList.isEmpty()) {
            showError("购物车为空，无法支付！");
            return;
        }

        // 检查是否有活跃班次
        if (!com.cashier.service.DataService.hasActiveShift()) {
            showError("当前没有开班，请先开班后再进行结算操作！");
            return;
        }

        // 扣减库存
        Map<String, Product> inventory = DataService.loadInventory();
        for (CartItem item : cartList) {
            Product product = inventory.get(item.product.name);
            if (product != null) {
                product.quantity -= item.quantity;
            }
        }
        DataService.saveInventory(inventory);

        // 更新会员余额和积分
        if (currentMember != null) {
            Map<String, Member> members = DataService.loadMembers();
            BigDecimal finalAmount = getFinalAmount();
            currentMember.balance = currentMember.getBalance().subtract(finalAmount);
            currentMember.points = currentMember.getPoints()
                .add(finalAmount.multiply(BigDecimal.TEN).setScale(0, RoundingMode.DOWN)); // 1元=10积分
            members.put(currentMember.phone, currentMember);
            DataService.saveMembers(members);
        }

        // 创建交易记录
        Transaction transaction = createTransaction(paymentMethod);
        saveTransaction(transaction);

        // 增加促销使用次数
        if (appliedPromotion != null) {
            try {
                PromotionDAO.incrementUsage(appliedPromotion.id);
                logger.info("促销 {} 使用次数已增加", appliedPromotion.name);
            } catch (Exception e) {
                logger.error("增加促销使用次数失败", e);
            }
        }

        // 保存交易信息用于打印
        lastTransaction = transaction;

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

        transaction.totalAmount = getFinalAmount();  // 使用最终金额（包含优惠）
        transaction.finalAmount = getFinalAmount();

        // 实现税费计算：从系统设置中读取税率
        Map<String, String> settings = DataService.loadSettings();
        double taxRate = FormValidator.parseDouble(settings.getOrDefault("taxRate", "0.0"));
        transaction.tax = transaction.totalAmount
            .multiply(BigDecimal.valueOf(taxRate))
            .divide(BigDecimal.valueOf(100));
        transaction.paymentMethod = paymentMethod;
        
        if (currentMember != null) {
            transaction.memberPhone = currentMember.phone;
        }
        
        // 设置操作员信息
        if (currentUser != null) {
            transaction.operatorUsername = currentUser.username;
            transaction.operatorName = currentUser.name;
        }
        
        return transaction;
    }

    /**
     * 保存交易记录
     * @param transaction 交易记录
     */
    private void saveTransaction(Transaction transaction) {
        try {
            // 直接插入新交易，而不是批量插入所有交易
            com.cashier.dao.TransactionDAO.insert(transaction);
        } catch (Exception e) {
            showError("保存交易记录失败: " + e.getMessage());
            logger.error("保存交易记录失败", e);
        }
    }

    /**
     * 取消
     */
    @FXML
    public void handleCancel() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18nManager.getInstance().get("common.confirm"));
        alert.setHeaderText(null);
        alert.setContentText("确定要取消结账吗？购物车将被清空。");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            // 关闭结账界面窗口
            if (cartTable.getScene() != null && cartTable.getScene().getWindow() != null) {
                cartTable.getScene().getWindow().hide();
            }
        }
    }

    /**
     * 打印小票
     */
    @FXML
    public void handlePrint() {
        if (lastTransaction == null) {
            FXUtils.showError("没有可打印的交易记录");
            return;
        }

        try {
            // 生成并打印小票
            String receiptPath = ReceiptPrinter.printReceipt(
                lastTransaction,
                new ArrayList<>(cartList),
                currentMember
            );

            if (receiptPath != null) {
                FXUtils.showInfoAlert("打印成功", "小票已打印成功！");
            } else {
                // 如果自动打印失败，生成小票文件并提示用户
                receiptPath = ReceiptPrinter.generateReceiptOnly(
                    lastTransaction,
                    new ArrayList<>(cartList),
                    currentMember
                );

                if (receiptPath != null) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("小票已生成");
                    alert.setHeaderText(null);
                    alert.setContentText("小票文件已生成：\n" + receiptPath + "\n\n是否打开文件？");

                    if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        ReceiptPrinter.openReceiptFile(receiptPath);
                    }
                } else {
                    FXUtils.showError("生成小票失败");
                }
            }
        } catch (Exception e) {
            FXUtils.showError("打印小票失败: " + e.getMessage());
            logger.error("打印小票失败", e);
        }
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
    private BigDecimal getTotalAmount() {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cartList) {
            total = total.add(item.subtotal);
        }
        return total;
    }

    /**
     * 获取最终金额
     * @return 最终金额（包含会员折扣和促销优惠）
     */
    private BigDecimal getFinalAmount() {
        BigDecimal totalAmount = getTotalAmount();

        // 计算会员折扣
        BigDecimal discountRate = currentMember != null ? currentMember.getDiscountRate().divide(BigDecimal.TEN) : BigDecimal.ONE;
        BigDecimal amountAfterMemberDiscount = totalAmount.multiply(discountRate);

        // 计算促销优惠
        BigDecimal promotionDiscount = BigDecimal.ZERO;
        try {
            List<Promotion> promotions = PromotionDAO.findActive();
            for (Promotion promotion : promotions) {
                BigDecimal discount = promotion.calculateDiscount(totalAmount);
                if (discount.compareTo(promotionDiscount) > 0) {
                    promotionDiscount = discount;
                }
            }
        } catch (Exception e) {
            logger.error("加载促销数据失败", e);
        }

        // 返回最终金额：会员折扣后减去促销优惠
        BigDecimal finalAmount = amountAfterMemberDiscount.subtract(promotionDiscount);
        return finalAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : finalAmount;
    }

    /**
     * 显示成功消息
     * @param paymentMethod 支付方式
     * @param transaction 交易记录
     */
    private void showSuccess(String paymentMethod, Transaction transaction) {
        I18nManager i18n = I18nManager.getInstance();
        String content = String.format(
            i18n.get("payment.success") + "\n\n" +
            i18n.get("label.order_id") + ": %s\n" +
            i18n.get("transaction.payment_method") + ": %s\n" +
            i18n.get("transaction.amount") + ": ¥%.2f\n" +
            i18n.get("cart.product_count") + ": %d",
            transaction.transactionId,
            paymentMethod,
            getFinalAmount(),
            cartList.size()
        );

        DialogBuilder.information()
                .title(i18n.get("label.success"))
                .header(i18n.get("payment.success.header"))
                .content(content)
                .width(400)
                .show();
    }

    /**
     * 清空结算信息
     */
    public void clear() {
        // 清除会员信息
        currentMember = null;
        memberPhoneField.clear();
        memberInfoLabel.setText("");

        // 清除优惠券
        clearCoupon();

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
        alert.setTitle(I18nManager.getInstance().get("label.error"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示快捷键帮助
     */
    private void showShortcutHelp() {
        String shortcuts =
            "结账页面快捷键:\n\n" +
            "支付方式:\n" +
            "F8 - 现金支付\n" +
            "Ctrl+1 - 微信支付\n" +
            "Ctrl+2 - 支付宝支付\n" +
            "Ctrl+3 - 银行卡支付\n\n" +
            "会员操作:\n" +
            "Ctrl+M - 聚焦到会员手机号框\n" +
            "Enter - 查询会员（在会员手机号框中）\n" +
            "Escape - 清空会员信息（在会员手机号框中）\n\n" +
            "其他操作:\n" +
            "Escape - 取消结账（非会员手机号框焦点时）\n" +
            "Ctrl+/ - 显示快捷键帮助";

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("快捷键帮助");
        alert.setHeaderText(null);
        alert.setContentText(shortcuts);
        alert.getDialogPane().setPrefWidth(500);
        alert.showAndWait();
    }

    /**
     * 验证优惠券
     */
    @FXML
    public void handleVerifyCoupon() {
        String couponCode = couponCodeField.getText().trim();
        if (couponCode.isEmpty()) {
            showError("请输入优惠券代码！");
            return;
        }

        try {
            // 根据优惠券代码查询促销
            List<Promotion> allPromotions = PromotionDAO.findAll();
            Promotion coupon = null;

            for (Promotion p : allPromotions) {
                if ("优惠券".equals(p.type) && couponCode.equals(p.promotionCode)) {
                    coupon = p;
                    break;
                }
            }

            if (coupon == null) {
                showError("优惠券代码无效！");
                couponInfoLabel.setText("优惠券代码无效");
                couponInfoLabel.setStyle("-fx-text-fill: #F44336;");
                return;
            }

            // 检查优惠券是否启用
            if (!coupon.enabled) {
                showError("优惠券已禁用！");
                couponInfoLabel.setText("优惠券已禁用");
                couponInfoLabel.setStyle("-fx-text-fill: #F44336;");
                return;
            }

            // 检查优惠券是否有效期内
            if (!coupon.isValid()) {
                showError("优惠券已过期！");
                couponInfoLabel.setText("优惠券已过期");
                couponInfoLabel.setStyle("-fx-text-fill: #F44336;");
                return;
            }

            // 检查使用次数
            if (coupon.maxUsage > 0 && coupon.usageCount >= coupon.maxUsage) {
                showError("优惠券已达到最大使用次数！");
                couponInfoLabel.setText("优惠券已达到最大使用次数");
                couponInfoLabel.setStyle("-fx-text-fill: #F44336;");
                return;
            }

            // 验证成功，应用优惠券
            appliedCoupon = coupon;
            couponInfoLabel.setText(String.format("优惠券: %s (面额: ¥%.2f)", coupon.name, coupon.discount));
            couponInfoLabel.setStyle("-fx-text-fill: #4CAF50;");
            clearCouponButton.setDisable(false);
            logger.info("优惠券验证成功: {} (面额: ¥{})", coupon.name, coupon.discount);

            // 重新计算金额
            updateStatistics();

        } catch (Exception e) {
            logger.error("验证优惠券失败", e);
            showError("验证优惠券失败：" + e.getMessage());
        }
    }

    /**
     * 清除优惠券
     */
    @FXML
    public void handleClearCoupon() {
        clearCoupon();
    }

    /**
     * 清除优惠券逻辑
     */
    private void clearCoupon() {
        appliedCoupon = null;
        couponCodeField.clear();
        couponInfoLabel.setText("");
        clearCouponButton.setDisable(true);
        logger.info("优惠券已清除");
    }
}