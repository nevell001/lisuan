package com.cashier.scanner;

/**
 * 扫描监听器接口
 * 用于接收扫描事件
 */
@FunctionalInterface
public interface ScanListener {
    
    /**
     * 扫描完成时调用
     * @param event 扫描事件
     */
    void onScan(ScanEvent event);
}