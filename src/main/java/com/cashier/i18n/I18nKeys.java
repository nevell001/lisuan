package com.cashier.i18n;

/**
 * Centralized i18n key definitions.
 *
 * <p>Keep this class focused on keys only. Use {@link I18n} or
 * {@link I18nManager} to resolve keys into localized text.</p>
 */
public final class I18nKeys {
    private I18nKeys() {
    }

    public static final class Common {
        public static final String ADD = "common.add";
        public static final String CANCEL = "common.cancel";
        public static final String CONFIRM = "common.confirm";
        public static final String DELETE = "common.delete";
        public static final String DESCRIPTION = "common.description";
        public static final String EDIT = "common.edit";
        public static final String NONE = "common.none";
        public static final String OK = "common.ok";
        public static final String SEARCH = "common.search";
        public static final String TIP = "common.tip";
        public static final String WARNING = "common.warning";

        private Common() {
        }
    }

    public static final class Dialog {
        public static final String CONFIRM = "dialog.confirm";

        private Dialog() {
        }
    }

    public static final class Chart {
        public static final String AMOUNT = "chart.amount";
        public static final String CATEGORY = "chart.category";
        public static final String DATE = "chart.date";
        public static final String SERIES_SALES = "chart.series.sales";

        private Chart() {
        }
    }

    public static final class Error {
        public static final String DELETE_DATA = "error.delete_data";
        public static final String EXPORT_DATA = "error.export_data";
        public static final String EXPORT_FAILED = "error.export_failed";
        public static final String IMPORT_DATA = "error.import_data";
        public static final String LOAD_DATA = "error.load_data";
        public static final String SAVE_DATA = "error.save_data";

        private Error() {
        }
    }

    public static final class InventoryAlert {
        public static final String CLOSE = "inventory_alert.close";
        public static final String INFO = "inventory_alert.info";

        private InventoryAlert() {
        }
    }

    public static final class Inventory {
        public static final class Status {
            public static final String LOW_STOCK = "inventory.status.low_stock";
            public static final String NORMAL = "inventory.status.normal";

            private Status() {
            }
        }

        private Inventory() {
        }
    }

    public static final class Label {
        public static final String ERROR = "label.error";
        public static final String EXPORT_FORMAT = "label.export_format";
        public static final String FAILED = "label.failed";
        public static final String PLEASE_SELECT_FORMAT = "label.please_select_format";
        public static final String SUCCESS = "label.success";
        public static final String TRANSACTION_DETAIL = "label.transaction_detail";

        private Label() {
        }
    }

    public static final class MemberEdit {
        public static final String POINTS_HINT = "member.edit.points_hint";

        private MemberEdit() {
        }
    }

    public static final class Menu {
        public static final class Help {
            public static final String ABOUT = "menu.help.about";

            private Help() {
            }
        }

        public static final class Theme {
            public static final String DARK = "menu.theme.dark";
            public static final String LIGHT = "menu.theme.light";
            public static final String LISUAN = "menu.theme.lisuan";

            private Theme() {
            }
        }

        private Menu() {
        }
    }

    public static final class Message {
        public static final String DATA_EMPTY = "message.data.empty";
        public static final String OPERATION_FAILED = "message.operation.failed";

        private Message() {
        }
    }

    public static final class Filter {
        public static final String ALL = "filter.all";
        public static final String ALL_CATEGORIES = "filter.all_categories";

        private Filter() {
        }
    }

