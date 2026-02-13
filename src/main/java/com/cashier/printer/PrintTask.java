package com.cashier.printer;

import java.util.Date;

/**
 * 打印任务类
 */
public class PrintTask {
    
    /**
     * 任务ID
     */
    private final String taskId;
    
    /**
     * 任务名称
     */
    private final String taskName;
    
    /**
     * 任务类型
     */
    private final PrintTaskType taskType;
    
    /**
     * 打印内容
     */
    private final String content;
    
    /**
     * 创建时间
     */
    private final Date createdAt;
    
    /**
     * 打印份数
     */
    private final int copies;
    
    /**
     * 是否打印Logo
     */
    private final boolean printLogo;
    
    /**
     * 是否打开钱箱
     */
    private final boolean openCashDrawer;
    
    /**
     * 是否切纸
     */
    private final boolean cutPaper;
    
    /**
     * 是否需要预览
     */
    private final boolean requirePreview;
    
    public PrintTask(String taskId, String taskName, PrintTaskType taskType, String content) {
        this(taskId, taskName, taskType, content, 1, false, false, false, false);
    }
    
    public PrintTask(String taskId, String taskName, PrintTaskType taskType, String content,
                    int copies, boolean printLogo, boolean openCashDrawer, 
                    boolean cutPaper, boolean requirePreview) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskType = taskType;
        this.content = content;
        this.createdAt = new Date();
        this.copies = copies;
        this.printLogo = printLogo;
        this.openCashDrawer = openCashDrawer;
        this.cutPaper = cutPaper;
        this.requirePreview = requirePreview;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public String getTaskName() {
        return taskName;
    }
    
    public PrintTaskType getTaskType() {
        return taskType;
    }
    
    public String getContent() {
        return content;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public int getCopies() {
        return copies;
    }
    
    public boolean isPrintLogo() {
        return printLogo;
    }
    
    public boolean isOpenCashDrawer() {
        return openCashDrawer;
    }
    
    public boolean isCutPaper() {
        return cutPaper;
    }
    
    public boolean isRequirePreview() {
        return requirePreview;
    }
    
    /**
     * 创建销售小票任务
     */
    public static PrintTask createReceiptTask(String content, boolean printLogo, boolean openCashDrawer) {
        String taskId = "RCP-" + System.currentTimeMillis();
        return new PrintTask(taskId, "销售小票", PrintTaskType.RECEIPT, content, 
                           1, printLogo, openCashDrawer, true, false);
    }
    
    /**
     * 创建入库单据任务
     */
    public static PrintTask createInboundTask(String content) {
        String taskId = "INB-" + System.currentTimeMillis();
        return new PrintTask(taskId, "入库单据", PrintTaskType.INBOUND, content, 
                           1, false, false, true, false);
    }
    
    /**
     * 创建会员收据任务
     */
    public static PrintTask createMemberReceiptTask(String content) {
        String taskId = "MBR-" + System.currentTimeMillis();
        return new PrintTask(taskId, "会员收据", PrintTaskType.MEMBER_RECEIPT, content, 
                           1, false, false, true, false);
    }
    
    /**
     * 创建盘点报表任务
     */
    public static PrintTask createInventoryReportTask(String content) {
        String taskId = "INV-" + System.currentTimeMillis();
        return new PrintTask(taskId, "盘点报表", PrintTaskType.INVENTORY_REPORT, content, 
                           1, false, false, true, true);
    }
    
    /**
     * 创建销售统计报表任务
     */
    public static PrintTask createSalesReportTask(String content) {
        String taskId = "SLR-" + System.currentTimeMillis();
        return new PrintTask(taskId, "销售统计报表", PrintTaskType.SALES_REPORT, content, 
                           1, false, false, true, true);
    }
}