package com.cashier.printer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * 打印工具类
 * 提供便捷的打印方法
 */
public class PrintUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(PrintUtil.class);
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
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
        try {
            PrintTemplate template = PrintTemplate.createReceiptTemplate();
            
            template.setVariable("storeName", storeName);
            template.setVariable("cashierName", cashierName);
            template.setVariable("transactionId", transactionId);
            template.setVariable("transactionTime", DATE_FORMAT.format(new Date()));
            template.setVariable("items", items);
            template.setVariable("totalQuantity", String.valueOf(totalQuantity));
            template.setVariable("totalAmount", String.format("%.2f", totalAmount));
            template.setVariable("discountAmount", String.format("%.2f", discountAmount));
            template.setVariable("finalAmount", String.format("%.2f", finalAmount));
            template.setVariable("paidAmount", String.format("%.2f", paidAmount));
            template.setVariable("changeAmount", String.format("%.2f", changeAmount));
            template.setVariable("paymentMethod", paymentMethod);
            template.setVariable("memberInfo", memberInfo != null ? memberInfo : "非会员");
            
            PrintTask task = PrintTask.createReceiptTask(template.generate(), true, true);
            
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
            template.setVariable("totalAmount", String.format("%.2f", totalAmount));
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
            template.setVariable("rechargeTime", DATE_FORMAT.format(new Date()));
            template.setVariable("memberName", memberName);
            template.setVariable("memberPhone", memberPhone);
            template.setVariable("memberLevel", memberLevel);
            template.setVariable("rechargeAmount", String.format("%.2f", rechargeAmount));
            template.setVariable("bonusPoints", String.valueOf(bonusPoints));
            template.setVariable("paymentMethod", paymentMethod);
            template.setVariable("newBalance", String.format("%.2f", newBalance));
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
            
            template.setVariable("reportTime", DATE_FORMAT.format(new Date()));
            template.setVariable("timeRange", timeRange);
            template.setVariable("totalRevenue", String.format("%.2f", totalRevenue));
            template.setVariable("totalQuantity", String.valueOf(totalQuantity));
            template.setVariable("transactionCount", String.valueOf(transactionCount));
            template.setVariable("avgTicket", String.format("%.2f", avgTicket));
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
}