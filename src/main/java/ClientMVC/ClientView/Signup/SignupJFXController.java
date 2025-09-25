package ClientMVC.ClientView.Signup;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SignupJFXController {

    @FXML
    private Hyperlink signInLink;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button signupButton;

    @FXML
    private TextField verificationField;

    private Runnable signInListener;

    private Runnable signupButtonListener;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private void initialize() {
        signupButton.setOnAction(event -> {
            if (signupButtonListener != null) {
                signupButtonListener.run();
            }
        });

        signInLink.setOnAction(e -> {
            if (signInListener != null) {
                signInListener.run();
            }
        });
    }

    public String getVerificationCode() {
        return verificationField.getText();
    }

    public void setSignupButtonListener(Runnable listener) {
        this.signupButtonListener = listener;
    }

    public void setSignInListener(Runnable listener) {
        this.signInListener = listener;
    }

    public String getUsername() {
        return usernameField.getText();
    }

    public String getPassword() {
        return passwordField.getText();
    }

    public String getConfirmPassword() {
        return confirmPasswordField.getText();
    }

    public void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Signup");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    public Button getSignupButton() {
        return signupButton;
    }

}