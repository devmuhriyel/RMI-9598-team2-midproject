package ClientMVC.ClientController;

import ClientMVC.ClientModel.AdminModel;
import ClientMVC.ClientView.Admin.AdminPageJFXController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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
        // Load AdminGroups.fxml
        loadFXML("/FXMLFiles/AdminGroups.fxml", "Groups Page");
    }

    private void switchToUsers() {
        view.getGroupsTableView().getScene().getWindow().hide();
        loadFXML("/FXMLFiles/Admin.fxml", "Users Page");
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
}
