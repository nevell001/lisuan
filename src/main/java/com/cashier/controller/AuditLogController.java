package com.cashier.controller;

import com.cashier.dao.DAOFactory;
import com.cashier.i18n.I18nManager;
import com.cashier.model.OperationLog;
import com.cashier.util.LoggerFactoryUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import org.slf4j.Logger;

import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

public class AuditLogController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(AuditLogController.class);
    private static final int AUDIT_LOG_LIMIT = 500;
    private final ObservableList<OperationLog> allLogs = FXCollections.observableArrayList();

    @FXML private TableView<OperationLog> auditTable;
    @FXML private TableColumn<OperationLog, String> timeColumn;
    @FXML private TableColumn<OperationLog, String> usernameColumn;
    @FXML private TableColumn<OperationLog, String> categoryColumn;
    @FXML private TableColumn<OperationLog, String> operationColumn;
    @FXML private TableColumn<OperationLog, String> resultColumn;
    @FXML private TableColumn<OperationLog, String> detailsColumn;
    @FXML private TableColumn<OperationLog, Number> affectedColumn;
    @FXML private TextField usernameField;
    @FXML private TextField keywordField;
    @FXML private ComboBox<String> categoryBox;
    @FXML private ComboBox<String> resultBox;
    @FXML private Label countLabel;

    /** 分类枚举值（存储在数据库中的原始值，用于筛选） */
    private static final String[] CATEGORY_VALUES = {
        "ALL", "AUTH", "TRANSACTION", "REFUND", "INVENTORY", "PURCHASE", "MEMBER", "USER", "SETTINGS", "SYSTEM"
    };
    /** 结果枚举值（存储在数据库中的原始值，用于筛选） */
    private static final String[] RESULT_VALUES = {"ALL", "SUCCESS", "FAILURE"};

    @FXML
    private void initialize() {
        timeColumn.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().timestamp != null
                ? data.getValue().timestamp.atZone(ZoneId.systemDefault()).toLocalDateTime()
                    .format(com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME)
                : ""));
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().username == null ? "-" : data.getValue().username));
        categoryColumn.setCellValueFactory(data -> new SimpleStringProperty(
            translateCategory(data.getValue().category)));
        operationColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().operation));
        resultColumn.setCellValueFactory(data -> new SimpleStringProperty(
            translateResult(data.getValue().result)));
        detailsColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().details));
        affectedColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().affectedRecords));

        // ComboBox 存储原始枚举值用于筛选，通过 StringConverter 显示本地化文本
        categoryBox.setItems(FXCollections.observableArrayList(CATEGORY_VALUES));
        categoryBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(String value) {
                return translateCategory(value);
            }
            @Override
            public String fromString(String string) {
                return string;
            }
        });
        resultBox.setItems(FXCollections.observableArrayList(RESULT_VALUES));
        resultBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(String value) {
                return translateResult(value);
            }
            @Override
            public String fromString(String string) {
                return string;
            }
        });
        categoryBox.getSelectionModel().selectFirst();
        resultBox.getSelectionModel().selectFirst();
        handleRefresh();
    }

    /**
     * 将分类枚举值翻译为本地化显示文本
     * @param category 数据库中的原始枚举值（如 AUTH, TRANSACTION）
     * @return 本地化文本（如"登录认证"、"Authentication"）
     */
    private String translateCategory(String category) {
        if (category == null || category.isEmpty()) {
            return "-";
        }
        String key = "audit.category." + category.toLowerCase(Locale.ROOT);
        String translated = I18nManager.getInstance().get(key);
        // 如果翻译结果等于 key 本身，说明未找到翻译，回退显示原始值
        return translated.equals(key) ? category : translated;
    }

    /**
     * 将结果枚举值翻译为本地化显示文本
     * @param result 数据库中的原始枚举值（SUCCESS/FAILURE）
     * @return 本地化文本（如"成功"、"Success"）
     */
    private String translateResult(String result) {
        if (result == null || result.isEmpty()) {
            return "-";
        }
        String key = "audit.result." + result.toLowerCase(Locale.ROOT);
        String translated = I18nManager.getInstance().get(key);
        return translated.equals(key) ? result : translated;
    }

    @FXML
    private void handleRefresh() {
        Thread backgroundThread = new Thread(() -> {
            try {
                List<OperationLog> logs = DAOFactory.getInstance().getOperationLogDAO().findRecent(AUDIT_LOG_LIMIT);
                Platform.runLater(() -> {
                    allLogs.setAll(logs);
                    handleSearch();
                });
            } catch (Exception e) {
                logger.error("Failed to load audit logs", e);
                Platform.runLater(() -> {
                    countLabel.setText(I18nManager.getInstance().get("audit.load_failed"));
                });
            }
        });
        // TD-035：非 daemon 线程会让关窗后进程不退出
        backgroundThread.setDaemon(true);
        backgroundThread.start();
    }

    @FXML
    private void handleSearch() {
        String username = usernameField.getText().trim().toLowerCase(Locale.ROOT);
        String keyword = keywordField.getText().trim().toLowerCase(Locale.ROOT);
        String category = categoryBox.getValue();
        String result = resultBox.getValue();

        List<OperationLog> filtered = allLogs.stream().filter(log ->
            (username.isEmpty() || safe(log.username).toLowerCase(Locale.ROOT).contains(username)) &&
            ("ALL".equals(category) || category.equals(log.category)) &&
            ("ALL".equals(result) || result.equals(log.result)) &&
            (keyword.isEmpty() || (safe(log.operation) + " " + safe(log.details)).toLowerCase(Locale.ROOT).contains(keyword))
        ).toList();
        auditTable.setItems(FXCollections.observableArrayList(filtered));
        countLabel.setText(I18nManager.getInstance().get("audit.count", filtered.size()));
    }

    @FXML
    private void handleClear() {
        usernameField.clear();
        keywordField.clear();
        categoryBox.getSelectionModel().selectFirst();
        resultBox.getSelectionModel().selectFirst();
        handleSearch();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
