package com.cashier.controller;

import com.cashier.i18n.I18nManager;
import com.cashier.util.ShortcutManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Map;

/**
 * 快捷键帮助控制器
 * 显示系统中所有可用的快捷键
 */
public class ShortcutHelpController {
    private final I18nManager i18n = I18nManager.getInstance();

    @FXML
    private TextField searchField;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private VBox contentArea;

    private Stage stage;

    @FXML
    public void initialize() {
        // 设置焦点到搜索框
        Platform.runLater(() -> searchField.requestFocus());

        // 设置 ESC 键关闭窗口
        contentArea.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                handleClose();
            }
        });
    }

    /**
     * 设置关联的舞台
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * 关闭帮助窗口
     */
    @FXML
    public void handleClose() {
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * 搜索快捷键
     */
    @FXML
    public void handleSearch() {
        String searchText = searchField.getText().toLowerCase().trim();

        if (searchText.isEmpty()) {
            // 显示所有分组
            contentArea.getChildren().forEach(node -> node.setVisible(true));
            return;
        }

        // 遍历所有分组
        contentArea.getChildren().forEach(group -> {
            if (group instanceof VBox) {
                VBox groupBox = (VBox) group;
                boolean hasMatch = false;

                // 检查分组内的所有标签
                for (var child : groupBox.getChildren()) {
                    if (child instanceof Label) {
                        Label label = (Label) child;
                        String text = label.getText().toLowerCase();
                        if (text.contains(searchText)) {
                            hasMatch = true;
                        }
                    } else if (child instanceof javafx.scene.layout.GridPane) {
                        // 检查 GridPane 中的所有标签
                        javafx.scene.layout.GridPane grid = (javafx.scene.layout.GridPane) child;
                        for (var gridChild : grid.getChildren()) {
                            if (gridChild instanceof Label) {
                                Label label = (Label) gridChild;
                                String text = label.getText().toLowerCase();
                                if (text.contains(searchText)) {
                                    hasMatch = true;
                                }
                            }
                        }
                    }
                }

                groupBox.setVisible(hasMatch);
                groupBox.setManaged(hasMatch);
            }
        });
    }

    /**
     * 重新构建快捷键内容（用于国际化切换时更新）
     */
    public void rebuildContent() {
        // 这里可以根据需要动态重建内容
        // 目前通过 FXML 绑定自动更新
    }
}
