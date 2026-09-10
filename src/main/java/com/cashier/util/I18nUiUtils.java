package com.cashier.util;

import com.cashier.i18n.I18nKeys;

import com.cashier.i18n.I18nManager;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.geometry.Pos;

import java.util.function.Function;

/** UI-only localization helpers for business values stored in Chinese or codes. */
public final class I18nUiUtils {
    private I18nUiUtils() {}

    public static void configureComboBox(ComboBox<String> comboBox, Function<String, String> mapper) {
        comboBox.setButtonCell(createCell(mapper));
        comboBox.setCellFactory(listView -> createCell(mapper));
    }

    private static ListCell<String> createCell(Function<String, String> mapper) {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : mapper.apply(item));
                setAlignment(Pos.CENTER_LEFT);
                setMinHeight(36);
                setPrefHeight(36);
                setMaxWidth(Double.MAX_VALUE);
            }
        };
    }

    public static String dateRange(String value) {
        String key = switch (value) {
            case "今天", "今日报表" -> "date_option.today";
            case "昨天" -> "date_option.yesterday";
            case "本周", "本周报表" -> "date_option.this_week";
            case "上周" -> "date_option.last_week";
            case "本月", "本月报表" -> "date_option.this_month";
            case "上月" -> "date_option.last_month";
            case "全部报表" -> "date_option.all";
            case "自定义", "自定义日期" -> "date_option.custom";
            default -> null;
        };
        return key == null ? value : I18nManager.getInstance().get(key);
    }

    /**
     * 归一化支付方式：历史数据存中文（现金/微信/支付宝/银行卡），筛选下拉框用代码
     * （CASH/WECHAT/ALIPAY/CARD）。两侧统一到代码后再比较，避免永远匹配不上。
     * @param value 中文名称或代码
     * @return 稳定代码；无法识别时原样返回
     */
    public static String canonicalPaymentMethod(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "现金", "CASH" -> "CASH";
            case "微信", "WECHAT" -> "WECHAT";
            case "支付宝", "ALIPAY" -> "ALIPAY";
            case "银行卡", "CARD" -> "CARD";
            default -> value;
        };
    }

    public static String paymentMethod(String value) {
        String canonical = canonicalPaymentMethod(value);
        String key = switch (canonical == null ? "" : canonical) {
            case "全部" -> I18nKeys.Filter.ALL;
            case "CASH" -> I18nKeys.Runtime.PAYMENT_CASH;
            case "WECHAT" -> I18nKeys.Runtime.PAYMENT_WECHAT;
            case "ALIPAY" -> I18nKeys.Runtime.PAYMENT_ALIPAY;
            case "CARD" -> I18nKeys.Runtime.PAYMENT_CARD;
            default -> null;
        };
        return key == null ? value : I18nManager.getInstance().get(key);
    }

    public static String purchaseStatus(String value) {
        String normalized = value == null ? "" : value.toLowerCase();
        String key = switch (normalized) {
            case "pending", "pending_approval", "待审批" -> I18nKeys.Runtime.STATUS_PENDING_APPROVAL;
            case "approved", "已审批", "已批准" -> I18nKeys.Runtime.STATUS_APPROVED;
            case "completed", "已完成" -> I18nKeys.Runtime.STATUS_COMPLETED;
            case "rejected", "已拒绝" -> I18nKeys.Runtime.STATUS_REJECTED;
            default -> null;
        };
        return key == null ? value : I18nManager.getInstance().get(key);
    }

    public static String inventoryStatus(String value) {
        String key = switch (value) {
            case "正常" -> I18nKeys.Inventory.Status.NORMAL;
            case "库存不足" -> I18nKeys.Inventory.Status.LOW_STOCK;
            case "滞销" -> "inventory_report.status.slow";
            case "积压" -> "inventory_report.status.overstock";
            default -> null;
        };
        return key == null ? value : I18nManager.getInstance().get(key);
    }

    public static String inventoryCheckStatus(String value) {
        String normalized = value == null ? "" : value.toLowerCase();
        String key = switch (normalized) {
            case "pending", "待盘点" -> "runtime.status.pending_check";
            case "checking", "盘点中" -> "runtime.status.checking";
            case "completed", "已完成" -> I18nKeys.Runtime.STATUS_COMPLETED;
            default -> null;
        };
        return key == null ? value : I18nManager.getInstance().get(key);
    }

    public static String inventoryCheckType(String value) {
        String normalized = value == null ? "" : value.toLowerCase();
        String key = switch (normalized) {
            case "full", "全盘", "全盤" -> "runtime.check_type_full";
            case "partial", "部分盘点", "部分盤點" -> "runtime.check_type_partial";
            default -> null;
        };
        return key == null ? value : I18nManager.getInstance().get(key);
    }

    public static String itemCondition(String value) {
        String normalized = value == null ? "" : value.toUpperCase();
        String key = switch (normalized) {
            case "GOOD", "完好" -> "runtime.condition_good";
            case "DAMAGED", "损坏" -> "runtime.condition_damaged";
            case "OPENED", "已拆封" -> "runtime.condition_opened";
            default -> null;
        };
        return key == null ? value : I18nManager.getInstance().get(key);
    }
}
