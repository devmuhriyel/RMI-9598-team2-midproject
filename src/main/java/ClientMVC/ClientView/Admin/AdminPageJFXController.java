package ClientMVC.ClientView.Admin;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AdminPageJFXController {

    @FXML
    private TableView<?> tableView;

    @FXML
    private TableColumn<?, ?> ownerCol;

    @FXML
    private TableColumn<?, ?> groupChatCol;

    @FXML
    private TableColumn<?, ?> configCol;

    @FXML
    private TextField searchField;

    @FXML
    private Button searchButton;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private void initialize() {
        searchButton.setOnAction(e -> {
            System.out.println("Searching for: " + searchField.getText());
        });
    }

    public String getUsername() {
        return usernameField.getText();
    }

    public String getNewPassword() {
        return newPasswordField.getText();
    }

    public String getConfirmPassword() {
        return confirmPasswordField.getText();
    }
}
