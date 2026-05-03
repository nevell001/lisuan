package com.cashier.service;

import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;
import java.io.File;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 自动备份服务
 * 根据设置的备份频率自动执行数据备份
 */
public class BackupService {
    private static final Logger logger = LoggerFactoryUtil.getLogger(BackupService.class);
    private static final int MAX_BACKUP_FILES = 30;
    private static BackupService instance;

    private Timer backupTimer;
    private boolean running;

    private BackupService() {
        this.running = false;
    }

    public static synchronized BackupService getInstance() {
        if (instance == null) {
            instance = new BackupService();
        }
        return instance;
    }

    /**
     * 启动自动备份服务
     */
    public void start() {
        if (running) {
            logger.warn("自动备份服务已经在运行中");
            return;
        }

        Map<String, String> settings = DataService.loadSettings();
        boolean autoBackup = Boolean.parseBoolean(settings.getOrDefault("autoBackup", "false"));

        if (!autoBackup) {
            logger.info("自动备份未启用，跳过启动");
            return;
        }

        String frequency = settings.getOrDefault("backupFrequency", "每天");
        String backupPath = settings.getOrDefault("backupPath", System.getProperty("user.dir"));

        logger.info("启动自动备份服务: 频率={}, 路径={}", frequency, backupPath);

        backupTimer = new Timer("BackupTimer", true);
        long delay = calculateInitialDelay(frequency);
        long period = calculatePeriod(frequency);

        backupTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                performBackup(backupPath);
            }
        }, delay, period);

        running = true;
        logger.info("自动备份服务已启动");
    }

    /**
     * 停止自动备份服务
     */
    public void stop() {
        if (!running) {
            logger.warn("自动备份服务未在运行");
            return;
        }

        if (backupTimer != null) {
            backupTimer.cancel();
            backupTimer = null;
        }

        running = false;
        logger.info("自动备份服务已停止");
    }

    /**
     * 执行备份
     */
    private void performBackup(String backupPath) {
        try {
            logger.info("开始执行自动备份...");
            DataService.backupData(backupPath);
            logger.info("自动备份成功");

            // 清理旧备份文件（保留最近 30 个）
            cleanOldBackups(backupPath);
        } catch (Exception e) {
            logger.error("自动备份失败", e);
        }
    }

    /**
     * 清理旧的备份文件
     * @param backupPath 备份路径
     */
    private void cleanOldBackups(String backupPath) {
        try {
            File backupDir = new File(backupPath);
            if (!backupDir.exists() || !backupDir.isDirectory()) {
                return;
            }

            File[] sqlFiles = backupDir.listFiles((dir, name) ->
                name.startsWith("cashier_system_") && name.endsWith(".sql"));

            if (sqlFiles == null || sqlFiles.length <= MAX_BACKUP_FILES) {
                return;
            }

            // 按修改时间排序，旧文件在前
            java.util.Arrays.sort(sqlFiles, (a, b) ->
                Long.compare(a.lastModified(), b.lastModified()));

            // 删除最旧的文件，保留最近 MAX_BACKUP_FILES 个
            int filesToDelete = sqlFiles.length - MAX_BACKUP_FILES;
            for (int i = 0; i < filesToDelete; i++) {
                boolean deleted = sqlFiles[i].delete();
                logger.info("删除旧备份文件: {} ({})", sqlFiles[i].getName(), deleted ? "成功" : "失败");
            }
        } catch (Exception e) {
            logger.error("清理旧备份文件失败", e);
        }
    }

    /**
     * 计算初始延迟（让第一次备份在特定时间执行）
     * @param frequency 备份频率
     * @return 延迟毫秒数
     */
    private long calculateInitialDelay(String frequency) {
        LocalDateTime now = LocalDateTime.now();

        switch (frequency) {
            case "每天":
                // 每天凌晨 2 点执行
                LocalDateTime nextDaily = now.toLocalDate().plusDays(1).atTime(LocalTime.of(2, 0));
                return java.time.Duration.between(now, nextDaily).toMillis();

            case "每周":
                // 每周一凌晨 2 点执行
                LocalDateTime nextWeekly = now.plusDays(1);
                while (nextWeekly.getDayOfWeek().getValue() != 1) {
                    nextWeekly = nextWeekly.plusDays(1);
                }
                nextWeekly = nextWeekly.toLocalDate().atTime(LocalTime.of(2, 0));
                return java.time.Duration.between(now, nextWeekly).toMillis();

            case "每月":
                // 每月 1 号凌晨 2 点执行
                LocalDateTime nextMonthly = now.plusMonths(1)
                    .withDayOfMonth(1)
                    .withHour(2)
                    .withMinute(0)
                    .withSecond(0);
                return java.time.Duration.between(now, nextMonthly).toMillis();

            default:
                return 24 * 60 * 60 * 1000L; // 默认每天
        }
    }

    /**
     * 计算备份周期
     * @param frequency 备份频率
     * @return 周期毫秒数
     */
    private long calculatePeriod(String frequency) {
        switch (frequency) {
            case "每天":
                return 24 * 60 * 60 * 1000L; // 1天
            case "每周":
                return 7 * 24 * 60 * 60 * 1000L; // 7天
            case "每月":
                return 30 * 24 * 60 * 60 * 1000L; // 30天
            default:
                return 24 * 60 * 60 * 1000L;
        }
    }

    /**
     * 检查服务是否正在运行
     * @return 如果正在运行返回 true
     */
    public boolean isRunning() {
        return running;
    }
}
