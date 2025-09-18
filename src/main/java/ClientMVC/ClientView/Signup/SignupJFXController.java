package ClientMVC.ClientView.Signup;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SignupJFXController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button signupButton;
    @FXML private Label messageLabel;
    @FXML private Hyperlink signInLink;

    private Runnable signInListener;

    public String getUsername() {
        return usernameField.getText().trim();
    }

    public String getPassword() {
        return passwordField.getText().trim();
    }

    public String getConfirmPassword() {
        return confirmPasswordField.getText().trim();
    }

    public Button getSignupButton() {
        return signupButton;
    }

    private void handleSignInLink() {
        if (signInListener != null) signInListener.run();
    }

    public void setMessage(String msg) {
        messageLabel.setText(msg);
    }

    public void setSignInListener(Runnable listener) {
        signInLink.setOnAction(e -> listener.run());
    }
    public void setSignupButtonListener(Runnable listener) {
        signupButton.setOnAction(e -> listener.run());
    }
}
