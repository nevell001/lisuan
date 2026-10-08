package com.cashier.printer;

import org.slf4j.Logger;
import com.cashier.service.DataService;
import com.cashier.util.DateTimeFormats;
import com.cashier.util.LoggerFactoryUtil;

import java.util.Map;


/**
 * 打印工具类
 * 提供便捷的打印方法
 */
public class PrintUtil {
    
    private static final Logger logger = LoggerFactoryUtil.getLogger(PrintUtil.class);
    
    /**
     * 打印销售小票
     * @param transactionId 交易ID
     * @param storeName 门店名称
     * @param cashierName 收银员姓名
     * @param items 商品列表
     * @param totalQuantity 总数量
     * @param totalAmount 总金额
     * @param discountAmount 折扣金额
     * @param finalAmount 应收金额
     * @param paidAmount 实收金额
     * @param changeAmount 找零金额
     * @param paymentMethod 支付方式
     * @param memberInfo 会员信息
     * @return 是否打印成功
     */
    public static boolean printReceipt(String transactionId, String storeName, String cashierName,
                                       String items, int totalQuantity, double totalAmount,
                                       double discountAmount, double finalAmount, double paidAmount,
                                       double changeAmount, String paymentMethod, String memberInfo) {
        return printReceipt(transactionId, storeName, cashierName, items, totalQuantity, totalAmount,
                            discountAmount, finalAmount, paidAmount, changeAmount, paymentMethod,
                            memberInfo, true);
    }

    /**
     * 打印销售小票
     * @param printLogo 是否打印 Logo（来自设置页）
     * @return 是否打印成功
     */
    public static boolean printReceipt(String transactionId, String storeName, String cashierName,
                                       String items, int totalQuantity, double totalAmount,
                                       double discountAmount, double finalAmount, double paidAmount,
                                       double changeAmount, String paymentMethod, String memberInfo,
                                       boolean printLogo) {
        try {
            PrintTemplate template = PrintTemplate.createReceiptTemplate();
            
            // 门店地址/电话取自系统设置：与标准收银台（ReceiptPrinter）同源、格式一致；
            // 留空时 storeContactLines 返回空串，模板不会留下空标签行
            Map<String, String> settings = DataService.loadSettings();
            template.setVariable("storeInfo",
                storeContactLines(settings.get("storeAddress"), settings.get("storePhone")));

            template.setVariable("storeName", storeName);
            template.setVariable("cashierName", cashierName);
            template.setVariable("transactionId", transactionId);
            template.setVariable("transactionTime", formatNow());
            template.setVariable("items", items);
            template.setVariable("totalQuantity", String.valueOf(totalQuantity));
            template.setVariable("totalAmount", String.format(java.util.Locale.ROOT, "%.2f", totalAmount));
            template.setVariable("discountAmount", String.format(java.util.Locale.ROOT, "%.2f", discountAmount));
            template.setVariable("finalAmount", String.format(java.util.Locale.ROOT, "%.2f", finalAmount));
            template.setVariable("paidAmount", String.format(java.util.Locale.ROOT, "%.2f", paidAmount));
            template.setVariable("changeAmount", String.format(java.util.Locale.ROOT, "%.2f", changeAmount));
            template.setVariable("paymentMethod", paymentMethod);
            template.setVariable("memberInfo", memberInfo != null ? memberInfo : "非会员");
            
            // 打印条码开关（默认 true，与设置页复选框默认勾选一致）：条码以 ESC/POS 原始字节
            // 挂在打印任务上，由 NetworkPrinterDevice 在正文之后直写设备；关掉则什么都不带。
            // 文本/文件小票（ReceiptPrinter 的 .txt 路径）不打印条码。
            byte[] barcodeBytes = Boolean.parseBoolean(settings.getOrDefault("printBarcode", "true"))
                ? EscPosUtils.barcodeCode128(transactionId) : null;
            
            PrintTask task = PrintTask.createReceiptTask(template.generate(), printLogo, true, barcodeBytes);
            
            return PrinterManager.getInstance().print(task);
            
        } catch (Exception e) {
            logger.error("打印销售小票失败", e);
            return false;
        }
    }
    
    /**
     * 打印入库单据
     * @param inboundNo 入库单号
     * @param orderNo 采购订单号
     * @param inboundDate 入库日期
     * @param operator 操作员
     * @param items 商品列表
     * @param totalQuantity 总数量
     * @param totalAmount 总金额
     * @param remark 备注
     * @return 是否打印成功
     */
    public static boolean printInbound(String inboundNo, String orderNo, String inboundDate,
                                     String operator, String items, int totalQuantity,
                                     double totalAmount, String remark) {
        try {
            PrintTemplate template = PrintTemplate.createInboundTemplate();
            
            template.setVariable("inboundNo", inboundNo);
            template.setVariable("orderNo", orderNo);
            template.setVariable("inboundDate", inboundDate);
            template.setVariable("operator", operator);
            template.setVariable("items", items);
            template.setVariable("totalQuantity", String.valueOf(totalQuantity));
            template.setVariable("totalAmount", String.format(java.util.Locale.ROOT, "%.2f", totalAmount));
            template.setVariable("remark", remark != null ? remark : "");
            
            PrintTask task = PrintTask.createInboundTask(template.generate());
            
            return PrinterManager.getInstance().print(task);
            
        } catch (Exception e) {
            logger.error("打印入库单据失败", e);
            return false;
        }
    }
    
