package AdminMVC.AdminControllers;

import AdminMVC.AdminCallbackImplementation;
import AdminMVC.AdminModel.UnifiedAdminModel;
import AdminMVC.AdminView.Admin.AdminView;
import AdminMVC.AdminView.Login.LoginJFXController;
import AdminMVC.AdminView.Login.LoginView;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class AdminLoginController {
    private final LoginJFXController loginJFXController;
    private final LoginView view;
    private final UnifiedAdminModel model;
    private String currentPasswordResetUser = null;

    public AdminLoginController(LoginView loginView, UnifiedAdminModel unifiedModel, LoginJFXController loginJFXController) {
        this.loginJFXController = loginJFXController;
        this.view = loginView;
        this.model = unifiedModel;

        if (model.getCallback() instanceof AdminCallbackImplementation) {
            AdminCallbackImplementation callback = (AdminCallbackImplementation) model.getCallback();
            callback.setAdminLoginController(this);
            callback.setModel(model);  // ADD THIS LINE
        }

        model.setLoginResponseListener(this::handleLoginResponse);
        model.setVerificationCodeValidListener(this::handleVerificationCodeValid);
        model.setOperationResultListener(this::handleGeneralNotification);
        initialize();
    }

    private void initialize() {
        loginJFXController.setLoginButtonListener(() -> {
            loginJFXController.getLoginButton().setDisable(true);
            String username = loginJFXController.getUsername();
            String password = loginJFXController.getPassword();
            model.login(username, password);
        });

        loginJFXController.setForgotPasswordLinkListener(() -> {
            handleForgotPassword();
        });
    }

    public void handleFailedLogin() {
        Platform.runLater(() -> {
            loginJFXController.showAlert(Alert.AlertType.ERROR,
                    "Login Failed",
                    "Invalid username or password");
            loginJFXController.getLoginButton().setDisable(false);
        });
    }

    public void handleFailedCodeVerification() {
        Platform.runLater(() -> {
            loginJFXController.showAlert(Alert.AlertType.ERROR,
                    "Verification Failed",
                    "Invalid verification code");
        });
    }

    private void handleForgotPassword() {
        loginJFXController.showUsernameDialog(usernameAndCode -> {
            if (usernameAndCode != null && usernameAndCode.contains("![]!")) {
                String[] parts = usernameAndCode.split("!\\[]!");
                if (parts.length >= 2) {
                    String username = parts[0];
                    String verificationCode = parts[1];
                    currentPasswordResetUser = username;
                    sendAdminPasswordResetRequest(username, verificationCode);
                }
            }
        });
    }

    private void sendAdminPasswordResetRequest(String username, String verificationCode) {
        try {
            String xmlMessage = "<Request><type>ADMIN_REQUEST_PASSWORD_RESET_WITH_CODE</type><content>" +
                    username + "![]!" + verificationCode + "</content></Request>";
            model.sendAdminCommand(xmlMessage);
        } catch (Exception e) {
            System.err.println("Error sending admin password reset request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleVerificationCodeValid(String response) {
        Platform.runLater(() -> {
            try {
                String username = "";
                if (username.isEmpty() && currentPasswordResetUser != null) {
                    username = currentPasswordResetUser;
                } else {
                    username = response.trim();
                }

                if (username != null && !username.isEmpty()) {
                    String finalUsername = username;
                    loginJFXController.showPasswordChangeDialog(username, newPassword -> {
                        resetAdminPasswordOnServer(finalUsername, newPassword);
                    });
                } else {
                    loginJFXController.showAlert(Alert.AlertType.ERROR,
                            "Error",
                            "Unable to process admin password reset. Please try again.");
                    currentPasswordResetUser = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                loginJFXController.showAlert(Alert.AlertType.ERROR,
                        "Error",
                        "An error occurred while processing your request. Please try again.");
            }
        });
    }

    private void resetAdminPasswordOnServer(String username, String newPassword) {
        try {
            String xmlMessage = "<Request><type>ADMIN_RESET_PASSWORD</type><content>" +
                    username + "![]!" + newPassword + "</content></Request>";
            model.sendAdminCommand(xmlMessage);
        } catch (Exception e) {
            System.err.println("Error sending admin password reset: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleGeneralNotification(String message) {
        Platform.runLater(() -> {
            String alertType, title, header;

            if (message.contains("VERIFICATION_CODE_INVALID") || message.contains("Invalid verification code")) {
                alertType = "ERROR";
                title = "Invalid Verification Code";
                header = "Admin Verification Failed";
                currentPasswordResetUser = null;
            } else if (message.contains("PASSWORD_RESET_FAILED") || message.contains("Password reset failed")) {
                alertType = "ERROR";
                title = "Admin Password Reset Failed";
                header = "Unable to reset your administrator password";
                currentPasswordResetUser = null;
            } else if (message.contains("PASSWORD_CHANGED") || message.contains("Password changed successfully")) {
                alertType = "SUCCESS";
                title = "Password Changed";
                header = "Admin Password Successfully Changed";

                if (currentPasswordResetUser != null) {
                    loginJFXController.showPasswordResetSuccessDialog(currentPasswordResetUser);
                    currentPasswordResetUser = null;
                    return;
                }
            } else if (message.contains("ADMIN_PASSWORD_RESET_SUCCESS")) {
                alertType = "SUCCESS";
                title = "Password Reset Successful";
                header = "Admin Password Successfully Reset";

                if (currentPasswordResetUser != null) {
                    loginJFXController.showPasswordResetSuccessDialog(currentPasswordResetUser);
                    currentPasswordResetUser = null;
                    return;
                }
            } else {
                alertType = "INFO";
                title = "Admin Notification";
                header = "System Notification";
            }

            loginJFXController.showNotification(alertType, title, header, message);
        });
    }

    public void handleLoginResponse(String response) {
        Platform.runLater(() -> {
            System.out.println(">>> AdminLoginController: Received login response: " + response);

            if ("LOGIN_SUCCESS".equals(response)) {
                try {
                    System.out.println("✓ Login successful, proceeding to admin view...");

                    Stage loginStage = (Stage) loginJFXController.getLoginButton().getScene().getWindow();
                    String loggedInUsername = loginJFXController.getUsername();

                    System.out.println("  Setting admin username: " + loggedInUsername);
                    model.setAdminUsername(loggedInUsername);

                    System.out.println("  Creating admin view...");
                    AdminView adminView = new AdminView();
                    Stage adminStage = new Stage();
                    adminView.start(adminStage);

                    System.out.println("  Creating admin controller...");
                    AdminController adminController = new AdminController(adminView.getFXController(), model);

                    if (model.getCallback() instanceof AdminCallbackImplementation) {
                        ((AdminCallbackImplementation) model.getCallback()).setAdminController(adminController);
                    }

                    System.out.println("  Loading admin data...");
                    model.loadData();

                    System.out.println("  Closing login stage...");
                    loginStage.close();

                    System.out.println("✓ Admin view opened successfully");

                } catch (Exception e) {
                    System.err.println("✗ Error showing admin view: " + e.getMessage());
                    e.printStackTrace();
                    loginJFXController.showAlert(Alert.AlertType.ERROR,
                            "Error",
                            "Failed to open admin panel: " + e.getMessage());
                    loginJFXController.getLoginButton().setDisable(false);
                }
            } else {
                System.out.println("✗ Login failed with response: " + response);
                handleFailedLogin();
            }
        });
    }
}
