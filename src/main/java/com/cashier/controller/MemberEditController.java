package com.cashier.controller;

import com.cashier.dao.MemberDAO;
import com.cashier.model.Member;
import com.cashier.service.MemberService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.Map;

/**
 * 会员编辑控制器
 * 处理会员添加和编辑对话框的逻辑
 */
public class MemberEditController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(MemberEditController.class);

    @FXML
    private Label titleLabel;

    @FXML
    private TextField memberCodeField;

    @FXML
    private CheckBox autoCodeCheckBox;

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
        try {
            var memberList = MemberDAO.findAll();
            members = new java.util.HashMap<>();
            for (Member m : memberList) {
                members.put(m.phone, m);
            }
        } catch (SQLException e) {
            logger.error("加载会员数据失败", e);
            members = new java.util.HashMap<>();
        }

        // 初始化等级下拉框
        levelComboBox.setItems(FXCollections.observableArrayList(
            "普通", "银卡", "金卡", "钻石"
        ));
        levelComboBox.getSelectionModel().select(0);

        // 设置默认折扣
        discountField.setText("10.0");

        // 设置自动编号复选框默认选中
        autoCodeCheckBox.setSelected(true);
        memberCodeField.setDisable(true);

        // 添加复选框监听器
        autoCodeCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            memberCodeField.setDisable(newVal);
        });
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
            memberCodeField.setText(member.memberCode);
            memberCodeField.setDisable(true);
            autoCodeCheckBox.setDisable(true);
            phoneField.setText(member.phone);
            phoneField.setDisable(true); // 手机号不可修改
            nameField.setText(member.name);
            pointsField.setText(String.valueOf(member.getPoints().intValue()));
            levelComboBox.getSelectionModel().select(member.level);
            discountField.setText(String.valueOf(member.getDiscount()));
            balanceField.setText(String.format("%.2f", member.getBalance()));
            birthdayField.setText(member.birthday);
        } else {
            // 添加模式
            titleLabel.setText("添加会员");
            phoneField.setDisable(false);
            autoCodeCheckBox.setDisable(false);
            autoCodeCheckBox.setSelected(true);
            memberCodeField.setDisable(true);
            memberCodeField.clear();
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
    public void handleSave() {
        if (isInputValid()) {
            if (member == null) {
                // 添加新会员
                member = new Member(
                    phoneField.getText().trim(),
                    nameField.getText().trim()
                );
            }

            // 更新会员信息
            member.memberCode = memberCodeField.getText().trim();
            member.points = new BigDecimal(pointsField.getText().trim());
            member.level = levelComboBox.getSelectionModel().getSelectedItem();
            member.discount = new BigDecimal(discountField.getText().trim());
            member.discountRate = member.discount;
            member.balance = new BigDecimal(balanceField.getText().trim());
            member.birthday = birthdayField.getText().trim();

            // 根据新的积分自动计算并更新会员等级
            try {
                MemberService.updateMemberLevel(member);
                // 如果等级已更新，同步到对话框显示
                member.level = MemberService.calculateLevel(member.points);
                member.discount = MemberService.getDiscountByLevelDecimal(member.level);
                member.discountRate = member.discount;
                logger.info("会员 {} 等级已根据积分自动更新: 积分={}, 等级={}",
                    member.phone, member.points, member.level);
            } catch (Exception e) {
                logger.error("自动更新会员等级失败", e);
            }

            okClicked = true;
            dialogStage.close();
        }
    }

    /**
     * 处理取消
     */
    @FXML
    public void handleCancel() {
        dialogStage.close();
    }

    /**
     * 验证输入
     * @return 如果输入有效返回true，否则返回false
     */
    private boolean isInputValid() {
        String errorMessage = "";

        // 验证会员编号（仅当手动输入时）
        if (!autoCodeCheckBox.isSelected() && memberCodeField.getText().trim().isEmpty()) {
            errorMessage += "会员编号不能为空！\n";
        }

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