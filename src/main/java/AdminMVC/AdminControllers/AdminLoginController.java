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

        // Connect callback to this controller
        if (model.getCallback() instanceof AdminCallbackImplementation) {
            ((AdminCallbackImplementation) model.getCallback()).setAdminLoginController(this);
        }

        model.setLoginResponseListener(this::handleLoginResponse);
        model.setVerificationCodeValidListener(this::handleVerificationCodeValid);
        model.setPasswordResetListener(this::handlePasswordResetListener);
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
            System.out.println("Sending admin password reset request: " + xmlMessage);
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
            System.out.println("Sending admin password reset: " + xmlMessage);
            model.sendAdminCommand(xmlMessage);
        } catch (Exception e) {
            System.err.println("Error sending admin password reset: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handlePasswordResetListener(String username) {
        Platform.runLater(() -> {
            System.out.println("Password reset requested for admin: " + username);
        });
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
            if ("LOGIN_SUCCESS".equals(response)) {
                view.login();
                try {
                    AdminView homeView = new AdminView();
                    Stage stage1 = new Stage();
                    homeView.start(stage1);
                    new AdminController(homeView.getFXController(), model);
                    model.loadData();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if ("LOGIN_FAILED".equals(response)) {
                System.out.println("Invalid admin credentials");
                loginJFXController.getLoginButton().setDisable(false);
            }
        });
    }
}