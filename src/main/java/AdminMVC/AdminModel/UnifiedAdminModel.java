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
    private volatile Socket socket;
    private volatile String ip;
    private volatile int port;
    private volatile boolean connected;
    private final ReentrantLock connectionLock = new ReentrantLock();

    private BufferedReader readMSG;
    private BufferedWriter writeMSG;
    private String adminUsername;
    private volatile boolean shouldStop = false;

    private ObservableList<AdminModel> allUsers;
    private ObservableList<AdminModel> allGroups;
    private ObservableList<String> activeUsers;
    private ObservableList<String> communityMessages;
    private static ArrayList<UnifiedAdminModel> adminClients = new ArrayList<>();

    private AdminXMLHandler adminXMLHandler;

    private Consumer<String> messageListener;
    private Consumer<String> loginResponseListener;
    private Consumer<String> dataUpdateListener;
    private Consumer<String> operationResultListener;
    private Consumer<String> activeUsersUpdateListener;
    private Consumer<String> communityMessagesUpdateListener;
    private Consumer<String> userStatsListener;

    public enum ConnectionStatus {
        SUCCESS, EMPTY_FIELDS, INVALID_PORT_FORMAT, INVALID_IP_FORMAT, INVALID_PORT_RANGE, CONNECTION_FAILED
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
        this.activeUsers = FXCollections.observableArrayList();
        this.communityMessages = FXCollections.observableArrayList();
    }

    public ConnectionResult connect(String ip, String portText) {
        connectionLock.lock();
        try {
            this.connected = false;
            if (this.socket != null && !this.socket.isClosed()) {
                try {
                    this.socket.close();
                } catch (IOException e) {
                    // ignore
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

            if (port < 1024 || port > 65535) {
                return new ConnectionResult(ConnectionStatus.INVALID_PORT_RANGE, "Port must be between 1024 and 65535");
            }

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
                    System.out.println("Admin received: " + finalMsg);

                    try {
                        String reply = adminXMLHandler.getReply(finalMsg);
                        if (reply != null) {
                            handleServerResponse(reply, finalMsg);
                        }
                    } catch (Exception e) {
                        System.out.println("Error parsing server response: " + e.getMessage());
                        e.printStackTrace();
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
                System.err.println("Admin model connection lost: " + e.getMessage());
            }
        }
    }

    private void handleServerResponse(String reply, String finalMsg) {
        switch (reply) {
            case "ADMIN_LOGIN_SUCCESS":
                try {
                    String responseBody = adminXMLHandler.getResponseBody(finalMsg);
                    System.out.println("Admin login successful: " + responseBody);

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

            case "ACTIVE_USERS":
                handleActiveUsersResponse(finalMsg);
                break;

            case "COMMUNITY_MESSAGES":
                handleCommunityMessagesResponse(finalMsg);
                break;

            case "USER_DELETED":
                handleOperationResponse("User deleted successfully", finalMsg);
                loadData(); // Refresh data after delete
                break;

            case "GROUP_DELETED":
                handleOperationResponse("Group deleted successfully", finalMsg);
                loadData(); // Refresh data after delete
                break;

            case "PASSWORD_CHANGED":
                handleOperationResponse("Password changed successfully", finalMsg);
                loadData(); // Refresh data after password change
                break;

            case "USER_CREATED":
                handleOperationResponse("User created successfully", finalMsg);
                loadData(); // Refresh data after user creation
                break;

            case "GROUP_CREATED":
                handleOperationResponse("Group created successfully", finalMsg);
                loadData(); // Refresh data after group creation
                break;

            case "USER_STATS":
                handleUserStatsResponse(finalMsg);
                break;

            case "USER_DELETE_FAILED":
                handleOperationResponse("Failed to delete user", finalMsg);
                break;

            case "PASSWORD_CHANGE_FAILED":
                handleOperationResponse("Failed to change password", finalMsg);
                break;

            case "USER_CREATE_FAILED":
                handleOperationResponse("Failed to create user", finalMsg);
                break;

            case "GROUP_CREATE_FAILED":
                handleOperationResponse("Failed to create group", finalMsg);
                break;

            case "GROUP_DELETE_FAILED":
                handleOperationResponse("Failed to delete group", finalMsg);
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
                Platform.runLater(() -> {
                    allUsers.clear();
                    String[] userEntries = body.split(",");
                    for (String entry : userEntries) {
                        if (!entry.trim().isEmpty()) {
                            String[] parts = entry.split("!\\[]!");
                            if (parts.length >= 2) {
                                allUsers.add(new AdminModel(parts[0], parts[1]));
                            }
                        }
                    }

                    if (dataUpdateListener != null) {
                        dataUpdateListener.accept("USERS_UPDATED");
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Error handling users response: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleGroupsResponse(String xmlResponse) {
        try {
            String body = adminXMLHandler.getResponseBody(xmlResponse);
            if (body != null && !body.isEmpty()) {
                Platform.runLater(() -> {
                    allGroups.clear();
                    String[] groupEntries = body.split(",");
                    for (String entry : groupEntries) {
                        if (!entry.trim().isEmpty()) {
                            String[] parts = entry.split("!\\[]!");
                            if (parts.length >= 2) {
                                allGroups.add(new AdminModel(parts[1], parts[0], true)); // owner, groupName, isGroup
                            }
                        }
                    }

                    if (dataUpdateListener != null) {
                        dataUpdateListener.accept("GROUPS_UPDATED");
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Error handling groups response: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleActiveUsersResponse(String xmlResponse) {
        try {
            String body = adminXMLHandler.getResponseBody(xmlResponse);
            if (body != null && !body.isEmpty()) {
                Platform.runLater(() -> {
                    activeUsers.clear();
                    String[] users = body.split(",");
                    for (String user : users) {
                        if (!user.trim().isEmpty()) {
                            activeUsers.add(user.trim());
                        }
                    }

                    if (activeUsersUpdateListener != null) {
                        activeUsersUpdateListener.accept("ACTIVE_USERS_UPDATED");
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Error handling active users response: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleCommunityMessagesResponse(String xmlResponse) {
        try {
            String body = adminXMLHandler.getResponseBody(xmlResponse);
            if (body != null && !body.isEmpty()) {
                Platform.runLater(() -> {
                    communityMessages.clear();
                    String[] messages = body.split("!\\[\\]!");
                    for (String message : messages) {
                        if (!message.trim().isEmpty()) {
                            communityMessages.add(message.trim());
                        }
                    }

                    if (communityMessagesUpdateListener != null) {
                        communityMessagesUpdateListener.accept("COMMUNITY_MESSAGES_UPDATED");
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Error handling community messages response: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleOperationResponse(String operation, String xmlResponse) {
        try {
            String body = adminXMLHandler.getResponseBody(xmlResponse);
            String message = operation;
            if (body != null && !body.trim().isEmpty()) {
                message = operation + ": " + body;
            }

            System.out.println(message);

            if (operationResultListener != null) {
                String finalMessage = message;
                Platform.runLater(() -> operationResultListener.accept(finalMessage));
            }

        } catch (Exception e) {
            System.err.println("Error handling operation response: " + e.getMessage());
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    // Getters
    public ObservableList<AdminModel> getAllUsers() {
        return allUsers;
    }

    public ObservableList<AdminModel> getAllGroups() {
        return allGroups;
    }

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

    // Setters
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

    // Cleanup methods
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

    // Utility methods
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