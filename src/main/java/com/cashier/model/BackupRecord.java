package com.cashier.model;

import java.util.Date;

/**
 * 备份记录模型
 */
public class BackupRecord {
    
    /**
     * 备份ID
     */
    public String backupId;
    
    /**
     * 备份类型
     */
    public BackupType backupType;
    
    /**
     * 备份目标
     */
    public BackupTarget target;
    
    /**
     * 备份文件名
     */
    public String fileName;
    
    /**
     * 备份文件路径（本地）
     */
    public String localPath;
    
    /**
     * 远程存储路径
     */
    public String remotePath;
    
    /**
     * 文件大小（字节）
     */
    public long fileSize;
    
    /**
     * 备份状态
     */
    public BackupStatus status;
    
    /**
     * 创建时间
     */
    public Date createTime;
    
    /**
     * 开始时间
     */
    public Date startTime;
    
    /**
     * 完成时间
     */
    public Date finishTime;
    
    /**
     * 耗时（秒）
     */
    public int durationSeconds;
    
    /**
     * 备份内容类型
     */
    public BackupContentType contentType;
    
    /**
     * 备份范围（全量/增量）
     */
    public BackupScope scope;
    
    /**
     * 操作员
     */
    public String operator;
    
    /**
     * 备注
     */
    public String remark;
    
    /**
     * 错误信息
     */
    public String errorMessage;
    
    /**
     * 校验码（MD5）
     */
    public String checksum;
    
    /**
     * 是否自动备份
     */
    public boolean autoBackup;
    
    /**
     * 备份类型枚举
     */
    public enum BackupType {
        MANUAL("手动备份"),
        SCHEDULED("定时备份"),
        AUTO("自动备份");
        
        private final String displayName;
        
        BackupType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 备份目标枚举（存储位置）
     */
    public enum BackupTarget {
        LOCAL("本地存储"),
        ALIYUN_OSS("阿里云OSS"),
        TENCENT_COS("腾讯云COS"),
        QINIU("七牛云"),
        AWS_S3("AWS S3"),
        FTP("FTP服务器"),
        WEBDAV("WebDAV");
        
        private final String displayName;
        
        BackupTarget(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static BackupTarget fromString(String str) {
            for (BackupTarget target : values()) {
                if (target.name().equalsIgnoreCase(str) || 
                    target.displayName.equals(str)) {
                    return target;
                }
            }
            return LOCAL;
        }
    }
    
    /**
     * 备份状态枚举
     */
    public enum BackupStatus {
        CREATED("已创建"),
        RUNNING("运行中"),
        UPLOADING("上传中"),
        SUCCESS("成功"),
        FAILED("失败"),
        CANCELLED("已取消");
        
        private final String displayName;
        
        BackupStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public boolean isFinal() {
            return this == SUCCESS || this == FAILED || this == CANCELLED;
        }
        
        public boolean isSuccess() {
            return this == SUCCESS;
        }
    }
    
    /**
     * 备份内容类型枚举
     */
    public enum BackupContentType {
        FULL("全量备份"),           // 数据库 + 文件
        DATABASE("数据库备份"),     // 仅数据库
        FILES("文件备份"),          // 仅文件
        CONFIG("配置备份"),         // 仅配置文件
        LOGS("日志备份");           // 仅日志
        
        private final String displayName;
        
        BackupContentType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 备份范围枚举
     */
    public enum BackupScope {
        FULL("全量"),
        INCREMENTAL("增量"),
        DIFFERENTIAL("差异");
        
        private final String displayName;
        
        BackupScope(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 默认构造函数
     */
    public BackupRecord() {
        this.backupId = generateBackupId();
        this.status = BackupStatus.CREATED;
        this.createTime = new Date();
        this.autoBackup = false;
    }
    
    /**
     * 创建手动备份记录
     */
    public static BackupRecord createManual(BackupContentType contentType, 
                                             BackupTarget target, String operator) {
        BackupRecord record = new BackupRecord();
        record.backupType = BackupType.MANUAL;
        record.contentType = contentType;
        record.target = target;
        record.scope = BackupScope.FULL;
        record.operator = operator;
        record.fileName = generateFileName(contentType);
        return record;
    }
    
    /**
     * 创建自动备份记录
     */
    public static BackupRecord createAuto(BackupContentType contentType, 
                                           BackupTarget target) {
        BackupRecord record = new BackupRecord();
        record.backupType = BackupType.AUTO;
        record.contentType = contentType;
        record.target = target;
        record.scope = BackupScope.FULL;
        record.autoBackup = true;
        record.fileName = generateFileName(contentType);
        return record;
    }
    
    /**
     * 生成备份ID
     */
    private static String generateBackupId() {
        return "BAK" + System.currentTimeMillis();
    }
    
    /**
     * 生成备份文件名
     */
    private static String generateFileName(BackupContentType contentType) {
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prefix = contentType.name().toLowerCase();
        return prefix + "_backup_" + timestamp + ".zip";
    }
    
    /**
     * 计算耗时
     */
    public void calculateDuration() {
        if (startTime != null && finishTime != null) {
            durationSeconds = (int)((finishTime.getTime() - startTime.getTime()) / 1000);
        }
    }
    
    /**
     * 格式化文件大小
     */
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return (fileSize / 1024) + " KB";
        } else if (fileSize < 1024 * 1024 * 1024) {
            return (fileSize / (1024 * 1024)) + " MB";
        } else {
            return (fileSize / (1024 * 1024 * 1024)) + " GB";
        }
    }
    
    @Override
    public String toString() {
        return "BackupRecord{" +
                "backupId='" + backupId + '\'' +
                ", type=" + backupType +
                ", target=" + target +
                ", status=" + status +
                ", fileSize=" + getFormattedFileSize() +
                '}';
    }
}