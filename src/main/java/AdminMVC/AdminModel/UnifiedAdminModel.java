package AdminMVC.AdminModel;

import AdminMVC.AdminJSONHandler;
import Common.Interfaces.AdminCallbackInterface;
import Common.Interfaces.AdminInterface;
import Common.Objects.ClientObjects.Client;
import Common.Objects.ClientObjects.GroupChat;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.rmi.RemoteException;
import java.util.*;
import java.util.function.Consumer;

public class UnifiedAdminModel {
    private boolean isLoadingData = false;
    private ObservableList<Map<String, String>> allUsers;
    private ObservableList<Map<String, String>> allGroups;
    private ObservableList<String> activeUsers;
    private ObservableList<String> communityMessagesList;

    private AdminJSONHandler adminJSONHandler;

    private Consumer<String> passwordResetListener;
    private Consumer<String> verificationCodeValidListener;
    private Consumer<String> messageListener;
    private Consumer<String> loginResponseListener;
    private Consumer<String> dataUpdateListener;
    private Consumer<String> operationResultListener;
    private Consumer<String> activeUsersUpdateListener;
    private Consumer<String> communityMessagesUpdateListener;
    private Consumer<String> userStatsListener;

    private AdminInterface server;
    private AdminCallbackInterface callback;
    private String adminUsername;

    // Track online users (received from server callbacks)
    private Set<String> onlineUsers;

    public UnifiedAdminModel(AdminInterface server, AdminCallbackInterface callback) {
        this();
        this.server = server;
        this.callback = callback;
    }

    public UnifiedAdminModel() {
        this.adminJSONHandler = new AdminJSONHandler();
        this.allUsers = FXCollections.observableArrayList();
        this.allGroups = FXCollections.observableArrayList();
        this.activeUsers = FXCollections.observableArrayList();
        this.communityMessagesList = FXCollections.observableArrayList();
        this.onlineUsers = new HashSet<>();
    }
    public void setAdminUsername(String username) {
        this.adminUsername = username;
        System.out.println("✓ Admin username set to: " + username);
    }
    // AUTHENTICATION METHODS
    public void login(String username, String password) {
        try {
            if (server == null) {
                System.err.println("Server not connected");
                if (loginResponseListener != null) {
                    Platform.runLater(() -> loginResponseListener.accept("LOGIN_FAILED"));
                }
                return;
            }

            if (!adminJSONHandler.validateUsername(username) || !adminJSONHandler.validatePassword(password)) {
                System.err.println("Invalid admin credentials format");
                if (loginResponseListener != null) {
                    Platform.runLater(() -> loginResponseListener.accept("LOGIN_FAILED"));
                }
                return;
            }

            // **FIX: Store username BEFORE making the RMI call**
            this.adminUsername = username;
            System.out.println("✓ Storing admin username: " + username);

            String jsonString = adminJSONHandler.createLoginJSON(username, password);
            server.logIn(username, jsonString, callback);
        } catch (RemoteException e) {
            System.err.println("Error during login: " + e.getMessage());
            e.printStackTrace();
            if (loginResponseListener != null) {
                Platform.runLater(() -> loginResponseListener.accept("LOGIN_FAILED"));
            }
        }
    }

    public void updateGroupsList(List<Map<String, String>> groups) {
        Platform.runLater(() -> {
            allGroups.clear();
            if (groups != null) {
                allGroups.addAll(groups);
            }

            if (dataUpdateListener != null) {
                dataUpdateListener.accept("GROUPS_UPDATED");
            }
        });
    }

