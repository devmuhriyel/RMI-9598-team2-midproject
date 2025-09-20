package AdminMVC.AdminControllers;

import AdminMVC.AdminModel.UnifiedAdminModel;
import ClientMVC.ClientModel.AdminModel;
import AdminMVC.AdminView.AdminPageJFXController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;
import java.util.List;

public class AdminController {

    private AdminPageJFXController view;
    // **CHANGED: Removed direct XML file loading, now uses UnifiedAdminModel**
    private UnifiedAdminModel adminModel;

    public AdminController(AdminPageJFXController view) {
        this.view = view;
        // **CHANGED: Initialize admin model instead of loading XML directly**
        this.adminModel = new UnifiedAdminModel();
        setupListeners();
        initListeners();
    }

    // **CHANGED: Added constructor that accepts an existing admin model**
    public AdminController(AdminPageJFXController view, UnifiedAdminModel adminModel) {
        this.view = view;
        this.adminModel = adminModel;
        setupListeners();
        initListeners();
    }

    // **CHANGED: Setup listeners for admin model updates**
    private void setupListeners() {
        if (adminModel != null) {
            adminModel.setDataUpdateListener(updateType -> {
                switch (updateType) {
                    case "USERS_UPDATED":
                        if (view.getTableView() != null) {
                            view.getTableView().setItems(adminModel.getAllUsers());
                        }
                        break;
                    case "GROUPS_UPDATED":
                        if (view.getGroupsTableView() != null) {
                            view.getGroupsTableView().setItems(adminModel.getAllGroups());
                        }
                        break;
                }
            });

            adminModel.setOperationResultListener(result -> {
                showAlert("Operation Result", result, Alert.AlertType.INFORMATION);
            });

            adminModel.setUserStatsListener(stats -> {
                showAlert("User Statistics", stats, Alert.AlertType.INFORMATION);
            });
        }
    }

    private void initListeners() {
        // Users search
        if (view.getSearchButton() != null) {
            view.getSearchButton().setOnAction(e -> onSearchUsers());
        }

        // Groups search
        if (view.getGroupSearchButton() != null) {
            view.getGroupSearchButton().setOnAction(e -> onSearchGroups());
        }

        // Navigation
        if (view.getGroupsButton() != null) {
            view.getGroupsButton().setOnAction(e -> switchToGroups());
        }
        if (view.getBackToUsersButton() != null) {
            view.getBackToUsersButton().setOnAction(e -> switchToUsers());
        }

        // Delete buttons
        if (view.getDeleteButton() != null) {
            view.getDeleteButton().setOnAction(e -> onDeleteUser());
        }
        if (view.getGroupDeleteButton() != null) {
            view.getGroupDeleteButton().setOnAction(e -> onDeleteGroup());
        }

        // Edit buttons
        if (view.getEditButton() != null) {
            view.getEditButton().setOnAction(e -> onEditUser());
        }
        if (view.getGroupsEditButton() != null) {
            view.getGroupsEditButton().setOnAction(e -> onEditGroups());
        }

        // **CHANGED: Added new admin operation buttons**
        if (view.getCreateUserButton() != null) {
            view.getCreateUserButton().setOnAction(e -> onCreateUser());
        }
        if (view.getCreateGroupButton() != null) {
            view.getCreateGroupButton().setOnAction(e -> onCreateGroup());
        }
        if (view.getViewStatsButton() != null) {
            view.getViewStatsButton().setOnAction(e -> onViewUserStats());
        }
        if (view.getViewActiveUsersButton() != null) {
            view.getViewActiveUsersButton().setOnAction(e -> onViewActiveUsers());
        }
        if (view.getViewCommunityMessagesButton() != null) {
            view.getViewCommunityMessagesButton().setOnAction(e -> onViewCommunityMessages());
        }
        if (view.getRefreshButton() != null) {
            view.getRefreshButton().setOnAction(e -> onRefreshData());
        }
    }

    // **CHANGED: Updated search methods to use admin model**
    private void onSearchUsers() {
        if (adminModel == null || !adminModel.isConnected()) {
            showAlert("Error", "Not connected to server", Alert.AlertType.ERROR);
            return;
        }
        String keyword = view.getSearchField().getText();
        adminModel.searchUsers(keyword);
    }

    private void onSearchGroups() {
        if (adminModel == null || !adminModel.isConnected()) {
            showAlert("Error", "Not connected to server", Alert.AlertType.ERROR);
            return;
        }
        String keyword = view.getGroupSearchField().getText();
        adminModel.searchGroups(keyword);
    }

