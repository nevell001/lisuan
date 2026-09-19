package com.cashier.controller;

import com.cashier.i18n.I18nKeys;
import com.cashier.i18n.I18nManager;
import com.cashier.model.CartItem;
import com.cashier.model.Product;
import com.cashier.util.CurrencyUtil;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * 触屏收银台的视图构建工厂。
 *
 * <p>把“怎么画”从 {@link TouchCartController}（负责“什么时候画、点了做什么”）拆出来：
 * 这里的每个方法都是无状态的节点构建器，交互行为通过回调参数注入，
 * 因此可以独立演进样式而不动控制器逻辑，控制器也不再被上百行布局代码淹没。</p>
 */
final class TouchCartViewFactory {

    private static final I18nManager i18n = I18nManager.getInstance();

    private TouchCartViewFactory() {
    }

    static Label message(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("fs-18");
        return label;
    }

    static ToggleButton categoryButton(String label, String categoryName, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(label);
        btn.getStyleClass().add("tpos-category-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setUserData(categoryName); // HOT_CATEGORY_KEY = 热销, null = 全部, 其他 = 分类名
        btn.setToggleGroup(group);
        return btn;
    }

    /** 商品卡片:首字符色块 + 名称 + 库存 + 价格(无图标字段,用首字符代替) */
    static VBox productCard(Product p, Consumer<Product> onAddToCart) {
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
            card.setOnMouseClicked(e -> onAddToCart.accept(p));
        }
        return card;
    }

    static HBox cartRow(CartItem item, Consumer<CartItem> onIncrement,
                        Consumer<CartItem> onDecrement, Consumer<CartItem> onRemove) {
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
        minus.setOnAction(e -> onDecrement.accept(item));

        Label qty = new Label(String.valueOf(item.quantity));
        qty.getStyleClass().add("tpos-qty-val");

        Button plus = new Button("+");
        plus.getStyleClass().add("tpos-qty-plus");
        plus.setOnAction(e -> onIncrement.accept(item));

        Button remove = new Button("×");
        remove.getStyleClass().add("tpos-remove-btn");
        remove.setOnAction(e -> onRemove.accept(item));

        ctrl.getChildren().addAll(minus, qty, plus, remove);
        right.getChildren().addAll(subtotal, ctrl);

        row.getChildren().addAll(info, right);
        return row;
    }

    // ===== 现金支付弹窗 =====

    /** 创建应付金额展示行 */
    static HBox cashDueBox(BigDecimal finalAmount) {
        Label dueTitleLabel = new Label(i18n.get(I18nKeys.Tpos.CASH_AMOUNT_DUE_LABEL));
        dueTitleLabel.getStyleClass().add("cash-section-title");

        Label dueLabel = new Label(CurrencyUtil.format(finalAmount.doubleValue()));
        dueLabel.getStyleClass().add("cash-due-highlight");

        HBox dueBox = new HBox(8, dueTitleLabel, dueLabel);
        dueBox.setAlignment(Pos.CENTER_LEFT);
        return dueBox;
    }

