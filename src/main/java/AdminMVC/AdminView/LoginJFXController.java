package AdminMVC.AdminView;

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
    private ToggleButton loginTab;

    @FXML
    private ToggleButton signupTab;

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

    public Hyperlink getForgotPasswordLink() {
        return forgotPasswordLink;
    }

    public ToggleButton getLoginTab() {
        return loginTab;
    }

    public ToggleButton getSignupTab() {
        return signupTab;
    }

    public void setSignupLinkListener(Runnable listener) {
        this.signupLinkListener = listener;
    }

    public void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Login");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
