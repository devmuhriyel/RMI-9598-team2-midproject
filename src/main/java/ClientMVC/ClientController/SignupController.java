package ClientMVC.ClientController;

import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Signup.SignupJFXController;
import ClientMVC.ClientView.Signup.SignupView;
import ClientMVC.UserXMLHandler;
import javafx.application.Platform;
import javafx.stage.Stage;

public class SignupController {
    private final UnifiedClientModel model;
    private final SignupView view;
    private final SignupJFXController fxController;
    private UserXMLHandler xmlHandler = new UserXMLHandler();

    public SignupController(UnifiedClientModel model, SignupView view, SignupJFXController fxController) {
        this.model = model;
        this.view = view;
        this.fxController = fxController;

        // Set up message listener for signup responses
        model.setMessageListener(this::handleSignupResponse);
        initialize();
    }

    private void initialize() {
        // Handle signup button click
        fxController.setSignupButtonListener(() -> {
            if (validateInput()) {
                fxController.getSignupButton().setDisable(true);

                String username = fxController.getUsername();
                String password = fxController.getPassword();
                String verification = fxController.getVerificationCode();

                // Send signup request through unified model
                String xmlMessage = "<Request><type>REGISTER</type><content>" +
                        username + "![]!" + password + "![]!" + verification + "</content></Request>";
                model.sendRawMessage(xmlMessage);
            }
        });

        // Handle sign in link click
        fxController.setSignInListener(() -> {
            Platform.runLater(() -> {
                try {
                    // Hide signup stage
                    view.getStage().hide();

                    // Show login stage
                    Stage loginStage = ClientMVC.ClientView.Login.LoginView.getPrimaryStage();
                    if (loginStage != null) {
                        loginStage.show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    private boolean validateInput() {
        String username = fxController.getUsername();
        String password = fxController.getPassword();
        String confirmPassword = fxController.getConfirmPassword();
        String verification = fxController.getVerificationCode();

        // Basic validation
        if (username == null || username.trim().isEmpty()) {
            fxController.showAlert("Username is required");
            return false;
        }

        if (password == null || password.trim().isEmpty()) {
            fxController.showAlert("Password is required");
            return false;
        }

        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            fxController.showAlert("Please confirm your password");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            fxController.showAlert("Passwords do not match");
            return false;
        }

        if (verification == null || verification.trim().isEmpty()) {
            fxController.showAlert("Verification is required");
            return false;
        }

        // Password length validation
        if (password.length() < 6) {
            fxController.showAlert("Password must be at least 6 characters long");
            return false;
        }

        return true;
    }

    private void handleSignupResponse(String msg) {
        try {
            String reply = xmlHandler.getReply(msg.trim());
            if (reply != null && reply.equals("REGISTER_SUCCESS")) {
                Platform.runLater(() -> {
                    fxController.showAlert("Registration successful! Please login with your new account.");
                    fxController.getSignupButton().setDisable(false);

                    // Navigate back to Log in
                    try {
                        view.getStage().hide();
                        Stage loginStage = ClientMVC.ClientView.Login.LoginView.getPrimaryStage();
                        if (loginStage != null) {
                            loginStage.show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else if (reply != null && reply.equals("REGISTER_FAILED")) {
                Platform.runLater(() -> {
                    fxController.showAlert("Registration failed. Username may already exist.");
                    fxController.getSignupButton().setDisable(false);
                });
            }
        } catch (Exception e) {
            Platform.runLater(() -> {
                fxController.showAlert("Error processing registration response.");
                fxController.getSignupButton().setDisable(false);
            });
            System.out.println("Error processing signup response: " + e.getMessage());
        }
    }
}
