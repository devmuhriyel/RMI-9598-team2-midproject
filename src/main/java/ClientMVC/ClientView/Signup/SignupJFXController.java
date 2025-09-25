package ClientMVC.ClientView.Signup;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.function.Consumer;

public class SignupJFXController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private TextField verificationField;

    @FXML
    private Button signupButton;

    @FXML
    private Hyperlink signInLink;

    // Callbacks → set by SignupController
    private Runnable onSignup;
    private Runnable onSignIn;

    // ====== Exposed Getters for Controller ======
    public String getUsername() {
        return usernameField.getText();
    }

    public String getPassword() {
        return passwordField.getText();
    }

    public String getConfirmPassword() {
        return confirmPasswordField.getText();
    }

    public String getVerificationCode() {
        return verificationField.getText();
    }

    public Button getSignupButton() {
        return signupButton;
    }

    // ====== Listeners to be injected by Controller ======
    public void setSignupButtonListener(Runnable listener) {
        this.onSignup = listener;
        signupButton.setOnAction(e -> {
            if (onSignup != null) onSignup.run();
        });
    }

    public void setSignInListener(Runnable listener) {
        this.onSignIn = listener;
        signInLink.setOnAction(e -> {
            if (onSignIn != null) onSignIn.run();
        });
    }

    // ====== Feedback helper for Controller ======
    public void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
