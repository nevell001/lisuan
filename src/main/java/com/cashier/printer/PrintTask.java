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

    /**
     * 正文之后直写的原始字节（ESC/POS 指令，如条码）
     *
     * <p>只有能发原始字节的设备（{@link NetworkPrinterDevice#print(PrintTask)}）会写出它；
     * 文本/文件小票的调用方不设置该字段，因此控制字节不会混进小票文本。</p>
     */
    private final byte[] rawPrintBytes;

    /**
     * 打印状态
     */
    private PrintTaskStatus status;

    /**
     * 开始打印时间
     */
    private Date startedAt;

    /**
     * 完成打印时间
     */
    private Date finishedAt;

    /**
     * 失败原因
     */
    private String errorMessage;
    
    public PrintTask(String taskId, String taskName, PrintTaskType taskType, String content) {
        this(taskId, taskName, taskType, content, 1, false, false, false, false);
    }
    
    public PrintTask(String taskId, String taskName, PrintTaskType taskType, String content,
                    int copies, boolean printLogo, boolean openCashDrawer, 
                    boolean cutPaper, boolean requirePreview) {
        this(taskId, taskName, taskType, content, copies, printLogo, openCashDrawer,
             cutPaper, requirePreview, null);
    }

    /**
     * @param rawPrintBytes 正文之后直写的原始字节（ESC/POS 指令）；不需要时传 null
     */
    public PrintTask(String taskId, String taskName, PrintTaskType taskType, String content,
                    int copies, boolean printLogo, boolean openCashDrawer,
                    boolean cutPaper, boolean requirePreview, byte[] rawPrintBytes) {
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
        this.rawPrintBytes = rawPrintBytes;
        this.status = PrintTaskStatus.PENDING;
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
     * 正文之后要直写的原始字节（ESC/POS 指令，如条码）；未设置时为 null
     */
    public byte[] getRawPrintBytes() {
        return rawPrintBytes;
    }

    public PrintTaskStatus getStatus() {
        return status;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public Date getFinishedAt() {
        return finishedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void markRunning() {
        this.status = PrintTaskStatus.RUNNING;
        this.startedAt = new Date();
        this.finishedAt = null;
        this.errorMessage = null;
    }

    public void markSuccess() {
        this.status = PrintTaskStatus.SUCCESS;
        this.finishedAt = new Date();
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = PrintTaskStatus.FAILED;
        this.finishedAt = new Date();
        this.errorMessage = errorMessage;
    }
    
    /**
     * 创建销售小票任务
     */
    public static PrintTask createReceiptTask(String content, boolean printLogo, boolean openCashDrawer) {
        return createReceiptTask(content, printLogo, openCashDrawer, null);
    }

    /**
     * 创建销售小票任务
     * @param rawPrintBytes 正文之后直写的原始字节（ESC/POS 指令，如条码）；不需要时传 null
     */
    public static PrintTask createReceiptTask(String content, boolean printLogo, boolean openCashDrawer,
                                             byte[] rawPrintBytes) {
        String taskId = "RCP-" + System.currentTimeMillis();
        return new PrintTask(taskId, "销售小票", PrintTaskType.RECEIPT, content,
                           1, printLogo, openCashDrawer, true, false, rawPrintBytes);
    }

    /**
     * 创建测试打印任务
     */
    public static PrintTask createTestTask(String content) {
        String taskId = "TST-" + System.currentTimeMillis();
        return new PrintTask(taskId, "测试打印", PrintTaskType.TEST, content,
                           1, true, false, true, false);
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
