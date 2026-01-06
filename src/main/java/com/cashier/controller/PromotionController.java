package com.cashier.controller;

import com.cashier.model.DataManager;
import com.cashier.model.Promotion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 促销管理控制器
 * 处理促销活动的增删改查
 */
public class PromotionController {

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

    private ObservableList<Promotion> promotionList;
    private List<Promotion> allPromotions;

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
        typeFilterComboBox.getSelectionModel().select(0);

        // 初始化状态筛选下拉框
        statusFilterComboBox.setItems(FXCollections.observableArrayList(
            "全部",
            "启用",
            "禁用"
        ));
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
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        periodColumn.setCellValueFactory(cellData -> {
            Promotion p = cellData.getValue();
            return new SimpleStringProperty(String.format("%s 至 %s",
                sdf.format(p.startDate),
                sdf.format(p.endDate)));
        });

        usageColumn.setCellValueFactory(cellData -> {
            Promotion p = cellData.getValue();
            String maxUsage = p.maxUsage == -1 ? "无限制" : String.valueOf(p.maxUsage);
            return new SimpleStringProperty(String.format("%d/%s", p.usageCount, maxUsage));
        });

        statusColumn.setCellValueFactory(cellData -> {
            Promotion p = cellData.getValue();
            String status = p.enabled ? "启用" : "禁用";
            String validity = p.isValid() ? "有效" : "已过期";
            return new SimpleStringProperty(status + " (" + validity + ")");
        });
    }

    /**
     * 加载促销数据
     */
    private void loadPromotions() {
        System.out.println("PromotionController: 开始加载促销数据...");
        allPromotions = DataManager.loadPromotions();
        promotionList = FXCollections.observableArrayList(allPromotions);
        promotionTable.setItems(promotionList);
        updateCountLabel();
        System.out.println("PromotionController: 加载了 " + allPromotions.size() + " 条促销记录");
    }

    /**
     * 更新促销数量标签
     */
    private void updateCountLabel() {
        countLabel.setText("促销数量: " + promotionList.size());
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
    private void handleAddPromotion() {
        showPromotionDialog(null);
    }

    /**
     * 处理编辑促销
     */
    @FXML
    private void handleEditPromotion() {
        Promotion selected = promotionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showPromotionDialog(selected);
        }
    }

    /**
     * 显示促销对话框
     * @param promotion 要编辑的促销，null表示添加新促销
     */
    private void showPromotionDialog(Promotion promotion) {
        Dialog<Promotion> dialog = new Dialog<>();
        dialog.setTitle(promotion == null ? "添加促销" : "编辑促销");
        dialog.setHeaderText(null);

        // 创建对话框内容
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        ComboBox<String> typeComboBox = new ComboBox<>();
        TextField thresholdField = new TextField();
        TextField discountField = new TextField();
        TextArea descriptionArea = new TextArea();
        DatePicker startDatePicker = new DatePicker();
        DatePicker endDatePicker = new DatePicker();
        TextField maxUsageField = new TextField();

        typeComboBox.setItems(FXCollections.observableArrayList("满减", "打折", "优惠券"));

        if (promotion != null) {
            nameField.setText(promotion.name);
            typeComboBox.getSelectionModel().select(promotion.type);
            thresholdField.setText(String.valueOf(promotion.threshold));
            discountField.setText(String.valueOf(promotion.discount));
            descriptionArea.setText(promotion.description);
            startDatePicker.setValue(java.time.LocalDate.of(
                promotion.startDate.getYear() + 1900,
                promotion.startDate.getMonth() + 1,
                promotion.startDate.getDate()));
            endDatePicker.setValue(java.time.LocalDate.of(
                promotion.endDate.getYear() + 1900,
                promotion.endDate.getMonth() + 1,
                promotion.endDate.getDate()));
            maxUsageField.setText(promotion.maxUsage == -1 ? "" : String.valueOf(promotion.maxUsage));
        }

        grid.add(new Label("促销名称:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("促销类型:"), 0, 1);
        grid.add(typeComboBox, 1, 1);
        grid.add(new Label("门槛金额:"), 0, 2);
        grid.add(thresholdField, 1, 2);
        grid.add(new Label("折扣值:"), 0, 3);
        grid.add(discountField, 1, 3);
        grid.add(new Label("描述:"), 0, 4);
        grid.add(descriptionArea, 1, 4);
        grid.add(new Label("开始日期:"), 0, 5);
        grid.add(startDatePicker, 1, 5);
        grid.add(new Label("结束日期:"), 0, 6);
        grid.add(endDatePicker, 1, 6);
        grid.add(new Label("最大使用次数:"), 0, 7);
        grid.add(maxUsageField, 1, 7);

        dialog.getDialogPane().setContent(grid);

        ButtonType okButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                Promotion newPromotion = promotion != null ? promotion : new Promotion();
                newPromotion.id = promotion != null ? promotion.id : "PROM" + System.currentTimeMillis();
                newPromotion.name = nameField.getText().trim();
                newPromotion.type = typeComboBox.getSelectionModel().getSelectedItem();
                newPromotion.threshold = Double.parseDouble(thresholdField.getText().trim());
                newPromotion.discount = Double.parseDouble(discountField.getText().trim());
                newPromotion.description = descriptionArea.getText().trim();

                if (startDatePicker.getValue() != null) {
                    newPromotion.startDate = java.sql.Date.valueOf(startDatePicker.getValue());
                }
                if (endDatePicker.getValue() != null) {
                    newPromotion.endDate = java.sql.Date.valueOf(endDatePicker.getValue());
                }

                String maxUsageText = maxUsageField.getText().trim();
                newPromotion.maxUsage = maxUsageText.isEmpty() ? -1 : Integer.parseInt(maxUsageText);

                return newPromotion;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (promotion == null) {
                allPromotions.add(result);
            }
            DataManager.savePromotions(allPromotions);
            loadPromotions();
            updateStatus(promotion == null ? "促销添加成功" : "促销更新成功");
        });
    }

    /**
     * 处理删除促销
     */
    @FXML
    private void handleDeletePromotion() {
        List<Promotion> selected = promotionTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText(null);
        alert.setContentText("确定要删除选中的 " + selected.size() + " 个促销吗？");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            allPromotions.removeAll(selected);
            DataManager.savePromotions(allPromotions);
            loadPromotions();
            updateStatus("促销删除成功");
        }
    }

    /**
     * 处理启用促销
     */
    @FXML
    private void handleEnablePromotion() {
        List<Promotion> selected = promotionTable.getSelectionModel().getSelectedItems();
        for (Promotion p : selected) {
            p.enabled = true;
        }
        DataManager.savePromotions(allPromotions);
        loadPromotions();
        updateStatus("促销已启用");
    }

    /**
     * 处理禁用促销
     */
    @FXML
    private void handleDisablePromotion() {
        List<Promotion> selected = promotionTable.getSelectionModel().getSelectedItems();
        for (Promotion p : selected) {
            p.enabled = false;
        }
        DataManager.savePromotions(allPromotions);
        loadPromotions();
        updateStatus("促销已禁用");
    }

    /**
     * 处理搜索
     */
    @FXML
    private void handleSearch() {
        applyFilters();
    }

    /**
     * 处理清除搜索
     */
    @FXML
    private void handleClearSearch() {
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
        // TODO: 更新主界面的状态栏
        System.out.println("状态: " + status);
    }

    /**
     * 刷新促销列表
     */
    public void refreshPromotions() {
        loadPromotions();
    }
}