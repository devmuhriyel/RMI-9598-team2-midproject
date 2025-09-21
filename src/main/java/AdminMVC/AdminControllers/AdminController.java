package AdminMVC.AdminControllers;

import AdminMVC.AdminModel.UnifiedAdminModel;
import ClientMVC.ClientModel.AdminModel;
import AdminMVC.AdminView.AdminPageJFXController;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.application.Platform;

import java.util.Optional;
import java.util.List;

public class AdminController {

    private AdminPageJFXController view;
    private UnifiedAdminModel adminModel;
    private static UnifiedAdminModel sharedAdminModel; // Shared model for navigation

    public AdminController(AdminPageJFXController view) {
        this.view = view;
        this.adminModel = sharedAdminModel; // Use shared model if available
        if (this.adminModel == null) {
            this.adminModel = new UnifiedAdminModel();
        }
        setupListeners();
        initListeners();

        // Load data if already connected
        if (adminModel != null && adminModel.isConnected()) {
            Platform.runLater(() -> adminModel.loadData());
        }
    }

    public AdminController(AdminPageJFXController view, UnifiedAdminModel adminModel) {
        this.view = view;
        this.adminModel = adminModel;
        sharedAdminModel = adminModel;
        setupListeners();
        initListeners();

        // Load data immediately
        if (adminModel != null && adminModel.isConnected()) {
            Platform.runLater(() -> adminModel.loadData());
        }
    }

    private void setupListeners() {
        if (adminModel != null) {
            adminModel.setDataUpdateListener(updateType -> {
                Platform.runLater(() -> {
                    switch (updateType) {
                        case "USERS_UPDATED":
                            if (view.getTableView() != null) {
                                view.getTableView().setItems(adminModel.getAllUsers());
                                view.getTableView().refresh();
                            }
                            break;
                        case "GROUPS_UPDATED":
                            if (view.getGroupsTableView() != null) {
                                view.getGroupsTableView().setItems(adminModel.getAllGroups());
                                view.getGroupsTableView().refresh();
                            }
                            break;
                    }
                });
            });

            adminModel.setOperationResultListener(result -> {
                showAlert("Operation Result", result, Alert.AlertType.INFORMATION);
            });

            adminModel.setUserStatsListener(stats -> {
                showAlert("User Statistics", stats, Alert.AlertType.INFORMATION);
            });

            adminModel.setActiveUsersUpdateListener(updateType -> {
                if ("ACTIVE_USERS_UPDATED".equals(updateType)) {
                    StringBuilder activeUsersText = new StringBuilder("Currently Active Users:\n\n");
                    List<String> users = adminModel.getActiveUsers();
                    if (users.isEmpty()) {
                        activeUsersText.append("No active users");
                    } else {
                        for (String user : users) {
                            activeUsersText.append("• ").append(user).append("\n");
                        }
                    }
                    showAlert("Active Users", activeUsersText.toString(), Alert.AlertType.INFORMATION);
                }
            });

            adminModel.setCommunityMessagesUpdateListener(updateType -> {
                if ("COMMUNITY_MESSAGES_UPDATED".equals(updateType)) {
                    StringBuilder messagesText = new StringBuilder("Recent Community Messages:\n\n");
                    List<String> messages = adminModel.getCommunityMessages();
                    if (messages.isEmpty()) {
                        messagesText.append("No community messages");
                    } else {
                        int start = Math.max(0, messages.size() - 10);
                        for (int i = start; i < messages.size(); i++) {
                            messagesText.append(messages.get(i)).append("\n\n");
                        }
                    }
                    showAlert("Community Messages", messagesText.toString(), Alert.AlertType.INFORMATION);
                }
            });
        }
    }

