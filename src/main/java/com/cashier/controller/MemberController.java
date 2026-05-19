package com.cashier.controller;

import com.cashier.controller.base.BaseController;
import com.cashier.dao.MemberDAO;
import com.cashier.i18n.I18nManager;
import com.cashier.model.Member;
import com.cashier.util.DialogBuilder;
import com.cashier.util.FXMLUtils;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.sql.SQLException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Map;

/**
 * 会员管理控制器
 * 处理会员的增删改查和充值
 */
public class MemberController extends BaseController<Member> {
    private static final Logger logger = LoggerFactoryUtil.getLogger(MemberController.class);

    @FXML
    private TableView<Member> memberTable;

    @FXML
    private TableColumn<Member, String> phoneColumn;

    @FXML
    private TableColumn<Member, String> nameColumn;

    @FXML
    private TableColumn<Member, String> levelColumn;

    @FXML
    private TableColumn<Member, String> pointsColumn;

    @FXML
    private TableColumn<Member, String> balanceColumn;

    @FXML
    private TableColumn<Member, String> discountColumn;

    @FXML
    private TableColumn<Member, String> birthdayColumn;

    @FXML
    private TextField searchField;

    @FXML
    private Label countLabel;

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button rechargeButton;

    private ObservableList<Member> memberList;
    private Map<String, Member> members;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 设置表格列
        setupTableColumns();

        // 加载会员数据
        loadTableData();

        // 设置表格选择模式
        memberTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // 添加表格选择监听（使用BaseController方法）
        setupTableSelectionListener(memberTable, member -> updateButtonState(memberTable, editButton, deleteButton, rechargeButton));

        // 设置表格双击编辑监听（使用BaseController方法）
        setupTableDoubleClickListener(memberTable);

