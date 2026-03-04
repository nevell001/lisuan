package com.cashier.util;

import org.slf4j.Logger;
import java.sql.*;
import java.util.*;

/**
 * 批量操作工具类
 * 提供批量插入、批量更新等功能
 */
public class BatchOperationUtil {
    private static final Logger logger = LoggerFactoryUtil.getLogger(BatchOperationUtil.class);
    
    /**
     * 批量插入
     */
    public static int[] batchInsert(Connection conn, String sql, List<Object[]> params) 
        throws SQLException {
        
        if (params == null || params.isEmpty()) {
            return new int[0];
        }
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // 禁用自动提交
            conn.setAutoCommit(false);
            
            // 添加批次
            for (Object[] param : params) {
                for (int i = 0; i < param.length; i++) {
                    pstmt.setObject(i + 1, param[i]);
                }
                pstmt.addBatch();
            }
            
            // 执行批次
            int[] results = pstmt.executeBatch();
            
            // 提交事务
            conn.commit();
            
            logger.info("批量插入成功: {} 条记录", results.length);
            return results;
            
        } catch (SQLException e) {
            // 回滚事务
            conn.rollback();
            logger.error("批量插入失败", e);
            throw e;
        } finally {
            // 恢复自动提交
            conn.setAutoCommit(true);
        }
    }
    
    /**
     * 批量更新
     */
    public static int[] batchUpdate(Connection conn, String sql, List<Object[]> params) 
        throws SQLException {
        
        if (params == null || params.isEmpty()) {
            return new int[0];
        }
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // 禁用自动提交
            conn.setAutoCommit(false);
            
            // 添加批次
            for (Object[] param : params) {
                for (int i = 0; i < param.length; i++) {
                    pstmt.setObject(i + 1, param[i]);
                }
                pstmt.addBatch();
            }
            
            // 执行批次
            int[] results = pstmt.executeBatch();
            
            // 提交事务
            conn.commit();
            
            logger.info("批量更新成功: {} 条记录", results.length);
            return results;
            
        } catch (SQLException e) {
            // 回滚事务
            conn.rollback();
            logger.error("批量更新失败", e);
            throw e;
        } finally {
            // 恢复自动提交
            conn.setAutoCommit(true);
        }
    }
    
    /**
     * 批量删除
     */
    public static int[] batchDelete(Connection conn, String sql, List<Integer> ids) 
        throws SQLException {
        
        if (ids == null || ids.isEmpty()) {
            return new int[0];
        }
        
        List<Object[]> params = new ArrayList<>();
        for (Integer id : ids) {
            params.add(new Object[]{id});
        }
        
        return batchUpdate(conn, sql, params);
    }
    
    /**
     * 优化的大批量插入（使用LOAD DATA INFILE，仅适用于MySQL）
     */
    public static int optimizedBatchInsert(Connection conn, String tableName, 
                                          List<String> csvLines, char delimiter) 
        throws SQLException {
        
        if (csvLines == null || csvLines.isEmpty()) {
            return 0;
        }
        
        // 生成临时文件路径
        String tempFilePath = "/tmp/batch_insert_" + System.currentTimeMillis() + ".csv";
        
        try {
            // 写入CSV文件
            java.nio.file.Files.write(
                java.nio.file.Paths.get(tempFilePath),
                csvLines
            );
            
            // 执行LOAD DATA INFILE
            String sql = String.format(
                "LOAD DATA LOCAL INFILE '%s' INTO TABLE %s FIELDS TERMINATED BY '%c'",
                tempFilePath, tableName, delimiter
            );
            
            try (Statement stmt = conn.createStatement()) {
                int result = stmt.executeUpdate(sql);
                logger.info("优化批量插入成功: {} 条记录", result);
                return result;
            }
            
        } catch (Exception e) {
            logger.error("优化批量插入失败", e);
            throw new SQLException("优化批量插入失败", e);
        } finally {
            // 删除临时文件
            try {
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tempFilePath));
            } catch (Exception e) {
                logger.warn("删除临时文件失败", e);
            }
        }
    }
    
    /**
     * 事务包装器
     */
    public static <T> T executeInTransaction(Connection conn, TransactionFunction<T> function) 
        throws SQLException {
        
        try {
            conn.setAutoCommit(false);
            
            T result = function.execute();
            
            conn.commit();
            return result;
            
        } catch (SQLException e) {
            conn.rollback();
            logger.error("事务执行失败，已回滚", e);
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
    
    /**
     * 事务函数接口
     */
    @FunctionalInterface
    public interface TransactionFunction<T> {
        T execute() throws SQLException;
    }
}