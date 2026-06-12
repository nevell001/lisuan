package com.cashier.dao;

import com.cashier.model.BackupRecord;
import com.cashier.model.BackupConfig;
import com.cashier.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 备份记录数据访问层
 */
public class BackupDAO {
    private static final Logger logger = LoggerFactory.getLogger(BackupDAO.class);

    /**
     * 创建备份表
     */
    public static void createTable() throws SQLException {
        String backupSql = """
            CREATE TABLE IF NOT EXISTS backup_records (
                backup_id VARCHAR(50) PRIMARY KEY,
                backup_type VARCHAR(20),
                target VARCHAR(20),
                file_name VARCHAR(100),
                local_path VARCHAR(200),
                remote_path VARCHAR(200),
                file_size BIGINT,
                status VARCHAR(20),
                create_time DATETIME,
                start_time DATETIME,
                finish_time DATETIME,
                duration_seconds INT,
                content_type VARCHAR(20),
                scope VARCHAR(20),
                operator VARCHAR(50),
                remark VARCHAR(200),
                error_message VARCHAR(500),
                checksum VARCHAR(50),
                auto_backup BOOLEAN
            )
            """;

        String configSql = """
            CREATE TABLE IF NOT EXISTS backup_config (
                id INT PRIMARY KEY,
                auto_backup_enabled BOOLEAN,
                target VARCHAR(20),
                content_type VARCHAR(20),
                backup_interval_hours INT,
                retention_days INT,
                max_backup_count INT,
                last_backup_time DATETIME,
                next_backup_time DATETIME,
                aliyun_endpoint VARCHAR(100),
                aliyun_bucket VARCHAR(50),
                aliyun_access_key VARCHAR(100),
                aliyun_secret_key VARCHAR(100),
                tencent_region VARCHAR(50),
                tencent_bucket VARCHAR(50),
                tencent_secret_id VARCHAR(100),
                tencent_secret_key VARCHAR(100),
                qiniu_domain VARCHAR(100),
                qiniu_bucket VARCHAR(50),
                qiniu_access_key VARCHAR(100),
                qiniu_secret_key VARCHAR(100),
                aws_region VARCHAR(50),
                aws_bucket VARCHAR(50),
                aws_access_key VARCHAR(100),
                aws_secret_key VARCHAR(100),
                ftp_host VARCHAR(100),
                ftp_port INT,
                ftp_user VARCHAR(50),
                ftp_password VARCHAR(100),
                ftp_path VARCHAR(100),
                webdav_url VARCHAR(200),
                webdav_user VARCHAR(50),
                webdav_password VARCHAR(100),
                webdav_path VARCHAR(100),
                local_backup_path VARCHAR(100),
                create_time DATETIME,
                update_time DATETIME
            )
            """;

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(backupSql);
            stmt.execute(configSql);
            logger.info("备份表创建成功");
        }
    }

    /**
     * 插入备份记录
     */
    public static boolean insert(BackupRecord record) throws SQLException {
        String sql = """
            INSERT INTO backup_records (
                backup_id, backup_type, target, file_name, local_path, remote_path,
                file_size, status, create_time, start_time, finish_time, duration_seconds,
                content_type, scope, operator, remark, error_message, checksum, auto_backup
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, record.backupId);
            pstmt.setString(2, record.backupType != null ? record.backupType.name() : "MANUAL");
            pstmt.setString(3, record.target != null ? record.target.name() : "LOCAL");
            pstmt.setString(4, record.fileName);
            pstmt.setString(5, record.localPath);
            pstmt.setString(6, record.remotePath);
            pstmt.setLong(7, record.fileSize);
            pstmt.setString(8, record.status != null ? record.status.name() : "CREATED");
            pstmt.setTimestamp(9, record.createTime != null ? new Timestamp(record.createTime.getTime()) : null);
            pstmt.setTimestamp(10, record.startTime != null ? new Timestamp(record.startTime.getTime()) : null);
            pstmt.setTimestamp(11, record.finishTime != null ? new Timestamp(record.finishTime.getTime()) : null);
            pstmt.setInt(12, record.durationSeconds);
            pstmt.setString(13, record.contentType != null ? record.contentType.name() : "FULL");
            pstmt.setString(14, record.scope != null ? record.scope.name() : "FULL");
            pstmt.setString(15, record.operator);
            pstmt.setString(16, record.remark);
            pstmt.setString(17, record.errorMessage);
            pstmt.setString(18, record.checksum);
            pstmt.setBoolean(19, record.autoBackup);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新备份状态
     */
    public static boolean updateStatus(String backupId, BackupRecord.BackupStatus status) throws SQLException {
        String sql = "UPDATE backup_records SET status = ? WHERE backup_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status.name());
            pstmt.setString(2, backupId);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新备份完成信息
     */
    public static boolean updateFinish(String backupId, String localPath, String remotePath,
                                        long fileSize, String checksum, BackupRecord.BackupStatus status,
                                        String errorMessage) throws SQLException {
        String sql = """
            UPDATE backup_records SET
                local_path = ?, remote_path = ?, file_size = ?, checksum = ?,
                status = ?, finish_time = ?, duration_seconds = ?, error_message = ?
            WHERE backup_id = ?
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, localPath);
            pstmt.setString(2, remotePath);
            pstmt.setLong(3, fileSize);
            pstmt.setString(4, checksum);
            pstmt.setString(5, status.name());
            pstmt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(7, 0); // duration will be calculated
            pstmt.setString(8, errorMessage);
            pstmt.setString(9, backupId);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据ID查询
     */
    public static BackupRecord findById(String backupId) throws SQLException {
        String sql = "SELECT * FROM backup_records WHERE backup_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, backupId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBackupRecord(rs);
                }
            }
        }
        return null;
    }

    /**
     * 查询最近的备份记录
     */
    public static List<BackupRecord> findRecent(int limit) throws SQLException {
        String sql = "SELECT * FROM backup_records ORDER BY create_time DESC LIMIT ?";
        List<BackupRecord> records = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSetToBackupRecord(rs));
                }
            }
        }
        return records;
    }

    /**
     * 查询成功的备份记录
     */
    public static List<BackupRecord> findSuccessful() throws SQLException {
        String sql = "SELECT * FROM backup_records WHERE status = 'SUCCESS' ORDER BY create_time DESC";
        List<BackupRecord> records = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                records.add(mapResultSetToBackupRecord(rs));
            }
        }
        return records;
    }

    /**
     * 删除过期备份记录
     */
    public static int deleteExpired(int retentionDays) throws SQLException {
        String sql = "DELETE FROM backup_records WHERE create_time < DATE_SUB(NOW(), INTERVAL ? DAY)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, retentionDays);
            return pstmt.executeUpdate();
        }
    }

    /**
     * 统计备份数量
     */
    public static int countBackups() throws SQLException {
        String sql = "SELECT COUNT(*) FROM backup_records";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    // ========== 配置操作 ==========

    /**
     * 获取备份配置
     */
    public static BackupConfig getConfig() throws SQLException {
        String sql = "SELECT * FROM backup_config WHERE id = 1";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return mapResultSetToBackupConfig(rs);
            }
        }

        // 返回默认配置
        BackupConfig config = new BackupConfig();
        saveConfig(config);
        return config;
    }

    /**
     * 保存备份配置
     */
    public static boolean saveConfig(BackupConfig config) throws SQLException {
        // MySQL 使用 INSERT ... ON DUPLICATE KEY UPDATE
        String sql = """
            INSERT INTO backup_config (
                id, auto_backup_enabled, target, content_type, backup_interval_hours,
                retention_days, max_backup_count, local_backup_path, update_time
            ) VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                auto_backup_enabled = VALUES(auto_backup_enabled),
                target = VALUES(target),
                content_type = VALUES(content_type),
                backup_interval_hours = VALUES(backup_interval_hours),
                retention_days = VALUES(retention_days),
                max_backup_count = VALUES(max_backup_count),
                local_backup_path = VALUES(local_backup_path),
                update_time = VALUES(update_time)
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBoolean(1, config.autoBackupEnabled);
            pstmt.setString(2, config.target != null ? config.target.name() : "LOCAL");
            pstmt.setString(3, config.contentType != null ? config.contentType.name() : "FULL");
            pstmt.setInt(4, config.backupIntervalHours);
            pstmt.setInt(5, config.retentionDays);
            pstmt.setInt(6, config.maxBackupCount);
            pstmt.setString(7, config.localBackupPath);
            pstmt.setTimestamp(8, new Timestamp(System.currentTimeMillis()));

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新最后备份时间
     */
    public static boolean updateLastBackupTime(Date lastBackupTime) throws SQLException {
        String sql = "UPDATE backup_config SET last_backup_time = ?, next_backup_time = ? WHERE id = 1";

        Date nextTime = new Date(lastBackupTime.getTime() + 24 * 60 * 60 * 1000L);

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, new Timestamp(lastBackupTime.getTime()));
            pstmt.setTimestamp(2, new Timestamp(nextTime.getTime()));

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * ResultSet 映射到 BackupRecord
     */
    private static BackupRecord mapResultSetToBackupRecord(ResultSet rs) throws SQLException {
        BackupRecord record = new BackupRecord();
        record.backupId = rs.getString("backup_id");
        record.backupType = BackupRecord.BackupType.valueOf(rs.getString("backup_type"));
        record.target = BackupRecord.BackupTarget.valueOf(rs.getString("target"));
        record.fileName = rs.getString("file_name");
        record.localPath = rs.getString("local_path");
        record.remotePath = rs.getString("remote_path");
        record.fileSize = rs.getLong("file_size");
        record.status = BackupRecord.BackupStatus.valueOf(rs.getString("status"));

        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) record.createTime = new Date(createTime.getTime());

        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) record.startTime = new Date(startTime.getTime());

        Timestamp finishTime = rs.getTimestamp("finish_time");
        if (finishTime != null) record.finishTime = new Date(finishTime.getTime());

        record.durationSeconds = rs.getInt("duration_seconds");
        record.contentType = BackupRecord.BackupContentType.valueOf(rs.getString("content_type"));
        record.scope = BackupRecord.BackupScope.valueOf(rs.getString("scope"));
        record.operator = rs.getString("operator");
        record.remark = rs.getString("remark");
        record.errorMessage = rs.getString("error_message");
        record.checksum = rs.getString("checksum");
        record.autoBackup = rs.getBoolean("auto_backup");

        return record;
    }

    /**
     * ResultSet 映射到 BackupConfig
     */
    private static BackupConfig mapResultSetToBackupConfig(ResultSet rs) throws SQLException {
        BackupConfig config = new BackupConfig();
        config.id = rs.getInt("id");
        config.autoBackupEnabled = rs.getBoolean("auto_backup_enabled");
        config.target = BackupRecord.BackupTarget.valueOf(rs.getString("target"));
        config.contentType = BackupRecord.BackupContentType.valueOf(rs.getString("content_type"));
        config.backupIntervalHours = rs.getInt("backup_interval_hours");
        config.retentionDays = rs.getInt("retention_days");
        config.maxBackupCount = rs.getInt("max_backup_count");
        config.localBackupPath = rs.getString("local_backup_path");

        Timestamp lastBackup = rs.getTimestamp("last_backup_time");
        if (lastBackup != null) config.lastBackupTime = new Date(lastBackup.getTime());

        Timestamp nextBackup = rs.getTimestamp("next_backup_time");
        if (nextBackup != null) config.nextBackupTime = new Date(nextBackup.getTime());

        Timestamp updateTime = rs.getTimestamp("update_time");
        if (updateTime != null) config.updateTime = new Date(updateTime.getTime());

        return config;
    }
}
