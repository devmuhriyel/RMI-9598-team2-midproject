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
        if (!isValidPassword(newPassword)) {
            notifyUIOfError("Password must be at least 4 characters long and contain only letters and numbers");
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
        if (!isValidPassword(newPassword)) {
            notifyUIOfError("Password must be at least 4 characters long and contain only letters and numbers");
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
        passwordDialog.setContentText("Enter new password (letters and numbers only):");

        Optional<String> passwordResult = passwordDialog.showAndWait();
        if (passwordResult.isPresent() && !passwordResult.get().trim().isEmpty()) {
            String newPassword = passwordResult.get().trim();

            if (!isValidPassword(newPassword)) {
                notifyUIOfError("Password must be at least 4 characters long and contain only letters and numbers");
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
            ui.getAddButton().setOnAction(event -> showAddUserDialog());
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
    private void showAddUserDialog() {
        Dialog<ButtonType> addUserDialog = new Dialog<>();
        addUserDialog.setTitle("Add New User");
        addUserDialog.setHeaderText("Create a New User Account");

        // Create input fields
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username (letters and numbers only, min 3 characters)");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password (letters and numbers only, min 4 characters)");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm Password");

        // Create labels for validation feedback
        Label usernameValidationLabel = new Label();
        usernameValidationLabel.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");

        Label passwordValidationLabel = new Label();
        passwordValidationLabel.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");

        Label confirmPasswordValidationLabel = new Label();
        confirmPasswordValidationLabel.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");

        // Create layout
        VBox content = new VBox(10);
        content.getChildren().addAll(
                new Label("Username:"),
                usernameField,
                usernameValidationLabel,
                new Label("Password:"),
                passwordField,
                passwordValidationLabel,
                new Label("Confirm Password:"),
                confirmPasswordField,
                confirmPasswordValidationLabel
        );

        addUserDialog.getDialogPane().setContent(content);
        addUserDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Real-time validation for username
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                usernameValidationLabel.setText("");
            } else if (newValue.length() < 3) {
                usernameValidationLabel.setText("Username must be at least 3 characters");
            } else if (!newValue.matches("^[a-zA-Z0-9]+$")) {
                usernameValidationLabel.setText("Only letters and numbers allowed (no symbols or spaces)");
            } else if (isUsernameDuplicate(newValue)) {
                usernameValidationLabel.setText("Username already exists");
            } else {
                usernameValidationLabel.setText("");
                usernameValidationLabel.setStyle("-fx-text-fill: green; -fx-font-size: 10px;");
                usernameValidationLabel.setText("✓ Valid username");
            }
        });

        // Real-time validation for password
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                passwordValidationLabel.setText("");
            } else if (newValue.length() < 4) {
                passwordValidationLabel.setText("Password must be at least 4 characters");
            } else if (!newValue.matches("^[a-zA-Z0-9]+$")) {
                passwordValidationLabel.setText("Only letters and numbers allowed (no symbols or spaces)");
            } else {
                passwordValidationLabel.setText("");
                passwordValidationLabel.setStyle("-fx-text-fill: green; -fx-font-size: 10px;");
                passwordValidationLabel.setText("✓ Valid password");
            }

            // Also check confirm password match
            if (!confirmPasswordField.getText().isEmpty()) {
                if (!newValue.equals(confirmPasswordField.getText())) {
                    confirmPasswordValidationLabel.setText("Passwords do not match");
                } else {
                    confirmPasswordValidationLabel.setText("");
                    confirmPasswordValidationLabel.setStyle("-fx-text-fill: green; -fx-font-size: 10px;");
                    confirmPasswordValidationLabel.setText("✓ Passwords match");
                }
            }
        });

        // Real-time validation for confirm password
        confirmPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                confirmPasswordValidationLabel.setText("");
            } else if (!newValue.equals(passwordField.getText())) {
                confirmPasswordValidationLabel.setText("Passwords do not match");
            } else {
                confirmPasswordValidationLabel.setText("");
                confirmPasswordValidationLabel.setStyle("-fx-text-fill: green; -fx-font-size: 10px;");
                confirmPasswordValidationLabel.setText("✓ Passwords match");
            }
        });

        // Handle dialog result
        Optional<ButtonType> result = addUserDialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            String confirmPassword = confirmPasswordField.getText().trim();

            // Final validation before submission
            if (!isValidUsername(username)) {
                notifyUIOfError("Invalid username format.\n\n" +
                        "Requirements:\n" +
                        "- At least 3 characters\n" +
                        "- Only letters and numbers (no symbols or spaces)");
                return;
            }

            if (isUsernameDuplicate(username)) {
                notifyUIOfError("Username '" + username + "' already exists.\n\nPlease choose a different username.");
                return;
            }

            if (!isValidPassword(password)) {
                notifyUIOfError("Invalid password format.\n\n" +
                        "Requirements:\n" +
                        "- At least 4 characters\n" +
                        "- Only letters and numbers (no symbols or spaces)");
                return;
            }

            if (!password.equals(confirmPassword)) {
                notifyUIOfError("Passwords do not match.\n\nPlease make sure both passwords are identical.");
                return;
            }

            // All validations passed - create the user
            try {
                model.createNewUser(username, password);
                // Success notification will be handled by the callback
            } catch (RemoteException e) {
                notifyUIOfError("Failed to create user: " + e.getMessage());
            }
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
            if (model.getAdminUsername() == null || model.getAdminUsername().isEmpty()) {
                System.err.println("✗ Cannot refresh data: adminUsername not set!");
                return;
            }

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

    // VALIDATION METHODS
    private boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        if (username.length() < 3) {
            return false;
        }
        return username.matches("^[a-zA-Z0-9]+$");
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        if (password.length() < 4) {
            return false;
        }
        return password.matches("^[a-zA-Z0-9]+$");
    }

    private boolean isUsernameDuplicate(String username) {
        List<Map<String, String>> allUsers = ui.getOriginalUsers();
        if (allUsers == null) {
            return false;
        }

        for (Map<String, String> user : allUsers) {
            if (user != null && user.get("username") != null) {
                if (user.get("username").equalsIgnoreCase(username)) {
                    return true;
                }
            }
        }
        return false;
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
        passwordDialog.setHeaderText("Change Password for: " + username);
        passwordDialog.setContentText("New Password (letters and numbers only, min 4 characters):");

        Optional<String> result = passwordDialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String newPassword = result.get().trim();

            // Validate password format
            if (!isValidPassword(newPassword)) {
                notifyUIOfError("Invalid password format.\n\n" +
                        "Requirements:\n" +
                        "- At least 4 characters\n" +
                        "- Only letters and numbers (no symbols or spaces)");
                return;
            }

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
            try {
                String adminUsername = model.getAdminUsername();
                model.joinCommunityChat(adminUsername);
            } catch (RemoteException e) {
                notifyUIOfError("Failed to join community chat: " + e.getMessage());
            }
            loadCommunityMessagesForAdmin(messagesArea);
            sendButton.setOnAction(event -> {
                String messageText = messageField.getText().trim();
                if (!messageText.isEmpty()) {
                    try {
                        model.sendCommunityMessage(messageText);
                        messageField.clear();
                        Timeline refreshTimeline = new Timeline(new KeyFrame(Duration.millis(500),
                                e -> loadCommunityMessagesForAdmin(messagesArea)));
                        refreshTimeline.play();
                    } catch (RemoteException e) {
                        notifyUIOfError("Failed to send message: " + e.getMessage());
                    }
                }
            });
            messageField.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    sendButton.fire();
                }
            });
            refreshButton.setOnAction(event -> {
                loadCommunityMessagesForAdmin(messagesArea);
            });
            Timeline autoRefresh = new Timeline(new KeyFrame(Duration.seconds(3),
                    event -> loadCommunityMessagesForAdmin(messagesArea)));
            autoRefresh.setCycleCount(Timeline.INDEFINITE);
            autoRefresh.play();
            communityDialog.setOnCloseRequest(event -> {
                autoRefresh.stop();
                try {
                    String adminUsername = model.getAdminUsername();
                    // You'll need to add this method to the model
                    model.leaveCommunityChat(adminUsername);
                } catch (RemoteException e) {
                    System.err.println("Failed to leave community chat: " + e.getMessage());
                }
            });

            communityDialog.showAndWait();
        });
    }
    private void loadCommunityMessagesForAdmin(TextArea messagesArea) {
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
        showAlert("Success", "User created successfully!");
        // Refresh the user list to show the new user
        refreshData();
    }

    public void handleFailedSignUp() {
        showAlert("Failed", "Failed to create user. Please check:\n" +
                "- Username must be at least 3 characters\n" +
                "- Password must be at least 4 characters\n" +
                "- Only letters and numbers allowed\n" +
                "- Username might already exist");
    }

    public void handleSuccessfulLogin() {
        refreshData();
    }

    public void handleFailedLogin() {
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
    }

    public void handleGetChatHistoryOfGroup() {
    }

    public void handleGetChatHistoryOfCommunity() {
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