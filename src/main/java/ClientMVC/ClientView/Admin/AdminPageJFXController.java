package ClientMVC.ClientView.Admin;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

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
    private Button deleteButton;

    @FXML
    private Button editButton;

    @FXML
    private TabPane tabPane;

    @FXML
    private BorderPane usersPane;

    @FXML
    private BorderPane groupsPane;

    @FXML
    private Button groupDeleteButton;

    @FXML
    private Button groupEditButton;

    @FXML
    private void initialize() {
        searchButton.setOnAction(e -> {
            System.out.println("Searching for: " + searchField.getText());
        });

        if (tabPane != null) {
            tabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldIdx, newIdx) -> {
                boolean showUsers = newIdx.intValue() == 0;
                usersPane.setVisible(showUsers);
                usersPane.setManaged(showUsers);
                groupsPane.setVisible(!showUsers);
                groupsPane.setManaged(!showUsers);
            });
        }
    }

    public Button getDeleteButton(){ return deleteButton; }
    public Button getEditButton(){ return editButton; }
    public TableView<?> getTableView(){ return tableView; }

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