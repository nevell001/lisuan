package com.cashier;

import com.cashier.util.LoggerFactoryUtil;
import javafx.application.Preloader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;

/**
 * 应用启动画面
 * 在 JavaFX 应用初始化时显示，提供视觉反馈
 */
public class SplashScreen extends Preloader {
    private static final Logger logger = LoggerFactoryUtil.getLogger(SplashScreen.class);

    private Stage stage;
    private ProgressBar progress;
    private Label statusLabel;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        // Logo
        ImageView logo = new ImageView();
        try {
            Image logoImage = new Image("/images/logos/app-icon.png");
            logo.setImage(logoImage);
            logo.setFitWidth(96);
            logo.setFitHeight(96);
        } catch (Exception e) {
            logger.warn("无法加载启动画面 Logo", e);
        }

        // 进度条
        progress = new ProgressBar();
        progress.setPrefWidth(300);
        progress.setProgress(0);
        progress.setStyle("-fx-accent: #4CAF50;");

        // 状态标签
        statusLabel = new Label("正在启动...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        VBox root = new VBox(20, logo, progress, statusLabel);
        root.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 40; -fx-alignment: center;");
        root.getStylesheets().add("/css/splash.css");

        Scene scene = new Scene(root, 400, 280);
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("收银系统");
        stage.setResizable(false);
        stage.centerOnScreen();

        logger.info("启动画面已显示");
        stage.show();
    }

    @Override
    public void handleApplicationNotification(PreloaderNotification info) {
        if (info instanceof ProgressNotification) {
            ProgressNotification pn = (ProgressNotification) info;
            progress.setProgress(pn.getProgress());
            updateStatusText(pn.getProgress());
        }

        if (info instanceof StateChangeNotification) {
            StateChangeNotification scn = (StateChangeNotification) info;
            if (scn.getType() == StateChangeNotification.Type.BEFORE_START) {
                // 应用即将启动，隐藏启动画面
                logger.info("应用即将启动，隐藏启动画面");
                if (stage != null) {
                    stage.hide();
                }
            }
        }
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification info) {
        if (info.getType() == StateChangeNotification.Type.BEFORE_START) {
            if (stage != null) {
                stage.hide();
            }
        }
    }

    /**
     * 根据进度更新状态文本
     */
    private void updateStatusText(double progress) {
        if (progress < 0.3) {
            statusLabel.setText("正在初始化...");
        } else if (progress < 0.6) {
            statusLabel.setText("正在加载数据...");
        } else if (progress < 0.9) {
            statusLabel.setText("正在启动服务...");
        } else {
            statusLabel.setText("即将完成...");
        }
    }

    @Override
    public boolean handleErrorNotification(ErrorNotification info) {
        logger.error("启动画面错误通知");
        return false;
    }
}