    private void switchToGroups() {
        view.getTableView().getScene().getWindow().hide();
        loadFXML("/FXMLFiles/AdminGroups.fxml", "Groups Page");
    }

    private void switchToUsers() {
        view.getGroupsTableView().getScene().getWindow().hide();
        loadFXML("/FXMLFiles/Admin.fxml", "Admins Page");
    }

    private void loadFXML(String path, String title) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(path));
            javafx.scene.Parent root = loader.load();
            AdminPageJFXController newController = loader.getController();
            // **CHANGED: Pass the admin model to maintain connection**
            new AdminController(newController, adminModel);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setScene(new javafx.scene.Scene(root, 800, 600));
            stage.setTitle(title);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // **CHANGED: Updated delete methods to use admin model**
    private void onDeleteUser() {
        if (adminModel == null || !adminModel.isConnected()) {
            showAlert("Error", "Not connected to server", Alert.AlertType.ERROR);
            return;
        }

        AdminModel selected = view.getTableView().getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirm Deletion");
            confirmation.setHeaderText("Delete User");
            confirmation.setContentText("Are you sure you want to delete user: " + selected.getUsername() + "?");

            Optional<javafx.scene.control.ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
                adminModel.deleteUser(selected.getUsername());
            }
        } else {
            showAlert("No Selection", "Please select a user to delete", Alert.AlertType.WARNING);
        }
    }

    private void onDeleteGroup() {
        if (adminModel == null || !adminModel.isConnected()) {
            showAlert("Error", "Not connected to server", Alert.AlertType.ERROR);
            return;
        }

        AdminModel selected = view.getGroupsTableView().getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirm Deletion");
            confirmation.setHeaderText("Delete Group");
            confirmation.setContentText("Are you sure you want to delete group: " + selected.getGroupName() + "?");

            Optional<javafx.scene.control.ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
                adminModel.deleteGroup(selected.getGroupName(), selected.getOwner());
            }
        } else {
            showAlert("No Selection", "Please select a group to delete", Alert.AlertType.WARNING);
        }
    }

    // **CHANGED: Updated edit methods to use admin model for password changes**
    private void onEditUser() {
        if (adminModel == null || !adminModel.isConnected()) {
            showAlert("Error", "Not connected to server", Alert.AlertType.ERROR);
            return;
        }

        AdminModel selected = view.getTableView().getSelectionModel().getSelectedItem();
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
            adminModel.changeUserPassword(selected.getUsername(), result.get().trim());
        }
    }

    private void onEditGroups() {
        showAlert("Info", "Group editing is not available in server mode", Alert.AlertType.INFORMATION);
    }

    // **CHANGED: Added new admin operation methods**
    private void onCreateUser() {
        if (adminModel == null || !adminModel.isConnected()) {
            showAlert("Error", "Not connected to server", Alert.AlertType.ERROR);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXMLFiles/CreateUserDialog.fxml"));
            Parent root = loader.load();

            TextField usernameField = (TextField) root.lookup("#usernameField");
            TextField passwordField = (TextField) root.lookup("#passwordField");

            Stage dialog = new Stage();
            dialog.setTitle("Create New User");
            dialog.setScene(new Scene(root, 300, 200));
            dialog.showAndWait();

            if (usernameField != null && passwordField != null) {
                String username = usernameField.getText().trim();
                String password = passwordField.getText().trim();

                if (!username.isEmpty() && !password.isEmpty()) {
                    adminModel.createNewUser(username, password);
                }
            }
        } catch (Exception e) {
            // Fallback to input dialogs
            TextInputDialog usernameDialog = new TextInputDialog();
            usernameDialog.setTitle("Create User");
            usernameDialog.setHeaderText("Create New User");
            usernameDialog.setContentText("Username:");

            Optional<String> usernameResult = usernameDialog.showAndWait();
            if (usernameResult.isPresent() && !usernameResult.get().trim().isEmpty()) {
                TextInputDialog passwordDialog = new TextInputDialog();
                passwordDialog.setTitle("Create User");
                passwordDialog.setHeaderText("Create New User");
                passwordDialog.setContentText("Password:");

                Optional<String> passwordResult = passwordDialog.showAndWait();
                if (passwordResult.isPresent() && !passwordResult.get().trim().isEmpty()) {
                    adminModel.createNewUser(usernameResult.get().trim(), passwordResult.get().trim());
                }
            }
        }
    }

    private void onCreateGroup() {
        if (adminModel == null || !adminModel.isConnected()) {
            showAlert("Error", "Not connected to server", Alert.AlertType.ERROR);
            return;
        }

        TextInputDialog groupDialog = new TextInputDialog();
        groupDialog.setTitle("Create Group");
        groupDialog.setHeaderText("Create New Group");
        groupDialog.setContentText("Group Name:");

        Optional<String> groupResult = groupDialog.showAndWait();
        if (groupResult.isPresent() && !groupResult.get().trim().isEmpty()) {
            TextInputDialog creatorDialog = new TextInputDialog();
            creatorDialog.setTitle("Create Group");
            creatorDialog.setHeaderText("Create New Group");
            creatorDialog.setContentText("Creator Username:");

            Optional<String> creatorResult = creatorDialog.showAndWait();
            if (creatorResult.isPresent() && !creatorResult.get().trim().isEmpty()) {
                adminModel.createNewGroup(groupResult.get().trim(), creatorResult.get().trim());
            }
        }
    }

    private void onViewUserStats() {
        if (adminModel == null || !adminModel.isConnected()) {
            showAlert("Error", "Not connected to server", Alert.AlertType.ERROR);
            return;
        }

        AdminModel selected = view.getTableView().getSelectionModel().getSelectedItem();
        if (selected != null) {
            adminModel.getUserStats(selected.getUsername());
        } else {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("User Statistics");
            dialog.setHeaderText("Get User Statistics");
            dialog.setContentText("Enter username:");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                adminModel.getUserStats(result.get().trim());
            }
        }
    }

    private void onViewActiveUsers() {
        if (adminModel == null || !adminModel.isConnected()) {
            showAlert("Error", "Not connected to server", Alert.AlertType.ERROR);
            return;
        }

        adminModel.requestActiveUsers();
        // Create a dialog to show active users
        adminModel.setActiveUsersUpdateListener(updateType -> {
            if ("ACTIVE_USERS_UPDATED".equals(updateType)) {
                StringBuilder activeUsersText = new StringBuilder("Currently Active Users:\n");
                for (String user : adminModel.getActiveUsers()) {
                    activeUsersText.append("- ").append(user).append("\n");
                }
                if (adminModel.getActiveUsers().isEmpty()) {
                    activeUsersText.append("No active users");
                }
                showAlert("Active Users", activeUsersText.toString(), Alert.AlertType.INFORMATION);
            }
        });
    }

    private void onViewCommunityMessages() {
        if (adminModel == null || !adminModel.isConnected()) {
            showAlert("Error", "Not connected to server", Alert.AlertType.ERROR);
            return;
        }

        adminModel.requestCommunityMessages();
        // Create a dialog to show community messages
        adminModel.setCommunityMessagesUpdateListener(updateType -> {
            if ("COMMUNITY_MESSAGES_UPDATED".equals(updateType)) {
                StringBuilder messagesText = new StringBuilder("Recent Community Messages:\n\n");
                List<String> messages = adminModel.getCommunityMessages();
                if (messages.isEmpty()) {
                    messagesText.append("No community messages");
                } else {
                    // Show last 10 messages
                    int start = Math.max(0, messages.size() - 10);
                    for (int i = start; i < messages.size(); i++) {
                        messagesText.append(messages.get(i)).append("\n");
                    }
                }
                showAlert("Community Messages", messagesText.toString(), Alert.AlertType.INFORMATION);
            }
        });
    }

    private void onRefreshData() {
        if (adminModel == null || !adminModel.isConnected()) {
            showAlert("Error", "Not connected to server", Alert.AlertType.ERROR);
            return;
        }

        adminModel.refreshData();
        showAlert("Refresh", "Data refreshed successfully", Alert.AlertType.INFORMATION);
    }

    // **CHANGED: Added utility method for showing alerts**
    private void showAlert(String title, String message, Alert.AlertType type) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // **CHANGED: Added method to set admin model (for external initialization)**
    public void setAdminModel(UnifiedAdminModel adminModel) {
        this.adminModel = adminModel;
        setupListeners();
        if (adminModel != null && adminModel.isConnected()) {
            adminModel.loadData();
        }
    }

    // **CHANGED: Added getter for admin model**
    public UnifiedAdminModel getAdminModel() {
        return adminModel;
    }
}