package AdminMVC.AdminControllers;

import AdminMVC.AdminModel.UnifiedAdminModel;
import AdminMVC.AdminView.Admin.AdminPageJFXController;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdminController {
    //VARIABLES
    private final UnifiedAdminModel model;
    private final AdminPageJFXController ui;

    //CONSTRUCTORS
    public AdminController(AdminPageJFXController ui, UnifiedAdminModel model) {
        this.ui = ui;
        this.model = model;
        initialize();
    }

    //METHODS
    private void initialize() {
        setupSearchListener();
        setupButtonListeners();
        setupModelListeners();
        setupPasswordResetHandling();
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

    private void setupSearchListener() {
        ui.setSearchListener(queryText -> {
            String q = (queryText == null) ? "" : queryText.trim().toLowerCase();
            if (q.isEmpty()) {
                ui.renderUsers(ui.getOriginalUsers());
                ui.renderGroups(ui.getOriginalGroups());
                return;
            }
            List<Map<String, String>> filteredUsers = filterUsers(ui.getOriginalUsers(), q);
            List<Map<String, String>> filteredGroups = filterGroups(ui.getOriginalGroups(), q);

            ui.renderUsers(filteredUsers);
            ui.renderGroups(filteredGroups);
        });
    }

    private void handleVerificationCodeValidRequest(String username) {
        if (currentPasswordResetRequest != null && currentPasswordResetRequest.getUsername().equals(username)) {
            currentPasswordResetRequest = new PasswordResetData(username,
                    currentPasswordResetRequest.getTimestamp(), true);
        }
        notifyUIOfVerificationSuccess(username);
    }

    public void processPasswordResetApproval(String username, String verificationCode, String newPassword) {
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
        String request = "<Request><type>ADMIN_VERIFY_AND_CHANGE_PASSWORD</type><content>" +
                username + "![]!" + verificationCode + "![]!" + newPassword + "</content></Request>";
        model.sendAdminCommand(request);
        notifyUIOfProcessingStatus("Verifying security code and changing password...");
    }

    public void processPasswordResetRejection(String username) {
        if (username == null || username.trim().isEmpty()) {
            notifyUIOfError("Username cannot be empty");
            return;
        }

        String rejectRequest = "<Request><type>REJECT_PASSWORD_RESET</type><content>" +
                username + "</content></Request>";
        model.sendAdminCommand(rejectRequest);
        notifyUIOfProcessingStatus("Password reset request rejected for user: " + username);
    }

    public void processDirectPasswordChange(String username, String newPassword) {
        if (username == null || username.trim().isEmpty()) {
            notifyUIOfError("Username cannot be empty");
            return;
        }
        if (newPassword == null || newPassword.trim().length() < 4) {
            notifyUIOfError("Password must be at least 4 characters long");
            return;
        }
        model.changeUserPassword(username, newPassword);
    }

    private void notifyUIOfVerificationSuccess(String username) {
        Platform.runLater(() -> {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Password Reset Request");
            confirmAlert.setHeaderText("Client Verification Successful");
            confirmAlert.setContentText("Client '" + username + "' has been verified successfully.\n\n" +
                    "Their security verification code is correct.\n\n" +
                    "Do you want to proceed with changing their password?");

            confirmAlert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                showPasswordChangeDialog(username);
            } else {
                processPasswordResetRejection(username);
            }
        });
    }
    private void showPasswordChangeDialog(String username) {
        TextInputDialog passwordDialog = new TextInputDialog();
        passwordDialog.setTitle("Change Password");
        passwordDialog.setHeaderText("Set New Password for: " + username);
        passwordDialog.setContentText("Enter new password:");

        Optional<String> passwordResult = passwordDialog.showAndWait();
        if (passwordResult.isPresent() && !passwordResult.get().trim().isEmpty()) {
            String newPassword = passwordResult.get().trim();

            if (newPassword.length() < 4) {
                notifyUIOfError("Password must be at least 4 characters long");
                return;
            }

            String verificationCode = getStoredVerificationCode(username);
            if (verificationCode != null) {
                processPasswordResetApproval(username, verificationCode, newPassword);
            } else {
                processDirectPasswordChange(username, newPassword);
            }
        }
    }
    private String getStoredVerificationCode(String username) {
        return null;
    }

    private void notifyUIOfProcessingStatus(String message) {
        Platform.runLater(() -> {
            Alert statusAlert = new Alert(Alert.AlertType.INFORMATION);
            statusAlert.setTitle("Processing");
            statusAlert.setHeaderText("Password Reset Processing");
            statusAlert.setContentText(message);
            statusAlert.show();
            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> statusAlert.close()));
            timeline.play();
        });
    }

    private void notifyUIOfError(String message) {
        Platform.runLater(() -> {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setHeaderText("Operation Failed");
            errorAlert.setContentText(message);
            errorAlert.showAndWait();
        });
    }

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

    private PasswordResetData currentPasswordResetRequest;

    private void setupButtonListeners() {
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

        if (ui.getAddButton() != null) {
            ui.getAddButton().setOnAction(event -> showCreateUserDialog());
        }

        if (ui.getEditButton() != null) {
            ui.getEditButton().setOnAction(event -> editSelectedUser());
        }

        if (ui.getDeleteButton() != null) {
            ui.getDeleteButton().setOnAction(event -> deleteSelectedUser());
        }

        if (ui.getDeleteGroupButton() != null) {
            ui.getDeleteGroupButton().setOnAction(event -> deleteSelectedGroup());
        }

        if (ui.getRefreshButton() != null) {
            ui.getRefreshButton().setOnAction(event -> {
                ui.setShowingActiveUsersOnly(false);
                refreshData();
            });
        }

        if (ui.getActiveButton() != null) {
            ui.getActiveButton().setOnAction(event -> {
                ui.setShowingActiveUsersOnly(true);
                showActiveUsers();
            });
        }

        if (ui.getCommunityButton() != null) {
            ui.getCommunityButton().setOnAction(event -> showCommunityMessages());
        }
    }

    private void setupModelListeners() {
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

        model.setCommunityMessagesUpdateListener(updateType -> {
            Platform.runLater(() -> {
                if ("COMMUNITY_MESSAGES_UPDATED".equals(updateType)) {
                }
            });
        });

        model.setOperationResultListener(result -> {
            Platform.runLater(() -> {
                if (result.contains("ADMIN_PASSWORD_RESET_SUCCESS")) {
                    showAlert("Success", "Password reset completed successfully. Client has been notified with new password.");
                    if (currentPasswordResetRequest != null) {
                        currentPasswordResetRequest = null;
                    }
                } else if (result.contains("ADMIN_PASSWORD_RESET_FAILED")) {
                    if (result.contains("Invalid verification code")) {
                        showAlert("Error", "Verification code does not match user's security code.");
                    } else {
                        showAlert("Error", "Password reset failed: " + result);
                    }
                } else if (result.contains("PASSWORD_RESET_REJECTED")) {
                    showAlert("Success", "Password reset request rejected. Client has been notified.");
                    if (currentPasswordResetRequest != null) {
                        currentPasswordResetRequest = null;
                    }
                } else {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Operation Result");
                    alert.setHeaderText(null);

                    if (result.contains("already exists") || result.contains("Failed") ||
                            result.contains("Invalid") || result.contains("cannot contain")) {
                        alert.setAlertType(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                    }
                    alert.setContentText(result);
                    alert.showAndWait();
                }
            });
        });
        model.setPasswordResetListener(username -> {
            Platform.runLater(() -> {
                ui.showPasswordResetNotification(username);
            });
        });
    }

    private void performSearch(String query) {
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
        model.loadData();
    }

    private void showActiveUsers() {
        model.requestActiveUsersOnly();
    }

    private void showCreateUserDialog() {
        TextInputDialog usernameDialog = new TextInputDialog();
        usernameDialog.setTitle("Create Client");
        usernameDialog.setHeaderText("Create New Client");
        usernameDialog.setContentText("Username:");
        Optional<String> usernameResult = usernameDialog.showAndWait();
        if (usernameResult.isPresent() && !usernameResult.get().trim().isEmpty()) {
            String username = usernameResult.get().trim();
            TextInputDialog passwordDialog = new TextInputDialog();
            passwordDialog.setTitle("Create Client");
            passwordDialog.setHeaderText("Create New Client");
            passwordDialog.setContentText("Password:");
            Optional<String> passwordResult = passwordDialog.showAndWait();
            if (passwordResult.isPresent() && !passwordResult.get().trim().isEmpty()) {
                String password = passwordResult.get().trim();
                model.createNewUser(username, password);
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
        String verificationCode = ui.getVerificationCodeForUser(username);
        if (verificationCode != null) {
            TextInputDialog verificationDialog = new TextInputDialog();
            verificationDialog.setTitle("Verify Password Reset");
            verificationDialog.setHeaderText("Client '" + username + "' requested password reset");
            verificationDialog.setContentText("Enter verification code (sent to user):");
            Optional<String> codeResult = verificationDialog.showAndWait();
            if (codeResult.isPresent() && codeResult.get().equals(verificationCode)) {
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
            TextInputDialog passwordDialog = new TextInputDialog();
            passwordDialog.setTitle("Edit Client");
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
        confirmAlert.setHeaderText("Delete Client");
        confirmAlert.setContentText("Are you sure you want to delete user: " + username + "?");
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            model.deleteUser(username);
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
        Platform.runLater(() -> {
            Dialog<String> communityDialog = new Dialog<>();
            communityDialog.setTitle("Community Chat");
            communityDialog.setHeaderText("Community Chat - Admin View");
            communityDialog.setResizable(true);
            TextArea messagesArea = new TextArea();
            messagesArea.setEditable(false);
            messagesArea.setPrefRowCount(15);
            messagesArea.setPrefColumnCount(50);
            messagesArea.setWrapText(true);
            TextField messageField = new TextField();
            messageField.setPromptText("Type your message here...");
            messageField.setPrefWidth(400);
            Button sendButton = new Button("Send");
            Button refreshButton = new Button("Refresh");
            VBox content = new VBox(10);
            HBox messageInputBox = new HBox(10, messageField, sendButton, refreshButton);
            content.getChildren().addAll(
                    new Label("Community Messages:"),
                    messagesArea,
                    new Label("Send Message:"),
                    messageInputBox
            );
            communityDialog.getDialogPane().setContent(content);
            communityDialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
            loadCommunityMessages(messagesArea);
            sendButton.setOnAction(event -> {
                String messageText = messageField.getText().trim();
                if (!messageText.isEmpty()) {
                    sendCommunityMessage(messageText);
                    messageField.clear();
                    Timeline refreshTimeline = new Timeline(new KeyFrame(Duration.millis(500),
                            e -> loadCommunityMessages(messagesArea)));
                    refreshTimeline.play();
                }
            });
            messageField.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    sendButton.fire();
                }
            });
            refreshButton.setOnAction(event -> {
                model.requestCommunityMessages();
                loadCommunityMessages(messagesArea);
            });
            Timeline autoRefresh = new Timeline(new KeyFrame(Duration.seconds(3),
                    event -> loadCommunityMessages(messagesArea)));
            autoRefresh.setCycleCount(Timeline.INDEFINITE);
            autoRefresh.play();
            communityDialog.setOnCloseRequest(event -> autoRefresh.stop());
            communityDialog.showAndWait();
        });
    }

    private void loadCommunityMessages(TextArea messagesArea) {
        if (model.getCommunityMessages() != null && !model.getCommunityMessages().isEmpty()) {
            Platform.runLater(() -> {
                StringBuilder messageText = new StringBuilder();
                for (String message : model.getCommunityMessages()) {
                    messageText.append(message).append("\n");
                }
                messagesArea.setText(messageText.toString());
                messagesArea.setScrollTop(Double.MAX_VALUE);
            });
        }
    }

    private void sendCommunityMessage(String message) {
        try {
            if (model.isConnected() && model.getAdminUsername() != null) {
                String request = "<Request><type>SEND_COMMUNITY_MESSAGE</type><content>" +
                        model.getAdminUsername() + "![]!" + message + "</content></Request>";
                model.sendAdminCommand(request);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    //BUTTON LISTENERS
    private void setupPasswordResetHandling() {
        model.setVerificationCodeValidListener(this::handleVerificationCodeValidRequest);
    }
}