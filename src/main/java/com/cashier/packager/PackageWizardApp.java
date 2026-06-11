package com.cashier.packager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * 图形界面打包向导启动类
 */
public class PackageWizardApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/com/cashier/view/PackageWizardView.fxml")));
        Parent root = loader.load();

        Scene scene = new Scene(root, 800, 650);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/package-wizard.css")).toExternalForm());

        primaryStage.setTitle("狸算(LiSuan)收银系统 - 打包向导");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
