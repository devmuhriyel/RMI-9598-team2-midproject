package AdminMVC.AdminControllers;

import AdminMVC.AdminModel.UnifiedAdminModel;
import AdminMVC.AdminView.Admin.AdminPageJFXController;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdminController {
    private final UnifiedAdminModel model;
    private final AdminPageJFXController ui;

    public AdminController(AdminPageJFXController ui, UnifiedAdminModel model) {
        this.ui = ui;
        this.model = model;
        initialize();
    }

    private void initialize() {
        setupSearchListener();
        setupButtonListeners();
        setupModelListeners();

        // Load initial data
        refreshData();
    }

    private void setupSearchListener() {
        ui.setSearchListener(queryText -> {
            String q = (queryText == null) ? "" : queryText.trim().toLowerCase();
            if (q.isEmpty()) {
                ui.renderUsers(ui.getOriginalUsers());
                ui.renderGroups(ui.getOriginalGroups());
                return;
            }

            // Filter users
            List<Map<String, String>> filteredUsers = filterUsers(ui.getOriginalUsers(), q);

            // Filter groups
            List<Map<String, String>> filteredGroups = filterGroups(ui.getOriginalGroups(), q);

            ui.renderUsers(filteredUsers);
            ui.renderGroups(filteredGroups);
        });
    }

    private void setupButtonListeners() {
        // Search button
        if (ui.getSearchButton() != null) {
            ui.getSearchButton().setOnAction(event -> {
                String query = ui.getSearchField().getText();
                if (query != null && !query.trim().isEmpty()) {
                    performSearch(query.trim());
                } else {
                    refreshData();
                }
            });
        }

        // Add User button
        if (ui.getAddButton() != null) {
            ui.getAddButton().setOnAction(event -> showCreateUserDialog());
        }

        // Create Group button
        if (ui.getCreateButton() != null) {
            ui.getCreateButton().setOnAction(event -> showCreateGroupDialogEnhanced());
        }

        // Edit User button
        if (ui.getEditButton() != null) {
            ui.getEditButton().setOnAction(event -> editSelectedUser());
        }

        // Delete User button
        if (ui.getDeleteButton() != null) {
            ui.getDeleteButton().setOnAction(event -> deleteSelectedUser());
        }

        // Edit Group button
        if (ui.getEditGroupButton() != null) {
            ui.getEditGroupButton().setOnAction(event -> editSelectedGroup());
        }

        // Delete Group button
        if (ui.getDeleteGroupButton() != null) {
            ui.getDeleteGroupButton().setOnAction(event -> deleteSelectedGroup());
        }

        // Refresh button - shows all users
        if (ui.getRefreshButton() != null) {
            ui.getRefreshButton().setOnAction(event -> {
                ui.setShowingActiveUsersOnly(false);
                refreshData();
            });
        }

        // Active Users button - shows only online users
        if (ui.getActiveButton() != null) {
            ui.getActiveButton().setOnAction(event -> {
                ui.setShowingActiveUsersOnly(true);
                showActiveUsers();
            });
        }

        // Community Messages button
        if (ui.getCommunityButton() != null) {
            ui.getCommunityButton().setOnAction(event -> showCommunityMessages());
        }
    }

    private void setupModelListeners() {
        // Set up data update listeners
        model.setDataUpdateListener(updateType -> {
            Platform.runLater(() -> {
                switch (updateType) {
                    case "USERS_UPDATED":
                        ui.setUsers(model.getAllUsers(), ui.isShowingActiveUsersOnly());
                        break;
                    case "GROUPS_UPDATED":
                        ui.setGroups(model.getAllGroups());
                        break;
                }
            });
        });

        // Operation result listener
        model.setOperationResultListener(result -> {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Operation Result");
                alert.setHeaderText(null);
                alert.setContentText(result);
                alert.showAndWait();
            });
        });

        // Password reset listener
        model.setPasswordResetListener(username -> {
            Platform.runLater(() -> {
                ui.showPasswordResetNotification(username);
                ui.addPasswordResetRequest(username, generateVerificationCode());
            });
        });
    }

    private void performSearch(String query) {
        // Search both users and groups
        model.searchUsers(query);
        model.searchGroups(query);
    }

    private List<Map<String, String>> filterUsers(List<Map<String, String>> users, String queryLower) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> user : users) {
            if (user != null && user.get("username") != null &&
                    user.get("username").toLowerCase().contains(queryLower)) {
                result.add(user);
            }
        }
        return result;
    }

    private List<Map<String, String>> filterGroups(List<Map<String, String>> groups, String queryLower) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> group : groups) {
            if (group != null && group.get("groupName") != null &&
                    group.get("groupName").toLowerCase().contains(queryLower)) {
                result.add(group);
            }
        }
        return result;
    }

    private void refreshData() {
        model.loadData(); // This will trigger the listeners to update the UI
    }

    private void showActiveUsers() {
        model.requestActiveUsersOnly();
        // The model listener will handle displaying the results
    }

    private void showCreateUserDialog() {
        TextInputDialog usernameDialog = new TextInputDialog();
        usernameDialog.setTitle("Create User");
        usernameDialog.setHeaderText("Create New User");
        usernameDialog.setContentText("Username:");

        Optional<String> usernameResult = usernameDialog.showAndWait();
        if (usernameResult.isPresent() && !usernameResult.get().trim().isEmpty()) {
            String username = usernameResult.get().trim();

            TextInputDialog passwordDialog = new TextInputDialog();
            passwordDialog.setTitle("Create User");
            passwordDialog.setHeaderText("Create New User");
            passwordDialog.setContentText("Password:");

            Optional<String> passwordResult = passwordDialog.showAndWait();
            if (passwordResult.isPresent() && !passwordResult.get().trim().isEmpty()) {
                String password = passwordResult.get().trim();
                model.createNewUser(username, password);
            }
        }
    }

    private void showCreateGroupDialogEnhanced() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Group");
        dialog.setHeaderText("Create New Group");

        // Create form fields
        TextField groupNameField = new TextField();
        groupNameField.setPromptText("Group Name");

        TextField creatorField = new TextField();
        creatorField.setPromptText("Creator Username");

        TextArea membersArea = new TextArea();
        membersArea.setPromptText("Additional Members (comma-separated usernames)\nOptional - leave blank for group with just creator");
        membersArea.setPrefRowCount(4);

        VBox content = new VBox(10);
        content.getChildren().addAll(
                new Label("Group Name:"), groupNameField,
                new Label("Creator:"), creatorField,
                new Label("Additional Members:"), membersArea
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String groupName = groupNameField.getText().trim();
            String creator = creatorField.getText().trim();
            String membersText = membersArea.getText().trim();

            if (!groupName.isEmpty() && !creator.isEmpty()) {
                // Parse additional members
                List<String> additionalMembers = new ArrayList<>();
                if (!membersText.isEmpty()) {
                    String[] members = membersText.split(",");
                    for (String member : members) {
                        String trimmedMember = member.trim();
                        if (!trimmedMember.isEmpty()) {
                            additionalMembers.add(trimmedMember);
                        }
                    }
                }

                model.createNewGroupWithMembers(groupName, creator, additionalMembers);
            } else {
                showAlert("Invalid Input", "Group name and creator are required.");
            }
        }
    }

    private void editSelectedUser() {
        Map<String, String> selectedUser = ui.getSelectedUser();
        if (selectedUser == null) {
            showAlert("No Selection", "Please select a user to edit.");
            return;
        }

        String username = selectedUser.get("username");

        // Check for password reset requests
        String verificationCode = ui.getVerificationCodeForUser(username);
        if (verificationCode != null) {
            // Show verification dialog
            TextInputDialog verificationDialog = new TextInputDialog();
            verificationDialog.setTitle("Verify Password Reset");
            verificationDialog.setHeaderText("User '" + username + "' requested password reset");
            verificationDialog.setContentText("Enter verification code (sent to user):");

            Optional<String> codeResult = verificationDialog.showAndWait();
            if (codeResult.isPresent() && codeResult.get().equals(verificationCode)) {
                // Verification successful, allow password change
                TextInputDialog passwordDialog = new TextInputDialog();
                passwordDialog.setTitle("Reset Password");
                passwordDialog.setHeaderText("Reset Password for " + username);
                passwordDialog.setContentText("New Password:");

                Optional<String> passwordResult = passwordDialog.showAndWait();
                if (passwordResult.isPresent() && !passwordResult.get().trim().isEmpty()) {
                    String newPassword = passwordResult.get().trim();
                    model.changeUserPassword(username, newPassword);
                    ui.removePasswordResetRequest(username);
                }
            } else {
                showAlert("Verification Failed", "Invalid verification code.");
            }
        } else {
            // Normal password change
            TextInputDialog passwordDialog = new TextInputDialog();
            passwordDialog.setTitle("Edit User");
            passwordDialog.setHeaderText("Change Password for " + username);
            passwordDialog.setContentText("New Password:");

            Optional<String> result = passwordDialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                String newPassword = result.get().trim();
                model.changeUserPassword(username, newPassword);
            }
        }
    }

    private void deleteSelectedUser() {
        Map<String, String> selectedUser = ui.getSelectedUser();
        if (selectedUser == null) {
            showAlert("No Selection", "Please select a user to delete.");
            return;
        }

        String username = selectedUser.get("username");

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete User");
        confirmAlert.setContentText("Are you sure you want to delete user: " + username + "?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            model.deleteUser(username);
        }
    }

    private void editSelectedGroup() {
        Map<String, String> selectedGroup = ui.getSelectedGroup();
        if (selectedGroup == null) {
            showAlert("No Selection", "Please select a group to edit.");
            return;
        }

        String groupName = selectedGroup.get("groupName");
        String owner = selectedGroup.get("username");

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Group");
        dialog.setHeaderText("Edit Group: " + groupName);

        TextArea membersArea = new TextArea();
        membersArea.setPromptText("Add members (comma-separated usernames)");
        membersArea.setPrefRowCount(4);

        VBox content = new VBox(10);
        content.getChildren().addAll(
                new Label("Current Owner: " + owner),
                new Label("Add Members:"), membersArea
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String membersText = membersArea.getText().trim();
            if (!membersText.isEmpty()) {
                List<String> newMembers = Arrays.asList(membersText.split(","));
                model.addMembersToGroup(groupName, newMembers);
            }
        }
    }

    private void deleteSelectedGroup() {
        Map<String, String> selectedGroup = ui.getSelectedGroup();
        if (selectedGroup == null) {
            showAlert("No Selection", "Please select a group to delete.");
            return;
        }

        String groupName = selectedGroup.get("groupName");
        String owner = selectedGroup.get("username");

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Group");
        confirmAlert.setContentText("Are you sure you want to delete group: " + groupName + "?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            model.deleteGroup(groupName, owner);
        }
    }

    private void showCommunityMessages() {
        model.requestCommunityMessages();
        // The model listener will handle displaying the results
    }

    private String generateVerificationCode() {
        return String.format("%06d", new java.util.Random().nextInt(1000000));
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Public methods for external use
    public void connectToServer(String ip, String port) {
        UnifiedAdminModel.ConnectionResult result = model.connect(ip, port);
        Platform.runLater(() -> {
            Alert alert = new Alert(
                    result.getStatus() == UnifiedAdminModel.ConnectionStatus.SUCCESS ?
                            Alert.AlertType.INFORMATION : Alert.AlertType.ERROR
            );
            alert.setTitle("Connection Result");
            alert.setHeaderText(null);
            alert.setContentText(result.getMessage());
            alert.showAndWait();
        });
    }

    public void loginAdmin(String username, String password) {
        model.login(username, password);
    }

    public UnifiedAdminModel getModel() {
        return model;
    }
}