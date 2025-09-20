package ClientMVC.ClientView.Login;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginJFXController {

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
    }

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
}
