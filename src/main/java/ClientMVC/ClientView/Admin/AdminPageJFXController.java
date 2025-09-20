package ClientMVC.ClientView.Admin;

import ClientMVC.ClientModel.AdminModel;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class AdminPageJFXController {

    // Users Table
    @FXML private TableView<AdminModel> tableView;
    @FXML private TableColumn<AdminModel, String> usernameCol;
    @FXML private TableColumn<AdminModel, String> passwordCol;

    // Groups Table
    @FXML private TableView<AdminModel> groupsTableView;
    @FXML private TableColumn<AdminModel, String> groupOwnerCol;
    @FXML private TableColumn<AdminModel, String> groupCol;

    // Users form fields
    @FXML private TextField searchField;
    @FXML private Button searchButton;

    // Delete Buttons
    @FXML private Button deleteButton;
    @FXML private Button groupDeleteButton;

    // Groups form fields
    @FXML private TextField groupSearchField;
    @FXML private Button groupSearchButton;

    // Navigation
    @FXML private Button groupsButton;        // Switch to Groups
    @FXML private Button backToUsersButton;   // Switch to Users

    @FXML private Button editButton;

    @FXML private Button groupEditButton;

    @FXML
    private void initialize() {
        // Users Table setup
        if (tableView != null) {
            usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
            passwordCol.setCellValueFactory(new PropertyValueFactory<>("password"));
        }

        // Groups Table setup
        if (groupsTableView != null) {
            groupOwnerCol.setCellValueFactory(new PropertyValueFactory<>("owner"));
            groupCol.setCellValueFactory(new PropertyValueFactory<>("groupName"));
        }
    }

    // --- Getters for Controller ---
    public TableView<AdminModel> getTableView() {
        return tableView;
    }
    public TableView<AdminModel> getGroupsTableView() {
        return groupsTableView;
    }
    public TextField getSearchField() {
        return searchField;
    }
    public Button getSearchButton() {
        return searchButton;
    }
    public TextField getGroupSearchField() {
        return groupSearchField;
    }
    public Button getGroupSearchButton() {
        return groupSearchButton;
    }
    public Button getGroupsButton() {
        return groupsButton;
    }
    public Button getBackToUsersButton() {
        return backToUsersButton;
    }
    public Button getDeleteButton() {
        return deleteButton;
    }
    public Button getGroupDeleteButton() {
        return groupDeleteButton;
    }
    public Button getEditButton() {
        return editButton;
    }
    public Button getGroupsEditButton() {
        return groupEditButton;
    }
}
