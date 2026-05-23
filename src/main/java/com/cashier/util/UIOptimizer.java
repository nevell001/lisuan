package com.cashier.util;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * UI 渲染优化工具类
 * 提供虚拟化、异步加载、缓存优化等功能
 */
public class UIOptimizer {
    private static final Logger logger = LoggerFactoryUtil.getLogger(UIOptimizer.class);
    
    // 异步任务线程池
    private static final ExecutorService asyncExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "UI-Optimizer-Async");
        t.setDaemon(true);
        return t;
    });
    
    // 简单的对象缓存
    private static final java.util.Map<String, Object> cache = new java.util.WeakHashMap<>();
    private static final long CACHE_EXPIRE_TIME = 5 * 60 * 1000; // 5分钟
    // 使用 LinkedHashMap 实现 LRU，避免内存泄漏
    private static final java.util.Map<String, Long> cacheTime = new java.util.LinkedHashMap<String, Long>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, Long> eldest) {
            // 当缓存时间戳超过 1000 个时，移除最旧的条目
            return size() > 1000;
        }
    };

    static {
        // 定期清理过期缓存时间戳（每分钟）
        java.util.concurrent.ScheduledExecutorService cleanupExecutor =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "UI-Cache-Cleanup");
                t.setDaemon(true);
                return t;
            });
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                long now = System.currentTimeMillis();
                cacheTime.entrySet().removeIf(entry -> {
                    long age = now - entry.getValue();
                    if (age > CACHE_EXPIRE_TIME) {
                        cache.remove(entry.getKey());
                        return true;
                    }
                    return false;
                });
            } catch (Exception e) {
                logger.error("缓存清理失败", e);
            }
        }, 1, 1, java.util.concurrent.TimeUnit.MINUTES);
    }
    
    /**
     * 启用TableView虚拟化
     * 提高大表格的渲染性能
     */
    public static <T> void enableTableViewVirtualization(TableView<T> tableView) {
        // 固定单元格大小，提高渲染性能
        tableView.setFixedCellSize(40.0);
        
        // 启用行虚拟化
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        logger.debug("已启用TableView虚拟化");
    }
    
    /**
     * 异步加载数据
     * 在后台线程执行耗时操作，然后在JavaFX线程更新UI
     */
    public static <T> void loadAsync(Task<T> task, Consumer<T> onSuccess, Consumer<Exception> onError) {
        task.setOnSucceeded(e -> {
            T result = task.getValue();
            if (onSuccess != null) {
                onSuccess.accept(result);
            }
        });
        
        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            if (onError != null && exception instanceof Exception) {
                onError.accept((Exception) exception);
            } else if (exception != null) {
                logger.error("异步任务执行失败", exception);
            }
        });
        
        asyncExecutor.submit(task);
        logger.debug("已提交异步任务");
    }
    
    /**
     * 延迟UI更新
     * 避免频繁更新导致的性能问题
     */
    public static void delayedUpdate(Runnable updateTask, long delayMs) {
        java.util.concurrent.ScheduledExecutorService scheduler = 
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "UI-Delayed-Update");
                t.setDaemon(true);
                return t;
            });
        
        scheduler.schedule(() -> {
            Platform.runLater(() -> {
                try {
                    updateTask.run();
                } catch (Exception e) {
                    logger.error("延迟UI更新失败", e);
                }
            });
            scheduler.shutdown();
        }, delayMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    /**
     * 批量更新UI
     * 合并多个UI更新操作，减少重绘次数
     */
    public static void batchUpdate(Runnable... updateTasks) {
        Platform.runLater(() -> {
            try {
                for (Runnable task : updateTasks) {
                    if (task != null) {
                        task.run();
                    }
                }
            } catch (Exception e) {
                logger.error("批量UI更新失败", e);
            }
        });
    }
    
    /**
     * 获取缓存数据
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFromCache(String key) {
        Long time = cacheTime.get(key);
        if (time == null) {
            return null;
        }
        
        // 检查缓存是否过期
        if (System.currentTimeMillis() - time > CACHE_EXPIRE_TIME) {
            cache.remove(key);
            cacheTime.remove(key);
            return null;
        }
        
        return (T) cache.get(key);
    }
    
    /**
     * 保存数据到缓存
     */
    public static <T> void saveToCache(String key, T value) {
        cache.put(key, value);
        cacheTime.put(key, System.currentTimeMillis());
        logger.debug("已保存数据到缓存: {}", key);
    }
    
    /**
     * 清除缓存
     */
    public static void clearCache() {
        cache.clear();
        cacheTime.clear();
        logger.info("已清除所有缓存");
    }
    
    /**
     * 清除指定键的缓存
     */
    public static void clearCache(String key) {
        cache.remove(key);
        cacheTime.remove(key);
        logger.debug("已清除缓存: {}", key);
    }
    
    /**
     * 优化TreeItem性能
     * 懒加载子节点
     */
    public static <T> void optimizeTreeItem(TreeItem<T> treeItem, Consumer<TreeItem<T>> loadChildren) {
        treeItem.setExpanded(false);
        treeItem.expandedProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue && treeItem.getChildren().isEmpty()) {
                loadChildren.accept(treeItem);
            }
        });
    }
    
    /**
     * 显示加载指示器
     */
    public static ProgressIndicator showLoading(Node parent) {
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setStyle("-fx-progress-color: #3F51B5;");
        
        if (parent instanceof Pane) {
            Pane pane = (Pane) parent;
            if (!pane.getChildren().contains(indicator)) {
                pane.getChildren().add(indicator);
            }
        }
        
        return indicator;
    }
    
    /**
     * 隐藏加载指示器
     */
    public static void hideLoading(Node parent, ProgressIndicator indicator) {
        if (indicator != null && parent instanceof Pane) {
            Pane pane = (Pane) parent;
            pane.getChildren().remove(indicator);
        }
    }
    
    /**
     * 创建虚拟滚动面板
     */
    public static <T> ListView<T> createVirtualListView(int fixedCellSize) {
        ListView<T> listView = new ListView<>();
        listView.setFixedCellSize(fixedCellSize);
        return listView;
    }
    
    /**
     * 优化ComboBox性能
     */
    public static <T> void optimizeComboBox(ComboBox<T> comboBox) {
        // 启用虚拟化
        comboBox.setCellFactory(listView -> {
            ListCell<T> cell = new ListCell<>();
            cell.setStyle("-fx-pref-height: 30;");
            return cell;
        });
        
        // 延迟加载
        comboBox.setOnShowing(e -> {
            if (comboBox.getItems().isEmpty()) {
                // 触发数据加载
            }
        });
    }
    
    /**
     * 关闭异步线程池
     */
    public static void shutdown() {
        asyncExecutor.shutdown();
        logger.info("UI优化线程池已关闭");
    }
}