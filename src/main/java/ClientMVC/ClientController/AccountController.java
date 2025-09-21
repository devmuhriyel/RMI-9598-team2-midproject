package ClientMVC.ClientController;

import ClientMVC.ClientModel.AccountModel;
import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Account.AccountView;
import ClientMVC.ClientView.Account.AccountJFXController;
import ClientMVC.ClientModel.AccountModel;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.ClientView.Login.LoginView;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.IOException;

public class AccountController {
    private final UnifiedClientModel model;
    private final AccountView view;
    private final AccountJFXController accountJFXController;
    private BufferedWriter serverWriter;

    //This for the Friend Function
    public AccountController(UnifiedClientModel model, AccountView view, AccountJFXController accountJFXController) {
        this.view = view;
        this.model = model;
        this.accountJFXController = accountJFXController;

        initialize();
    }


    private void initialize() {
        System.out.println("AccountController initialize() called");

        // Set the current username in the display
        accountJFXController.updateDisplayedUsername(model.getUsername());

        // Handle back button click
        accountJFXController.setBackButtonListener(() -> {
            view.getStage().close();
            HomeView homeView = new HomeView();
            Stage homeStage = new Stage();
            homeView.start(homeStage);
            new HomeController(model, homeView, homeView.getFXController());
        });

        // Handle logout
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

        // Set up account update handling
        setupAccountUpdateHandling();

        // Connect the button actions - PROPER MVC WAY
        System.out.println("Setting up username and password change listeners");
        accountJFXController.setUsernameChangeListener(this::handleUsernameChange);
        accountJFXController.setPasswordChangeListener(this::handlePasswordChange);

        System.out.println("AccountController initialization complete");

        // NOW show the profile view after listeners are set up
        try {
            accountJFXController.showProfile();
            System.out.println("Profile view loaded after listeners were set");
        } catch (Exception e) {
            System.err.println("Error loading profile view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupAccountUpdateHandling() {
        // Listen for account update responses from server
        model.setAccountUpdateListener(response -> {
            System.out.println("Raw server response: " + response);

            // Parse the XML response to get the status
            String status = extractStatusFromResponse(response);
            System.out.println("Extracted status: " + status);

            switch (status) {
                case "USERNAME_CHANGED":
                    handleUsernameChangeSuccess(response);
                    break;
                case "USERNAME_CHANGE_FAILED":
                    accountJFXController.showUsernameUpdateFailed();
                    // Close the form after showing error
                    accountJFXController.hideChangeUsername();
                    break;
                case "USERNAME_EXISTS":
                    accountJFXController.showUsernameAlreadyExists();
                    // Don't close form so user can try again
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
                    // Don't close form so user can try again
                    break;
                default:
                    System.out.println("Unknown status received: " + status);
                    break;
            }
        });
    }

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

            // Fallback: look for common status patterns in the response
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

    private void handleUsernameChangeSuccess(String xmlResponse) {
        try {
            // Extract new username from server response if needed
            String newUsername = extractNewUsernameFromResponse(xmlResponse);
            if (newUsername != null && !newUsername.isEmpty()) {
                // Update model's username
                model.setUsername(newUsername);
                // Update the display
                accountJFXController.updateDisplayedUsername(newUsername);
            }

            accountJFXController.showUsernameUpdated();
            accountJFXController.clearUsernameField();
            accountJFXController.hideChangeUsername();
        } catch (Exception e) {
            System.err.println("Error handling username change success: " + e.getMessage());
            accountJFXController.showUsernameUpdated();
            accountJFXController.hideChangeUsername();
        }
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

    // Method to handle username change request
    public void handleUsernameChange() {
        String newUsername = accountJFXController.getNewUsername();
        String currentUsername = model.getUsername();

        // Validation
        if (newUsername == null || newUsername.trim().isEmpty()) {
            accountJFXController.showUsernameEmpty();
            return;
        }

        // Check if new username is the same as current username
        if (newUsername.trim().equals(currentUsername)) {
            accountJFXController.showUsernameSameAsCurrent();
            return;
        }

        // Send request to server
        model.changeUsername(newUsername.trim());
    }

    // Method to handle password change request
    public void handlePasswordChange() {
        System.out.println("=== AccountController.handlePasswordChange() called ===");
        String oldPassword = accountJFXController.getOldPassword();
        String newPassword = accountJFXController.getNewPassword();

        // Server-side validation could be added here if needed
        // But the view already did basic validation

        System.out.println("Sending password change request to server");
        model.changePassword(oldPassword, newPassword);
    }

    private void sendMessage(String msg) throws IOException {
        serverWriter.write(msg);
        serverWriter.newLine();
        serverWriter.flush();
    }
}


//    public void listRequests(String user) throws IOException {
//        String msg = "<Request type=\"LIST_REQUESTS\"><content>" + user + "</content></Request>";
//        sendMessage(msg);
//    }
//
//    public void acceptRequest(String user, String friend) throws IOException {
//        String msg = "<Request type=\"ACCEPT_REQUEST\"><content>" + user + "waw" + friend + "</content></Request>";
//        sendMessage(msg);
//    }
//
//    public void declineRequest(String user, String friend) throws IOException {
//        String msg = "<Request type=\"DECLINE_REQUEST\"><content>" + user + "waw" + friend + "</content></Request>";
//        sendMessage(msg);
//    }
//
//    public void updateRequests(java.util.List<String> requests) {
//        model.setFriendRequests(requests);
//    }