    public static final class Nav {
        public static final String CART = "nav.cart";
        public static final String INVENTORY = "nav.inventory";
        public static final String INVENTORY_CHECK = "nav.inventory_check";
        public static final String INVENTORY_REPORT = "nav.inventory_report";
        public static final String MEMBERS = "nav.members";
        public static final String PROFIT_REPORT = "nav.profit_report";
        public static final String PROMOTIONS = "nav.promotions";
        public static final String PURCHASE_APPROVAL = "nav.purchase_approval";
        public static final String PURCHASE_INBOUND = "nav.purchase_inbound";
        public static final String PURCHASE_ORDER = "nav.purchase_order";
        public static final String PURCHASE_REPORT = "nav.purchase_report";
        public static final String RETURN_ORDER = "nav.return_order";
        public static final String SETTINGS = "nav.settings";
        public static final String SHIFT = "nav.shift";
        public static final String STATISTICS = "nav.statistics";
        public static final String SUPPLIER = "nav.supplier";
        public static final String TRANSACTIONS = "nav.transactions";
        public static final String USER_MANAGEMENT = "nav.user_management";

        private Nav() {
        }
    }

    public static final class Report {
        public static final String UNCATEGORIZED = "report.uncategorized";

        private Report() {
        }
    }

    public static final class ReturnApproval {
        public static final String PRODUCT_NAME = "return_approval.product_name";
        public static final String UNIT_PRICE = "return_approval.unit_price";

        private ReturnApproval() {
        }
    }

    public static final class ReturnOrder {
        public static final String CANCEL = "return_order.cancel";

        private ReturnOrder() {
        }
    }

    public static final class Runtime {
        public static final String AMOUNT_DUE = "runtime.amount_due";
        public static final String AMOUNT_PAID = "runtime.amount_paid";
        public static final String AMOUNT_REMAINING = "runtime.amount_remaining";
        public static final String CART_EMPTY_PAYMENT = "runtime.cart_empty_payment";
        public static final String CHANGE_AMOUNT = "runtime.change_amount";
        public static final String CONFIRM_EXIT = "runtime.confirm_exit";
        public static final String EXPORT_FAILED_DETAIL = "runtime.export_failed_detail";
        public static final String EXPORT_SUCCESS_PATH = "runtime.export_success_path";
        public static final String FORMAT_LABEL = "runtime.format_label";
        public static final String INVALID_AMOUNT = "runtime.invalid_amount";
        public static final String INVALID_DATE_RANGE = "runtime.invalid_date_range";
        public static final String NO_ACTIVE_SHIFT = "runtime.no_active_shift";
        public static final String PRINT_NO_PRINTER = "runtime.print_no_printer";
        public static final String ORDER_INFO = "runtime.order_info";
        public static final String PAYMENT_ALIPAY = "runtime.payment.alipay";
        public static final String PAYMENT_AMOUNT_HINT = "runtime.payment_amount_hint";
        public static final String PAYMENT_CARD = "runtime.payment.card";
        public static final String PAYMENT_CASH = "runtime.payment.cash";
        public static final String PAYMENT_WECHAT = "runtime.payment.wechat";
        public static final String PRODUCT_DETAILS = "runtime.product_details";
        public static final String PURCHASE_DATE = "runtime.purchase_date";
        public static final String PURCHASE_ORDER_DETAIL_TITLE = "runtime.purchase_order_detail_title";
        public static final String PURCHASER = "runtime.purchaser";
        public static final String SELECT_DATE_RANGE = "runtime.select_date_range";
        public static final String SELECT_EXPORT_CONTENT = "runtime.select_export_content";
        public static final String EXPORT_CONTENT_LABEL = "runtime.export_content_label";
        public static final String SELECT_EXPORT_CONTENT_HEADER = "runtime.select_export_content_header";
        public static final String SELECT_INVENTORY_CHECK = "runtime.select_inventory_check";
        public static final String SELECT_MEMBER = "runtime.select_member";
        public static final String SELECT_PRODUCT_FIRST = "runtime.select_product_first";
        public static final String SELECT_PROMOTION = "runtime.select_promotion";
        public static final String SELECT_PURCHASE_ORDER = "runtime.select_purchase_order";
        public static final String SELECT_RETURN_ORDER = "runtime.select_return_order";
        public static final String STATUS_APPROVED = "runtime.status.approved";
        public static final String STATUS_COMPLETED = "runtime.status.completed";
        public static final String STATUS_PENDING_APPROVAL = "runtime.status.pending_approval";
        public static final String STATUS_REJECTED = "runtime.status.rejected";
        public static final String SUBTOTAL = "runtime.subtotal";
        public static final String SUPPLIER_SELECT = "runtime.supplier_select";
        public static final String TOTAL_AMOUNT = "runtime.total_amount";

