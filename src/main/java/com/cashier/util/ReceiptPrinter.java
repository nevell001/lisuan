package com.cashier.util;

import com.cashier.constant.SystemPropertyKeys;

import com.cashier.model.CartItem;
import com.cashier.model.Member;
import com.cashier.model.Transaction;
import com.cashier.model.ReturnOrder;
import com.cashier.model.ReturnOrderItem;
import com.cashier.printer.PrinterManager;
import com.cashier.printer.PrintTask;

import java.io.File;
import java.math.BigDecimal;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.slf4j.Logger;

/**
 * 小票打印工具类
 * 支持打印收银小票
 */
public class ReceiptPrinter {

    private static final Logger logger = LoggerFactoryUtil.getLogger(ReceiptPrinter.class);

    // 使用线程安全的 DateTimeFormatter 替代 SimpleDateFormat
    private static final DateTimeFormatter DATE_FORMATTER = com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME;
    private static final String RECEIPT_DIR = "receipts";
    private static final String THICK_SEPARATOR = "========================================\n";
    private static final String THIN_SEPARATOR = "----------------------------------------\n";

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
                java.time.LocalDateTime.now().format(com.cashier.util.DateTimeFormats.COMPACT_DATE_TIME));

            File receiptFile = new File(dir, fileName);

            // 生成小票内容
            String content = generateReceiptContent(transaction, cartItems, member);

            // 写入文件
            try (java.io.BufferedWriter writer = Files.newBufferedWriter(receiptFile.toPath(), StandardCharsets.UTF_8)) {
                writer.write(content);
            }

            // 打印小票（使用系统默认打印机）
            printFile(receiptFile);

            return receiptFile.getAbsolutePath();

        } catch (Exception e) {
            logger.error("打印小票失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 生成小票内容
     */
    /** 生成小票正文（包内可见，便于无界面单测；只拼字符串，不打印、不落盘）。 */
    static String generateReceiptContent(Transaction transaction, List<CartItem> cartItems, Member member) {
        StringBuilder sb = new StringBuilder();

        // 店铺信息（带 Logo）
        sb.append(THICK_SEPARATOR);
        sb.append("              ╔═══╗                    \n");
        sb.append("              ║狸算║                    \n");
        sb.append("              ║收银║                    \n");
        sb.append("              ╚═══╝                    \n");
        sb.append("        狸算(LiSuan)收银系统小票\n");
        sb.append(THICK_SEPARATOR).append("\n");

        // 交易信息
        sb.append("订单号: ").append(transaction.transactionId).append("\n");
        try {
            // 解析时间戳字符串并重新格式化
            java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(
                transaction.timestamp, 
                com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME
            );
            sb.append("时间: ").append(DATE_FORMATTER.format(dateTime)).append("\n");
        } catch (Exception e) {
            sb.append("时间: ").append(transaction.timestamp).append("\n");
        }
        sb.append("收银员: 系统\n");

        // 会员信息
        if (member != null) {
            sb.append(THIN_SEPARATOR);
            sb.append("会员信息:\n");
            sb.append("  会员姓名: ").append(member.name).append("\n");
            sb.append("  手机号: ").append(member.phone).append("\n");
            sb.append("  积分: ").append(member.points).append("\n");
            sb.append(THIN_SEPARATOR);
        }

        sb.append("\n");

        // 商品列表
        sb.append("商品列表:\n");
        sb.append(THIN_SEPARATOR);
        sb.append(String.format("%-20s %5s %8s %10s\n", "商品名称", "数量", "单价", "金额"));
        sb.append(THIN_SEPARATOR);

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

        sb.append(THIN_SEPARATOR);

        // 金额汇总
        sb.append(String.format("%35s %10.2f\n", "商品总额:", transaction.totalAmount));
        BigDecimal discount = receiptDiscount(transaction);
        if (discount != null) {
            sb.append(String.format("%35s %10.2f\n", "优惠:", discount));
        }
        if (transaction.tax.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("%35s %10.2f\n", "税费:", transaction.tax));
        }
        sb.append(THIN_SEPARATOR);
        sb.append(String.format("%35s %10.2f\n", "实付金额:", transaction.finalAmount));
        sb.append(THICK_SEPARATOR);

        // 支付方式
        String paymentMethod = getPaymentMethodDisplayName(transaction.paymentMethod);
        sb.append(String.format("支付方式: %s\n", paymentMethod));

        // 会员折扣（如果有）
        if (member != null && member.getDiscount().compareTo(BigDecimal.TEN) < 0) {
            sb.append(String.format("会员折扣: %.1f折\n", member.getDiscount()));
        }

        sb.append("\n");

        // 底部信息
        sb.append("谢谢惠顾！欢迎再次光临！\n");
        sb.append(THICK_SEPARATOR);
        sb.append("       退换货凭据\n");
        sb.append(THICK_SEPARATOR);

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
     * H-27: 使用 ProcessBuilder 消费进程输出，设超时，避免挂起
     */
    private static void printFile(File file) {
        try {
            String os = System.getProperty(SystemPropertyKeys.OS_NAME).toLowerCase();

            String[] command;
            if (os.contains("win")) {
                // Windows: 使用 notepad /p 打印
                command = new String[]{"notepad", "/p", file.getAbsolutePath()};
            } else if (os.contains("mac")) {
                // macOS: 使用 lpr 打印
                command = new String[]{"lpr", file.getAbsolutePath()};
            } else if (os.contains("nix") || os.contains("nux")) {
                // Linux: 使用 lpr 打印
                command = new String[]{"lpr", file.getAbsolutePath()};
            } else {
                // 未知系统，显示提示
                logger.info("小票已生成: {}", file.getAbsolutePath());
                logger.info("请使用系统打印机打开文件并打印。");
                return;
            }

            executeCommandWithTimeout(command, 10);
        } catch (IOException e) {
            logger.error("打印命令执行失败: {}", e.getMessage(), e);
            logger.info("小票文件: {}", file.getAbsolutePath());
            logger.info("请手动打开文件并打印。");
        }
    }

    /**
     * 小票上的优惠额（商品总额 - 实付），无优惠或金额缺失时返回 null（不打印该行）。
     *
     * <p>返回负数，使小票读起来是"商品总额 - 优惠 = 实付金额"。</p>
     */
    private static BigDecimal receiptDiscount(Transaction transaction) {
        if (transaction.totalAmount == null || transaction.finalAmount == null) {
            return null;
        }
        BigDecimal discount = transaction.totalAmount.subtract(transaction.finalAmount);
        return discount.compareTo(BigDecimal.ZERO) > 0 ? discount.negate() : null;
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
                java.time.LocalDateTime.now().format(com.cashier.util.DateTimeFormats.COMPACT_DATE_TIME));

            File receiptFile = new File(dir, fileName);

            // 生成小票内容
            String content = generateReceiptContent(transaction, cartItems, member);

            // 写入文件
            try (java.io.BufferedWriter writer = Files.newBufferedWriter(receiptFile.toPath(), StandardCharsets.UTF_8)) {
                writer.write(content);
            }

            return receiptFile.getAbsolutePath();

        } catch (Exception e) {
            logger.error("生成小票失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 打开小票文件（使用系统默认程序）
     * H-27: 使用 ProcessBuilder 消费进程输出，设超时，避免挂起
     * @param filePath 文件路径
     */
    public static void openReceiptFile(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                logger.warn("文件不存在: {}", filePath);
                return;
            }

            String os = System.getProperty(SystemPropertyKeys.OS_NAME).toLowerCase();

            String[] command;
            if (os.contains("mac")) {
                // macOS: 使用 open 命令
                command = new String[]{"open", filePath};
            } else if (os.contains("win")) {
                // Windows: 使用 start 命令
                command = new String[]{"cmd", "/c", "start", "", filePath};
            } else if (os.contains("nix") || os.contains("nux")) {
                // Linux: 使用 xdg-open
                command = new String[]{"xdg-open", filePath};
            } else {
                logger.warn("不支持的操作系统，无法自动打开文件: {}", filePath);
                return;
            }

            executeCommandWithTimeout(command, 10);
        } catch (IOException e) {
            logger.error("打开文件失败: {}", e.getMessage(), e);
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
                java.time.LocalDateTime.now().format(com.cashier.util.DateTimeFormats.COMPACT_DATE_TIME));

            File returnReceiptFile = new File(dir, fileName);

            // 生成退货单据内容
            String content = generateReturnReceiptContent(returnOrder, returnItems);

            // 写入文件
            try (java.io.BufferedWriter writer = Files.newBufferedWriter(returnReceiptFile.toPath(), StandardCharsets.UTF_8)) {
                writer.write(content);
            }

            // 打印退货单据（使用系统默认打印机）
            printFile(returnReceiptFile);

            return returnReceiptFile.getAbsolutePath();

        } catch (Exception e) {
            logger.error("打印退货单据失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 生成退货单据内容
     */
    private static String generateReturnReceiptContent(ReturnOrder returnOrder, List<ReturnOrderItem> returnItems) {
        StringBuilder sb = new StringBuilder();

        appendReturnReceiptHeader(sb, returnOrder);
        appendReturnApprovalInfo(sb, returnOrder);
        appendReturnMemberInfo(sb, returnOrder);
        appendReturnItems(sb, returnItems);
        appendReturnSummary(sb, returnOrder);
        appendReturnReceiptFooter(sb);

        return sb.toString();
    }

    private static void appendReturnReceiptHeader(StringBuilder sb, ReturnOrder returnOrder) {
        sb.append(THICK_SEPARATOR);
        sb.append("           退货单据\n");
        sb.append(THICK_SEPARATOR).append("\n");
        sb.append("退货单号: ").append(returnOrder.returnOrderId).append("\n");
        sb.append("原订单号: ").append(returnOrder.originalTransactionId != null ? returnOrder.originalTransactionId : "无").append("\n");
        sb.append("退货日期: ").append(returnOrder.getReturnDateFormatted()).append("\n");
        sb.append("操作员: ").append(returnOrder.operatorName).append("\n");
    }

    private static void appendReturnApprovalInfo(StringBuilder sb, ReturnOrder returnOrder) {
        if (!"APPROVED".equals(returnOrder.status) && !"COMPLETED".equals(returnOrder.status)) {
            return;
        }
        sb.append(THIN_SEPARATOR);
        sb.append("审批信息:\n");
        sb.append("  审批人: ").append(returnOrder.approverName != null ? returnOrder.approverName : "无").append("\n");
        if (returnOrder.approvalDate != null) {
            sb.append("  审批日期: ").append(returnOrder.approvalDate.atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime().format(DateTimeFormats.STANDARD_DATE_TIME)).append("\n");
        }
        if (returnOrder.approvalComment != null && !returnOrder.approvalComment.isEmpty()) {
            sb.append("  审批意见: ").append(returnOrder.approvalComment).append("\n");
        }
        sb.append(THIN_SEPARATOR);
    }

    private static void appendReturnMemberInfo(StringBuilder sb, ReturnOrder returnOrder) {
        if (returnOrder.memberName == null) {
            return;
        }
        sb.append(THIN_SEPARATOR);
        sb.append("会员信息:\n");
        sb.append("  会员姓名: ").append(returnOrder.memberName).append("\n");
        sb.append(THIN_SEPARATOR);
    }

    private static void appendReturnItems(StringBuilder sb, List<ReturnOrderItem> returnItems) {
        sb.append("\n");
        sb.append("退货商品列表:\n");
        sb.append(THIN_SEPARATOR);
        sb.append(String.format("%-20s %8s %8s %10s %10s\n", "商品名称", "退货数量", "单价", "退货金额", "商品状态"));
        sb.append(THIN_SEPARATOR);

        for (ReturnOrderItem item : returnItems) {
            sb.append(String.format("%-20s %8d %8.2f %10.2f %10s\n",
                abbreviateReturnItemName(item.productName),
                item.returnQuantity,
                item.unitPrice,
                item.returnAmount,
                item.condition != null ? item.condition : "正常"));
        }
        sb.append(THIN_SEPARATOR);
    }

    private static String abbreviateReturnItemName(String name) {
        return name.length() > 18 ? name.substring(0, 17) + "~" : name;
    }

    private static void appendReturnSummary(StringBuilder sb, ReturnOrder returnOrder) {
        sb.append(String.format("%35s %10.2f\n", "退货总额:", returnOrder.totalAmount));
        sb.append(THICK_SEPARATOR);
        sb.append(String.format("退款方式: %s\n", returnOrder.getPaymentMethodText()));
        if (returnOrder.returnReason != null && !returnOrder.returnReason.isEmpty()) {
            sb.append(String.format("退货原因: %s\n", returnOrder.returnReason));
        }
        sb.append(String.format("订单状态: %s\n", returnOrder.getStatusText()));
        sb.append("\n");
        if (returnOrder.notes != null && !returnOrder.notes.isEmpty()) {
            sb.append("备注: ").append(returnOrder.notes).append("\n\n");
        }
    }

    private static void appendReturnReceiptFooter(StringBuilder sb) {
        sb.append(THICK_SEPARATOR);
        sb.append("      退货单据\n");
        sb.append(THICK_SEPARATOR);
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
                java.time.LocalDateTime.now().format(com.cashier.util.DateTimeFormats.COMPACT_DATE_TIME));

            File returnReceiptFile = new File(dir, fileName);

            // 生成退货单据内容
            String content = generateReturnReceiptContent(returnOrder, returnItems);

            // 写入文件
            try (java.io.BufferedWriter writer = Files.newBufferedWriter(returnReceiptFile.toPath(), StandardCharsets.UTF_8)) {
                writer.write(content);
            }

            return returnReceiptFile.getAbsolutePath();

        } catch (Exception e) {
            logger.error("生成退货单据失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 使用网络打印机打印小票（支持位图 Logo）
     * @param transaction 交易信息
     * @param cartItems 购物车商品列表
     * @param member 会员信息（可选）
     * @param printerId 打印机ID（可选，null 使用默认）
     * @return 是否打印成功
     */
    public static boolean printReceiptWithPrinter(Transaction transaction, List<CartItem> cartItems, Member member, String printerId) {
        PrinterManager printerManager = PrinterManager.getInstance();

        try {
            // 构建小票内容（ESC/POS 格式）
            StringBuilder content = new StringBuilder();

            // 添加 Logo（通过 PrintTask 参数控制）
            // 打印机会自动调用 printLogo()

            // 店铺名称
            content.append(new String(EscPosUtils.ALIGN_CENTER, StandardCharsets.ISO_8859_1));
            content.append(new String(EscPosUtils.DOUBLE_HEIGHT_WIDTH_ON, StandardCharsets.ISO_8859_1));
            content.append("狸算(LiSuan)收银系统\n");
            content.append(new String(EscPosUtils.FONT_NORMAL, StandardCharsets.ISO_8859_1));
            content.append(new String(EscPosUtils.ALIGN_LEFT, StandardCharsets.ISO_8859_1));
            content.append(new String(EscPosUtils.LINE_FEED, StandardCharsets.ISO_8859_1));

            // 分隔线
            content.append(THICK_SEPARATOR);

            // 交易信息
            content.append(String.format("订单号: %s\n", transaction.transactionId));
            content.append(String.format("时间: %s\n", transaction.timestamp));
            content.append("收银员: 系统\n");

            // 会员信息
            if (member != null) {
                content.append(THIN_SEPARATOR);
                content.append(String.format("会员: %s\n", member.name));
                content.append(String.format("手机: %s\n", member.phone));
                content.append(String.format("积分: %d\n", member.points));
                content.append(THIN_SEPARATOR);
            }

            content.append("\n商品列表:\n");
            String sym = CurrencyUtil.getSymbol();
            content.append(THIN_SEPARATOR);

            // 商品列表
            for (CartItem item : cartItems) {
                String name = item.product.name;
                if (name.length() > 16) {
                    name = name.substring(0, 15) + "~";
                }
                content.append(String.format("%-16s x%d  " + sym + "%.2f\n", name, item.quantity, item.product.price));
                content.append(String.format("                  小计: " + sym + "%.2f\n", item.subtotal));
            }

            content.append(THIN_SEPARATOR);
            content.append(String.format("商品总额: " + sym + "%.2f\n", transaction.totalAmount));
            BigDecimal discount = receiptDiscount(transaction);
            if (discount != null) {
                content.append(String.format("优惠: " + sym + "%.2f\n", discount));
            }
            if (transaction.tax.compareTo(BigDecimal.ZERO) > 0) {
                content.append(String.format("税费: " + sym + "%.2f\n", transaction.tax));
            }
            content.append(THIN_SEPARATOR);
            content.append(String.format("实付金额: " + sym + "%.2f\n", transaction.finalAmount));
            content.append(THICK_SEPARATOR);

            // 支付方式
            String paymentMethod = getPaymentMethodDisplayName(transaction.paymentMethod);
            content.append(String.format("支付方式: %s\n", paymentMethod));

            // 会员折扣
            if (member != null && member.getDiscount().compareTo(BigDecimal.TEN) < 0) {
                content.append(String.format("会员折扣: %.1f折\n", member.getDiscount()));
            }

            content.append("\n");
            content.append("谢谢惠顾！欢迎再次光临！\n");
            content.append(THICK_SEPARATOR);

            // 创建打印任务
            PrintTask task = new PrintTask(
                "receipt_" + transaction.transactionId,
                "销售小票",
                com.cashier.printer.PrintTaskType.RECEIPT,
                content.toString(),
                1,    // 份数
                true, // 打印 Logo
                false, // 不打开钱箱
                true,  // 切纸
                false  // 不需要预览
            );

            // 执行打印
            if (printerId != null && !printerId.isEmpty()) {
                com.cashier.printer.PrinterDevice printer = printerManager.getDevice(printerId);
                if (printer != null) {
                    return printer.print(task);
                }
            }

            // 使用默认打印机
            return printerManager.print(task);

        } catch (Exception e) {
            logger.error("网络打印机打印小票失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * ESC/POS 指令快捷访问
     */
    private static class EscPosUtils {
        public static final byte[] ALIGN_CENTER = {0x1B, 0x61, 0x01};
        public static final byte[] ALIGN_LEFT = {0x1B, 0x61, 0x00};
        public static final byte[] DOUBLE_HEIGHT_WIDTH_ON = {0x1B, 0x21, 0x30};
        public static final byte[] FONT_NORMAL = {0x1B, 0x21, 0x00};
        public static final byte[] LINE_FEED = {0x0A};
    }

    /**
     * H-27: 安全执行外部命令
     * 合并 stdout/stderr，消费输出流，设超时，防止进程挂起
     *
     * @param command 命令及参数
     * @param timeoutSeconds 超时秒数
     * @throws IOException 如果启动进程失败
     */
    private static void executeCommandWithTimeout(String[] command, int timeoutSeconds) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // 消费合并后的输出流，防止缓冲区满导致进程挂起
        try (java.io.InputStream is = process.getInputStream()) {
            byte[] buffer = new byte[4096];
            while (is.read(buffer) != -1) {
                // 丢弃输出，仅消费以防止阻塞
            }
        } catch (IOException e) {
            // 进程可能已结束，流关闭是正常的
            logger.debug("进程输出流已关闭", e);
        }

        try {
            boolean finished = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                logger.warn("命令执行超时 ({}s)，强制销毁进程: {}", timeoutSeconds, String.join(" ", command));
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            logger.warn("命令执行被中断，进程已销毁");
        }
    }
}
