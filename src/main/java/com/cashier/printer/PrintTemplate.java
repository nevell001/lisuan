package com.cashier.printer;

import java.util.Map;

/**
 * 打印模板类
 * 用于生成各种单据的打印内容
 */
public class PrintTemplate {
    
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
            "================================\n" +
            "          收银小票           \n" +
            "================================\n" +
            "门店名称: {{storeName}}\n" +
            "收银员: {{cashierName}}\n" +
            "交易号: {{transactionId}}\n" +
            "交易时间: {{transactionTime}}\n" +
            "--------------------------------\n" +
            "{{items}}\n" +
            "--------------------------------\n" +
            "商品数量: {{totalQuantity}}\n" +
            "商品金额: ¥{{totalAmount}}\n" +
            "折扣金额: ¥{{discountAmount}}\n" +
            "应收金额: ¥{{finalAmount}}\n" +
            "实收金额: ¥{{paidAmount}}\n" +
            "找零金额: ¥{{changeAmount}}\n" +
            "支付方式: {{paymentMethod}}\n" +
            "--------------------------------\n" +
            "会员信息: {{memberInfo}}\n" +
            "--------------------------------\n" +
            "谢谢惠顾，欢迎下次光临！\n" +
            "================================\n";
        
        return new PrintTemplate(PrintTaskType.RECEIPT, template);
    }
    
    /**
     * 创建入库单据模板
     */
    public static PrintTemplate createInboundTemplate() {
        String template = 
            "================================\n" +
            "          入库单据           \n" +
            "================================\n" +
            "入库单号: {{inboundNo}}\n" +
            "采购订单号: {{orderNo}}\n" +
            "入库日期: {{inboundDate}}\n" +
            "操作员: {{operator}}\n" +
            "--------------------------------\n" +
            "{{items}}\n" +
            "--------------------------------\n" +
            "入库总数量: {{totalQuantity}}\n" +
            "入库总金额: ¥{{totalAmount}}\n" +
            "--------------------------------\n" +
            "备注: {{remark}}\n" +
            "================================\n";
        
        return new PrintTemplate(PrintTaskType.INBOUND, template);
    }
    
    /**
     * 创建会员收据模板
     */
    public static PrintTemplate createMemberReceiptTemplate() {
        String template = 
            "================================\n" +
            "          会员充值收据         \n" +
            "================================\n" +
            "门店名称: {{storeName}}\n" +
            "收银员: {{cashierName}}\n" +
            "充值时间: {{rechargeTime}}\n" +
            "--------------------------------\n" +
            "会员姓名: {{memberName}}\n" +
            "会员手机: {{memberPhone}}\n" +
            "会员等级: {{memberLevel}}\n" +
            "--------------------------------\n" +
            "充值金额: ¥{{rechargeAmount}}\n" +
            "赠送积分: {{bonusPoints}}\n" +
            "支付方式: {{paymentMethod}}\n" +
            "--------------------------------\n" +
            "充值后余额: ¥{{newBalance}}\n" +
            "充值后积分: {{newPoints}}\n" +
            "--------------------------------\n" +
            "谢谢惠顾，欢迎下次光临！\n" +
            "================================\n";
        
        return new PrintTemplate(PrintTaskType.MEMBER_RECEIPT, template);
    }
    
    /**
     * 创建盘点报表模板
     */
    public static PrintTemplate createInventoryReportTemplate() {
        String template = 
            "================================\n" +
            "          库存盘点报表         \n" +
            "================================\n" +
            "盘点单号: {{checkNo}}\n" +
            "盘点日期: {{checkDate}}\n" +
            "盘点类型: {{checkType}}\n" +
            "操作员: {{operator}}\n" +
            "--------------------------------\n" +
            "{{items}}\n" +
            "--------------------------------\n" +
            "盘点商品总数: {{totalItems}}\n" +
            "差异商品数: {{diffItems}}\n" +
            "--------------------------------\n" +
            "备注: {{remark}}\n" +
            "================================\n";
        
        return new PrintTemplate(PrintTaskType.INVENTORY_REPORT, template);
    }
    
    /**
     * 创建销售统计报表模板
     */
    public static PrintTemplate createSalesReportTemplate() {
        String template = 
            "================================\n" +
            "          销售统计报表         \n" +
            "================================\n" +
            "统计时间: {{reportTime}}\n" +
            "时间范围: {{timeRange}}\n" +
            "--------------------------------\n" +
            "销售总额: ¥{{totalRevenue}}\n" +
            "销售数量: {{totalQuantity}}\n" +
            "交易次数: {{transactionCount}}\n" +
            "平均客单价: ¥{{avgTicket}}\n" +
            "--------------------------------\n" +
            "{{details}}\n" +
            "================================\n";
        
        return new PrintTemplate(PrintTaskType.SALES_REPORT, template);
    }
}
