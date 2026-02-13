package com.cashier.printer;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * 打印预览对话框
 */
public class PrintPreviewDialog {
    
    private Stage dialogStage;
    private TextArea previewArea;
    private boolean confirmed = false;
    
    /**
     * 显示预览对话框
     * @param owner 父窗口
     * @param title 标题
     * @param content 内容
     * @return 是否确认打印
     */
    public boolean show(Stage owner, String title, String content) {
        dialogStage = new Stage();
        dialogStage.setTitle(title);
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(owner);
        dialogStage.setResizable(true);
        
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        
        // 标题
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // 预览区域
        Label previewLabel = new Label("打印预览:");
        previewArea = new TextArea();
        previewArea.setPrefWidth(400);
        previewArea.setPrefHeight(400);
        previewArea.setEditable(false);
        previewArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px;");
        previewArea.setText(content);
        
        // 按钮区域
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        Button printButton = new Button("打印");
        printButton.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        printButton.setPrefWidth(100);
        printButton.setOnAction(e -> {
            confirmed = true;
            dialogStage.close();
        });
        
        Button cancelButton = new Button("取消");
        cancelButton.setStyle("-fx-font-size: 14px;");
        cancelButton.setPrefWidth(100);
        cancelButton.setOnAction(e -> {
            confirmed = false;
            dialogStage.close();
        });
        
        buttonBox.getChildren().addAll(printButton, cancelButton);
        
        root.getChildren().addAll(titleLabel, previewLabel, previewArea, buttonBox);
        
        Scene scene = new Scene(root);
        dialogStage.setScene(scene);
        
        dialogStage.showAndWait();
        
        return confirmed;
    }
    
    /**
     * 是否确认打印
     * @return 是否确认
     */
    public boolean isConfirmed() {
        return confirmed;
    }
}