package com.cashier;

import com.cashier.util.LoggerFactoryUtil;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import com.cashier.i18n.I18nKeys;
import com.cashier.i18n.I18nManager;

/**
 * 轻量启动窗口。
 *
 * <p>只承担启动过渡效果，不承载任何业务状态，因此用普通 {@link Stage} 实现，
 * 不依赖 JavaFX 的 {@code Preloader} 机制。</p>
 *
 * <p>背景：{@code Preloader} 只有在由 JDK 启动器直接运行 {@code Application} 子类时才生效，
 * 而可执行 JAR 的 Main-Class 必须是 {@link Launcher}（否则 {@code java -jar} 会报
 * "缺少 JavaFX 运行时组件"）。恢复 Preloader 需要改用内部 API
 * {@code LauncherImpl} 并给启动脚本加 {@code --add-exports}，维护成本高于收益。</p>
 */
public final class SplashWindow {

    private static final Logger logger = LoggerFactoryUtil.getLogger(SplashWindow.class);

    private final Stage stage = new Stage();
    private final ProgressBar progress = new ProgressBar();
    private final Label statusLabel = new Label(I18nManager.getInstance().get(I18nKeys.Runtime.SPLASH_STARTING));

    public SplashWindow() {
        ImageView logo = new ImageView();
        try {
            logo.setImage(new Image("/images/logos/app-icon.png"));
            logo.setFitWidth(96);
            logo.setFitHeight(96);
        } catch (Exception e) {
            logger.warn("无法加载启动画面 Logo", e);
        }

        progress.setPrefWidth(300);
        progress.setProgress(0);
        progress.getStyleClass().add("splash-progress");

        statusLabel.getStyleClass().add("splash-status");

        VBox root = new VBox(20, logo, progress, statusLabel);
        root.getStyleClass().add("splash-root");
        root.getStylesheets().add("/css/splash.css");

        stage.setScene(new Scene(root, 400, 280));
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle(I18nManager.getInstance().get(I18nKeys.Common.APP_NAME));
        stage.setResizable(false);
        stage.centerOnScreen();
    }

    public void show() {
        stage.show();
        logger.info("启动窗口已显示");
    }

    /**
     * 更新进度与状态文案（0.0 - 1.0）。
     */
    public void updateProgress(double value) {
        progress.setProgress(value);
        updateStatusText(value);
    }

    /**
     * 更新进度并显式指定状态文案。
     *
     * <p>启动期的等待需要说清"在等什么、等了多久"（例如连接数据库），
     * 只按进度区间套固定文案是不够的。</p>
     */
    public void updateProgress(double value, String status) {
        progress.setProgress(value);
        statusLabel.setText(status);
    }

    public void close() {
        if (stage.isShowing()) {
            stage.close();
        }
    }

    private void updateStatusText(double value) {
        if (value < 0.3) {
            statusLabel.setText(I18nManager.getInstance().get(I18nKeys.Runtime.SPLASH_INITIALIZING));
        } else if (value < 0.6) {
            statusLabel.setText(I18nManager.getInstance().get(I18nKeys.Runtime.SPLASH_LOADING_DATA));
        } else if (value < 0.9) {
            statusLabel.setText(I18nManager.getInstance().get(I18nKeys.Runtime.SPLASH_STARTING_SERVICES));
        } else {
            statusLabel.setText(I18nManager.getInstance().get(I18nKeys.Runtime.SPLASH_FINISHING));
        }
    }
}
