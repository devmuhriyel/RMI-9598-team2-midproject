package AdminMVC.AdminView;

import ClientMVC.ClientModel.AdminModel;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class AdminPageJFXController {

    // Users Table
    @FXML private TableView<AdminModel> usersTable;
    @FXML private TableColumn<AdminModel, String> usernameCol;
    @FXML private TableColumn<AdminModel, String> passwordCol;

    // Groups Table
    @FXML private TableView<AdminModel> groupsTable;
    @FXML private TableColumn<AdminModel, String> groupOwnerCol;
    @FXML private TableColumn<AdminModel, String> groupNameCol;

    // Search field
    @FXML private TextField searchField;
    @FXML private Button searchButton;

    // Main action buttons (matching FXML fx:id names)
    @FXML private Button AddButton;              // Add User
    @FXML private Button ActiveButton;           // Active Users
    @FXML private Button CreateButton;           // Create Group
    @FXML private Button CommunityButton;        // Community Messages
    @FXML private Button refreshButton;          // Refresh

    // Edit and Delete buttons for users
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    // Edit and Delete buttons for groups
    @FXML private Button editGroupButton;
    @FXML private Button deleteGroupButton;

    @FXML
    private void initialize() {
        // Users Table setup
        if (usersTable != null) {
            usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
            passwordCol.setCellValueFactory(new PropertyValueFactory<>("password"));
        }

        // Groups Table setup
        if (groupsTable != null) {
            groupOwnerCol.setCellValueFactory(new PropertyValueFactory<>("owner"));
            groupNameCol.setCellValueFactory(new PropertyValueFactory<>("groupName"));
        }
    }

    // --- Getters for Controller (Updated to match FXML) ---

    // Table getters
    public TableView<AdminModel> getTableView() { return usersTable; }
    public TableView<AdminModel> getGroupsTableView() { return groupsTable; }

    // Search getters
    public TextField getSearchField() { return searchField; }
    public Button getSearchButton() { return searchButton; }

    // Main action button getters (mapped to AdminController expected names)
    public Button getCreateUserButton() { return AddButton; }              // Add User -> Create User
    public Button getViewActiveUsersButton() { return ActiveButton; }      // Active Users
    public Button getCreateGroupButton() { return CreateButton; }          // Create Group
    public Button getViewCommunityMessagesButton() { return CommunityButton; } // Community Messages
    public Button getRefreshButton() { return refreshButton; }             // Refresh

    // Edit and Delete button getters for users
    public Button getEditButton() { return editButton; }
    public Button getDeleteButton() { return deleteButton; }

    // Edit and Delete button getters for groups
    public Button getGroupsEditButton() { return editGroupButton; }
    public Button getGroupDeleteButton() { return deleteGroupButton; }

    // Getters that return null for buttons not in FXML (to avoid null pointer exceptions)
    public TextField getGroupSearchField() { return null; }
    public Button getGroupSearchButton() { return null; }
    public Button getGroupsButton() { return null; }
    public Button getBackToUsersButton() { return null; }
    public Button getViewStatsButton() { return null; }
}