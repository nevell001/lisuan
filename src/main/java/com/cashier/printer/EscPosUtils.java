package com.cashier.printer;

/**
 * ESC/POS 打印指令工具类
 * 热敏打印机标准控制命令
 */
public class EscPosUtils {
    
    // ========== 基础指令 ==========
    
    /**
     * 初始化打印机
     * ESC @
     */
    public static final byte[] INIT = {0x1B, 0x40};
    
    /**
     * 换行
     * LF
     */
    public static final byte[] LINE_FEED = {0x0A};
    
    /**
     * 回车
     * CR
     */
    public static final byte[] CR = {0x0D};
    
    // ========== 对齐指令 ==========
    
    /**
     * 左对齐
     * ESC a 0
     */
    public static final byte[] ALIGN_LEFT = {0x1B, 0x61, 0x00};
    
    /**
     * 居中对齐
     * ESC a 1
     */
    public static final byte[] ALIGN_CENTER = {0x1B, 0x61, 0x01};
    
    /**
     * 右对齐
     * ESC a 2
     */
    public static final byte[] ALIGN_RIGHT = {0x1B, 0x61, 0x02};
    
    // ========== 字体指令 ==========
    
    /**
     * 选择字体A（标准字体）
     * ESC ! 0
     */
    public static final byte[] FONT_A = {0x1B, 0x21, 0x00};
    
    /**
     * 选择字体B（压缩字体）
     * ESC ! 1
     */
    public static final byte[] FONT_B = {0x1B, 0x21, 0x01};
    
    /**
     * 加粗模式开启
     * ESC E 1
     */
    public static final byte[] BOLD_ON = {0x1B, 0x45, 0x01};
    
    /**
     * 加粗模式关闭
     * ESC E 0
     */
    public static final byte[] BOLD_OFF = {0x1B, 0x45, 0x00};
    
    /**
     * 双倍高度开启
     * ESC ! 16
     */
    public static final byte[] DOUBLE_HEIGHT_ON = {0x1B, 0x21, 0x10};
    
    /**
     * 双倍宽度开启
     * ESC ! 32
     */
    public static final byte[] DOUBLE_WIDTH_ON = {0x1B, 0x21, 0x20};
    
    /**
     * 双倍高度宽度开启
     * ESC ! 48
     */
    public static final byte[] DOUBLE_HEIGHT_WIDTH_ON = {0x1B, 0x21, 0x30};
    
    /**
     * 恢复正常字体
     * ESC ! 0
     */
    public static final byte[] FONT_NORMAL = {0x1B, 0x21, 0x00};
    
    // ========== 下划线指令 ==========
    
    /**
     * 下划线开启
     * ESC - 1
     */
    public static final byte[] UNDERLINE_ON = {0x1B, 0x2D, 0x01};
    
    /**
     * 下划线关闭
     * ESC - 0
     */
    public static final byte[] UNDERLINE_OFF = {0x1B, 0x2D, 0x00};
    
    /**
     * 双下划线开启
     * ESC - 2
     */
    public static final byte[] UNDERLINE_DOUBLE = {0x1B, 0x2D, 0x02};
    
    // ========== 反白指令 ==========
    
    /**
     * 反白打印开启
     * GS B 1
     */
    public static final byte[] INVERT_ON = {0x1D, 0x42, 0x01};
    
    /**
     * 反白打印关闭
     * GS B 0
     */
    public static final byte[] INVERT_OFF = {0x1D, 0x42, 0x00};
    
    // ========== 切纸指令 ==========
    
    /**
     * 全切纸
     * GS V 0
     */
    public static final byte[] CUT_PAPER = {0x1D, 0x56, 0x00};
    
    /**
     * 半切纸
     * GS V 1
     */
    public static final byte[] CUT_PAPER_PARTIAL = {0x1D, 0x56, 0x01};
    
    /**
     * 全切纸并走纸 n 行
     * GS V 65 n
     */
    public static byte[] cutPaperWithFeed(int feedLines) {
        return new byte[]{0x1D, 0x56, 0x41, (byte) feedLines};
    }
    
    // ========== 钱箱指令 ==========
    
    /**
     * 打开钱箱（脉冲1）
     * ESC p 0 100 250
     */
    public static final byte[] OPEN_CASH_DRAWER = {0x1B, 0x70, 0x00, (byte) 0x19, (byte) 0xFA};
    
    /**
     * 打开钱箱（脉冲2）
     * ESC p 1 100 250
     */
    public static final byte[] OPEN_CASH_DRAWER_2 = {0x1B, 0x70, 0x01, (byte) 0x19, (byte) 0xFA};
    
    // ========== 走纸指令 ==========
    
    /**
     * 走纸 n 行
     * ESC d n
     */
    public static byte[] feedLines(int lines) {
        return new byte[]{0x1B, 0x64, (byte) lines};
    }
    
    /**
     * 走纸到切纸位置
     * GS V 66 n
     */
    public static byte[] feedAndCut(int feedLines) {
        return new byte[]{0x1D, 0x56, 0x42, (byte) feedLines};
    }
    
    // ========== 条码指令 ==========
    
    /**
     * 打印 UPC-A 条码
     * GS k 0 n data
     */
    public static byte[] barcodeUPCA(String data) {
        byte[] barcodeData = data.getBytes();
        byte[] command = new byte[barcodeData.length + 4];
        command[0] = 0x1D;
        command[1] = 0x6B;
        command[2] = 0x00;
        command[3] = (byte) barcodeData.length;
        System.arraycopy(barcodeData, 0, command, 4, barcodeData.length);
        return command;
    }
    
