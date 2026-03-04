package com.cashier.notification;

import com.cashier.model.Product;
import com.cashier.model.Shift;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息通知业务集成类
 * 在关键业务场景中自动触发通知
 */
public class NotificationIntegration {
    private static final Logger logger = LoggerFactoryUtil.getLogger(NotificationIntegration.class);
    
    // 库存预警阈值
    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final int CRITICAL_STOCK_THRESHOLD = 5;
    
    /**
     * 初始化通知集成
     */
    public static void initialize() {
        NotificationManager.getInstance();
        logger.info("消息通知系统已初始化");
    }
    
    /**
     * 库存预警通知
     * 当商品库存低于阈值时触发
     */
    public static void notifyLowStock(Product product) {
        if (product == null) {
            return;
        }
        
        NotificationType type;
        String message;
        String title;
        
        if (product.quantity <= CRITICAL_STOCK_THRESHOLD) {
            type = NotificationType.CRITICAL;
            title = "库存严重不足";
            message = String.format("商品【%s】库存严重不足！当前库存：%d，最低库存：%d",
                product.name, product.quantity, product.minStock);
        } else if (product.quantity <= LOW_STOCK_THRESHOLD) {
            type = NotificationType.WARNING;
            title = "库存预警";
            message = String.format("商品【%s】库存不足。当前库存：%d，最低库存：%d",
                product.name, product.quantity, product.minStock);
        } else {
            return;
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("productId", product.id);
        data.put("productName", product.name);
        data.put("currentStock", product.quantity);
        data.put("minStock", product.minStock);
        
        Notification notification = new Notification(
            type,
            title,
            message,
            data
        );
        
        NotificationManager.getInstance().addNotification(notification);
        logger.warn("库存预警通知已发送: {}", message);
    }
    
    /**
     * 促销活动提醒
     */
    public static void notifyPromotion(String promotionName, Date startDate, Date endDate) {
        String message = String.format("促销活动【%s】即将开始，时间：%s 至 %s",
            promotionName, startDate, endDate);
        
        Map<String, Object> data = new HashMap<>();
        data.put("promotionName", promotionName);
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        
        Notification notification = new Notification(
            NotificationType.PROMOTION,
            "促销活动提醒",
            message,
            data
        );
        
        NotificationManager.getInstance().addNotification(notification);
        logger.info("促销提醒通知已发送: {}", message);
    }
    
    /**
     * 交接班通知
     */
    public static void notifyShiftChange(Shift shift, String action) {
        String message;
        NotificationType type;
        
        if ("start".equals(action)) {
            type = NotificationType.INFO;
            message = String.format("操作员【%s】已开始班次，班次ID：%s",
                shift.operatorName, shift.shiftId);
        } else if ("end".equals(action)) {
            type = NotificationType.INFO;
            message = String.format("操作员【%s】已完成交班，班次ID：%s，营业额：¥%.2f",
                shift.operatorName, shift.shiftId, shift.shiftRevenue);
        } else {
            return;
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("shiftId", shift.shiftId);
        data.put("operatorName", shift.operatorName);
        data.put("action", action);
        data.put("revenue", shift.shiftRevenue);
        
        Notification notification = new Notification(
            type,
            "交接班通知",
            message,
            data
        );
        
        NotificationManager.getInstance().addNotification(notification);
        logger.info("交接班通知已发送: {}", message);
    }
    
    /**
     * 系统通知
     */
    public static void notifySystem(String title, String message, NotificationType type) {
        Notification notification = new Notification(
            type,
            title,
            message,
            null
        );
        
        NotificationManager.getInstance().addNotification(notification);
        logger.info("系统通知已发送: {} - {}", title, message);
    }
    
    /**
     * 数据备份通知
     */
    public static void notifyBackup(String backupPath, boolean success) {
        NotificationType type = success ? NotificationType.SUCCESS : NotificationType.ERROR;
        String title = success ? "数据备份成功" : "数据备份失败";
        String message = success ? 
            String.format("数据已成功备份到：%s", backupPath) :
            String.format("数据备份失败，请查看日志");
        
        Map<String, Object> data = new HashMap<>();
        data.put("backupPath", backupPath);
        data.put("success", success);
        
        Notification notification = new Notification(
            type,
            title,
            message,
            data
        );
        
        NotificationManager.getInstance().addNotification(notification);
        logger.info("备份通知已发送: {}", message);
    }
    
    /**
     * 采购订单通知
     */
    public static void notifyPurchaseOrder(String orderNo, String status) {
        NotificationType type;
        String title;
        
        switch (status.toLowerCase()) {
            case "pending":
                type = NotificationType.INFO;
                title = "新采购订单";
                break;
            case "approved":
                type = NotificationType.SUCCESS;
                title = "采购订单已批准";
                break;
            case "rejected":
                type = NotificationType.ERROR;
                title = "采购订单已拒绝";
                break;
            case "completed":
                type = NotificationType.SUCCESS;
                title = "采购订单已完成";
                break;
            default:
                return;
        }
        
        String message = String.format("采购订单【%s】状态更新为：%s", orderNo, status);
        
        Map<String, Object> data = new HashMap<>();
        data.put("orderNo", orderNo);
        data.put("status", status);
        
        Notification notification = new Notification(
            type,
            title,
            message,
            data
        );
        
        NotificationManager.getInstance().addNotification(notification);
        logger.info("采购订单通知已发送: {}", message);
    }
    
    /**
     * 会员充值通知
     */
    public static void notifyMemberRecharge(String memberName, double amount) {
        String message = String.format("会员【%s】充值成功，金额：¥%.2f", memberName, amount);
        
        Map<String, Object> data = new HashMap<>();
        data.put("memberName", memberName);
        data.put("amount", amount);
        
        Notification notification = new Notification(
            NotificationType.SUCCESS,
            "会员充值成功",
            message,
            data
        );
        
        NotificationManager.getInstance().addNotification(notification);
        logger.info("会员充值通知已发送: {}", message);
    }
    
    /**
     * 退货申请通知
     */
    public static void notifyReturnOrder(String returnOrderId, String status) {
        NotificationType type;
        String title;
        
        switch (status.toUpperCase()) {
            case "PENDING":
                type = NotificationType.INFO;
                title = "新退货申请";
                break;
            case "APPROVED":
                type = NotificationType.SUCCESS;
                title = "退货申请已批准";
                break;
            case "REJECTED":
                type = NotificationType.ERROR;
                title = "退货申请已拒绝";
                break;
            case "COMPLETED":
                type = NotificationType.SUCCESS;
                title = "退货已完成";
                break;
            default:
                return;
        }
        
        String message = String.format("退货单【%s】状态更新为：%s", returnOrderId, status);
        
        Map<String, Object> data = new HashMap<>();
        data.put("returnOrderId", returnOrderId);
        data.put("status", status);
        
        Notification notification = new Notification(
            type,
            title,
            message,
            data
        );
        
        NotificationManager.getInstance().addNotification(notification);
        logger.info("退货通知已发送: {}", message);
    }
    
    /**
     * 销售里程碑通知
     */
    public static void notifySalesMilestone(String message) {
        Notification notification = new Notification(
            NotificationType.MILESTONE,
            "销售里程碑",
            message,
            null
        );
        
        NotificationManager.getInstance().addNotification(notification);
        logger.info("销售里程碑通知已发送: {}", message);
    }
}