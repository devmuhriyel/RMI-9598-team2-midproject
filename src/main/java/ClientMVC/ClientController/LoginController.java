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
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.util.Optional;

public class LoginController {
    private final LoginJFXController loginJFXController;
    private final LoginView view;
    private final UnifiedClientModel model;
    private UserXMLHandler userXMLHandler = new UserXMLHandler();

    public LoginController(LoginView loginView, UnifiedClientModel unifiedModel, LoginJFXController loginJFXController) {
        this.loginJFXController = loginJFXController;
        this.view = loginView;
        this.model = unifiedModel;

        // Set up message listener for login responses
        model.setMessageListener(this::handleLoginResponse);

        initialize();
    }

    private void initialize() {
        loginJFXController.setLoginButtonListener(() -> {
            loginJFXController.getLoginButton().setDisable(true);
            String username = loginJFXController.getUsername();
            String password = loginJFXController.getPassword();
            model.login(username, password);
        });

        loginJFXController.setSignupLinkListener(() -> {
            Platform.runLater(() -> {
                try {
                    LoginView.getPrimaryStage().close();
                    SignupView signupView = new SignupView();
                    Stage signupStage = new Stage();
                    signupView.start(signupStage);
                    // Pass the unified model to signup controller
                    new SignupController(model, signupView, signupView.getFXController());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        });

        loginJFXController.setForgotPasswordLinkListener(() -> {
            handleForgotPassword();
        });
    }

    public void handleLoginResponse(String msg) {
        try {
            String reply = userXMLHandler.getReply(msg.trim());
            if (reply != null && reply.equals("LOGIN_SUCCESS")) {
                view.login();
                Platform.runLater(() -> {
                    try {
                        HomeView homeView = new HomeView();
                        Stage stage1 = new Stage();
                        homeView.start(stage1);
                        // Pass the same unified model to home controller
                        new HomeController(model, homeView, homeView.getFXController());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else if (reply != null && reply.equals("LOGIN_FAILED")) {
                Platform.runLater(() -> {
                    System.out.println("Invalid username or password");
                    loginJFXController.getLoginButton().setDisable(false);
                });
            }
        } catch (Exception e) {
            System.out.println("Error processing login response: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleForgotPassword() {
        // Show dialog to get username
        TextInputDialog usernameDialog = new TextInputDialog();
        usernameDialog.setTitle("Forgot Password");
        usernameDialog.setHeaderText("Password Reset Request");
        usernameDialog.setContentText("Enter your username:");
        usernameDialog.getEditor().setPromptText("Username");

        usernameDialog.showAndWait().ifPresent(username -> {
            if (!username.trim().isEmpty()) {
                // Validate username format
                if (username.trim().length() < 3) {
                    showAlert(Alert.AlertType.WARNING, "Invalid Username",
                            "Username must be at least 3 characters long.");
                    return;
                }

                // Show information dialog explaining the process
                Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                infoAlert.setTitle("Password Reset Process");
                infoAlert.setHeaderText("Verification Required");
                infoAlert.setContentText("To reset your password for account: " + username.trim() + "\n\n" +
                        "You will need to provide your security verification code.\n" +
                        "This is the verification code that was set up with your account.\n\n" +
                        "Next Steps:\n" +
                        "1. Enter your verification code when prompted\n" +
                        "2. An administrator will validate your request\n" +
                        "3. Your password will be reset if approved\n\n" +
                        "If you don't remember your verification code, please contact an administrator.");

                infoAlert.getDialogPane().setPrefSize(550, 320);
                infoAlert.setResizable(true);

                // Set up message handling for password reset responses
                setupPasswordResetResponseHandling();

                infoAlert.showAndWait();

                // Show dialog to get their existing verification code
                showVerificationCodeDialog(username.trim());
            } else {
                showAlert(Alert.AlertType.WARNING, "Invalid Input", "Username cannot be empty.");
            }
        });
    }
    /**
     * Show verification code input dialog - this is for the user's existing security code
     */
    private void showVerificationCodeDialog(String username) {
        Dialog<String> codeDialog = new Dialog<>();
        codeDialog.setTitle("Security Verification");
        codeDialog.setHeaderText("Enter Your Security Verification Code");

        // Create form content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField codeField = new TextField();
        codeField.setPromptText("Your verification code");

        Label instructionLabel = new Label("Enter the verification code that was set up with your account:");
        instructionLabel.setWrapText(true);
        instructionLabel.setMaxWidth(350);

        Label warningLabel = new Label("This is your personal security verification code, not a code sent to you.");
        warningLabel.setStyle("-fx-text-fill: #1976d2; -fx-font-size: 11px;");
        warningLabel.setWrapText(true);
        warningLabel.setMaxWidth(350);

        Label helpLabel = new Label("If you don't remember this code, please contact an administrator for assistance.");
        helpLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 10px;");
        helpLabel.setWrapText(true);
        helpLabel.setMaxWidth(350);

        grid.add(instructionLabel, 0, 0, 2, 1);
        grid.add(new Label("Verification Code:"), 0, 1);
        grid.add(codeField, 1, 1);
        grid.add(warningLabel, 0, 2, 2, 1);
        grid.add(helpLabel, 0, 3, 2, 1);

        codeDialog.getDialogPane().setContent(grid);

        // Add buttons
        ButtonType submitButton = new ButtonType("Submit Request", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        codeDialog.getDialogPane().getButtonTypes().addAll(submitButton, cancelButton);

        // Enable submit button only when code is entered
        Node submitButtonNode = codeDialog.getDialogPane().lookupButton(submitButton);
        submitButtonNode.setDisable(true);

        codeField.textProperty().addListener((obs, oldText, newText) -> {
            submitButtonNode.setDisable(newText.trim().isEmpty());
        });

        // Set result converter
        codeDialog.setResultConverter(dialogButton -> {
            if (dialogButton == submitButton) {
                return codeField.getText().trim();
            }
            return null;
        });

        // Focus on code field
        Platform.runLater(() -> codeField.requestFocus());

        Optional<String> result = codeDialog.showAndWait();

        if (result.isPresent()) {
            String verificationCode = result.get();

            if (!verificationCode.isEmpty()) {
                // Send password reset request with username and their verification code
                model.requestPasswordResetWithCode(username, verificationCode);

                // Show confirmation message
                Alert confirmAlert = new Alert(Alert.AlertType.INFORMATION);
                confirmAlert.setTitle("Password Reset Request Submitted");
                confirmAlert.setHeaderText("Request Sent for Administrator Review");
                confirmAlert.setContentText("Your password reset request has been submitted!\n\n" +
                        "Username: " + username + "\n" +
                        "Verification Code: [Protected]\n\n" +
                        "Next Steps:\n" +
                        "1. An administrator will verify your security code\n" +
                        "2. If approved, your password will be reset\n" +
                        "3. You will receive your new password\n" +
                        "4. Use the new password to log in\n\n" +
                        "You will be notified of the outcome shortly.");

                confirmAlert.getDialogPane().setPrefSize(500, 350);
                confirmAlert.setResizable(true);
                confirmAlert.showAndWait();
            } else {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter your verification code.");
            }
        }
    }
    /**
     * Set up enhanced response handling for password reset
     */
    private void setupPasswordResetResponseHandling() {
        model.setMessageListener(response -> {
            // Handle the original login responses
            handleLoginResponse(response);

            // Handle password reset specific responses - these are already handled in UnifiedClientModel
            // No additional handling needed here as UnifiedClientModel shows the appropriate dialogs
        });
    }
    /**
     * Utility method to show alerts
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setPrefSize(450, 220);
        alert.setResizable(true);
        alert.showAndWait();
    }
}