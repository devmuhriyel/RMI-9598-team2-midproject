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
            String[] parts = response.split("!\\[]!", 2);
            String status = parts[0];

            System.out.println("Received account update response: " + status);

            switch (status) {
                case "USERNAME_CHANGED":
                    handleUsernameChangeSuccess(parts.length > 1 ? parts[1] : "");
                    break;
                case "USERNAME_CHANGE_FAILED":
                    accountJFXController.showUsernameUpdateFailed();
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
                    break;
                case "INVALID_OLD_PASSWORD":
                    accountJFXController.showInvalidOldPassword();
                    break;
            }
        });
    }

    private void handleUsernameChangeSuccess(String xmlResponse) {
        try {
            // Extract new username from server response if needed
            String newUsername = extractNewUsernameFromResponse(xmlResponse);
            if (newUsername != null && !newUsername.isEmpty()) {
                // Update model's username
                model.setUsername(newUsername);
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

    private void clearPasswordFields() {
        accountJFXController.clearPasswordFields();
    }

    // Method to handle username change request
    public void handleUsernameChange() {
        String newUsername = accountJFXController.getNewUsername();

        // Validation
        if (newUsername == null || newUsername.trim().isEmpty()) {
            accountJFXController.showUsernameEmpty();
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

    private void sendMessage(String msg) throws IOException {
        serverWriter.write(msg);
        serverWriter.newLine();
        serverWriter.flush();
    }
}


