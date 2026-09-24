package com.cashier.api.controller;

import com.cashier.dao.DAOFactory;
import com.cashier.model.BackupRecord;
import com.cashier.model.BackupConfig;
import com.cashier.service.BackupService;
import com.cashier.api.sync.SyncManager;
import com.cashier.api.sync.SyncEventType;
import com.cashier.util.LoggerFactoryUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 云备份 REST API 控制器
 */
public class BackupApiController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(BackupApiController.class);
    private static final String BACKUP_ID_FIELD = "backupId";
    private static final String CONTENT_TYPE_FIELD = "contentType";
    private static final int DEFAULT_BACKUP_LIST_LIMIT = 20;
    private static final int MAX_BACKUP_LIST_LIMIT = 200;
    private static final String DEFAULT_BACKUP_DIR = "backups";
    
    /**
     * 执行备份
     * POST /api/backup/execute
     * Body: { "contentType": "FULL", "target": "LOCAL", "operator": "admin" }
     */
    public static void executeBackup(Context ctx) {
        try {
            Map<?, ?> body = ApiRequest.parse(ctx, Map.class);
            if (body == null) {
                ctx.status(400).json(Map.of(
                    "success", false,
                    "error", "请求体为空或字段不合法"
                ));
                return;
            }
            
            String contentTypeStr = getString(body, CONTENT_TYPE_FIELD, "FULL");
            String targetStr = getString(body, "target", "LOCAL");
            String operator = getString(body, "operator", "system");
            
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
                "error", "备份失败"
            ));
        }
    }
    
    /**
     * 查询备份记录列表
     * GET /api/backup/list?limit=20
     */
    public static void listBackups(Context ctx) {
        int requestedLimit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(DEFAULT_BACKUP_LIST_LIMIT);
        int limit = Math.max(1, Math.min(requestedLimit, MAX_BACKUP_LIST_LIMIT));
        
        try {
            List<BackupRecord> records = DAOFactory.getInstance().getBackupDAO().findRecent(limit);
            
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
                "error", "查询失败"
            ));
        }
    }
    
    /**
     * 查询备份详情
     * GET /api/backup/:backupId
     */
    public static void getBackup(Context ctx) {
        String backupId = ctx.pathParam(BACKUP_ID_FIELD);
        
        try {
            BackupRecord record = DAOFactory.getInstance().getBackupDAO().findById(backupId);
            
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
                "error", "查询失败"
            ));
        }
    }
    
    /**
     * 恢复备份
     * POST /api/backup/:backupId/restore
     */
    public static void restoreBackup(Context ctx) {
        String backupId = ctx.pathParam(BACKUP_ID_FIELD);
        
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
                "error", "恢复失败"
            ));
        }
    }
    
    /**
     * 下载备份文件
     * GET /api/backup/:backupId/download
     */
    public static void downloadBackup(Context ctx) {
        String backupId = ctx.pathParam(BACKUP_ID_FIELD);
        
        try {
            BackupRecord record = DAOFactory.getInstance().getBackupDAO().findById(backupId);
            
            if (record == null || record.localPath == null) {
                ctx.status(404).json(Map.of(
                    "success", false,
                    "error", "备份文件不存在"
                ));
                return;
            }
            
            File file = resolveDownloadableBackupFile(record);
            
            if (!file.exists() || !file.isFile()) {
                ctx.status(404).json(Map.of(
                    "success", false,
                    "error", "文件不存在"
                ));
                return;
            }
            
            // 返回文件
            ctx.header("Content-Disposition", "attachment; filename=\"" + sanitizeDownloadFileName(record.fileName) + "\"");
            ctx.header("Content-Type", "application/zip");
            ctx.result(new FileInputStream(file));
            
        } catch (SQLException e) {
            logger.error("下载备份失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "下载失败"
            ));
        } catch (FileNotFoundException e) {
            ctx.status(404).json(Map.of(
                "success", false,
                "error", "文件不存在"
            ));
        }
    }

    private static File resolveDownloadableBackupFile(BackupRecord record) throws SQLException, FileNotFoundException {
        BackupConfig config = DAOFactory.getInstance().getBackupDAO().getConfig();
        Path backupRoot = Path.of(
            config.localBackupPath == null || config.localBackupPath.isBlank()
                ? DEFAULT_BACKUP_DIR
                : config.localBackupPath
        ).toAbsolutePath().normalize();
        Path backupFile = Path.of(record.localPath).toAbsolutePath().normalize();

        if (!backupFile.startsWith(backupRoot) || !Files.isRegularFile(backupFile)) {
            logger.warn("拒绝下载备份目录外文件: backupId={}, path={}", record.backupId, record.localPath);
            throw new FileNotFoundException("备份文件不存在");
        }
        return backupFile.toFile();
    }

    private static String sanitizeDownloadFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "backup.zip";
        }
        String sanitized = fileName.replaceAll("[\\\\/\\r\\n\"]", "_");
        return sanitized.isBlank() ? "backup.zip" : sanitized;
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
                "error", "清理失败"
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
                "error", "获取失败"
            ));
        }
    }
    
    /**
     * 更新备份配置
     * PUT /api/backup/config
     */
    public static void updateConfig(Context ctx) {
        try {
            Map<?, ?> body = ApiRequest.parse(ctx, Map.class);
            if (body == null) {
                ctx.status(400).json(Map.of(
                    "success", false,
                    "error", "请求体为空或字段不合法"
                ));
                return;
            }
            BackupConfig config = BackupService.getConfig();
            applyBasicBackupConfig(body, config);
            applyAliyunConfig(body, config);
            applyTencentConfig(body, config);
            BackupService.updateConfig(config);
            
            ctx.json(Map.of(
                "success", true,
                "message", "备份配置已更新"
            ));
            
        } catch (Exception e) {
            logger.error("更新备份配置失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "更新失败"
            ));
        }
    }

    private static void applyBasicBackupConfig(Map<?, ?> body, BackupConfig config) {
        if (body.containsKey("autoBackupEnabled")) {
            config.autoBackupEnabled = Boolean.parseBoolean(String.valueOf(body.get("autoBackupEnabled")));
        }
        if (body.containsKey("target")) {
            config.target = BackupRecord.BackupTarget.fromString(getString(body, "target", null));
        }
        if (body.containsKey(CONTENT_TYPE_FIELD)) {
            config.contentType = BackupRecord.BackupContentType.valueOf(getString(body, CONTENT_TYPE_FIELD, null));
        }
        if (body.containsKey("backupIntervalHours")) {
            config.backupIntervalHours = getInt(body, "backupIntervalHours", config.backupIntervalHours);
        }
        if (body.containsKey("retentionDays")) {
            config.retentionDays = getInt(body, "retentionDays", config.retentionDays);
        }
        if (body.containsKey("maxBackupCount")) {
            config.maxBackupCount = getInt(body, "maxBackupCount", config.maxBackupCount);
        }
        if (body.containsKey("localBackupPath")) {
            config.localBackupPath = getString(body, "localBackupPath", null);
        }
    }

    private static void applyAliyunConfig(Map<?, ?> body, BackupConfig config) {
        config.aliyunEndpoint = getStringIfPresent(body, "aliyunEndpoint", config.aliyunEndpoint);
        config.aliyunBucket = getStringIfPresent(body, "aliyunBucket", config.aliyunBucket);
        config.aliyunAccessKey = getStringIfPresent(body, "aliyunAccessKey", config.aliyunAccessKey);
        config.aliyunSecretKey = getStringIfPresent(body, "aliyunSecretKey", config.aliyunSecretKey);
    }

    private static void applyTencentConfig(Map<?, ?> body, BackupConfig config) {
        config.tencentRegion = getStringIfPresent(body, "tencentRegion", config.tencentRegion);
        config.tencentBucket = getStringIfPresent(body, "tencentBucket", config.tencentBucket);
        config.tencentSecretId = getStringIfPresent(body, "tencentSecretId", config.tencentSecretId);
        config.tencentSecretKey = getStringIfPresent(body, "tencentSecretKey", config.tencentSecretKey);
    }

    private static String getStringIfPresent(Map<?, ?> body, String key, String currentValue) {
        return body.containsKey(key) ? getString(body, key, null) : currentValue;
    }
    
    /**
     * 获取备份统计
     * GET /api/backup/stats
     */
    public static void getStats(Context ctx) {
        try {
            int totalBackups = DAOFactory.getInstance().getBackupDAO().countBackups();
            List<BackupRecord> successful = DAOFactory.getInstance().getBackupDAO().findSuccessful();
            
            long totalSize = successful.stream()
                .mapToLong(r -> r.fileSize)
                .sum();
            
            BackupConfig config = BackupService.getConfig();

            Map<String, Object> data = new HashMap<>();
            data.put("totalBackups", totalBackups);
            data.put("successfulBackups", successful.size());
            data.put("totalSizeBytes", totalSize);
            data.put("totalSizeFormatted", formatSize(totalSize));
            data.put("autoBackupEnabled", config.autoBackupEnabled);
            data.put("nextBackupTime", config.nextBackupTime != null ? config.nextBackupTime.toString() : null);
            data.put("retentionDays", config.retentionDays);
            
            ctx.json(Map.of(
                "success", true,
                "data", data
            ));
            
        } catch (SQLException e) {
            logger.error("获取备份统计失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "获取失败"
            ));
        }
    }
    
    /**
     * 构建备份记录数据（静态方法用于方法引用）
     */
    public static Map<String, Object> toBackupRecordData(BackupRecord record) {
        Map<String, Object> data = new HashMap<>();
        data.put(BACKUP_ID_FIELD, record.backupId);
        data.put("backupType", record.backupType.getDisplayName());
        data.put("target", record.target.getDisplayName());
        data.put(CONTENT_TYPE_FIELD, record.contentType.getDisplayName());
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
        data.put(CONTENT_TYPE_FIELD, config.contentType.getDisplayName());
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

    private static String getString(Map<?, ?> body, String key, String defaultValue) {
        Object value = body.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private static int getInt(Map<?, ?> body, String key, int defaultValue) {
        Object value = body.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            return Integer.parseInt(value.toString());
        }
        return defaultValue;
    }
}