    private void initListeners() {
        // Search functionality
        if (view.getSearchButton() != null) {
            view.getSearchButton().setOnAction(e -> onSearchUsers());
        }

        // Main admin operation buttons
        if (view.getCreateUserButton() != null) {
            view.getCreateUserButton().setOnAction(e -> onCreateUser());
        }
        if (view.getViewActiveUsersButton() != null) {
            view.getViewActiveUsersButton().setOnAction(e -> onViewActiveUsers());
        }
        if (view.getCreateGroupButton() != null) {
            view.getCreateGroupButton().setOnAction(e -> onCreateGroup());
        }
        if (view.getViewCommunityMessagesButton() != null) {
            view.getViewCommunityMessagesButton().setOnAction(e -> onViewCommunityMessages());
        }
        if (view.getRefreshButton() != null) {
            view.getRefreshButton().setOnAction(e -> onRefreshData());
        }

        // Edit and Delete buttons for users
        if (view.getEditButton() != null) {
            view.getEditButton().setOnAction(e -> onEditUser());
        }
        if (view.getDeleteButton() != null) {
            view.getDeleteButton().setOnAction(e -> onDeleteUser());
        }

        // Edit and Delete buttons for groups
        if (view.getGroupsEditButton() != null) {
            view.getGroupsEditButton().setOnAction(e -> onEditGroups());
        }
        if (view.getGroupDeleteButton() != null) {
            view.getGroupDeleteButton().setOnAction(e -> onDeleteGroup());
        }
    }

    // Search functionality
    private void onSearchUsers() {
        if (!validateConnection()) return;

        String keyword = "";
        if (view.getSearchField() != null) {
            keyword = view.getSearchField().getText().trim();
        }

        if (keyword.isEmpty()) {
            adminModel.requestAllUsers();
        } else {
            adminModel.searchUsers(keyword);
        }
    }

    // User operations
    private void onCreateUser() {
        if (!validateConnection()) return;

        TextInputDialog usernameDialog = new TextInputDialog();
        usernameDialog.setTitle("Create User");
        usernameDialog.setHeaderText("Create New User");
        usernameDialog.setContentText("Username (min 3 characters):");

        Optional<String> usernameResult = usernameDialog.showAndWait();
        if (usernameResult.isPresent() && !usernameResult.get().trim().isEmpty()) {
            String username = usernameResult.get().trim();

            if (username.length() < 3) {
                showAlert("Invalid Username", "Username must be at least 3 characters long", Alert.AlertType.WARNING);
                return;
            }

            TextInputDialog passwordDialog = new TextInputDialog();
            passwordDialog.setTitle("Create User");
            passwordDialog.setHeaderText("Create New User: " + username);
            passwordDialog.setContentText("Password (min 4 characters):");

            Optional<String> passwordResult = passwordDialog.showAndWait();
            if (passwordResult.isPresent() && !passwordResult.get().trim().isEmpty()) {
                String password = passwordResult.get().trim();

                if (password.length() < 4) {
                    showAlert("Invalid Password", "Password must be at least 4 characters long", Alert.AlertType.WARNING);
                    return;
                }

                adminModel.createNewUser(username, password);
            }
        }
    }

    private void onEditUser() {
        if (!validateConnection()) return;

        AdminModel selected = null;
        if (view.getTableView() != null) {
            selected = view.getTableView().getSelectionModel().getSelectedItem();
        }

        if (selected == null) {
            showAlert("No Selection", "Please select a user first", Alert.AlertType.WARNING);
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Change Password");
        dialog.setHeaderText("Change password for: " + selected.getUsername());
        dialog.setContentText("Enter new password:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String newPassword = result.get().trim();
            if (newPassword.length() < 4) {
                showAlert("Invalid Password", "Password must be at least 4 characters long", Alert.AlertType.WARNING);
                return;
            }
            adminModel.changeUserPassword(selected.getUsername(), newPassword);
        }
    }

    private void onDeleteUser() {
        if (!validateConnection()) return;

        AdminModel selected = null;
        if (view.getTableView() != null) {
            selected = view.getTableView().getSelectionModel().getSelectedItem();
        }

        if (selected != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirm Deletion");
            confirmation.setHeaderText("Delete User");
            confirmation.setContentText("Are you sure you want to delete user: " + selected.getUsername() + "?\n\nThis action cannot be undone.");

            Optional<javafx.scene.control.ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
                adminModel.deleteUser(selected.getUsername());
            }
        } else {
            showAlert("No Selection", "Please select a user to delete", Alert.AlertType.WARNING);
        }
    }

