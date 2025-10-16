package ClientMVC.ClientView.Login;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class LoginJFXController {
    //VARIABLES
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Hyperlink forgotPasswordLink;
    @FXML
    private Hyperlink signupLink;
    private Runnable signupLinkListener;
    private Runnable loginButtonListener;
    private Runnable forgotPasswordLinkListener;

    //METHODS
    @FXML
    private void initialize() {
        loginButton.setOnAction(e -> {
            if (loginButtonListener != null) {
                loginButtonListener.run();
            }
        });

        signupLink.setOnAction(e -> {
            if (signupLinkListener != null) {
                signupLinkListener.run();
            }
        });
        forgotPasswordLink.setOnAction(e -> {
            if (forgotPasswordLinkListener != null) {
                forgotPasswordLinkListener.run();
            }
        });
    }
    // ========== PASSWORD RESET UI METHODS ==========
    public void showUsernameDialog(java.util.function.Consumer<String> callback) {
        TextInputDialog usernameDialog = new TextInputDialog();
        usernameDialog.setTitle("Forgot Password");
        usernameDialog.setHeaderText("Password Reset Request");
        usernameDialog.setContentText("Enter your username:");
        usernameDialog.getEditor().setPromptText("Username");
        usernameDialog.showAndWait().ifPresent(username -> {
            if (!username.trim().isEmpty()) {
                if (username.trim().length() < 3) {
                    showAlert(Alert.AlertType.WARNING, "Invalid Username",
                            "Username must be at least 3 characters long.");
                    return;
                }
                Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                infoAlert.setTitle("Password Reset Process");
                infoAlert.setHeaderText("Verification Required");
                infoAlert.setContentText("To reset your password for account: " + username.trim() + "\n\n" +
                        "You will need to provide your security verification code.\n" +
                        "This is the verification code that was set up with your account.\n\n" +
                        "Next Steps:\n" +
                        "1. Enter your verification code when prompted\n" +
                        "2. Your verification code will be validated\n" +
                        "3. You can then set your new password\n\n" +
                        "If you don't remember your verification code, please contact an administrator.");
                infoAlert.getDialogPane().setPrefSize(550, 320);
                infoAlert.setResizable(true);
                infoAlert.showAndWait().ifPresent(result -> {
                    showVerificationCodeDialog(username.trim(), callback);
                });
            } else {
                showAlert(Alert.AlertType.WARNING, "Invalid Input", "Username cannot be empty.");
            }
        });
    }
    private void showVerificationCodeDialog(String username, java.util.function.Consumer<String> callback) {
        Dialog<String> codeDialog = new Dialog<>();
        codeDialog.setTitle("Security Verification");
        codeDialog.setHeaderText("Enter Your Security Verification Code");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        TextField codeField = new TextField();
        codeField.setPromptText("Your verification code");
        Label instructionLabel = new Label("Enter the verification code that was set up with your account:");
        instructionLabel.setWrapText(true);
        instructionLabel.setMaxWidth(350);
        Label warningLabel = new Label("This is your personal security verification code.");
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
        ButtonType submitButton = new ButtonType("Submit Request", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        codeDialog.getDialogPane().getButtonTypes().addAll(submitButton, cancelButton);
        Node submitButtonNode = codeDialog.getDialogPane().lookupButton(submitButton);
        submitButtonNode.setDisable(true);
        codeField.textProperty().addListener((obs, oldText, newText) -> {
            submitButtonNode.setDisable(newText.trim().isEmpty());
        });
        codeDialog.setResultConverter(dialogButton -> {
            if (dialogButton == submitButton) {
                return codeField.getText().trim();
            }
            return null;
        });
        javafx.application.Platform.runLater(() -> codeField.requestFocus());
        Optional<String> result = codeDialog.showAndWait();
        if (result.isPresent()) {
            String verificationCode = result.get();
            if (!verificationCode.isEmpty()) {
                callback.accept(username + "![]!" + verificationCode);
            } else {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter your verification code.");
            }
        }
    }
    public void showPasswordChangeDialog(String username, java.util.function.Consumer<String> callback) {
        Dialog<String> passwordDialog = new Dialog<>();
        passwordDialog.setTitle("Set New Password");
        passwordDialog.setHeaderText("Verification Successful - Set Your New Password");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Enter new password");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm new password");
        Label userLabel = new Label("Client: " + username);
        userLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        Label instructionLabel = new Label("Please enter your new password:");
        instructionLabel.setWrapText(true);
        instructionLabel.setMaxWidth(350);
        Label requirementLabel = new Label("Password must be at least 4 characters long.");
        requirementLabel.setStyle("-fx-text-fill: #1976d2; -fx-font-size: 10px;");
        requirementLabel.setWrapText(true);
        requirementLabel.setMaxWidth(350);
        Label matchLabel = new Label("");
        matchLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 10px;");
        grid.add(userLabel, 0, 0, 2, 1);
        grid.add(instructionLabel, 0, 1, 2, 1);
        grid.add(new Label("New Password:"), 0, 2);
        grid.add(newPasswordField, 1, 2);
        grid.add(new Label("Confirm Password:"), 0, 3);
        grid.add(confirmPasswordField, 1, 3);
        grid.add(requirementLabel, 0, 4, 2, 1);
        grid.add(matchLabel, 0, 5, 2, 1);
        passwordDialog.getDialogPane().setContent(grid);
        ButtonType changeButton = new ButtonType("Change Password", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        passwordDialog.getDialogPane().getButtonTypes().addAll(changeButton, cancelButton);
        Node changeButtonNode = passwordDialog.getDialogPane().lookupButton(changeButton);
        changeButtonNode.setDisable(true);
        Runnable validatePasswords = () -> {
            String newPassword = newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();
            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                matchLabel.setText("");
                changeButtonNode.setDisable(true);
                return;
            }
            if (newPassword.length() < 4) {
                matchLabel.setText("Password must be at least 4 characters long");
                matchLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 10px;");
                changeButtonNode.setDisable(true);
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                matchLabel.setText("Passwords do not match");
                matchLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 10px;");
                changeButtonNode.setDisable(true);
                return;
            }
            matchLabel.setText("Passwords match ✓");
            matchLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 10px;");
            changeButtonNode.setDisable(false);
        };
        newPasswordField.textProperty().addListener((obs, oldText, newText) -> validatePasswords.run());
        confirmPasswordField.textProperty().addListener((obs, oldText, newText) -> validatePasswords.run());
        passwordDialog.setResultConverter(dialogButton -> {
            if (dialogButton == changeButton) {
                return newPasswordField.getText();
            }
            return null;
        });
        Platform.runLater(() -> newPasswordField.requestFocus());
        Optional<String> result = passwordDialog.showAndWait();
        if (result.isPresent()) {
            String newPassword = result.get();
            if (newPassword.length() >= 4) {
                callback.accept(newPassword);
            } else {
                showAlert(Alert.AlertType.ERROR, "Invalid Password", "Password must be at least 4 characters long.");
            }
        }
    }
    public void showPasswordResetSuccessDialog(String username) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Password Reset Successful");
        alert.setHeaderText("Your Password Has Been Changed");
        alert.setContentText("Hello " + username + ",\n\n" +
                "Your password has been successfully changed!\n\n" +
                "IMPORTANT NOTES:\n" +
                "• Your new password is now active\n" +
                "• You can now log in using your new password\n" +
                "• Make sure to remember your new password\n" +
                "• Consider updating your password manager if you use one\n\n" +
                "You can now close this window and log in with your new password.");
        alert.getDialogPane().setPrefSize(500, 350);
        alert.setResizable(true);
        alert.getDialogPane().setStyle(
                "-fx-background-color: #e8f5e8;" +
                        "-fx-border-color: #4caf50;" +
                        "-fx-border-width: 2px;" +
                        "-fx-font-size: 12px;"
        );
        alert.showAndWait();
    }
    public void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setPrefSize(450, 220);
        alert.setResizable(true);
        alert.showAndWait();
    }
    public void showNotification(String type, String title, String header, String message) {
        Alert.AlertType alertType;
        String style = "";

        switch (type) {
            case "SUCCESS":
                alertType = Alert.AlertType.INFORMATION;
                style = "-fx-background-color: #e8f5e8; -fx-border-color: #4caf50; -fx-border-width: 2px;";
                break;
            case "WARNING":
                alertType = Alert.AlertType.WARNING;
                style = "-fx-background-color: #fff3cd; -fx-border-color: #ffc107; -fx-border-width: 2px;";
                break;
            case "ERROR":
                alertType = Alert.AlertType.ERROR;
                style = "-fx-background-color: #f8d7da; -fx-border-color: #dc3545; -fx-border-width: 2px;";
                break;
            default:
                alertType = Alert.AlertType.INFORMATION;
                break;
        }
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.getDialogPane().setPrefSize(500, 250);
        alert.setResizable(true);

        if (!style.isEmpty()) {
            alert.getDialogPane().setStyle(style);
        }

        alert.showAndWait();
    }
    // ========== GETTERS AND SETTERS ==========
    public void setLoginButtonListener(Runnable listener) {
        this.loginButtonListener = listener;
    }
    public String getUsername() {
        return usernameField.getText();
    }
    public String getPassword() {
        return passwordField.getText();
    }
    public Button getLoginButton() {
        return loginButton;
    }
    public void setSignupLinkListener(Runnable listener) {
        this.signupLinkListener = listener;
    }
    public void setForgotPasswordLinkListener(Runnable listener) {
        this.forgotPasswordLinkListener = listener;
    }
}