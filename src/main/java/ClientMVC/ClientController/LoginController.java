package ClientMVC.ClientController;

import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.ClientView.Login.LoginJFXController;
import ClientMVC.ClientView.Login.LoginView;
import ClientMVC.ClientView.Signup.SignupView;
import ClientMVC.UserXMLHandler;
import ClientMVC.ClientController.SignupController;
import ClientMVC.ClientController.HomeController;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class LoginController {
    private final LoginJFXController loginJFXController;
    private final LoginView view;
    private final UnifiedClientModel model;
    private UserXMLHandler userXMLHandler = new UserXMLHandler();
    private String currentPasswordResetUser = null;

    public LoginController(LoginView loginView, UnifiedClientModel unifiedModel, LoginJFXController loginJFXController) {
        this.loginJFXController = loginJFXController;
        this.view = loginView;
        this.model = unifiedModel;
        model.setMessageListener(this::handleAllResponses);
        model.setGeneralNotificationCallback(this::handleGeneralNotification);
        model.setVerificationCodeValidListener(this::handleVerificationCodeValid);

        initialize();
    }

    private void initialize() {
        loginJFXController.setLoginButtonListener(() -> {
            String username = loginJFXController.getUsername();
            String password = loginJFXController.getPassword();
            model.login(username, password);
        });
        loginJFXController.setSignupLinkListener(() -> {
            Platform.runLater(() -> {
                try {
                    LoginView.getPrimaryStage().close();

                    SignupView signupView = new SignupView();
                    new SignupController(model, signupView, signupView.getFxController());
                    signupView.getStage().show();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
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
                    // Store current reset user
                    currentPasswordResetUser = username;
                    // Send verification request
                    model.requestPasswordResetWithCode(username, verificationCode);
                }
            }
        });
    }

    private void handleVerificationCodeValid(String xmlResponse) {
        Platform.runLater(() -> {
            try {
                // Extract username from response body
                String body = userXMLHandler.getResponseBody(xmlResponse);
                String username;
                if (body != null && !body.trim().isEmpty()) {
                    username = body.trim();
                } else {
                    username = currentPasswordResetUser;
                }
                if (username != null && !username.isEmpty()) {
                    loginJFXController.showPasswordChangeDialog(username, newPassword -> {
                        model.resetPasswordOnServer(username, newPassword);
                    });
                } else {
                    loginJFXController.showAlert(Alert.AlertType.ERROR,
                            "Error",
                            "Unable to process password reset. Please try again.");
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

    private void handleGeneralNotification(String type, String message) {
        Platform.runLater(() -> {
            String alertType, title, header;

            switch (type) {
                case "VERIFICATION_CODE_INVALID":
                    alertType = "ERROR";
                    title = "Invalid Verification Code";
                    header = "Verification Failed";
                    currentPasswordResetUser = null;
                    break;
                case "PASSWORD_RESET_FAILED":
                    alertType = "ERROR";
                    title = "Password Reset Failed";
                    header = "Unable to reset your password";
                    currentPasswordResetUser = null; // Clear reset session
                    break;
                default:
                    alertType = "INFO";
                    title = "Notification";
                    header = "System Notification";
                    break;
            }

            loginJFXController.showNotification(alertType, title, header, message);
        });
    }

    private void handleAllResponses(String msg) {
        try {
            String reply = userXMLHandler.getReply(msg.trim());

            if (reply != null) {
                switch (reply) {
                    case "LOGIN_SUCCESS":
                        Platform.runLater(() -> {
                            view.login();
                            try {
                                HomeView homeView = new HomeView();
                                Stage stage1 = new Stage();
                                homeView.start(stage1);
                                new HomeController(model, homeView, homeView.getFXController());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                        break;
                    case "LOGIN_FAILED":
                        Platform.runLater(() -> {
                            loginJFXController.showAlert(Alert.AlertType.ERROR,
                                    "Login Failed",
                                    "Invalid username or password. Please try again.");
                        });
                        break;
                    case "PASSWORD_CHANGED":
                        Platform.runLater(() -> {
                            if (currentPasswordResetUser != null) {
                                loginJFXController.showPasswordResetSuccessDialog(currentPasswordResetUser);
                                currentPasswordResetUser = null; // Clear reset session
                            }
                        });
                        break;

                    case "PASSWORD_CHANGE_FAILED":
                    default:
                        System.out.println("Unhandled response: " + reply);
                        break;
                }

            }
        } catch (Exception e) {
            System.out.println("Error processing response: " + e.getMessage());
            e.printStackTrace();
        }
    }
}