    private void onViewActiveUsers() {
        if (!validateConnection()) return;
        adminModel.requestActiveUsers();
    }

    // Group operations
    private void onCreateGroup() {
        if (!validateConnection()) return;

        TextInputDialog groupDialog = new TextInputDialog();
        groupDialog.setTitle("Create Group");
        groupDialog.setHeaderText("Create New Group");
        groupDialog.setContentText("Group Name (min 2 characters):");

        Optional<String> groupResult = groupDialog.showAndWait();
        if (groupResult.isPresent() && !groupResult.get().trim().isEmpty()) {
            String groupName = groupResult.get().trim();

            if (groupName.length() < 2) {
                showAlert("Invalid Group Name", "Group name must be at least 2 characters long", Alert.AlertType.WARNING);
                return;
            }

            TextInputDialog creatorDialog = new TextInputDialog();
            creatorDialog.setTitle("Create Group");
            creatorDialog.setHeaderText("Create New Group: " + groupName);
            creatorDialog.setContentText("Creator Username:");

            Optional<String> creatorResult = creatorDialog.showAndWait();
            if (creatorResult.isPresent() && !creatorResult.get().trim().isEmpty()) {
                String creator = creatorResult.get().trim();

                if (creator.length() < 3) {
                    showAlert("Invalid Creator", "Creator username must be at least 3 characters long", Alert.AlertType.WARNING);
                    return;
                }

                adminModel.createNewGroup(groupName, creator);
            }
        }
    }

    private void onEditGroups() {
        showAlert("Info", "Group editing is not available. Use delete and create operations instead.", Alert.AlertType.INFORMATION);
    }

    private void onDeleteGroup() {
        if (!validateConnection()) return;

        AdminModel selected = null;
        if (view.getGroupsTableView() != null) {
            selected = view.getGroupsTableView().getSelectionModel().getSelectedItem();
        }

        if (selected != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirm Deletion");
            confirmation.setHeaderText("Delete Group");
            confirmation.setContentText("Are you sure you want to delete group: " + selected.getGroupName() + "?\n\nThis action cannot be undone.");

            Optional<javafx.scene.control.ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
                adminModel.deleteGroup(selected.getGroupName(), selected.getOwner());
            }
        } else {
            showAlert("No Selection", "Please select a group to delete", Alert.AlertType.WARNING);
        }
    }

    // Community operations
    private void onViewCommunityMessages() {
        if (!validateConnection()) return;
        adminModel.requestCommunityMessages();
    }

    private void onRefreshData() {
        if (!validateConnection()) return;
        adminModel.refreshData();
        showAlert("Refresh", "Data refresh requested successfully", Alert.AlertType.INFORMATION);
    }

    // Utility methods
    private boolean validateConnection() {
        if (adminModel == null || !adminModel.isConnected()) {
            showAlert("Error", "Not connected to server. Please reconnect.", Alert.AlertType.ERROR);
            return false;
        }
        return true;
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // Getters and setters
    public void setAdminModel(UnifiedAdminModel adminModel) {
        this.adminModel = adminModel;
        sharedAdminModel = adminModel;
        setupListeners();
        if (adminModel != null && adminModel.isConnected()) {
            adminModel.loadData();
        }
    }

    public UnifiedAdminModel getAdminModel() {
        return adminModel;
    }

    public static UnifiedAdminModel getSharedAdminModel() {
        return sharedAdminModel;
    }

    public static void setSharedAdminModel(UnifiedAdminModel model) {
        sharedAdminModel = model;
    }
}