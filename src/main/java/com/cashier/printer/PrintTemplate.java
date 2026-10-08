package com.cashier.printer;

import java.util.Map;

/**
 * 打印模板类
 * 用于生成各种单据的打印内容
 */
public class PrintTemplate {
    private static final String THICK_SEPARATOR = "================================\n";
    private static final String THIN_SEPARATOR = "--------------------------------\n";
    
    /**
     * 模板类型
     */
    private final PrintTaskType templateType;
    
    /**
     * 模板内容
     */
    private String template;
    
    /**
     * 模板变量
     */
    private Map<String, String> variables;
    
    public PrintTemplate(PrintTaskType templateType, String template) {
        this.templateType = templateType;
        this.template = template;
        this.variables = new java.util.HashMap<>();
    }
    
    /**
     * 设置变量
     * @param key 变量名
     * @param value 变量值
     */
    public void setVariable(String key, String value) {
        variables.put(key, value);
    }
    
    /**
     * 批量设置变量
     * @param vars 变量映射
     */
    public void setVariables(Map<String, String> vars) {
        if (vars != null) {
            variables.putAll(vars);
        }
    }
    
    /**
     * 生成打印内容
     * @return 打印内容
     */
    public String generate() {
        String content = template;
        
        // 替换变量
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue() : "";
            content = content.replace("{{" + key + "}}", value);
        }
        
        return content;
    }
    
    /**
     * 获取模板类型
     * @return 模板类型
     */
    public PrintTaskType getTemplateType() {
        return templateType;
    }
    
    /**
     * 创建销售小票模板
     */
    public static PrintTemplate createReceiptTemplate() {
        String template = 
            THICK_SEPARATOR +
            "          收银小票           \n" +
            THICK_SEPARATOR +
            "门店名称: {{storeName}}\n" +
            // 门店地址/电话：整行（含换行）都放在变量值里，设置里留空时替换成空串，
            // 既不会印出"地址: "这种没有内容的行，也不会多出一条空行。
            // 注意 generate() 只替换**变量表里有**的 key，漏传会原样印出 {{storeInfo}}，
            // 所以每个调用 createReceiptTemplate() 的地方都必须提供该变量（目前只有 PrintUtil）。
            "{{storeInfo}}" +
            "收银员: {{cashierName}}\n" +
            "交易号: {{transactionId}}\n" +
            "交易时间: {{transactionTime}}\n" +
            THIN_SEPARATOR +
            "{{items}}\n" +
            THIN_SEPARATOR +
            "商品数量: {{totalQuantity}}\n" +
            "商品金额: ¥{{totalAmount}}\n" +
            "折扣金额: ¥{{discountAmount}}\n" +
            "应收金额: ¥{{finalAmount}}\n" +
            "实收金额: ¥{{paidAmount}}\n" +
            "找零金额: ¥{{changeAmount}}\n" +
            "支付方式: {{paymentMethod}}\n" +
            THIN_SEPARATOR +
            "会员信息: {{memberInfo}}\n" +
            THIN_SEPARATOR +
            "谢谢惠顾，欢迎下次光临！\n" +
            THICK_SEPARATOR;
        
        return new PrintTemplate(PrintTaskType.RECEIPT, template);
    }
    
    /**
     * 创建入库单据模板
     */
    public static PrintTemplate createInboundTemplate() {
        String template = 
            THICK_SEPARATOR +
            "          入库单据           \n" +
            THICK_SEPARATOR +
            "入库单号: {{inboundNo}}\n" +
            "采购订单号: {{orderNo}}\n" +
            "入库日期: {{inboundDate}}\n" +
            "操作员: {{operator}}\n" +
            THIN_SEPARATOR +
            "{{items}}\n" +
            THIN_SEPARATOR +
            "入库总数量: {{totalQuantity}}\n" +
            "入库总金额: ¥{{totalAmount}}\n" +
            THIN_SEPARATOR +
            "备注: {{remark}}\n" +
            THICK_SEPARATOR;
        
        return new PrintTemplate(PrintTaskType.INBOUND, template);
    }
    
    /**
     * 创建会员收据模板
     */
    public static PrintTemplate createMemberReceiptTemplate() {
        String template = 
            THICK_SEPARATOR +
            "          会员充值收据         \n" +
            THICK_SEPARATOR +
            "门店名称: {{storeName}}\n" +
            "收银员: {{cashierName}}\n" +
            "充值时间: {{rechargeTime}}\n" +
            THIN_SEPARATOR +
            "会员姓名: {{memberName}}\n" +
            "会员手机: {{memberPhone}}\n" +
            "会员等级: {{memberLevel}}\n" +
            THIN_SEPARATOR +
            "充值金额: ¥{{rechargeAmount}}\n" +
            "赠送积分: {{bonusPoints}}\n" +
            "支付方式: {{paymentMethod}}\n" +
            THIN_SEPARATOR +
            "充值后余额: ¥{{newBalance}}\n" +
            "充值后积分: {{newPoints}}\n" +
            THIN_SEPARATOR +
            "谢谢惠顾，欢迎下次光临！\n" +
            THICK_SEPARATOR;
        
        return new PrintTemplate(PrintTaskType.MEMBER_RECEIPT, template);
    }
    
    /**
     * 创建盘点报表模板
     */
    public static PrintTemplate createInventoryReportTemplate() {
        String template = 
            THICK_SEPARATOR +
            "          库存盘点报表         \n" +
            THICK_SEPARATOR +
            "盘点单号: {{checkNo}}\n" +
            "盘点日期: {{checkDate}}\n" +
            "盘点类型: {{checkType}}\n" +
            "操作员: {{operator}}\n" +
            THIN_SEPARATOR +
            "{{items}}\n" +
            THIN_SEPARATOR +
            "盘点商品总数: {{totalItems}}\n" +
            "差异商品数: {{diffItems}}\n" +
            THIN_SEPARATOR +
            "备注: {{remark}}\n" +
            THICK_SEPARATOR;
        
        return new PrintTemplate(PrintTaskType.INVENTORY_REPORT, template);
    }
    
    /**
     * 创建销售统计报表模板
     */
    public static PrintTemplate createSalesReportTemplate() {
        String template = 
            THICK_SEPARATOR +
            "          销售统计报表         \n" +
            THICK_SEPARATOR +
            "统计时间: {{reportTime}}\n" +
            "时间范围: {{timeRange}}\n" +
            THIN_SEPARATOR +
            "销售总额: ¥{{totalRevenue}}\n" +
            "销售数量: {{totalQuantity}}\n" +
            "交易次数: {{transactionCount}}\n" +
            "平均客单价: ¥{{avgTicket}}\n" +
            THIN_SEPARATOR +
            "{{details}}\n" +
            THICK_SEPARATOR;
        
        return new PrintTemplate(PrintTaskType.SALES_REPORT, template);
    }
}
