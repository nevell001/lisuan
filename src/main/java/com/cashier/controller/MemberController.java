package com.cashier.controller;

import com.cashier.dao.MemberDAO;
import com.cashier.model.Member;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class MemberController {
    private static final Logger logger = LoggerFactory.getLogger(MemberController.class);

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
        loadMembers();

        // 设置表格选择模式
        memberTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // 添加表格选择监听
        memberTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> updateButtonStates()
        );
    }

    /**
     * 设置表格列
     */
    private void setupTableColumns() {
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        levelColumn.setCellValueFactory(new PropertyValueFactory<>("level"));
        pointsColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.valueOf((int)cellData.getValue().points)));
        balanceColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%.2f", cellData.getValue().balance)));
        discountColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(String.format("%.1f折", cellData.getValue().discount)));        birthdayColumn.setCellValueFactory(new PropertyValueFactory<>("birthday"));
    }

    /**
     * 加载会员数据
     */
    private void loadMembers() {
        try {
            var memberData = MemberDAO.findAll();
            members = new java.util.HashMap<>();
            for (Member member : memberData) {
                members.put(member.phone, member);
            }
        } catch (SQLException e) {
            System.err.println("加载会员数据失败: " + e.getMessage());
            logger.error("加载会员数据失败", e);
            showError("加载会员数据失败: " + e.getMessage());
            members = new java.util.HashMap<>();
        }
        memberList = FXCollections.observableArrayList(members.values());
        memberTable.setItems(memberList);
        updateCountLabel();
    }

    /**
     * 更新会员数量标签
     */
    private void updateCountLabel() {
        countLabel.setText("会员数量: " + memberList.size());
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        boolean hasSelection = !memberTable.getSelectionModel().getSelectedItems().isEmpty();
        editButton.setDisable(!hasSelection);
        deleteButton.setDisable(!hasSelection);
        rechargeButton.setDisable(!hasSelection);
    }

    /**
     * 处理添加会员
     */
    @FXML
    private void handleAddMember() {
        try {
            // 加载会员编辑对话框
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/MemberEditView.fxml"));
            VBox root = loader.load();

            // 获取控制器
            MemberEditController controller = loader.getController();

            // 创建对话框
            Stage dialogStage = new Stage();
            dialogStage.setTitle("添加会员");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(memberTable.getScene().getWindow());
            dialogStage.setResizable(false);

            // 设置场景
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());

            dialogStage.setScene(scene);

            // 设置控制器引用
            controller.setDialogStage(dialogStage);
            controller.setMember(null);

            // 显示对话框并等待响应
            dialogStage.showAndWait();

            // 如果用户点击了保存
            if (controller.isOkClicked()) {
                Member newMember = controller.getMember();
                try {
                    MemberDAO.insert(newMember);
                    loadMembers();
                    updateStatus("会员添加成功: " + newMember.name);
                } catch (SQLException e) {
                    System.err.println("添加会员失败: " + e.getMessage());
                    logger.error("添加会员失败", e);
                    showError("添加会员失败: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            showError("加载添加会员对话框失败: " + e.getMessage());
        }
    }

    /**
     * 处理编辑会员
     */
    @FXML
    private void handleEditMember() {
        Member selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                // 加载会员编辑对话框
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getResource("/com/cashier/view/MemberEditView.fxml"));
                VBox root = loader.load();

                // 获取控制器
                MemberEditController controller = loader.getController();

                // 创建对话框
                Stage dialogStage = new Stage();
                dialogStage.setTitle("编辑会员");
                dialogStage.initModality(Modality.WINDOW_MODAL);
                dialogStage.initOwner(memberTable.getScene().getWindow());
                dialogStage.setResizable(false);

                // 设置场景
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());

                dialogStage.setScene(scene);

                // 设置控制器引用
                controller.setDialogStage(dialogStage);
                controller.setMember(selected);

                // 显示对话框并等待响应
                dialogStage.showAndWait();

                // 如果用户点击了保存
                if (controller.isOkClicked()) {
                    Member updatedMember = controller.getMember();
                    try {
                        MemberDAO.update(updatedMember);
                        loadMembers();
                        updateStatus("会员更新成功: " + updatedMember.name);
                    } catch (SQLException e) {
                    System.err.println("更新会员失败: " + e.getMessage());
                    logger.error("更新会员失败", e);
                    showError("更新会员失败: " + e.getMessage());
                }
                }

            } catch (IOException e) {
                showError("加载编辑会员对话框失败: " + e.getMessage());
            }
        }
    }

    /**
     * 处理删除会员
     */
    @FXML
    private void handleDeleteMember() {
        Member selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认删除");
            alert.setHeaderText(null);
            alert.setContentText("确定要删除会员 \"" + selected.name + "\" 吗？");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    MemberDAO.delete(selected.id);
                    loadMembers();
                    updateStatus("会员删除成功: " + selected.name);
                } catch (SQLException e) {
                    System.err.println("删除会员失败: " + e.getMessage());
                    logger.error("删除会员失败", e);
                    showError("删除会员失败: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 处理充值
     */
    @FXML
    private void handleRecharge() {
        Member selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                // 加载充值对话框
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getResource("/com/cashier/view/RechargeView.fxml"));
                VBox root = loader.load();

                // 获取控制器
                RechargeController controller = loader.getController();

                // 创建对话框
                Stage dialogStage = new Stage();
                dialogStage.setTitle("会员充值");
                dialogStage.initModality(Modality.WINDOW_MODAL);
                dialogStage.initOwner(memberTable.getScene().getWindow());
                dialogStage.setResizable(false);

                // 设置场景
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());

                dialogStage.setScene(scene);

                // 设置控制器引用
                controller.setDialogStage(dialogStage);
                controller.setMember(selected);

                // 显示对话框并等待响应
                dialogStage.showAndWait();

                // 如果用户点击了确认
                if (controller.isOkClicked()) {
                    loadMembers();
                    updateStatus("充值成功: " + selected.name + " (+" + controller.getRechargeAmount() + "元)");
                }

            } catch (IOException e) {
                showError("加载充值对话框失败: " + e.getMessage());
            }
        }
    }

    /**
     * 处理搜索
     */
    @FXML
    private void handleSearch() {
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
     * 处理清除搜索
     */
    @FXML
    private void handleClearSearch() {
        searchField.clear();
        memberList.setAll(members.values());
        updateCountLabel();
    }

    /**
     * 刷新会员列表
     */
    public void refreshMembers() {
        loadMembers();
    }

    /**
     * 更新状态
     * @param status 状态文本
     */
    private void updateStatus(String status) {
        StatusBarManager.updateStatus(status);
    }

    /**
     * 显示错误信息
     * @param message 错误消息
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
