package com.cashier.api.controller;

import com.cashier.model.BackupRecord;
import com.cashier.model.BackupConfig;
import com.cashier.dao.BackupDAO;
import com.cashier.service.BackupService;
import com.cashier.api.sync.SyncManager;
import com.cashier.api.sync.SyncEventType;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 云备份 REST API 控制器
 */
public class BackupApiController {
    private static final Logger logger = LoggerFactory.getLogger(BackupApiController.class);
    
    /**
     * 执行备份
     * POST /api/backup/execute
     * Body: { "contentType": "FULL", "target": "LOCAL", "operator": "admin" }
     */
    public static void executeBackup(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            
            String contentTypeStr = (String) body.getOrDefault("contentType", "FULL");
            String targetStr = (String) body.getOrDefault("target", "LOCAL");
            String operator = (String) body.getOrDefault("operator", "system");
            
            BackupRecord.BackupContentType contentType = 
                BackupRecord.BackupContentType.valueOf(contentTypeStr);
            BackupRecord.BackupTarget target = 
                BackupRecord.BackupTarget.fromString(targetStr);
            
            // 异步执行备份
            BackupRecord record = BackupService.executeBackup(contentType, target, operator);
            
            ctx.json(Map.of(
                "success", true,
                "data", buildBackupRecordData(record),
                "message", record.status.isSuccess() ? "备份成功" : "备份失败"
            ));
            
        } catch (Exception e) {
            logger.error("执行备份失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "备份失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 查询备份记录列表
     * GET /api/backup/list?limit=20
     */
    public static void listBackups(Context ctx) {
        int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(20);
        
        try {
            List<BackupRecord> records = BackupDAO.findRecent(limit);
            
            List<Map<String, Object>> list = records.stream()
                .map(BackupApiController::toBackupRecordData)
                .collect(Collectors.toList());
            
            ctx.json(Map.of(
                "success", true,
                "data", list,
                "total", list.size()
            ));
            
        } catch (SQLException e) {
            logger.error("查询备份列表失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "查询失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 查询备份详情
     * GET /api/backup/:backupId
     */
    public static void getBackup(Context ctx) {
        String backupId = ctx.pathParam("backupId");
        
        try {
            BackupRecord record = BackupDAO.findById(backupId);
            
            if (record == null) {
                ctx.status(404).json(Map.of(
                    "success", false,
                    "error", "备份记录不存在"
                ));
                return;
            }
            
            ctx.json(Map.of(
                "success", true,
                "data", buildBackupRecordData(record)
            ));
            
        } catch (SQLException e) {
            logger.error("查询备份详情失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "查询失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 恢复备份
     * POST /api/backup/:backupId/restore
     */
    public static void restoreBackup(Context ctx) {
        String backupId = ctx.pathParam("backupId");
        
        try {
            boolean success = BackupService.restoreBackup(backupId);
            
            ctx.json(Map.of(
                "success", success,
                "message", success ? "备份恢复成功" : "备份恢复失败"
            ));
            
        } catch (Exception e) {
            logger.error("恢复备份失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "恢复失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 下载备份文件
     * GET /api/backup/:backupId/download
     */
    public static void downloadBackup(Context ctx) {
        String backupId = ctx.pathParam("backupId");
        
        try {
            BackupRecord record = BackupDAO.findById(backupId);
            
            if (record == null || record.localPath == null) {
                ctx.status(404).json(Map.of(
                    "success", false,
                    "error", "备份文件不存在"
                ));
                return;
            }
            
            File file = new File(record.localPath);
            
            if (!file.exists()) {
                ctx.status(404).json(Map.of(
                    "success", false,
                    "error", "文件不存在: " + record.localPath
                ));
                return;
            }
            
            // 返回文件
            ctx.header("Content-Disposition", "attachment; filename=\"" + record.fileName + "\"");
            ctx.header("Content-Type", "application/zip");
            ctx.result(new FileInputStream(file));
            
        } catch (SQLException e) {
            logger.error("下载备份失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "下载失败: " + e.getMessage()
            ));
        } catch (FileNotFoundException e) {
            ctx.status(404).json(Map.of(
                "success", false,
                "error", "文件不存在"
            ));
        }
    }
    
    /**
     * 清理过期备份
     * POST /api/backup/cleanup
     */
    public static void cleanupBackups(Context ctx) {
        try {
            int deleted = BackupService.cleanupExpiredBackups();
            
            ctx.json(Map.of(
                "success", true,
                "data", Map.of("deletedCount", deleted),
                "message", "清理过期备份: " + deleted + " 个"
            ));
            
            if (deleted > 0) {
                SyncManager.getInstance().broadcastSyncEvent(SyncEventType.BACKUP_CLEANED,
                    Map.of("deletedCount", deleted));
            }
            
        } catch (Exception e) {
            logger.error("清理备份失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "清理失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 获取备份配置
     * GET /api/backup/config
     */
    public static void getConfig(Context ctx) {
        try {
            BackupConfig config = BackupService.getConfig();
            
            ctx.json(Map.of(
                "success", true,
                "data", buildBackupConfigData(config)
            ));
            
        } catch (SQLException e) {
            logger.error("获取备份配置失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "获取失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 更新备份配置
     * PUT /api/backup/config
     */
    public static void updateConfig(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            
            BackupConfig config = BackupService.getConfig();
            
            if (body.containsKey("autoBackupEnabled")) {
                config.autoBackupEnabled = Boolean.parseBoolean(body.get("autoBackupEnabled").toString());
            }
            if (body.containsKey("target")) {
                config.target = BackupRecord.BackupTarget.fromString((String) body.get("target"));
            }
            if (body.containsKey("contentType")) {
                config.contentType = BackupRecord.BackupContentType.valueOf((String) body.get("contentType"));
            }
            if (body.containsKey("backupIntervalHours")) {
                config.backupIntervalHours = ((Number) body.get("backupIntervalHours")).intValue();
            }
            if (body.containsKey("retentionDays")) {
                config.retentionDays = ((Number) body.get("retentionDays")).intValue();
            }
            if (body.containsKey("maxBackupCount")) {
                config.maxBackupCount = ((Number) body.get("maxBackupCount")).intValue();
            }
            if (body.containsKey("localBackupPath")) {
                config.localBackupPath = (String) body.get("localBackupPath");
            }
            
            // 云存储配置
            if (body.containsKey("aliyunEndpoint")) config.aliyunEndpoint = (String) body.get("aliyunEndpoint");
            if (body.containsKey("aliyunBucket")) config.aliyunBucket = (String) body.get("aliyunBucket");
            if (body.containsKey("aliyunAccessKey")) config.aliyunAccessKey = (String) body.get("aliyunAccessKey");
            if (body.containsKey("aliyunSecretKey")) config.aliyunSecretKey = (String) body.get("aliyunSecretKey");
            
            if (body.containsKey("tencentRegion")) config.tencentRegion = (String) body.get("tencentRegion");
            if (body.containsKey("tencentBucket")) config.tencentBucket = (String) body.get("tencentBucket");
            if (body.containsKey("tencentSecretId")) config.tencentSecretId = (String) body.get("tencentSecretId");
            if (body.containsKey("tencentSecretKey")) config.tencentSecretKey = (String) body.get("tencentSecretKey");
            
            BackupService.updateConfig(config);
            
            ctx.json(Map.of(
                "success", true,
                "message", "备份配置已更新"
            ));
            
        } catch (Exception e) {
            logger.error("更新备份配置失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "更新失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 获取备份统计
     * GET /api/backup/stats
     */
    public static void getStats(Context ctx) {
        try {
            int totalBackups = BackupDAO.countBackups();
            List<BackupRecord> successful = BackupDAO.findSuccessful();
            
            long totalSize = successful.stream()
                .mapToLong(r -> r.fileSize)
                .sum();
            
            BackupConfig config = BackupService.getConfig();
            
            ctx.json(Map.of(
                "success", true,
                "data", Map.of(
                    "totalBackups", totalBackups,
                    "successfulBackups", successful.size(),
                    "totalSizeBytes", totalSize,
                    "totalSizeFormatted", formatSize(totalSize),
                    "autoBackupEnabled", config.autoBackupEnabled,
                    "nextBackupTime", config.nextBackupTime != null ? config.nextBackupTime.toString() : null,
                    "retentionDays", config.retentionDays
                )
            ));
            
        } catch (SQLException e) {
            logger.error("获取备份统计失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "获取失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 构建备份记录数据（静态方法用于方法引用）
     */
    public static Map<String, Object> toBackupRecordData(BackupRecord record) {
        Map<String, Object> data = new HashMap<>();
        data.put("backupId", record.backupId);
        data.put("backupType", record.backupType.getDisplayName());
        data.put("target", record.target.getDisplayName());
        data.put("contentType", record.contentType.getDisplayName());
        data.put("fileName", record.fileName);
        data.put("fileSize", record.getFormattedFileSize());
        data.put("status", record.status.getDisplayName());
        data.put("createTime", record.createTime != null ? record.createTime.toString() : null);
        data.put("finishTime", record.finishTime != null ? record.finishTime.toString() : null);
        data.put("durationSeconds", record.durationSeconds);
        data.put("checksum", record.checksum);
        data.put("operator", record.operator);
        data.put("autoBackup", record.autoBackup);
        data.put("errorMessage", record.errorMessage);
        return data;
    }
    
    /**
     * 构建备份记录数据
     */
    private static Map<String, Object> buildBackupRecordData(BackupRecord record) {
        return toBackupRecordData(record);
    }
    
    /**
     * 构建备份配置数据（不包含敏感信息）
     */
    private static Map<String, Object> buildBackupConfigData(BackupConfig config) {
        Map<String, Object> data = new HashMap<>();
        data.put("autoBackupEnabled", config.autoBackupEnabled);
        data.put("target", config.target.getDisplayName());
        data.put("contentType", config.contentType.getDisplayName());
        data.put("backupIntervalHours", config.backupIntervalHours);
        data.put("retentionDays", config.retentionDays);
        data.put("maxBackupCount", config.maxBackupCount);
        data.put("localBackupPath", config.localBackupPath);
        data.put("lastBackupTime", config.lastBackupTime != null ? config.lastBackupTime.toString() : null);
        data.put("nextBackupTime", config.nextBackupTime != null ? config.nextBackupTime.toString() : null);
        
        // 云存储配置（隐藏密钥）
        data.put("aliyunBucket", config.aliyunBucket);
        data.put("tencentBucket", config.tencentBucket);
        data.put("qiniuBucket", config.qiniuBucket);
        data.put("awsBucket", config.awsBucket);
        
        return data;
    }
    
    /**
     * 格式化文件大小
     */
    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)) + " MB";
        return (bytes / (1024 * 1024 * 1024)) + " GB";
    }
}