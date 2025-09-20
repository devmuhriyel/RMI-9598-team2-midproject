package AdminMVC.AdminModel;

import ClientMVC.ClientModel.AdminModel;
import AdminMVC.AdminXMLHandler;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class UnifiedAdminModel implements Runnable {
    // Connection properties
    private volatile Socket socket;
    private volatile String ip;
    private volatile int port;
    private volatile boolean connected;
    private final ReentrantLock connectionLock = new ReentrantLock();

    // Communication properties
    private BufferedReader readMSG;
    private BufferedWriter writeMSG;
    private String adminUsername;
    private volatile boolean shouldStop = false;

    // Data properties
    private ObservableList<AdminModel> allUsers;
    private ObservableList<AdminModel> allGroups;
    // **CHANGED: Added new data containers for admin operations**
    private ObservableList<String> activeUsers;
    private ObservableList<String> communityMessages;
    private static ArrayList<UnifiedAdminModel> adminClients = new ArrayList<>();

    // XML Handler
    private AdminXMLHandler adminXMLHandler;

    // Listeners
    private Consumer<String> messageListener;
    private Consumer<String> loginResponseListener;
    private Consumer<String> dataUpdateListener;
    private Consumer<String> operationResultListener;
    // **CHANGED: Added new listeners for admin-specific operations**
    private Consumer<String> activeUsersUpdateListener;
    private Consumer<String> communityMessagesUpdateListener;
    private Consumer<String> userStatsListener;

    // Connection Status Enum
    public enum ConnectionStatus {
        SUCCESS,
        EMPTY_FIELDS,
        INVALID_PORT_FORMAT,
        INVALID_IP_FORMAT,
        INVALID_PORT_RANGE,
        CONNECTION_FAILED
    }

    public static class ConnectionResult {
        private final ConnectionStatus status;
        private final String message;

        public ConnectionResult(ConnectionStatus status, String message) {
            this.status = status;
            this.message = message;
        }

        public ConnectionStatus getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }

    public UnifiedAdminModel(Socket socket) throws IOException {
        this();
        this.socket = socket;
        this.readMSG = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writeMSG = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        adminClients.add(this);
        this.connected = true;
    }
    public UnifiedAdminModel() {
        this.adminXMLHandler = new AdminXMLHandler();
        this.allUsers = FXCollections.observableArrayList();
        this.allGroups = FXCollections.observableArrayList();
        // **CHANGED: Initialize new data containers**
        this.activeUsers = FXCollections.observableArrayList();
        this.communityMessages = FXCollections.observableArrayList();
    }

    // ========== CONNECTION METHODS ==========

    public ConnectionResult connect(String ip, String portText) {
        connectionLock.lock();
        try {
            // Reset state before new attempt
            this.connected = false;
            if (this.socket != null && !this.socket.isClosed()) {
                try {
                    this.socket.close();
                } catch (IOException e) {
                }
            }
            this.socket = null;

            if (ip == null || ip.trim().isEmpty()) {
                return new ConnectionResult(ConnectionStatus.EMPTY_FIELDS, "IP address cannot be empty");
            }
            if (portText == null || portText.trim().isEmpty()) {
                return new ConnectionResult(ConnectionStatus.EMPTY_FIELDS, "Port cannot be empty");
            }
            ip = ip.trim();
            portText = portText.trim();
            int port;
            try {
                port = Integer.parseInt(portText);
            } catch (NumberFormatException e) {
                return new ConnectionResult(ConnectionStatus.INVALID_PORT_FORMAT, "Port must be a valid number");
            }

            // Validate port range
            if (port < 1024 || port > 65535) {
                return new ConnectionResult(ConnectionStatus.INVALID_PORT_RANGE, "Port must be between 1024 and 65535");
            }

            // Validate IP format
            String ipv4Pattern = "^(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)" +
                    "(\\.(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)){3}$";

            if (!ip.matches(ipv4Pattern)) {
                return new ConnectionResult(ConnectionStatus.INVALID_IP_FORMAT, "Invalid IP address format");
            }

            try {
                socket = new Socket(ip, port);
                this.readMSG = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.writeMSG = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                this.connected = true;
                this.ip = ip;
                this.port = port;
                adminClients.add(this);
                return new ConnectionResult(ConnectionStatus.SUCCESS, "Connected successfully");
            } catch (IOException e) {
                this.connected = false;
                this.socket = null;
                return new ConnectionResult(ConnectionStatus.CONNECTION_FAILED,
                        "Failed to connect: " + e.getMessage());
            }
        } finally {
            connectionLock.unlock();
        }
    }

    // ========== LOGIN METHODS ==========

    public void login(String username, String password) {
        try {
            if (!adminXMLHandler.validateAdminCredentials(username, password)) {
                System.err.println("Invalid admin credentials format");
                if (loginResponseListener != null) {
                    Platform.runLater(() -> loginResponseListener.accept("LOGIN_FAILED"));
                }
                return;
            }

            String message = adminXMLHandler.createAdminLoginRequest(username, password);
            writeMSG.write(message);
            writeMSG.newLine();
            writeMSG.flush();
            adminUsername = username;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ========== ADMIN DATA METHODS ==========

    /**
     * **CHANGED: Updated to load all admin data including active users and community messages**
     */
    public void loadData() {
        requestAllUsers();
        requestAllGroups();
        requestActiveUsers();
        requestCommunityMessages();
    }

    public void requestAllUsers() {
        try {
            if (writeMSG != null && isConnected()) {
                String request = adminXMLHandler.createGetAllUsersRequest();
                writeMSG.write(request);
                writeMSG.newLine();
                writeMSG.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestAllGroups() {
        try {
            if (writeMSG != null && isConnected()) {
                String request = adminXMLHandler.createGetAllGroupsRequest();
                writeMSG.write(request);
                writeMSG.newLine();
                writeMSG.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // **CHANGED: Added new admin-specific data request methods**
    public void requestActiveUsers() {
        try {
            if (writeMSG != null && isConnected()) {
                String request = adminXMLHandler.createGetActiveUsersRequest();
                writeMSG.write(request);
                writeMSG.newLine();
                writeMSG.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestCommunityMessages() {
        try {
            if (writeMSG != null && isConnected()) {
                String request = adminXMLHandler.createGetCommunityMessagesRequest();
                writeMSG.write(request);
                writeMSG.newLine();
                writeMSG.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ========== SEARCH METHODS ==========

    public void searchUsers(String keyword) {
        try {
            if (writeMSG != null && isConnected()) {
                String request = adminXMLHandler.createSearchUsersRequest(keyword);
                writeMSG.write(request);
                writeMSG.newLine();
                writeMSG.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void searchGroups(String keyword) {
        try {
            if (writeMSG != null && isConnected()) {
                String request = adminXMLHandler.createSearchGroupsRequest(keyword);
                writeMSG.write(request);
                writeMSG.newLine();
                writeMSG.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ========== ADMIN OPERATIONS METHODS ==========

    public void deleteUser(String username) {
        if (!adminXMLHandler.validateUsername(username)) {
            System.err.println("Invalid username format: " + username);
            return;
        }

        try {
            if (writeMSG != null && isConnected()) {
                String request = adminXMLHandler.createDeleteUserRequest(username);
                writeMSG.write(request);
                writeMSG.newLine();
                writeMSG.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteGroup(String groupName, String owner) {
        if (!adminXMLHandler.validateGroupName(groupName) || !adminXMLHandler.validateUsername(owner)) {
            System.err.println("Invalid group name or owner format");
            return;
        }

        try {
            if (writeMSG != null && isConnected()) {
                String request = adminXMLHandler.createDeleteGroupRequest(groupName, owner);
                writeMSG.write(request);
                writeMSG.newLine();
                writeMSG.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * **CHANGED: Updated method name to match server expectation**
     */
    public void changeUserPassword(String username, String newPassword) {
        if (!adminXMLHandler.validateUsername(username) || !adminXMLHandler.validatePassword(newPassword)) {
            System.err.println("Invalid username or password format");
            return;
        }

        try {
            if (writeMSG != null && isConnected()) {
                String request = adminXMLHandler.createChangePasswordRequest(username, newPassword);
                writeMSG.write(request);
                writeMSG.newLine();
                writeMSG.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getUserStats(String username) {
        if (!adminXMLHandler.validateUsername(username)) {
            System.err.println("Invalid username format: " + username);
            return;
        }

        try {
            if (writeMSG != null && isConnected()) {
                String request = adminXMLHandler.createGetUserStatsRequest(username);
                writeMSG.write(request);
                writeMSG.newLine();
                writeMSG.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // **CHANGED: Added new admin operations**
    public void createNewUser(String username, String password) {
        if (!adminXMLHandler.validateUsername(username) || !adminXMLHandler.validatePassword(password)) {
            System.err.println("Invalid username or password format");
            return;
        }

        try {
            if (writeMSG != null && isConnected()) {
                String request = adminXMLHandler.createNewUserRequest(username, password);
                writeMSG.write(request);
                writeMSG.newLine();
                writeMSG.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createNewGroup(String groupName, String creator) {
        if (!adminXMLHandler.validateGroupName(groupName) || !adminXMLHandler.validateUsername(creator)) {
            System.err.println("Invalid group name or creator format");
            return;
        }

        try {
            if (writeMSG != null && isConnected()) {
                String request = adminXMLHandler.createNewGroupRequest(groupName, creator);
                writeMSG.write(request);
                writeMSG.newLine();
                writeMSG.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ========== MESSAGING THREAD ==========

    public void stopListening() {
        shouldStop = true;
    }

    @Override
    public void run() {
        try {
            String msg;
            while ((msg = readMSG.readLine()) != null && !shouldStop) {
                if (msg.trim().startsWith("<")) {
                    String finalMsg = msg.trim();

                    try {
                        String reply = adminXMLHandler.getReply(finalMsg);
                        if (reply != null) {
                            handleServerResponse(reply, finalMsg);
                        }
                    } catch (Exception e) {
                        System.out.println("Error parsing server response: " + e.getMessage());
                    }

                    if (messageListener != null) {
                        Platform.runLater(() -> messageListener.accept(finalMsg));
                    }
                } else {
                    System.out.println("Server message: " + msg);
                }
            }
        } catch (IOException e) {
            if (!shouldStop) {
                e.printStackTrace();
            }
        }
    }

    /**
     * **CHANGED: Updated response handling for new admin operations**
     */
    private void handleServerResponse(String reply, String finalMsg) {
        switch (reply) {
            case "ADMIN_LOGIN_SUCCESS":
                try {
                    String responseBody = adminXMLHandler.getResponseBody(finalMsg);
                    System.out.println("Admin login successful: " + responseBody);

                    // **FIXED: Don't stop the thread - admin needs to stay connected for operations**
                    // shouldStop = true; // REMOVED THIS LINE

                    if (loginResponseListener != null) {
                        Platform.runLater(() -> loginResponseListener.accept("LOGIN_SUCCESS"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (loginResponseListener != null) {
                        Platform.runLater(() -> loginResponseListener.accept("LOGIN_FAILED"));
                    }
                }
                break;

            case "ADMIN_LOGIN_FAILED":
                try {
                    System.out.println("Admin login failed: " + adminXMLHandler.getResponseBody(finalMsg));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (loginResponseListener != null) {
                    Platform.runLater(() -> loginResponseListener.accept("LOGIN_FAILED"));
                }
                break;

            case "ALL_USERS":
                handleUsersResponse(finalMsg);
                break;

            case "ALL_GROUPS":
                handleGroupsResponse(finalMsg);
                break;

            case "SEARCH_USERS_RESULT":
                handleUsersResponse(finalMsg);
                break;

            case "SEARCH_GROUPS_RESULT":
                handleGroupsResponse(finalMsg);
                break;

            // **CHANGED: Added handlers for new admin operations**
            case "ACTIVE_USERS":
                handleActiveUsersResponse(finalMsg);
                break;

            case "COMMUNITY_MESSAGES":
                handleCommunityMessagesResponse(finalMsg);
                break;

            case "USER_DELETED":
                handleOperationResponse("User deleted successfully", finalMsg);
                break;

            case "GROUP_DELETED":
                handleOperationResponse("Group deleted successfully", finalMsg);
                break;

            case "PASSWORD_CHANGED":
                handleOperationResponse("Password changed successfully", finalMsg);
                break;

            case "USER_CREATED":
                handleOperationResponse("User created successfully", finalMsg);
                break;

            case "GROUP_CREATED":
                handleOperationResponse("Group created successfully", finalMsg);
                break;

            case "USER_STATS":
                handleUserStatsResponse(finalMsg);
                break;

            case "FORCE_LOGOUT":
                String body = null;
                try {
                    body = adminXMLHandler.getResponseBody(finalMsg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String finalBody = body;
                Platform.runLater(() -> {
                    System.out.println("Force logged out: " + finalBody);
                    try {
                        if (socket != null) {
                            socket.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                break;

            default:
                System.out.println("Unhandled admin response: " + reply);
                break;
        }
    }

    private void handleUsersResponse(String xmlResponse) {
        try {
            String body = adminXMLHandler.getResponseBody(xmlResponse);
            if (body != null && !body.isEmpty()) {
                allUsers.clear();
                String[] userEntries = body.split(",");
                for (String entry : userEntries) {
                    String[] parts = entry.split("!\\[]!");
                    if (parts.length >= 2) {
                        allUsers.add(new AdminModel(parts[0], parts[1]));
                    }
                }

                if (dataUpdateListener != null) {
                    Platform.runLater(() -> dataUpdateListener.accept("USERS_UPDATED"));
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling users response: " + e.getMessage());
        }
    }

    private void handleGroupsResponse(String xmlResponse) {
        try {
            String body = adminXMLHandler.getResponseBody(xmlResponse);
            if (body != null && !body.isEmpty()) {
                allGroups.clear();
                String[] groupEntries = body.split(",");
                for (String entry : groupEntries) {
                    String[] parts = entry.split("!\\[]!");
                    if (parts.length >= 2) {
                        allGroups.add(new AdminModel(parts[1], parts[0], true)); // owner, groupName, isGroup
                    }
                }

                if (dataUpdateListener != null) {
                    Platform.runLater(() -> dataUpdateListener.accept("GROUPS_UPDATED"));
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling groups response: " + e.getMessage());
        }
    }

    // **CHANGED: Added new response handlers**
    private void handleActiveUsersResponse(String xmlResponse) {
        try {
            String body = adminXMLHandler.getResponseBody(xmlResponse);
            if (body != null && !body.isEmpty()) {
                activeUsers.clear();
                String[] users = body.split(",");
                for (String user : users) {
                    activeUsers.add(user.trim());
                }

                if (activeUsersUpdateListener != null) {
                    Platform.runLater(() -> activeUsersUpdateListener.accept("ACTIVE_USERS_UPDATED"));
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling active users response: " + e.getMessage());
        }
    }

    private void handleCommunityMessagesResponse(String xmlResponse) {
        try {
            String body = adminXMLHandler.getResponseBody(xmlResponse);
            if (body != null && !body.isEmpty()) {
                communityMessages.clear();
                String[] messages = body.split("!\\[\\]!");
                for (String message : messages) {
                    if (!message.trim().isEmpty()) {
                        communityMessages.add(message.trim());
                    }
                }

                if (communityMessagesUpdateListener != null) {
                    Platform.runLater(() -> communityMessagesUpdateListener.accept("COMMUNITY_MESSAGES_UPDATED"));
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling community messages response: " + e.getMessage());
        }
    }

    private void handleOperationResponse(String operation, String xmlResponse) {
        try {
            String body = adminXMLHandler.getResponseBody(xmlResponse);
            System.out.println(operation + ": " + body);

            if (operationResultListener != null) {
                Platform.runLater(() -> operationResultListener.accept(operation + ": " + body));
            }

            // Refresh data after successful operations
            requestAllUsers();
            requestAllGroups();

        } catch (Exception e) {
            System.err.println("Error handling operation response: " + e.getMessage());
        }
    }

    private void handleUserStatsResponse(String xmlResponse) {
        try {
            String body = adminXMLHandler.getResponseBody(xmlResponse);
            System.out.println("User stats: " + body);

            if (userStatsListener != null) {
                Platform.runLater(() -> userStatsListener.accept(body));
            }
        } catch (Exception e) {
            System.err.println("Error handling user stats response: " + e.getMessage());
        }
    }

    // ========== GETTERS ==========

    public ObservableList<AdminModel> getAllUsers() {
        return allUsers;
    }

    public ObservableList<AdminModel> getAllGroups() {
        return allGroups;
    }

    // **CHANGED: Added new getters for admin data**
    public ObservableList<String> getActiveUsers() {
        return activeUsers;
    }

    public ObservableList<String> getCommunityMessages() {
        return communityMessages;
    }

    public Socket getSocket() {
        connectionLock.lock();
        try {
            return socket;
        } finally {
            connectionLock.unlock();
        }
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    // ========== SETTERS ==========

    public void setMessageListener(Consumer<String> listener) {
        this.messageListener = listener;
    }

    public void setLoginResponseListener(Consumer<String> listener) {
        this.loginResponseListener = listener;
    }

    public void setDataUpdateListener(Consumer<String> listener) {
        this.dataUpdateListener = listener;
    }

    public void setOperationResultListener(Consumer<String> listener) {
        this.operationResultListener = listener;
    }

    // **CHANGED: Added new setters for admin-specific listeners**
    public void setActiveUsersUpdateListener(Consumer<String> listener) {
        this.activeUsersUpdateListener = listener;
    }

    public void setCommunityMessagesUpdateListener(Consumer<String> listener) {
        this.communityMessagesUpdateListener = listener;
    }

    public void setUserStatsListener(Consumer<String> listener) {
        this.userStatsListener = listener;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    // ========== CLEANUP METHODS ==========

    public void disconnect() {
        try {
            shouldStop = true;
            if (readMSG != null) {
                readMSG.close();
            }
            if (writeMSG != null) {
                writeMSG.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            synchronized (adminClients) {
                adminClients.remove(this);
            }
            connected = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopReadWrite() throws IOException {
        if (readMSG != null) {
            readMSG.close();
        }
        if (writeMSG != null) {
            writeMSG.close();
        }
    }

    // ========== UTILITY METHODS ==========

    public void refreshData() {
        loadData();
    }

    public boolean hasValidConnection() {
        return isConnected() && socket != null;
    }

    public void sendAdminCommand(String command) {
        try {
            if (writeMSG != null && isConnected()) {
                writeMSG.write(command);
                writeMSG.newLine();
                writeMSG.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}