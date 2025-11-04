package ClientMVC.ClientController;

import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Signup.SignupJFXController;
import ClientMVC.ClientView.Signup.SignupView;
import javafx.application.Platform;
import javafx.stage.Stage;

public class SignupController {
    private final UnifiedClientModel model;
    private final SignupView view;
    private final SignupJFXController fxController;

    public SignupController(UnifiedClientModel model, SignupView view, SignupJFXController fxController) {
        this.model = model;
        this.view = view;
        this.fxController = fxController;
        // Set up the signup controller reference in callback
        model.getCallback().setClientSignupController(this);
        initialize();
    }

    private void initialize() {
        fxController.setSignupButtonListener(() -> {
            if (validateInput()) {
                fxController.getSignupButton().setDisable(true);
                String username = fxController.getUsername();
                String password = fxController.getPassword();
                String verification = fxController.getVerificationCode();

                try {
                    // Use RMI to signup
                    String jsonString = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"verificationCode\":\"" + verification + "\"}";
                    model.signUp(username, password, verification, jsonString, model.getCallback());
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        fxController.getSignupButton().setDisable(false);
                        fxController.showAlert("Signup Error: Failed to connect to server: " + e.getMessage());
                    });
                    e.printStackTrace();
                }
            }
        });

        fxController.setSignInListener(() -> {
            Platform.runLater(() -> {
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
        });
    }

    public void onSuccessfulSignUpFromRemote() {
        Platform.runLater(() -> {
            fxController.showAlert("Registration successful! Please login with your new account.");
            fxController.getSignupButton().setDisable(false);
            navigateToLogin();
        });
    }

    public void onFailedSignUpFromRemote() {
        Platform.runLater(() -> {
            fxController.showAlert("Registration failed. Username may already exist.");
            fxController.getSignupButton().setDisable(false);
        });
    }

    private boolean validateInput() {
        String username = fxController.getUsername();
        String password = fxController.getPassword();
        String confirmPassword = fxController.getConfirmPassword();
        String verification = fxController.getVerificationCode();
        
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
            fxController.showAlert("Verification code is required");
            return false;
        }
        if (password.length() < 6) {
            fxController.showAlert("Password must be at least 6 characters long");
            return false;
        }
        return true;
    }

    private void navigateToLogin() {
        try {
            view.getStage().hide();
            Stage loginStage = ClientMVC.ClientView.Login.LoginView.getPrimaryStage();
            if (loginStage != null) {
                loginStage.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
