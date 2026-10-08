package com.cashier.service;

import com.cashier.dao.DAOFactory;
import com.cashier.model.BackupRecord;
import com.cashier.model.BackupConfig;
import com.cashier.exception.DatabaseException;
import com.cashier.i18n.I18nManager;
import com.cashier.util.DatabaseManager;
import com.cashier.api.sync.SyncManager;
import com.cashier.api.sync.SyncEventType;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

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
    private static final Logger logger = LoggerFactoryUtil.getLogger(BackupService.class);

    private static volatile BackupService instance;
    private static volatile BackupConfig config;
    private ScheduledExecutorService scheduler;

    private BackupService() {}

    public static synchronized BackupService getInstance() {
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
            DAOFactory.getInstance().getBackupDAO().createTable();
            config = DAOFactory.getInstance().getBackupDAO().getConfig();

            // 创建备份目录
            if (config.localBackupPath != null) {
                Files.createDirectories(Paths.get(config.localBackupPath));
            }

            logger.info("备份服务初始化成功");
        } catch (Exception e) {
            logger.error("备份服务初始化失败", e);
            throw new DatabaseException("备份服务初始化失败", DatabaseException.DbErrorType.CONNECTION_FAILED, e);
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
        DAOFactory.getInstance().getBackupDAO().insert(record);

        // 开始备份
        record.startTime = new Date();
        DAOFactory.getInstance().getBackupDAO().updateStatus(record.backupId, BackupRecord.BackupStatus.RUNNING);

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
                DAOFactory.getInstance().getBackupDAO().updateStatus(record.backupId, BackupRecord.BackupStatus.UPLOADING);
                String remotePath = uploadToCloud(record, backupFile);
                record.remotePath = remotePath;
            }

            // 完成
            record.finishTime = new Date();
            record.calculateDuration();
            DAOFactory.getInstance().getBackupDAO().updateFinish(record.backupId, localPath, record.remotePath,
                record.fileSize, record.checksum, BackupRecord.BackupStatus.SUCCESS, null);

            // 广播备份成功事件
            SyncManager.getInstance().broadcastSyncEvent(SyncEventType.BACKUP_SUCCESS,
                Map.of("backupId", record.backupId, "fileSize", record.fileSize));

            logger.info("备份完成: {} - {} bytes", record.backupId, record.fileSize);

        } catch (Exception e) {
            logger.error("备份失败: {}", record.backupId, e);

            DAOFactory.getInstance().getBackupDAO().updateFinish(record.backupId, null, null, 0, null,
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
                    List<String> failedFull = addFilesBackup(zos, "data/");
                    if (!failedFull.isEmpty()) {
                        logger.warn("完整备份中有 {} 个文件备份失败", failedFull.size());
                    }
                    addConfigBackup(zos);
                    break;

                case DATABASE:
                    addDatabaseBackup(zos);
                    break;

                case FILES:
                    List<String> failedFiles = addFilesBackup(zos, "data/");
                    if (!failedFiles.isEmpty()) {
                        logger.warn("文件备份中有 {} 个文件备份失败，备份可能不完整", failedFiles.size());
                    }
                    break;

                case CONFIG:
                    addConfigBackup(zos);
                    break;

                case LOGS:
                    addLogsBackup(zos);
                    break;

                default:
                    logger.warn("未知备份内容类型: {}", record.contentType);
                    break;
            }
        }

        if (!Files.exists(zipPath) || Files.size(zipPath) == 0) {
            throw new IOException(I18nManager.getInstance().get("service.backup_file_empty", zipPath));
        }

        logger.debug("备份文件创建: {}", zipPath);
        return zipPath.toString();
    }

    /**
     * 添加数据库备份
     */
    private static void addDatabaseBackup(ZipOutputStream zos) throws IOException {
        // 创建临时文件用于 SQL 备份
        File tempSqlFile = File.createTempFile("cashier_backup_", ".sql");

        try {
            // 使用 DatabaseManager 进行 MySQL 备份
            boolean success = com.cashier.util.DatabaseManager.backup(tempSqlFile);

            if (success && tempSqlFile.exists() && tempSqlFile.length() > 0) {
                addToZip(zos, "database/backup.sql", tempSqlFile);
            } else {
                throw new IOException(I18nManager.getInstance().get("service.backup_sql_export_failed"));
            }
        } finally {
            // 删除临时文件
            if (tempSqlFile.exists()) {
                tempSqlFile.delete();
            }
        }
    }

    /**
     * 添加文件备份，返回备份失败的文件路径列表
     */
    private static List<String> addFilesBackup(ZipOutputStream zos, String prefix) throws IOException {
        List<String> failedFiles = new ArrayList<>();

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
                        failedFiles.add(path.toString());
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
                        failedFiles.add(path.toString());
                    }
                });
        }

        if (!failedFiles.isEmpty()) {
            logger.warn("文件备份完成，但有 {} 个文件备份失败: {}", failedFiles.size(), failedFiles);
        }
        return failedFiles;
    }

    /**
     * 添加配置备份
     */
    private static void addConfigBackup(ZipOutputStream zos) throws IOException {
        // 备份配置文件
        Path configDir = Paths.get("config");

        if (Files.exists(configDir)) {
            Files.walk(configDir)
                .filter(path -> !Files.isDirectory(path) &&
                    (path.toString().endsWith(".properties") ||
                     path.toString().endsWith(".yaml") ||
                     path.toString().endsWith(".json")))
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
        if (record.target == BackupRecord.BackupTarget.LOCAL) {
            return null;
        }

        throw new IOException("云备份目标 " + record.target.getDisplayName() + " 尚未接入真实上传适配器，已阻止模拟成功");
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
        BackupRecord record = DAOFactory.getInstance().getBackupDAO().findById(backupId);

        File backupFile = validateRestorableBackup(backupId, record);
        if (backupFile == null) {
            return false;
        }

        if (!verifyBackupChecksum(backupId, record, backupFile)) {
            return false;
        }

        Path workspaceRoot = Paths.get("").toAbsolutePath().normalize();
        Path tempRestoreDir = Files.createTempDirectory("cashier_restore_");

        try {
            Path databaseBackupFile = extractBackupArchive(backupFile, workspaceRoot, tempRestoreDir);
            if (!restoreDatabaseBackup(backupId, databaseBackupFile)) {
                return false;
            }
        } finally {
            deleteDirectoryQuietly(tempRestoreDir);
        }

        broadcastBackupRestored(backupId);
        logger.info("备份恢复完成: {}", backupId);
        return true;
    }

    private static File validateRestorableBackup(String backupId, BackupRecord record) {
        if (record == null || !record.status.isSuccess()) {
            logger.warn("无法恢复备份: {}", backupId);
            return null;
        }
        if (record.localPath == null || record.localPath.isBlank()) {
            logger.warn("备份文件路径为空: {}", backupId);
            return null;
        }

        File backupFile = new File(record.localPath);
        if (!backupFile.exists()) {
            logger.warn("备份文件不存在: {}", record.localPath);
            return null;
        }
        return backupFile;
    }

    private static boolean verifyBackupChecksum(String backupId, BackupRecord record, File backupFile)
            throws IOException {
        if (record.checksum == null || record.checksum.isBlank()) {
            return true;
        }
        try {
            String actualChecksum = calculateChecksum(backupFile);
            if (!record.checksum.equalsIgnoreCase(actualChecksum)) {
                logger.error("备份校验失败: {}, expected={}, actual={}", backupId, record.checksum, actualChecksum);
                return false;
            }
            return true;
        } catch (Exception e) {
            throw new IOException("备份校验失败: " + backupId, e);
        }
    }

    private static Path extractBackupArchive(File backupFile, Path workspaceRoot, Path tempRestoreDir)
            throws IOException {
        Path databaseBackupFile = null;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                databaseBackupFile = extractBackupEntry(zis, entry, workspaceRoot, tempRestoreDir, databaseBackupFile);
                zis.closeEntry();
            }
        }
        return databaseBackupFile;
    }

    private static Path extractBackupEntry(
            ZipInputStream zis,
            ZipEntry entry,
            Path workspaceRoot,
            Path tempRestoreDir,
            Path databaseBackupFile) throws IOException {
        String entryName = entry.getName();
        if (entry.isDirectory()) {
            Files.createDirectories(resolveZipEntryPath(workspaceRoot, entryName));
            return databaseBackupFile;
        }

        Path destPath = "database/backup.sql".equals(entryName)
            ? tempRestoreDir.resolve("backup.sql").normalize()
            : resolveZipEntryPath(workspaceRoot, entryName);
        Files.createDirectories(destPath.getParent());
        Files.copy(zis, destPath, StandardCopyOption.REPLACE_EXISTING);
        return "database/backup.sql".equals(entryName) ? destPath : databaseBackupFile;
    }

    private static boolean restoreDatabaseBackup(String backupId, Path databaseBackupFile) throws IOException {
        if (databaseBackupFile == null || !Files.exists(databaseBackupFile)) {
            return true;
        }
        boolean databaseRestored = DatabaseManager.restore(databaseBackupFile.toFile());
        if (!databaseRestored) {
            logger.error("数据库备份恢复失败: {}", backupId);
            return false;
        }
        return true;
    }

    private static void broadcastBackupRestored(String backupId) {
        SyncManager.getInstance().broadcastSyncEvent(SyncEventType.BACKUP_RESTORED,
            Map.of("backupId", backupId));
    }

    static Path resolveZipEntryPath(Path targetRoot, String entryName) throws IOException {
        Path normalizedRoot = targetRoot.toAbsolutePath().normalize();
        Path normalizedPath = normalizedRoot.resolve(entryName).normalize();
        if (!normalizedPath.startsWith(normalizedRoot)) {
            throw new IOException("备份文件包含非法路径: " + entryName);
        }
        return normalizedPath;
    }

    private static void deleteDirectoryQuietly(Path directory) {
        try {
            if (directory == null || !Files.exists(directory)) {
                return;
            }

            Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        logger.debug("清理临时恢复目录失败: {}", path, e);
                    }
                });
        } catch (IOException e) {
            logger.debug("清理临时恢复目录失败: {}", directory, e);
        }
    }

    /**
     * 清理过期备份
     */
    public static int cleanupExpiredBackups() throws SQLException, IOException {
        int retentionDays = config.retentionDays;
        int deleted = DAOFactory.getInstance().getBackupDAO().deleteExpired(retentionDays);

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
            config = DAOFactory.getInstance().getBackupDAO().getConfig();
        }
        return config;
    }

    /**
     * 更新配置
     */
    public static void updateConfig(BackupConfig newConfig) throws SQLException {
        DAOFactory.getInstance().getBackupDAO().saveConfig(newConfig);
        config = newConfig;
        logger.info("备份配置已更新");
    }

    /**
     * 启动自动备份服务
     */
    public void start() {
        try {
            init();
            config = DAOFactory.getInstance().getBackupDAO().getConfig();

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
     * 把设置页的「自动备份 / 备份频率」落到 {@code backup_config} 并**立即重启调度器**。
     *
     * <p>备份配置的唯一权威是 {@code backup_config}（备份页与 REST API 都读它），而设置页此前
     * 只把 autoBackup/backupFrequency 写进 settings 表、没有任何读取方 —— 管理员以为开了自动备份，
     * 调度器其实还在按旧配置跑（2026-10 审计 F6）。这里把两边接上：写配置 + 重启。</p>
     *
     * @param autoBackupEnabled 设置页「自动备份」勾选状态
     * @param frequency         设置页「备份频率」文案（中文或英文，落库原值）
     */
    public static void applyScheduleFromSettings(boolean autoBackupEnabled, String frequency) {
        try {
            BackupConfig current = DAOFactory.getInstance().getBackupDAO().getConfig();
            BackupConfig updated = current != null ? current : new BackupConfig();
            updated.autoBackupEnabled = autoBackupEnabled;
            int hours = intervalHoursForFrequency(frequency);
            if (hours > 0) {
                updated.backupIntervalHours = hours;
            }
            updateConfig(updated);

            // 重启调度器让新周期立即生效（start() 会重新从库里读配置）
            BackupService service = getInstance();
            service.stop();
            service.start();
            logger.info("备份设置已生效: 自动备份={}, 周期={} 小时", autoBackupEnabled, updated.backupIntervalHours);
        } catch (Exception e) {
            logger.error("应用备份设置失败", e);
        }
    }

    /**
     * 备份频率文案 → 小时数。文案来自语言包（{@code settings.backup_daily/weekly/monthly}）：
     * 简繁为「每天/每周/每月」，英文为 Daily/Weekly/Monthly。
     *
     * @return 小时数；无法识别返回 0（调用方保持原周期不动）
     */
    static int intervalHoursForFrequency(String frequency) {
        if (frequency == null || frequency.isBlank()) {
            return 0;
        }
        return switch (frequency.trim()) {
            case "每天", "每日", "Daily" -> 24;
            case "每周", "每週", "Weekly" -> 24 * 7;
            case "每月", "Monthly" -> 24 * 30;
            default -> 0;
        };
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
                DAOFactory.getInstance().getBackupDAO().updateLastBackupTime(new Date());

                // 清理过期备份
                cleanupExpiredBackups();
            }
        } catch (Exception e) {
            logger.error("自动备份失败", e);
        }
    }
}