        private Runtime() {
        }
    }

    public static final class Settings {
        public static final String FONT_SIZE_EXTRA_LARGE = "settings.font_size_extra_large";
        public static final String FONT_SIZE_LARGE = "settings.font_size_large";
        public static final String FONT_SIZE_MEDIUM = "settings.font_size_medium";
        public static final String FONT_SIZE_SMALL = "settings.font_size_small";
        public static final String TEST_PRINT = "settings.test_print";
        public static final String SCANNER_TEST = "settings.scanner_test";
        public static final String SCANNER_TEST_PROMPT = "settings.scanner_test_prompt";
        public static final String SCANNER_TEST_OK = "settings.scanner_test_ok";
        public static final String SCANNER_TEST_TOO_LONG = "settings.scanner_test_too_long";
        public static final String SCANNER_TEST_EMPTY = "settings.scanner_test_empty";

        private Settings() {
        }
    }

    public static final class Success {
        public static final String EXPORT = "success.export";
        public static final String SHIFT_END = "success.shift_end";

        private Success() {
        }
    }

    public static final class Checkout {
        public static final String ORDER_NUMBER = "checkout.order_number";

        private Checkout() {
        }
    }

    public static final class Currency {
        public static final String CNY = "currency.cny";

        private Currency() {
        }
    }

    public static final class ProductEdit {
        public static final String AUTO_GENERATE = "product.edit.auto_generate";
        public static final String EDIT = "product.edit";
        public static final String SUPPLIER = "product.edit.supplier";

        private ProductEdit() {
        }
    }

    public static final class Statistics {
        public static final String NO_DATA = "statistics.no_data";

        private Statistics() {
        }
    }

    public static final class Cart {
        public static final String QUANTITY = "cart.quantity";

        private Cart() {
        }
    }

    public static final class Product {
        public static final String STOCK = "product.stock";

        private Product() {
        }
    }

    public static final class Restock {
        public static final String REASON = "restock.reason";

        private Restock() {
        }
    }

    public static final class ReturnOrderList {
        public static final String NOTES_LABEL = "return_order_list.notes_label";
        public static final String STATUS_LABEL = "return_order_list.status_label";

        private ReturnOrderList() {
        }
    }

    public static final class Shortcut {
        public static final String SAVE = "shortcut.save";
        public static final String SELECT_ALL = "shortcut.select_all";

        private Shortcut() {
        }
    }

    public static final class Status {
        public static final String CANCELLED = "status.cancelled";
        public static final String READY = "status.ready";

        private Status() {
        }
    }

    /** 触屏收银台（TouchCartView / TouchCartController）文案；历史 tpos.* 仍是字面量，新增项统一走这里。 */
    public static final class Tpos {
        public static final String CASH_AMOUNT_DUE_LABEL = "tpos.cash.amount_due_label";
        public static final String CASH_EXACT_AMOUNT = "tpos.cash.exact_amount";
        public static final String CASH_CLEAR_AMOUNT = "tpos.cash.clear_amount";
        public static final String CASH_CONFIRM_RECEIPT = "tpos.cash.confirm_receipt";
        public static final String CASH_SECTION_AMOUNT_RECEIVED = "tpos.cash.section_amount_received";
        public static final String CASH_SECTION_QUICK_AMOUNT = "tpos.cash.section_quick_amount";
        public static final String CASH_PARTIAL_PAYMENT_HINT = "tpos.cash.partial_payment_hint";

        private Tpos() {
        }
    }

    public static final class Unit {
        public static final String NAME = "unit.name";

        private Unit() {
        }
    }
}
