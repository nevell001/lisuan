package com.cashier.controller;

import com.cashier.dao.DAOFactory;
import com.cashier.model.Member;
import com.cashier.service.MemberService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import javafx.scene.control.*;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

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

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 初始化等级下拉框
        levelComboBox.setItems(FXCollections.observableArrayList(
            "普通", "银卡", "金卡", "钻石"
        ));
        levelComboBox.setCellFactory(listView -> createLevelCell());
        levelComboBox.setButtonCell(createLevelCell());
        levelComboBox.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(levelComboBox, Priority.ALWAYS);
        levelComboBox.getSelectionModel().select(0);

        // 设置默认折扣
        pointsField.setText("0");
        discountField.setText("10");
        balanceField.setText("0.00");

        errorLabel.visibleProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        // 设置自动编号复选框默认选中
        autoCodeCheckBox.setSelected(true);
        memberCodeField.setDisable(true);

        // 添加复选框监听器
        autoCodeCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            memberCodeField.setDisable(newVal);
        });
    }

    private ListCell<String> createLevelCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    String key = switch (item) {
                        case "银卡" -> "member.level.silver";
                        case "金卡" -> "member.level.gold";
                        case "钻石" -> "member.level.diamond";
                        default -> "member.level.regular";
                    };
                    setText(com.cashier.i18n.I18nManager.getInstance().get(key));
                }
                setAlignment(Pos.CENTER_LEFT);
                setMinHeight(34);
                setPrefHeight(34);
            }
        };
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
            titleLabel.setText(com.cashier.i18n.I18nManager.getInstance().get("member.edit.title"));
            memberCodeField.setText(member.memberCode);
            memberCodeField.setDisable(true);
            autoCodeCheckBox.setDisable(true);
            phoneField.setText(member.phone);
            phoneField.setDisable(true); // 手机号不可修改
            nameField.setText(member.name);
            pointsField.setText(String.valueOf(member.getPoints().intValue()));
            levelComboBox.getSelectionModel().select(member.level);
            discountField.setText(String.valueOf(member.getDiscount()));
            balanceField.setText(String.format(java.util.Locale.ROOT, "%.2f", member.getBalance()));
            birthdayField.setText(member.birthday);
        } else {
            // 添加模式
            titleLabel.setText(com.cashier.i18n.I18nManager.getInstance().get("member.add.title"));
            phoneField.setDisable(false);
            autoCodeCheckBox.setDisable(false);
            autoCodeCheckBox.setSelected(true);
            memberCodeField.setDisable(true);
            memberCodeField.clear();
        }
    }

    /**
     * 受限角色（收银员）不得修改"金额相关"字段：等级、折扣、积分、余额。
     *
     * <p>等级与折扣由**积分**推导，故积分必须一起锁；余额是会员储值金额（等同于钱），
     * 与 REST API 把 {@code POST /api/members/{id}/recharge} 限为 finance/admin 的口径一致。
     * 收银员的充值走独立的充值流程（会写充值流水），不依赖本对话框。</p>
     */
    private boolean sensitiveFieldsEditable = true;
    private String lockedLevel;
    private java.math.BigDecimal lockedDiscount;
    private java.math.BigDecimal lockedPoints;
    private java.math.BigDecimal lockedBalance;

    /**
     * 设置能否修改等级/折扣（与 REST API 口径一致：{@code PUT /api/members/{id}} 在
     * {@code AuthorizationMiddleware} 里限 finance/admin）。
     *
     * <p>注意：等级与折扣在 {@link #handleSave()} 里由**积分**推导
     * （{@code MemberService.calculateLevel(points)}），所以只锁这两个字段挡不住改折扣——
     * 积分字段必须一起锁；并且保存时把三者还原为打开对话框时的值，
     * 防止程序化改动（禁用控件本身不阻止代码赋值）绕过。</p>
     */
    public void setSensitiveFieldsEditable(boolean editable) {
        this.sensitiveFieldsEditable = editable;
        levelComboBox.setDisable(!editable);
        discountField.setDisable(!editable);
        pointsField.setDisable(!editable);
        balanceField.setDisable(!editable);
        if (!editable && member != null) {
            lockedLevel = member.level;
            lockedDiscount = member.getDiscount();
            lockedPoints = member.getPoints();
            lockedBalance = member.getBalance();
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

            // 受限角色：等级/折扣/积分/余额一律还原为打开对话框时的值。
            // 上面那段由积分推导等级，因此只禁用控件是不够的（程序化赋值照样能改）。
            if (!sensitiveFieldsEditable && lockedLevel != null) {
                member.level = lockedLevel;
                member.discount = lockedDiscount;
                member.discountRate = lockedDiscount;
                member.points = lockedPoints;
                member.balance = lockedBalance;
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
        StringBuilder errorMessage = new StringBuilder();

        validateMemberCode(errorMessage);
        validatePhone(errorMessage);
        validateName(errorMessage);
        validatePoints(errorMessage);
        validateDiscount(errorMessage);
        validateBalance(errorMessage);
        validateBirthday(errorMessage);

        if (errorMessage.isEmpty()) {
            errorLabel.setText("");
            return true;
        }

        errorLabel.setText(errorMessage.toString());
        return false;
    }

    private void validateMemberCode(StringBuilder errorMessage) {
        if (!autoCodeCheckBox.isSelected() && memberCodeField.getText().trim().isEmpty()) {
            appendValidationError(errorMessage, "member.validation.code_required");
        }
    }

    private void validatePhone(StringBuilder errorMessage) {
        String phone = phoneField.getText().trim();
        if (phone.isEmpty()) {
            appendValidationError(errorMessage, "member.validation.phone_required");
        } else if (!phone.matches("\\d{11}")) {
            appendValidationError(errorMessage, "member.validation.phone_invalid");
        } else if (member == null) {
            validateNewMemberPhone(errorMessage, phone);
        }
    }

    private void validateNewMemberPhone(StringBuilder errorMessage, String phone) {
        try {
            if (DAOFactory.getInstance().getMemberDAO().findByPhone(phone) != null) {
                appendValidationError(errorMessage, "member.validation.phone_exists");
            }
        } catch (SQLException e) {
            logger.error("检查会员手机号是否存在失败", e);
            appendValidationError(errorMessage, "error.load_data");
        }
    }

    private void validateName(StringBuilder errorMessage) {
        if (nameField.getText().trim().isEmpty()) {
            appendValidationError(errorMessage, "member.validation.name_required");
        }
    }

    private void validatePoints(StringBuilder errorMessage) {
        String pointsText = pointsField.getText().trim();
        if (pointsText.isEmpty()) {
            appendValidationError(errorMessage, "member.validation.points_required");
        } else {
            try {
                double points = Double.parseDouble(pointsText);
                if (points < 0) {
                    appendValidationError(errorMessage, "member.validation.points_negative");
                }
            } catch (IllegalArgumentException e) {
                appendValidationError(errorMessage, "member.validation.points_invalid");
            }
        }
    }

    private void validateDiscount(StringBuilder errorMessage) {
        try {
            double discount = Double.parseDouble(discountField.getText().trim());
            if (discount < 0 || discount > 10) {
                appendValidationError(errorMessage, "member.validation.discount_range");
            }
        } catch (IllegalArgumentException e) {
            appendValidationError(errorMessage, "member.validation.discount_invalid");
        }
    }

    private void validateBalance(StringBuilder errorMessage) {
        String balanceText = balanceField.getText().trim();
        if (balanceText.isEmpty()) {
            appendValidationError(errorMessage, "member.validation.balance_required");
        } else {
            try {
                double balance = Double.parseDouble(balanceText);
                if (balance < 0) {
                    appendValidationError(errorMessage, "member.validation.balance_negative");
                }
            } catch (IllegalArgumentException e) {
                appendValidationError(errorMessage, "member.validation.balance_invalid");
            }
        }
    }

    private void validateBirthday(StringBuilder errorMessage) {
        String birthday = birthdayField.getText().trim();
        if (!birthday.isEmpty() && !birthday.matches("\\d{2}-\\d{2}")) {
            appendValidationError(errorMessage, "member.validation.birthday_invalid");
        }
    }

    private void appendValidationError(StringBuilder errorMessage, String key) {
        errorMessage.append(i18n(key));
    }

    private String i18n(String key) {
        return com.cashier.i18n.I18nManager.getInstance().get(key) + "\n";
    }
}
