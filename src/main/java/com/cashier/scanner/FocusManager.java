package com.cashier.scanner;

import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 焦点管理器
 * 负责智能管理输入焦点，处理键盘输入和扫描输入的冲突
 */
public class FocusManager {
    
    private static final Logger logger = LoggerFactoryUtil.getLogger(FocusManager.class);
    
    /**
     * 注册的焦点目标
     */
    private final List<FocusTarget> focusTargets;
    
    /**
     * 当前活跃的焦点目标
     */
    private FocusTarget currentTarget;
    
    /**
     * 上次输入时间
     */
    private long lastInputTime;
    
    /**
     * 判定为扫描输入的时间间隔（毫秒）
     */
    private static final long SCAN_INPUT_INTERVAL = 50;
    
    /**
     * 判定为键盘输入的时间间隔（毫秒）
     */
    private static final long KEYBOARD_INPUT_INTERVAL = 500;
    
    public FocusManager() {
        this.focusTargets = new ArrayList<>();
    }
    
    /**
     * 注册焦点目标
     * @param target 焦点目标
     */
    public void registerFocusTarget(FocusTarget target) {
        if (target != null && !focusTargets.contains(target)) {
            focusTargets.add(target);
            logger.debug("注册焦点目标: {}", target.getName());
        }
    }
    
    /**
     * 注销焦点目标
     * @param target 焦点目标
     */
    public void unregisterFocusTarget(FocusTarget target) {
        if (target != null) {
            focusTargets.remove(target);
            if (currentTarget == target) {
                currentTarget = null;
            }
            logger.debug("注销焦点目标: {}", target.getName());
        }
    }
    
    /**
     * 请求焦点
     * @param target 焦点目标
     */
    public void requestFocus(FocusTarget target) {
        if (target != null) {
            if (currentTarget != null && currentTarget != target) {
                currentTarget.loseFocus();
            }
            currentTarget = target;
            currentTarget.gainFocus();
            logger.debug("焦点切换到: {}", target.getName());
        }
    }
    
    /**
     * 获取当前焦点目标
     * @return 焦点目标
     */
    public FocusTarget getCurrentTarget() {
        return currentTarget;
    }
    
    /**
     * 处理输入事件
     * @param input 输入内容
     * @param isEnter 是否是回车键
     * @return 输入类型
     */
    public InputType handleInput(String input, boolean isEnter) {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastInputTime;
        
        if (currentTarget == null && !focusTargets.isEmpty()) {
            // 自动选择第一个可用的焦点目标
            currentTarget = focusTargets.get(0);
            currentTarget.gainFocus();
        }
        
        if (currentTarget == null) {
            return InputType.UNKNOWN;
        }
        
        // 根据时间间隔判断输入类型
        if (timeDiff <= SCAN_INPUT_INTERVAL) {
            // 快速连续输入，判定为扫描输入
            currentTarget.onScanInput(input);
            if (isEnter) {
                currentTarget.onScanComplete(input);
            }
            lastInputTime = currentTime;
            return InputType.SCAN;
        } else if (timeDiff <= KEYBOARD_INPUT_INTERVAL) {
            // 正常速度输入，判定为键盘输入
            currentTarget.onKeyboardInput(input);
            lastInputTime = currentTime;
            return InputType.KEYBOARD;
        } else {
            // 超过间隔，可能是新的扫描开始
            currentTarget.onScanInput(input);
            lastInputTime = currentTime;
            return InputType.KEYBOARD; // 暂时判定为键盘，等待下一个字符
        }
    }
    
    /**
     * 自动选择扫描焦点目标
     * 当检测到扫描输入时，自动将焦点切换到最适合的目标
     */
    public void autoSelectScanTarget() {
        // 查找优先级最高的扫描目标
        for (FocusTarget target : focusTargets) {
            if (target.isScanTarget() && target.canReceiveFocus()) {
                requestFocus(target);
                return;
            }
        }
    }
    
    /**
     * 输入类型枚举
     */
    public enum InputType {
        /**
         * 键盘输入
         */
        KEYBOARD,
        
        /**
         * 扫描输入
         */
        SCAN,
        
        /**
         * 未知类型
         */
        UNKNOWN
    }
}