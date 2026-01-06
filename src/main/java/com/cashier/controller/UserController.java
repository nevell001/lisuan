package com.cashier.controller;

import com.cashier.model.DataManager;
import com.cashier.model.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * 用户管理控制器
 * 处理用户的增删改查
 */
public class UserController {

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
        System.out.println("UserController: 开始加载用户数据...");
        users = DataManager.loadUsers();
        userList = FXCollections.observableArrayList(users.values());
        userTable.setItems(userList);
        updateCountLabel();
        System.out.println("UserController: 加载了 " + users.size() + " 个用户");
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

        TextField usernameField = new TextField();
        TextField passwordField = new TextField();
        TextField nameField = new TextField();
        ComboBox<String> roleComboBox = new ComboBox<>();

        roleComboBox.setItems(FXCollections.observableArrayList("admin", "cashier", "finance"));
        roleComboBox.getItems().setAll("管理员", "收银员", "财务");

        if (user != null) {
            usernameField.setText(user.username);
            usernameField.setDisable(true); // 用户名不可修改
            passwordField.setText(user.password);
            nameField.setText(user.name);
            roleComboBox.getSelectionModel().select(user.getRoleDisplayName());
        }

        grid.add(new Label("用户名:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("密码:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("姓名:"), 0, 2);
        grid.add(nameField, 1, 2);
        grid.add(new Label("角色:"), 0, 3);
        grid.add(roleComboBox, 1, 3);

        dialog.getDialogPane().setContent(grid);

        ButtonType okButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                User newUser = user != null ? user : new User();
                
                if (user == null) {
                    newUser.username = usernameField.getText().trim();
                }
                newUser.password = passwordField.getText().trim();
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
            if (user == null) {
                users.put(result.username, result);
            }
            DataManager.saveUsers(users);
            loadUsers();
            updateStatus(user == null ? "用户添加成功" : "用户更新成功");
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
            alert.setTitle("确认删除");
            alert.setHeaderText(null);
            alert.setContentText("确定要删除用户 \"" + selected.name + "\" 吗？");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                users.remove(selected.username);
                DataManager.saveUsers(users);
                loadUsers();
                updateStatus("用户删除成功");
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

                selected.password = newPassword.trim();
                DataManager.saveUsers(users);
                loadUsers();
                updateStatus("密码重置成功");
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
            DataManager.saveUsers(users);
            loadUsers();
            updateStatus("用户已激活");
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
            alert.setTitle("确认禁用");
            alert.setHeaderText(null);
            alert.setContentText("确定要禁用用户 \"" + selected.name + "\" 吗？");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                selected.active = false;
                DataManager.saveUsers(users);
                loadUsers();
                updateStatus("用户已禁用");
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
        // TODO: 更新主界面的状态栏
        System.out.println("状态: " + status);
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

    /**
     * 刷新用户列表
     */
    public void refreshUsers() {
        loadUsers();
    }
}