    /** 创建部分付款的“已付/还需”展示行，未发生部分付款时返回 null */
    static HBox cashPartialBox(BigDecimal cashReceivedAmount, BigDecimal remainingAmount) {
        if (cashReceivedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        Label paidLabel = new Label(i18n.get(I18nKeys.Runtime.AMOUNT_PAID,
            CurrencyUtil.format(cashReceivedAmount.doubleValue())));
        paidLabel.getStyleClass().add("cash-paid-label");
        Label sep = new Label("  |  ");
        sep.getStyleClass().add("cash-separator");
        Label remainLabel = new Label(i18n.get(I18nKeys.Runtime.AMOUNT_REMAINING,
            CurrencyUtil.format(remainingAmount.doubleValue())));
        remainLabel.getStyleClass().add("cash-remain-label");

        HBox partialBox = new HBox(4, paidLabel, sep, remainLabel);
        partialBox.setAlignment(Pos.CENTER_LEFT);
        return partialBox;
    }

    /** 创建现金支付弹窗的分节标题 */
    static Label cashSectionTitle(String text) {
        Label title = new Label(text);
        title.getStyleClass().add("cash-section-title");
        return title;
    }

    /** 创建收款金额输入框 */
    static TextField cashInputField() {
        TextField receivedField = new TextField();
        receivedField.setPromptText(i18n.get(I18nKeys.Runtime.PAYMENT_AMOUNT_HINT));
        receivedField.setPrefHeight(56);
        receivedField.setMaxWidth(Double.MAX_VALUE);
        receivedField.getStyleClass().add("cash-input-field");
        return receivedField;
    }

    /** 创建快捷面额按钮区（含精确金额/清除按钮） */
    static GridPane cashDenominationGrid(TextField receivedField, BigDecimal finalAmount) {
        String symbol = CurrencyUtil.getSymbol();
        int[] amounts = {100, 50, 20, 10, 5, 1};
        GridPane denomGrid = new GridPane();
        denomGrid.setHgap(10);
        denomGrid.setVgap(10);
        denomGrid.setAlignment(Pos.CENTER);

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

        Button exactBtn = new Button(i18n.get(I18nKeys.Tpos.CASH_EXACT_AMOUNT));
        exactBtn.setPrefSize(110, 62);
        exactBtn.getStyleClass().add("cash-exact-btn");
        exactBtn.setOnAction(e -> {
            receivedField.setText(finalAmount.toPlainString());
            receivedField.requestFocus();
        });
        denomGrid.add(exactBtn, 2, 1);

        Button clearBtn = new Button(i18n.get(I18nKeys.Tpos.CASH_CLEAR_AMOUNT));
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
    static Label cashStatusLabel(TextField receivedField, BigDecimal cashReceivedAmount, BigDecimal finalAmount) {
        Label statusLabel = new Label(i18n.get(I18nKeys.Runtime.PAYMENT_AMOUNT_HINT));
        statusLabel.getStyleClass().add("cash-status-default");
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setPrefHeight(40);

        receivedField.textProperty().addListener((o, ov, nv) -> {
            try {
                BigDecimal thisPayment = new BigDecimal(nv.trim());
                BigDecimal totalAfterThis = cashReceivedAmount.add(thisPayment);
                BigDecimal diff = totalAfterThis.subtract(finalAmount);

                if (thisPayment.compareTo(BigDecimal.ZERO) <= 0) {
                    statusLabel.setText(i18n.get(I18nKeys.Runtime.PAYMENT_AMOUNT_HINT));
                    statusLabel.getStyleClass().setAll("cash-status-default");
                } else if (totalAfterThis.compareTo(finalAmount) < 0) {
                    BigDecimal stillNeed = finalAmount.subtract(totalAfterThis);
                    statusLabel.setText(i18n.get(I18nKeys.Runtime.AMOUNT_REMAINING, CurrencyUtil.format(stillNeed.doubleValue())));
                    statusLabel.getStyleClass().setAll("cash-status-warn");
                } else {
                    statusLabel.setText(i18n.get(I18nKeys.Runtime.CHANGE_AMOUNT, CurrencyUtil.format(diff.doubleValue())));
                    statusLabel.getStyleClass().setAll("cash-status-change");
                }
            } catch (NumberFormatException e) {
                statusLabel.setText(i18n.get(I18nKeys.Runtime.PAYMENT_AMOUNT_HINT));
                statusLabel.getStyleClass().setAll("cash-status-default");
            }
        });
        return statusLabel;
    }

    /** 创建确认收款按钮 */
    static Button cashConfirmButton() {
        Button continueBtn = new Button(i18n.get(I18nKeys.Tpos.CASH_CONFIRM_RECEIPT));
        continueBtn.setDefaultButton(true);
        continueBtn.setPrefHeight(52);
        continueBtn.setMaxWidth(Double.MAX_VALUE);
        continueBtn.getStyleClass().add("cash-confirm-btn");
        return continueBtn;
    }
}
