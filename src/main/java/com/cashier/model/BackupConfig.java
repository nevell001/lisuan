package com.cashier.model;

import java.util.Date;

/**
 * 备份配置模型
 */
public class BackupConfig {
    
    /**
     * 配置ID
     */
    public int id;
    
    /**
     * 是否启用自动备份
     */
    public boolean autoBackupEnabled;
    
    /**
     * 备份目标
     */
    public BackupRecord.BackupTarget target;
    
    /**
     * 备份内容类型
     */
    public BackupRecord.BackupContentType contentType;
    
    /**
     * 备份周期（小时）
     */
    public int backupIntervalHours;
    
    /**
     * 保留天数
     */
    public int retentionDays;
    
    /**
     * 最大备份数量
     */
    public int maxBackupCount;
    
    /**
     * 上次备份时间
     */
    public Date lastBackupTime;
    
    /**
     * 下次备份时间
     */
    public Date nextBackupTime;
    
    // ========== 云存储配置 ==========
    
    /**
     * 阿里云OSS配置
     */
    public String aliyunEndpoint;
    public String aliyunBucket;
    public String aliyunAccessKey;
    public String aliyunSecretKey;
    public String aliyunRegion;
    
    /**
     * 腾讯云COS配置
     */
    public String tencentRegion;
    public String tencentBucket;
    public String tencentSecretId;
    public String tencentSecretKey;
    
    /**
     * 七牛云配置
     */
    public String qiniuDomain;
    public String qiniuBucket;
    public String qiniuAccessKey;
    public String qiniuSecretKey;
    
    /**
     * AWS S3配置
     */
    public String awsRegion;
    public String awsBucket;
    public String awsAccessKey;
    public String awsSecretKey;
    
    /**
     * FTP配置
     */
    public String ftpHost;
    public int ftpPort;
    public String ftpUser;
    public String ftpPassword;
    public String ftpPath;
    
    /**
     * WebDAV配置
     */
    public String webdavUrl;
    public String webdavUser;
    public String webdavPassword;
    public String webdavPath;
    
    /**
     * 本地备份路径
     */
    public String localBackupPath;
    
    /**
     * 创建时间
     */
    public Date createTime;
    
    /**
     * 更新时间
     */
    public Date updateTime;
    
    /**
     * 默认构造函数
     */
    public BackupConfig() {
        this.autoBackupEnabled = false;
        this.target = BackupRecord.BackupTarget.LOCAL;
        this.contentType = BackupRecord.BackupContentType.FULL;
        this.backupIntervalHours = 24;
        this.retentionDays = 30;
        this.maxBackupCount = 10;
        this.localBackupPath = "backups";
        this.createTime = new Date();
    }
    
    /**
     * 计算下次备份时间
     */
    public void calculateNextBackupTime() {
        if (lastBackupTime != null && backupIntervalHours > 0) {
            nextBackupTime = new Date(lastBackupTime.getTime() + 
                backupIntervalHours * 60 * 60 * 1000L);
        }
    }
    
    /**
     * 检查是否需要备份
     */
    public boolean needsBackup() {
        if (!autoBackupEnabled) {
            return false;
        }
        
        if (nextBackupTime == null) {
            return true;
        }
        
        return new Date().after(nextBackupTime);
    }
}