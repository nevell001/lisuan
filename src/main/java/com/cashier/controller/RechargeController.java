package com.cashier.controller;

import com.cashier.service.DataService;
import com.cashier.model.Member;
import com.cashier.model.RechargeRecord;
import com.cashier.util.CurrencyUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 会员充值控制器
 * 处理会员充值操作
 */
public class RechargeController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(RechargeController.class);

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
    private boolean okClicked = false;
    private double rechargeAmount = 0.0;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 初始化支付方式下拉框
        paymentMethodComboBox.getItems().addAll("现金", "微信", "支付宝", "银行卡");
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
     * 加载充值历史记录
     */
    private void loadRechargeHistory() {
        try {
            List<RechargeRecord> allRecords = DataService.loadRechargeRecords();
            List<RechargeRecord> memberRecords = new ArrayList<>();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (RechargeRecord record : allRecords) {
                if (record.memberPhone.equals(member.phone)) {
                    memberRecords.add(record);
                }
            }

            // 按时间倒序排列
            memberRecords.sort((r1, r2) -> r2.timestamp.compareTo(r1.timestamp));

            // 只显示最近10条记录
            if (memberRecords.size() > 10) {
                memberRecords = memberRecords.subList(0, 10);
            }

            historyTable.getItems().setAll(memberRecords);
        } catch (Exception e) {
            logger.error("加载充值历史记录失败", e);
        }
    }

    /**
     * 更新预览信息
     */
    private void updatePreview() {
        try {
            String amountText = amountField.getText().trim();
            if (amountText.isEmpty()) {
                newBalanceLabel.setText(CurrencyUtil.format(member.getBalance().doubleValue()));
                bonusPointsLabel.setText("0");
                okButton.setDisable(true);
                return;
            }

            double amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                newBalanceLabel.setText(CurrencyUtil.format(member.getBalance().doubleValue()));
                bonusPointsLabel.setText("0");
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
            bonusPointsLabel.setText("0");
            okButton.setDisable(true);
        }
    }

    /**
     * 处理确定按钮
     */
    @FXML
    private void handleOk() {
        if (isInputValid()) {
            rechargeAmount = Double.parseDouble(amountField.getText().trim());
            String paymentMethod = paymentMethodComboBox.getSelectionModel().getSelectedItem();

            boolean success = com.cashier.service.MemberService.recharge(member, rechargeAmount, paymentMethod, "系统");
            if (!success) {
                showError("会员充值失败，请稍后重试。");
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
    private void handleCancel() {
        dialogStage.close();
    }

    /**
     * 验证输入
     * @return 输入是否有效
     */
    private boolean isInputValid() {
        String errorMessage = "";

        try {
            String amountText = amountField.getText().trim();
            if (amountText.isEmpty()) {
                errorMessage += "充值金额不能为空！\n";
            } else {
                double amount = Double.parseDouble(amountText);
                if (amount <= 0) {
                    errorMessage += "充值金额必须大于0！\n";
                }
                if (amount > 10000) {
                    errorMessage += "单次充值金额不能超过10000元！\n";
                }
            }
        } catch (NumberFormatException e) {
            errorMessage += "充值金额格式不正确！\n";
        }

        if (paymentMethodComboBox.getSelectionModel().getSelectedItem() == null) {
            errorMessage += "请选择支付方式！\n";
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("输入错误");
            alert.setHeaderText(null);
            alert.setContentText(errorMessage);
            alert.showAndWait();
            return false;
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
