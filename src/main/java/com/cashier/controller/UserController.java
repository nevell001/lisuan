package com.cashier.controller;

import com.cashier.dao.UserDAO;
import com.cashier.i18n.I18nManager;
import com.cashier.model.User;
import com.cashier.util.PasswordUtil;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * 用户管理控制器
 * 处理用户的增删改查
 */
public class UserController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(UserController.class);

    @FXML
    private TableView<User> userTable;

    @FXML
    private TableColumn<User, String> usernameColumn;

    @FXML
    private TableColumn<User, String> nameColumn;

    @FXML
    private TableColumn<User, String> roleColumn;

    @FXML
    private TableColumn<User, String> createTimeColumn;

    @FXML
    private TableColumn<User, String> lastLoginColumn;

    @FXML
    private TableColumn<User, String> statusColumn;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> roleFilterComboBox;

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
    private Button resetPasswordButton;

    @FXML
    private Button activateButton;

    @FXML
    private Button deactivateButton;

    private ObservableList<User> userList;
    private Map<String, User> users;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 初始化角色筛选下拉框
        roleFilterComboBox.setItems(FXCollections.observableArrayList(
            "全部",
            "管理员",
            "收银员",
            "财务"
        ));
        roleFilterComboBox.getSelectionModel().select(0);

        // 初始化状态筛选下拉框
        statusFilterComboBox.setItems(FXCollections.observableArrayList(
            "全部",
            "激活",
            "禁用"
        ));
        statusFilterComboBox.getSelectionModel().select(0);

        // 设置表格列
        setupTableColumns();

        // 加载用户数据
        loadUsers();

        // 设置表格选择模式
        userTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // 添加表格选择监听
        userTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> updateButtonStates()
        );
    }

    /**
     * 设置表格列
     */
    private void setupTableColumns() {
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        roleColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getRoleDisplayName()));
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        createTimeColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(sdf.format(cellData.getValue().createTime)));
        lastLoginColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(sdf.format(cellData.getValue().lastLoginTime)));
        statusColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().active ? "激活" : "禁用"));
    }

    /**
     * 加载用户数据
     */
    private void loadUsers() {
        logger.info("UserController: 开始加载用户数据...");
        try {
            List<User> userListData = UserDAO.findAll();
            users = new java.util.HashMap<>();
            for (User user : userListData) {
                users.put(user.username, user);
            }
        } catch (SQLException e) {
            logger.error("加载用户数据失败", e);
            showError("加载用户数据失败: " + e.getMessage());
            users = new java.util.HashMap<>();
        }
        userList = FXCollections.observableArrayList(users.values());
        userTable.setItems(userList);
        updateCountLabel();
        logger.info("UserController: 加载了 {} 个用户", users.size());
    }

    /**
     * 更新用户数量标签
     */
    private void updateCountLabel() {
        countLabel.setText("用户数量: " + userList.size());
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        boolean hasSelection = !userTable.getSelectionModel().getSelectedItems().isEmpty();
        editButton.setDisable(!hasSelection);
        deleteButton.setDisable(!hasSelection);
        resetPasswordButton.setDisable(!hasSelection);
        activateButton.setDisable(!hasSelection);
        deactivateButton.setDisable(!hasSelection);
    }

    /**
     * 处理添加用户
     */
    @FXML
    private void handleAddUser() {
        showUserDialog(null);
    }

    /**
     * 处理编辑用户
     */
    @FXML
    private void handleEditUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showUserDialog(selected);
        }
    }

    /**
     * 显示用户对话框
     * @param user 要编辑的用户，null表示添加新用户
     */
    private void showUserDialog(User user) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(user == null ? "添加用户" : "编辑用户");
        dialog.setHeaderText(null);

        // 创建对话框内容
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField idField = new TextField();
        CheckBox autoIdCheckBox = new CheckBox("自动生成");
        TextField usernameField = new TextField();
        TextField passwordField = new TextField();
        TextField nameField = new TextField();
        ComboBox<String> roleComboBox = new ComboBox<>();

        roleComboBox.setItems(FXCollections.observableArrayList("admin", "cashier", "finance"));
        roleComboBox.getItems().setAll("管理员", "收银员", "财务");

        // 设置ID输入框和复选框的默认状态
        if (user == null) {
            autoIdCheckBox.setSelected(true);
            idField.setDisable(true);
        } else {
            idField.setText(String.valueOf(user.id));
            idField.setDisable(true);
            autoIdCheckBox.setDisable(true);
        }

        // 添加复选框监听器
        autoIdCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            idField.setDisable(newVal);
        });

        if (user != null) {
            usernameField.setText(user.username);
            usernameField.setDisable(true); // 用户名不可修改
            passwordField.setPromptText("留空则不修改密码");
            nameField.setText(user.name);
            roleComboBox.getSelectionModel().select(user.getRoleDisplayName());
        }

        // 创建ID的HBox
        HBox idBox = new HBox(10);
        idBox.getChildren().addAll(idField, autoIdCheckBox);
        idField.setPrefWidth(150);

        grid.add(new Label("ID:"), 0, 0);
        grid.add(idBox, 1, 0);
        grid.add(new Label("用户名:"), 0, 1);
        grid.add(usernameField, 1, 1);
        grid.add(new Label("密码:"), 0, 2);
        grid.add(passwordField, 1, 2);
        grid.add(new Label("姓名:"), 0, 3);
        grid.add(nameField, 1, 3);
        grid.add(new Label("角色:"), 0, 4);
        grid.add(roleComboBox, 1, 4);

        dialog.getDialogPane().setContent(grid);

        ButtonType okButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                User newUser = user != null ? user : new User();

                // 验证ID（仅当手动输入时）
                if (user == null && !autoIdCheckBox.isSelected() && !idField.getText().trim().isEmpty()) {
                    try {
                        int id = Integer.parseInt(idField.getText().trim());
                        if (id <= 0) {
                            showError("ID必须是大于0的数字！");
                            return null;
                        }
                        newUser.id = id;
                    } catch (NumberFormatException e) {
                        showError("ID格式不正确！");
                        return null;
                    }
                }

                if (user == null) {
                    newUser.username = usernameField.getText().trim();
                }

                // 只有在新用户或输入了新密码时才哈希密码
                String passwordInput = passwordField.getText().trim();
                if (user == null || !passwordInput.isEmpty()) {
                    newUser.password = PasswordUtil.hashPassword(passwordInput);
                }

                newUser.name = nameField.getText().trim();

                String roleDisplayName = roleComboBox.getSelectionModel().getSelectedItem();
                switch (roleDisplayName) {
                    case "管理员":
                        newUser.role = "admin";
                        break;
                    case "收银员":
                        newUser.role = "cashier";
                        break;
                    case "财务":
                        newUser.role = "finance";
                        break;
                }

                return newUser;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                if (user == null) {
                    // 添加新用户
                    if (UserDAO.insert(result)) {
                        users.put(result.username, result);
                        loadUsers();
                        updateStatus("用户添加成功");
                    } else {
                        showError("用户添加失败");
                    }
                } else {
                    // 更新现有用户
                    if (UserDAO.update(result)) {
                        users.put(result.username, result);
                        loadUsers();
                        updateStatus("用户更新成功");
                    } else {
                        showError("用户更新失败");
                    }
                }
            } catch (SQLException e) {
                logger.error("保存用户失败", e);
                showError("保存用户失败: " + e.getMessage());
            }
        });
    }

    /**
     * 处理删除用户
     */
    @FXML
    private void handleDeleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // 不允许删除admin用户
            if ("admin".equals(selected.username)) {
                showError("不能删除管理员账户！");
                return;
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18nManager.getInstance().get("common.confirm"));
            alert.setHeaderText(null);
            alert.setContentText("确定要删除用户 \"" + selected.name + "\" 吗？");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    if (UserDAO.deleteByUsername(selected.username)) {
                        users.remove(selected.username);
                        loadUsers();
                        updateStatus("用户删除成功");
                    } else {
                        showError("用户删除失败");
                    }
                } catch (SQLException e) {
                    logger.error("删除用户失败", e);
                    showError("删除用户失败: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 处理重置密码
     */
    @FXML
    private void handleResetPassword() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("重置密码");
            dialog.setHeaderText(null);
            dialog.setContentText("请输入新密码:");

            dialog.showAndWait().ifPresent(newPassword -> {
                if (newPassword.trim().isEmpty()) {
                    showError("密码不能为空！");
                    return;
                }

                selected.password = PasswordUtil.hashPassword(newPassword.trim());
                try {
                    if (UserDAO.update(selected)) {
                        loadUsers();
                        updateStatus("密码重置成功");
                    } else {
                        showError("密码重置失败");
                    }
                } catch (SQLException e) {
                    logger.error("重置密码失败", e);
                    showError("重置密码失败: " + e.getMessage());
                }
            });
        }
    }

    /**
     * 处理激活用户
     */
    @FXML
    private void handleActivateUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.active = true;
            try {
                if (UserDAO.update(selected)) {
                    loadUsers();
                    updateStatus("用户已激活");
                } else {
                    showError("激活用户失败");
                }
            } catch (SQLException e) {
                logger.error("激活用户失败", e);
                showError("激活用户失败: " + e.getMessage());
            }
        }
    }

    /**
     * 处理禁用用户
     */
    @FXML
    private void handleDeactivateUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // 不允许禁用admin用户
            if ("admin".equals(selected.username)) {
                showError("不能禁用管理员账户！");
                return;
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18nManager.getInstance().get("common.confirm"));
            alert.setHeaderText(null);
            alert.setContentText("确定要禁用用户 \"" + selected.name + "\" 吗？");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                selected.active = false;
                try {
                    if (UserDAO.update(selected)) {
                        loadUsers();
                        updateStatus("用户已禁用");
                    } else {
                        showError("禁用用户失败");
                    }
                } catch (SQLException e) {
                    logger.error("禁用用户失败", e);
                    showError("禁用用户失败: " + e.getMessage());
                }
            }
        }
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
        roleFilterComboBox.getSelectionModel().select(0);
        statusFilterComboBox.getSelectionModel().select(0);
        applyFilters();
    }

    /**
     * 应用筛选条件
     */
    private void applyFilters() {
        String searchText = searchField.getText().trim().toLowerCase();
        String roleFilter = roleFilterComboBox.getSelectionModel().getSelectedItem();
        String statusFilter = statusFilterComboBox.getSelectionModel().getSelectedItem();

        userList.setAll(users.values().stream()
            .filter(u -> {
                // 角色筛选
                if (!"全部".equals(roleFilter)) {
                    String roleDisplayName = u.getRoleDisplayName();
                    if (!roleFilter.equals(roleDisplayName)) {
                        return false;
                    }
                }

                // 状态筛选
                if (!"全部".equals(statusFilter)) {
                    boolean active = "激活".equals(statusFilter);
                    if (u.active != active) {
                        return false;
                    }
                }

                // 搜索文本筛选
                if (!searchText.isEmpty()) {
                    return u.username.toLowerCase().contains(searchText) ||
                           u.name.toLowerCase().contains(searchText);
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
     * 显示错误信息
     * @param message 错误消息
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(I18nManager.getInstance().get("label.error"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 刷新用户列表
     */
    public void refreshUsers() {
        loadUsers();
    }
}