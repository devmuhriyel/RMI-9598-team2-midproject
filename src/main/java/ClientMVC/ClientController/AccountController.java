package ClientMVC.ClientController;

import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Account.AccountView;
import ClientMVC.ClientView.Account.AccountJFXController;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.ClientView.Login.LoginView;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Account Management functionality.
 * Handles the communication between AccountView and UnifiedClientModel.
 * Manages user account operations like username/password changes.
 */
public class AccountController {
    private final UnifiedClientModel model;
    private final AccountView view;
    private final AccountJFXController accountJFXController;

    /**
     * Constructor for AccountController.
     * @param model The unified client model for server communication
     * @param view The account view
     * @param accountJFXController The FXML controller for the account view
     */
    public AccountController(UnifiedClientModel model, AccountView view, AccountJFXController accountJFXController) {
        this.view = view;
        this.model = model;
        this.accountJFXController = accountJFXController;

        initialize();
    }

    /**
     * Initializes the controller by setting up listeners and displaying current user info.
     */
    private void initialize() {
        // Set the current username in the display
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

        // Handle back button - return to home view
        accountJFXController.setBackButtonListener(() -> {
            view.getStage().close();
            HomeView homeView = new HomeView();
            Stage homeStage = new Stage();
            homeView.start(homeStage);
            new HomeController(model, homeView, homeView.getFXController());
        });

        // Handle logout - return to login view
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

        // Set up server response handling
        setupAccountUpdateHandling();

        // Connect UI actions to controller methods
        accountJFXController.setUsernameChangeListener(this::handleUsernameChange);
        accountJFXController.setPasswordChangeListener(this::handlePasswordChange);

        // Show profile view by default
        try {
            accountJFXController.showProfile();
        } catch (Exception e) {
            System.err.println("Error loading profile view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sets up listeners for server responses to account update requests.
     */
    private void setupAccountUpdateHandling() {
        model.setAccountUpdateListener(response -> {
            String status = extractStatusFromResponse(response);

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
                    // Keep form open so user can try again
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
                    // Keep form open so user can correct the old password
                    break;
                default:
                    System.err.println("Unknown account update status: " + status);
                    break;
            }
        });
    }

    /**
     * Extracts the status from server XML response.
     * @param xmlResponse The XML response from server
     * @return The status string or "UNKNOWN" if not found
     */
    private String extractStatusFromResponse(String xmlResponse) {
        try {
            // Look for status attribute in XML
            if (xmlResponse.contains("status=\"")) {
                int startIndex = xmlResponse.indexOf("status=\"") + 8;
                int endIndex = xmlResponse.indexOf("\"", startIndex);
                if (endIndex > startIndex) {
                    return xmlResponse.substring(startIndex, endIndex);
                }
            }

            // Fallback: look for status patterns in the response body
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

    /**
     * Handles successful username change response from server.
     * @param xmlResponse The server response containing the new username
     */
    private void handleUsernameChangeSuccess(String xmlResponse) {
        try {
            // Extract new username from server response
            String newUsername = extractNewUsernameFromResponse(xmlResponse);
            if (newUsername != null && !newUsername.isEmpty()) {
                // Update model's username
                model.setUsername(newUsername);
                // Update the display
                accountJFXController.updateDisplayedUsername(newUsername);

                // The account XML should already be updated by UnifiedClientModel
                System.out.println("Username successfully changed to: " + newUsername);
            }

            // Show success message and clean up UI
            accountJFXController.showUsernameUpdated();
            accountJFXController.clearUsernameField();
            accountJFXController.hideChangeUsername();
        } catch (Exception e) {
            System.err.println("Error handling username change success: " + e.getMessage());
            // Still show success and hide form even if extraction fails
            accountJFXController.showUsernameUpdated();
            accountJFXController.hideChangeUsername();
        }
    }
    private void setupFriendRequestHandling() {
        model.setFriendRequestListener(response -> {
            Platform.runLater(() -> {
                switch (response) {
                    case "REQUEST_ACCEPTED":
                        System.out.println("Friend request was accepted");
                        // Optionally refresh data or show message
                        break;
                    case "REQUEST_DECLINED":
                        System.out.println("Friend request was declined");
                        break;
                    default:
                        if (response.startsWith("REQUESTS_RECEIVED:")) {
                            String requestData = response.substring("REQUESTS_RECEIVED:".length());
                            handleIncomingFriendRequests(requestData);
                        }
                        break;
                }
            });
        });
    }
    private void handleIncomingFriendRequests(String requestData) {
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

    /**
     * Extracts the new username from server response.
     * @param xmlResponse The XML response from server
     * @return The new username or null if not found
     */
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

    /**
     * Handles username change request from UI.
     * Validates input and sends request to server.
     */
    public void handleUsernameChange() {
        String newUsername = accountJFXController.getNewUsername();
        String currentUsername = model.getUsername();

        // Validate input is not empty
        if (newUsername == null || newUsername.trim().isEmpty()) {
            accountJFXController.showUsernameEmpty();
            return;
        }

        // Validate new username is different from current
        if (newUsername.trim().equals(currentUsername)) {
            accountJFXController.showUsernameSameAsCurrent();
            return;
        }

        // Send request to server
        model.changeUsername(newUsername.trim());
    }

    /**
     * Handles password change request from UI.
     * The validation is already done in the view, so we just send the request.
     */
    public void handlePasswordChange() {
        String oldPassword = accountJFXController.getOldPassword();
        String newPassword = accountJFXController.getNewPassword();

        // Send request to server
        model.changePassword(oldPassword, newPassword);
    }
}