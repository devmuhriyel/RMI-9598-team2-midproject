package AdminMVC.AdminControllers;

import AdminMVC.AdminModel.UnifiedAdminModel;
import AdminMVC.AdminView.Admin.AdminPageJFXController;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

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
        setupPasswordResetHandling();
        // Load initial data
        refreshData();
        startAutoRefresh();
    }

    private void startAutoRefresh() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            model.requestAllUsersWithStatus();
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    /**
     * Set up password reset handling - call this in your initialize() method
     */

    private void setupPasswordResetHandling() {
        // Set up listener for verification code validation requests
        model.setVerificationCodeValidListener(this::handleVerificationCodeValidRequest);

        // Store the password reset request data
        // (The operation result listener is already set up in setupModelListeners())
    }

    /**
     * Handle incoming password reset request from a user
     */
    private void handlePasswordResetRequest(String username) {
        System.out.println("[ADMIN] Password reset request received for user: " + username);
        System.out.println("[ADMIN] User has provided their security verification code for validation");

        // Store the password reset request data
        currentPasswordResetRequest = new PasswordResetData(username,
                java.time.LocalDateTime.now().toString(), false);

        // Notify UI layer to display password reset request
        notifyUIOfPasswordResetRequest(username);
    }

    /**
     * Handle operation results including password reset confirmations
     */
    private void handleOperationResult(String result) {
        System.out.println("[ADMIN] Operation result received: " + result);

        if (result.contains("ADMIN_PASSWORD_RESET_SUCCESS")) {
            System.out.println("[ADMIN] Password reset completed successfully");
            notifyUIOfSuccess("Password reset completed successfully. User has been notified with new password.");

            // Clear current password reset request
            currentPasswordResetRequest = null;

        } else if (result.contains("ADMIN_PASSWORD_RESET_FAILED")) {
            if (result.contains("Invalid verification code")) {
                System.out.println("[ADMIN] Password reset failed - invalid verification code");
                notifyUIOfError("Verification code does not match user's security code. User has been notified.");

            } else if (result.contains("Failed to change password")) {
                System.out.println("[ADMIN] Password reset failed - system error");
                notifyUIOfError("System error occurred while changing password. Please try again.");

            } else {
                System.out.println("[ADMIN] Password reset failed - unknown reason");
                notifyUIOfError("Password reset failed: " + result);
            }

        } else if (result.contains("PASSWORD_RESET_REJECTED")) {
            System.out.println("[ADMIN] Password reset request rejected successfully");
            notifyUIOfSuccess("Password reset request rejected. User has been notified.");

            // Clear current password reset request
            currentPasswordResetRequest = null;

        } else if (result.contains("Password changed successfully")) {
            System.out.println("[ADMIN] Password changed successfully");
            notifyUIOfSuccess("Password changed successfully. User has been notified.");

        } else if (result.contains("Password change failed")) {
            System.out.println("[ADMIN] Password change failed");
            notifyUIOfError("Failed to change password. Please try again.");
        }

        // Refresh admin data after any password operation
        model.refreshData();
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
    /**
     * Handle verification code validation request
     */
    private void handleVerificationCodeValidRequest(String username) {
        System.out.println("[ADMIN] Verification code validated for user: " + username);
        System.out.println("[ADMIN] User's security code has been verified - ready for password change");

        // Update password reset request status
        if (currentPasswordResetRequest != null && currentPasswordResetRequest.getUsername().equals(username)) {
            currentPasswordResetRequest = new PasswordResetData(username,
                    currentPasswordResetRequest.getTimestamp(), true);
        }

        // Notify UI layer that verification was successful
        notifyUIOfVerificationSuccess(username);
    }

    /**
     * Process admin's decision to approve password reset with verification code
     */
    public void processPasswordResetApproval(String username, String verificationCode, String newPassword) {
        // Validate inputs
        if (username == null || username.trim().isEmpty()) {
            notifyUIOfError("Username cannot be empty");
            return;
        }

        if (verificationCode == null || verificationCode.trim().isEmpty()) {
            notifyUIOfError("Verification code cannot be empty");
            return;
        }

        if (newPassword == null || newPassword.trim().length() < 4) {
            notifyUIOfError("Password must be at least 4 characters long");
            return;
        }

        System.out.println("[ADMIN] Processing password reset approval for user: " + username);

        // Send verification and password change request to server
        String request = "<Request><type>ADMIN_VERIFY_AND_CHANGE_PASSWORD</type><content>" +
                username + "![]!" + verificationCode + "![]!" + newPassword + "</content></Request>";

        model.sendAdminCommand(request);

        System.out.println("[ADMIN] Password reset approval sent to server");
        notifyUIOfProcessingStatus("Verifying security code and changing password...");
    }

    /**
     * Process admin's decision to reject password reset request
     */
    public void processPasswordResetRejection(String username) {
        if (username == null || username.trim().isEmpty()) {
            notifyUIOfError("Username cannot be empty");
            return;
        }

        System.out.println("[ADMIN] Processing password reset rejection for user: " + username);

        // Send rejection to server
        String rejectRequest = "<Request><type>REJECT_PASSWORD_RESET</type><content>" +
                username + "</content></Request>";

        model.sendAdminCommand(rejectRequest);

        System.out.println("[ADMIN] Password reset rejection sent to server");
        notifyUIOfProcessingStatus("Password reset request rejected for user: " + username);
    }


    /**
     * Process direct password change (when verification is already complete)
     */
    public void processDirectPasswordChange(String username, String newPassword) {
        if (username == null || username.trim().isEmpty()) {
            notifyUIOfError("Username cannot be empty");
            return;
        }

        if (newPassword == null || newPassword.trim().length() < 4) {
            notifyUIOfError("Password must be at least 4 characters long");
            return;
        }

        System.out.println("[ADMIN] Processing direct password change for user: " + username);

        // Change password directly since verification is already done
        model.changeUserPassword(username, newPassword);

        System.out.println("[ADMIN] Direct password change request sent to server");
    }

    // ========== UI NOTIFICATION METHODS (to be implemented by UI layer) ==========

    /**
     * Notify UI layer of password reset request - to be implemented by UI
     */
    private void notifyUIOfPasswordResetRequest(String username) {
        // UI layer should implement this to show password reset request notification
        System.out.println("[ADMIN UI] Should display password reset request for user: " + username);
        // Example: Show dialog or update UI component to display the request
    }

    /**
     * Notify UI layer of verification success - to be implemented by UI
     */
    private void notifyUIOfVerificationSuccess(String username) {
        // UI layer should implement this to show verification success
        System.out.println("[ADMIN UI] Should display verification success for user: " + username);
        // Example: Show confirmation dialog or update status
    }

    /**
     * Notify UI layer of processing status - to be implemented by UI
     */
    private void notifyUIOfProcessingStatus(String message) {
        // UI layer should implement this to show processing status
        System.out.println("[ADMIN UI] Processing status: " + message);
        // Example: Show progress indicator or status message
    }

    /**
     * Notify UI layer of success - to be implemented by UI
     */
    private void notifyUIOfSuccess(String message) {
        // UI layer should implement this to show success message
        System.out.println("[ADMIN UI] Success: " + message);
        // Example: Show success dialog or notification
    }

    /**
     * Notify UI layer of error - to be implemented by UI
     */
    private void notifyUIOfError(String message) {
        // UI layer should implement this to show error message
        System.out.println("[ADMIN UI] Error: " + message);
        // Example: Show error dialog or alert
    }

    // ========== UTILITY METHODS ==========

    /**
     * Validate password format
     */
    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 4;
    }

    /**
     * Validate username format
     */
    private boolean isValidUsername(String username) {
        return username != null && username.trim().length() >= 3;
    }

    /**
     * Get current password reset request data for UI
     */
    public PasswordResetData getCurrentPasswordResetRequest() {
        return currentPasswordResetRequest;
    }

    /**
     * Check if there is a pending password reset request
     */
    public boolean hasPendingPasswordResetRequest() {
        return currentPasswordResetRequest != null;
    }

    /**
     * Clear current password reset request
     */
    public void clearCurrentPasswordResetRequest() {
        currentPasswordResetRequest = null;
    }

    // ========== DATA CLASSES ==========

    /**
     * Data class for password reset information
     */
    public static class PasswordResetData {
        private final String username;
        private final String timestamp;
        private final boolean isVerified;

        public PasswordResetData(String username, String timestamp, boolean isVerified) {
            this.username = username;
            this.timestamp = timestamp;
            this.isVerified = isVerified;
        }

        public String getUsername() { return username; }
        public String getTimestamp() { return timestamp; }
        public boolean isVerified() { return isVerified; }

        @Override
        public String toString() {
            return "PasswordResetData{" +
                    "username='" + username + '\'' +
                    ", timestamp='" + timestamp + '\'' +
                    ", isVerified=" + isVerified +
                    '}';
        }
    }

    // Store current password reset request
    private PasswordResetData currentPasswordResetRequest;



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
                // Handle password reset related results first
                if (result.contains("ADMIN_PASSWORD_RESET_SUCCESS")) {
                    System.out.println("[ADMIN] Password reset completed successfully");
                    showAlert("Success", "Password reset completed successfully. User has been notified with new password.");
                    currentPasswordResetRequest = null;
                } else if (result.contains("ADMIN_PASSWORD_RESET_FAILED")) {
                    if (result.contains("Invalid verification code")) {
                        System.out.println("[ADMIN] Password reset failed - invalid verification code");
                        showAlert("Error", "Verification code does not match user's security code. User has been notified.");
                    } else if (result.contains("Failed to change password")) {
                        System.out.println("[ADMIN] Password reset failed - system error");
                        showAlert("Error", "System error occurred while changing password. Please try again.");
                    } else {
                        System.out.println("[ADMIN] Password reset failed - unknown reason");
                        showAlert("Error", "Password reset failed: " + result);
                    }
                } else if (result.contains("PASSWORD_RESET_REJECTED")) {
                    System.out.println("[ADMIN] Password reset request rejected successfully");
                    showAlert("Success", "Password reset request rejected. User has been notified.");
                    currentPasswordResetRequest = null;
                } else {
                    // Handle regular operation results
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Operation Result");
                    alert.setHeaderText(null);

                    // Check if it's an error message and change alert type
                    if (result.contains("already exists") || result.contains("Failed") ||
                            result.contains("Invalid") || result.contains("cannot contain")) {
                        alert.setAlertType(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                    }

                    alert.setContentText(result);
                    alert.showAndWait();
                }

                // Refresh admin data after any operation
                model.refreshData();
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