package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.dao.DAOFactory;
import com.cashier.service.DataService;
import com.cashier.model.Promotion;
import com.cashier.i18n.I18nManager;
import com.cashier.util.StatusBarManager;
import com.cashier.util.UIOptimizer;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.FormValidator;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 促销管理控制器
 * 处理促销活动的增删改查
 */
public class PromotionController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(PromotionController.class);

    @FXML
    private TableView<Promotion> promotionTable;

    @FXML
    private TableColumn<Promotion, String> nameColumn;

    @FXML
    private TableColumn<Promotion, String> typeColumn;

    @FXML
    private TableColumn<Promotion, String> descriptionColumn;

    @FXML
    private TableColumn<Promotion, String> periodColumn;

    @FXML
    private TableColumn<Promotion, String> usageColumn;

    @FXML
    private TableColumn<Promotion, String> statusColumn;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> typeFilterComboBox;

    @FXML
    private ComboBox<String> statusFilterComboBox;

    @FXML
    private Label countLabel;

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button enableButton;

    @FXML
    private Button disableButton;

    // 声明即初始化：这些集合由异步回调赋值，别处可能在数据到达前读它（Windows 实测过 NPE）
    private ObservableList<Promotion> promotionList = FXCollections.observableArrayList();
    // 声明即初始化：这些集合由异步回调赋值，别处可能在数据到达前读它（Windows 实测过 NPE）
    private List<Promotion> allPromotions = new java.util.ArrayList<>();

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 初始化类型筛选下拉框
        typeFilterComboBox.setItems(FXCollections.observableArrayList(
            "全部",
            "满减",
            "打折",
            "优惠券"
        ));
        typeFilterComboBox.setButtonCell(createPromotionFilterCell(true));
        typeFilterComboBox.setCellFactory(listView -> createPromotionFilterCell(true));
        typeFilterComboBox.getSelectionModel().select(0);

        // 初始化状态筛选下拉框
        statusFilterComboBox.setItems(FXCollections.observableArrayList(
            "全部",
            "启用",
            "禁用"
        ));
        statusFilterComboBox.setButtonCell(createPromotionFilterCell(false));
        statusFilterComboBox.setCellFactory(listView -> createPromotionFilterCell(false));
        statusFilterComboBox.getSelectionModel().select(0);

        // 设置表格列
        setupTableColumns();

        // 加载促销数据
        loadPromotions();

        // 设置表格选择模式
        promotionTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // 添加表格选择监听
        promotionTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> updateButtonStates()
        );
    }

    /**
     * 设置表格列
     */
    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        typeColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(localizePromotionType(cellData.getValue().type)));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        DateTimeFormatter formatter = com.cashier.util.DateTimeFormats.DATE;
        periodColumn.setCellValueFactory(cellData -> {
            Promotion p = cellData.getValue();
            LocalDate startDate = p.startDate.toLocalDate();
            LocalDate endDate = p.endDate.toLocalDate();
            return new SimpleStringProperty(I18nManager.getInstance().get("promotion.date_range",
                startDate.format(formatter), endDate.format(formatter)));
        });

        usageColumn.setCellValueFactory(cellData -> {
            Promotion p = cellData.getValue();
            String maxUsage = p.maxUsage == -1
                ? I18nManager.getInstance().get("promotion.unlimited")
                : String.valueOf(p.maxUsage);
            return new SimpleStringProperty(String.format("%d/%s", p.usageCount, maxUsage));
        });

        statusColumn.setCellValueFactory(cellData -> {
            Promotion p = cellData.getValue();
            String status = I18nManager.getInstance().get(
                p.enabled ? "promotion.status.enabled" : "promotion.status.disabled");
            String validity = I18nManager.getInstance().get(
                p.isValid() ? "promotion.status.valid" : "promotion.status.expired");
            return new SimpleStringProperty(I18nManager.getInstance().get(
                "promotion.status_display", status, validity));
        });
    }

    /**
     * 加载促销数据
     */
    private void loadPromotions() {
        logger.info("PromotionController: 开始加载促销数据...");
        // 打开促销页即查库：放后台，避免整屏冻结
        UIOptimizer.runInBackground(
            DataService::loadPromotions,
            promotions -> {
                allPromotions = promotions;
                promotionList = FXCollections.observableArrayList(allPromotions);
                promotionTable.setItems(promotionList);
                updateCountLabel();
                logger.info("PromotionController: 加载了 {} 条促销记录", allPromotions.size());
            },
            e -> {
                logger.error("加载促销数据失败", e);
                StatusBarManager.updateError(I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
            });
    }

    /**
     * 更新促销数量标签
     */
    private void updateCountLabel() {
        countLabel.setText(com.cashier.i18n.I18nManager.getInstance().get("runtime.promotion_count", promotionList.size()));
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        boolean hasSelection = !promotionTable.getSelectionModel().getSelectedItems().isEmpty();
        editButton.setDisable(!hasSelection);
        deleteButton.setDisable(!hasSelection);
        enableButton.setDisable(!hasSelection);
        disableButton.setDisable(!hasSelection);
    }

    /**
     * 处理添加促销
     */
    @FXML
    public void handleAddPromotion() {
        showPromotionDialog(null);
    }

    /**
     * 处理编辑促销
     */
    @FXML
    public void handleEditPromotion() {
        Promotion selected = promotionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showPromotionDialog(selected);
        } else {
            showWarning(I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_PROMOTION));
        }
    }

    /**
     * 显示促销对话框
     * @param promotion 要编辑的促销，null表示添加新促销
     */
    private void showPromotionDialog(Promotion promotion) {
        Dialog<Promotion> dialog = new Dialog<>();
        dialog.setTitle(promotion == null ? com.cashier.i18n.I18nManager.getInstance().get("promotion.add") : com.cashier.i18n.I18nManager.getInstance().get("runtime.promotion_edit"));
        dialog.setHeaderText(null);
        dialog.getDialogPane().setPrefWidth(760);
        dialog.getDialogPane().getStyleClass().add("promotion-dialog");
        if (promotionTable.getScene() != null) {
            dialog.initOwner(promotionTable.getScene().getWindow());
            dialog.getDialogPane().getStylesheets().addAll(promotionTable.getScene().getStylesheets());
        }

        // 创建对话框内容
        VBox content = new VBox(14);
        content.getStyleClass().add("dialog-content");

        Label titleLabel = new Label(promotion == null ? com.cashier.i18n.I18nManager.getInstance().get("promotion.add") : com.cashier.i18n.I18nManager.getInstance().get("runtime.promotion_edit"));
        titleLabel.getStyleClass().add("view-title");

        GridPane grid = new GridPane();
        grid.getStyleClass().addAll("form-grid", "dialog-form-grid");
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(4, 0, 0, 0));
        grid.getColumnConstraints().addAll(
            createLabelColumn(),
            createFieldColumn(),
            createLabelColumn(),
            createFieldColumn()
        );

        PromotionFormControls controls = createPromotionFormControls();

        populatePromotionForm(promotion, controls);

        controls.typeComboBox.valueProperty().addListener((obs, oldType, newType) -> {
            updatePromotionFieldHints(newType, controls.thresholdField, controls.discountField);
        });
        updatePromotionFieldHints(
            controls.typeComboBox.getSelectionModel().getSelectedItem(),
            controls.thresholdField, controls.discountField);

        layoutPromotionForm(grid, controls);

        content.getChildren().addAll(titleLabel, grid);
        dialog.getDialogPane().setContent(content);

        ButtonType okButtonType = new ButtonType(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Common.OK), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType(I18nManager.getInstance().get(I18nKeys.Common.CANCEL), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);

        Node okButton = dialog.getDialogPane().lookupButton(okButtonType);
        okButton.getStyleClass().addAll("primary-button", "button-normal");
        Node cancelButton = dialog.getDialogPane().lookupButton(cancelButtonType);
        cancelButton.getStyleClass().addAll("secondary-button", "button-normal");
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                Promotion result = buildPromotionFromForm(
                    promotion,
                    controls.promotionCodeField,
                    controls.nameField,
                    controls.typeComboBox,
                    controls.thresholdField,
                    controls.discountField,
                    controls.descriptionArea,
                    controls.startDatePicker,
                    controls.endDatePicker,
                    controls.maxUsageField
                );
                controls.errorLabel.setText("");
                dialog.setResult(result);
                dialog.close();
                event.consume();
            } catch (IllegalArgumentException e) {
                logger.info("促销数据验证失败: {}", e.getMessage());
                StatusBarManager.updateWarning(e.getMessage());
                controls.errorLabel.setText(e.getMessage());
                event.consume();
            } catch (Exception e) {
                logger.error("保存促销失败", e);
                String message = com.cashier.i18n.I18nManager.getInstance().get("runtime.promotion_save_validation_error", e.getMessage());
                StatusBarManager.updateError(message);
                controls.errorLabel.setText(message);
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                persistPromotion(result);
                loadPromotions();
                updateStatus(I18nManager.getInstance().get(
                    promotion == null ? "promotion.added" : "promotion.updated"));
            } catch (Exception e) {
                logger.error("保存促销失败", e);
                showAlert(com.cashier.i18n.I18nManager.getInstance().get("message.save.failed"), com.cashier.i18n.I18nManager.getInstance().get("runtime.promotion_save_error", e.getMessage()));
            }
        });
    }

    /**
     * 生成促销编号
     */
    /** 促销表单控件集合，避免在对话框方法中散落大量局部变量 */
    private static class PromotionFormControls {
        final TextField promotionCodeField = new TextField();
        final TextField nameField = new TextField();
        final ComboBox<String> typeComboBox = new ComboBox<>();
        final TextField thresholdField = new TextField();
        final TextField discountField = new TextField();
        final TextArea descriptionArea = new TextArea();
        final DatePicker startDatePicker = new DatePicker();
        final DatePicker endDatePicker = new DatePicker();
        final TextField maxUsageField = new TextField();
        final Label errorLabel = new Label();
    }

    private PromotionFormControls createPromotionFormControls() {
        PromotionFormControls controls = new PromotionFormControls();
        controls.errorLabel.setWrapText(true);
        controls.errorLabel.setMaxWidth(Double.MAX_VALUE);
        controls.errorLabel.getStyleClass().add("error-label");
        controls.errorLabel.visibleProperty().bind(controls.errorLabel.textProperty().isNotEmpty());
        controls.errorLabel.managedProperty().bind(controls.errorLabel.visibleProperty());

        stylePromotionDialogControls(
            controls.promotionCodeField,
            controls.nameField,
            controls.typeComboBox,
            controls.thresholdField,
            controls.discountField,
            controls.descriptionArea,
            controls.startDatePicker,
            controls.endDatePicker,
            controls.maxUsageField
        );

        controls.typeComboBox.setItems(FXCollections.observableArrayList("满减", "打折", "优惠券"));
        controls.typeComboBox.setButtonCell(createPromotionTypeCell());
        controls.typeComboBox.setCellFactory(listView -> createPromotionTypeCell());
        controls.typeComboBox.getSelectionModel().select("满减");
        controls.thresholdField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.promotion_threshold_hint"));
        controls.discountField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.promotion_reduction_hint"));
        controls.maxUsageField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.promotion_unlimited_hint"));
        controls.startDatePicker.setValue(LocalDate.now());
        controls.endDatePicker.setValue(LocalDate.now().plusDays(30));
        return controls;
    }

    private void populatePromotionForm(Promotion promotion, PromotionFormControls controls) {
        if (promotion != null) {
            controls.promotionCodeField.setText(promotion.promotionCode);
            controls.promotionCodeField.setDisable(true);  // 编辑时禁用促销编号
            controls.nameField.setText(promotion.name);
            controls.typeComboBox.getSelectionModel().select(promotion.type);
            controls.thresholdField.setText(String.valueOf(promotion.threshold));
            controls.discountField.setText(String.valueOf(promotion.discount));
            controls.descriptionArea.setText(promotion.description);
            controls.startDatePicker.setValue(promotion.startDate.toLocalDate());
            controls.endDatePicker.setValue(promotion.endDate.toLocalDate());
            controls.maxUsageField.setText(promotion.maxUsage == -1 ? "" : String.valueOf(promotion.maxUsage));
        } else {
            // 新建促销时自动生成编号
            controls.promotionCodeField.setText(generatePromotionCode());
            controls.promotionCodeField.setDisable(true);  // 禁用促销编号字段
            controls.thresholdField.setText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.MemberEdit.POINTS_HINT));
        }
    }

    private void layoutPromotionForm(GridPane grid, PromotionFormControls controls) {
        grid.add(createFormLabel(I18nManager.getInstance().get("promotion.code")), 0, 0);
        grid.add(controls.promotionCodeField, 1, 0);
        grid.add(createFormLabel(I18nManager.getInstance().get("promotion.name_required")), 2, 0);
        grid.add(controls.nameField, 3, 0);
        grid.add(createFormLabel(I18nManager.getInstance().get("promotion.type_required")), 0, 1);
        grid.add(controls.typeComboBox, 1, 1);
        grid.add(createFormLabel(I18nManager.getInstance().get("promotion.max_usage")), 2, 1);
        grid.add(controls.maxUsageField, 3, 1);
        grid.add(createFormLabel(I18nManager.getInstance().get("promotion.threshold_required")), 0, 2);
        grid.add(controls.thresholdField, 1, 2);
        grid.add(createFormLabel(I18nManager.getInstance().get("promotion.discount_required")), 2, 2);
        grid.add(controls.discountField, 3, 2);
        grid.add(createFormLabel(I18nManager.getInstance().get("promotion.start_date_required")), 0, 3);
        grid.add(controls.startDatePicker, 1, 3);
        grid.add(createFormLabel(I18nManager.getInstance().get("promotion.end_date_required")), 2, 3);
        grid.add(controls.endDatePicker, 3, 3);
        grid.add(createFormLabel(I18nManager.getInstance().get("promotion.description_label")), 0, 4);
        grid.add(controls.descriptionArea, 1, 4, 3, 1);
        grid.add(controls.errorLabel, 1, 5, 3, 1);
    }

    private String generatePromotionCode() {
        return "P" + System.currentTimeMillis();
    }

    private ColumnConstraints createLabelColumn() {
        ColumnConstraints column = new ColumnConstraints();
        column.setMinWidth(92);
        column.setPrefWidth(92);
        column.setHgrow(Priority.NEVER);
        return column;
    }

    private ColumnConstraints createFieldColumn() {
        ColumnConstraints column = new ColumnConstraints();
        column.setMinWidth(220);
        column.setPrefWidth(240);
        column.setHgrow(Priority.ALWAYS);
        return column;
    }

    private Label createFormLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
    }

    private void stylePromotionDialogControls(TextField promotionCodeField,
                                              TextField nameField,
                                              ComboBox<String> typeComboBox,
                                              TextField thresholdField,
                                              TextField discountField,
                                              TextArea descriptionArea,
                                              DatePicker startDatePicker,
                                              DatePicker endDatePicker,
                                              TextField maxUsageField) {
        TextField[] textFields = {
            promotionCodeField, nameField, thresholdField, discountField, maxUsageField
        };
        for (TextField field : textFields) {
            field.getStyleClass().add("form-input");
            field.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(field, Priority.ALWAYS);
        }

        thresholdField.getStyleClass().add("input-amount");
        discountField.getStyleClass().add("input-amount");
        maxUsageField.getStyleClass().add("input-number");
        promotionCodeField.getStyleClass().add("input-code");

        typeComboBox.getStyleClass().add("form-combo");
        typeComboBox.setMinWidth(200);
        typeComboBox.setPrefWidth(220);
        typeComboBox.setMinHeight(36);
        typeComboBox.setPrefHeight(36);
        typeComboBox.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(typeComboBox, Priority.ALWAYS);

        DatePicker[] datePickers = {startDatePicker, endDatePicker};
        for (DatePicker picker : datePickers) {
            picker.getStyleClass().add("promotion-date-picker");
            picker.setMinWidth(200);
            picker.setPrefWidth(220);
            picker.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(picker, Priority.ALWAYS);
        }

        descriptionArea.getStyleClass().add("form-textarea");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(descriptionArea, Priority.ALWAYS);
    }

    private ListCell<String> createPromotionTypeCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : localizePromotionType(item));
                setAlignment(Pos.CENTER_LEFT);
                setMinHeight(34);
                setPrefHeight(34);
            }
        };
    }

    private ListCell<String> createPromotionFilterCell(boolean typeFilter) {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else if ("全部".equals(item)) {
                    setText(I18nManager.getInstance().get("promotion.filter.all"));
                } else if (typeFilter) {
                    setText(localizePromotionType(item));
                } else {
                    setText(I18nManager.getInstance().get(
                        "启用".equals(item) ? "promotion.status.enabled" : "promotion.status.disabled"));
                }
            }
        };
    }

    private String localizePromotionType(String type) {
        if ("打折".equals(type)) {
            return I18nManager.getInstance().get("promotion.type.discount");
        }
        if ("优惠券".equals(type)) {
            return I18nManager.getInstance().get("promotion.type.coupon");
        }
        if ("满减".equals(type)) {
            return I18nManager.getInstance().get("promotion.type.reduction");
        }
        return type == null ? "" : type;
    }

    private void updatePromotionFieldHints(String type, TextField thresholdField, TextField discountField) {
        if ("打折".equals(type)) {
            thresholdField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.promotion_discount_threshold_hint"));
            discountField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.promotion_discount_hint"));
        } else if ("优惠券".equals(type)) {
            thresholdField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.promotion_coupon_threshold_hint"));
            discountField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.promotion_coupon_value_hint"));
            if (thresholdField.getText().trim().isEmpty()) {
                thresholdField.setText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.MemberEdit.POINTS_HINT));
            }
        } else {
            thresholdField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.promotion_threshold_hint"));
            discountField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.promotion_reduction_value_hint"));
        }
    }

    private Promotion buildPromotionFromForm(Promotion promotion,
                                             TextField promotionCodeField,
                                             TextField nameField,
                                             ComboBox<String> typeComboBox,
                                             TextField thresholdField,
                                             TextField discountField,
                                             TextArea descriptionArea,
                                             DatePicker startDatePicker,
                                             DatePicker endDatePicker,
                                             TextField maxUsageField) {
        String code = promotionCodeField.getText().trim();
        String name = nameField.getText().trim();
        String type = typeComboBox.getSelectionModel().getSelectedItem();
        String thresholdText = thresholdField.getText().trim();
        String discountText = discountField.getText().trim();

        if (name.isEmpty()) {
            throw new IllegalArgumentException(I18nManager.getInstance().get("promotion.validation.name"));
        }
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException(I18nManager.getInstance().get("promotion.validation.type"));
        }
        if (thresholdText.isEmpty()) {
            throw new IllegalArgumentException(I18nManager.getInstance().get("promotion.validation.threshold"));
        }
        if (discountText.isEmpty()) {
            throw new IllegalArgumentException(getDiscountRequiredMessage(type));
        }
        if (startDatePicker.getValue() == null) {
            throw new IllegalArgumentException(I18nManager.getInstance().get("promotion.validation.start_date"));
        }
        if (endDatePicker.getValue() == null) {
            throw new IllegalArgumentException(I18nManager.getInstance().get("promotion.validation.end_date"));
        }
        if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
            throw new IllegalArgumentException(I18nManager.getInstance().get("promotion.validation.date_order"));
        }

        BigDecimal threshold = parseDecimal(thresholdText, I18nManager.getInstance().get("promotion.threshold"));
        BigDecimal discount = parseDecimal(discountText, I18nManager.getInstance().get("promotion.discount_value"));

        if (threshold.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(I18nManager.getInstance().get("promotion.validation.threshold_negative"));
        }
        validateDiscountValue(type, threshold, discount);

        String maxUsageText = maxUsageField.getText().trim();
        int maxUsage = -1;
        if (!maxUsageText.isEmpty()) {
            try {
                maxUsage = FormValidator.parseInt(maxUsageText);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(I18nManager.getInstance().get("promotion.validation.max_usage_integer"));
            }
            if (maxUsage <= 0) {
                throw new IllegalArgumentException(I18nManager.getInstance().get("promotion.validation.max_usage_positive"));
            }
        }

        Promotion newPromotion = promotion != null ? promotion : new Promotion();
        newPromotion.promotionCode = code.isEmpty() ? generatePromotionCode() : code;
        newPromotion.name = name;
        newPromotion.type = type;
        newPromotion.threshold = threshold;
        newPromotion.discount = discount;
        newPromotion.description = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();
        newPromotion.startDate = startDatePicker.getValue().atStartOfDay();
        newPromotion.endDate = endDatePicker.getValue().atTime(23, 59, 59);
        newPromotion.maxUsage = maxUsage;

        return newPromotion;
    }

    private BigDecimal parseDecimal(String text, String fieldName) {
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(I18nManager.getInstance().get("promotion.validation.number", fieldName));
        }
    }

    private String getDiscountRequiredMessage(String type) {
        if ("打折".equals(type)) {
            return I18nManager.getInstance().get("promotion.validation.discount_rate_required");
        }
        if ("优惠券".equals(type)) {
            return I18nManager.getInstance().get("promotion.validation.coupon_value_required");
        }
        return I18nManager.getInstance().get("promotion.validation.reduction_required");
    }

    private void validateDiscountValue(String type, BigDecimal threshold, BigDecimal discount) {
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(getDiscountRequiredMessage(type));
        }
        if ("打折".equals(type)) {
            if (discount.compareTo(BigDecimal.ONE) >= 0) {
                throw new IllegalArgumentException(I18nManager.getInstance().get("promotion.validation.discount_rate_range"));
            }
        } else if ("满减".equals(type) && threshold.compareTo(BigDecimal.ZERO) > 0
                && discount.compareTo(threshold) > 0) {
            throw new IllegalArgumentException(I18nManager.getInstance().get("promotion.validation.reduction_limit"));
        }
    }

    /**
     * 显示警告对话框
     */
    private void showAlert(String title, String message) {
        StatusBarManager.updateWarning(message);
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        StatusBarManager.updateWarning(message);
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(I18nManager.getInstance().get(I18nKeys.Common.WARNING));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 处理删除促销
     */
    @FXML
    public void handleDeletePromotion() {
        List<Promotion> selected = promotionTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showWarning(I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_PROMOTION));
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.confirm_delete"));
        alert.setHeaderText(null);
        alert.setContentText(com.cashier.i18n.I18nManager.getInstance().get("runtime.promotion_delete_confirm", selected.size()));

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                deletePromotions(new ArrayList<>(selected));
                loadPromotions();
                updateStatus(I18nManager.getInstance().get("promotion.deleted"));
            } catch (SQLException e) {
                logger.error("删除促销失败", e);
                showAlert(I18nManager.getInstance().get("message.delete.failed"),
                    I18nManager.getInstance().get("runtime.promotion_delete_error", e.getMessage()));
            }
        }
    }

    /**
     * 处理启用促销
     */
    @FXML
    public void handleEnablePromotion() {
        List<Promotion> selected = promotionTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showWarning(I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_PROMOTION));
            return;
        }
        try {
            updateSelectedPromotionState(selected, true);
            loadPromotions();
            updateStatus(I18nManager.getInstance().get("promotion.enabled"));
        } catch (SQLException e) {
            logger.error("启用促销失败", e);
            showAlert(I18nManager.getInstance().get("message.save.failed"),
                I18nManager.getInstance().get("runtime.promotion_save_error", e.getMessage()));
        }
    }

    /**
     * 处理禁用促销
     */
    @FXML
    public void handleDisablePromotion() {
        List<Promotion> selected = promotionTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showWarning(I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_PROMOTION));
            return;
        }
        try {
            updateSelectedPromotionState(selected, false);
            loadPromotions();
            updateStatus(I18nManager.getInstance().get("promotion.disabled"));
        } catch (SQLException e) {
            logger.error("禁用促销失败", e);
            showAlert(I18nManager.getInstance().get("message.save.failed"),
                I18nManager.getInstance().get("runtime.promotion_save_error", e.getMessage()));
        }
    }

    private void persistPromotion(Promotion promotion) throws SQLException {
        if (promotion.id > 0) {
            DAOFactory.getInstance().getPromotionDAO().update(promotion);
        } else {
            DAOFactory.getInstance().getPromotionDAO().insert(promotion);
        }
    }

    private void deletePromotions(List<Promotion> promotions) throws SQLException {
        for (Promotion promotion : promotions) {
            DAOFactory.getInstance().getPromotionDAO().delete(promotion.id);
        }
    }

    private void updateSelectedPromotionState(List<Promotion> selected, boolean enabled) throws SQLException {
        for (Promotion promotion : new ArrayList<>(selected)) {
            promotion.enabled = enabled;
            DAOFactory.getInstance().getPromotionDAO().update(promotion);
        }
    }

    /**
     * 处理搜索
     */
    @FXML
    public void handleSearch() {
        applyFilters();
    }

    /**
     * 处理清除搜索
     */
    @FXML
    public void handleClearSearch() {
        searchField.clear();
        typeFilterComboBox.getSelectionModel().select(0);
        statusFilterComboBox.getSelectionModel().select(0);
        applyFilters();
    }

    /**
     * 应用筛选条件
     */
    private void applyFilters() {
        String searchText = searchField.getText().trim().toLowerCase();
        String typeFilter = typeFilterComboBox.getSelectionModel().getSelectedItem();
        String statusFilter = statusFilterComboBox.getSelectionModel().getSelectedItem();

        promotionList.setAll(allPromotions.stream()
            .filter(p -> {
                // 类型筛选
                if (!"全部".equals(typeFilter) && !typeFilter.equals(p.type)) {
                    return false;
                }

                // 状态筛选
                if (!"全部".equals(statusFilter)) {
                    boolean enabled = "启用".equals(statusFilter);
                    if (p.enabled != enabled) {
                        return false;
                    }
                }

                // 搜索文本筛选
                if (!searchText.isEmpty()) {
                    return p.name.toLowerCase().contains(searchText) ||
                           p.description.toLowerCase().contains(searchText);
                }

                return true;
            })
            .toList());

        updateCountLabel();
    }

    /**
     * 更新状态
     * @param status 状态文本
     */
    private void updateStatus(String status) {
        StatusBarManager.updateSuccess(status);
    }

    /**
     * 刷新促销列表
     */
    public void refreshPromotions() {
        loadPromotions();
    }
}
