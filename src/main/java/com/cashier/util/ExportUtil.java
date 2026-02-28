package com.cashier.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * 导出为 PDF（支持中文）
     */
    private static String exportToPDF(String title, List<String> headers, List<String[]> data,
                                      String exportPath, String fileName) throws IOException {
        String filePath = exportPath + File.separator + fileName + ".pdf";

        try (PDDocument document = new PDDocument()) {
            // 加载中文字体
            PDFont font = loadChineseFont(document);

            PDPage page = new PDPage();
            document.addPage(page);

            float margin = 50;
            float yPosition = 750;
            float lineHeight = 20;
            float titleFontSize = 16;
            float normalFontSize = 10;
            float columnWidth = 120;

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // 写入标题
                contentStream.setFont(font, titleFontSize);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(title);
                contentStream.endText();

                yPosition -= 30;

                // 计算列宽
                int columnCount = headers.size();
                float totalWidth = page.getMediaBox().getWidth() - 2 * margin;
                columnWidth = totalWidth / Math.max(columnCount, 1);

                // 写入表头
                contentStream.setFont(font, normalFontSize);
                float xPosition = margin;
                for (String header : headers) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(xPosition, yPosition);
                    contentStream.showText(header != null ? header : "");
                    contentStream.endText();
                    xPosition += columnWidth;
                }

                yPosition -= lineHeight;

                // 写入数据
                for (String[] rowData : data) {
                    if (yPosition < margin + lineHeight) {
                        // 页面已满，创建新页
                        contentStream.close();

                        page = new PDPage();
                        document.addPage(page);
                        yPosition = 750;

                        // 需要重新创建 contentStream
                        PDPageContentStream newStream = new PDPageContentStream(document, page);
                        newStream.setFont(font, normalFontSize);

                        // 继续写入数据
                        xPosition = margin;
                        for (int i = 0; i < rowData.length; i++) {
                            String cell = rowData[i];
                            newStream.beginText();
                            newStream.newLineAtOffset(xPosition, yPosition);
                            String text = cell != null && cell.length() > 30 ? cell.substring(0, 27) + "..." : cell;
                            newStream.showText(text != null ? text : "");
                            newStream.endText();
                            xPosition += columnWidth;
                        }
                        yPosition -= lineHeight;
                        newStream.close();
                        continue;
                    }

                    xPosition = margin;
                    for (String cell : rowData) {
                        contentStream.beginText();
                        contentStream.newLineAtOffset(xPosition, yPosition);
                        // 截断过长的文字
                        String text = cell != null && cell.length() > 30 ? cell.substring(0, 27) + "..." : cell;
                        contentStream.showText(text != null ? text : "");
                        contentStream.endText();
                        xPosition += columnWidth;
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