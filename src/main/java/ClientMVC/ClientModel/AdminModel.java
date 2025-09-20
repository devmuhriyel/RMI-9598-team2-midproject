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
    // flag to distinguish between User and Group
    private boolean isGroup;


    // --- User Constructor ---
    public AdminModel(String username, String password) {
        this.username = username;
        this.password = password;
        this.isGroup = false;
    }

    // --- Getters ---
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public String getOwner() {
        return owner;
    }
    public String getGroupName() {
        return groupName;
    }
    public boolean isGroup() {
        return isGroup;
    }

    // --- Setters (if needed) ---
    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setOwner(String owner) {
        this.owner = owner; }
    public void setGroupName(String groupName) {
        this.groupName = groupName; }

    /*
     * searchUsers method: and searchGroups method:
     * 1. If the keyword is null or empty → return all users.
     * 2. Otherwise, convert the list into a stream for processing.
     * 3. Check if the username contains the keyword (case-insensitive).
     * 4. Or check if the password contains the keyword (case-insensitive).
     * 5. Collect all matching users and return them as an ObservableList.
     * 6. Collect all matching groups and return them as an ObservableList.
     */
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
