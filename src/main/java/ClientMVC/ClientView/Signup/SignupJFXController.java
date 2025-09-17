package ClientMVC.ClientView.Signup;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SignupJFXController {

    @FXML
    private Hyperlink signInLink;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button signupButton;

    @FXML
    private CheckBox termsCheckbox;

    private Runnable signInListener;
    private Runnable signupButtonListener;

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
    public void setSignupButtonListener(Runnable listener) {
        this.signupButtonListener = listener;
    }

    public void setSignInListener(Runnable listener) {
        this.signInListener = listener;
    }

    public String getUsername() {
        return usernameField.getText();
    }

    public String getFirstName() {
        return firstNameField.getText();
    }

    public String getLastName() {
        return lastNameField.getText();
    }

    public String getEmail() {
        return emailField.getText();
    }

    public String getPassword() {
        return passwordField.getText();
    }

    public boolean isTermsAccepted() {
        return termsCheckbox.isSelected();
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
