package com.cashier.util;

import com.cashier.model.CartItem;
import com.cashier.model.Member;
import com.cashier.model.Transaction;
import com.cashier.model.ReturnOrder;
import com.cashier.model.ReturnOrderItem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;

/**
 * 小票打印工具类
 * 支持打印收银小票
 */
public class ReceiptPrinter {

    private static final Logger logger = LoggerFactoryUtil.getLogger(ReceiptPrinter.class);

    // 使用线程安全的 DateTimeFormatter 替代 SimpleDateFormat
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String RECEIPT_DIR = "receipts";

    /**
     * 生成并打印小票
     * @param transaction 交易信息
     * @param cartItems 购物车商品列表
     * @param member 会员信息（可选）
     * @return 小票文件路径，如果打印失败则返回 null
     */
    public static String printReceipt(Transaction transaction, List<CartItem> cartItems, Member member) {
        try {
            // 创建收据目录
            File dir = new File(RECEIPT_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成小票文件名
            String fileName = String.format("receipt_%s_%s.txt",
                transaction.transactionId,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

            File receiptFile = new File(dir, fileName);

            // 生成小票内容
            String content = generateReceiptContent(transaction, cartItems, member);

            // 写入文件
            try (FileWriter writer = new FileWriter(receiptFile)) {
                writer.write(content);
            }

            // 打印小票（使用系统默认打印机）
            printFile(receiptFile);

            return receiptFile.getAbsolutePath();

        } catch (Exception e) {
            System.err.println("打印小票失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 生成小票内容
     */
    private static String generateReceiptContent(Transaction transaction, List<CartItem> cartItems, Member member) {
        StringBuilder sb = new StringBuilder();

        // 店铺信息
        sb.append("========================================\n");
        sb.append("           收银系统小票\n");
        sb.append("========================================\n\n");

        // 交易信息
        sb.append("订单号: ").append(transaction.transactionId).append("\n");
        try {
            // 解析时间戳字符串并重新格式化
            java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(
                transaction.timestamp, 
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            );
            sb.append("时间: ").append(DATE_FORMATTER.format(dateTime)).append("\n");
        } catch (Exception e) {
            sb.append("时间: ").append(transaction.timestamp).append("\n");
        }
        sb.append("收银员: 系统\n");

        // 会员信息
        if (member != null) {
            sb.append("----------------------------------------\n");
            sb.append("会员信息:\n");
            sb.append("  会员姓名: ").append(member.name).append("\n");
            sb.append("  手机号: ").append(member.phone).append("\n");
            sb.append("  积分: ").append(member.points).append("\n");
            sb.append("----------------------------------------\n");
        }

        sb.append("\n");

        // 商品列表
        sb.append("商品列表:\n");
        sb.append("----------------------------------------\n");
        sb.append(String.format("%-20s %5s %8s %10s\n", "商品名称", "数量", "单价", "金额"));
        sb.append("----------------------------------------\n");

        for (CartItem item : cartItems) {
            String name = item.product.name;
            if (name.length() > 18) {
                name = name.substring(0, 17) + "~";
            }
            sb.append(String.format("%-20s %5d %8.2f %10.2f\n",
                name,
                item.quantity,
                item.product.price,
                item.subtotal));
        }

        sb.append("----------------------------------------\n");

        // 金额汇总
        sb.append(String.format("%35s %10.2f\n", "商品总额:", transaction.totalAmount));
        if (transaction.tax > 0) {
            sb.append(String.format("%35s %10.2f\n", "税费:", transaction.tax));
        }
        sb.append("----------------------------------------\n");
        sb.append(String.format("%35s %10.2f\n", "实付金额:", transaction.finalAmount));
        sb.append("========================================\n");

        // 支付方式
        String paymentMethod = getPaymentMethodDisplayName(transaction.paymentMethod);
        sb.append(String.format("支付方式: %s\n", paymentMethod));

        // 会员折扣（如果有）
        if (member != null && member.discount < 10.0) {
            sb.append(String.format("会员折扣: %.1f折\n", member.discount));
        }

        sb.append("\n");

        // 底部信息
        sb.append("谢谢惠顾！欢迎再次光临！\n");
        sb.append("========================================\n");
        sb.append("       退换货凭据\n");
        sb.append("========================================\n");

        return sb.toString();
    }

    /**
     * 获取支付方式显示名称
     */
    private static String getPaymentMethodDisplayName(String method) {
        if (method == null) return "未知";

        switch (method) {
            case "cash": return "现金";
            case "wechat": return "微信支付";
            case "alipay": return "支付宝";
            case "card": return "银行卡";
            default: return method;
        }
    }

    /**
     * 打印文件（使用系统默认打印机和命令）
     */
    private static void printFile(File file) {
        try {
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                // Windows: 使用 notepad /p 打印
                Runtime.getRuntime().exec(new String[]{"notepad", "/p", file.getAbsolutePath()});
            } else if (os.contains("mac")) {
                // macOS: 使用 lpr 打印
                Runtime.getRuntime().exec(new String[]{"lpr", file.getAbsolutePath()});
            } else if (os.contains("nix") || os.contains("nux")) {
                // Linux: 使用 lpr 打印
                Runtime.getRuntime().exec(new String[]{"lpr", file.getAbsolutePath()});
            } else {
                // 未知系统，显示提示
                logger.info("小票已生成: {}", file.getAbsolutePath());
                logger.info("请使用系统打印机打开文件并打印。");
            }
        } catch (IOException e) {
            logger.error("打印命令执行失败: {}", e.getMessage(), e);
            logger.info("小票文件: {}", file.getAbsolutePath());
            logger.info("请手动打开文件并打印。");
        }
    }

    /**
     * 仅生成小票文件（不打印）
     * @param transaction 交易信息
     * @param cartItems 购物车商品列表
     * @param member 会员信息（可选）
     * @return 小票文件路径，如果失败则返回 null
     */
    public static String generateReceiptOnly(Transaction transaction, List<CartItem> cartItems, Member member) {
        try {
            // 创建收据目录
            File dir = new File(RECEIPT_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成小票文件名
            String fileName = String.format("receipt_%s_%s.txt",
                transaction.transactionId,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

            File receiptFile = new File(dir, fileName);

            // 生成小票内容
            String content = generateReceiptContent(transaction, cartItems, member);

            // 写入文件
            try (FileWriter writer = new FileWriter(receiptFile)) {
                writer.write(content);
            }

            return receiptFile.getAbsolutePath();

        } catch (Exception e) {
            System.err.println("生成小票失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 打开小票文件（使用系统默认程序）
     * @param filePath 文件路径
     */
    public static void openReceiptFile(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("文件不存在: " + filePath);
                return;
            }

            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("mac")) {
                // macOS: 使用 open 命令
                Runtime.getRuntime().exec(new String[]{"open", filePath});
            } else if (os.contains("win")) {
                // Windows: 使用 start 命令
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "", filePath});
            } else if (os.contains("nix") || os.contains("nux")) {
                // Linux: 使用 xdg-open
                Runtime.getRuntime().exec(new String[]{"xdg-open", filePath});
            }

        } catch (IOException e) {
            System.err.println("打开文件失败: " + e.getMessage());
        }
    }

    /**
     * 生成并打印退货单据
     * @param returnOrder 退货订单
     * @param returnItems 退货商品列表
     * @return 退货单据文件路径，如果打印失败则返回 null
     */
    public static String printReturnReceipt(ReturnOrder returnOrder, List<ReturnOrderItem> returnItems) {
        try {
            // 创建收据目录
            File dir = new File(RECEIPT_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成退货单据文件名
            String fileName = String.format("return_%s_%s.txt",
                returnOrder.returnOrderId,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

            File returnReceiptFile = new File(dir, fileName);

            // 生成退货单据内容
            String content = generateReturnReceiptContent(returnOrder, returnItems);

            // 写入文件
            try (FileWriter writer = new FileWriter(returnReceiptFile)) {
                writer.write(content);
            }

            // 打印退货单据（使用系统默认打印机）
            printFile(returnReceiptFile);

            return returnReceiptFile.getAbsolutePath();

        } catch (Exception e) {
            System.err.println("打印退货单据失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 生成退货单据内容
     */
    private static String generateReturnReceiptContent(ReturnOrder returnOrder, List<ReturnOrderItem> returnItems) {
        StringBuilder sb = new StringBuilder();

        // 店铺信息
        sb.append("========================================\n");
        sb.append("           退货单据\n");
        sb.append("========================================\n\n");

        // 退货单信息
        sb.append("退货单号: ").append(returnOrder.returnOrderId).append("\n");
        sb.append("原订单号: ").append(returnOrder.originalTransactionId != null ? returnOrder.originalTransactionId : "无").append("\n");
        sb.append("退货日期: ").append(returnOrder.getReturnDateFormatted()).append("\n");
        sb.append("操作员: ").append(returnOrder.operatorName).append("\n");

        // 审批信息
        if ("APPROVED".equals(returnOrder.status) || "COMPLETED".equals(returnOrder.status)) {
            sb.append("----------------------------------------\n");
            sb.append("审批信息:\n");
            sb.append("  审批人: ").append(returnOrder.approverName != null ? returnOrder.approverName : "无").append("\n");
            if (returnOrder.approvalDate != null) {
                sb.append("  审批日期: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(returnOrder.approvalDate)).append("\n");
            }
            if (returnOrder.approvalComment != null && !returnOrder.approvalComment.isEmpty()) {
                sb.append("  审批意见: ").append(returnOrder.approvalComment).append("\n");
            }
            sb.append("----------------------------------------\n");
        }

        // 会员信息
        if (returnOrder.memberName != null) {
            sb.append("----------------------------------------\n");
            sb.append("会员信息:\n");
            sb.append("  会员姓名: ").append(returnOrder.memberName).append("\n");
            sb.append("----------------------------------------\n");
        }

        sb.append("\n");

        // 退货商品列表
        sb.append("退货商品列表:\n");
        sb.append("----------------------------------------\n");
        sb.append(String.format("%-20s %8s %8s %10s %10s\n", "商品名称", "退货数量", "单价", "退货金额", "商品状态"));
        sb.append("----------------------------------------\n");

        for (ReturnOrderItem item : returnItems) {
            String name = item.productName;
            if (name.length() > 18) {
                name = name.substring(0, 17) + "~";
            }
            sb.append(String.format("%-20s %8d %8.2f %10.2f %10s\n",
                name,
                item.returnQuantity,
                item.unitPrice,
                item.returnAmount,
                item.condition != null ? item.condition : "正常"));
        }

        sb.append("----------------------------------------\n");

        // 金额汇总
        sb.append(String.format("%35s %10.2f\n", "退货总额:", returnOrder.totalAmount));
        sb.append("========================================\n");

        // 退款方式
        String paymentMethod = returnOrder.getPaymentMethodText();
        sb.append(String.format("退款方式: %s\n", paymentMethod));

        // 退货原因
        if (returnOrder.returnReason != null && !returnOrder.returnReason.isEmpty()) {
            sb.append(String.format("退货原因: %s\n", returnOrder.returnReason));
        }

        // 状态
        sb.append(String.format("订单状态: %s\n", returnOrder.getStatusText()));

        sb.append("\n");

        // 备注
        if (returnOrder.notes != null && !returnOrder.notes.isEmpty()) {
            sb.append("备注: ").append(returnOrder.notes).append("\n\n");
        }

        // 底部信息
        sb.append("========================================\n");
        sb.append("      退货单据\n");
        sb.append("========================================\n");

        return sb.toString();
    }

    /**
     * 生成退货单据内容（不打印，只生成文件）
     * @param returnOrder 退货订单
     * @param returnItems 退货商品列表
     * @return 退货单据文件路径，如果生成失败则返回 null
     */
    public static String generateReturnReceiptOnly(ReturnOrder returnOrder, List<ReturnOrderItem> returnItems) {
        try {
            // 创建收据目录
            File dir = new File(RECEIPT_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成退货单据文件名
            String fileName = String.format("return_%s_%s.txt",
                returnOrder.returnOrderId,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

            File returnReceiptFile = new File(dir, fileName);

            // 生成退货单据内容
            String content = generateReturnReceiptContent(returnOrder, returnItems);

            // 写入文件
            try (FileWriter writer = new FileWriter(returnReceiptFile)) {
                writer.write(content);
            }

            return returnReceiptFile.getAbsolutePath();

        } catch (Exception e) {
            System.err.println("生成退货单据失败: " + e.getMessage());
            return null;
        }
    }
}
