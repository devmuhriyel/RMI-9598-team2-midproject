package ClientMVC.ClientController;

import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Signup.SignupJFXController;
import ClientMVC.ClientView.Signup.SignupView;
import ClientMVC.UserXMLHandler;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class SignupController {
    //    private final UnifiedClientModel model;
//    private final SignupView view;
//    private final SignupJFXController fxController;
//    private final UserXMLHandler xmlHandler = new UserXMLHandler();
//
//    public SignupController(UnifiedClientModel model, SignupView view, SignupJFXController fxController) {
//        this.model = model;
//        this.view = view;
//        this.fxController = fxController;
//        model.setMessageListener(this::handleSignupResponse);
//
//        initialize();
//    }
//
//    private void initialize() {
//        fxController.setSignupButtonListener(() -> {
//            if (validateInput()) {
//                fxController.getSignupButton().setDisable(true);
//                String username = fxController.getUsername();
//                String password = fxController.getPassword();
//                String verification = fxController.getVerificationCode();
//                model.signup(username, password, verification);
//            }
//        });
//        fxController.setSignInListener(() -> {
//            Platform.runLater(() -> {
//                try {
//                    view.getStage().hide();
//                    Stage loginStage = ClientMVC.ClientView.Login.LoginView.getPrimaryStage();
//                    if (loginStage != null) {
//                        loginStage.show();
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            });
//        });
//    }
//
//    private boolean validateInput() {
//        String username = fxController.getUsername();
//        String password = fxController.getPassword();
//        String confirmPassword = fxController.getConfirmPassword();
//        String verification = fxController.getVerificationCode();
//        if (username == null || username.trim().isEmpty()) {
//            fxController.showAlert("Username is required");
//            return false;
//        }
//        if (password == null || password.trim().isEmpty()) {
//            fxController.showAlert("Password is required");
//            return false;
//        }
//        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
//            fxController.showAlert("Please confirm your password");
//            return false;
//        }
//        if (!password.equals(confirmPassword)) {
//            fxController.showAlert("Passwords do not match");
//            return false;
//        }
//        if (verification == null || verification.trim().isEmpty()) {
//            fxController.showAlert("Verification code is required");
//            return false;
//        }
//        if (password.length() < 6) {
//            fxController.showAlert("Password must be at least 6 characters long");
//            return false;
//        }
//        return true;
//    }
//
//    private void handleSignupResponse(String msg) {
//        try {
//            String reply = xmlHandler.getReply(msg.trim());
//            if ("REGISTER_SUCCESS".equals(reply)) {
//                Platform.runLater(() -> {
//                    fxController.showAlert("Registration successful! Please login with your new account.");
//                    fxController.getSignupButton().setDisable(false);
//                    navigateToLogin();
//                });
//            } else if ("REGISTER_FAILED".equals(reply)) {
//                Platform.runLater(() -> {
//                    fxController.showAlert("Registration failed. Username may already exist.");
//                    fxController.getSignupButton().setDisable(false);
//                });
//            }
//        } catch (Exception e) {
//            Platform.runLater(() -> {
//                fxController.showAlert("Error processing registration response.");
//                fxController.getSignupButton().setDisable(false);
//            });
//            System.out.println("Error in handleSignupResponse: " + e.getMessage());
//        }
//    }
//
//    private void navigateToLogin() {
//        try {
//            view.getStage().hide();
//            Stage loginStage = ClientMVC.ClientView.Login.LoginView.getPrimaryStage();
//            if (loginStage != null) {
//                loginStage.show();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
    private final UnifiedClientModel model;
    private final SignupView view;
    private final SignupJFXController fxController;
    private final UserXMLHandler xmlHandler = new UserXMLHandler();

    public SignupController(UnifiedClientModel model, SignupView view, SignupJFXController fxController) {
        this.model = model;
        this.view = view;
        this.fxController = fxController;
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
                    fxController.getSignupButton().setDisable(false);
                    fxController.showAlert("Signup Error: Failed to connect to server: " + e.getMessage());
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
        
        // Set up callback listeners
        setupCallbackListeners();
    }

    private void setupCallbackListeners() {
        // Set up callback for successful signup
        model.getCallback().setOnSuccessfulSignUpListener(() -> {
            Platform.runLater(() -> {
                fxController.showAlert("Registration successful! Please login with your new account.");
                fxController.getSignupButton().setDisable(false);
                navigateToLogin();
            });
        });
        
        // Set up callback for failed signup
        model.getCallback().setOnFailedSignUpListener(() -> {
            Platform.runLater(() -> {
                fxController.showAlert("Registration failed. Username may already exist.");
                fxController.getSignupButton().setDisable(false);
            });
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