        // 启用 UI 性能优化（固定行高启用更好的虚拟流）
        memberTable.setFixedCellSize(40.0);
        memberTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    /**
     * 设置表格列
     */
    private void setupTableColumns() {
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        levelColumn.setCellValueFactory(new PropertyValueFactory<>("level"));
        pointsColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.valueOf(cellData.getValue().getPoints().intValue())));
        balanceColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format("%.2f", cellData.getValue().balance)));
        discountColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(String.format("%.1f折", cellData.getValue().discount)));
        birthdayColumn.setCellValueFactory(new PropertyValueFactory<>("birthday"));
    }

    /**
     * 加载表格数据（实现BaseController抽象方法）
     */
    @Override
    protected void loadTableData() {
        try {
            var memberData = MemberDAO.findAll();
            members = new java.util.HashMap<>();
            for (Member member : memberData) {
                members.put(member.phone, member);
            }
        } catch (SQLException e) {
            logger.error("加载会员数据失败", e);
            showError("加载会员数据失败: " + e.getMessage());
            members = new java.util.HashMap<>();
        }
        memberList = FXCollections.observableArrayList(members.values());
        memberTable.setItems(memberList);
        updateCountLabel();
    }

    /**
     * 处理添加会员（实现BaseController抽象方法）
     */
    @Override
    protected void handleAdd() {
        handleAddMember();
    }

    /**
     * 处理编辑会员（实现BaseController抽象方法）
     */
    @Override
    protected void handleEdit() {
        handleEditMember();
    }

    /**
     * 处理删除会员（实现BaseController抽象方法）
     */
    @Override
    protected void handleDelete() {
        handleDeleteMember();
    }

    /**
     * 处理搜索（实现BaseController抽象方法）
     */
    @Override
    public void handleSearch() {
        String searchText = searchField.getText().trim().toLowerCase();
        if (searchText.isEmpty()) {
            memberList.setAll(members.values());
        } else {
            memberList.setAll(members.values().stream()
                .filter(m -> m.phone.contains(searchText) || m.name.toLowerCase().contains(searchText))
                .toList());
        }
        updateCountLabel();
    }

    /**
     * 显示编辑对话框（实现BaseController抽象方法）
     */
    @Override
    protected boolean showEditDialog(Member item) {
        try {
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/MemberEditView.fxml");
            VBox root = loader.load();

            MemberEditController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(item == null ? i18n.get("member.add.title") : i18n.get("member.edit.title"));
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(memberTable.getScene().getWindow());
            dialogStage.setResizable(false);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());

            dialogStage.setScene(scene);
            controller.setDialogStage(dialogStage);
            controller.setMember(item);

            dialogStage.showAndWait();

            if (controller.isOkClicked()) {
                Member updatedMember = controller.getMember();
                try {
                    if (item == null) {
                        MemberDAO.insert(updatedMember);
                    } else {
                        MemberDAO.update(updatedMember);
                    }
                    loadTableData();
                    updateStatus(item == null ? "会员添加成功: " + updatedMember.name : "会员更新成功: " + updatedMember.name);
                    return true;
                } catch (SQLException e) {
                    logger.error(item == null ? "添加会员失败" : "更新会员失败", e);
                    showError(item == null ? "添加会员失败: " + e.getMessage() : "更新会员失败: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            showError("加载对话框失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 处理添加会员
     */
    @FXML
    public void handleAddMember() {
        showEditDialog(null);
    }

    /**
     * 处理编辑会员
     */
    @FXML
    public void handleEditMember() {
        Member selected = getSelectedItem(memberTable);
        if (selected != null) {
            showEditDialog(selected);
        }
    }

    /**
     * 处理删除会员 - 支持批量删除
     */
    @FXML
    public void handleDeleteMember() {
        ObservableList<Member> selected = getSelectedItems(memberTable);
        if (selected.isEmpty()) {
            return;
        }

        if (selected.size() == 1) {
            // 单个删除
            Member member = selected.get(0);
            if (confirmDeleteWithName(member.name)) {
                try {
                    MemberDAO.delete(member.id);
                    loadTableData();
                    showSuccess(i18n.get("member.delete.success", member.name));
                } catch (SQLException e) {
                    logger.error("删除会员失败", e);
                    showError(i18n.get("member.delete.error") + ": " + e.getMessage());
                }
            }
        } else {
            // 批量删除
            if (confirm(i18n.get("member.delete.batch_confirm", String.valueOf(selected.size())),
                      i18n.get("member.delete.batch_detail", String.valueOf(selected.size())))) {
                try {
                    int successCount = 0;
                    for (Member member : selected) {
                        MemberDAO.delete(member.id);
                        successCount++;
                    }
                    loadTableData();
                    showSuccess(i18n.get("member.delete.batch_success", String.valueOf(successCount)));
                } catch (SQLException e) {
                    logger.error("批量删除会员失败", e);
                    showError(i18n.get("member.delete.batch_error") + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * 处理充值
     */
    @FXML
    public void handleRecharge() {
        Member selected = getSelectedItem(memberTable);
        if (selected != null) {
            try {
                FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/RechargeView.fxml");
                VBox root = loader.load();

                RechargeController controller = loader.getController();

                Stage dialogStage = new Stage();
                dialogStage.setTitle(i18n.get("member.recharge.title"));
                dialogStage.initModality(Modality.WINDOW_MODAL);
                dialogStage.initOwner(memberTable.getScene().getWindow());
                dialogStage.setResizable(false);

                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());

                dialogStage.setScene(scene);
                controller.setDialogStage(dialogStage);
                controller.setMember(selected);

                dialogStage.showAndWait();

                if (controller.isOkClicked()) {
                    loadTableData();
                    updateStatus(i18n.get("member.recharge.success", selected.name, String.valueOf(controller.getRechargeAmount())));
                }

            } catch (IOException e) {
                showError(i18n.get("member.recharge.load_error") + ": " + e.getMessage());
            }
        }
    }

    /**
     * 处理清除搜索
     */
    @FXML
    public void handleClearSearch() {
        searchField.clear();
        memberList.setAll(members.values());
        updateCountLabel();
    }

    /**
     * 刷新会员列表
     */
    public void refreshMembers() {
        loadTableData();
    }

    /**
     * 更新会员数量标签
     */
    private void updateCountLabel() {
        countLabel.setText(i18n.get("member.count", String.valueOf(memberList.size())));
    }

    /**
     * 更新按钮状态（扩展BaseController方法以支持充值按钮）
     */
    private void updateButtonState(TableView<Member> table, Button editButton, Button deleteButton, Button rechargeButton) {
        boolean hasSelection = getSelectedItem(table) != null;
        setButtonEnabled(editButton, hasSelection);
        setButtonEnabled(deleteButton, hasSelection);
        setButtonEnabled(rechargeButton, hasSelection);
    }

    /**
     * 更新状态
     */
    private void updateStatus(String status) {
        StatusBarManager.updateStatus(status);
    }
}
