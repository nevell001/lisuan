package com.cashier.controller;

import com.cashier.model.DataManager;
import com.cashier.model.Member;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.Map;

/**
 * 会员编辑控制器
 * 处理会员添加和编辑对话框的逻辑
 */
public class MemberEditController {

    @FXML
    private Label titleLabel;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField nameField;

    @FXML
    private TextField pointsField;

    @FXML
    private ComboBox<String> levelComboBox;

    @FXML
    private TextField discountField;

    @FXML
    private TextField balanceField;

    @FXML
    private TextField birthdayField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button cancelButton;

    @FXML
    private Button saveButton;

    private Stage dialogStage;
    private Member member;
    private boolean okClicked = false;
    private Map<String, Member> members;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 加载会员数据
        members = DataManager.loadMembers();

        // 初始化等级下拉框
        levelComboBox.setItems(FXCollections.observableArrayList(
            "普通", "银卡", "金卡", "钻石"
        ));
        levelComboBox.getSelectionModel().select(0);

        // 设置默认折扣
        discountField.setText("10.0");
    }

    /**
     * 设置对话框舞台
     * @param dialogStage 对话框舞台
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * 设置要编辑的会员
     * @param member 会员对象
     */
    public void setMember(Member member) {
        this.member = member;

        if (member != null) {
            // 编辑模式
            titleLabel.setText("编辑会员");
            phoneField.setText(member.phone);
            phoneField.setDisable(true); // 手机号不可修改
            nameField.setText(member.name);
            pointsField.setText(String.valueOf((int)member.points));
            levelComboBox.getSelectionModel().select(member.level);
            discountField.setText(String.valueOf(member.discount));
            balanceField.setText(String.format("%.2f", member.balance));
            birthdayField.setText(member.birthday);
        } else {
            // 添加模式
            titleLabel.setText("添加会员");
            phoneField.setDisable(false);
        }
    }

    /**
     * 获取编辑后的会员
     * @return 会员对象
     */
    public Member getMember() {
        return member;
    }

    /**
     * 是否点击了确定按钮
     * @return 如果点击了确定返回true，否则返回false
     */
    public boolean isOkClicked() {
        return okClicked;
    }

    /**
     * 处理保存
     */
    @FXML
    private void handleSave() {
        if (isInputValid()) {
            if (member == null) {
                // 添加新会员
                member = new Member(
                    phoneField.getText().trim(),
                    nameField.getText().trim()
                );
            }

            // 更新会员信息
            member.points = Double.parseDouble(pointsField.getText().trim());
            member.level = levelComboBox.getSelectionModel().getSelectedItem();
            member.discount = Double.parseDouble(discountField.getText().trim());
            member.balance = Double.parseDouble(balanceField.getText().trim());
            member.birthday = birthdayField.getText().trim();

            okClicked = true;
            dialogStage.close();
        }
    }

    /**
     * 处理取消
     */
    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    /**
     * 验证输入
     * @return 如果输入有效返回true，否则返回false
     */
    private boolean isInputValid() {
        String errorMessage = "";

        // 验证手机号
        String phone = phoneField.getText().trim();
        if (phone.isEmpty()) {
            errorMessage += "手机号不能为空！\n";
        } else if (!phone.matches("\\d{11}")) {
            errorMessage += "手机号格式不正确（必须是11位数字）！\n";
        } else if (member == null && members.containsKey(phone)) {
            errorMessage += "该手机号已注册！\n";
        }

        // 验证姓名
        if (nameField.getText().trim().isEmpty()) {
            errorMessage += "姓名不能为空！\n";
        }

        // 验证积分
        try {
            double points = Double.parseDouble(pointsField.getText().trim());
            if (points < 0) {
                errorMessage += "积分不能为负数！\n";
            }
        } catch (NumberFormatException e) {
            errorMessage += "积分格式不正确！\n";
        }

        // 验证折扣
        try {
            double discount = Double.parseDouble(discountField.getText().trim());
            if (discount < 0 || discount > 10) {
                errorMessage += "折扣必须在0到10之间（0表示免费，10表示不打折）！\n";
            }
        } catch (NumberFormatException e) {
            errorMessage += "折扣格式不正确！\n";
        }

        // 验证余额
        try {
            double balance = Double.parseDouble(balanceField.getText().trim());
            if (balance < 0) {
                errorMessage += "余额不能为负数！\n";
            }
        } catch (NumberFormatException e) {
            errorMessage += "余额格式不正确！\n";
        }

        // 验证生日格式
        String birthday = birthdayField.getText().trim();
        if (!birthday.isEmpty() && !birthday.matches("\\d{2}-\\d{2}")) {
            errorMessage += "生日格式不正确（格式：MM-dd）！\n";
        }

        if (errorMessage.isEmpty()) {
            errorLabel.setText("");
            return true;
        } else {
            errorLabel.setText(errorMessage);
            return false;
        }
    }
}