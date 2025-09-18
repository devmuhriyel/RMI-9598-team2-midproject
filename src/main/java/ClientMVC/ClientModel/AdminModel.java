package ClientMVC.ClientModel;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.stream.Collectors;

public class AdminModel {
    private String username;
    private String password;
    private String owner;
    private String groupName;

    public AdminModel() {}

    // User constructor
    public AdminModel(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Group constructor
    public AdminModel(String owner, String groupName, boolean isGroup) {
        this.owner = owner;
        this.groupName = groupName;
    }

    // --- Getters ---
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getOwner() { return owner; }
    public String getGroupName() { return groupName; }

    // --- Search Methods ---
    public static ObservableList<AdminModel> searchUsers(List<AdminModel> allUsers, String keyword) {
        if (keyword == null || keyword.isEmpty()) return FXCollections.observableArrayList(allUsers);
        return FXCollections.observableArrayList(
                allUsers.stream()
                        .filter(u -> u.getUsername().toLowerCase().contains(keyword.toLowerCase()) ||
                                u.getPassword().toLowerCase().contains(keyword.toLowerCase()))
                        .collect(Collectors.toList())
        );
    }

    public static ObservableList<AdminModel> searchGroups(List<AdminModel> allGroups, String keyword) {
        if (keyword == null || keyword.isEmpty()) return FXCollections.observableArrayList(allGroups);
        return FXCollections.observableArrayList(
                allGroups.stream()
                        .filter(g -> g.getOwner().toLowerCase().contains(keyword.toLowerCase()) ||
                                g.getGroupName().toLowerCase().contains(keyword.toLowerCase()))
                        .collect(Collectors.toList())
        );
    }
}
