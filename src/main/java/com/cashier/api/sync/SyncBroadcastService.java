package com.cashier.api.sync;

import com.cashier.dao.ProductDAO;
import com.cashier.dao.TransactionDAO;
import com.cashier.dao.MemberDAO;
import com.cashier.model.Product;
import com.cashier.model.Transaction;
import com.cashier.model.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 事件广播服务
 * 在业务操作完成后广播同步事件
 */
public class SyncBroadcastService {
    private static final Logger logger = LoggerFactory.getLogger(SyncBroadcastService.class);
    
    /**
     * 广播交易创建事件
     */
    public static void broadcastTransactionCreated(Transaction transaction) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("transactionId", transaction.transactionId);
            data.put("timestamp", transaction.timestamp);
            data.put("finalAmount", transaction.finalAmount != null ? transaction.finalAmount.toString() : "0");
            data.put("paymentMethod", transaction.paymentMethod);
            data.put("operatorUsername", transaction.operatorUsername);
            
            SyncManager.getInstance().broadcastSyncEvent(SyncEventType.TRANSACTION_CREATED, data);
            
            logger.debug("广播交易创建: {}", transaction.transactionId);
        } catch (Exception e) {
            logger.error("广播交易创建失败", e);
        }
    }
    
    /**
     * 广播交易退款事件
     */
    public static void broadcastTransactionRefunded(String transactionId, String reason) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("transactionId", transactionId);
            data.put("reason", reason);
            data.put("refundedAt", System.currentTimeMillis());
            
            SyncManager.getInstance().broadcastSyncEvent(SyncEventType.TRANSACTION_REFUNDED, data);
            
            logger.debug("广播交易退款: {}", transactionId);
        } catch (Exception e) {
            logger.error("广播交易退款失败", e);
        }
    }
    
    /**
     * 广播商品更新事件
     */
    public static void broadcastProductUpdated(Product product) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("productId", product.id);
            data.put("productCode", product.productCode);
            data.put("name", product.name);
            data.put("price", product.price != null ? product.price.toString() : "0");
            data.put("quantity", product.quantity);
            data.put("category", product.category);
            
            SyncManager.getInstance().broadcastSyncEvent(SyncEventType.PRODUCT_UPDATED, data);
            
            logger.debug("广播商品更新: {} - {}", product.id, product.name);
        } catch (Exception e) {
            logger.error("广播商品更新失败", e);
        }
    }
    
    /**
     * 广播商品新增事件
     */
    public static void broadcastProductCreated(Product product) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("productId", product.id);
            data.put("productCode", product.productCode);
            data.put("name", product.name);
            data.put("price", product.price != null ? product.price.toString() : "0");
            data.put("quantity", product.quantity);
            data.put("category", product.category);
            
            SyncManager.getInstance().broadcastSyncEvent(SyncEventType.PRODUCT_CREATED, data);
            
            logger.debug("广播商品新增: {} - {}", product.id, product.name);
        } catch (Exception e) {
            logger.error("广播商品新增失败", e);
        }
    }
    
    /**
     * 广播商品删除事件
     */
    public static void broadcastProductDeleted(int productId, String productName) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("productId", productId);
            data.put("name", productName);
            
            SyncManager.getInstance().broadcastSyncEvent(SyncEventType.PRODUCT_DELETED, data);
            
            logger.debug("广播商品删除: {}", productId);
        } catch (Exception e) {
            logger.error("广播商品删除失败", e);
        }
    }
    
    /**
     * 广播会员更新事件
     */
    public static void broadcastMemberUpdated(Member member) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("memberId", member.id);
            data.put("phone", member.phone);
            data.put("name", member.name);
            data.put("level", member.level);
            data.put("balance", member.balance != null ? member.balance.toString() : "0");
            data.put("points", member.points != null ? member.points.toString() : "0");
            
            SyncManager.getInstance().broadcastSyncEvent(SyncEventType.MEMBER_UPDATED, data);
            
            logger.debug("广播会员更新: {} - {}", member.id, member.phone);
        } catch (Exception e) {
            logger.error("广播会员更新失败", e);
        }
    }
    
    /**
     * 广播会员充值事件
     */
    public static void broadcastMemberRecharged(Member member, java.math.BigDecimal amount) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("memberId", member.id);
            data.put("phone", member.phone);
            data.put("rechargeAmount", amount.toString());
            data.put("newBalance", member.balance != null ? member.balance.toString() : "0");
            data.put("rechargedAt", System.currentTimeMillis());
            
            SyncManager.getInstance().broadcastSyncEvent(SyncEventType.MEMBER_RECHARGED, data);
            
            logger.debug("广播会员充值: {} +{}", member.phone, amount);
        } catch (Exception e) {
            logger.error("广播会员充值失败", e);
        }
    }
    
    /**
     * 广播库存变化事件
     */
    public static void broadcastInventoryChanged(int productId, String productName, int oldQuantity, int newQuantity) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("productId", productId);
            data.put("name", productName);
            data.put("oldQuantity", oldQuantity);
            data.put("newQuantity", newQuantity);
            data.put("delta", newQuantity - oldQuantity);
            data.put("changedAt", System.currentTimeMillis());
            
            SyncManager.getInstance().broadcastSyncEvent(SyncEventType.INVENTORY_CHANGED, data);
            
            logger.debug("广播库存变化: {} {} -> {}", productId, oldQuantity, newQuantity);
        } catch (Exception e) {
            logger.error("广播库存变化失败", e);
        }
    }
    
    /**
     * 广播库存预警事件
     */
    public static void broadcastInventoryAlert(Product product) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("productId", product.id);
            data.put("name", product.name);
            data.put("currentQuantity", product.quantity);
            data.put("minStock", product.minStock);
            data.put("alertLevel", product.quantity <= 0 ? "严重" : "警告");
            data.put("alertAt", System.currentTimeMillis());
            
            SyncManager.getInstance().broadcastSyncEvent(SyncEventType.INVENTORY_ALERT, data);
            
            logger.warn("广播库存预警: {} 当前库存 {} < 最低 {}", product.name, product.quantity, product.minStock);
        } catch (Exception e) {
            logger.error("广播库存预警失败", e);
        }
    }
    
    /**
     * 广播系统告警
     */
    public static void broadcastSystemAlert(String alertType, String message, String level) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("alertType", alertType);
            data.put("message", message);
            data.put("level", level);
            data.put("alertAt", System.currentTimeMillis());
            
            SyncManager.getInstance().broadcastSyncEvent(SyncEventType.SYSTEM_ALERT, data);
            
            logger.info("广播系统告警: {} - {}", alertType, message);
        } catch (Exception e) {
            logger.error("广播系统告警失败", e);
        }
    }
}