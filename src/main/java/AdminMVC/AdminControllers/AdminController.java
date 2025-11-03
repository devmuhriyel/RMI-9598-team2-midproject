package AdminMVC.AdminControllers;

import AdminMVC.AdminModel.UnifiedAdminModel;
import AdminMVC.AdminView.Admin.AdminPageJFXController;
import Common.Objects.ClientObjects.Client;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.rmi.RemoteException;
import java.util.*;

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
        try {
            model.verifyAndChangePassword(username, verificationCode, newPassword);
        } catch (RemoteException e) {
            notifyUIOfError("Failed to process password reset: " + e.getMessage());
        }
    }

    public void processPasswordResetRejection(String username) {
        if (username == null || username.trim().isEmpty()) {
            notifyUIOfError("Username cannot be empty");
            return;
        }
        try {
            model.rejectPasswordReset(username);
        } catch (RemoteException e) {
            notifyUIOfError("Failed to reject password reset: " + e.getMessage());
        }
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
        try {
            model.changeUserPassword(username, newPassword);
        } catch (RemoteException e) {
            notifyUIOfError("Failed to change password: " + e.getMessage());
        }
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
            });
        });

        model.setPasswordResetListener(username -> {
            Platform.runLater(() -> {
                ui.showPasswordResetNotification(username);
            });
        });
    }

    private void performSearch(String query) {
        try {
            model.searchUsers(query);
            model.searchGroups(query);
        } catch (RemoteException e) {
            notifyUIOfError("Search failed: " + e.getMessage());
        }
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
        try {
            model.getAllClientsFromServer();
            model.getAllGroupsFromServer();
        } catch (RemoteException e) {
            notifyUIOfError("Failed to refresh data: " + e.getMessage());
        }
    }

    private void showActiveUsers() {
        try {
            model.getActiveClientsOnly();
        } catch (RemoteException e) {
            notifyUIOfError("Failed to get active users: " + e.getMessage());
        }
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
                try {
                    model.createNewUser(username, password);
                } catch (RemoteException e) {
                    notifyUIOfError("Failed to create user: " + e.getMessage());
                }
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

        TextInputDialog passwordDialog = new TextInputDialog();
        passwordDialog.setTitle("Edit Client");
        passwordDialog.setHeaderText("Change Password for " + username);
        passwordDialog.setContentText("New Password:");
        Optional<String> result = passwordDialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String newPassword = result.get().trim();
            try {
                model.changeUserPassword(username, newPassword);
            } catch (RemoteException e) {
                notifyUIOfError("Failed to change password: " + e.getMessage());
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
            try {
                model.deleteUser(username);
            } catch (RemoteException e) {
                notifyUIOfError("Failed to delete user: " + e.getMessage());
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
            try {
                model.deleteGroup(groupName, owner);
            } catch (RemoteException e) {
                notifyUIOfError("Failed to delete group: " + e.getMessage());
            }
        }
    }

    private void showCommunityMessages() {
        try {
            model.getCommunityMessages();
        } catch (RemoteException e) {
            notifyUIOfError("Failed to load community messages: " + e.getMessage());
            return;
        }

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
                    try {
                        model.sendCommunityMessage(messageText);
                    } catch (RemoteException e) {
                        notifyUIOfError("Failed to send message: " + e.getMessage());
                    }
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
                try {
                    model.getCommunityMessages();
                } catch (RemoteException e) {
                    notifyUIOfError("Failed to refresh messages: " + e.getMessage());
                }
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
        if (model.getCommunityMessagesList() != null && !model.getCommunityMessagesList().isEmpty()) {
            Platform.runLater(() -> {
                StringBuilder messageText = new StringBuilder();
                for (String message : model.getCommunityMessagesList()) {
                    messageText.append(message).append("\n");
                }
                messagesArea.setText(messageText.toString());
                messagesArea.setScrollTop(Double.MAX_VALUE);
            });
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    //CALLBACK HANDLERS
    public void handleSuccessfulSignUp() {
        showAlert("Success", "Admin account created successfully");
    }

    public void handleFailedSignUp() {
        showAlert("Failed", "Failed to create admin account");
    }

    public void handleSuccessfulLogin() {
        System.out.println("✓ Controller: Handling successful login");
        refreshData();
    }

    public void handleFailedLogin() {
        System.out.println("✗ Controller: Handling failed login");
        showAlert("Login Failed", "Invalid username or password");
    }

    public void handleSuccessfulLogout() {
        showAlert("Success", "Logged out successfully");
    }

    public void handleSuccessfulPasswordChange() {
        showAlert("Success", "Password changed successfully");
        refreshData();
    }

    public void handleFailedPasswordChange() {
        showAlert("Failed", "Failed to change password");
    }

    public void handleSuccessfulCodeVerification() {
        showAlert("Success", "Verification code validated");
    }

    public void handleFailedCodeVerification() {
        showAlert("Failed", "Invalid verification code");
    }

    public void handleSearchUserResults() {
        refreshData();
    }

    public void handleSearchGroupResults() {
        refreshData();
    }

    public void handleDeleteUser() {
        showAlert("Success", "User deleted successfully");
        refreshData();
    }

    public void handleDeleteGroup() {
        showAlert("Success", "Group deleted successfully");
        refreshData();
    }

    public void handleGetChatHistoryOfUser() {
        // Handle chat history display
    }

    public void handleGetChatHistoryOfGroup() {
        // Handle group chat history display
    }

    public void handleGetChatHistoryOfCommunity() {
        // Handle community chat history display
    }

    public void handleCommunityMessageSent() {
        System.out.println("Community message sent");
    }

    public void handleCommunityMessageReceived() {
        System.out.println("Community message received");
    }

    private void setupPasswordResetHandling() {
        model.setVerificationCodeValidListener(this::handleVerificationCodeValidRequest);
    }
}