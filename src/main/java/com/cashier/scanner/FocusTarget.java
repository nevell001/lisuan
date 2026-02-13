package com.cashier.scanner;

/**
 * 焦点目标接口
 * 定义可以接收焦点的组件行为
 */
public interface FocusTarget {
    
    /**
     * 获取目标名称
     * @return 名称
     */
    String getName();
    
    /**
     * 获取焦点
     */
    void gainFocus();
    
    /**
     * 失去焦点
     */
    void loseFocus();
    
    /**
     * 是否可以接收焦点
     * @return 是否可以接收焦点
     */
    boolean canReceiveFocus();
    
    /**
     * 是否是扫描目标（优先处理扫描输入）
     * @return 是否是扫描目标
     */
    boolean isScanTarget();
    
    /**
     * 处理键盘输入
     * @param input 输入内容
     */
    void onKeyboardInput(String input);
    
    /**
     * 处理扫描输入
     * @param input 输入内容
     */
    void onScanInput(String input);
    
    /**
     * 处理扫描完成
     * @param input 输入内容
     */
    void onScanComplete(String input);
}