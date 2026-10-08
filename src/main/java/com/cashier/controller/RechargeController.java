package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.i18n.I18nManager;
import com.cashier.dao.DAOFactory;
import com.cashier.model.Member;
import com.cashier.model.RechargeRecord;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.FormValidator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.StatusBarManager;

import java.math.BigDecimal;
import java.util.*;

/**
 * 会员充值控制器
 * 处理会员充值操作
 */
public class RechargeController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(RechargeController.class);
    private static final int RECHARGE_HISTORY_LIMIT = 10;
    /** 单次充值上限（元）；提示文案里的数字与这条校验共用，避免改了阈值忘了文案 */
    private static final int MAX_RECHARGE_AMOUNT = 10000;

    @FXML
    private Label memberNameLabel;

    @FXML
    private Label memberPhoneLabel;

    @FXML
    private Label currentBalanceLabel;

    @FXML
    private Label currentPointsLabel;

    @FXML
    private TextField amountField;

    @FXML
    private ComboBox<String> paymentMethodComboBox;

    @FXML
    private Label newBalanceLabel;

    @FXML
    private Label bonusPointsLabel;

    @FXML
    private Button okButton;

    @FXML
    private Button cancelButton;

    @FXML
    private TableView<RechargeRecord> historyTable;

    @FXML
    private TableColumn<RechargeRecord, String> dateColumn;

    @FXML
    private TableColumn<RechargeRecord, String> amountColumn;

    @FXML
    private TableColumn<RechargeRecord, String> paymentColumn;

    @FXML
    private TableColumn<RechargeRecord, String> operatorColumn;

    private Stage dialogStage;
    private Member member;
    private String operatorName = "系统"; // 默认值，可由外部设置实际操作员
    private boolean okClicked = false;
    private double rechargeAmount = 0.0;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 初始化支付方式下拉框
        // 下拉项仍是**规范中文落库值**（TD-002），只是显示层按当前语言本地化
        paymentMethodComboBox.getItems().addAll("现金", "微信", "支付宝", "银行卡");
        paymentMethodComboBox.setConverter(new javafx.util.StringConverter<String>() {
            @Override
            public String toString(String value) {
                return value == null ? "" : com.cashier.util.I18nUiUtils.paymentMethod(value);
            }

            @Override
            public String fromString(String text) {
                return text;
            }
        });
        paymentMethodComboBox.getSelectionModel().select("现金");

        // 设置历史记录表格列
        setupHistoryTableColumns();

        // 监听金额输入变化
        amountField.textProperty().addListener((obs, oldVal, newVal) -> updatePreview());

        // 初始化按钮状态
        okButton.setDisable(true);
    }

    /**
     * 设置历史记录表格列
     */
    private void setupHistoryTableColumns() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        amountColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(CurrencyUtil.format(cellData.getValue().amount.doubleValue())));
        paymentColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        operatorColumn.setCellValueFactory(new PropertyValueFactory<>("operator"));
    }

    /**
     * 设置对话框阶段
     * @param dialogStage 对话框阶段
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * 设置会员信息
     * @param member 会员
     */
    public void setMember(Member member) {
        this.member = member;

        // 更新会员信息显示
        memberNameLabel.setText(member.name);
        memberPhoneLabel.setText(member.phone);
        currentBalanceLabel.setText(CurrencyUtil.format(member.getBalance().doubleValue()));
        currentPointsLabel.setText(String.valueOf(member.getPoints().intValue()));

        // 加载充值历史记录
        loadRechargeHistory();
    }

    /**
     * 设置操作员用户名（用于审计记录）
     * @param operatorName 实际操作员用户名
     */
    public void setOperatorName(String operatorName) {
        if (operatorName != null && !operatorName.isBlank()) {
            this.operatorName = operatorName;
        }
    }

    /**
     * 加载充值历史记录（后台线程执行 DB 查询，Platform.runLater 更新 UI）
     */
    private void loadRechargeHistory() {
        new Thread(() -> {
            try {
                List<RechargeRecord> memberRecords = DAOFactory.getInstance().getRechargeRecordDAO().findRecentByMemberPhone(
                    member.phone,
                    RECHARGE_HISTORY_LIMIT
                );
                Platform.runLater(() -> {
                    historyTable.getItems().setAll(memberRecords);
                });
            } catch (Exception e) {
                // 查库失败不能表现成"这个会员没有充值记录"：要说清是加载失败（审计 F11）
                logger.error("加载充值历史记录失败", e);
                Platform.runLater(() -> showError(
                    com.cashier.i18n.I18nManager.getInstance().get(com.cashier.i18n.I18nKeys.Error.LOAD_DATA)));
            }
        }).start();
    }

    /**
     * 更新预览信息
     */
    private void updatePreview() {
        try {
            String amountText = amountField.getText().trim();
            if (amountText.isEmpty()) {
                newBalanceLabel.setText(CurrencyUtil.format(member.getBalance().doubleValue()));
                bonusPointsLabel.setText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.MemberEdit.POINTS_HINT));
                okButton.setDisable(true);
                return;
            }

            double amount = FormValidator.parseDouble(amountText, 0);
            if (amount <= 0) {
                newBalanceLabel.setText(CurrencyUtil.format(member.getBalance().doubleValue()));
                bonusPointsLabel.setText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.MemberEdit.POINTS_HINT));
                okButton.setDisable(true);
                return;
            }

            BigDecimal rechargeAmountDecimal = BigDecimal.valueOf(amount);

            // 计算赠送积分（1元=10积分）
            int bonusPoints = rechargeAmountDecimal.multiply(BigDecimal.TEN).intValue();
            BigDecimal newBalance = member.getBalance().add(rechargeAmountDecimal);

            newBalanceLabel.setText(CurrencyUtil.format(newBalance.doubleValue()));
            bonusPointsLabel.setText(String.valueOf(bonusPoints));
            okButton.setDisable(false);

        } catch (NumberFormatException e) {
            newBalanceLabel.setText(CurrencyUtil.format(member.getBalance().doubleValue()));
            bonusPointsLabel.setText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.MemberEdit.POINTS_HINT));
            okButton.setDisable(true);
        }
    }

    /**
     * 处理确定按钮
     */
    @FXML
    public void handleOk() {
        if (isInputValid()) {
            rechargeAmount = FormValidator.parseDouble(amountField.getText().trim());
            String paymentMethod = paymentMethodComboBox.getSelectionModel().getSelectedItem();

            boolean success = com.cashier.service.MemberService.recharge(member, rechargeAmount, paymentMethod, operatorName);
            if (!success) {
                showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.recharge_failed"));
                return;
            }

            okClicked = true;
            dialogStage.close();
        }
    }

    /**
     * 处理取消按钮
     */
    @FXML
    public void handleCancel() {
        dialogStage.close();
    }

    /**
     * 验证输入
     * @return 输入是否有效
     */
    private boolean isInputValid() {
        String errorMessage = "";
        // 校验文案走 i18n：这些串是拼进变量的，门禁的"调用点"规则看不见（2026-10 F14 第二批）
        com.cashier.i18n.I18nManager i18n = com.cashier.i18n.I18nManager.getInstance();

        try {
            String amountText = amountField.getText().trim();
            if (amountText.isEmpty()) {
                errorMessage += i18n.get("recharge.validation.amount_empty") + "\n";
            } else {
                double amount = FormValidator.parseDouble(amountText);
                if (amount <= 0) {
                    errorMessage += i18n.get("recharge.validation.amount_positive") + "\n";
                }
                if (amount > MAX_RECHARGE_AMOUNT) {
                    errorMessage += i18n.get("recharge.validation.amount_max", MAX_RECHARGE_AMOUNT) + "\n";
                }
            }
        } catch (IllegalArgumentException e) {
            errorMessage += i18n.get("recharge.validation.amount_format") + "\n";
        }

        if (paymentMethodComboBox.getSelectionModel().getSelectedItem() == null) {
            errorMessage += i18n.get("recharge.validation.payment_method") + "\n";
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            StatusBarManager.updateError(errorMessage);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.input_error"));
            alert.setHeaderText(null);
            alert.setContentText(errorMessage);
            alert.showAndWait();
            return false;
        }
    }

    private void showError(String message) {
        com.cashier.util.FXUtils.showError(message);
    }

    /**
     * 返回是否点击了确定
     * @return 是否点击了确定
     */
    public boolean isOkClicked() {
        return okClicked;
    }

    /**
     * 获取充值金额
     * @return 充值金额
     */
    public double getRechargeAmount() {
        return rechargeAmount;
    }
}
