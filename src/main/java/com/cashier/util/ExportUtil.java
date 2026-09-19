package com.cashier.util;

import com.cashier.constant.SystemPropertyKeys;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.slf4j.Logger;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 导出工具类
 * 支持导出数据为 Excel 或 PDF 格式
 */
public class ExportUtil {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ExportUtil.class);
    private static final String EXPORT_DIR = System.getProperty(SystemPropertyKeys.USER_DIR) + File.separator + "exports";

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
            // 安全校验：防止路径遍历攻击
            if (subDir == null || subDir.isEmpty()) {
                subDir = "default";
            }
            if (subDir.contains("..") || subDir.contains(File.separator) || subDir.contains("/")) {
                throw new IllegalArgumentException("非法的子目录名称: " + subDir);
            }
            // 清理 title 中的危险字符，防止文件名注入
            String safeTitle = title.replaceAll("[^A-Za-z0-9_\\-\\u4e00-\\u9fa5]", "_");

            // 确保导出目录存在
            Path exportPath = Paths.get(EXPORT_DIR, subDir).normalize();
            // 二次校验：确保路径仍在导出目录内
            if (!exportPath.startsWith(EXPORT_DIR)) {
                throw new SecurityException("路径遍历检测：目标路径超出导出目录");
            }
            if (!Files.exists(exportPath)) {
                Files.createDirectories(exportPath);
            }

            // 生成文件名
            String timestamp = LocalDateTime.now(ZoneId.systemDefault())
                .format(com.cashier.util.DateTimeFormats.BACKUP_TIMESTAMP);
            String fileName = safeTitle + "_" + timestamp;

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

            // 根据字符数设置列宽，避免 autoSizeColumn 触发 AWT 字体测量导致无图形环境崩溃
            for (int i = 0; i < headers.size(); i++) {
                sheet.setColumnWidth(i, calculateExcelColumnWidth(headers, data, i));
            }

            // 保存文件
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }

            logger.info("Excel 导出成功: {}", filePath);
            return filePath;
        }
    }

    private static int calculateExcelColumnWidth(List<String> headers, List<String[]> data, int columnIndex) {
        int maxChars = getDisplayWidth(headers.get(columnIndex));
        for (String[] row : data) {
            if (row != null && columnIndex < row.length) {
                maxChars = Math.max(maxChars, getDisplayWidth(row[columnIndex]));
            }
        }

        int paddedChars = Math.min(Math.max(maxChars + 2, 10), 60);
        return paddedChars * 256;
    }

    private static int getDisplayWidth(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }

        int width = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            width += ch <= 0x7F ? 1 : 2;
        }
        return width;
    }

    /**
     * 加载中文字体
     * 优先级：系统字体 > 文件系统 > 项目资源
     * 注意：TTC 文件必须使用 File 对象加载，不能使用 InputStream
     */
    private static PDFont loadChineseFont(PDDocument document) throws IOException {
        // 1. 优先尝试系统字体（TTC 文件使用 File 对象加载）
        String os = System.getProperty(SystemPropertyKeys.OS_NAME, "").toLowerCase();
        String[] systemFontPaths;

        if (os.contains("mac")) {
            // macOS 系统字体路径
            systemFontPaths = new String[]{
                "/Library/Fonts/Arial Unicode.ttf",             // Arial Unicode
                "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
                "/System/Library/Fonts/PingFang.ttc",           // 苹方字体
                "/System/Library/Fonts/STHeiti Light.ttc",      // 黑体
                "/System/Library/Fonts/Hiragino Sans GB.ttc"    // 冬青黑体
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
            // Linux 系统字体路径：必须是 PDFBox 能嵌入的 TrueType/glyf 字体，且必须覆盖数字与中文
            // （OTF/CFF 会被 isEmbeddable 跳过；Droid Sans Fallback 只有 CJK 字形、没有数字，
            //   会被 canRender 跳过，否则渲染金额/数量时会抛 No glyph for U+0031）
            systemFontPaths = new String[]{
                "/usr/share/fonts/truetype/arphic/uming.ttc",               // AR PL UMing（TrueType，含拉丁+中文）
                "/usr/share/fonts/truetype/arphic-gkai00mp/gkai00mp.ttf",   // AR PL 楷体（TrueType）
                "/usr/share/fonts/truetype/fonts-ukij-uyghur/UKIJCJK.ttf",  // UKIJ CJK (支持中文和英文)
                "/usr/share/fonts/truetype/lxgw-wenkai/LXGWWenKai-Regular.ttf",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"    // OTF/CFF，会被跳过
            };
        }

        for (String fontPath : systemFontPaths) {
            File fontFile = new File(fontPath);
            if (fontFile.exists()) {
                try {
                    PDFont font = loadFont(document, fontFile);
                    if (!canRender(font)) {
                        logger.debug("系统字体缺少数字/中文覆盖，跳过: {}", fontPath);
                        continue;
                    }
                    logger.info("成功加载系统字体: {}", fontPath);
                    return font;
                } catch (IOException e) {
                    logger.debug("系统字体加载失败 {}: {}", fontPath, e.getMessage());
                }
            }
        }

        // 2. 尝试从文件系统加载
        String[] fsPaths = {
            "src/main/resources/fonts/NotoSansSC-Regular.ttc",
            "src/main/resources/fonts/NotoSansSC-Regular.ttf"
        };

        for (String fsPath : fsPaths) {
            File fontFile = new File(fsPath);
            if (fontFile.exists() && fontFile.length() > 1000) {
                try {
                    PDFont font = loadFont(document, fontFile);
                    if (!canRender(font)) {
                        logger.debug("文件系统字体缺少数字/中文覆盖，跳过: {}", fsPath);
                        continue;
                    }
                    logger.info("成功从文件系统加载字体: {}", fsPath);
                    return font;
                } catch (IOException e) {
                    logger.debug("文件系统字体加载失败 {}: {}", fsPath, e.getMessage());
                }
            }
        }

        // 3. 尝试从项目资源加载（同时支持 TTF/OTF 与 TTC）
        String[] resourcePaths = {
            "/fonts/NotoSansSC-Regular.ttc",
            "/fonts/NotoSansSC-Regular.ttf",
            "/fonts/NotoSansSC-Regular.otf",
            "/com/cashier/fonts/NotoSansSC-Regular.ttc",
            "/com/cashier/fonts/NotoSansSC-Regular.ttf",
            "/com/cashier/fonts/NotoSansSC-Regular.otf"
        };

        for (String fontPath : resourcePaths) {
            try (InputStream is = ExportUtil.class.getResourceAsStream(fontPath)) {
                if (is != null && is.available() > 1000) {
                    PDFont font = loadFont(document, is, fontPath.endsWith(".ttc"));
                    if (!canRender(font)) {
                        logger.debug("项目字体缺少数字/中文覆盖，跳过: {}", fontPath);
                        continue;
                    }
                    logger.info("成功加载项目字体: {}", fontPath);
                    return font;
                }
            } catch (IOException e) {
                logger.debug("项目字体加载失败 {}: {}", fontPath, e.getMessage());
            }
        }

        logger.error("未能加载任何中文字体，PDF 导出将无法正确显示中文");
        throw new IOException("无法加载中文字体，请安装中文字体或检查字体文件");
    }

    /** 字体必须覆盖报表里必然出现的字符：数字、常见符号与中文 */
    private static final String REQUIRED_SAMPLE = "0123456789%.-: 中";

    /**
     * 字体是否覆盖报表必需字符。
     *
     * <p>只校验可嵌入还不够：Droid Sans Fallback 这类只有 CJK 字形的字体能嵌入，
     * 但渲染金额/数量时会抛 {@code IllegalArgumentException: No glyph for U+0031}。</p>
     */
    private static boolean canRender(PDFont font) {
        try {
            font.getStringWidth(REQUIRED_SAMPLE);
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /**
     * 加载字体文件（支持 .ttf/.otf 与 .ttc）
     * .ttc 为字体集合，需通过 TrueTypeCollection 解包后选择简体中文子字体嵌入
     */
    private static PDFont loadFont(PDDocument document, File fontFile) throws IOException {
        String name = fontFile.getName().toLowerCase();
        if (!name.endsWith(".ttc")) {
            return PDType0Font.load(document, fontFile);
        }
        try (TrueTypeCollection collection = new TrueTypeCollection(fontFile)) {
            return loadFromCollection(document, collection);
        }
    }

    /**
     * 从资源流加载字体（TTC 需解包）
     */
    private static PDFont loadFont(PDDocument document, InputStream is, boolean isTtc) throws IOException {
        if (!isTtc) {
            return PDType0Font.load(document, is);
        }
        try (TrueTypeCollection collection = new TrueTypeCollection(is)) {
            return loadFromCollection(document, collection);
        }
    }

    /**
     * 从 TrueTypeCollection 中选择简体中文子字体并嵌入
     *
     * <p>两点约束：</p>
     * <ul>
     *   <li>必须使用子集嵌入（{@code embedSubset=true}）：PDFBox 不支持字体集合的整体嵌入，
     *       {@code false} 会抛 {@code IOException: Full embedding of TrueType font collections not supported}。</li>
     *   <li>必须跳过没有 {@code glyf} 表的子字体：NotoSansCJK 这类 OTF/CFF 字体在
     *       {@code PDDocument.save()} 子集化时会抛 {@code UnsupportedOperationException}，
     *       加载阶段看似成功、保存阶段才失败，跳过才能继续尝试下一个可用字体。</li>
     * </ul>
     */
    private static PDFont loadFromCollection(PDDocument document, TrueTypeCollection collection)
            throws IOException {
        // 优先按简体中文字体名查找
        String[][] candidates = {
            {"NotoSansCJKsc-Regular", "Noto Sans CJK SC", "NotoSansSC-Regular", "Noto Sans SC"},
            {"MicrosoftYaHei", "Microsoft YaHei", "微软雅黑"},
            {"SimSun", "宋体"}
        };
        for (String[] names : candidates) {
            for (String name : names) {
                try {
                    TrueTypeFont ttf = collection.getFontByName(name);
                    if (ttf != null && isEmbeddable(ttf)) {
                        return PDType0Font.load(document, ttf, true);
                    }
                } catch (IOException e) {
                    logger.debug("TTC 子字体 {} 加载失败: {}", name, e.getMessage());
                }
            }
        }
        // 兜底：嵌入第一个可嵌入的子字体
        final PDFont[] result = new PDFont[1];
        collection.processAllFonts(ttf -> {
            if (result[0] != null || !isEmbeddable(ttf)) {
                return;
            }
            try {
                result[0] = PDType0Font.load(document, ttf, true);
            } catch (IOException e) {
                logger.debug("TTC 兜底字体加载失败 {}: {}", ttf.getName(), e.getMessage());
            }
        });
        if (result[0] != null) {
            return result[0];
        }
        throw new IOException("TTC 字体集合中无可嵌入的子字体");
    }

    /**
     * 字体是否可被 PDFBox 嵌入。
     *
     * <p>OTF/CFF 字体没有 {@code glyf} 表，子集化时会失败，必须排除。</p>
     */
    private static boolean isEmbeddable(TrueTypeFont ttf) {
        try {
            return ttf.getGlyph() != null;
        } catch (IOException | RuntimeException e) {
            // ttf.getName() 同样会抛 IOException，这里只用异常信息记录
            logger.debug("字体不可嵌入（无 glyf 表）: {}", e.getMessage());
            return false;
        }
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
            float margin = 12;  // 进一步减小页边距，最大化表格宽度
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

            // H-25: 使用 try-finally 确保 PDPageContentStream 在异常路径下也被关闭
            PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);
            try {
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
            
            // 绘制表头
            drawTableHeader(contentStream, font, headerFontSize, margin, yPosition, rowHeight, tableWidth, headers, columnWidths);
            yPosition -= rowHeight;

            // 绘制数据行
            contentStream.setFont(font, dataFontSize);
            
            for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
                String[] rowData = data.get(rowIndex);
                
                // 计算每一列需要的行数
                int[] lineCounts = new int[columnCount];
                int maxLines = 1;
                for (int i = 0; i < columnCount && i < rowData.length; i++) {
                    String cellText = rowData[i] != null ? rowData[i] : "";
                    lineCounts[i] = calculateLineCount(cellText, font, dataFontSize, columnWidths[i] - 3);
                    maxLines = Math.max(maxLines, lineCounts[i]);
                }
                
                // 计算实际需要的行高（根据最大行数）
                float actualRowHeight = rowHeight * maxLines;
                float rowTopY = yPosition;
                float rowBottomY = yPosition - actualRowHeight;
                
                // 检查是否需要新页面
                if (yPosition < margin + actualRowHeight) {
                    // 关闭旧页面的 contentStream
                    try {
                        contentStream.close();
                    } catch (IOException e) {
                        logger.warn("关闭页面内容流失败: " + e.getMessage());
                    }
                    contentStream = null;
                    
                    // 创建新页面 - 保持横版
                    PDRectangle newLandscape = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
                    page = new PDPage(newLandscape);
                    document.addPage(page);
                    yPosition = pageHeight - margin;
                    contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);
                    contentStream.setFont(font, dataFontSize);
                    // 绘制表头
                    drawTableHeader(contentStream, font, headerFontSize, margin, yPosition, rowHeight, tableWidth, headers, columnWidths);
                    yPosition -= rowHeight;
                    rowTopY = yPosition;
                    rowBottomY = yPosition - actualRowHeight;
                }

                // 交替行背景色
                if (rowIndex % 2 == 1) {
                    contentStream.setNonStrokingColor(new Color(245, 245, 245));
                    contentStream.addRect(margin, rowBottomY, tableWidth, actualRowHeight);
                    contentStream.fill();
                    contentStream.setNonStrokingColor(Color.BLACK);
                }

                // 绘制行的边框
                contentStream.setStrokingColor(Color.BLACK);
                contentStream.setLineWidth(1);
                
                // 绘制外边框矩形（完整的边框，包括顶部、底部和左右）
                contentStream.addRect(margin, rowBottomY, tableWidth, actualRowHeight);
                contentStream.stroke();
                
                // 绘制垂直分隔线
                float xPos = margin;
                for (int i = 0; i < columnWidths.length - 1; i++) {
                    xPos += columnWidths[i];
                    contentStream.moveTo(xPos, rowBottomY);
                    contentStream.lineTo(xPos, rowTopY);
                    contentStream.stroke();
                }

                // 写入数据（支持多行）
                float xPosition = margin + 3;
                for (int i = 0; i < columnCount && i < rowData.length; i++) {
                    String cellText = rowData[i] != null ? rowData[i] : "";
                    String[] lines = splitTextIntoLines(cellText, font, dataFontSize, columnWidths[i] - 3);
                    
                    // 垂直居中绘制多行文本，第一行在上面
                    float lineSpacing = (actualRowHeight / lines.length);
                    float startY = rowTopY - lineSpacing / 2; // 从顶部开始，第一行在上
                    
                    for (int lineIdx = 0; lineIdx < lines.length; lineIdx++) {
                        contentStream.beginText();
                        contentStream.newLineAtOffset(xPosition, startY - lineIdx * lineSpacing);
                        contentStream.showText(lines[lineIdx]);
                        contentStream.endText();
                    }
                    xPosition += columnWidths[i];
                }
                yPosition -= actualRowHeight;
            }

            // H-25: 正常路径下先关闭 contentStream，再保存文档
            // PDFBox 要求所有 contentStream 在 document.save() 前关闭
            if (contentStream != null) {
                try {
                    contentStream.close();
                } catch (IOException e) {
                    logger.warn("关闭页面内容流失败", e);
                }
                contentStream = null;
            }

            // 保存文件
            document.save(filePath);

            logger.info("PDF 导出成功: {}", filePath);
            return filePath;
            } finally {
                // H-25: 异常路径下确保 contentStream 也被关闭
                if (contentStream != null) {
                    try {
                        contentStream.close();
                    } catch (IOException e) {
                        logger.warn("关闭页面内容流失败", e);
                    }
                }
            }
        }
    }

    /**
     * 计算列宽 - 优化列宽分配，根据列类型设置不同最小宽度
     */
    private static float[] calculateColumnWidths(List<String> headers, List<String[]> data, 
                                                  PDFont font, float fontSize, float totalWidth) {
        int columnCount = headers.size();
        ColumnLayout layout = computeColumnLayout(headers);
        float[] minWidths = layout.minWidths;
        
        try {
            float[] maxWidths = computeMaxWidths(headers, data, font, fontSize, minWidths, layout.isDateTimeColumn);
            return allocateColumnWidths(maxWidths, minWidths, totalWidth);
        } catch (IOException e) {
            // 如果无法计算，使用平均宽度
            float avgWidth = totalWidth / columnCount;
            float[] widths = new float[columnCount];
            for (int i = 0; i < columnCount; i++) {
                widths[i] = Math.max(avgWidth, minWidths[i]);
            }
            return widths;
        }
    }

    /** 列布局：最小宽度 + 时间列标记 */
    private record ColumnLayout(float[] minWidths, boolean[] isDateTimeColumn) {
    }

    /** 根据表头名称识别列类型并设置不同的最小宽度 */
    private static ColumnLayout computeColumnLayout(List<String> headers) {
        int columnCount = headers.size();
        float[] minWidths = new float[columnCount];
        boolean[] isDateTimeColumn = new boolean[columnCount];
        for (int i = 0; i < columnCount; i++) {
            String header = headers.get(i).toLowerCase();
            if (header.contains("时间") || header.contains("time") || header.contains("date") ||
                header.contains("开始") || header.contains("结束")) {
                minWidths[i] = 115;
                isDateTimeColumn[i] = true;
            } else if (header.contains("金额") || header.contains("收入") ||
                header.contains("revenue") || header.contains("amount") ||
                header.contains("元") || header.contains("¥") ||
                header.contains("$") || header.contains("₩")) {
                minWidths[i] = 65;
            } else if (header.contains("备注") || header.contains("说明") ||
                header.contains("note") || header.contains("remark")) {
                minWidths[i] = 65;
            } else if (header.contains("操作员") || header.contains("姓名") || header.contains("name")) {
                minWidths[i] = 55;
            } else if (header.contains("时长") || header.contains("duration")) {
                minWidths[i] = 55;
            } else if (header.contains("数量") || header.contains("count")) {
                minWidths[i] = 40;
            } else if (header.contains("编号") || header.contains("id") || header.contains("no")) {
                minWidths[i] = 85;
            } else {
                minWidths[i] = 65;
            }
        }
        return new ColumnLayout(minWidths, isDateTimeColumn);
    }

    /** 计算每列内容最大宽度（时间列按两行估算） */
    private static float[] computeMaxWidths(List<String> headers, List<String[]> data,
                                            PDFont font, float fontSize,
                                            float[] minWidths, boolean[] isDateTimeColumn) throws IOException {
        int columnCount = headers.size();
        float[] maxWidths = new float[columnCount];
        for (int i = 0; i < columnCount; i++) {
            maxWidths[i] = Math.max(font.getStringWidth(headers.get(i)) / 1000 * fontSize, minWidths[i]);
        }
        for (String[] row : data) {
            for (int i = 0; i < columnCount && i < row.length; i++) {
                String cell = row[i] != null ? row[i] : "";
                if (isDateTimeColumn[i] && isDateTimeFormat(cell)) {
                    float maxPartWidth = 0;
                    for (String part : splitDateTimeToLines(cell)) {
                        maxPartWidth = Math.max(maxPartWidth, font.getStringWidth(part) / 1000 * fontSize);
                    }
                    maxWidths[i] = Math.max(maxWidths[i], maxPartWidth) + 5;
                } else {
                    maxWidths[i] = Math.max(maxWidths[i], font.getStringWidth(cell) / 1000 * fontSize);
                }
                maxWidths[i] = Math.max(maxWidths[i], minWidths[i]);
            }
        }
        return maxWidths;
    }

    /** 按最小/最大宽度与可用总宽度分配列宽 */
    private static float[] allocateColumnWidths(float[] maxWidths, float[] minWidths, float totalWidth) {
        int columnCount = maxWidths.length;
        float totalMaxWidth = 0;
        for (float w : maxWidths) {
            totalMaxWidth += w;
        }
        totalMaxWidth += columnCount * 3;

        float[] widths = new float[columnCount];
        if (totalMaxWidth <= totalWidth) {
            float scale = totalWidth / totalMaxWidth;
            for (int i = 0; i < columnCount; i++) {
                widths[i] = Math.max((maxWidths[i] + 3) * scale, minWidths[i]);
            }
        } else {
            float totalMinWidth = 0;
            for (float mw : minWidths) {
                totalMinWidth += mw;
            }
            if (totalMinWidth > totalWidth) {
                float scale = totalWidth / totalMinWidth;
                for (int i = 0; i < columnCount; i++) {
                    widths[i] = minWidths[i] * scale;
                }
            } else {
                float remainingWidth = totalWidth - totalMinWidth;
                float extraWidth = 0;
                for (int i = 0; i < columnCount; i++) {
                    widths[i] = minWidths[i];
                    extraWidth += (maxWidths[i] - minWidths[i]);
                }
                if (extraWidth > 0) {
                    float scale = remainingWidth / extraWidth;
                    for (int i = 0; i < columnCount; i++) {
                        widths[i] += (maxWidths[i] - minWidths[i]) * scale;
                    }
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
                if (testWidth > maxWidth - 5) { // 减少省略号空间到5
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
     * 绘制表头
     */
    private static void drawTableHeader(PDPageContentStream contentStream, PDFont font, float headerFontSize,
                                         float margin, float yPosition, float rowHeight, float tableWidth,
                                         List<String> headers, float[] columnWidths) throws IOException {
        // 绘制表头背景
        contentStream.setNonStrokingColor(Color.LIGHT_GRAY);
        contentStream.addRect(margin, yPosition - rowHeight, tableWidth, rowHeight);
        contentStream.fill();
        
        // 绘制表头边框（包括竖线）
        contentStream.setStrokingColor(Color.BLACK);
        contentStream.setLineWidth(1);
        // 绘制表头矩形边框
        contentStream.addRect(margin, yPosition - rowHeight, tableWidth, rowHeight);
        contentStream.stroke();
        
        // 绘制表头的竖线分隔
        float xPos = margin;
        for (int i = 0; i < columnWidths.length - 1; i++) {
            xPos += columnWidths[i];
            contentStream.moveTo(xPos, yPosition - rowHeight);
            contentStream.lineTo(xPos, yPosition);
            contentStream.stroke();
        }
        
        // 绘制表头文字
        contentStream.setNonStrokingColor(Color.BLACK);
        contentStream.setFont(font, headerFontSize);
        float xPosition = margin + 3;
        for (int i = 0; i < headers.size(); i++) {
            contentStream.beginText();
            contentStream.newLineAtOffset(xPosition, yPosition - rowHeight + 15);
            String header = truncateText(headers.get(i), font, headerFontSize, columnWidths[i] - 3);
            contentStream.showText(header);
            contentStream.endText();
            xPosition += columnWidths[i];
        }
    }

    /**
     * 绘制数据行边框（不包括表头）
     */
    /**
     * 计算文本需要的行数（逐个字符测试宽度）
     */
    private static int calculateLineCount(String text, PDFont font, float fontSize, float maxWidth) {
        if (text == null || text.isEmpty()) {
            return 1;
        }
        
        // 如果是日期时间格式，返回 2 行（日期和时间）
        if (isDateTimeFormat(text)) {
            return 2;
        }
        
        try {
            float textWidth = font.getStringWidth(text) / 1000 * fontSize;
            if (textWidth <= maxWidth) {
                return 1;
            }
            
            // 逐个字符测试，计算实际需要的行数
            int lineCount = 1;
            float currentWidth = 0;
            
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                String cStr = String.valueOf(c);
                float charWidth = font.getStringWidth(cStr) / 1000 * fontSize;
                
                if (currentWidth + charWidth > maxWidth) {
                    lineCount++;
                    currentWidth = charWidth;
                } else {
                    currentWidth += charWidth;
                }
            }
            
            return lineCount;
        } catch (IOException e) {
            return 1;
        }
    }

    /**
     * 检测是否是日期时间格式（如：2026-02-23 22:13:43）
     */
    private static boolean isDateTimeFormat(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        // 匹配格式：YYYY-MM-DD HH:MM:SS 或类似格式
        return text.matches("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}\\s+\\d{1,2}:\\d{2}(:\\d{2})?");
    }

    /**
     * 将日期时间格式分割成两行（日期和时间）
     */
    private static String[] splitDateTimeToLines(String text) {
        if (text == null || text.isEmpty()) {
            return new String[]{""};
        }

        // 按空格分割日期和时间
        String[] parts = text.split("\\s+", 2);
        if (parts.length == 2) {
            return new String[]{parts[0], parts[1]};
        }
        return new String[]{text};
    }

    /**
     * 将文本分割成多行（逐个字符测试宽度）
     * 支持日期时间格式自动分行显示
     */
    private static String[] splitTextIntoLines(String text, PDFont font, float fontSize, float maxWidth) {
        if (text == null || text.isEmpty()) {
            return new String[]{""};
        }

        // 如果是日期时间格式，总是优先按日期和时间分行
        if (isDateTimeFormat(text)) {
            String[] dateTimeLines = splitDateTimeToLines(text);
            // 检查两行是否都适合宽度
            try {
                boolean bothFit = true;
                for (String line : dateTimeLines) {
                    float lineWidth = font.getStringWidth(line) / 1000 * fontSize;
                    if (lineWidth > maxWidth) {
                        bothFit = false;
                        break;
                    }
                }
                // 如果两行都适合，直接返回分行结果
                if (bothFit) {
                    return dateTimeLines;
                }
                // 如果不适合，仍然按日期时间分行，但可能超出单元格宽度
                // 这是可接受的，因为优先保证日期时间的可读性
                return dateTimeLines;
            } catch (IOException e) {
                // 忽略异常，返回日期时间分行结果
                return splitDateTimeToLines(text);
            }
        }
        
        try {
            float textWidth = font.getStringWidth(text) / 1000 * fontSize;
            if (textWidth <= maxWidth) {
                return new String[]{text};
            }
            
            // 逐个字符测试，按实际宽度分割
            List<String> lines = new ArrayList<>();
            StringBuilder currentLine = new StringBuilder();
            float currentWidth = 0;
            
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                String cStr = String.valueOf(c);
                float charWidth = font.getStringWidth(cStr) / 1000 * fontSize;
                
                // 如果加上这个字符超过最大宽度，则开始新的一行
                if (currentWidth + charWidth > maxWidth) {
                    if (currentLine.length() > 0) {
                        lines.add(currentLine.toString());
                        currentLine = new StringBuilder();
                        currentWidth = 0;
                    }
                    // 单个字符就超过宽度，只能添加它
                    currentLine.append(c);
                    currentWidth = charWidth;
                } else {
                    currentLine.append(c);
                    currentWidth += charWidth;
                }
            }
            
            // 添加最后一行
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
            
            return lines.toArray(new String[0]);
        } catch (IOException e) {
            return new String[]{text};
        }
    }
}
