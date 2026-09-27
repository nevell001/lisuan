package com.cashier.printer;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.cashier.i18n.I18nKeys;
import com.cashier.i18n.I18nManager;

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
        titleLabel.getStyleClass().add("title-sm");
        
        // 预览区域
        Label previewLabel = new Label(I18nManager.getInstance().get(I18nKeys.Runtime.PRINT_PREVIEW));
        previewArea = new TextArea();
        previewArea.setPrefWidth(400);
        previewArea.setPrefHeight(400);
        previewArea.setEditable(false);
        previewArea.setStyle("-fx-font-family: 'Noto Sans Mono CJK SC', 'Noto Sans CJK SC', 'PingFang SC', 'Microsoft YaHei UI', monospace; -fx-font-size: 12px;");
        previewArea.setText(content);
        
        // 按钮区域
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        Button printButton = new Button(I18nManager.getInstance().get(I18nKeys.Runtime.PRINT_BUTTON));
        printButton.getStyleClass().add("title-xs");
        printButton.setPrefWidth(100);
        printButton.setOnAction(e -> {
            confirmed = true;
            dialogStage.close();
        });
        
        Button cancelButton = new Button(I18nManager.getInstance().get(I18nKeys.Common.CANCEL));
        cancelButton.getStyleClass().add("text-lg");
        cancelButton.setPrefWidth(100);
        cancelButton.setOnAction(e -> {
            confirmed = false;
            dialogStage.close();
        });
        
        buttonBox.getChildren().addAll(printButton, cancelButton);
        
        root.getChildren().addAll(titleLabel, previewLabel, previewArea, buttonBox);
        
        Scene scene = new Scene(root);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
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
