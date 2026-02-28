package com.cashier.controller;

import com.cashier.dao.TransactionDAO;
import com.cashier.model.Transaction;
import com.cashier.model.Product;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import javafx.fxml.FXML;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import javafx.scene.control.*;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 数据统计控制器
 * 处理销售统计和报表
 */
public class StatisticsController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(StatisticsController.class);

    // 导出目录
    private static final String EXPORT_DIR = System.getProperty("user.home") + File.separator + "cashier-exports";

    // PDF 布局常量
    private static final float MARGIN = 50;
    private static final float ROW_HEIGHT = 20;
    private static final float[] COLUMN_WIDTHS = {150, 80, 120};

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private ComboBox<String> timeRangeComboBox;

    @FXML
    private Label totalSalesLabel;

    @FXML
    private Label transactionCountLabel;

    @FXML
    private Label avgTransactionLabel;

    @FXML
    private Label memberSalesLabel;

    @FXML
    private Label cashSalesLabel;

    @FXML
    private Label wechatSalesLabel;

    @FXML
    private Label alipaySalesLabel;

    @FXML
    private Label cardSalesLabel;

    @FXML
    private Label topProductLabel;

    @FXML
    private Label topProductCountLabel;

    @FXML
    private Label topProductAmountLabel;

    @FXML
    private TableView<StatisticsRecord> categoryTable;

    @FXML
    private TableColumn<StatisticsRecord, String> categoryNameColumn;

    @FXML
    private TableColumn<StatisticsRecord, String> categoryCountColumn;

    @FXML
    private TableColumn<StatisticsRecord, String> categoryAmountColumn;

    @FXML
    private TableView<StatisticsRecord> hourlyTable;

    @FXML
    private TableColumn<StatisticsRecord, String> hourColumn;

    @FXML
    private TableColumn<StatisticsRecord, String> hourCountColumn;

    @FXML
    private TableColumn<StatisticsRecord, String> hourAmountColumn;

    @FXML
    private Button queryButton;

    @FXML
    private Button exportButton;

    private List<Transaction> allTransactions;
    private List<Transaction> currentFilteredTransactions;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 确保导出目录存在
        createExportDirectory();

        // 初始化时间范围下拉框
        timeRangeComboBox.setItems(javafx.collections.FXCollections.observableArrayList(
            "今天",
            "昨天",
            "本周",
            "上周",
            "本月",
            "上月",
            "自定义"
        ));
        timeRangeComboBox.getSelectionModel().select(0);

        // 设置默认日期范围（今天）
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today);
        endDatePicker.setValue(today);

        // 设置表格列
        setupCategoryTableColumns();
        setupHourlyTableColumns();

        // 加载交易数据
        loadTransactions();

        // 执行查询
        handleQuery();

        // 监听时间范围变化
        timeRangeComboBox.setOnAction(event -> handleTimeRangeChange());
    }

    /**
     * 创建导出目录
     */
    private void createExportDirectory() {
        try {
            Path exportPath = Paths.get(EXPORT_DIR);
            if (!Files.exists(exportPath)) {
                Files.createDirectories(exportPath);
                logger.info("导出目录创建成功: {}", EXPORT_DIR);
            }
        } catch (IOException e) {
            logger.error("创建导出目录失败", e);
        }
    }

    /**
     * 设置分类表格列
     */
    private void setupCategoryTableColumns() {
        categoryNameColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().name));
        categoryCountColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().count)));
        categoryAmountColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format("¥%.2f", cellData.getValue().amount)));
    }

    /**
     * 设置小时表格列
     */
    private void setupHourlyTableColumns() {
        hourColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().name));
        hourCountColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().count)));
        hourAmountColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format("¥%.2f", cellData.getValue().amount)));
    }

    /**
     * 加载交易数据
     */
    private void loadTransactions() {
        logger.info("StatisticsController: 开始加载交易数据...");
        try {
            allTransactions = TransactionDAO.findAll();
        } catch (SQLException e) {
            logger.error("加载交易数据失败", e);
            showError("加载交易数据失败: " + e.getMessage());
            allTransactions = new java.util.ArrayList<>();
        }
        logger.info("StatisticsController: 加载了 {} 条交易记录", allTransactions.size());
    }

    /**
     * 处理时间范围变化
     */
    private void handleTimeRangeChange() {
        String selected = timeRangeComboBox.getSelectionModel().getSelectedItem();
        LocalDate today = LocalDate.now();

        switch (selected) {
            case "今天":
                startDatePicker.setValue(today);
                endDatePicker.setValue(today);
                break;
            case "昨天":
                LocalDate yesterday = today.minusDays(1);
                startDatePicker.setValue(yesterday);
                endDatePicker.setValue(yesterday);
                break;
            case "本周":
                LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
                startDatePicker.setValue(startOfWeek);
                endDatePicker.setValue(today);
                break;
            case "上周":
                LocalDate startOfLastWeek = today.minusDays(today.getDayOfWeek().getValue() - 1).minusWeeks(1);
                LocalDate endOfLastWeek = startOfLastWeek.plusDays(6);
                startDatePicker.setValue(startOfLastWeek);
                endDatePicker.setValue(endOfLastWeek);
                break;
            case "本月":
                LocalDate startOfMonth = today.withDayOfMonth(1);
                startDatePicker.setValue(startOfMonth);
                endDatePicker.setValue(today);
                break;
            case "上月":
                LocalDate startOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
                LocalDate endOfLastMonth = today.withDayOfMonth(1).minusDays(1);
                startDatePicker.setValue(startOfLastMonth);
                endDatePicker.setValue(endOfLastMonth);
                break;
            case "自定义":
                // 不自动设置日期
                break;
        }
    }

    /**
     * 处理查询
     */
    @FXML
    private void handleQuery() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            showError("请选择日期范围！");
            return;
        }

        if (startDate.isAfter(endDate)) {
            showError("开始日期不能晚于结束日期！");
            return;
        }

        // 筛选交易记录
        currentFilteredTransactions = filterTransactionsByDate(startDate, endDate);

        // 计算统计数据
        calculateStatistics(currentFilteredTransactions);
    }

    /**
     * 按日期筛选交易记录
     */
    private List<Transaction> filterTransactionsByDate(LocalDate startDate, LocalDate endDate) {
        List<Transaction> filtered = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (Transaction t : allTransactions) {
            try {
                Date date = sdf.parse(t.timestamp);
                LocalDate localDate = date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

                if (!localDate.isBefore(startDate) && !localDate.isAfter(endDate)) {
                    filtered.add(t);
                }
            } catch (Exception e) {
                // 日期解析失败，跳过该记录
            }
        }

        return filtered;
    }

    /**
     * 计算统计数据
     */
    private void calculateStatistics(List<Transaction> transactions) {
        // 总销售额
        double totalSales = 0.0;
        int transactionCount = transactions.size();

        // 按支付方式统计
        double cashSales = 0.0;
        double wechatSales = 0.0;
        double alipaySales = 0.0;
        double cardSales = 0.0;
        double memberSales = 0.0;

        // 商品统计
        Map<String, Integer> productCountMap = new HashMap<>();
        Map<String, Double> productAmountMap = new HashMap<>();

        // 分类统计
        Map<String, Integer> categoryCountMap = new HashMap<>();
        Map<String, Double> categoryAmountMap = new HashMap<>();

        // 小时统计
        Map<Integer, Integer> hourCountMap = new HashMap<>();
        Map<Integer, Double> hourAmountMap = new HashMap<>();

        for (Transaction t : transactions) {
            totalSales += t.finalAmount;

            // 支付方式统计
            switch (t.paymentMethod) {
                case "现金":
                    cashSales += t.finalAmount;
                    break;
                case "微信":
                    wechatSales += t.finalAmount;
                    break;
                case "支付宝":
                    alipaySales += t.finalAmount;
                    break;
                case "银行卡":
                    cardSales += t.finalAmount;
                    break;
            }

            // 会员统计
            if (t.memberPhone != null && !t.memberPhone.isEmpty()) {
                memberSales += t.finalAmount;
            }

            // 商品统计
            if (t.items != null) {
                for (var item : t.items) {
                    productCountMap.put(item.name, productCountMap.getOrDefault(item.name, 0) + item.quantity);
                    productAmountMap.put(item.name, productAmountMap.getOrDefault(item.name, 0.0) + item.price * item.quantity);

                    // 分类统计
                    String category = item.category;
                    if (category == null || category.isEmpty()) {
                        category = "未分类";
                    }
                    categoryCountMap.put(category, categoryCountMap.getOrDefault(category, 0) + item.quantity);
                    categoryAmountMap.put(category, categoryAmountMap.getOrDefault(category, 0.0) + item.price * item.quantity);
                }
            }

            // 小时统计
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = sdf.parse(t.timestamp);
                // 使用 Calendar API 替代已过时的 Date.getHours()
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                hourCountMap.put(hour, hourCountMap.getOrDefault(hour, 0) + 1);
                hourAmountMap.put(hour, hourAmountMap.getOrDefault(hour, 0.0) + t.finalAmount);
            } catch (Exception e) {
                // 解析失败，跳过
            }
        }

        // 更新UI
        totalSalesLabel.setText(String.format("¥%.2f", totalSales));
        transactionCountLabel.setText(String.valueOf(transactionCount));
        avgTransactionLabel.setText(transactionCount > 0 ? String.format("¥%.2f", totalSales / transactionCount) : "¥0.00");
        memberSalesLabel.setText(String.format("¥%.2f", memberSales));
        cashSalesLabel.setText(String.format("¥%.2f", cashSales));
        wechatSalesLabel.setText(String.format("¥%.2f", wechatSales));
        alipaySalesLabel.setText(String.format("¥%.2f", alipaySales));
        cardSalesLabel.setText(String.format("¥%.2f", cardSales));

        // 找出销售最多的商品
        if (!productCountMap.isEmpty()) {
            String topProduct = Collections.max(productCountMap.entrySet(), Map.Entry.comparingByValue()).getKey();
            topProductLabel.setText(topProduct);
            topProductCountLabel.setText(String.valueOf(productCountMap.get(topProduct)));
            topProductAmountLabel.setText(String.format("¥%.2f", productAmountMap.get(topProduct)));
        } else {
            topProductLabel.setText("无");
            topProductCountLabel.setText("0");
            topProductAmountLabel.setText("¥0.00");
        }

        // 更新分类表格
        updateCategoryTable(categoryCountMap, categoryAmountMap);

        // 更新小时表格
        updateHourlyTable(hourCountMap, hourAmountMap);
    }

    /**
     * 更新分类表格
     */
    private void updateCategoryTable(Map<String, Integer> countMap, Map<String, Double> amountMap) {
        javafx.collections.ObservableList<StatisticsRecord> list = javafx.collections.FXCollections.observableArrayList();

        for (String category : countMap.keySet()) {
            list.add(new StatisticsRecord(category, countMap.get(category), amountMap.get(category)));
        }

        // 按金额排序
        list.sort((a, b) -> Double.compare(b.amount, a.amount));
        categoryTable.setItems(list);
    }

    /**
     * 更新小时表格
     */
    private void updateHourlyTable(Map<Integer, Integer> countMap, Map<Integer, Double> amountMap) {
        javafx.collections.ObservableList<StatisticsRecord> list = javafx.collections.FXCollections.observableArrayList();

        for (int hour = 0; hour < 24; hour++) {
            if (countMap.containsKey(hour)) {
                list.add(new StatisticsRecord(
                    String.format("%02d:00-%02d:00", hour, hour + 1),
                    countMap.get(hour),
                    amountMap.get(hour)
                ));
            }
        }

        hourlyTable.setItems(list);
    }

    /**
     * 处理导出
     */
    @FXML
    private void handleExport() {
        if (currentFilteredTransactions == null || currentFilteredTransactions.isEmpty()) {
            showError("当前没有可导出的数据！请先执行查询。");
            return;
        }

        // 显示导出选项对话框
        showExportDialog();
    }

    /**
     * 显示导出对话框
     */
    private void showExportDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("导出数据");
        alert.setHeaderText("选择导出格式");
        alert.setContentText("请选择要导出的文件格式：");

        // 创建自定义按钮
        ButtonType excelButton = new ButtonType("导出为 Excel (.xlsx)");
        ButtonType pdfButton = new ButtonType("导出为 PDF (.pdf)");
        ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(excelButton, pdfButton, cancelButton);

        // 显示对话框并处理用户选择
        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == excelButton) {
                exportToExcel();
            } else if (buttonType == pdfButton) {
                exportToPDF();
            }
        });
    }

    /**
     * 导出为 Excel
     */
    private void exportToExcel() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // 1. 创建分类统计工作表
            createCategorySheet(workbook);

            // 2. 创建小时统计工作表
            createHourlySheet(workbook);

            // 3. 创建交易记录详情工作表
            createTransactionsSheet(workbook);

            // 生成带时间戳的文件名
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = EXPORT_DIR + File.separator + "销售统计_" + timestamp + ".xlsx";

            // 保存文件
            try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
                workbook.write(fileOut);
            }

            logger.info("Excel 导出成功: {}", fileName);
            showExportSuccess("Excel", fileName);

        } catch (IOException e) {
            logger.error("Excel 导出失败", e);
            showError("Excel 导出失败: " + e.getMessage());
        }
    }

    /**
     * 创建分类统计工作表
     */
    private void createCategorySheet(XSSFWorkbook workbook) {
        Sheet sheet = workbook.createSheet("分类统计");

        // 创建标题样式
        CellStyle headerStyle = createHeaderStyle(workbook);

        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"分类名称", "数量", "金额"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 填充数据
        javafx.collections.ObservableList<StatisticsRecord> data = categoryTable.getItems();
        if (data != null) {
            int rowNum = 1;
            for (StatisticsRecord record : data) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(record.name);
                row.createCell(1).setCellValue(record.count);
                row.createCell(2).setCellValue(record.amount);
            }

            // 自动调整列宽
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.autoSizeColumn(2);
        }
    }

    /**
     * 创建小时统计工作表
     */
    private void createHourlySheet(XSSFWorkbook workbook) {
        Sheet sheet = workbook.createSheet("小时统计");

        // 创建标题样式
        CellStyle headerStyle = createHeaderStyle(workbook);

        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"时间范围", "订单数", "金额"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 填充数据
        javafx.collections.ObservableList<StatisticsRecord> data = hourlyTable.getItems();
        if (data != null) {
            int rowNum = 1;
            for (StatisticsRecord record : data) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(record.name);
                row.createCell(1).setCellValue(record.count);
                row.createCell(2).setCellValue(record.amount);
            }

            // 自动调整列宽
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.autoSizeColumn(2);
        }
    }

    /**
     * 创建交易记录详情工作表
     */
    private void createTransactionsSheet(XSSFWorkbook workbook) {
        Sheet sheet = workbook.createSheet("交易记录");

        // 创建标题样式
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle moneyStyle = createMoneyStyle(workbook);

        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"交易ID", "日期时间", "支付方式", "会员手机", "操作员", "商品数量", "总金额", "税额", "最终金额", "商品明细"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 填充数据
        if (currentFilteredTransactions != null) {
            int rowNum = 1;
            for (Transaction t : currentFilteredTransactions) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(t.transactionId);
                row.createCell(1).setCellValue(t.timestamp);
                row.createCell(2).setCellValue(t.paymentMethod);
                row.createCell(3).setCellValue(t.memberPhone != null ? t.memberPhone : "");
                row.createCell(4).setCellValue(t.operatorName != null ? t.operatorName : "");

                int itemCount = t.items != null ? t.items.size() : 0;
                row.createCell(5).setCellValue(itemCount);

                Cell totalCell = row.createCell(6);
                totalCell.setCellValue(t.totalAmount);
                totalCell.setCellStyle(moneyStyle);

                Cell taxCell = row.createCell(7);
                taxCell.setCellValue(t.tax);
                taxCell.setCellStyle(moneyStyle);

                Cell finalCell = row.createCell(8);
                finalCell.setCellValue(t.finalAmount);
                finalCell.setCellStyle(moneyStyle);

                // 商品明细
                StringBuilder itemsStr = new StringBuilder();
                if (t.items != null && !t.items.isEmpty()) {
                    for (int i = 0; i < t.items.size(); i++) {
                        Product item = t.items.get(i);
                        itemsStr.append(item.name)
                               .append(" x").append(item.quantity)
                               .append(" (¥").append(item.price).append(")");
                        if (i < t.items.size() - 1) {
                            itemsStr.append("; ");
                        }
                    }
                }
                row.createCell(9).setCellValue(itemsStr.toString());
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
        }
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 设置字体
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        // 设置背景色
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 设置边框
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // 设置对齐
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    /**
     * 创建金额样式
     */
    private CellStyle createMoneyStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 设置货币格式
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("¥#,##0.00"));

        // 设置对齐
        style.setAlignment(HorizontalAlignment.RIGHT);

        return style;
    }

    /**
     * 导出为 PDF
     */
    private void exportToPDF() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = EXPORT_DIR + File.separator + "销售统计_" + timestamp + ".pdf";

        try (PDDocument document = new PDDocument()) {
            // 1. 创建封面页
            createCoverPage(document);

            // 2. 创建分类统计页
            createCategoryPDFPage(document);

            // 3. 创建小时统计页
            createHourlyPDFPage(document);

            // 4. 创建交易记录页
            createTransactionsPDFPage(document);

            // 保存文件
            document.save(fileName);

            logger.info("PDF 导出成功: {}", fileName);
            showExportSuccess("PDF", fileName);

        } catch (IOException e) {
            logger.error("PDF 导出失败", e);
            showError("PDF 导出失败: " + e.getMessage());
        }
    }

    /**
     * 创建封面页
     */
    private void createCoverPage(PDDocument document) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 24);
            contentStream.beginText();
            contentStream.newLineAtOffset(200, 700);
            contentStream.showText("销售统计报告");
            contentStream.endText();

            contentStream.setFont(PDType1Font.HELVETICA, 14);
            contentStream.beginText();
            contentStream.newLineAtOffset(200, 650);

            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
            String dateRange = "统计时间: " + startDate.format(formatter) + " 至 " + endDate.format(formatter);
            contentStream.showText(dateRange);
            contentStream.endText();

            contentStream.beginText();
            contentStream.newLineAtOffset(200, 620);
            String exportTime = "导出时间: " + new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss").format(new Date());
            contentStream.showText(exportTime);
            contentStream.endText();

            // 显示汇总数据
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            float yPosition = 550;

            contentStream.beginText();
            contentStream.newLineAtOffset(100, yPosition);
            contentStream.showText("总销售额: " + totalSalesLabel.getText());
            contentStream.endText();

            yPosition -= 30;
            contentStream.beginText();
            contentStream.newLineAtOffset(100, yPosition);
            contentStream.showText("交易笔数: " + transactionCountLabel.getText());
            contentStream.endText();

            yPosition -= 30;
            contentStream.beginText();
            contentStream.newLineAtOffset(100, yPosition);
            contentStream.showText("平均交易额: " + avgTransactionLabel.getText());
            contentStream.endText();

            yPosition -= 30;
            contentStream.beginText();
            contentStream.newLineAtOffset(100, yPosition);
            contentStream.showText("会员销售额: " + memberSalesLabel.getText());
            contentStream.endText();
        }
    }

    /**
     * 创建分类统计PDF页
     */
    private void createCategoryPDFPage(PDDocument document) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float yPosition = 750;

            // 标题
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
            contentStream.beginText();
            contentStream.newLineAtOffset(220, yPosition);
            contentStream.showText("分类统计");
            contentStream.endText();

            yPosition -= 40;

            // 表头
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
            drawTableRow(contentStream, yPosition, new String[]{"分类名称", "数量", "金额"}, COLUMN_WIDTHS);

            yPosition -= ROW_HEIGHT;

            // 数据行
            contentStream.setFont(PDType1Font.HELVETICA, 10);
            javafx.collections.ObservableList<StatisticsRecord> data = categoryTable.getItems();
            if (data != null) {
                for (StatisticsRecord record : data) {
                    if (yPosition < MARGIN) {
                        break; // 防止超出页面
                    }
                    drawTableRow(contentStream, yPosition,
                        new String[]{
                            record.name,
                            String.valueOf(record.count),
                            String.format("¥%.2f", record.amount)
                        }, COLUMN_WIDTHS);
                    yPosition -= ROW_HEIGHT;
                }
            }
        }
    }

    /**
     * 创建小时统计PDF页
     */
    private void createHourlyPDFPage(PDDocument document) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float yPosition = 750;

            // 标题
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
            contentStream.beginText();
            contentStream.newLineAtOffset(220, yPosition);
            contentStream.showText("小时统计");
            contentStream.endText();

            yPosition -= 40;

            // 表头
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
            drawTableRow(contentStream, yPosition, new String[]{"时间范围", "订单数", "金额"}, COLUMN_WIDTHS);

            yPosition -= ROW_HEIGHT;

            // 数据行
            contentStream.setFont(PDType1Font.HELVETICA, 10);
            javafx.collections.ObservableList<StatisticsRecord> data = hourlyTable.getItems();
            if (data != null) {
                for (StatisticsRecord record : data) {
                    if (yPosition < MARGIN) {
                        break;
                    }
                    drawTableRow(contentStream, yPosition,
                        new String[]{
                            record.name,
                            String.valueOf(record.count),
                            String.format("¥%.2f", record.amount)
                        }, new float[]{120, 80, 100});
                    yPosition -= ROW_HEIGHT;
                }
            }
        }
    }

    /**
     * 创建交易记录PDF页
     */
    private void createTransactionsPDFPage(PDDocument document) throws IOException {
        int pageSize = 25; // 每页显示记录数
        int totalRecords = currentFilteredTransactions != null ? currentFilteredTransactions.size() : 0;
        int pageCount = (int) Math.ceil((double) totalRecords / pageSize);

        for (int pageIdx = 0; pageIdx < Math.max(pageCount, 1); pageIdx++) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float yPosition = 750;

                // 标题
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
                contentStream.beginText();
                contentStream.newLineAtOffset(180, yPosition);
                contentStream.showText("交易记录详情");
                contentStream.endText();

                yPosition -= 40;

                // 表头 - 使用较小的字体以适应更多列
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 8);
                float[] txColumnWidths = {60, 70, 50, 50, 40, 40, 50, 50};
                drawTableRow(contentStream, yPosition,
                    new String[]{"交易ID", "日期时间", "支付方式", "会员", "操作员", "数量", "总金额", "最终金额"},
                    txColumnWidths);

                yPosition -= ROW_HEIGHT;

                // 数据行
                contentStream.setFont(PDType1Font.HELVETICA, 8);
                int startIdx = pageIdx * pageSize;
                int endIdx = Math.min(startIdx + pageSize, totalRecords);

                for (int i = startIdx; i < endIdx; i++) {
                    if (yPosition < MARGIN) {
                        break;
                    }
                    Transaction t = currentFilteredTransactions.get(i);

                    // 截取交易ID后8位
                    String shortTxId = t.transactionId.length() > 8
                        ? t.transactionId.substring(t.transactionId.length() - 8)
                        : t.transactionId;

                    // 格式化日期时间（只显示日期和时分）
                    String dateTime = t.timestamp;
                    try {
                        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date date = inputFormat.parse(t.timestamp);
                        SimpleDateFormat outputFormat = new SimpleDateFormat("MM-dd HH:mm");
                        dateTime = outputFormat.format(date);
                    } catch (Exception e) {
                        // 使用原始格式
                    }

                    drawTableRow(contentStream, yPosition,
                        new String[]{
                            shortTxId,
                            dateTime,
                            t.paymentMethod,
                            t.memberPhone != null && !t.memberPhone.isEmpty() ? "是" : "否",
                            t.operatorName != null ? t.operatorName.substring(0, Math.min(3, t.operatorName.length())) : "",
                            String.valueOf(t.items != null ? t.items.size() : 0),
                            String.format("¥%.0f", t.totalAmount),
                            String.format("¥%.0f", t.finalAmount)
                        }, txColumnWidths);
                    yPosition -= ROW_HEIGHT;
                }

                // 页码
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(270, 30);
                contentStream.showText("第 " + (pageIdx + 1) + " 页 / 共 " + Math.max(pageCount, 1) + " 页");
                contentStream.endText();
            }
        }
    }

    /**
     * 绘制PDF表格行
     */
    private void drawTableRow(PDPageContentStream contentStream, float y, String[] texts, float[] widths) throws IOException {
        float x = MARGIN;

        // 绘制边框
        contentStream.setLineWidth(0.5f);
        contentStream.moveTo(MARGIN, y);
        contentStream.lineTo(MARGIN + sum(widths), y);
        contentStream.stroke();

        // 绘制文字
        for (int i = 0; i < texts.length; i++) {
            contentStream.beginText();
            contentStream.newLineAtOffset(x, y - 5);
            // 截断过长的文字
            String text = texts[i];
            if (text != null && text.length() > 15) {
                text = text.substring(0, 12) + "...";
            }
            contentStream.showText(text != null ? text : "");
            contentStream.endText();
            x += widths[i];
        }

        // 绘制底部边框
        contentStream.moveTo(MARGIN, y - ROW_HEIGHT);
        contentStream.lineTo(MARGIN + sum(widths), y - ROW_HEIGHT);
        contentStream.stroke();
    }

    /**
     * 计算数组之和
     */
    private float sum(float[] array) {
        float sum = 0;
        for (float v : array) {
            sum += v;
        }
        return sum;
    }

    /**
     * 显示导出成功消息
     */
    private void showExportSuccess(String format, String fileName) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("导出成功");
        alert.setHeaderText(null);
        alert.setContentText(format + " 文件已成功导出！\n文件位置: " + fileName);

        // 添加打开目录按钮
        ButtonType openButton = new ButtonType("打开导出目录");
        ButtonType okButton = new ButtonType("确定", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(openButton, okButton);

        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == openButton) {
                openExportDirectory();
            }
        });
    }

    /**
     * 打开导出目录
     */
    private void openExportDirectory() {
        try {
            File exportDir = new File(EXPORT_DIR).getAbsoluteFile();

            // 根据操作系统打开文件管理器
            String os = System.getProperty("os.name").toLowerCase();
            Runtime runtime = Runtime.getRuntime();

            if (os.contains("win")) {
                runtime.exec("explorer.exe /select,\"" + exportDir.getAbsolutePath() + "\"");
            } else if (os.contains("mac")) {
                runtime.exec("open " + exportDir.getAbsolutePath());
            } else if (os.contains("nix") || os.contains("nux")) {
                runtime.exec("xdg-open " + exportDir.getAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("打开导出目录失败", e);
            showError("无法打开导出目录: " + e.getMessage());
        }
    }

    /**
     * 显示错误信息
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 统计记录内部类
     */
    private static class StatisticsRecord {
        String name;
        int count;
        double amount;

        public StatisticsRecord(String name, int count, double amount) {
            this.name = name;
            this.count = count;
            this.amount = amount;
        }
    }
}
