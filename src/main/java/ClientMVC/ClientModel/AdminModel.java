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
    private boolean isGroup; // flag to distinguish between User and Group

    public AdminModel() {}

    // --- User Constructor ---
    public AdminModel(String username, String password) {
        this.username = username;
        this.password = password;
        this.isGroup = false;
    }

    // --- Group Constructor ---
    public AdminModel(String owner, String groupName, boolean isGroup) {
        this.owner = owner;
        this.groupName = groupName;
        this.isGroup = true;
    }

    // --- Getters ---
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getOwner() { return owner; }
    public String getGroupName() { return groupName; }
    public boolean isGroup() { return isGroup; }

    // --- Setters (if needed) ---
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setOwner(String owner) { this.owner = owner; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    // --- Search Users ---
    public static ObservableList<AdminModel> searchUsers(List<AdminModel> allUsers, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return FXCollections.observableArrayList(allUsers);
        }
        return FXCollections.observableArrayList(
                allUsers.stream()
                        .filter(u -> u.getUsername().toLowerCase().contains(keyword.toLowerCase())
                                || u.getPassword().toLowerCase().contains(keyword.toLowerCase()))
                        .collect(Collectors.toList())
        );
    }

    public static ObservableList<AdminModel> searchGroups(List<AdminModel> allGroups, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return FXCollections.observableArrayList(allGroups);
        }
        return FXCollections.observableArrayList(
                allGroups.stream()
                        .filter(g -> g.getOwner().toLowerCase().contains(keyword.toLowerCase())
                                || g.getGroupName().toLowerCase().contains(keyword.toLowerCase()))
                        .collect(Collectors.toList())
        );
    }
}
