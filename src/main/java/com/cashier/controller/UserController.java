package com.cashier.controller;

import com.cashier.api.ApiServer;
import com.cashier.i18n.I18nKeys;

import com.cashier.dao.DAOFactory;
import com.cashier.i18n.I18nManager;
import com.cashier.model.User;
import com.cashier.util.PasswordUtil;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.FormValidator;

import java.sql.SQLException;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * 用户管理控制器
 * 处理用户的增删改查
 */
public class UserController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(UserController.class);
    private static final int FIRST_PAGE = 1;
    private static final int USER_LIST_PAGE_SIZE = 500;

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
    private User currentUser;

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 初始化角色筛选下拉框
        roleFilterComboBox.setItems(FXCollections.observableArrayList(
            "all",
            "admin",
            "cashier",
            "finance"
        ));
        roleFilterComboBox.setButtonCell(createUserFilterCell(true));
        roleFilterComboBox.setCellFactory(listView -> createUserFilterCell(true));
        roleFilterComboBox.getSelectionModel().select(0);

        // 初始化状态筛选下拉框
        statusFilterComboBox.setItems(FXCollections.observableArrayList(
            "all",
            "active",
            "inactive"
        ));
        statusFilterComboBox.setButtonCell(createUserFilterCell(false));
        statusFilterComboBox.setCellFactory(listView -> createUserFilterCell(false));
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
            new SimpleStringProperty(localizeRole(cellData.getValue().role)));
        
        java.time.format.DateTimeFormatter sdf = com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME_MINUTE;
        java.util.function.Function<java.util.Date, String> formatDate = date ->
            date == null ? "-" : java.time.LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).format(sdf);
        createTimeColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(formatDate.apply(cellData.getValue().createTime)));
        lastLoginColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(formatDate.apply(cellData.getValue().lastLoginTime)));
        statusColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(I18nManager.getInstance().get(
                cellData.getValue().active ? "user.enabled" : "user.disabled")));
    }

    /**
     * 加载用户数据（后台线程执行 DB 查询，Platform.runLater 更新 UI）
     */
    private void loadUsers() {
        logger.info("UserController: 开始加载用户数据...");
        new Thread(() -> {
            try {
                List<User> userListData = DAOFactory.getInstance().getUserDAO().findAll(FIRST_PAGE, USER_LIST_PAGE_SIZE).getData();
                java.util.HashMap<String, User> userMap = new java.util.HashMap<>();
                for (User user : userListData) {
                    userMap.put(user.username, user);
                }
                Platform.runLater(() -> {
                    users = userMap;
                    userList = FXCollections.observableArrayList(users.values());
                    userTable.setItems(userList);
                    updateCountLabel();
                    logger.info("UserController: 加载了 {} 个用户", users.size());
                });
            } catch (SQLException e) {
                logger.error("加载用户数据失败", e);
                Platform.runLater(() -> {
                    showError(I18nManager.getInstance().get("runtime.user_load_failed", e.getMessage()));
                    users = new java.util.HashMap<>();
                    userList = FXCollections.observableArrayList(users.values());
                    userTable.setItems(userList);
                    updateCountLabel();
                });
            }
        }).start();
    }

    /**
     * 更新用户数量标签
     */
    private void updateCountLabel() {
        countLabel.setText(I18nManager.getInstance().get("runtime.user_count", userList.size()));
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
    public void handleAddUser() {
        if (!requireAdmin()) {
            return;
        }
        showUserDialog(null);
    }

    /**
     * 处理编辑用户
     */
    @FXML
    public void handleEditUser() {
        if (!requireAdmin()) {
            return;
        }
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
        dialog.setTitle(user == null ? com.cashier.i18n.I18nManager.getInstance().get("user.add") : com.cashier.i18n.I18nManager.getInstance().get("runtime.user_edit"));
        dialog.setHeaderText(null);
        dialog.getDialogPane().setPrefWidth(620);
        dialog.getDialogPane().getStyleClass().add("user-dialog");
        if (userTable.getScene() != null) {
            dialog.initOwner(userTable.getScene().getWindow());
            dialog.getDialogPane().getStylesheets().addAll(userTable.getScene().getStylesheets());
        }

        VBox content = new VBox(14);
        content.getStyleClass().add("dialog-content");
        Label titleLabel = new Label(user == null
            ? I18nManager.getInstance().get("user.add")
            : I18nManager.getInstance().get("runtime.user_edit"));
        titleLabel.getStyleClass().add("view-title");

        // 创建对话框内容
        GridPane grid = new GridPane();
        grid.getStyleClass().addAll("form-grid", "dialog-form-grid");
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new javafx.geometry.Insets(4, 0, 0, 0));
        javafx.scene.layout.ColumnConstraints labelColumn = new javafx.scene.layout.ColumnConstraints();
        labelColumn.setMinWidth(120);
        labelColumn.setPrefWidth(135);
        javafx.scene.layout.ColumnConstraints fieldColumn = new javafx.scene.layout.ColumnConstraints();
        fieldColumn.setMinWidth(380);
        fieldColumn.setPrefWidth(420);
        fieldColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelColumn, fieldColumn);

        TextField idField = new TextField();
        CheckBox autoIdCheckBox = new CheckBox(I18nManager.getInstance().get(I18nKeys.ProductEdit.AUTO_GENERATE));
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        TextField nameField = new TextField();
        ComboBox<String> roleComboBox = new ComboBox<>();

        for (TextField field : new TextField[]{idField, usernameField, passwordField, nameField}) {
            field.getStyleClass().add("form-input");
            field.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(field, Priority.ALWAYS);
        }
        roleComboBox.getStyleClass().addAll("form-combo", "user-role-combo");
        roleComboBox.setMaxWidth(Double.MAX_VALUE);
        roleComboBox.setMinWidth(380);
        roleComboBox.setPrefWidth(420);
        GridPane.setHgrow(roleComboBox, Priority.ALWAYS);

        roleComboBox.setItems(FXCollections.observableArrayList("admin", "cashier", "finance"));
        roleComboBox.setButtonCell(createRoleCell());
        roleComboBox.setCellFactory(listView -> createRoleCell());

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
            passwordField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.password_unchanged_hint"));
            nameField.setText(user.name);
            roleComboBox.getSelectionModel().select(user.role);
        } else {
            roleComboBox.getSelectionModel().select("cashier");
        }

        // 创建ID的HBox
        HBox idBox = new HBox(10);
        idBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        idBox.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(idBox, Priority.ALWAYS);
        idBox.getChildren().addAll(idField, autoIdCheckBox);
        HBox.setHgrow(idField, Priority.ALWAYS);

        grid.add(createUserFormLabel("ID:"), 0, 0);
        grid.add(idBox, 1, 0);
        grid.add(createUserFormLabel(com.cashier.i18n.I18nManager.getInstance().get("runtime.username")), 0, 1);
        grid.add(usernameField, 1, 1);
        grid.add(createUserFormLabel(com.cashier.i18n.I18nManager.getInstance().get("runtime.password")), 0, 2);
        grid.add(passwordField, 1, 2);
        grid.add(createUserFormLabel(com.cashier.i18n.I18nManager.getInstance().get("runtime.name")), 0, 3);
        grid.add(nameField, 1, 3);
        grid.add(createUserFormLabel(com.cashier.i18n.I18nManager.getInstance().get("runtime.role")), 0, 4);
        grid.add(roleComboBox, 1, 4);

        content.getChildren().addAll(titleLabel, grid);
        dialog.getDialogPane().setContent(content);

        ButtonType okButtonType = new ButtonType(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Common.OK), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType(I18nManager.getInstance().get(I18nKeys.Common.CANCEL), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);
        dialog.getDialogPane().lookupButton(okButtonType).getStyleClass().addAll("primary-button", "button-normal");
        dialog.getDialogPane().lookupButton(cancelButtonType).getStyleClass().addAll("secondary-button", "button-normal");

        dialog.setResultConverter(dialogButton -> dialogButton == okButtonType
            ? buildUserFromDialog(user, autoIdCheckBox, idField, usernameField, passwordField, nameField, roleComboBox)
            : null);

        dialog.showAndWait().ifPresent(result -> saveUserDialogResult(result, user == null));
    }

    private User buildUserFromDialog(
            User existingUser,
            CheckBox autoIdCheckBox,
            TextField idField,
            TextField usernameField,
            PasswordField passwordField,
            TextField nameField,
            ComboBox<String> roleComboBox) {

        User newUser = existingUser != null ? existingUser : new User();
        if (!applyManualUserIdIfNeeded(newUser, existingUser, autoIdCheckBox, idField)) {
            return null;
        }

        if (existingUser == null) {
            newUser.username = usernameField.getText().trim();
        }

        String passwordInput = passwordField.getText().trim();
        if (existingUser == null || !passwordInput.isEmpty()) {
            newUser.password = PasswordUtil.hashPassword(passwordInput);
        }

        newUser.name = nameField.getText().trim();
        newUser.role = roleComboBox.getSelectionModel().getSelectedItem();
        return newUser;
    }

    private boolean applyManualUserIdIfNeeded(User newUser, User existingUser, CheckBox autoIdCheckBox, TextField idField) {
        if (existingUser != null || autoIdCheckBox.isSelected() || idField.getText().trim().isEmpty()) {
            return true;
        }

        try {
            int id = FormValidator.parseInt(idField.getText().trim());
            if (id <= 0) {
                showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.id_positive"));
                return false;
            }
            newUser.id = id;
            return true;
        } catch (NumberFormatException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.id_invalid"));
            return false;
        }
    }

    private void saveUserDialogResult(User result, boolean isNewUser) {
        try {
            if (isNewUser) {
                saveNewUser(result);
            } else {
                updateExistingUser(result);
            }
        } catch (SQLException e) {
            logger.error("保存用户失败", e);
            showError(I18nManager.getInstance().get("runtime.user_save_failed", e.getMessage()));
        }
    }

    private void saveNewUser(User result) throws SQLException {
        if (DAOFactory.getInstance().getUserDAO().insert(result)) {
            audit("USER_CREATED", result.username);
            users.put(result.username, result);
            loadUsers();
            updateStatus(I18nManager.getInstance().get("user.added"));
        } else {
            showError(I18nManager.getInstance().get("runtime.user_add_failed"));
        }
    }

    private void updateExistingUser(User result) throws SQLException {
        // 记录变更前的状态，用于判断是否需要作废该用户的 API token
        User before = DAOFactory.getInstance().getUserDAO().findById(result.id);
        if (DAOFactory.getInstance().getUserDAO().update(result)) {
            if (before != null
                && (result.active != before.active
                    || !java.util.Objects.equals(result.role, before.role)
                    || !java.util.Objects.equals(result.password, before.password))) {
                // 禁用/改密/角色变更后作废该用户已签发的全部 API token
                ApiServer.getInstance().invalidateUserTokens(result.id);
            }
            audit("USER_UPDATED", result.username);
            users.put(result.username, result);
            loadUsers();
            updateStatus(I18nManager.getInstance().get("user.updated"));
        } else {
            showError(I18nManager.getInstance().get("runtime.user_update_failed"));
        }
    }

    private Label createUserFormLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
    }

    private ListCell<String> createRoleCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : localizeRole(item));
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                setMinHeight(36);
                setPrefHeight(36);
                setMaxWidth(Double.MAX_VALUE);
            }
        };
    }

    private ListCell<String> createUserFilterCell(boolean roleFilter) {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else if ("all".equals(item)) {
                    setText(I18nManager.getInstance().get("user.filter.all"));
                } else if (roleFilter) {
                    setText(localizeRole(item));
                } else {
                    setText(I18nManager.getInstance().get(
                        "active".equals(item) ? "user.enabled" : "user.disabled"));
                }
            }
        };
    }

    private String localizeRole(String role) {
        return I18nManager.getInstance().get(switch (role) {
            case "admin" -> "user.role.admin";
            case "finance" -> "user.role.finance";
            default -> "user.role.cashier";
        });
    }

    /**
     * 处理删除用户
     */
    @FXML
    public void handleDeleteUser() {
        if (!requireAdmin()) {
            return;
        }
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // 不允许删除admin用户
            if ("admin".equals(selected.username)) {
                showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.admin_delete_forbidden"));
                return;
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18nManager.getInstance().get(I18nKeys.Common.CONFIRM));
            alert.setHeaderText(null);
            alert.setContentText(I18nManager.getInstance().get("runtime.user_delete_confirm", selected.name));

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    if (DAOFactory.getInstance().getUserDAO().deleteByUsername(selected.username)) {
                        audit("USER_DELETED", selected.username);
                        users.remove(selected.username);
                        loadUsers();
                        updateStatus(I18nManager.getInstance().get("user.deleted"));
                    } else {
                        showError(I18nManager.getInstance().get("runtime.user_delete_result_failed"));
                    }
                } catch (SQLException e) {
                    logger.error("删除用户失败", e);
                    showError(I18nManager.getInstance().get("runtime.user_delete_failed", e.getMessage()));
                }
            }
        }
    }

    /**
     * 处理重置密码
     * 使用 PasswordField 替代 TextInputDialog，防止密码明文显示
     */
    @FXML
    public void handleResetPassword() {
        if (!requireAdmin()) {
            return;
        }
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // 自定义密码重置对话框，使用 PasswordField 防止明文显示
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle(com.cashier.i18n.I18nManager.getInstance().get("password_reset.title"));
            dialog.setHeaderText(null);
            dialog.getDialogPane().setPrefWidth(400);

            if (userTable.getScene() != null) {
                dialog.initOwner(userTable.getScene().getWindow());
                dialog.getDialogPane().getStylesheets().addAll(userTable.getScene().getStylesheets());
            }

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20));

            PasswordField newPasswordField = new PasswordField();
            PasswordField confirmPasswordField = new PasswordField();
            Label hintLabel = new Label(I18nManager.getInstance().get("runtime.password_hint_min6"));
            hintLabel.getStyleClass().add("hint-label");

            grid.add(new Label(I18nManager.getInstance().get("runtime.enter_new_password")), 0, 0);
            grid.add(newPasswordField, 1, 0);
            grid.add(new Label(I18nManager.getInstance().get("runtime.confirm_password")), 0, 1);
            grid.add(confirmPasswordField, 1, 1);
            grid.add(hintLabel, 0, 2, 2, 1);

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            // OK 按钮在输入校验前禁用
            dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
            newPasswordField.textProperty().addListener((obs, oldVal, newVal) ->
                validatePasswordFields(newPasswordField, confirmPasswordField, dialog));
            confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) ->
                validatePasswordFields(newPasswordField, confirmPasswordField, dialog));

            dialog.setResultConverter(button -> {
                if (button == ButtonType.OK) {
                    return newPasswordField.getText().trim();
                }
                return null;
            });

            dialog.showAndWait().ifPresent(newPassword -> {
                // 二次校验
                if (newPassword.length() < 6) {
                    showError(I18nManager.getInstance().get("runtime.password_too_short"));
                    return;
                }

                selected.password = PasswordUtil.hashPassword(newPassword);
                try {
                    if (DAOFactory.getInstance().getUserDAO().update(selected)) {
                        // 密码重置后作废该用户已签发的全部 API token
                        ApiServer.getInstance().invalidateUserTokens(selected.id);
                        audit("USER_PASSWORD_RESET", selected.username);
                        loadUsers();
                        updateStatus(I18nManager.getInstance().get("user.password_reset"));
                    } else {
                        showError(I18nManager.getInstance().get("runtime.password_reset_failed"));
                    }
                } catch (SQLException e) {
                    logger.error("重置密码失败", e);
                    showError(I18nManager.getInstance().get("runtime.operation_failed"));
                }
            });
        }
    }

    /**
     * 校验密码输入字段
     */
    private void validatePasswordFields(PasswordField newPwd, PasswordField confirmPwd, Dialog<?> dialog) {
        boolean valid = !newPwd.getText().trim().isEmpty()
            && newPwd.getText().trim().length() >= 6
            && newPwd.getText().equals(confirmPwd.getText());
        dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(!valid);
    }

    /**
     * 权限校验：要求当前用户为管理员
     * @return 如果有权限返回 true，否则返回 false
     */
    private boolean requireAdmin() {
        if (currentUser == null || !"admin".equals(currentUser.role)) {
            showError(I18nManager.getInstance().get("runtime.permission_denied"));
            return false;
        }
        return true;
    }

    /**
     * 处理激活用户
     */
    @FXML
    public void handleActivateUser() {
        if (!requireAdmin()) {
            return;
        }
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.active = true;
            try {
                if (DAOFactory.getInstance().getUserDAO().update(selected)) {
                    audit("USER_ACTIVATED", selected.username);
                    loadUsers();
                    updateStatus(I18nManager.getInstance().get("user.activated"));
                } else {
                    showError(I18nManager.getInstance().get("runtime.user_activate_result_failed"));
                }
            } catch (SQLException e) {
                logger.error("激活用户失败", e);
                showError(I18nManager.getInstance().get("runtime.user_activate_failed", e.getMessage()));
            }
        }
    }

    /**
     * 处理禁用用户
     */
    @FXML
    public void handleDeactivateUser() {
        if (!requireAdmin()) {
            return;
        }
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // 不允许禁用admin用户
            if ("admin".equals(selected.username)) {
                showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.admin_disable_forbidden"));
                return;
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18nManager.getInstance().get(I18nKeys.Common.CONFIRM));
            alert.setHeaderText(null);
            alert.setContentText(I18nManager.getInstance().get("runtime.user_disable_confirm", selected.name));

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                selected.active = false;
                try {
                if (DAOFactory.getInstance().getUserDAO().update(selected)) {
                    // 禁用后立即作废该用户已签发的全部 API token（辞退/停用场景）
                    ApiServer.getInstance().invalidateUserTokens(selected.id);
                    audit("USER_DEACTIVATED", selected.username);
                        loadUsers();
                        updateStatus(I18nManager.getInstance().get("user.deactivated"));
                    } else {
                        showError(I18nManager.getInstance().get("runtime.user_disable_result_failed"));
                    }
                } catch (SQLException e) {
                    logger.error("禁用用户失败", e);
                    showError(I18nManager.getInstance().get("runtime.user_disable_failed", e.getMessage()));
                }
            }
        }
    }

    private void audit(String operation, String targetUsername) {
        com.cashier.service.AuditService.success(
            currentUser == null ? null : currentUser.username,
            "USER", operation, "目标用户=" + targetUsername, 1);
    }

    /**
     * 处理搜索
     */
    @FXML
    public void handleSearch() {
        applyFilters();
    }

    /**
     * 处理清除搜索
     */
    @FXML
    public void handleClearSearch() {
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
                if (!"all".equals(roleFilter)) {
                    if (!roleFilter.equals(u.role)) {
                        return false;
                    }
                }

                // 状态筛选
                if (!"all".equals(statusFilter)) {
                    boolean active = "active".equals(statusFilter);
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
        com.cashier.util.FXUtils.showError(message);
    }

    /**
     * 刷新用户列表
     */
    public void refreshUsers() {
        loadUsers();
    }
}
