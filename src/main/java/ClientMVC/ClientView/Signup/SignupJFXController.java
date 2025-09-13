package ClientMVC.ClientView.Signup;

import Server.ServerXMLHandler; // import your XML handler
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
    private PasswordField passwordField; //add this if you want password input

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
        String password = passwordField.getText(); // collect password

        // Check Terms
        if (!termsCheckbox.isSelected()) {
            showAlert("Please agree to the Terms of Service.");
            return;
        }

        // Validate inputs
        if (username.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert("Please fill in all fields.");
            return;
        }

        // Save user into XML
        try {
//            UserXmlHandler.registerUser(username, firstName, lastName, password);
            showAlert("Signup successful!\nWelcome, " + firstName + "!");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to register user.");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Signup");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
