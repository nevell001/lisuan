package com.cashier.service;

import com.cashier.model.Invoice;
import com.cashier.model.InvoiceItem;
import org.slf4j.Logger;
import com.cashier.util.DateTimeFormats;
import com.cashier.util.LoggerFactoryUtil;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 发票打印服务
 * 生成发票 HTML/PDF 文件
 */
public class InvoicePrintService {
    private static final Logger logger = LoggerFactoryUtil.getLogger(InvoicePrintService.class);
    private static final String INFO_ROW_START = "<div class=\"info-row\">\n";
    private static final String DIV_END = "</div>\n";
    
    // 输出目录
    private static volatile String outputDir = "invoices";
    
    /**
     * 初始化输出目录
     */
    public static void init() {
        Path path = Paths.get(outputDir);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                logger.info("发票输出目录创建成功: {}", outputDir);
            } catch (IOException e) {
                logger.error("创建发票输出目录失败", e);
            }
        }
    }
    
    /**
     * 生成发票 HTML 文件
     */
    public static String generateHtml(Invoice invoice) throws IOException {
        init();
        
        String fileName = invoice.invoiceId + ".html";
        String filePath = outputDir + "/" + fileName;
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-CN\">\n");
        html.append("<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>电子发票 - ").append(esc(invoice.invoiceId)).append("</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: 'Noto Sans CJK SC', 'PingFang SC', 'Microsoft YaHei UI', 'Microsoft YaHei', sans-serif; margin: 20px; }\n");
        html.append(".invoice-container { max-width: 800px; margin: auto; border: 2px solid #000; padding: 20px; }\n");
        html.append(".header { text-align: center; font-size: 24px; font-weight: bold; margin-bottom: 20px; }\n");
        html.append(".info-row { margin: 10px 0; }\n");
        html.append(".info-row span { display: inline-block; width: 200px; }\n");
        html.append(".items-table { width: 100%; border-collapse: collapse; margin: 20px 0; }\n");
        html.append(".items-table th, .items-table td { border: 1px solid #000; padding: 8px; }\n");
        html.append(".items-table th { background: #f0f0f0; }\n");
        html.append(".amount-row { text-align: right; margin: 10px 0; font-size: 18px; }\n");
        html.append(".footer { text-align: center; margin-top: 30px; }\n");
        html.append(".stamp { border: 2px solid red; color: red; padding: 10px; display: inline-block; }\n");
        html.append("</style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<div class=\"invoice-container\">\n");
        
        // 标题
        html.append("<div class=\"header\">电子发票</div>\n");
        
        // 发票基本信息
        html.append(INFO_ROW_START);
        html.append("<span>发票代码:</span>").append(esc(invoice.invoiceCode)).append("\n");
        html.append("<span>发票号码:</span>").append(esc(invoice.invoiceNumber)).append("\n");
        html.append(DIV_END);
        
        html.append(INFO_ROW_START);
        html.append("<span>开票日期:</span>").append(formatDate(invoice.createTime)).append("\n");
        html.append("<span>校验码:</span>").append(generateCheckCode()).append("\n");
        html.append(DIV_END);
        
        // 购买方信息
        html.append("<div class=\"section-title\" style=\"font-weight: bold; margin: 15px 0;\">购买方信息</div>\n");
        html.append(INFO_ROW_START);
        html.append("<span>名称:</span>").append(esc(invoice.buyerName)).append("\n");
        html.append(DIV_END);
        if (invoice.buyerTaxId != null && !invoice.buyerTaxId.isEmpty()) {
            html.append(INFO_ROW_START);
            html.append("<span>纳税人识别号:</span>").append(esc(invoice.buyerTaxId)).append("\n");
            html.append(DIV_END);
        }
        if (invoice.buyerAddress != null && !invoice.buyerAddress.isEmpty()) {
            html.append(INFO_ROW_START);
            html.append("<span>地址电话:</span>").append(esc(invoice.buyerAddress)).append(" ").append(esc(invoice.buyerPhone)).append("\n");
            html.append(DIV_END);
        }
        if (invoice.buyerBank != null && !invoice.buyerBank.isEmpty()) {
            html.append(INFO_ROW_START);
            html.append("<span>开户行及账号:</span>").append(esc(invoice.buyerBank)).append("\n");
            html.append(DIV_END);
        }
        
        // 商品明细表
        html.append("<table class=\"items-table\">\n");
        html.append("<tr><th>商品名称</th><th>规格型号</th><th>单位</th><th>数量</th><th>单价</th><th>金额</th><th>税率</th><th>税额</th></tr>\n");
        
        if (invoice.items != null) {
            for (InvoiceItem item : invoice.items) {
                html.append("<tr>\n");
                html.append("<td>").append(esc(item.productName)).append("</td>\n");
                html.append("<td>").append(esc(item.specification)).append("</td>\n");
                html.append("<td>").append(esc(item.unit)).append("</td>\n");
                html.append("<td>").append(item.quantity).append("</td>\n");
                html.append("<td>").append(formatAmount(item.unitPrice)).append("</td>\n");
                html.append("<td>").append(formatAmount(item.amount)).append("</td>\n");
                html.append("<td>").append(formatPercent(item.taxRate)).append("</td>\n");
                html.append("<td>").append(formatAmount(item.taxAmount)).append("</td>\n");
                html.append("</tr>\n");
            }
        }
        
        html.append("</table>\n");
        
        // 合计金额
        html.append("<div class=\"amount-row\">\n");
        html.append("合计金额（不含税）: ").append(formatAmount(invoice.totalAmount)).append(" 元\n");
        html.append(DIV_END);
        
        html.append("<div class=\"amount-row\">\n");
        html.append("税额: ").append(formatAmount(invoice.taxAmount)).append(" 元\n");
        html.append(DIV_END);
        
        html.append("<div class=\"amount-row\" style=\"font-size: 20px; font-weight: bold;\">\n");
        html.append("价税合计（大写）: ").append(toChineseAmount(invoice.finalAmount)).append("\n");
        html.append(DIV_END);
        
        html.append("<div class=\"amount-row\" style=\"font-size: 20px; font-weight: bold;\">\n");
        html.append("价税合计（小写）: ").append(formatAmount(invoice.finalAmount)).append(" 元\n");
        html.append(DIV_END);
        
        // 销售方信息
        html.append("<div class=\"section-title\" style=\"font-weight: bold; margin: 15px 0;\">销售方信息</div>\n");
        html.append(INFO_ROW_START);
        html.append("<span>名称:</span>").append(esc(invoice.sellerName)).append("\n");
        html.append(DIV_END);
        html.append(INFO_ROW_START);
        html.append("<span>纳税人识别号:</span>").append(esc(invoice.sellerTaxId)).append("\n");
        html.append(DIV_END);
        html.append(INFO_ROW_START);
        html.append("<span>地址电话:</span>").append(esc(invoice.sellerAddress)).append(" ").append(esc(invoice.sellerPhone)).append("\n");
        html.append(DIV_END);
        html.append(INFO_ROW_START);
        html.append("<span>开户行及账号:</span>").append(esc(invoice.sellerBank)).append("\n");
        html.append(DIV_END);
        
        // 备注
        if (invoice.remark != null && !invoice.remark.isEmpty()) {
            html.append("<div class=\"info-row\" style=\"margin-top: 20px;\">\n");
            html.append("<span>备注:</span>").append(esc(invoice.remark)).append("\n");
            html.append(DIV_END);
        }
        
        // 签章区域
        html.append("<div class=\"footer\">\n");
        html.append("<div class=\"stamp\">发票专用章</div>\n");
        html.append("<div style=\"margin-top: 20px;\">\n");
        html.append("收款人: ").append(esc(invoice.payee != null ? invoice.payee : "")).append("\n");
        html.append("复核人: ").append(esc(invoice.checker != null ? invoice.checker : "")).append("\n");
        html.append("开票人: ").append(esc(invoice.createBy != null ? invoice.createBy : "")).append("\n");
        html.append(DIV_END);
        html.append(DIV_END);
        
        html.append(DIV_END);
        html.append("</body>\n");
        html.append("</html>\n");
        
        // 写入文件
        File file = new File(filePath);
        try (java.io.BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write(html.toString());
        }
        
        logger.info("发票 HTML 生成成功: {}", filePath);
        
        return filePath;
    }
    
    /**
     * 生成校验码
     */
    private static String generateCheckCode() {
        return String.format("%06d", System.currentTimeMillis() % 1000000);
    }
    
    /**
     * 格式化金额
     */
    private static String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, RoundingMode.HALF_UP).toString();
    }
    
    /**
     * 格式化百分比
     */
    private static String formatPercent(BigDecimal rate) {
        if (rate == null) return "0%";
        // 必须给 RoundingMode：税率 0.065 时 6.500.setScale(0) 会抛
        // ArithmeticException: Rounding necessary，打印发票接口直接 500（TD-034）
        return rate.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toString() + "%";
    }

    private static String formatDate(Date date) {
        return java.time.Instant.ofEpochMilli(date.getTime())
            .atZone(java.time.ZoneId.systemDefault())
            .format(com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME);
    }

    /**
     * HTML 转义，防止发票字段注入脚本/HTML（存储型 XSS）
     */
    private static String esc(Object value) {
        if (value == null) return "";
        return value.toString()
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
    
    /**
     * 数字转中文大写
     */
    private static String toChineseAmount(BigDecimal amount) {
        if (amount == null) return "零元整";

        String[] digits = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
        String[] units = {"", "拾", "佰", "仟", "万", "拾", "佰", "仟", "亿"};

        // 保留两位小数，分离整数和小数部分
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        long integerValue = amount.setScale(0, RoundingMode.DOWN).longValue();
        int fen = amount.remainder(BigDecimal.ONE)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValue();
        int jiao = fen / 10;
        int fenDigit = fen % 10;

        if (integerValue == 0 && jiao == 0 && fenDigit == 0) return "零元整";

        StringBuilder result = new StringBuilder();

        // 整数部分
        if (integerValue > 0) {
            long value = integerValue;
            int unitIndex = 0;
            boolean lastZero = false;

            while (value > 0) {
                int digit = (int)(value % 10);

                if (digit == 0) {
                    if (!lastZero && unitIndex > 0) {
                        result.insert(0, digits[0]);
                        lastZero = true;
                    }
                } else {
                    result.insert(0, digits[digit] + units[unitIndex]);
                    lastZero = false;
                }

                value /= 10;
                unitIndex++;
            }
            result.append("元");
        } else {
            result.append("零元");
        }

        // 角分部分
        if (jiao == 0 && fenDigit == 0) {
            result.append("整");
        } else {
            if (jiao > 0) {
                result.append(digits[jiao]).append("角");
            } else if (integerValue > 0) {
                result.append("零");
            }
            if (fenDigit > 0) {
                result.append(digits[fenDigit]).append("分");
            }
        }

        return result.toString();
    }
    
    /**
     * 设置输出目录
     */
    public static void setOutputDir(String dir) {
        outputDir = dir;
    }
    
    /**
     * 获取输出目录
     */
    public static String getOutputDir() {
        return outputDir;
    }
}
