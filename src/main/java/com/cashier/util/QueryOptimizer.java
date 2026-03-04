package com.cashier.util;

import org.slf4j.Logger;
import java.sql.*;
import java.util.*;

/**
 * 数据库查询优化工具类
 * 提供批量查询、索引优化建议等功能
 */
public class QueryOptimizer {
    private static final Logger logger = LoggerFactoryUtil.getLogger(QueryOptimizer.class);
    
    // 批量查询的默认批次大小
    private static final int DEFAULT_BATCH_SIZE = 1000;
    
    /**
     * 批量查询优化
     * 将大量ID分成小批次进行查询，避免IN子句过长
     */
    public static <T> List<T> batchQuery(List<Integer> ids, BatchQueryFunction<T> queryFunction, 
                                         int batchSize) {
        List<T> results = new ArrayList<>();
        
        for (int i = 0; i < ids.size(); i += batchSize) {
            int end = Math.min(i + batchSize, ids.size());
            List<Integer> batch = ids.subList(i, end);
            
            try {
                List<T> batchResults = queryFunction.query(batch);
                results.addAll(batchResults);
            } catch (SQLException e) {
                logger.error("批量查询失败: 批次 {} - {}", i, end, e);
            }
        }
        
        return results;
    }
    
    /**
     * 批量查询（使用默认批次大小）
     */
    public static <T> List<T> batchQuery(List<Integer> ids, BatchQueryFunction<T> queryFunction) {
        return batchQuery(ids, queryFunction, DEFAULT_BATCH_SIZE);
    }
    
    /**
     * 分析查询性能
     */
    public static QueryPerformance analyzeQuery(Connection conn, String sql) {
        long startTime = System.currentTimeMillis();
        QueryPerformance performance = new QueryPerformance();
        
        try {
            // 执行EXPLAIN分析
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("EXPLAIN " + sql)) {
                
                List<String> explainResults = new ArrayList<>();
                while (rs.next()) {
                    String explainLine = "";
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        explainLine += rs.getString(i) + " ";
                    }
                    explainResults.add(explainLine.trim());
                }
                performance.explainResults = explainResults;
            }
            
            // 检查是否使用了索引
            performance.usesIndex = checkIndexUsage(performance.explainResults);
            
            // 检查是否进行了全表扫描
            performance.fullTableScan = checkFullTableScan(performance.explainResults);
            
        } catch (SQLException e) {
            logger.error("查询性能分析失败", e);
        }
        
        performance.executionTime = System.currentTimeMillis() - startTime;
        return performance;
    }
    
    /**
     * 检查是否使用了索引
     */
    private static boolean checkIndexUsage(List<String> explainResults) {
        for (String line : explainResults) {
            String lowerLine = line.toLowerCase();
            if (lowerLine.contains("using index") || lowerLine.contains("where") && 
                !lowerLine.contains("all")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查是否进行了全表扫描
     */
    private static boolean checkFullTableScan(List<String> explainResults) {
        for (String line : explainResults) {
            String lowerLine = line.toLowerCase();
            if (lowerLine.contains("type: all") || lowerLine.contains("scan")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 批量查询函数接口
     */
    @FunctionalInterface
    public interface BatchQueryFunction<T> {
        List<T> query(List<Integer> ids) throws SQLException;
    }
    
    /**
     * 查询性能结果
     */
    public static class QueryPerformance {
        public long executionTime;
        public List<String> explainResults;
        public boolean usesIndex;
        public boolean fullTableScan;
        
        public List<String> getOptimizationSuggestions() {
            List<String> suggestions = new ArrayList<>();
            
            if (fullTableScan) {
                suggestions.add("警告：查询使用了全表扫描，建议添加合适的索引");
            }
            
            if (!usesIndex && !fullTableScan) {
                suggestions.add("建议：添加索引以提高查询性能");
            }
            
            if (executionTime > 1000) {
                suggestions.add("警告：查询执行时间过长（" + executionTime + "ms），建议优化");
            }
            
            if (suggestions.isEmpty()) {
                suggestions.add("查询性能良好");
            }
            
            return suggestions;
        }
    }
}