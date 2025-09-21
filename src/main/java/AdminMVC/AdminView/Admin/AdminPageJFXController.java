package AdminMVC.AdminView.Admin;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AdminPageJFXController {

    // Users Table
    @FXML private TableView<Map<String, String>> usersTable;
    @FXML private TableColumn<Map<String, String>, String> usernameCol;
    @FXML private TableColumn<Map<String, String>, String> passwordCol;
    @FXML private TableColumn<Map<String, String>, String> statusCol; // Add status column

    // Groups Table
    @FXML private TableView<Map<String, String>> groupsTable;
    @FXML private TableColumn<Map<String, String>, String> groupOwnerCol;
    @FXML private TableColumn<Map<String, String>, String> groupNameCol;
    @FXML private TableColumn<Map<String, String>, String> memberCountCol; // Add member count column

    // Search Components
    @FXML private TextField searchField;
    @FXML private Button searchButton;

    // Action Buttons (matching FXML fx:id names)
    @FXML private Button AddButton;        // Add User (original FXML name)
    @FXML private Button ActiveButton;     // Active Users (original FXML name)
    @FXML private Button CreateButton;     // Create Group (original FXML name)
    @FXML private Button CommunityButton;  // Community Messages (original FXML name)
    @FXML private Button refreshButton;    // Refresh

    // User Management Buttons
    @FXML private Button editButton;       // Edit User
    @FXML private Button deleteButton;     // Delete User

    // Group Management Buttons
    @FXML private Button editGroupButton;   // Edit Group
    @FXML private Button deleteGroupButton; // Delete Group

    // Original data for search filtering
    private List<Map<String, String>> originalUsers = Collections.emptyList();
    private List<Map<String, String>> originalGroups = Collections.emptyList();
    private List<String> passwordResetRequests = new ArrayList<>();

    // Display mode tracking
    private boolean showingActiveUsersOnly = false;

    // Search listener
    private Consumer<String> searchListener;

    @FXML
    private void initialize() {
        // Users Table setup
        if (usersTable != null && usernameCol != null && passwordCol != null && statusCol != null) {
            // Custom cell value factories for Map<String, String>
            usernameCol.setCellValueFactory(cellData -> {
                Map<String, String> user = cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(user.get("username"));
            });

            passwordCol.setCellValueFactory(cellData -> {
                Map<String, String> user = cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(user.get("password"));
            });

            statusCol.setCellValueFactory(cellData -> {
                Map<String, String> user = cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(user.get("status"));
            });

            // Style status column cells based on online status
            statusCol.setCellFactory(column -> new TableCell<Map<String, String>, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        if ("Online".equals(item)) {
                            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: red;");
                        }
                    }
                }
            });
        }

        // Groups Table setup
        if (groupsTable != null && groupOwnerCol != null && groupNameCol != null && memberCountCol != null) {
            // Custom cell value factories for Map<String, String>
            groupOwnerCol.setCellValueFactory(cellData -> {
                Map<String, String> group = cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(group.get("username")); // owner
            });

            groupNameCol.setCellValueFactory(cellData -> {
                Map<String, String> group = cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(group.get("groupName"));
            });

            memberCountCol.setCellValueFactory(cellData -> {
                Map<String, String> group = cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(group.get("memberCount"));
            });
        }

        // Search field listener
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (searchListener != null) {
                    searchListener.accept(newValue);
                }
            });
        }
    }

    // --- Search Methods ---
    public void setSearchListener(Consumer<String> listener) {
        this.searchListener = listener;
    }

    public void setUsers(List<Map<String, String>> users) {
        setUsers(users, false);
    }

    public void setUsers(List<Map<String, String>> users, boolean activeOnly) {
        if (users == null) {
            originalUsers = Collections.emptyList();
        } else {
            originalUsers = new ArrayList<>(users);
        }
        this.showingActiveUsersOnly = activeOnly;
        renderUsers(originalUsers);
    }

    public void setGroups(List<Map<String, String>> groups) {
        if (groups == null) {
            originalGroups = Collections.emptyList();
        } else {
            // Remove duplicates - keep only unique group names
            List<Map<String, String>> uniqueGroups = new ArrayList<>();
            List<String> seenGroupNames = new ArrayList<>();

            for (Map<String, String> group : groups) {
                String groupName = group.get("groupName");
                if (!seenGroupNames.contains(groupName)) {
                    seenGroupNames.add(groupName);
                    uniqueGroups.add(group);
                }
            }
            originalGroups = uniqueGroups;
        }
        renderGroups(originalGroups);
    }

    public List<Map<String, String>> getOriginalUsers() {
        return originalUsers;
    }

    public List<Map<String, String>> getOriginalGroups() {
        return originalGroups;
    }

    public void renderUsers(List<Map<String, String>> users) {
        if (usersTable != null) {
            usersTable.getItems().clear();
            if (users != null && !users.isEmpty()) {
                usersTable.getItems().addAll(users);
            }
        }
    }

    public void renderGroups(List<Map<String, String>> groups) {
        if (groupsTable != null) {
            groupsTable.getItems().clear();
            if (groups != null && !groups.isEmpty()) {
                groupsTable.getItems().addAll(groups);
            }
        }
    }

    // --- Password Reset Methods ---
    public void addPasswordResetRequest(String username, String verificationCode) {
        String request = username + ":" + verificationCode;
        if (!passwordResetRequests.contains(request)) {
            passwordResetRequests.add(request);
        }
    }

    public void removePasswordResetRequest(String username) {
        passwordResetRequests.removeIf(request -> request.startsWith(username + ":"));
    }

    public List<String> getPasswordResetRequests() {
        return new ArrayList<>(passwordResetRequests);
    }

    public String getVerificationCodeForUser(String username) {
        for (String request : passwordResetRequests) {
            String[] parts = request.split(":");
            if (parts.length == 2 && parts[0].equals(username)) {
                return parts[1];
            }
        }
        return null;
    }

    // --- Display Mode Methods ---
    public boolean isShowingActiveUsersOnly() {
        return showingActiveUsersOnly;
    }

    public void setShowingActiveUsersOnly(boolean activeOnly) {
        this.showingActiveUsersOnly = activeOnly;
    }

    // --- Getters for Controller ---
    public TableView<Map<String, String>> getUsersTable() {
        return usersTable;
    }

    public TableView<Map<String, String>> getGroupsTable() {
        return groupsTable;
    }

    public TextField getSearchField() {
        return searchField;
    }

    public Button getSearchButton() {
        return searchButton;
    }

    // Action Button Getters (matching FXML names)
    public Button getAddButton() {
        return AddButton;
    }

    public Button getActiveButton() {
        return ActiveButton;
    }

    public Button getCreateButton() {
        return CreateButton;
    }

    public Button getCommunityButton() {
        return CommunityButton;
    }

    public Button getRefreshButton() {
        return refreshButton;
    }

    // User Management Button Getters
    public Button getEditButton() {
        return editButton;
    }

    public Button getDeleteButton() {
        return deleteButton;
    }

    // Group Management Button Getters
    public Button getEditGroupButton() {
        return editGroupButton;
    }

    public Button getDeleteGroupButton() {
        return deleteGroupButton;
    }

    // Selection Methods
    public Map<String, String> getSelectedUser() {
        return usersTable != null ? usersTable.getSelectionModel().getSelectedItem() : null;
    }

    public Map<String, String> getSelectedGroup() {
        return groupsTable != null ? groupsTable.getSelectionModel().getSelectedItem() : null;
    }

    // Utility Methods
    public void clearSelection() {
        if (usersTable != null) {
            usersTable.getSelectionModel().clearSelection();
        }
        if (groupsTable != null) {
            groupsTable.getSelectionModel().clearSelection();
        }
    }

    public void clearSearch() {
        if (searchField != null) {
            searchField.clear();
        }
    }

    // Method to show notification for password reset requests
    public void showPasswordResetNotification(String username) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Password Reset Request");
        alert.setHeaderText("User Password Reset Request");
        alert.setContentText("User '" + username + "' has requested a password reset.\nVerification code has been generated.");
        alert.showAndWait();
    }
}