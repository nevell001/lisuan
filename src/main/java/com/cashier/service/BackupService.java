package com.cashier.service;

import com.cashier.model.BackupRecord;
import com.cashier.model.BackupConfig;
import com.cashier.dao.BackupDAO;
import com.cashier.util.DatabaseManager;
import com.cashier.api.sync.SyncManager;
import com.cashier.api.sync.SyncEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.*;

/**
 * 备份服务
 * 支持本地备份和云存储上传
 */
public class BackupService {
    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);
    
    private static BackupService instance;
    private static BackupConfig config;
    private ScheduledExecutorService scheduler;
    
    private BackupService() {}
    
    public static BackupService getInstance() {
        if (instance == null) {
            instance = new BackupService();
        }
        return instance;
    }
    
    /**
     * 初始化备份服务
     */
    public static void init() {
        try {
            BackupDAO.createTable();
            config = BackupDAO.getConfig();
            
            // 创建备份目录
            if (config.localBackupPath != null) {
                Files.createDirectories(Paths.get(config.localBackupPath));
            }
            
            logger.info("备份服务初始化成功");
        } catch (Exception e) {
            logger.error("备份服务初始化失败", e);
        }
    }
    
    /**
     * 执行备份
     */
    public static BackupRecord executeBackup(BackupRecord.BackupContentType contentType,
                                              BackupRecord.BackupTarget target,
                                              String operator) throws SQLException {
        BackupRecord record = BackupRecord.createManual(contentType, target, operator);
        
        // 保存记录
        BackupDAO.insert(record);
        
        // 开始备份
        record.startTime = new Date();
        BackupDAO.updateStatus(record.backupId, BackupRecord.BackupStatus.RUNNING);
        
        try {
            // 创建备份文件
            String localPath = createBackupFile(record);
            record.localPath = localPath;
            
            // 计算文件大小和校验码
            File backupFile = new File(localPath);
            record.fileSize = backupFile.length();
            record.checksum = calculateChecksum(backupFile);
            
            // 上传到云存储（如果目标不是本地）
            if (target != BackupRecord.BackupTarget.LOCAL) {
                BackupDAO.updateStatus(record.backupId, BackupRecord.BackupStatus.UPLOADING);
                String remotePath = uploadToCloud(record, backupFile);
                record.remotePath = remotePath;
            }
            
            // 完成
            record.finishTime = new Date();
            record.calculateDuration();
            BackupDAO.updateFinish(record.backupId, localPath, record.remotePath, 
                record.fileSize, record.checksum, BackupRecord.BackupStatus.SUCCESS, null);
            
            // 广播备份成功事件
            SyncManager.getInstance().broadcastSyncEvent(SyncEventType.BACKUP_SUCCESS,
                Map.of("backupId", record.backupId, "fileSize", record.fileSize));
            
            logger.info("备份完成: {} - {} bytes", record.backupId, record.fileSize);
            
        } catch (Exception e) {
            logger.error("备份失败: {}", record.backupId, e);
            
            BackupDAO.updateFinish(record.backupId, null, null, 0, null, 
                BackupRecord.BackupStatus.FAILED, e.getMessage());
            
            SyncManager.getInstance().broadcastSyncEvent(SyncEventType.BACKUP_FAILED,
                Map.of("backupId", record.backupId, "error", e.getMessage()));
        }
        
        return record;
    }
    
    /**
     * 创建备份文件
     */
    private static String createBackupFile(BackupRecord record) throws IOException {
        String backupDir = config.localBackupPath != null ? config.localBackupPath : "backups";
        Path backupPath = Paths.get(backupDir);
        
        if (!Files.exists(backupPath)) {
            Files.createDirectories(backupPath);
        }
        
        String fileName = record.fileName;
        Path zipPath = backupPath.resolve(fileName);
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            
            switch (record.contentType) {
                case FULL:
                    // 备份数据库和文件
                    addDatabaseBackup(zos);
                    addFilesBackup(zos, "data/");
                    addConfigBackup(zos);
                    break;
                    
                case DATABASE:
                    addDatabaseBackup(zos);
                    break;
                    
                case FILES:
                    addFilesBackup(zos, "data/");
                    break;
                    
                case CONFIG:
                    addConfigBackup(zos);
                    break;
                    
                case LOGS:
                    addLogsBackup(zos);
                    break;
            }
        }
        
        logger.debug("备份文件创建: {}", zipPath);
        return zipPath.toString();
    }
    
    /**
     * 添加数据库备份
     */
    private static void addDatabaseBackup(ZipOutputStream zos) throws IOException {
        // SQLite数据库文件路径（默认路径）
        String dbPath = "data/cashier.db";
        
        if (Files.exists(Paths.get(dbPath))) {
            File dbFile = new File(dbPath);
            addToZip(zos, "database/cashier.db", dbFile);
        }
    }
    
    /**
     * 添加文件备份
     */
    private static void addFilesBackup(ZipOutputStream zos, String prefix) throws IOException {
        // 备份数据目录下的文件
        Path dataDir = Paths.get("data");
        
        if (Files.exists(dataDir)) {
            Files.walk(dataDir)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    try {
                        String entryName = prefix + dataDir.relativize(path).toString();
                        addToZip(zos, entryName, path.toFile());
                    } catch (IOException e) {
                        logger.warn("备份文件失败: {}", path, e);
                    }
                });
        }
        
        // 备份发票文件
        Path invoiceDir = Paths.get("invoices");
        if (Files.exists(invoiceDir)) {
            Files.walk(invoiceDir)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    try {
                        addToZip(zos, "invoices/" + invoiceDir.relativize(path).toString(), path.toFile());
                    } catch (IOException e) {
                        logger.warn("备份发票文件失败: {}", path, e);
                    }
                });
        }
    }
    
    /**
     * 添加配置备份
     */
    private static void addConfigBackup(ZipOutputStream zos) throws IOException {
        // 备份配置文件
        Path configDir = Paths.get("config");
        
        if (Files.exists(configDir)) {
            Files.walk(configDir)
                .filter(path -> !Files.isDirectory(path) && path.toString().endsWith(".properties") || 
                               path.toString().endsWith(".yaml") || path.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        addToZip(zos, "config/" + configDir.relativize(path).toString(), path.toFile());
                    } catch (IOException e) {
                        logger.warn("备份配置文件失败: {}", path, e);
                    }
                });
        }
    }
    
    /**
     * 添加日志备份
     */
    private static void addLogsBackup(ZipOutputStream zos) throws IOException {
        Path logsDir = Paths.get("logs");
        
        if (Files.exists(logsDir)) {
            Files.walk(logsDir)
                .filter(path -> !Files.isDirectory(path) && path.toString().endsWith(".log"))
                .forEach(path -> {
                    try {
                        addToZip(zos, "logs/" + logsDir.relativize(path).toString(), path.toFile());
                    } catch (IOException e) {
                        logger.warn("备份日志文件失败: {}", path, e);
                    }
                });
        }
    }
    
    /**
     * 添加文件到ZIP
     */
    private static void addToZip(ZipOutputStream zos, String entryName, File file) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
        }
        
        zos.closeEntry();
    }
    
    /**
     * 上传到云存储
     */
    private static String uploadToCloud(BackupRecord record, File backupFile) throws IOException {
        String remotePath;
        
        switch (record.target) {
            case ALIYUN_OSS:
                remotePath = uploadToAliyunOSS(record.fileName, backupFile);
                break;
                
            case TENCENT_COS:
                remotePath = uploadToTencentCOS(record.fileName, backupFile);
                break;
                
            case QINIU:
                remotePath = uploadToQiniu(record.fileName, backupFile);
                break;
                
            case AWS_S3:
                remotePath = uploadToAwsS3(record.fileName, backupFile);
                break;
                
            case FTP:
                remotePath = uploadToFtp(record.fileName, backupFile);
                break;
                
            case WEBDAV:
                remotePath = uploadToWebDAV(record.fileName, backupFile);
                break;
                
            default:
                remotePath = null;
        }
        
        return remotePath;
    }
    
    /**
     * 上传到阿里云OSS（模拟）
     */
    private static String uploadToAliyunOSS(String fileName, File file) throws IOException {
        // 实际项目中需要使用阿里云OSS SDK
        // ossClient.putObject(bucketName, objectKey, file)
        
        // 模拟上传
        logger.info("模拟上传到阿里云OSS: {}", fileName);
        return "https://" + config.aliyunBucket + ".oss-" + config.aliyunRegion + 
               ".aliyuncs.com/backups/" + fileName;
    }
    
    /**
     * 上传到腾讯云COS（模拟）
     */
    private static String uploadToTencentCOS(String fileName, File file) throws IOException {
        // 实际项目使用腾讯云COS SDK
        
        logger.info("模拟上传到腾讯云COS: {}", fileName);
        return "https://" + config.tencentBucket + ".cos." + config.tencentRegion + 
               ".myqcloud.com/backups/" + fileName;
    }
    
    /**
     * 上传到七牛云（模拟）
     */
    private static String uploadToQiniu(String fileName, File file) throws IOException {
        // 实际项目使用七牛云SDK
        
        logger.info("模拟上传到七牛云: {}", fileName);
        return config.qiniuDomain + "/backups/" + fileName;
    }
    
    /**
     * 上传到AWS S3（模拟）
     */
    private static String uploadToAwsS3(String fileName, File file) throws IOException {
        // 实际项目使用AWS SDK
        
        logger.info("模拟上传到AWS S3: {}", fileName);
        return "https://" + config.awsBucket + ".s3." + config.awsRegion + 
               ".amazonaws.com/backups/" + fileName;
    }
    
    /**
     * 上传到FTP（模拟）
     */
    private static String uploadToFtp(String fileName, File file) throws IOException {
        // 实际项目使用FTP客户端
        
        logger.info("模拟上传到FTP: {}", fileName);
        return config.ftpPath + "/" + fileName;
    }
    
    /**
     * 上传到WebDAV（模拟）
     */
    private static String uploadToWebDAV(String fileName, File file) throws IOException {
        // 实际项目使用WebDAV客户端
        
        logger.info("模拟上传到WebDAV: {}", fileName);
        return config.webdavUrl + config.webdavPath + "/" + fileName;
    }
    
    /**
     * 计算文件MD5校验码
     */
    private static String calculateChecksum(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                md.update(buffer, 0, len);
            }
        }
        
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        
        return sb.toString();
    }
    
    /**
     * 恢复备份
     */
    public static boolean restoreBackup(String backupId) throws SQLException, IOException {
        BackupRecord record = BackupDAO.findById(backupId);
        
        if (record == null || !record.status.isSuccess()) {
            logger.warn("无法恢复备份: {}", backupId);
            return false;
        }
        
        File backupFile = new File(record.localPath);
        
        if (!backupFile.exists()) {
            logger.warn("备份文件不存在: {}", record.localPath);
            return false;
        }
        
        // 解压恢复
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path destPath = Paths.get(entry.getName());
                
                if (entry.isDirectory()) {
                    Files.createDirectories(destPath);
                } else {
                    Files.createDirectories(destPath.getParent());
                    Files.copy(zis, destPath, StandardCopyOption.REPLACE_EXISTING);
                }
                
                zis.closeEntry();
            }
        }
        
        // 广播恢复事件
        SyncManager.getInstance().broadcastSyncEvent(SyncEventType.BACKUP_RESTORED,
            Map.of("backupId", backupId));
        
        logger.info("备份恢复完成: {}", backupId);
        return true;
    }
    
    /**
     * 清理过期备份
     */
    public static int cleanupExpiredBackups() throws SQLException, IOException {
        int retentionDays = config.retentionDays;
        int deleted = BackupDAO.deleteExpired(retentionDays);
        
        // 删除对应的文件
        Path backupDir = Paths.get(config.localBackupPath);
        if (Files.exists(backupDir)) {
            long cutoff = System.currentTimeMillis() - retentionDays * 24 * 60 * 60 * 1000L;
            
            Files.walk(backupDir)
                .filter(path -> !Files.isDirectory(path))
                .filter(path -> new File(path.toString()).lastModified() < cutoff)
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        logger.debug("删除过期备份文件: {}", path);
                    } catch (IOException e) {
                        logger.warn("删除文件失败: {}", path, e);
                    }
                });
        }
        
        if (deleted > 0) {
            logger.info("清理过期备份: {} 个", deleted);
        }
        
        return deleted;
    }
    
    /**
     * 获取配置
     */
    public static BackupConfig getConfig() throws SQLException {
        if (config == null) {
            config = BackupDAO.getConfig();
        }
        return config;
    }
    
    /**
     * 更新配置
     */
    public static void updateConfig(BackupConfig newConfig) throws SQLException {
        BackupDAO.saveConfig(newConfig);
        config = newConfig;
        logger.info("备份配置已更新");
    }
    
    /**
     * 启动自动备份服务
     */
    public void start() {
        try {
            init();
            config = BackupDAO.getConfig();
            
            if (config.autoBackupEnabled && config.backupIntervalHours > 0) {
                scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.scheduleAtFixedRate(() -> {
                    try {
                        if (config.needsBackup()) {
                            executeAutoBackup();
                        }
                    } catch (Exception e) {
                        logger.error("自动备份执行失败", e);
                    }
                }, config.backupIntervalHours, config.backupIntervalHours, TimeUnit.HOURS);
                
                logger.info("自动备份服务已启动，周期: {} 小时", config.backupIntervalHours);
            }
        } catch (Exception e) {
            logger.error("启动自动备份服务失败", e);
        }
    }
    
    /**
     * 停止自动备份服务
     */
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
            scheduler = null;
            logger.info("自动备份服务已停止");
        }
    }
    
    /**
     * 执行自动备份
     */
    private void executeAutoBackup() {
        try {
            BackupRecord record = executeBackup(config.contentType, config.target, "system");
            
            if (record.status.isSuccess()) {
                // 更新最后备份时间
                BackupDAO.updateLastBackupTime(new Date());
                
                // 清理过期备份
                cleanupExpiredBackups();
            }
        } catch (Exception e) {
            logger.error("自动备份失败", e);
        }
    }
}