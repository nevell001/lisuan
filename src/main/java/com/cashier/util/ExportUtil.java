package com.cashier.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
     * 加载中文字体
     * 优先级：项目资源 > 系统字体
     */
    private static PDFont loadChineseFont(PDDocument document) throws IOException {
        // 1. 尝试从项目资源加载中文字体
        String[] resourcePaths = {
            "/fonts/NotoSansSC-Regular.ttf",
            "/com/cashier/fonts/NotoSansSC-Regular.ttf"
        };

        for (String fontPath : resourcePaths) {
            try (InputStream is = ExportUtil.class.getResourceAsStream(fontPath)) {
                if (is != null) {
                    // 检查流是否有效（至少大于 1KB）
                    byte[] buffer = new byte[1024];
                    int bytesRead = is.read(buffer);
                    if (bytesRead > 100) {
                        // 重新获取流
                        try (InputStream is2 = ExportUtil.class.getResourceAsStream(fontPath)) {
                            if (is2 != null) {
                                PDFont font = PDType0Font.load(document, is2);
                                logger.info("成功加载项目字体: {}", fontPath);
                                return font;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                logger.debug("项目字体加载失败 {}: {}", fontPath, e.getMessage());
            }
        }

        // 2. 尝试从文件系统加载
        String[] fsPaths = {
            "src/main/resources/fonts/NotoSansSC-Regular.ttf"
        };

        for (String fsPath : fsPaths) {
            File fontFile = new File(fsPath);
            if (fontFile.exists() && fontFile.length() > 1000) {
                try {
                    PDFont font = PDType0Font.load(document, fontFile);
                    logger.info("成功从文件系统加载字体: {}", fsPath);
                    return font;
                } catch (IOException e) {
                    logger.debug("文件系统字体加载失败 {}: {}", fsPath, e.getMessage());
                }
            }
        }

        // 3. 尝试加载系统字体
        String os = System.getProperty("os.name", "").toLowerCase();
        String[] systemFontPaths;

        if (os.contains("mac")) {
            // macOS 系统字体路径
            systemFontPaths = new String[]{
                "/System/Library/Fonts/PingFang.ttc",           // 苹方字体
                "/System/Library/Fonts/STHeiti Light.ttc",      // 黑体
                "/System/Library/Fonts/Hiragino Sans GB.ttc",   // 冬青黑体
                "/Library/Fonts/Arial Unicode.ttf",             // Arial Unicode
                "/System/Library/Fonts/Supplemental/Arial Unicode.ttf"
            };
        } else if (os.contains("win")) {
            // Windows 系统字体路径
            String windir = System.getenv("WINDIR");
            if (windir == null) {
                windir = "C:\\Windows";
            }
            systemFontPaths = new String[]{
                windir + "\\Fonts\\msyh.ttc",      // 微软雅黑
                windir + "\\Fonts\\simhei.ttf",    // 黑体
                windir + "\\Fonts\\simsun.ttc",    // 宋体
                windir + "\\Fonts\\simkai.ttf"     // 楷体
            };
        } else {
            // Linux 系统字体路径
            systemFontPaths = new String[]{
                "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
                "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
            };
        }

        for (String fontPath : systemFontPaths) {
            File fontFile = new File(fontPath);
            if (fontFile.exists()) {
                try {
                    PDFont font = PDType0Font.load(document, fontFile);
                    logger.info("成功加载系统字体: {}", fontPath);
                    return font;
                } catch (IOException e) {
                    logger.debug("系统字体加载失败 {}: {}", fontPath, e.getMessage());
                }
            }
        }

        logger.error("未能加载任何中文字体，PDF 导出将无法正确显示中文");
        throw new IOException("无法加载中文字体，请安装中文字体或检查字体文件");
    }

    /**
     * 导出为 PDF（支持中文，带表格边框，横版布局）
     */
    private static String exportToPDF(String title, List<String> headers, List<String[]> data,
                                      String exportPath, String fileName) throws IOException {
        String filePath = exportPath + File.separator + fileName + ".pdf";

        try (PDDocument document = new PDDocument()) {
            // 加载中文字体
            PDFont font = loadChineseFont(document);

            // PDF 页面设置 - 使用横版（交换宽度和高度）
            PDRectangle landscape = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
            PDPage page = new PDPage(landscape);
            document.addPage(page);
            
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            
            // 布局参数 - 横版优化
            float margin = 20;  // 减小页边距，最大化表格宽度
            float rowHeight = 45;  // 增加行高，让文字更清晰
            float titleFontSize = 18;
            float headerFontSize = 12;  // 增加表头字体大小
            float dataFontSize = 11;  // 增加数据字体大小
            
            // 计算列宽 - 根据内容自动调整
            int columnCount = headers.size();
            float tableWidth = pageWidth - 2 * margin;
            float[] columnWidths = calculateColumnWidths(headers, data, font, dataFontSize, tableWidth);

            // 当前 Y 位置
            float yPosition = pageHeight - margin;

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            // 写入标题
            contentStream.setFont(font, titleFontSize);
            float titleWidth = font.getStringWidth(title) / 1000 * titleFontSize;
            float titleX = (pageWidth - titleWidth) / 2;
            contentStream.beginText();
            contentStream.newLineAtOffset(titleX, yPosition);
            contentStream.showText(title);
            contentStream.endText();
            yPosition -= rowHeight * 1.5f;

            // 绘制表格
            float tableTop = yPosition;
            
            // 绘制表头背景
            contentStream.setNonStrokingColor(Color.LIGHT_GRAY);
            contentStream.addRect(margin, yPosition - rowHeight, tableWidth, rowHeight);
            contentStream.fill();
            
            // 绘制表头文字
            contentStream.setNonStrokingColor(Color.BLACK);
            contentStream.setFont(font, headerFontSize);
            float xPosition = margin + 5;
            for (int i = 0; i < headers.size(); i++) {
                contentStream.beginText();
                contentStream.newLineAtOffset(xPosition, yPosition - rowHeight + 15);
                String header = truncateText(headers.get(i), font, headerFontSize, columnWidths[i] - 5);
                contentStream.showText(header);
                contentStream.endText();
                xPosition += columnWidths[i];
            }
            yPosition -= rowHeight;

            // 绘制数据行
            contentStream.setFont(font, dataFontSize);
            for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
                // 检查是否需要新页面
                if (yPosition < margin + rowHeight) {
                    // 绘制当前页面的表格边框
                    drawTableBorder(contentStream, margin, yPosition, tableWidth, tableTop - yPosition + rowHeight, columnWidths);
                    contentStream.close();
                    
                    // 创建新页面 - 保持横版
                    PDRectangle newLandscape = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
                    page = new PDPage(newLandscape);
                    document.addPage(page);
                    yPosition = pageHeight - margin;
                    tableTop = yPosition;
                    contentStream = new PDPageContentStream(document, page);
                    contentStream.setFont(font, dataFontSize);
                }

                String[] rowData = data.get(rowIndex);
                
                // 交替行背景色
                if (rowIndex % 2 == 1) {
                    contentStream.setNonStrokingColor(new Color(245, 245, 245));
                    contentStream.addRect(margin, yPosition - rowHeight, tableWidth, rowHeight);
                    contentStream.fill();
                    contentStream.setNonStrokingColor(Color.BLACK);
                }

                // 写入数据
                xPosition = margin + 5;
                for (int i = 0; i < columnCount && i < rowData.length; i++) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(xPosition, yPosition - rowHeight + 15);
                    String cellText = rowData[i] != null ? rowData[i] : "";
                    String text = truncateText(cellText, font, dataFontSize, columnWidths[i] - 5);
                    contentStream.showText(text);
                    contentStream.endText();
                    xPosition += columnWidths[i];
                }
                yPosition -= rowHeight;
            }

            // 绘制最终表格边框
            float tableHeight = tableTop - yPosition;
            drawTableBorder(contentStream, margin, yPosition, tableWidth, tableHeight, columnWidths);

            contentStream.close();

            // 保存文件
            document.save(filePath);

            logger.info("PDF 导出成功: {}", filePath);
            return filePath;
        }
    }

    /**
     * 计算列宽 - 优化列宽分配，根据列类型设置不同最小宽度
     */
    private static float[] calculateColumnWidths(List<String> headers, List<String[]> data, 
                                                  PDFont font, float fontSize, float totalWidth) {
        int columnCount = headers.size();
        float[] maxWidths = new float[columnCount];
        
        // 根据表头名称识别列类型并设置不同的最小宽度
        float[] minWidths = new float[columnCount];
        for (int i = 0; i < columnCount; i++) {
            String header = headers.get(i).toLowerCase();
            // 时间相关列需要更宽
            if (header.contains("时间") || header.contains("时间") || 
                header.contains("time") || header.contains("date") ||
                header.contains("开始") || header.contains("结束")) {
                minWidths[i] = 130;  // 时间列最小宽度
            } 
            // 金额相关列需要适中宽度
            else if (header.contains("金额") || header.contains("收入") || 
                     header.contains("revenue") || header.contains("amount") ||
                     header.contains("¥") || header.contains("元")) {
                minWidths[i] = 90;
            }
            // 备注列需要更宽
            else if (header.contains("备注") || header.contains("说明") || 
                     header.contains("note") || header.contains("remark")) {
                minWidths[i] = 180;
            }
            // 操作员等姓名列
            else if (header.contains("操作员") || header.contains("姓名") || header.contains("name")) {
                minWidths[i] = 100;
            }
            // 默认宽度
            else {
                minWidths[i] = 80;
            }
        }
        
        try {
            // 计算每列的最大宽度
            for (int i = 0; i < columnCount; i++) {
                float headerWidth = font.getStringWidth(headers.get(i)) / 1000 * fontSize;
                maxWidths[i] = Math.max(headerWidth, minWidths[i]);
            }
            
            for (String[] row : data) {
                for (int i = 0; i < columnCount && i < row.length; i++) {
                    String cell = row[i] != null ? row[i] : "";
                    float cellWidth = font.getStringWidth(cell) / 1000 * fontSize;
                    maxWidths[i] = Math.max(maxWidths[i], cellWidth);
                    // 确保至少达到最小宽度
                    maxWidths[i] = Math.max(maxWidths[i], minWidths[i]);
                }
            }
        } catch (IOException e) {
            // 如果无法计算，使用平均宽度
            float avgWidth = totalWidth / columnCount;
            float[] widths = new float[columnCount];
            for (int i = 0; i < columnCount; i++) {
                widths[i] = Math.max(avgWidth, minWidths[i]);
            }
            return widths;
        }
        
        // 添加一些内边距
        float totalMaxWidth = 0;
        for (float w : maxWidths) {
            totalMaxWidth += w;
        }
        totalMaxWidth += columnCount * 5;
        
        float[] widths = new float[columnCount];
        
        if (totalMaxWidth <= totalWidth) {
            // 内容不多，按比例分配剩余空间
            float scale = totalWidth / totalMaxWidth;
            for (int i = 0; i < columnCount; i++) {
                widths[i] = (maxWidths[i] + 5) * scale;
                // 确保不低于最小宽度
                widths[i] = Math.max(widths[i], minWidths[i]);
            }
        } else {
            // 内容太多，按比例压缩
            float scale = totalWidth / totalMaxWidth;
            for (int i = 0; i < columnCount; i++) {
                widths[i] = (maxWidths[i] + 5) * scale;
                widths[i] = Math.max(widths[i], minWidths[i]);
            }
            
            // 如果总宽度超出，重新按比例调整
            float actualTotal = 0;
            for (float w : widths) {
                actualTotal += w;
            }
            if (actualTotal > totalWidth) {
                float adjustScale = totalWidth / actualTotal;
                for (int i = 0; i < columnCount; i++) {
                    widths[i] *= adjustScale;
                    widths[i] = Math.max(widths[i], minWidths[i] * 0.8f);  // 允许最小缩小到80%
                }
            }
        }
        
        return widths;
    }

    /**
     * 截断文本以适应列宽 - 减少省略号空间
     */
    private static String truncateText(String text, PDFont font, float fontSize, float maxWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        try {
            float textWidth = font.getStringWidth(text) / 1000 * fontSize;
            if (textWidth <= maxWidth) {
                return text;
            }
            
            // 逐个字符截断
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                String testStr = sb.toString() + text.charAt(i);
                float testWidth = font.getStringWidth(testStr) / 1000 * fontSize;
                if (testWidth > maxWidth - 10) { // 减少省略号空间从15到10
                    return sb.toString() + "...";
                }
                sb.append(text.charAt(i));
            }
            return sb.toString();
        } catch (IOException e) {
            // 无法计算宽度，简单截断
            int maxChars = (int) (maxWidth / (fontSize * 0.5));
            if (text.length() > maxChars) {
                return text.substring(0, maxChars - 3) + "...";
            }
            return text;
        }
    }

    /**
     * 绘制表格边框
     */
    private static void drawTableBorder(PDPageContentStream contentStream, float x, float y, 
                                         float width, float height, float[] columnWidths) throws IOException {
        // 绘制外边框
        contentStream.setStrokingColor(Color.BLACK);
        contentStream.setLineWidth(1);
        contentStream.addRect(x, y, width, height);
        contentStream.stroke();
        
        // 绘制垂直分隔线
        float xPos = x;
        for (int i = 0; i < columnWidths.length - 1; i++) {
            xPos += columnWidths[i];
            contentStream.moveTo(xPos, y);
            contentStream.lineTo(xPos, y + height);
            contentStream.stroke();
        }
        
        // 绘制水平分隔线（表头下方）
        float rowHeight = 25;
        contentStream.moveTo(x, y + height - rowHeight);
        contentStream.lineTo(x + width, y + height - rowHeight);
        contentStream.stroke();
    }
}
