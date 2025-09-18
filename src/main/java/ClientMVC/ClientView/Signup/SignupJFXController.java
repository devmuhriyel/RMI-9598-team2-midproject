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

    public void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Signup Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Signup Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public Button getSignupButton() {
        return signupButton;
    }

    // Clear all fields method
    public void clearFields() {
        usernameField.clear();
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        passwordField.clear();
        termsCheckbox.setSelected(false);
    }

    // Enable/disable form
    public void setFormEnabled(boolean enabled) {
        usernameField.setDisable(!enabled);
        firstNameField.setDisable(!enabled);
        lastNameField.setDisable(!enabled);
        emailField.setDisable(!enabled);
        passwordField.setDisable(!enabled);
        termsCheckbox.setDisable(!enabled);
        signupButton.setDisable(!enabled);
        signInLink.setDisable(!enabled);
    }
}