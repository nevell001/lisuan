package com.cashier.constant;

/**
 * 应用常量
 * 用于集中管理应用级别的常量
 */
public class AppConstants {
    private AppConstants() {
        // 私有构造函数防止实例化
    }

    /**
     * 应用名称
     */
    public static final String APP_NAME = "收银系统 Cashier System";

    /**
     * 应用版本号
     * 注意：发布新版本时需要更新此值
     */
    public static final String APP_VERSION = "2.5.3";

    /**
     * 应用标题
     */
    public static final String APP_TITLE = "收银系统";

    /**
     * 应用副标题
     */
    public static final String APP_SUBTITLE = "JavaFX 版本";

    /**
     * 开发者
     */
    public static final String DEVELOPER = "nevell";

    /**
     * 许可证
     */
    public static final String LICENSE = "木兰宽松许可证 v2 (MulanPSL2)";

    /**
     * 完整的版本字符串（用于关于对话框）
     */
    public static final String FULL_VERSION_STRING = APP_VERSION + " (" + APP_SUBTITLE + ")";

    /**
     * JavaFX 版本
     */
    public static final String JAVAFX_VERSION = "17.0.8";

    /**
     * 最低 JDK 版本要求
     */
    public static final String MIN_JDK_VERSION = "17";

    /**
     * 最低 Maven 版本要求
     */
    public static final String MIN_MAVEN_VERSION = "3.8";
}