    public void sendAdminCommand(String xmlMessage) {
        try {
            if (server == null) {
                System.err.println("Server not connected");
                if (operationResultListener != null) {
                    Platform.runLater(() -> operationResultListener.accept("Command failed: Server not connected"));
                }
                return;
            }

            if (xmlMessage.contains("ADMIN_REQUEST_PASSWORD_RESET_WITH_CODE")) {
                String content = extractXMLContent(xmlMessage);
                if (content != null && content.contains("![]!")) {
                    String[] parts = content.split("!\\[]!");
                    if (parts.length >= 2) {
                        String username = parts[0];
                        String verificationCode = parts[1];
                    }
                }
            } else if (xmlMessage.contains("ADMIN_RESET_PASSWORD")) {
                String content = extractXMLContent(xmlMessage);
                if (content != null && content.contains("![]!")) {
                    String[] parts = content.split("!\\[]!");
                    if (parts.length >= 2) {
                        String username = parts[0];
                        String newPassword = parts[1];
                        resetAdminPassword(username, newPassword);
                    }
                }
            } else {
                System.err.println("Unknown admin command: " + xmlMessage);
                if (operationResultListener != null) {
                    Platform.runLater(() -> operationResultListener.accept("Unknown command"));
                }
            }

        } catch (Exception e) {
            System.err.println("Error sending admin command: " + e.getMessage());
            e.printStackTrace();
            if (operationResultListener != null) {
                Platform.runLater(() -> operationResultListener.accept("Command failed: " + e.getMessage()));
            }
        }
    }

    public void notifyLoginSuccess() {
        System.out.println(">>> Model: Notifying login success");
        if (loginResponseListener != null) {
            Platform.runLater(() -> {
                loginResponseListener.accept("LOGIN_SUCCESS");
            });
        } else {
            System.err.println("✗ loginResponseListener is null!");
        }
    }

    public void notifyLoginFailure() {
        System.out.println(">>> Model: Notifying login failure");
        if (loginResponseListener != null) {
            Platform.runLater(() -> {
                loginResponseListener.accept("LOGIN_FAILED");
            });
        } else {
            System.err.println("✗ loginResponseListener is null!");
        }
    }

    private String extractXMLContent(String xmlMessage) {
        try {
            int startIndex = xmlMessage.indexOf("<content>") + 9;
            int endIndex = xmlMessage.indexOf("</content>");
            if (startIndex > 8 && endIndex > startIndex) {
                return xmlMessage.substring(startIndex, endIndex);
            }
        } catch (Exception e) {
            System.err.println("Error extracting XML content: " + e.getMessage());
        }
        return null;
    }

    private void resetAdminPassword(String username, String newPassword) throws RemoteException {
        if (server == null || adminJSONHandler == null) return;

        try {
            boolean success = adminJSONHandler.changeAdminPassword(username, newPassword);

            if (success) {
                System.out.println("✓ Admin password reset successful for: " + username);
                if (operationResultListener != null) {
                    Platform.runLater(() -> operationResultListener.accept("ADMIN_PASSWORD_RESET_SUCCESS"));
                }
            } else {
                System.out.println("✗ Admin password reset failed for: " + username);
                if (operationResultListener != null) {
                    Platform.runLater(() -> operationResultListener.accept("PASSWORD_RESET_FAILED"));
                }
            }
        } catch (Exception e) {
            System.err.println("Error resetting admin password: " + e.getMessage());
            e.printStackTrace();
            if (operationResultListener != null) {
                Platform.runLater(() -> operationResultListener.accept("PASSWORD_RESET_FAILED"));
            }
        }
    }

    public void loadData() {
        try {
            if (server == null) {
                System.err.println("Cannot load data: Server not connected");
                return;
            }

            System.out.println("Loading admin data ONE TIME ONLY...");

            // Load all users - this will trigger callbacks
            getAllClientsFromServer();

            // Load all groups - this will trigger callbacks
            getAllGroupsFromServer();

            // DON'T call these here - they're redundant
            // getActiveClientsOnly();
            // getCommunityMessages();

            System.out.println("✓ Admin initial data load requested");

        } catch (RemoteException e) {
            System.err.println("Error loading admin data: " + e.getMessage());
            e.printStackTrace();
            if (operationResultListener != null) {
                Platform.runLater(() -> operationResultListener.accept("Failed to load data: " + e.getMessage()));
            }
        }
    }

    public void logout(String username) throws RemoteException {
        if (server != null) {
            String jsonString = adminJSONHandler.createJSONStringRequest("request", "LOGOUT", username);
            server.logOut(username, jsonString);
            adminUsername = null;
        }
    }

