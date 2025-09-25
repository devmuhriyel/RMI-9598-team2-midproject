package ClientMVC.ClientController;

import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Account.AccountView;
import ClientMVC.ClientView.Account.AccountJFXController;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.ClientView.Login.LoginView;
import javafx.application.Platform;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class AccountController {
    private final UnifiedClientModel model;
    private final AccountView view;
    private final AccountJFXController accountJFXController;

    public AccountController(UnifiedClientModel model, AccountView view, AccountJFXController accountJFXController) {
        this.view = view;
        this.model = model;
        this.accountJFXController = accountJFXController;

        initialize();
    }

    private void initialize() {
        accountJFXController.updateDisplayedUsername(model.getUsername());
        setupFriendRequestHandling();
        accountJFXController.setSentRequestButtonListener(() -> {
            model.getFriendRequests();
        });
        accountJFXController.setAcceptRequestListener(friendName -> {
            model.acceptFriendRequest(friendName);
        });
        accountJFXController.setDeclineRequestListener(friendName -> {
            model.declineFriendRequest(friendName);
        });

        accountJFXController.setBackButtonListener(() -> {
            view.getStage().close();
            HomeView homeView = new HomeView();
            Stage homeStage = new Stage();
            homeView.start(homeStage);
            new HomeController(model, homeView, homeView.getFXController());
        });

        accountJFXController.setLogoutAccountButtonListener(() -> {
            view.getStage().close();
            LoginView loginView = new LoginView();
            Stage loginStage = new Stage();
            try {
                loginView.start(loginStage);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            new LoginController(loginView, model, loginView.getFXController());
        });

        setupAccountUpdateHandling();
        accountJFXController.setUsernameChangeListener(this::handleUsernameChange);
        accountJFXController.setPasswordChangeListener(this::handlePasswordChange);

        try {
            accountJFXController.showProfile();
        } catch (Exception e) {
            System.err.println("Error loading profile view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupAccountUpdateHandling() {
        model.setAccountUpdateListener(response -> {
            String status = extractStatusFromResponse(response);

            Platform.runLater(() -> {
                switch (status) {
                    case "USERNAME_CHANGED":
                        handleUsernameChangeSuccess(response);
                        break;
                    case "USERNAME_CHANGE_FAILED":
                        accountJFXController.showUsernameUpdateFailed();
                        accountJFXController.hideChangeUsername();
                        break;
                    case "USERNAME_EXISTS":
                        accountJFXController.showUsernameAlreadyExists();
                        break;
                    case "PASSWORD_CHANGED":
                        accountJFXController.showPasswordChanged();
                        accountJFXController.clearPasswordFields();
                        accountJFXController.hideChangePassword();
                        break;
                    case "PASSWORD_CHANGE_FAILED":
                        accountJFXController.showPasswordUpdateFailed();
                        accountJFXController.hideChangePassword();
                        break;
                    case "INVALID_OLD_PASSWORD":
                        accountJFXController.showInvalidOldPassword();
                        break;
                    default:
                        System.err.println("Unknown account update status: " + status);
                        break;
                }
            });
        });
    }

    private String extractStatusFromResponse(String xmlResponse) {
        try {
            if (xmlResponse.contains("status=\"")) {
                int startIndex = xmlResponse.indexOf("status=\"") + 8;
                int endIndex = xmlResponse.indexOf("\"", startIndex);
                if (endIndex > startIndex) {
                    return xmlResponse.substring(startIndex, endIndex);
                }
            }
            if (xmlResponse.contains("USERNAME_CHANGED")) return "USERNAME_CHANGED";
            if (xmlResponse.contains("USERNAME_CHANGE_FAILED")) return "USERNAME_CHANGE_FAILED";
            if (xmlResponse.contains("USERNAME_EXISTS")) return "USERNAME_EXISTS";
            if (xmlResponse.contains("PASSWORD_CHANGED")) return "PASSWORD_CHANGED";
            if (xmlResponse.contains("PASSWORD_CHANGE_FAILED")) return "PASSWORD_CHANGE_FAILED";
            if (xmlResponse.contains("INVALID_OLD_PASSWORD")) return "INVALID_OLD_PASSWORD";

        } catch (Exception e) {
            System.err.println("Error extracting status from response: " + e.getMessage());
        }
        return "UNKNOWN";
    }
    public void handleUsernameChange() {
        String newUsername = accountJFXController.getNewUsername();
        String currentUsername = model.getUsername();

        // Input validation
        if (newUsername == null || newUsername.trim().isEmpty()) {
            accountJFXController.showUsernameEmpty();
            return;
        }

        newUsername = newUsername.trim();

        if (newUsername.equals(currentUsername)) {
            accountJFXController.showUsernameSameAsCurrent();
            return;
        }

        // Additional validation rules (if needed)
        if (newUsername.length() < 3) {
            accountJFXController.showUsernameValidationError("Username must be at least 3 characters long");
            return;
        }

        if (newUsername.length() > 20) {
            accountJFXController.showUsernameValidationError("Username must be no more than 20 characters long");
            return;
        }

        if (!newUsername.matches("^[a-zA-Z0-9_]+$")) {
            accountJFXController.showUsernameValidationError("Username can only contain letters, numbers, and underscores");
            return;
        }

        // Show loading/processing state
        accountJFXController.showUsernameProcessing();

        // Send the change request to the server
        try {
            model.changeUsername(newUsername);
            System.out.println("Username change request sent: " + currentUsername + " -> " + newUsername);
        } catch (Exception e) {
            System.err.println("Error sending username change request: " + e.getMessage());
            accountJFXController.showUsernameUpdateFailed();
            accountJFXController.hideChangeUsername();
        }
    }

    private void handleUsernameChangeSuccess(String xmlResponse) {
        try {
            String newUsername = extractNewUsernameFromResponse(xmlResponse);
            if (newUsername != null && !newUsername.isEmpty()) {
                String oldUsername = model.getUsername();

                // Update the model with new username
                model.setUsername(newUsername);

                // Update the UI immediately
                accountJFXController.updateDisplayedUsername(newUsername);

                // Handle local file updates through the model
                // (The model's handleUsernameChangeWithData method will handle this)

                // Show success message
                accountJFXController.showUsernameUpdated();
                accountJFXController.clearUsernameField();
                accountJFXController.hideChangeUsername();

                // Log the successful change
                System.out.println("Username change completed successfully: " + oldUsername + " -> " + newUsername);

                // Optional: Show additional success feedback
                showSuccessAlert("Username Changed",
                        "Your username has been successfully changed to: " + newUsername);

            } else {
                // Fallback if we can't extract the new username from response
                accountJFXController.showUsernameUpdated();
                accountJFXController.hideChangeUsername();
                System.out.println("Username change completed (username extraction failed)");
            }
        } catch (Exception e) {
            System.err.println("Error handling username change success: " + e.getMessage());
            e.printStackTrace();

            // Still show success message even if there were issues
            accountJFXController.showUsernameUpdated();
            accountJFXController.hideChangeUsername();
        }
    }
    private void setupFriendRequestHandling() {
        accountJFXController.setSentRequestButtonListener(() -> {
            System.out.println("Requesting sent friend requests...");
            model.getSentRequests();
        });

        accountJFXController.setReceivedRequestsListener((Void) -> {
            System.out.println("Requesting received friend requests...");
            model.getReceivedRequests();
        });

        accountJFXController.setAcceptRequestListener(friendName -> {
            System.out.println("Accepting friend request from: " + friendName);
            model.acceptFriendRequestNew(friendName);
        });

        accountJFXController.setDeclineRequestListener(friendName -> {
            System.out.println("Declining friend request from: " + friendName);
            model.declineFriendRequestNew(friendName);
        });

        model.setFriendRequestListener(response -> {
            Platform.runLater(() -> {
                switch (response) {
                    case "REQUEST_ACCEPTED":
                        System.out.println("Friend request was accepted");
                        showSuccessAlert("Friend Request", "Friend request accepted successfully!");
                        model.getReceivedRequests();
                        break;

                    case "REQUEST_DECLINED":
                        System.out.println("Friend request was declined");
                        showSuccessAlert("Friend Request", "Friend request declined.");
                        model.getReceivedRequests();
                        break;

                    case "FRIENDS_UPDATED":
                        System.out.println("Friends list updated");
                        break;

                    default:
                        if (response.startsWith("SENT_REQUESTS:")) {
                            String requestData = response.substring("SENT_REQUESTS:".length());
                            handleSentRequests(requestData);
                        } else if (response.startsWith("RECEIVED_REQUESTS:")) {
                            String requestData = response.substring("RECEIVED_REQUESTS:".length());
                            handleReceivedRequests(requestData);
                        }
                        break;
                }
            });
        });
        Platform.runLater(() -> {
            model.getReceivedRequests();
        });
    }
    private void handleSentRequests(String requestData) {
        System.out.println("Handling sent requests: " + requestData);

        if (requestData != null && !requestData.isEmpty()) {
            String[] requests = requestData.split(",");
            List<String> requestList = new ArrayList<>();
            for (String request : requests) {
                if (!request.trim().isEmpty()) {
                    requestList.add(request.trim());
                }
            }
            accountJFXController.renderRequestList(requestList);
        } else {
            accountJFXController.renderRequestList(new ArrayList<>());
        }
    }
    private void handleReceivedRequests(String requestData) {
        System.out.println("Handling received requests: " + requestData);
        if (requestData != null && !requestData.isEmpty()) {
            String[] requests = requestData.split(",");
            List<String> requestList = new ArrayList<>();
            for (String request : requests) {
                if (!request.trim().isEmpty()) {
                    requestList.add(request.trim());
                }
            }
            accountJFXController.renderRequestList(requestList);
        } else {
            accountJFXController.renderRequestList(new ArrayList<>());
        }
    }

    private void showSuccessAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setPrefSize(300, 120);
        alert.showAndWait();
    }

    private String extractNewUsernameFromResponse(String xmlResponse) {
        try {
            if (xmlResponse != null && xmlResponse.contains("Username successfully changed to ")) {
                String[] parts = xmlResponse.split("Username successfully changed to ");
                if (parts.length > 1) {
                    return parts[1].replace("</body></Response>", "").trim();
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting username from response: " + e.getMessage());
        }
        return null;
    }

    public void handlePasswordChange() {
        String oldPassword = accountJFXController.getOldPassword();
        String newPassword = accountJFXController.getNewPassword();
        model.changePassword(oldPassword, newPassword);
    }
}