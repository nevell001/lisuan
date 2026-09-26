package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.i18n.I18nManager;
import com.cashier.util.SearchManager;
import com.cashier.util.SearchManager.SearchResult;
import com.cashier.util.UIOptimizer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.util.List;

/**
 * 全局搜索控制器
 */
public class SearchController {
    private static final org.slf4j.Logger logger =
        com.cashier.util.LoggerFactoryUtil.getLogger(SearchController.class);
    private final I18nManager i18n = I18nManager.getInstance();

    @FXML
    private TextField searchField;

    @FXML
    private ListView<HBox> resultsList;

    @FXML
    private Label resultCountLabel;

    private Stage stage;
    private ObservableList<HBox> results = FXCollections.observableArrayList();
    private int selectedIndex = 0;
    /** 搜索序号：连续输入时只允许最新一次结果上屏 */
    private final java.util.concurrent.atomic.AtomicLong searchSequence =
        new java.util.concurrent.atomic.AtomicLong();

    @FXML
    public void initialize() {
        resultsList.setItems(results);
        resultsList.setFocusTraversable(false);
        resultsList.setCellFactory(param -> new SearchResultCell());

        // 搜索框监听
        searchField.textProperty().addListener((obs, oldVal, newVal) -> performSearch(newVal));

        // 键盘导航
        resultsList.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DOWN) {
                selectNext();
                event.consume();
            } else if (event.getCode() == KeyCode.UP) {
                selectPrevious();
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER) {
                executeSelected();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                handleClose();
                event.consume();
            }
        });

        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DOWN) {
                if (!results.isEmpty()) {
                    resultsList.requestFocus();
                    resultsList.getSelectionModel().select(0);
                }
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                handleClose();
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER) {
                if (!results.isEmpty()) {
                    executeSelected();
                }
                event.consume();
            }
        });

        // 聚焦到搜索框
        Platform.runLater(() -> searchField.requestFocus());
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void handleClose() {
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * 执行搜索
     */
    private void performSearch(String query) {
        results.clear();
        selectedIndex = 0;

        if (query == null || query.trim().isEmpty()) {
            resultCountLabel.setText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.MemberEdit.POINTS_HINT));
            return;
        }

        // 每次按键都会触发一次全库搜索：放后台，并用序号丢弃过期结果（与收银台搜索同一做法）
        final long sequence = searchSequence.incrementAndGet();
        UIOptimizer.runInBackground(
            () -> SearchManager.search(query, 10),
            searchResults -> {
                if (sequence != searchSequence.get()) {
                    return; // 已经有更新的输入，丢弃过期结果
                }
                for (SearchResult result : searchResults) {
                    results.add(createResultItem(result));
                }
                resultCountLabel.setText(String.valueOf(searchResults.size()));
                if (!results.isEmpty()) {
                    resultsList.getSelectionModel().select(0);
                }
            },
            e -> logger.error("搜索失败", e));
    }

    /**
     * 创建搜索结果项
     */
    private HBox createResultItem(SearchResult result) {
        HBox box = new HBox(12);
        box.setUserData(result);
        box.getStyleClass().add("search-result-item");

        Label typeLabel = new Label(result.getType());
        typeLabel.getStyleClass().add("result-type");

        Label titleLabel = new Label(result.getTitle());
        titleLabel.getStyleClass().add("result-title");

        Label shortcutLabel = new Label(result.getShortcut());
        shortcutLabel.getStyleClass().add("result-shortcut");
        if (result.getShortcut() == null || result.getShortcut().isEmpty()) {
            shortcutLabel.setVisible(false);
        }

        box.getChildren().addAll(typeLabel, titleLabel, new Region(), shortcutLabel);
        HBox.setHgrow(titleLabel, javafx.scene.layout.Priority.ALWAYS);

        box.setOnMouseClicked(e -> {
            result.execute();
            handleClose();
        });

        return box;
    }

    /**
     * 选择下一个结果
     */
    private void selectNext() {
        if (results.isEmpty()) return;
        selectedIndex = (selectedIndex + 1) % results.size();
        resultsList.getSelectionModel().select(selectedIndex);
        resultsList.scrollTo(selectedIndex);
    }

    /**
     * 选择上一个结果
     */
    private void selectPrevious() {
        if (results.isEmpty()) return;
        selectedIndex = (selectedIndex - 1 + results.size()) % results.size();
        resultsList.getSelectionModel().select(selectedIndex);
        resultsList.scrollTo(selectedIndex);
    }

    /**
     * 执行选中的结果
     */
    private void executeSelected() {
        HBox selected = resultsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            SearchResult result = (SearchResult) selected.getUserData();
            result.execute();
            handleClose();
        }
    }

    /**
     * 自定义列表单元格
     */
    private static class SearchResultCell extends ListCell<HBox> {
        SearchResultCell() {
            getStyleClass().add("search-result-cell");
        }

        @Override
        protected void updateItem(HBox item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                setGraphic(item);
            }
        }
    }
}
