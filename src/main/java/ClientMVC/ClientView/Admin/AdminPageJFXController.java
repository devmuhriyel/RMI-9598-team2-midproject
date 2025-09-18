package ClientMVC.ClientView.Admin;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class AdminPageJFXController {

    @FXML
    private TableView<?> tableView;

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
    private Button deleteButton;

    @FXML
    private Button editButton;

    @FXML
    private Button groupsButton;   // 🔹 button in Admin.fxml to open Groups.fxml

    @FXML
    private Button backToUsersButton;     // 🔹 button in Groups.fxml to go back to Admin.fxml

    @FXML
    private void initialize() {
        if (searchButton != null) {
            searchButton.setOnAction(e -> {
                System.out.println("Searching for: " + searchField.getText());
            });
        }

        if (groupsButton != null) {
            groupsButton.setOnAction(e -> openGroupsPage());
        }

        if (backToUsersButton != null) {
            backToUsersButton.setOnAction(e -> goBackToUsersPage());
        }
    }

    private void openGroupsPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXMLFiles/AdminGroups.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) groupsButton.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("AdminGroups Page");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void goBackToUsersPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXMLFiles/Admin.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) backToUsersButton.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Admin Page");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Getters
    public Button getDeleteButton(){ return deleteButton; }
    public Button getEditButton(){ return editButton; }
    public TableView<?> getTableView(){ return tableView; }

    public String getUsername() { return usernameField.getText(); }
    public String getNewPassword() { return newPasswordField.getText(); }
    public String getConfirmPassword() { return confirmPasswordField.getText(); }
}
