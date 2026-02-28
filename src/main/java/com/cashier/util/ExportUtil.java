package com.cashier.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 导出工具类
 * 支持导出数据为 Excel 或 PDF 格式
 */
public class ExportUtil {
    private static final Logger logger = LoggerFactory.getLogger(ExportUtil.class);
    private static final String EXPORT_DIR = System.getProperty("user.home") + File.separator + "cashier-exports";

    /**
     * 导出格式枚举
     */
    public enum ExportFormat {
        EXCEL,
        PDF
    }

    /**
     * 导出数据到文件
     *
     * @param title 标题
     * @param headers 表头
     * @param data 数据
     * @param format 导出格式
     * @param subDir 子目录名称
     * @return 导出文件的绝对路径，失败返回 null
     */
    public static String export(String title, List<String> headers, List<String[]> data,
                                ExportFormat format, String subDir) {
        try {
            // 确保导出目录存在
            Path exportPath = Paths.get(EXPORT_DIR, subDir);
            if (!Files.exists(exportPath)) {
                Files.createDirectories(exportPath);
            }

            // 生成文件名
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = title + "_" + timestamp;

            String filePath;
            if (format == ExportFormat.EXCEL) {
                filePath = exportToExcel(title, headers, data, exportPath.toString(), fileName);
            } else {
                filePath = exportToPDF(title, headers, data, exportPath.toString(), fileName);
            }

            return filePath;
        } catch (Exception e) {
            logger.error("导出数据失败", e);
            return null;
        }
    }

    /**
     * 导出为 Excel
     */
    private static String exportToExcel(String title, List<String> headers, List<String[]> data,
                                        String exportPath, String fileName) throws IOException {
        String filePath = exportPath + File.separator + fileName + ".xlsx";

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(title);

            // 创建标题样式
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 创建数据样式
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // 写入标题
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            titleCell.setCellStyle(titleStyle);

            // 写入表头
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // 写入数据
            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(i + 2);
                String[] rowData = data.get(i);
                for (int j = 0; j < rowData.length; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(rowData[j]);
                    cell.setCellStyle(dataStyle);
                }
            }

            // 自动调整列宽
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            // 保存文件
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }

            logger.info("Excel 导出成功: {}", filePath);
            return filePath;
        }
    }

    /**
     * 导出为 PDF
     */
    private static String exportToPDF(String title, List<String> headers, List<String[]> data,
                                      String exportPath, String fileName) throws IOException {
        String filePath = exportPath + File.separator + fileName + ".pdf";

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float margin = 50;
                float yPosition = 750;
                float lineHeight = 20;

                // 写入标题
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(title);
                contentStream.endText();

                yPosition -= 30;

                // 写入表头
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
                float xPosition = margin;
                for (String header : headers) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(xPosition, yPosition);
                    contentStream.showText(header);
                    contentStream.endText();
                    xPosition += 150;
                }

                yPosition -= lineHeight;

                // 写入数据
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                for (String[] rowData : data) {
                    if (yPosition < margin) {
                        break; // 防止超出页面
                    }
                    xPosition = margin;
                    for (String cell : rowData) {
                        contentStream.beginText();
                        contentStream.newLineAtOffset(xPosition, yPosition);
                        // 截断过长的文字
                        String text = cell != null && cell.length() > 20 ? cell.substring(0, 17) + "..." : cell;
                        contentStream.showText(text != null ? text : "");
                        contentStream.endText();
                        xPosition += 150;
                    }
                    yPosition -= lineHeight;
                }
            }

            // 保存文件
            document.save(filePath);

            logger.info("PDF 导出成功: {}", filePath);
            return filePath;
        }
    }
}