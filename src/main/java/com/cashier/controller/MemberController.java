package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.controller.base.BaseController;
import com.cashier.dao.DAOFactory;
import com.cashier.i18n.I18nManager;
import com.cashier.model.Member;
import com.cashier.model.PageResult;
import com.cashier.util.FXMLUtils;
import com.cashier.util.UIOptimizer;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.sql.SQLException;
import javafx.application.Platform;
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
    private static final int FIRST_PAGE = 1;
    private static final int DESKTOP_PAGE_SIZE = 500;

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
    private long totalMembers;

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
            new SimpleStringProperty(String.format(java.util.Locale.ROOT, "%.2f", cellData.getValue().balance)));
        discountColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(String.format(java.util.Locale.ROOT, "%.1f折", cellData.getValue().discount)));
        birthdayColumn.setCellValueFactory(new PropertyValueFactory<>("birthday"));
    }

    /**
     * 加载表格数据（后台线程执行 DB 查询，Platform.runLater 更新 UI）
     */
    @Override
    protected void loadTableData() {
        new Thread(() -> {
            try {
                PageResult<Member> memberData = DAOFactory.getInstance().getMemberDAO().findAll(FIRST_PAGE, DESKTOP_PAGE_SIZE);
                long total = memberData.getTotal();
                java.util.HashMap<String, Member> memberMap = new java.util.HashMap<>();
                for (Member m : memberData.getData()) {
                    memberMap.put(m.phone, m);
                }
                Platform.runLater(() -> {
                    members = memberMap;
                    totalMembers = total;
                    memberList = FXCollections.observableArrayList(members.values());
                    memberTable.setItems(memberList);
                    updateCountLabel();
                });
            } catch (SQLException e) {
                logger.error("加载会员数据失败", e);
                Platform.runLater(() -> {
                    showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
                    members = new java.util.HashMap<>();
                    totalMembers = 0;
                    memberList = FXCollections.observableArrayList(members.values());
                    memberTable.setItems(memberList);
                    updateCountLabel();
                });
            }
        }).start();
    }

    private void setLoadedMembers(java.util.Collection<Member> loadedMembers) {
        members = new java.util.HashMap<>();
        for (Member member : loadedMembers) {
            members.put(member.phone, member);
        }
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
            loadTableData();
            updateCountLabel();
            return;
        }

        // 搜索放后台（全表 LIKE 查询），回 FX 线程再填表（TD-006）
        UIOptimizer.runInBackground(
            () -> DAOFactory.getInstance().getMemberDAO().search(searchText, FIRST_PAGE, DESKTOP_PAGE_SIZE),
            memberData -> {
                totalMembers = memberData.getTotal();
                setLoadedMembers(memberData.getData());
                memberList.setAll(members.values());
                updateCountLabel();
            },
            e -> {
                logger.error("搜索会员失败", e);
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED)
                    + ": " + e.getMessage());
            });
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
            com.cashier.util.ThemeUtils.applyCurrentTheme(scene, getClass());

            dialogStage.setScene(scene);
            controller.setDialogStage(dialogStage);
            controller.setMember(item);

            dialogStage.showAndWait();

            if (controller.isOkClicked()) {
                Member updatedMember = controller.getMember();
                try {
                    if (item == null) {
                        DAOFactory.getInstance().getMemberDAO().insert(updatedMember);
                    } else {
                        DAOFactory.getInstance().getMemberDAO().update(updatedMember);
                    }
                    loadTableData();
                    updateStatus(item == null ? "会员添加成功: " + updatedMember.name : "会员更新成功: " + updatedMember.name);
                    return true;
                } catch (SQLException e) {
                    logger.error(item == null ? "添加会员失败" : "更新会员失败", e);
                    showError(item == null
                            ? i18n.get("runtime.member_add_failed", e.getMessage())
                            : i18n.get("runtime.member_update_failed", e.getMessage()));
                }
            }
        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
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
        } else {
            showWarning(i18n.get(I18nKeys.Runtime.SELECT_MEMBER));
        }
    }

    /**
     * 处理删除会员 - 支持批量删除
     */
    @FXML
    public void handleDeleteMember() {
        ObservableList<Member> selected = getSelectedItems(memberTable);
        if (selected.isEmpty()) {
            showWarning(i18n.get(I18nKeys.Runtime.SELECT_MEMBER));
            return;
        }

        if (selected.size() == 1) {
            // 单个删除
            Member member = selected.get(0);
            if (confirmDeleteWithName(member.name)) {
                try {
                    DAOFactory.getInstance().getMemberDAO().delete(member.id);
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
                        DAOFactory.getInstance().getMemberDAO().delete(member.id);
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
                com.cashier.util.ThemeUtils.applyCurrentTheme(scene, getClass());

                dialogStage.setScene(scene);
                controller.setDialogStage(dialogStage);
                controller.setMember(selected);
                // 传入实际操作员用户名，用于审计记录
                com.cashier.model.User currentUser = com.cashier.CashierSystemFXApplication.getInstance().getCurrentUser();
                if (currentUser != null) {
                    controller.setOperatorName(currentUser.username);
                }

                dialogStage.showAndWait();

                if (controller.isOkClicked()) {
                    loadTableData();
                    updateStatus(i18n.get("member.recharge.success", selected.name, String.valueOf(controller.getRechargeAmount())));
                }

            } catch (IOException e) {
                showError(i18n.get("member.recharge.load_error") + ": " + e.getMessage());
            }
        } else {
            showWarning(i18n.get(I18nKeys.Runtime.SELECT_MEMBER));
        }
    }

    /**
     * 处理清除搜索
     */
    @FXML
    public void handleClearSearch() {
        searchField.clear();
        loadTableData();
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
        countLabel.setText(i18n.get("member.count", memberList.size() + "/" + totalMembers));
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
