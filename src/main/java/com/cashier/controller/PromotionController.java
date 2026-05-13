package com.cashier.controller;

import com.cashier.service.DataService;
import com.cashier.model.Promotion;
import com.cashier.util.StatusBarManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;
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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        periodColumn.setCellValueFactory(cellData -> {
            Promotion p = cellData.getValue();
            LocalDate startDate = p.startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate endDate = p.endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return new SimpleStringProperty(String.format("%s 至 %s",
                startDate.format(formatter),
                endDate.format(formatter)));
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
        logger.info("PromotionController: 开始加载促销数据...");
        allPromotions = DataService.loadPromotions();
        promotionList = FXCollections.observableArrayList(allPromotions);
        promotionTable.setItems(promotionList);
        updateCountLabel();
        logger.info("PromotionController: 加载了 {} 条促销记录", allPromotions.size());
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

        TextField promotionCodeField = new TextField();
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
            promotionCodeField.setText(promotion.promotionCode);
            promotionCodeField.setDisable(true);  // 编辑时禁用促销编号
            nameField.setText(promotion.name);
            typeComboBox.getSelectionModel().select(promotion.type);
            thresholdField.setText(String.valueOf(promotion.threshold));
            discountField.setText(String.valueOf(promotion.discount));
            descriptionArea.setText(promotion.description);
            startDatePicker.setValue(promotion.startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            endDatePicker.setValue(promotion.endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            maxUsageField.setText(promotion.maxUsage == -1 ? "" : String.valueOf(promotion.maxUsage));
        } else {
            // 新建促销时自动生成编号
            promotionCodeField.setText(generatePromotionCode());
            promotionCodeField.setDisable(true);  // 禁用促销编号字段
        }

        grid.add(new Label("促销编号:"), 0, 0);
        grid.add(promotionCodeField, 1, 0);
        grid.add(new Label("促销名称:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("促销类型:"), 0, 2);
        grid.add(typeComboBox, 1, 2);
        grid.add(new Label("门槛金额:"), 0, 3);
        grid.add(thresholdField, 1, 3);
        grid.add(new Label("折扣值:"), 0, 4);
        grid.add(discountField, 1, 4);
        grid.add(new Label("描述:"), 0, 5);
        grid.add(descriptionArea, 1, 5);
        grid.add(new Label("开始日期:"), 0, 6);
        grid.add(startDatePicker, 1, 6);
        grid.add(new Label("结束日期:"), 0, 7);
        grid.add(endDatePicker, 1, 7);
        grid.add(new Label("最大使用次数:"), 0, 8);
        grid.add(maxUsageField, 1, 8);

        dialog.getDialogPane().setContent(grid);

        ButtonType okButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                try {
                    // 验证必填字段
                    String code = promotionCodeField.getText().trim();
                    String name = nameField.getText().trim();
                    String type = typeComboBox.getSelectionModel().getSelectedItem();
                    String thresholdText = thresholdField.getText().trim();
                    String discountText = discountField.getText().trim();

                    if (name.isEmpty()) {
                        throw new IllegalArgumentException("促销名称不能为空");
                    }
                    if (type == null || type.isEmpty()) {
                        throw new IllegalArgumentException("请选择促销类型");
                    }
                    if (thresholdText.isEmpty()) {
                        throw new IllegalArgumentException("门槛金额不能为空");
                    }
                    if (discountText.isEmpty()) {
                        throw new IllegalArgumentException("折扣值不能为空");
                    }

                    // 验证日期
                    if (startDatePicker.getValue() == null) {
                        throw new IllegalArgumentException("请选择开始日期");
                    }
                    if (endDatePicker.getValue() == null) {
                        throw new IllegalArgumentException("请选择结束日期");
                    }
                    if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
                        throw new IllegalArgumentException("结束日期不能早于开始日期");
                    }

                    // 解析数字字段
                    BigDecimal threshold = new BigDecimal(thresholdText);
                    BigDecimal discount = new BigDecimal(discountText);

                    if (threshold.compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException("门槛金额不能为负数");
                    }
                    if (discount.compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException("折扣值不能为负数");
                    }

                    // 解析最大使用次数
                    String maxUsageText = maxUsageField.getText().trim();
                    int maxUsage = maxUsageText.isEmpty() ? -1 : Integer.parseInt(maxUsageText);

                    if (maxUsage != -1 && maxUsage < 0) {
                        throw new IllegalArgumentException("最大使用次数不能为负数");
                    }

                    // 创建或更新促销对象
                    Promotion newPromotion = promotion != null ? promotion : new Promotion();
                    newPromotion.promotionCode = code.isEmpty() ? generatePromotionCode() : code;
                    newPromotion.name = name;
                    newPromotion.type = type;
                    newPromotion.threshold = threshold;
                    newPromotion.discount = discount;
                    newPromotion.description = descriptionArea.getText().trim();
                    newPromotion.startDate = java.sql.Date.valueOf(startDatePicker.getValue());
                    newPromotion.endDate = java.sql.Date.valueOf(endDatePicker.getValue());
                    newPromotion.maxUsage = maxUsage;

                    return newPromotion;
                } catch (NumberFormatException e) {
                    logger.error("促销数据格式错误", e);
                    showAlert("输入错误", "请输入有效的数字");
                } catch (IllegalArgumentException e) {
                    logger.error("促销数据验证失败", e);
                    showAlert("验证错误", e.getMessage());
                } catch (Exception e) {
                    logger.error("保存促销失败", e);
                    showAlert("保存失败", "保存促销时发生错误: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                if (promotion == null) {
                    allPromotions.add(result);
                }
                DataService.savePromotions(allPromotions);
                loadPromotions();
                updateStatus(promotion == null ? "促销添加成功" : "促销更新成功");
            } catch (Exception e) {
                logger.error("保存促销失败", e);
                showAlert("保存失败", "保存促销时发生错误: " + e.getMessage());
            }
        });
    }

    /**
     * 生成促销编号
     */
    private String generatePromotionCode() {
        return "P" + System.currentTimeMillis();
    }

    /**
     * 显示警告对话框
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
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
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText(null);
        alert.setContentText("确定要删除选中的 " + selected.size() + " 个促销吗？");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            allPromotions.removeAll(selected);
            DataService.savePromotions(allPromotions);
            loadPromotions();
            updateStatus("促销删除成功");
        }
    }

    /**
     * 处理启用促销
     */
    @FXML
    public void handleEnablePromotion() {
        List<Promotion> selected = promotionTable.getSelectionModel().getSelectedItems();
        for (Promotion p : selected) {
            p.enabled = true;
        }
        DataService.savePromotions(allPromotions);
        loadPromotions();
        updateStatus("促销已启用");
    }

    /**
     * 处理禁用促销
     */
    @FXML
    public void handleDisablePromotion() {
        List<Promotion> selected = promotionTable.getSelectionModel().getSelectedItems();
        for (Promotion p : selected) {
            p.enabled = false;
        }
        DataService.savePromotions(allPromotions);
        loadPromotions();
        updateStatus("促销已禁用");
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
        StatusBarManager.updateStatus(status);
    }

    /**
     * 刷新促销列表
     */
    public void refreshPromotions() {
        loadPromotions();
    }
}