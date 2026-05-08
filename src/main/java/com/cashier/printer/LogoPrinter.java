package com.cashier.printer;

import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Logo 打印工具类
 * 支持将图片转换为 ESC/POS 位图打印指令
 */
public class LogoPrinter {

    private static final Logger logger = LoggerFactoryUtil.getLogger(LogoPrinter.class);

    // 默认配置
    private static final int MAX_LOGO_WIDTH = 384;  // 58mm 纸张推荐宽度
    private static final int MAX_LOGO_WIDTH_80MM = 576;  // 80mm 纸张推荐宽度

    /**
     * 从文件加载 Logo 并转换为 ESC/POS 指令
     * @param imagePath 图片文件路径
     * @param paperWidthMM 纸张宽度（mm），58 或 80
     * @return ESC/POS 指令字节数组
     */
    public static byte[] loadLogoFromFile(String imagePath, int paperWidthMM) {
        try {
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                logger.warn("Logo 文件不存在: {}", imagePath);
                return null;
            }

            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                logger.warn("无法读取 Logo 图片: {}", imagePath);
                return null;
            }

            int maxWidth = (paperWidthMM >= 80) ? MAX_LOGO_WIDTH_80MM : MAX_LOGO_WIDTH;
            return convertImageToEscPos(image, maxWidth);

        } catch (IOException e) {
            logger.error("加载 Logo 失败: {} - {}", imagePath, e.getMessage());
            return null;
        }
    }

    /**
     * 从资源目录加载 Logo
     * @param resourcePath 资源路径（如 "images/logo.png"）
     * @param paperWidthMM 纸张宽度
     * @return ESC/POS 指令字节数组
     */
    public static byte[] loadLogoFromResource(String resourcePath, int paperWidthMM) {
        InputStream is = LogoPrinter.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            logger.warn("Logo 资源不存在: {}", resourcePath);
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(is);
            int maxWidth = (paperWidthMM >= 80) ? MAX_LOGO_WIDTH_80MM : MAX_LOGO_WIDTH;
            return convertImageToEscPos(image, maxWidth);
        } catch (IOException e) {
            logger.error("加载 Logo 资源失败: {} - {}", resourcePath, e.getMessage());
            return null;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // 忽略关闭异常
            }
        }
    }

    /**
     * 将 BufferedImage 转换为 ESC/POS 位图打印指令
     * @param image 源图像
     * @param maxWidth 最大宽度（像素）
     * @return ESC/POS 指令字节数组
     */
    public static byte[] convertImageToEscPos(BufferedImage image, int maxWidth) {
        if (image == null) {
            return null;
        }

        try {
            // 1. 调整图像大小（保持宽高比）
            BufferedImage resizedImage = resizeImage(image, maxWidth);

            // 2. 转换为灰度图
            BufferedImage grayImage = convertToGrayscale(resizedImage);

            // 3. 转换为单色位图（1 bit per pixel）
            boolean[] bitmap = convertToMonochrome(grayImage);

            // 4. 生成 ESC/POS 指令
            return generateEscPosBitmapCommand(bitmap, resizedImage.getWidth(), resizedImage.getHeight());

        } catch (Exception e) {
            logger.error("转换 Logo 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 调整图像大小
     */
    private static BufferedImage resizeImage(BufferedImage original, int maxWidth) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        if (originalWidth <= maxWidth) {
            return original;
        }

        // 计算新高度（保持宽高比）
        int newHeight = (int) ((double) originalHeight * maxWidth / originalWidth);

        // 创建调整后的图像
        BufferedImage resized = new BufferedImage(maxWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, maxWidth, newHeight, null);
        g.dispose();

        logger.debug("Logo 缩放: {}x{} -> {}x{}", originalWidth, originalHeight, maxWidth, newHeight);
        return resized;
    }

    /**
     * 转换为灰度图
     */
    private static BufferedImage convertToGrayscale(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        Graphics2D g = grayImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return grayImage;
    }

    /**
     * 转换为单色位图（使用抖动算法）
     */
    private static boolean[] convertToMonochrome(BufferedImage grayImage) {
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();
        boolean[] bitmap = new boolean[width * height];

        // 获取灰度值数组
        int[] pixels = grayImage.getRGB(0, 0, width, height, null, 0, width);

        // 应用 Floyd-Steinberg 抖动算法
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int gray = getGrayValue(pixels[index]);

                // 阈值判断
                boolean pixel = gray < 128;
                bitmap[index] = pixel;
                int error = gray - (pixel ? 0 : 255);

                // 扩散误差到相邻像素
                if (x + 1 < width) {
                    pixels[index + 1] = adjustGrayValue(pixels[index + 1], error * 7 / 16);
                }
                if (x > 0 && y + 1 < height) {
                    pixels[index + width - 1] = adjustGrayValue(pixels[index + width - 1], error * 3 / 16);
                }
                if (y + 1 < height) {
                    pixels[index + width] = adjustGrayValue(pixels[index + width], error * 5 / 16);
                }
                if (x + 1 < width && y + 1 < height) {
                    pixels[index + width + 1] = adjustGrayValue(pixels[index + width + 1], error * 1 / 16);
                }
            }
        }

        return bitmap;
    }

    /**
     * 获取灰度值
     */
    private static int getGrayValue(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (r * 77 + g * 150 + b * 29) >> 8;  // ITU-R BT.601 系数
    }

    /**
     * 调整灰度值（带溢出保护）
     */
    private static int adjustGrayValue(int rgb, int delta) {
        int gray = getGrayValue(rgb);
        gray = Math.max(0, Math.min(255, gray + delta));
        return (gray << 16) | (gray << 8) | gray | 0xFF000000;
    }

    /**
     * 生成 ESC/POS 位图打印指令
     * 使用 GS v 0 命令格式
     */
    private static byte[] generateEscPosBitmapCommand(boolean[] bitmap, int width, int height) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 计算字节数（每行 8 像素 = 1 字节）
        int bytesPerLine = (width + 7) / 8;
        int totalBytes = bytesPerLine * height;

        // GS v 0 命令格式
        // 1D 76 30 m xL xH yL yH d1...dk
        // m = 模式 (0x00 = 正常)
        // xL xH = 水平点数 (低字节，高字节)
        // yL yH = 垂直点数 (低字节，高字节)

        baos.write(0x1D);  // GS
        baos.write(0x76);  // v
        baos.write(0x30);  // 0
        baos.write(0x00);  // m = 0 (正常模式)
        baos.write(width & 0xFF);        // xL
        baos.write((width >> 8) & 0xFF); // xH
        baos.write(height & 0xFF);        // yL
        baos.write((height >> 8) & 0xFF); // yH

        // 写入位图数据
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < bytesPerLine; x++) {
                byte b = 0;
                for (int bit = 0; bit < 8; bit++) {
                    int pixelIndex = y * width + x * 8 + bit;
                    if (pixelIndex < bitmap.length && bitmap[pixelIndex]) {
                        b |= (1 << (7 - bit));
                    }
                }
                baos.write(b);
            }
        }

        logger.debug("生成 ESC/POS 位图指令: {}x{} = {} 字节", width, height, totalBytes);
        return baos.toByteArray();
    }

    /**
     * 创建文本 Logo（用于无图片时的备用方案）
     * @param text Logo 文本
     * @return ESC/POS 指令字节数组
     */
    public static byte[] createTextLogo(String text) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // 居中对齐
            baos.write(EscPosUtils.ALIGN_CENTER);

            // 放大字体（双倍宽高）
            baos.write(EscPosUtils.DOUBLE_HEIGHT_WIDTH_ON);

            // 写入文本
            baos.write(text.getBytes("GBK"));

            // 恢复正常字体和左对齐
            baos.write(EscPosUtils.FONT_NORMAL);
            baos.write(EscPosUtils.ALIGN_LEFT);

            // 换行
            baos.write(EscPosUtils.LINE_FEED);
            baos.write(EscPosUtils.LINE_FEED);

            return baos.toByteArray();

        } catch (IOException e) {
            logger.error("创建文本 Logo 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 生成打印 Logo 的完整指令（包含对齐和换行）
     * @param logoData Logo 位图数据
     * @param centered 是否居中
     * @return 完整的打印指令
     */
    public static byte[] generateLogoPrintCommand(byte[] logoData, boolean centered) {
        if (logoData == null || logoData.length == 0) {
            return new byte[0];
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // 居中对齐（如果需要）
            if (centered) {
                baos.write(EscPosUtils.ALIGN_CENTER);
            }

            // 发送位图数据
            baos.write(logoData);

            // 换行
            baos.write(EscPosUtils.LINE_FEED);
            baos.write(EscPosUtils.LINE_FEED);

            // 恢复左对齐
            if (centered) {
                baos.write(EscPosUtils.ALIGN_LEFT);
            }

            return baos.toByteArray();

        } catch (IOException e) {
            logger.error("生成 Logo 打印指令失败: {}", e.getMessage());
            return new byte[0];
        }
    }
}
