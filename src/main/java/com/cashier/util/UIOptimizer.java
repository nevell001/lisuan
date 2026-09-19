package com.cashier.util;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

    private static final ScheduledExecutorService cleanupExecutor =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "UI-Cache-Cleanup");
            t.setDaemon(true);
            return t;
        });
    
    // 统一缓存条目（值 + 时间戳），消除双 Map 竞态
    private static class CacheEntry {
        final Object value;
        final long timestamp;
        CacheEntry(Object value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
    private static final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRE_TIME = 5 * 60 * 1000; // 5分钟
    private static final int MAX_CACHE_SIZE = 1000;

    static {
        // 定期清理过期缓存（每分钟）
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                long now = System.currentTimeMillis();
                cache.entrySet().removeIf(entry -> {
                    long age = now - entry.getValue().timestamp;
                    return age > CACHE_EXPIRE_TIME;
                });
            } catch (Exception e) {
                logger.error("缓存清理失败", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
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
     * 在后台线程执行可能阻塞的操作（数据库/文件/网络），完成后回到 JavaFX 应用线程处理结果。
     *
     * <p>与 {@link #loadAsync(Task, Consumer, Consumer)} 的区别：直接接受可抛受检异常的
     * {@link Callable}，无需先构造 {@link Task}，适合收银台里"查库 → 刷新界面"的场景。
     * 所有界面更新都在 {@code Platform.runLater} 内执行，调用方无需再关心线程。</p>
     */
    public static <T> void runInBackground(Callable<T> work, Consumer<T> onSuccess, Consumer<Exception> onError) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return work.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }).whenComplete((result, error) -> Platform.runLater(() -> {
            if (error != null) {
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                Exception failure = cause instanceof Exception ? (Exception) cause : new RuntimeException(cause);
                if (onError != null) {
                    onError.accept(failure);
                } else {
                    logger.error("后台任务执行失败", failure);
                }
                return;
            }
            if (onSuccess != null) {
                onSuccess.accept(result);
            }
        }));
    }
    
    /**
     * 延迟UI更新
     * 避免频繁更新导致的性能问题
     * H-26: 复用已有的 cleanupExecutor，不再每次创建新线程池
     */
    public static void delayedUpdate(Runnable updateTask, long delayMs) {
        cleanupExecutor.schedule(() -> {
            try {
                Platform.runLater(() -> {
                    try {
                        updateTask.run();
                    } catch (Exception e) {
                        logger.error("延迟UI更新失败", e);
                    }
                });
            } catch (Exception e) {
                logger.error("延迟UI更新调度失败", e);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
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
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        // 检查缓存是否过期
        if (System.currentTimeMillis() - entry.timestamp > CACHE_EXPIRE_TIME) {
            cache.remove(key);
            return null;
        }

        return (T) entry.value;
    }

    /**
     * 保存数据到缓存
     */
    public static <T> void saveToCache(String key, T value) {
        if (cache.size() >= MAX_CACHE_SIZE) {
            // 超过上限时移除最早的条目
            cache.entrySet().stream()
                .min(java.util.Comparator.comparingLong(e -> e.getValue().timestamp))
                .ifPresent(e -> cache.remove(e.getKey()));
        }
        cache.put(key, new CacheEntry(value, System.currentTimeMillis()));
        logger.debug("已保存数据到缓存: {}", key);
    }

    /**
     * 清除缓存
     */
    public static void clearCache() {
        cache.clear();
        logger.info("已清除所有缓存");
    }
    
    /**
     * 清除指定键的缓存
     */
    public static void clearCache(String key) {
        cache.remove(key);
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
        indicator.getStyleClass().add("loading-indicator");
        
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
        shutdownExecutor(asyncExecutor, "UI优化线程池");
        shutdownExecutor(cleanupExecutor, "UI缓存清理线程池");
        logger.info("UI优化线程池已关闭");
    }

    private static void shutdownExecutor(java.util.concurrent.ExecutorService executor, String name) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("{}已关闭", name);
    }
}
