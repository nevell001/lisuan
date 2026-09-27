package com.cashier.i18n;

/**
 * 国际化字符串常量
 * 用于获取翻译文本
 */
public class I18n {
    
    // ========== 通用 ==========
    public static final String APP_NAME = "app.name";
    public static final String OK = I18nKeys.Common.OK;
    public static final String CANCEL = I18nKeys.Common.CANCEL;
    public static final String SAVE = "common.save";
    public static final String DELETE = I18nKeys.Common.DELETE;
    public static final String EDIT = I18nKeys.Common.EDIT;
    public static final String ADD = I18nKeys.Common.ADD;
    public static final String SEARCH = I18nKeys.Common.SEARCH;
    public static final String REFRESH = "common.refresh";
    public static final String CONFIRM = I18nKeys.Common.CONFIRM;
    public static final String SUCCESS = "common.success";
    public static final String ERROR = "common.error";
    public static final String WARNING = I18nKeys.Common.WARNING;
    public static final String INFO = "common.info";
    public static final String LOADING = "common.loading";
    
    // ========== 菜单 ==========
    public static final String MENU_FILE_EXPORT = "menu.file.export";
    public static final String MENU_HELP = "menu.help";
    public static final String MENU_HELP_ABOUT = I18nKeys.Menu.Help.ABOUT;
    
    // ========== 登录 ==========
    public static final String LOGIN_TITLE = "login.title";
    public static final String LOGIN_USERNAME = "login.username";
    public static final String LOGIN_PASSWORD = "login.password";
    public static final String LOGIN_BUTTON = "login.button";
    
    // ========== POS 收银 ==========
    public static final String POS_TITLE = "pos.title";
    public static final String POS_CART = "pos.cart";
    public static final String POS_TOTAL = "pos.total";
    public static final String POS_PAY = "pos.pay";
    public static final String POS_CASH = "pos.cash";
    public static final String POS_CARD = "pos.card";
    public static final String POS_MOBILE = "pos.mobile";
    public static final String POS_CLEAR = "pos.clear";
    
    // ========== 商品 ==========
    public static final String PRODUCT_NAME = "product.name";
    public static final String PRODUCT_PRICE = "product.price";
    public static final String PRODUCT_STOCK = I18nKeys.Product.STOCK;
    public static final String PRODUCT_CATEGORY = "product.category";
    public static final String PRODUCT_ADD = "product.add";
    public static final String PRODUCT_EDIT = I18nKeys.ProductEdit.EDIT;
    public static final String PRODUCT_DELETE = "product.delete";
    
    // ========== 会员 ==========
    public static final String MEMBER_NAME = "member.name";
    public static final String MEMBER_PHONE = "member.phone";
    public static final String MEMBER_LEVEL = "member.level";
    public static final String MEMBER_POINTS = "member.points";
    public static final String MEMBER_BALANCE = "member.balance";
    public static final String MEMBER_ADD = "member.add";
    
    // ========== 交易 ==========
    
    // ========== 报表 ==========
    public static final String COMMON_TIP = I18nKeys.Common.TIP;
    
    // ========== 发票 ==========
    
    // ========== 设置 ==========
    public static final String SETTINGS_LANGUAGE = "settings.language";
    
    // ========== 状态 ==========
    public static final String STATUS_CANCELLED = I18nKeys.Status.CANCELLED;
    
    // ========== 错误消息 ==========
    public static final String ERROR_LOAD_DATA = I18nKeys.Error.LOAD_DATA;
    public static final String ERROR_SAVE_DATA = I18nKeys.Error.SAVE_DATA;
    public static final String ERROR_DELETE_DATA = I18nKeys.Error.DELETE_DATA;
    public static final String ERROR_EXPORT_DATA = I18nKeys.Error.EXPORT_DATA;
    public static final String ERROR_IMPORT_DATA = I18nKeys.Error.IMPORT_DATA;
    public static final String ERROR_EXPORT_FAILED = I18nKeys.Error.EXPORT_FAILED;

    // ========== 成功消息 ==========
    public static final String SUCCESS_EXPORT = I18nKeys.Success.EXPORT;
    public static final String SUCCESS_EXPORT_FILE = "success.export_file";
    public static final String SUCCESS_CREATE_RETURN = "success.create_return";
    public static final String SUCCESS_SHIFT_END = I18nKeys.Success.SHIFT_END;

    // ========== 通用标签 ==========
    public static final String LABEL_SHIFT_ID = "label.shift_id";
    public static final String LABEL_OPERATOR = "label.operator";
    public static final String LABEL_SHIFT_DURATION = "label.shift_duration";
    public static final String LABEL_TRANSACTION_COUNT = "label.transaction_count";
    public static final String LABEL_REVENUE = "label.revenue";
    public static final String LABEL_PAYMENT_DETAIL = "label.payment_detail";
    public static final String LABEL_CASH = "label.cash";
    public static final String LABEL_WECHAT = "label.wechat";
    public static final String LABEL_ALIPAY = "label.alipay";
    public static final String LABEL_CARD = "label.card";
    public static final String LABEL_TRANSACTION_DETAIL = I18nKeys.Label.TRANSACTION_DETAIL;
    public static final String LABEL_EXPORT_FORMAT = I18nKeys.Label.EXPORT_FORMAT;
    public static final String LABEL_PLEASE_SELECT_FORMAT = I18nKeys.Label.PLEASE_SELECT_FORMAT;
    public static final String LABEL_FORMAT = "label.format";
    public static final String LABEL_ERROR = I18nKeys.Label.ERROR;
    public static final String LABEL_SUCCESS = I18nKeys.Label.SUCCESS;
    public static final String LABEL_FAILED = I18nKeys.Label.FAILED;

    /**
     * 获取翻译文本
     */
    public static String t(String key) {
        return I18nManager.getInstance().get(key);
    }
    
    /**
     * 获取翻译文本（带参数）
     */
    public static String t(String key, Object... params) {
        return I18nManager.getInstance().get(key, params);
    }
}