    // USER MANAGEMENT METHODS
    public void getAllClientsFromServer() throws RemoteException {
        if (server == null) return;

        if (isLoadingData) {  // ADD THIS CHECK
            System.out.println("⚠ Already loading data, skipping duplicate request");
            return;
        }

        isLoadingData = true;  // SET FLAG

        System.out.println("DEBUG: adminUsername = '" + adminUsername + "'");
        String jsonString = adminJSONHandler.createJSONStringRequest("request", "GET_ALL_USERS", adminUsername);
        System.out.println("DEBUG: Sending JSON to server: " + jsonString);
        server.searchUsers(jsonString);

        // Reset flag after a short delay
        new Thread(() -> {
            try {
                Thread.sleep(1000);  // Wait 1 second
                isLoadingData = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void getActiveClientsOnly() throws RemoteException {
        if (server == null) return;

        String jsonString = adminJSONHandler.createJSONStringRequest("request", "GET_ACTIVE_USERS", adminUsername);
        server.searchUsers(jsonString);
    }

    public void searchUsers(String keyword) throws RemoteException {
        if (server == null) return;

        Map<String, String> searchData = new HashMap<>();
        searchData.put("keyword", keyword);
        searchData.put("admin", adminUsername);
        String jsonString = new com.google.gson.Gson().toJson(searchData);
        server.searchUsers(jsonString);
    }

    public void searchGroups(String keyword) throws RemoteException {
        if (server == null) return;

        Map<String, String> searchData = new HashMap<>();
        searchData.put("keyword", keyword);
        searchData.put("admin", adminUsername);
        String jsonString = new com.google.gson.Gson().toJson(searchData);
        server.searchGroups(jsonString);
    }

    public void deleteUser(String username) throws RemoteException {
        if (server == null || !adminJSONHandler.validateUsername(username)) {
            throw new RemoteException("Invalid operation");
        }

        Map<String, String> deleteData = new HashMap<>();
        deleteData.put("username", username);
        deleteData.put("admin", adminUsername);
        String jsonString = new com.google.gson.Gson().toJson(deleteData);
        server.deleteUser(jsonString);
    }

    public void deleteGroup(String groupName, String owner) throws RemoteException {
        if (server == null) return;

        Map<String, String> deleteData = new HashMap<>();
        deleteData.put("groupName", groupName);
        deleteData.put("owner", owner);
        deleteData.put("admin", adminUsername);
        String jsonString = new com.google.gson.Gson().toJson(deleteData);
        server.deleteGroup(jsonString);
    }

    public void changeUserPassword(String username, String newPassword) throws RemoteException {
        if (server == null || !adminJSONHandler.validatePassword(newPassword)) {
            throw new RemoteException("Invalid operation");
        }

        Map<String, String> passwordData = new HashMap<>();
        passwordData.put("username", username);
        passwordData.put("newPassword", newPassword);
        passwordData.put("admin", adminUsername);
        String jsonString = new com.google.gson.Gson().toJson(passwordData);
        server.changePassword(jsonString);
    }

    public void verifyAndChangePassword(String username, String verificationCode, String newPassword) throws RemoteException {
        if (server == null) return;

        Map<String, String> verifyData = new HashMap<>();
        verifyData.put("username", username);
        verifyData.put("verificationCode", verificationCode);
        verifyData.put("newPassword", newPassword);
        verifyData.put("admin", adminUsername);
        String jsonString = new com.google.gson.Gson().toJson(verifyData);
        server.verifyCode(jsonString);
    }

    public void rejectPasswordReset(String username) throws RemoteException {
        if (server == null) return;

        Map<String, String> rejectData = new HashMap<>();
        rejectData.put("username", username);
        rejectData.put("admin", adminUsername);
        rejectData.put("action", "REJECT");
        String jsonString = new com.google.gson.Gson().toJson(rejectData);
        server.verifyCode(jsonString);
    }

    // GROUP MANAGEMENT METHODS
    public void getAllGroupsFromServer() throws RemoteException {
        if (server == null) return;

        if (isLoadingData) {  // ADD THIS CHECK
            System.out.println("⚠ Already loading data, skipping duplicate request");
            return;
        }

        String jsonString = adminJSONHandler.createJSONStringRequest("request", "GET_ALL_GROUPS", adminUsername);
        server.searchGroups(jsonString);
    }

    // CHAT HISTORY METHODS
    public void getUserChatHistory(String username) throws RemoteException {
        if (server == null) return;

        Map<String, String> requestData = new HashMap<>();
        requestData.put("username", username);
        requestData.put("admin", adminUsername);
        String jsonString = new com.google.gson.Gson().toJson(requestData);
        server.getChatHistoryOfUser(jsonString);
    }

    public void getGroupChatHistory(String groupName) throws RemoteException {
        if (server == null) return;

        Map<String, String> requestData = new HashMap<>();
        requestData.put("groupName", groupName);
        requestData.put("admin", adminUsername);
        String jsonString = new com.google.gson.Gson().toJson(requestData);
        server.getChatHistoryOfGroup(jsonString);
    }

    public void getCommunityMessages() throws RemoteException {
        if (server == null) return;

        String jsonString = adminJSONHandler.createJSONStringRequest("request", "GET_COMMUNITY_MESSAGES", adminUsername);
        server.getChatHistoryOfCommunity(jsonString);
    }

    public void sendCommunityMessage(String message) throws RemoteException {
        if (server == null || message == null || message.trim().isEmpty()) return;

        Map<String, String> messageData = new HashMap<>();
        messageData.put("sender", adminUsername);
        messageData.put("message", message);
        String jsonString = new com.google.gson.Gson().toJson(messageData);
        //server.sendCommunityMessage(jsonString);
    }

    // DATA UPDATE METHODS - FIXED TO USE AdminJSONHandler
    public void updateUsersList(List<Client> clients) {
        Platform.runLater(() -> {
            allUsers.clear();
            if (clients != null) {
                for (Client client : clients) {
                    Map<String, String> userMap = new HashMap<>();
                    userMap.put("username", client.getUsername());
                    userMap.put("password", client.getPassword());
                    userMap.put("verificationCode", client.getVerificationCode());

                    String status = onlineUsers.contains(client.getUsername()) ? "Online" : "Offline";
                    userMap.put("status", status);

                    userMap.put("friendsCount", client.getFriends() != null ? String.valueOf(client.getFriends().size()) : "0");
                    allUsers.add(userMap);
                }
            }

            if (dataUpdateListener != null) {
                dataUpdateListener.accept("USERS_UPDATED");  // THIS TRIGGERS REFRESH
            }

            System.out.println("✓ Updated users list: " + allUsers.size() + " users");
        });
    }

    public void updateActiveUsersList(List<String> activeUsernames) {
        Platform.runLater(() -> {
            activeUsers.clear();
            onlineUsers.clear();

            if (activeUsernames != null) {
                activeUsers.addAll(activeUsernames);
                onlineUsers.addAll(activeUsernames);
            }

            if (activeUsersUpdateListener != null) {
                activeUsersUpdateListener.accept("ACTIVE_USERS_UPDATED");
            }

            System.out.println("✓ Updated active users: " + onlineUsers.size() + " online");
        });
    }

    // GETTERS
    public AdminCallbackInterface getCallback() {
        return callback;
    }

    public ObservableList<Map<String, String>> getAllUsers() {
        return allUsers;
    }

    public ObservableList<Map<String, String>> getAllGroups() {
        return allGroups;
    }

    public ObservableList<String> getCommunityMessagesList() {
        return communityMessagesList;
    }

    public boolean isConnected() {
        return server != null;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    // LISTENER SETTERS
    public void setLoginResponseListener(Consumer<String> listener) {
        this.loginResponseListener = listener;
    }

    public void setDataUpdateListener(Consumer<String> listener) {
        this.dataUpdateListener = listener;
    }

    public void setOperationResultListener(Consumer<String> listener) {
        this.operationResultListener = listener;
    }

    public void setCommunityMessagesUpdateListener(Consumer<String> listener) {
        this.communityMessagesUpdateListener = listener;
    }

    public void setPasswordResetListener(Consumer<String> listener) {
        this.passwordResetListener = listener;
    }

    public void setVerificationCodeValidListener(Consumer<String> listener) {
        this.verificationCodeValidListener = listener;
    }

    public void setActiveUsersUpdateListener(Consumer<String> listener) {
        this.activeUsersUpdateListener = listener;
    }

    public void setUserStatsListener(Consumer<String> listener) {
        this.userStatsListener = listener;
    }
}