    /**
     * 打印会员收据
     * @param storeName 门店名称
     * @param cashierName 收银员姓名
     * @param memberName 会员姓名
     * @param memberPhone 会员手机号
     * @param memberLevel 会员等级
     * @param rechargeAmount 充值金额
     * @param bonusPoints 赠送积分
     * @param paymentMethod 支付方式
     * @param newBalance 新余额
     * @param newPoints 新积分
     * @return 是否打印成功
     */
    public static boolean printMemberReceipt(String storeName, String cashierName,
                                           String memberName, String memberPhone, String memberLevel,
                                           double rechargeAmount, int bonusPoints, String paymentMethod,
                                           double newBalance, double newPoints) {
        try {
            PrintTemplate template = PrintTemplate.createMemberReceiptTemplate();
            
            template.setVariable("storeName", storeName);
            template.setVariable("cashierName", cashierName);
            template.setVariable("rechargeTime", formatNow());
            template.setVariable("memberName", memberName);
            template.setVariable("memberPhone", memberPhone);
            template.setVariable("memberLevel", memberLevel);
            template.setVariable("rechargeAmount", String.format(java.util.Locale.ROOT, "%.2f", rechargeAmount));
            template.setVariable("bonusPoints", String.valueOf(bonusPoints));
            template.setVariable("paymentMethod", paymentMethod);
            template.setVariable("newBalance", String.format(java.util.Locale.ROOT, "%.2f", newBalance));
            template.setVariable("newPoints", String.valueOf((int)newPoints));
            
            PrintTask task = PrintTask.createMemberReceiptTask(template.generate());
            
            return PrinterManager.getInstance().print(task);
            
        } catch (Exception e) {
            logger.error("打印会员收据失败", e);
            return false;
        }
    }
    
    /**
     * 打印盘点报表
     * @param checkNo 盘点单号
     * @param checkDate 盘点日期
     * @param checkType 盘点类型
     * @param operator 操作员
     * @param items 商品列表
     * @param totalItems 总数量
     * @param diffItems 差异数量
     * @param remark 备注
     * @return 是否打印成功
     */
    public static boolean printInventoryReport(String checkNo, String checkDate, String checkType,
                                             String operator, String items, int totalItems,
                                             int diffItems, String remark) {
        try {
            PrintTemplate template = PrintTemplate.createInventoryReportTemplate();
            
            template.setVariable("checkNo", checkNo);
            template.setVariable("checkDate", checkDate);
            template.setVariable("checkType", checkType);
            template.setVariable("operator", operator);
            template.setVariable("items", items);
            template.setVariable("totalItems", String.valueOf(totalItems));
            template.setVariable("diffItems", String.valueOf(diffItems));
            template.setVariable("remark", remark != null ? remark : "");
            
            PrintTask task = PrintTask.createInventoryReportTask(template.generate());
            
            return PrinterManager.getInstance().print(task);
            
        } catch (Exception e) {
            logger.error("打印盘点报表失败", e);
            return false;
        }
    }
    
    /**
     * 打印销售统计报表
     * @param totalRevenue 总收入
     * @param totalQuantity 总数量
     * @param transactionCount 交易次数
     * @param avgTicket 平均客单价
     * @param details 详细信息
     * @param timeRange 时间范围
     * @return 是否打印成功
     */
    public static boolean printSalesReport(double totalRevenue, int totalQuantity,
                                          int transactionCount, double avgTicket,
                                          String details, String timeRange) {
        try {
            PrintTemplate template = PrintTemplate.createSalesReportTemplate();
            
            template.setVariable("reportTime", formatNow());
            template.setVariable("timeRange", timeRange);
            template.setVariable("totalRevenue", String.format(java.util.Locale.ROOT, "%.2f", totalRevenue));
            template.setVariable("totalQuantity", String.valueOf(totalQuantity));
            template.setVariable("transactionCount", String.valueOf(transactionCount));
            template.setVariable("avgTicket", String.format(java.util.Locale.ROOT, "%.2f", avgTicket));
            template.setVariable("details", details);
            
            PrintTask task = PrintTask.createSalesReportTask(template.generate());
            
            return PrinterManager.getInstance().print(task);
            
        } catch (Exception e) {
            logger.error("打印销售统计报表失败", e);
            return false;
        }
    }
    
    /**
     * 打开钱箱
     * @return 是否成功
     */
    public static boolean openCashDrawer() {
        return PrinterManager.getInstance().openCashDrawer();
    }

    /**
     * 小票上的门店地址/电话两行（模板变量 {@code {{storeInfo}}} 的值，也供
     * {@link com.cashier.util.ReceiptPrinter} 的文本与 ESC/POS 小票复用，保证两条打印路径格式一致）。
     *
     * <p>把**整行连换行**都放进返回值：设置里留空时返回空串，模板不会印出"地址: "这种没有内容的行，
     * 也不会多出一条空行；只有一项填了就只印那一行。</p>
     */
    public static String storeContactLines(String storeAddress, String storePhone) {
        StringBuilder lines = new StringBuilder();
        if (storeAddress != null && !storeAddress.trim().isEmpty()) {
            lines.append("地址: ").append(storeAddress.trim()).append("\n");
        }
        if (storePhone != null && !storePhone.trim().isEmpty()) {
            lines.append("电话: ").append(storePhone.trim()).append("\n");
        }
        return lines.toString();
    }

    private static String formatNow() {
        return java.time.LocalDateTime.now(java.time.ZoneId.systemDefault())
            .format(DateTimeFormats.STANDARD_DATE_TIME);
    }
}