    /**
     * 打印 CODE128 条码
     * GS k 73 n data
     */
    public static byte[] barcodeCode128(String data) {
        byte[] barcodeData = data.getBytes();
        byte[] command = new byte[barcodeData.length + 4];
        command[0] = 0x1D;
        command[1] = 0x6B;
        command[2] = 73;
        command[3] = (byte) barcodeData.length;
        System.arraycopy(barcodeData, 0, command, 4, barcodeData.length);
        return command;
    }
    
    /**
     * 设置条码高度
     * GS h n (n: 1-255, 默认162)
     */
    public static byte[] setBarcodeHeight(int height) {
        return new byte[]{0x1D, 0x68, (byte) height};
    }
    
    /**
     * 设置条码宽度
     * GS w n (n: 2-6, 默认3)
     */
    public static byte[] setBarcodeWidth(int width) {
        return new byte[]{0x1D, 0x77, (byte) width};
    }
    
    /**
     * 打印条码下方的文字
     * GS H n (0:不打印, 1:上方, 2:下方, 3:上下)
     */
    public static byte[] setBarcodeTextPosition(int position) {
        return new byte[]{0x1D, 0x48, (byte) position};
    }
    
    // ========== 二维码指令 ==========
    
    /**
     * 选择二维码模型
     * GS ( k cn fn n 65
     */
    public static final byte[] QR_MODEL_1 = {0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00};
    
    /**
     * 设置二维码大小
     * GS ( k cn fn n 67 n
     */
    public static byte[] setQRSize(int size) {
        // size: 1-16, 默认3, 确保在有效范围内
        int validSize = Math.max(1, Math.min(16, size));
        return new byte[]{0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, (byte) validSize};
    }
    
    /**
     * 设置二维码纠错级别
     * GS ( k cn fn n 69 n
     * n: 48=L, 49=M, 50=H, 51=Q
     */
    public static byte[] setQRErrorLevel(int level) {
        // 确保level在有效范围内
        int validLevel = Math.max(48, Math.min(51, level));
        return new byte[]{0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, (byte) validLevel};
    }
    
    /**
     * 打印二维码
     * GS ( k cn fn n 80 m data
     */
    public static byte[] printQRCode(String data) {
        byte[] qrData = data.getBytes();
        int length = qrData.length + 3;
        byte[] command = new byte[length + 8];
        
        // GS ( k
        command[0] = 0x1D;
        command[1] = 0x28;
        command[2] = 0x6B;
        
        // pL pH (参数长度)
        command[3] = (byte) (length & 0xFF);
        command[4] = (byte) ((length >> 8) & 0xFF);
        
        // cn fn
        command[5] = 0x31;
        command[6] = 0x50;
        command[7] = 0x30;
        
        // m (数据长度)
        command[8] = (byte) qrData.length;
        
        // data
        System.arraycopy(qrData, 0, command, 9, qrData.length);
        
        return command;
    }
    
    // ========== 状态查询指令 ==========
    
    /**
     * 查询打印机状态（实时）
     * DLE EOT n
     * n=1: 打印机状态, n=2: 错误状态, n=3: 纸张状态, n=4: 钱箱状态
     */
    public static byte[] getStatus(int statusType) {
        return new byte[]{0x10, 0x04, (byte) statusType};
    }
    
    /**
     * 查询打印机状态（发送后返回）
     * GS r n
     * n=1: 纸张状态, n=2: 钱箱状态
     */
    public static byte[] queryStatus(int statusType) {
        return new byte[]{0x1D, 0x72, (byte) statusType};
    }
    
    // ========== 图像打印指令 ==========
    
    /**
     * 打印位图图像
     * GS v 0 m xL xH yL yH data
     */
    public static byte[] printBitmap(int width, int height, byte[] imageData) {
        int xL = width % 256;
        int xH = width / 256;
        int yL = height % 256;
        int yH = height / 256;
        
        byte[] command = new byte[imageData.length + 8];
        command[0] = 0x1D;
        command[1] = 0x76;
        command[2] = 0x30;
        command[3] = 0x00;
        command[4] = (byte) xL;
        command[5] = (byte) xH;
        command[6] = (byte) yL;
        command[7] = (byte) yH;
        
        System.arraycopy(imageData, 0, command, 8, imageData.length);
        
        return command;
    }
    
    // ========== 辅助方法 ==========
    
    /**
     * 创建分隔线
     */
    public static String createSeparator(int width, char character) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width; i++) {
            sb.append(character);
        }
        return sb.toString();
    }
    
    /**
     * 创建居中文本（80mm纸张约48字符）
     */
    public static String centerText(String text, int width) {
        int textLength = text.length();
        int padding = (width - textLength) / 2;
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < padding; i++) {
            sb.append(' ');
        }
        sb.append(text);
        
        return sb.toString();
    }
    
    /**
     * 创建左右对齐的行
     */
    public static String alignLeftRight(String left, String right, int width) {
        int leftLength = left.length();
        int rightLength = right.length();
        int spaceBetween = width - leftLength - rightLength;
        
        StringBuilder sb = new StringBuilder();
        sb.append(left);
        for (int i = 0; i < spaceBetween; i++) {
            sb.append(' ');
        }
        sb.append(right);
        
        return sb.toString();
    }
    
    /**
     * 创建金额行（左侧文字，右侧金额）
     */
    public static String createAmountLine(String label, String amount, int width) {
        return alignLeftRight(label, amount, width);
    }
    
    /**
     * 将字符串转换为字节数组（GBK编码）
     */
    public static byte[] toBytes(String text) {
        try {
            return text.getBytes("GBK");
        } catch (java.io.UnsupportedEncodingException e) {
            return text.getBytes();
        }
    }
}