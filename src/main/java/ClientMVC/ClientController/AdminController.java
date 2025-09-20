package ClientMVC.ClientController;

import ClientMVC.ClientModel.AdminModel;
import ClientMVC.ClientView.Admin.AdminPageJFXController;
import ClientMVC.UserXMLHandler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.TextField;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class AdminController {

    private AdminPageJFXController view;
    private ObservableList<AdminModel> allUsers;
    private ObservableList<AdminModel> allGroups;

    public AdminController(AdminPageJFXController view) {
        this.view = view;
        loadData();
        initListeners();
    }

    private void loadData() {
        allUsers = loadUsersFromXML("src/main/resources/XMLFiles/AdminData.xml");
        if (view.getTableView() != null) {
            view.getTableView().setItems(allUsers);
        }

        allGroups = loadGroupsFromXML("src/main/resources/XMLFiles/AdminData.xml");
        if (view.getGroupsTableView() != null) {
            view.getGroupsTableView().setItems(allGroups);
        }
    }

    private void initListeners() {
        // Users search
        if (view.getSearchButton() != null) {
            view.getSearchButton().setOnAction(e -> onSearchUsers());
        }

        // Groups search
        if (view.getGroupSearchButton() != null) {
            view.getGroupSearchButton().setOnAction(e -> onSearchGroups());
        }

        // Navigation
        if (view.getGroupsButton() != null) {
            view.getGroupsButton().setOnAction(e -> switchToGroups());
        }
        if (view.getBackToUsersButton() != null) {
            view.getBackToUsersButton().setOnAction(e -> switchToUsers());
        }

        // Delete buttons
        if (view.getDeleteButton() != null) {
            view.getDeleteButton().setOnAction(e -> onDeleteUser());
        }
        if (view.getGroupDeleteButton() != null) {
            view.getGroupDeleteButton().setOnAction(e -> onDeleteGroup());
        }
        if (view.getEditButton() != null) {
            view.getEditButton().setOnAction(e -> onEditUser());
        }
        if (view.getGroupsEditButton() != null) {
            view.getGroupsEditButton().setOnAction(e -> onEditGroups());
        }
    }

    private void onSearchUsers() {
        String keyword = view.getSearchField().getText();
        view.getTableView().setItems(AdminModel.searchUsers(allUsers, keyword));
    }

    private void onSearchGroups() {
        String keyword = view.getGroupSearchField().getText();
        view.getGroupsTableView().setItems(AdminModel.searchGroups(allGroups, keyword));
    }

    private void switchToGroups() {
        view.getTableView().getScene().getWindow().hide();
        loadFXML("/FXMLFiles/AdminView/AdminGroups.fxml", "Groups Page");
    }

    private void switchToUsers() {
        view.getGroupsTableView().getScene().getWindow().hide();
        loadFXML("/FXMLFiles/AdminView/Admin.fxml", "Users Page");
    }


    private void loadFXML(String path, String title) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(path));
            javafx.scene.Parent root = loader.load();
            AdminPageJFXController newController = loader.getController();
            new AdminController(newController);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setScene(new javafx.scene.Scene(root, 800, 600));
            stage.setTitle(title);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- XML Loaders ---
    private ObservableList<AdminModel> loadUsersFromXML(String filePath) {
        ObservableList<AdminModel> users = FXCollections.observableArrayList();
        try {
            File xmlFile = new File(filePath);
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
            doc.getDocumentElement().normalize();
            NodeList list = doc.getElementsByTagName("user");
            for (int i = 0; i < list.getLength(); i++) {
                Element elem = (Element) list.item(i);
                String username = elem.getElementsByTagName("username").item(0).getTextContent();
                String password = elem.getElementsByTagName("password").item(0).getTextContent();
                users.add(new AdminModel(username, password));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return users;
    }

    private ObservableList<AdminModel> loadGroupsFromXML(String filePath) {
        ObservableList<AdminModel> groups = FXCollections.observableArrayList();
        try {
            File xmlFile = new File(filePath);
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
            doc.getDocumentElement().normalize();
            NodeList list = doc.getElementsByTagName("group");
            for (int i = 0; i < list.getLength(); i++) {
                Element elem = (Element) list.item(i);
                String owner = elem.getElementsByTagName("owner").item(0).getTextContent();
                String groupName = elem.getElementsByTagName("groupName").item(0).getTextContent();
                groups.add(new AdminModel(owner, groupName, true));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return groups;
    }

    private void onDeleteUser() {
        AdminModel selected = view.getTableView().getSelectionModel().getSelectedItem();
        if (selected != null) {
            String username = selected.getUsername();

            boolean success = UserXMLHandler.deleteUserFromXML(
                    "src/main/resources/XMLFiles/AdminData.xml",
                    username
            );

            if (success) {
                // Remove user from table
                view.getTableView().getItems().remove(selected);

                // Also remove groups owned by this user
                List<AdminModel> ownedGroups = allGroups.stream()
                        .filter(g -> g.getOwner().equals(username))
                        .collect(Collectors.toList());

                for (AdminModel g : ownedGroups) {
                    boolean groupDeleted = UserXMLHandler.deleteGroupFromXML(
                            "src/main/resources/XMLFiles/AdminData.xml",
                            g.getGroupName(),
                            g.getOwner() // ✅ pass owner too
                    );
                    if (groupDeleted) {
                        view.getGroupsTableView().getItems().remove(g);
                    }
                }
            }
        }
    }

    private void onDeleteGroup() {
        AdminModel selected = view.getGroupsTableView().getSelectionModel().getSelectedItem();
        if (selected != null) {
            boolean success = UserXMLHandler.deleteGroupFromXML(
                    "src/main/resources/XMLFiles/AdminData.xml",
                    selected.getGroupName(),
                    selected.getOwner() // ✅ now uses groupName + owner
            );

            if (success) {
                view.getGroupsTableView().getItems().remove(selected);
            }
        }
    }
    private void onEditUser() {
        AdminModel selected = view.getTableView().getSelectionModel().getSelectedItem();

        if (selected == null) {
            // No row selected
            System.out.println("⚠ Please select a user first.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXMLFiles/AdminView/UserEdit.fxml"));
            Parent root = loader.load();

            // Find fields from UserEdit.fxml
            TextField usernameField = (TextField) root.lookup("#usernameField");
            TextField passwordField = (TextField) root.lookup("#passwordField");

            // Pre-fill with selected user’s data
            if (usernameField != null) usernameField.setText(selected.getUsername());
            if (passwordField != null) passwordField.setText(selected.getPassword());

            Stage stage = new Stage();
            stage.setTitle("Edit User");
            stage.setScene(new Scene(root, 400, 250));
            stage.showAndWait();

            // After closing, update table
            view.getTableView().refresh();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void onEditGroups() {
        AdminModel selected = view.getGroupsTableView().getSelectionModel().getSelectedItem();

        if (selected == null) {
            // No row selected
            System.out.println("⚠ Please select a group first.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXMLFiles/AdminView/GroupsEdit.fxml"));
            Parent root = loader.load();

            // Find fields in EditGroups.fxml
            TextField ownerField = (TextField) root.lookup("#ownerField");
            TextField groupsField = (TextField) root.lookup("#groupsField");

            // Pre-fill with selected group data
            if (ownerField != null) ownerField.setText(selected.getOwner());
            if (groupsField != null) groupsField.setText(selected.getGroupName());

            Stage stage = new Stage();
            stage.setTitle("Edit Group");
            stage.setScene(new Scene(root, 400, 250));
            stage.showAndWait();

            // After closing, refresh groups table
            view.getGroupsTableView().refresh();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
