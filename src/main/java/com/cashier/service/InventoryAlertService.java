package com.cashier.service;

import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.model.Product;
import com.cashier.notification.NotificationManager;
import com.cashier.notification.NotificationType;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.*;

/**
 * 库存预警服务
 * 负责定期检查商品库存，当库存低于阈值时发送通知
 */
public class InventoryAlertService {
    private static final Logger logger = LoggerFactoryUtil.getLogger(InventoryAlertService.class);
    private static final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();
    private static InventoryAlertService instance;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private volatile boolean isRunning = false;
    
    // 默认检查间隔：5分钟
    private long checkInterval = 5 * 60 * 1000;
    
    // 上次检查的时间戳
    private volatile long lastCheckTime = 0;
    
    // 上次预警的商品ID集合，避免重复预警
    private final ConcurrentHashMap<Integer, Long> lastAlertMap = new ConcurrentHashMap<>();
    
    // 预警冷却时间：1小时（同一商品1小时内不重复预警）
    private long alertCooldown = 60 * 60 * 1000;
    
    private InventoryAlertService() {
        logger.info("库存预警服务初始化完成");
    }
    
    public static synchronized InventoryAlertService getInstance() {
        if (instance == null) {
            instance = new InventoryAlertService();
        }
        return instance;
    }
    
    /**
     * 启动库存预警检查
     */
    public void start() {
        if (isRunning) {
            logger.warn("库存预警服务已在运行中");
            return;
        }
        
        isRunning = true;
        
        // 立即执行一次检查
        checkInventoryAlert();
        
        // 启动定期检查任务
        scheduler.scheduleAtFixedRate(() -> {
            if (isRunning) {
                checkInventoryAlert();
            }
        }, checkInterval, checkInterval, TimeUnit.MILLISECONDS);
        
        logger.info("库存预警服务已启动，检查间隔: {} 毫秒", checkInterval);
    }
    
    /**
     * 停止库存预警检查
     */
    public void stop() {
        if (!isRunning) {
            logger.warn("库存预警服务未在运行");
            return;
        }
        
        isRunning = false;
        scheduler.shutdown();
        
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("库存预警服务已停止");
    }
    
    /**
     * 检查库存预警
     */
    public void checkInventoryAlert() {
        try {
            lastCheckTime = System.currentTimeMillis();
            logger.debug("开始检查库存预警...");
            
            // 获取所有商品
            List<Product> products = productDAO.findAll();
            
            if (products == null || products.isEmpty()) {
                logger.debug("没有商品需要检查");
                return;
            }
            
            int alertCount = 0;
            
            // 检查每个商品的库存
            for (Product product : products) {
                // 跳过没有设置最低库存的商品
                if (product.minStock <= 0) {
                    continue;
                }
                
                // 检查库存是否低于最低库存
                if (product.quantity <= product.minStock) {
                    // 检查是否在冷却时间内
                    Long lastAlertTime = lastAlertMap.get(product.id);
                    long currentTime = System.currentTimeMillis();
                    
                    if (lastAlertTime == null || (currentTime - lastAlertTime) > alertCooldown) {
                        // 发送库存预警通知
                        sendInventoryAlert(product);
                        lastAlertMap.put(product.id, currentTime);
                        alertCount++;
                        logger.info("商品库存预警: {} (库存: {}, 最低库存: {})", 
                            product.name, product.quantity, product.minStock);
                    }
                }
            }
            
            logger.debug("库存预警检查完成，共发送 {} 条预警通知", alertCount);
            
        } catch (SQLException e) {
            logger.error("从数据库加载商品失败", e);
        } catch (Exception e) {
            logger.error("检查库存预警时发生错误", e);
        }
    }
    
    /**
     * 发送库存预警通知
     */
    private void sendInventoryAlert(Product product) {
        try {
            String title = "库存预警";
            String message = String.format(
                "商品【%s】库存不足！\n" +
                "当前库存: %d %s\n" +
                "最低库存: %d %s\n" +
                "商品编号: %s\n" +
                "请及时补货！",
                product.name,
                product.quantity,
                product.unit != null ? product.unit : "个",
                product.minStock,
                product.unit != null ? product.unit : "个",
                product.productCode != null ? product.productCode : "无"
            );
            
            // 判断预警级别
            NotificationType notificationType;
            if (product.quantity == 0) {
                // 库存为0，严重警告
                notificationType = NotificationType.ERROR;
            } else if (product.quantity < product.minStock / 2) {
                // 库存低于最低库存的一半，警告
                notificationType = NotificationType.WARNING;
            } else {
                // 库存接近最低库存，信息提示
                notificationType = NotificationType.INFO;
            }
            
            // 发送通知
            NotificationManager.getInstance().sendNotification(
                notificationType,
                title,
                message
            );
            
            logger.info("库存预警通知已发送: {} - {}", title, message);
            
        } catch (Exception e) {
            logger.error("发送库存预警通知时发生错误", e);
        }
    }
    
    /**
     * 手动触发库存预警检查
     */
    public void triggerCheck() {
        logger.info("手动触发库存预警检查");
        checkInventoryAlert();
    }
    
    /**
     * 设置检查间隔
     */
    public void setCheckInterval(long intervalMillis) {
        this.checkInterval = intervalMillis;
        logger.info("库存预警检查间隔已设置为: {} 毫秒", intervalMillis);
    }
    
    /**
     * 设置预警冷却时间
     */
    public void setAlertCooldown(long cooldownMillis) {
        this.alertCooldown = cooldownMillis;
        logger.info("库存预警冷却时间已设置为: {} 毫秒", cooldownMillis);
    }
    
    /**
     * 清除指定商品的预警冷却
     * 用于测试或手动触发重新预警
     */
    public void clearAlertCooldown(int productId) {
        lastAlertMap.remove(productId);
        logger.info("已清除商品ID {} 的预警冷却", productId);
    }
    
    /**
     * 清除所有预警冷却
     */
    public void clearAllAlertCooldowns() {
        lastAlertMap.clear();
        logger.info("已清除所有商品的预警冷却");
    }
    
    /**
     * 获取上次检查时间
     */
    public long getLastCheckTime() {
        return lastCheckTime;
    }
    
    /**
     * 检查服务是否正在运行
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * 获取预警冷却时间
     */
    public long getAlertCooldown() {
        return alertCooldown;
    }
    
    /**
     * 获取检查间隔
     */
    public long getCheckInterval() {
        return checkInterval;
    }
}
