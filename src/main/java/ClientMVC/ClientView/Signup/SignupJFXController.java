package ClientMVC.ClientView.Signup;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SignupJFXController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField emailField;

    @FXML
    private Button signupButton;

    @FXML
    private CheckBox termsCheckbox;

    @FXML
    private void initialize() {
        signupButton.setOnAction(event -> handleSignup());
    }

    private void handleSignup() {
        String username = usernameField.getText();
        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String email = emailField.getText();

        if (!termsCheckbox.isSelected()) {
            showAlert("Please agree to the Terms of Service.");
            return;
        }

        if (username.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            showAlert("Please fill in all fields.");
            return;
        }

        // For now just show a confirmation
        showAlert("Signup successful!\nWelcome, " + firstName + "!");
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Signup");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}