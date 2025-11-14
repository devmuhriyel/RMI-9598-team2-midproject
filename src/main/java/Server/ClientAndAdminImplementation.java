package Server;

import Common.Interfaces.AdminCallbackInterface;
import Common.Interfaces.AdminInterface;
import Common.Interfaces.ClientCallbackInterface;
import Common.Interfaces.ClientInterface;
import Common.Objects.AdminObjects.Admin;
import Common.Objects.AdminObjects.AdminAccountsList;
import Common.Objects.ClientObjects.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClientAndAdminImplementation extends UnicastRemoteObject implements ClientInterface, AdminInterface {
    //VARIABLES
    private ConcurrentHashMap<String, Client> allUsers;
    private ConcurrentHashMap<String, Admin> allAdmins;
    private ConcurrentHashMap<String, ClientCallbackInterface> clientCallbacks;
    private ConcurrentHashMap<String, AdminCallbackInterface> adminCallbacks;
    private Set<String> communityChatActiveUsers;
    private ConcurrentHashMap<String, ReadWriteLock> conversationLocks;

    private ScheduledExecutorService callbackExecutor;
    private ReadWriteLock fileLock;
    private AdminAccountsList adminAccountsList;
    private ClientAccountsList clientAccountsList;
    private ServerJSONHandler serverJSONHandler;
    private Set<String> communityChatActiveAdmins = ConcurrentHashMap.newKeySet();

    //CONSTRUCTORS
    public ClientAndAdminImplementation() throws RemoteException {
        super();
        this.allUsers = new ConcurrentHashMap<>();
        this.allAdmins = new ConcurrentHashMap<>();
        this.clientCallbacks = new ConcurrentHashMap<>();
        this.adminCallbacks = new ConcurrentHashMap<>();
        this.conversationLocks = new ConcurrentHashMap<>();
        this.communityChatActiveUsers = ConcurrentHashMap.newKeySet();
        this.callbackExecutor = Executors.newSingleThreadScheduledExecutor();
        this.fileLock = new ReentrantReadWriteLock();
        this.serverJSONHandler = new ServerJSONHandler();
        this.adminAccountsList = serverJSONHandler.readAdminAccountFile("src/main/resources/JSONFiles/AdminAccounts.json");
        this.clientAccountsList = serverJSONHandler.readClientAccountFile("src/main/resources/JSONFiles/ClientAccounts.json");
        if (adminAccountsList == null) {
            System.err.println("Warning: adminAccountsList is null, creating new one");
            this.adminAccountsList = new AdminAccountsList();
        }
        if (clientAccountsList == null) {
            System.err.println("Warning: clientAccountsList is null, creating new one");
            this.clientAccountsList = new ClientAccountsList();
        }
        List<Client> clients = clientAccountsList.getClients();
        List<Admin> admins = adminAccountsList.getAdmins();

        if (clients != null) {
            for(Client client: clients) {
                allUsers.put(client.getUsername(), client);
            }
        } else {
            System.err.println("Warning: No clients loaded");
        }

        if (admins != null) {
            for(Admin admin: admins) {
                allAdmins.put(admin.getUsername(), admin);
            }
        } else {
            System.err.println("Warning: No admins loaded");
        }

    }

    //USER LOG IN, SIGN UP, AND OTHER RELATED METHODS
    @Override
    public void signUp(String username, String password, String verificationCode, String jsonString, AdminCallbackInterface adminCallback) throws RemoteException {
        adminCallback.onFailedSignUp();
    }

    @Override
    public synchronized void logIn(String username, String jsonString, AdminCallbackInterface adminCallback) throws RemoteException {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            String password = jsonObject.get("password").getAsString();
            Admin admin = allAdmins.get(username);

            if (admin == null) {
                adminCallback.onFailedLogin();
                return;
            }

            if (!admin.getPassword().equals(password)) {
                adminCallback.onFailedLogin();
                return;
            }

            adminCallbacks.put(username, adminCallback);
            adminCallback.onSuccessfulLogin();

        } catch (Exception e) {
            System.err.println("✗ Error during admin login: " + e.getMessage());
            e.printStackTrace();
            adminCallback.onFailedLogin();
        }
    }

    @Override
    public void signUp(String username, String password, String verificationCode, String jsonString, ClientCallbackInterface clientCallback) throws RemoteException {
        if (!isValidUsername(username)) {
            clientCallback.onFailedSignUp();
            return;
        }

        if (!isValidPassword(password)) {
            clientCallback.onFailedSignUp();
            return;
        }

        fileLock.writeLock().lock();
        try {
            if (allUsers.containsKey(username)) {
                clientCallback.onFailedSignUp();
                return;
            }

            Client newClient = new Client(username, password, verificationCode);
            allUsers.put(username, newClient);

            if (clientAccountsList.getClients() == null) {
                clientAccountsList.setClients(new ArrayList<>());
            }
            clientAccountsList.getClients().add(newClient);
            serverJSONHandler.writeClientAccountFile("src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList);
            clientCallbacks.put(username, clientCallback);
            clientCallback.onSuccessfulSignUp();

        } catch (Exception e) {
            System.err.println("Error during client signup: " + e.getMessage());
            e.printStackTrace();
            clientCallback.onFailedSignUp();
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    @Override
    public void logIn(String username, String jsonString, ClientCallbackInterface clientCallback) throws RemoteException {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            String jsonUsername = jsonObject.get("username").getAsString();
            String password = jsonObject.get("password").getAsString();

            if (!username.equals(jsonUsername)) {
                clientCallback.onFailedLogin();
                return;
            }

            Client client = allUsers.get(username);
            if (client == null) {
                clientCallback.onFailedLogin();
                return;
            }

            if (!client.getPassword().equals(password)) {
                clientCallback.onFailedLogin();
                return;
            }

            clientCallbacks.put(username, clientCallback);
            Gson gson = new Gson();
            String clientDataJson = gson.toJson(client);
            clientCallback.onSuccessfulLogin(clientDataJson);
            notifyAdminsOfOnlineUsers();

        } catch (JsonSyntaxException e) {
            System.err.println("Error parsing login JSON: " + e.getMessage());
            e.printStackTrace();
            clientCallback.onFailedLogin();
        } catch (Exception e) {
            System.err.println("Error during client login: " + e.getMessage());
            e.printStackTrace();
            clientCallback.onFailedLogin();
        }
    }

    @Override
    public void logOut(String username, String jsonString) throws RemoteException {

        try {
            if (clientCallbacks.containsKey(username)) {
                clientCallbacks.remove(username);
            }
            notifyAdminsOfOnlineUsers();

        } catch (Exception e) {
            System.err.println("Error during client logout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void notifyAdminsOfOnlineUsers() {
        List<String> onlineUsernames = new ArrayList<>(clientCallbacks.keySet());

        List<Map.Entry<String, AdminCallbackInterface>> adminEntries =
                new ArrayList<>(adminCallbacks.entrySet());

        for (Map.Entry<String, AdminCallbackInterface> entry : adminEntries) {
            String adminName = entry.getKey();
            AdminCallbackInterface adminCallback = entry.getValue();

            callbackExecutor.execute(() -> {
                try {
                    adminCallback.onReceiveActiveUsersList(onlineUsernames);
                } catch (RemoteException e) {
                    adminCallbacks.remove(adminName);
                }
            });
        }
    }
    @Override
    public void changePassword(String jsonString) throws RemoteException {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            String username = jsonObject.get("username").getAsString();
            String newPassword = jsonObject.get("newPassword").getAsString();
            String admin = jsonObject.has("admin") ? jsonObject.get("admin").getAsString() : "";
            if (!isValidPassword(newPassword)) {
                AdminCallbackInterface adminCallback = adminCallbacks.get(admin);
                if (adminCallback != null) {
                    callbackExecutor.execute(() -> {
                        try {
                            adminCallback.onFailedPasswordChange();
                        } catch (RemoteException e) {
                            System.err.println("Failed to notify admin: " + e.getMessage());
                        }
                    });
                }
                return;
            }

            fileLock.writeLock().lock();
            try {
                Client targetClient = allUsers.get(username);
                if (targetClient == null) {
                    AdminCallbackInterface adminCallback = adminCallbacks.get(admin);
                    if (adminCallback != null) {
                        callbackExecutor.execute(() -> {
                            try {
                                adminCallback.onFailedPasswordChange();
                            } catch (RemoteException e) {
                                System.err.println("Failed to notify admin: " + e.getMessage());
                            }
                        });
                    }
                    return;
                }

                targetClient.setPassword(newPassword);

                serverJSONHandler.writeClientAccountFile("src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList);

                AdminCallbackInterface adminCallback = adminCallbacks.get(admin);
                if (adminCallback != null) {
                    callbackExecutor.execute(() -> {
                        try {
                            adminCallback.onSuccessfulPasswordChange();
                        } catch (RemoteException e) {
                            System.err.println("Failed to notify admin: " + e.getMessage());
                        }
                    });
                }

            } finally {
                fileLock.writeLock().unlock();
            }

        } catch (Exception e) {
            System.err.println("Error in admin changePassword: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void verifyCode(String jsonString) throws RemoteException{

    }

    //USER/ADMIN HOME UI METHODS
    @Override
    public void searchUsers(String jsonString) throws RemoteException {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            String keyword = jsonObject.has("keyword") ? jsonObject.get("keyword").getAsString() : "";
            String admin = jsonObject.has("admin") ? jsonObject.get("admin").getAsString() : "";
            String messageType = jsonObject.has("messageType") ? jsonObject.get("messageType").getAsString() : "";
            AdminCallbackInterface adminCallback = adminCallbacks.get(admin);
            if (adminCallback == null) {
                return;
            }

            fileLock.readLock().lock();
            try {
                List<Client> matchingClients = new ArrayList<>();
                List<String> onlineUsernames = new ArrayList<>(clientCallbacks.keySet());

                if ("GET_ACTIVE_USERS".equals(messageType)) {
                    for (String onlineUsername : onlineUsernames) {
                        Client client = allUsers.get(onlineUsername);
                        if (client != null) {
                            matchingClients.add(client);
                        }
                    }
                } else if (keyword.isEmpty()) {
                    matchingClients.addAll(allUsers.values());
                } else {
                    for (Client client : allUsers.values()) {
                        if (client.getUsername().toLowerCase().contains(keyword.toLowerCase())) {
                            matchingClients.add(client);
                        }
                    }
                }

                List<Client> finalMatchingClients = matchingClients;
                List<String> finalOnlineUsernames = onlineUsernames;

                callbackExecutor.execute(() -> {
                    try {
                        adminCallback.onReceiveUsersList(finalMatchingClients);
                        adminCallback.onReceiveActiveUsersList(finalOnlineUsernames);
                        adminCallback.onSearchUserResults();
                    } catch (RemoteException e) {
                        System.err.println("Failed to send search results to admin: " + e.getMessage());
                    }
                });

            } finally {
                fileLock.readLock().unlock();
            }

        } catch (Exception e) {
            System.err.println("Error in searchUsers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void searchGroups(String jsonString) throws RemoteException {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            String keyword = jsonObject.has("keyword") ? jsonObject.get("keyword").getAsString() : "";
            String admin = jsonObject.has("admin") ? jsonObject.get("admin").getAsString() : "";
            AdminCallbackInterface adminCallback = adminCallbacks.get(admin);
            fileLock.readLock().lock();
            try {
                Map<String, String> groupOwners = new HashMap<>();
                Map<String, GroupChat> uniqueGroups = new LinkedHashMap<>();
                for (Client client : allUsers.values()) {
                    if (client.getGroupChats() != null) {
                        for (GroupChat group : client.getGroupChats()) {
                            String groupID = group.getGroupID();
                            if (!groupOwners.containsKey(groupID)) {
                                groupOwners.put(groupID, client.getUsername());
                                uniqueGroups.put(groupID, group);
                            }
                        }
                    }
                }

                List<JsonObject> filteredGroups = new ArrayList<>();
                for (Map.Entry<String, GroupChat> entry : uniqueGroups.entrySet()) {
                    GroupChat group = entry.getValue();
                    String owner = groupOwners.get(entry.getKey());
                    if (keyword.isEmpty() ||
                            (group.getGroupName() != null &&
                                    group.getGroupName().toLowerCase().contains(keyword.toLowerCase()))) {
                        JsonObject groupJson = new JsonObject();
                        groupJson.addProperty("groupID", group.getGroupID());
                        groupJson.addProperty("groupName", group.getGroupName());
                        groupJson.addProperty("owner", owner);  // Add owner explicitly
                        com.google.gson.JsonArray receiversArray = new com.google.gson.JsonArray();
                        if (group.getReceivers() != null) {
                            for (String receiver : group.getReceivers()) {
                                receiversArray.add(receiver);
                            }
                        }
                        groupJson.add("receivers", receiversArray);
                        groupJson.addProperty("memberCount",
                                group.getReceivers() != null ? group.getReceivers().size() : 0);
                        filteredGroups.add(groupJson);
                    }
                }

                com.google.gson.JsonArray groupsArray = new com.google.gson.JsonArray();
                for (JsonObject groupJson : filteredGroups) {
                    groupsArray.add(groupJson);
                }

                Gson gson = new Gson();
                String groupsJson = gson.toJson(groupsArray);

                callbackExecutor.execute(() -> {
                    try {
                        adminCallback.onReceiveGroupsList(groupsJson);
                        adminCallback.onSearchGroupResults();
                    } catch (RemoteException e) {
                        System.err.println("Failed to send group search results to admin: " + e.getMessage());
                        e.printStackTrace();
                    }
                });

            } finally {
                fileLock.readLock().unlock();
            }

        } catch (Exception e) {
            System.err.println("Error in searchGroups: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @Override
    public void deleteUser(String jsonString) throws RemoteException {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            String username = jsonObject.get("username").getAsString();
            String admin = jsonObject.has("admin") ? jsonObject.get("admin").getAsString() : "";

            AdminCallbackInterface adminCallback = adminCallbacks.get(admin);

            fileLock.writeLock().lock();
            try {
                Client clientToDelete = allUsers.get(username);
                if (clientToDelete == null) {
                    if (adminCallback != null) {
                        callbackExecutor.execute(() -> {
                            try {
                                adminCallback.onDeleteUser();
                            } catch (RemoteException e) {
                                System.err.println("Failed to notify admin: " + e.getMessage());
                            }
                        });
                    }
                    return;
                }

                allUsers.remove(username);
                clientCallbacks.remove(username);
                clientAccountsList.getClients().removeIf(c -> c.getUsername().equals(username));
                for (Client client : clientAccountsList.getClients()) {
                    if (client.getFriends() != null) {
                        client.getFriends().remove(username);
                    }

                    // Remove friend requests
                    if (client.getIncomingFriendRequests() != null) {
                        client.getIncomingFriendRequests().removeIf(req ->
                                req.getRequester().equals(username) || req.getResponder().equals(username));
                    }
                    if (client.getOutgoingFriendRequests() != null) {
                        client.getOutgoingFriendRequests().removeIf(req ->
                                req.getRequester().equals(username) || req.getResponder().equals(username));
                    }
                }

                serverJSONHandler.writeClientAccountFile("src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList);
                if (adminCallback != null) {
                    callbackExecutor.execute(() -> {
                        try {
                            adminCallback.onDeleteUser();
                        } catch (RemoteException e) {
                            System.err.println("Failed to notify admin: " + e.getMessage());
                        }
                    });
                }

            } finally {
                fileLock.writeLock().unlock();
            }

        } catch (Exception e) {
            System.err.println("Error in deleteUser: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void deleteGroup(String jsonString) throws RemoteException {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            String groupName = jsonObject.get("groupName").getAsString();
            String owner = jsonObject.get("owner").getAsString();
            String admin = jsonObject.has("admin") ? jsonObject.get("admin").getAsString() : "";

            AdminCallbackInterface adminCallback = adminCallbacks.get(admin);

            fileLock.writeLock().lock();
            try {
                boolean groupDeleted = false;
                for (Client client : clientAccountsList.getClients()) {
                    if (client.getGroupChats() != null) {
                        int beforeSize = client.getGroupChats().size();

                        client.getGroupChats().removeIf(g ->
                                g.getGroupName().equals(groupName));

                        int afterSize = client.getGroupChats().size();
                        if (beforeSize > afterSize) {
                            groupDeleted = true;
                        }
                    }
                }

                if (groupDeleted) {
                    serverJSONHandler.writeClientAccountFile("src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList);
                    serverJSONHandler.writeClientAccountFile(
                            "src/main/resources/JSONFiles/ClientAccounts.json",
                            clientAccountsList
                    );
                    System.out.println("✓ Server: Group deleted from memory and saved to JSON: " + groupName);
                } else {
                    System.out.println("✗ Server: Group not found: " + groupName);
                }

                if (adminCallback != null) {
                    callbackExecutor.execute(() -> {
                        try {
                            adminCallback.onDeleteGroup();
                        } catch (RemoteException e) {
                            System.err.println("Failed to notify admin: " + e.getMessage());
                        }
                    });
                }

            } finally {
                fileLock.writeLock().unlock();
            }

        } catch (Exception e) {
            System.err.println("Error in deleteGroup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void getUnreadMessages(String jsonString) throws RemoteException{

    }

    @Override
    public void sendFriendRequest(String jsonString) throws RemoteException {
        String requesterUsername;
        String targetUsername;

        try {
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            requesterUsername = jsonObject.get("requester").getAsString();
            targetUsername = jsonObject.get("responder").getAsString();

        } catch (Exception e) {
            System.err.println("Server Error: Failed to parse friend request JSON: " + jsonString);
            throw new RemoteException("Invalid request format.", e);
        }

        if (requesterUsername.equalsIgnoreCase(targetUsername)) {
            System.err.println("Friend request failed: Self-request is not allowed.");
            return;
        }

        fileLock.writeLock().lock();
        try {
            boolean success = handleFriendRequestLogic(requesterUsername, targetUsername);

            if (success) {
                serverJSONHandler.writeClientAccountFile(
                        "src/main/resources/JSONFiles/ClientAccounts.json",
                        clientAccountsList
                );
                ClientCallbackInterface senderCallback = clientCallbacks.get(requesterUsername);
                if(senderCallback != null) {
                    senderCallback.onFriendRequestSent();
                }

                ClientCallbackInterface receiverCallback = clientCallbacks.get(targetUsername);
                if(receiverCallback != null) {
                    receiverCallback.onFriendRequestReceived();
                }

            } else {
                System.err.println("Friend request failed (logic check failed, possibly already sent): " + requesterUsername + " -> " + targetUsername);
            }

        } catch (Exception e) {
            System.err.println("Error during friend request logic: " + e.getMessage());
            e.printStackTrace();
        } finally {
            fileLock.writeLock().unlock();
        }
    }
    private boolean handleFriendRequestLogic(String requester, String target) {
        Client requesterClient = findClient(requester); // Use findClient helper
        Client targetClient = findClient(target);       // Use findClient helper

        if (requesterClient == null || targetClient == null) {
            System.err.println("Friend Request Logic Failed: Client not found.");
            return false;
        }

        if (requesterClient.getFriends() != null && requesterClient.getFriends().contains(target)) {
            System.err.println("Friend Request Logic Failed: Users are already friends.");
            return false;
        }

        if (requesterClient.getOutgoingFriendRequests() != null) {
            for (FriendRequest fr : requesterClient.getOutgoingFriendRequests()) {
                if (fr.getResponder().equalsIgnoreCase(target) && fr.getStatus().equalsIgnoreCase("pending")) {
                    System.err.println("Friend Request Logic Failed: Request already pending (outgoing).");
                    return false;
                }
            }
        }

        if (requesterClient.getIncomingFriendRequests() != null) {
            for (FriendRequest fr : requesterClient.getIncomingFriendRequests()) {
                if (fr.getRequester().equalsIgnoreCase(target) && fr.getStatus().equalsIgnoreCase("pending")) {
                    System.err.println("Friend Request Logic Failed: Target has a pending request to requester.");
                    return false; // Client should accept this instead of sending new
                }
            }
        }

        FriendRequest outgoingReq = new FriendRequest(requester, target, "pending");
        FriendRequest incomingReq = new FriendRequest(requester, target, "pending");

        if (requesterClient.getOutgoingFriendRequests() == null) {
            requesterClient.setOutgoingFriendRequests(new ArrayList<>());
        }
        if (targetClient.getIncomingFriendRequests() == null) {
            targetClient.setIncomingFriendRequests(new ArrayList<>());
        }

        requesterClient.getOutgoingFriendRequests().add(outgoingReq);
        targetClient.getIncomingFriendRequests().add(incomingReq);

        return true;
    }

    @Override
    public void getOnlineFriends(String jsonString) throws RemoteException{

    }

    //USER/ADMIN ACCOUNT SETTINGS UI METHODS
    @Override
    public void changeUsername(String jsonString) throws RemoteException {
        String currentUsername = null;
        String newUsername = null;

        try {
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            currentUsername = jsonObject.get("currentUsername").getAsString();
            newUsername = jsonObject.get("newUsername").getAsString();

        } catch (Exception e) {
            System.err.println("Server Error: Failed to parse username change JSON: " + jsonString);
            throw new RemoteException("Invalid username change format.", e);
        }

        Client targetClient = allUsers.get(currentUsername);

        if (targetClient == null) {
            notifyUsernameChangeFailed(currentUsername);
            return;
        }

        if (allUsers.containsKey(newUsername)) {
            notifyUsernameExists(currentUsername);
            return;
        }

        fileLock.writeLock().lock();
        try {
            targetClient.setUsername(newUsername);
            updateUsernameReferences(currentUsername, newUsername);

            allUsers.remove(currentUsername);
            allUsers.put(newUsername, targetClient);

            ClientCallbackInterface callback = clientCallbacks.remove(currentUsername);
            if (callback != null) {
                clientCallbacks.put(newUsername, callback);
            }

            serverJSONHandler.writeClientAccountFile("src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList);
            notifyUsernameChangeSuccess(newUsername);

        } finally {
            fileLock.writeLock().unlock();
        }
    }

    private void updateUsernameReferences(String oldUsername, String newUsername) {

        for (Client client : clientAccountsList.getClients()) {
            if (client.getFriends() != null) {
                for (int i = 0; i < client.getFriends().size(); i++) {
                    if (client.getFriends().get(i).equals(oldUsername)) {
                        client.getFriends().set(i, newUsername);
                    }
                }
            }

            if (client.getOutgoingFriendRequests() != null) {
                for (FriendRequest req : client.getOutgoingFriendRequests()) {
                    if (req.getRequester().equals(oldUsername)) {
                        req.setRequester(newUsername);
                    }
                    if (req.getResponder().equals(oldUsername)) {
                        req.setResponder(newUsername);
                    }
                }
            }

            if (client.getIncomingFriendRequests() != null) {
                for (FriendRequest req : client.getIncomingFriendRequests()) {
                    if (req.getRequester().equals(oldUsername)) {
                        req.setRequester(newUsername);
                    }
                    if (req.getResponder().equals(oldUsername)) {
                        req.setResponder(newUsername);
                    }
                }
            }

            if (client.getConversations() != null) {
                for (Conversation conv : client.getConversations()) {
                    if (conv.getRecipient() != null && conv.getRecipient().equals(oldUsername)) {
                        conv.setRecipient(newUsername);
                    }

                    if (conv.getMessages() != null) {
                        for (Message msg : conv.getMessages()) {
                            if (msg.getSender() != null && msg.getSender().equals(oldUsername)) {
                                msg.setSender(newUsername);
                            }
                            if (msg.getReceiver() != null && msg.getReceiver().equals(oldUsername)) {
                                msg.setReceiver(newUsername);
                            }
                        }
                    }
                }
            }
        }
    }
    @Override
    public void changePasswordWithOldPassword(String jsonString) throws RemoteException {
        String username = null;
        String oldPassword = null;
        String newPassword = null;

        try {
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            username = jsonObject.get("username").getAsString();
            oldPassword = jsonObject.get("oldPassword").getAsString();
            newPassword = jsonObject.get("newPassword").getAsString();
        } catch (Exception e) {
            System.err.println("Server Error: Failed to parse password change JSON: " + jsonString);
            throw new RemoteException("Invalid password change format.", e);
        }

        if (!isValidPassword(newPassword)) {
            notifyPasswordChangeFailed(username);
            return;
        }

        Client targetClient = allUsers.get(username);

        if (targetClient == null) {
            notifyPasswordChangeFailed(username);
            return;
        }

        if (!targetClient.getPassword().equals(oldPassword)) {
            notifyInvalidOldPassword(username);
            return;
        }

        fileLock.writeLock().lock();
        try {
            targetClient.setPassword(newPassword);
            serverJSONHandler.writeClientAccountFile("src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList);
            notifyPasswordChangeSuccess(username);

        } finally {
            fileLock.writeLock().unlock();
        }
    }

    // ==================== CALLBACK NOTIFICATIONS ====================
    private void notifyUsernameChangeSuccess(String username) {
        ClientCallbackInterface callback = clientCallbacks.get(username);
        if (callback != null) {
            callbackExecutor.execute(() -> {
                try {
                    callback.onSuccessfulUsernameChange();
                } catch (RemoteException e) {
                    System.err.println("Failed to notify username change success: " + e.getMessage());
                }
            });
        }
    }

    private void notifyUsernameExists(String username) {
        ClientCallbackInterface callback = clientCallbacks.get(username);
        if (callback != null) {
            callbackExecutor.execute(() -> {
                try {
                    callback.onFailedUsernameChange();
                } catch (RemoteException e) {
                    System.err.println("Failed to notify username exists: " + e.getMessage());
                }
            });
        }
    }

    private void notifyUsernameChangeFailed(String username) {
        ClientCallbackInterface callback = clientCallbacks.get(username);
        if (callback != null) {
            callbackExecutor.execute(() -> {
                try {
                    callback.onFailedUsernameChange();
                } catch (RemoteException e) {
                    System.err.println("Failed to notify username change failure: " + e.getMessage());
                }
            });
        }
    }

    private void notifyPasswordChangeSuccess(String username) {
        ClientCallbackInterface callback = clientCallbacks.get(username);
        if (callback != null) {
            callbackExecutor.execute(() -> {
                try {
                    callback.onSuccessfulPasswordChangeWithOldPassword();
                } catch (RemoteException e) {
                    System.err.println("Failed to notify password change success: " + e.getMessage());
                }
            });
        }
    }

    private void notifyInvalidOldPassword(String username) {
        ClientCallbackInterface callback = clientCallbacks.get(username);
        if (callback != null) {
            callbackExecutor.execute(() -> {
                try {
                    callback.onFailedPasswordChangeWithOldPassword();
                } catch (RemoteException e) {
                    System.err.println("Failed to notify invalid old password: " + e.getMessage());
                }
            });
        }
    }

    private void notifyPasswordChangeFailed(String username) {
        ClientCallbackInterface callback = clientCallbacks.get(username);
        if (callback != null) {
            callbackExecutor.execute(() -> {
                try {
                    callback.onFailedPasswordChangeWithOldPassword();
                } catch (RemoteException e) {
                    System.err.println("Failed to notify password change failure: " + e.getMessage());
                }
            });
        }
    }


    //USER/ADMIN FRIEND REQUESTS UI METHODS
    @Override
    public void cancelFriendRequest(String jsonString) throws RemoteException {
        String requester;
        String target;

        try {
            com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(jsonString).getAsJsonObject();
            requester = jsonObject.get("requester").getAsString();
            target = jsonObject.get("responder").getAsString(); // Consistent with FriendRequest object

        } catch (Exception e) {
            System.err.println("Server Error: Failed to parse JSON for cancelFriendRequest: " + jsonString);
            throw new RemoteException("Invalid request format for cancellation.", e);
        }

        fileLock.writeLock().lock();
        try {
            Client requesterClient = findClient(requester);
            Client targetClient = findClient(target);

            boolean removedOutgoing = requesterClient.getOutgoingFriendRequests().removeIf(
                    req -> req.getResponder().equals(target) && req.getStatus().equals("pending")
            );

            boolean removedIncoming = targetClient.getIncomingFriendRequests().removeIf(
                    req -> req.getRequester().equals(requester) && req.getStatus().equals("pending")
            );

            if (removedOutgoing && removedIncoming) {
                serverJSONHandler.writeClientAccountFile(
                        "src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList // Corrected file path
                );

                ClientCallbackInterface senderCallback = clientCallbacks.get(requester);
                if(senderCallback != null) {
                    senderCallback.onCancelFriendRequest();
                }

            }

        } catch (Exception e) {
            System.err.println("Error during cancel friend request: " + e.getMessage());
            e.printStackTrace();
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    @Override
    public void acceptFriendRequest(String jsonString) throws RemoteException {
        String requester;
        String responder;

        try {
            com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(jsonString).getAsJsonObject();

            requester = jsonObject.get("requester").getAsString();
            responder = jsonObject.get("responder").getAsString(); // Consistent with FriendRequest object

        } catch (Exception e) {
            System.err.println("Server Error: Failed to parse JSON for acceptFriendRequest: " + jsonString);
            throw new RemoteException("Invalid request format for acceptance.", e);
        }

        fileLock.writeLock().lock();
        try {
            Client responderClient = findClient(responder); // Current user (the one who received the request)
            Client requesterClient = findClient(requester); // The friend

            if (responderClient == null || requesterClient == null) {
                return;
            }

            responderClient.getIncomingFriendRequests().removeIf(
                    req -> req.getRequester().equals(requester) && req.getStatus().equals("pending")
            );
            requesterClient.getOutgoingFriendRequests().removeIf(
                    req -> req.getResponder().equals(responder) && req.getStatus().equals("pending")
            );

            if (responderClient.getFriends() == null) {
                responderClient.setFriends(new ArrayList<>());
            }
            if (requesterClient.getFriends() == null) {
                requesterClient.setFriends(new ArrayList<>());
            }

            if (!responderClient.getFriends().contains(requester)) {
                responderClient.getFriends().add(requester);
            }
            if (!requesterClient.getFriends().contains(responder)) {
                requesterClient.getFriends().add(responder);
            }

            serverJSONHandler.writeClientAccountFile(
                    "src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList // Corrected file path
            );

            ClientCallbackInterface requesterCallback = clientCallbacks.get(requester);
            if(requesterCallback != null) {
                requesterCallback.onFriendAccepted();
            }

            ClientCallbackInterface responderCallback = clientCallbacks.get(responder);
            if(responderCallback != null) {
                responderCallback.onFriendAccepted();
            }

        } catch (Exception e) {
            System.err.println("Error during accept friend request: " + e.getMessage());
            e.printStackTrace();
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    @Override
    public void rejectFriendRequest(String jsonString) throws RemoteException{
        String requester;
        String responder;

        try {
            com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(jsonString).getAsJsonObject();

            requester = jsonObject.get("requester").getAsString();
            responder = jsonObject.get("responder").getAsString();

        } catch (Exception e) {
            System.err.println("Server Error: Failed to parse JSON for rejectFriendRequest: " + jsonString);
            throw new RemoteException("Invalid request format for rejection.", e);
        }

        fileLock.writeLock().lock();
        try {
            Client responderClient = findClient(responder);
            Client requesterClient = findClient(requester);

            if (responderClient == null || requesterClient == null) {
                return;
            }

            boolean removedIncoming = responderClient.getIncomingFriendRequests().removeIf(
                    req -> req.getRequester().equals(requester) && req.getStatus().equals("pending")
            );

            boolean removedOutgoing = requesterClient.getOutgoingFriendRequests().removeIf(
                    req -> req.getResponder().equals(responder) && req.getStatus().equals("pending")
            );

            if (removedIncoming && removedOutgoing) {
                serverJSONHandler.writeClientAccountFile(
                        "src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList // Corrected file path
                );

                ClientCallbackInterface responderCallback = clientCallbacks.get(responder);
                if(responderCallback != null) {
                    responderCallback.onFriendDeclined();
                }

                ClientCallbackInterface requesterCallback = clientCallbacks.get(requester);
                if(requesterCallback != null) {
                    requesterCallback.onFriendDeclined(); // Or a more specific callback like onFriendRejected
                }

            }

        } catch (Exception e) {
            System.err.println("Error during reject friend request: " + e.getMessage());
            e.printStackTrace();
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    @Override
    public void removeFromFriendsList(String jsonString) throws RemoteException{
        String unfriender;
        String friendToRemove;

        try {
            com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(jsonString).getAsJsonObject();
            unfriender = jsonObject.get("requester").getAsString();
            friendToRemove = jsonObject.get("responder").getAsString();
        } catch (Exception e) {
            System.err.println("Server Error: Failed to parse JSON for removeFromFriendsList: " + jsonString);
            throw new RemoteException("Invalid request format for unfriend.", e);
        }
        fileLock.writeLock().lock();
        try {
            Client unfrienderClient = findClient(unfriender);
            Client friendToRemoveClient = findClient(friendToRemove);
            if (unfrienderClient == null || friendToRemoveClient == null) {
                return;
            }
            unfrienderClient.getFriends().remove(friendToRemove);
            friendToRemoveClient.getFriends().remove(unfriender);
            serverJSONHandler.writeClientAccountFile(
                    "src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList
            );
            ClientCallbackInterface unfrienderCallback = clientCallbacks.get(unfriender);
            if(unfrienderCallback != null) {
                unfrienderCallback.onFriendRemoved();
            }
            ClientCallbackInterface friendToRemoveCallback = clientCallbacks.get(friendToRemove);
            if(friendToRemoveCallback != null) {
                friendToRemoveCallback.onFriendDeletedByOther();
            }
        } catch (Exception e) {
            System.err.println("Error during unfriend action: " + e.getMessage());
            e.printStackTrace();
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    @Override
    public void getSentFriendRequests(String jsonString) throws RemoteException{
        String username;
        try {
            com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(jsonString).getAsJsonObject();
            username = jsonObject.get("username").getAsString();
        } catch (Exception e) {
            System.err.println("Server Error: Failed to parse JSON for getSentFriendRequests: " + jsonString);
            throw new RemoteException("Invalid request format.", e);
        }
        Client client = findClient(username);
        ClientCallbackInterface callback = clientCallbacks.get(username);
        if (client == null || callback == null) {
            return;
        }
        java.util.List<String> sentRequests = new java.util.ArrayList<>();
        if (client.getOutgoingFriendRequests() != null) {
            for (Common.Objects.ClientObjects.FriendRequest req : client.getOutgoingFriendRequests()) {
                if (req.getStatus().equalsIgnoreCase("pending")) {
                    sentRequests.add(req.getResponder());
                }
            }
        }
        callbackExecutor.execute(() -> {
            try {
                callback.onGetSentFriendRequests(sentRequests);
            } catch (RemoteException e) {
                System.err.println("Failed to send sent friend requests to " + username + ": " + e.getMessage());
            }
        });
    }

    @Override
    public void getReceivedFriendRequests(String jsonString) throws RemoteException {
        String username;
        try {
            com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(jsonString).getAsJsonObject();
            username = jsonObject.get("username").getAsString();
        } catch (Exception e) {
            System.err.println("Server Error: Failed to parse JSON for getReceivedFriendRequests: " + jsonString);
            throw new RemoteException("Invalid request format.", e);
        }
        Client client = findClient(username);
        ClientCallbackInterface callback = clientCallbacks.get(username);
        if (client == null || callback == null) {
            return;
        }
        java.util.List<String> receivedRequests = new java.util.ArrayList<>();
        if (client.getIncomingFriendRequests() != null) {
            for (Common.Objects.ClientObjects.FriendRequest req : client.getIncomingFriendRequests()) {
                if (req.getStatus().equalsIgnoreCase("pending")) {
                    receivedRequests.add(req.getRequester());
                }
            }
        }
        callbackExecutor.execute(() -> {
            try {
                callback.onGetReceivedFriendRequests(receivedRequests);
            } catch (RemoteException e) {
                System.err.println("Failed to send received friend requests to " + username + ": " + e.getMessage());
            }
        });
    }

    @Override
    public void getFriends(String jsonString) throws RemoteException {
        String username;
        try {
            com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(jsonString).getAsJsonObject();
            username = jsonObject.get("username").getAsString();
        } catch (Exception e) {
            System.err.println("Server Error: Failed to parse JSON for getFriends: " + jsonString);
            throw new RemoteException("Invalid request format.", e);
        }
        Client client = findClient(username);
        ClientCallbackInterface callback = clientCallbacks.get(username);
        if (client == null || callback == null) {
            return;
        }
        java.util.List<String> friends = client.getFriends() != null ? client.getFriends() : new java.util.ArrayList<>();
        callbackExecutor.execute(() -> {
            try {
                callback.onGetFriendsList(friends);
            } catch (RemoteException e) {
                System.err.println("Failed to send friends list to " + username + ": " + e.getMessage());
            }
        });
    }

    //USER/ADMIN COMMON CHATTING METHODS
    @Override
    public List<GroupChat> getGroupChatHistories(String jsonString, String username) throws RemoteException {
        // Validation
        if (username == null || username.trim().isEmpty()) {
            throw new RemoteException("Username cannot be null or empty");
        }

        Client client = allUsers.get(username);
        if (client == null) {
            throw new RemoteException("User not found: " + username);
        }

        if (client.getGroupChats() == null) {
            client.setGroupChats(new ArrayList<>());
        }

        ClientCallbackInterface callback = clientCallbacks.get(username);
        if (callback == null) {
            return client.getGroupChats();
        }

        List<GroupChat> groupChats = client.getGroupChats();
        callbackExecutor.execute(() -> {
            try {
                callback.onGetGroupChatHistories(groupChats);
            } catch (RemoteException e) {
                System.err.println("Failed to send group chat histories to " + username + ": " + e.getMessage());
                e.printStackTrace();
            }
        });

        return groupChats;
    }

    @Override
    public void getChatHistories(String jsonString, String sender) throws RemoteException {
        if (sender == null || sender.trim().isEmpty()) {
            throw new RemoteException("Sender cannot be null or empty");
        }

        Client client = allUsers.get(sender);
        if (client == null) {
            throw new RemoteException("User not found: " + sender);
        }

        if (client.getConversations() == null) {
            client.setConversations(new ArrayList<>());
        }

        ClientCallbackInterface callback = clientCallbacks.get(sender);
        if (callback == null) {
            System.out.println("Warning: No callback registered for user: " + sender);
            return;
        }

        List<Conversation> conversations = client.getConversations();
        callbackExecutor.execute(() -> {
            try {
                callback.onGetChatHistories(conversations);
            } catch (RemoteException e) {
                System.err.println("Failed to send chat histories to " + sender + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public void getChatHistoryWith(String jsonString, String sender, String receiver) throws RemoteException {
        if (sender == null || sender.trim().isEmpty()) {
            throw new RemoteException("Sender cannot be null or empty");
        }

        if (receiver == null || receiver.trim().isEmpty()) {
            throw new RemoteException("Receiver cannot be null or empty");
        }

        Client client = allUsers.get(sender);
        if (client == null) {
            throw new RemoteException("User not found: " + sender);
        }

        if (client.getConversations() == null) {
            client.setConversations(new ArrayList<>());
        }

        List<Conversation> conversations = client.getConversations();
        Conversation conversationWith = null;

        for (Conversation conversation : conversations) {
            if (conversation.getRecipient() != null && conversation.getRecipient().equals(receiver)) {
                conversationWith = conversation;
                break;
            }
        }

        if (conversationWith == null) {
            conversationWith = new Conversation(receiver, new ArrayList<>());
        }

        ClientCallbackInterface callback = clientCallbacks.get(sender);
        if (callback == null) {
            return;
        }

        Conversation finalConversationWith = conversationWith;
        callbackExecutor.execute(() -> {
            try {
                callback.onGetChatHistoryWith(finalConversationWith);
            } catch (RemoteException e) {
                System.err.println("Failed to send chat history to " + sender + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public GroupChat getGroupChatHistoryWith(String jsonString, String username, String groupID) throws RemoteException {
        if (username == null || username.trim().isEmpty()) {
            throw new RemoteException("Username cannot be null or empty");
        }

        if (groupID == null || groupID.trim().isEmpty()) {
            throw new RemoteException("GroupID cannot be null or empty");
        }

        Client client = allUsers.get(username);
        if (client == null) {
            throw new RemoteException("User not found: " + username);
        }

        if (client.getGroupChats() == null) {
            client.setGroupChats(new ArrayList<>());
        }

        List<GroupChat> groupChats = client.getGroupChats();
        GroupChat groupChatWith = null;

        for (GroupChat groupChat : groupChats) {
            if (groupChat.getGroupID() != null && groupChat.getGroupID().equals(groupID)) {
                groupChatWith = groupChat;
                break;
            }
        }

        if (groupChatWith == null) {
            throw new RemoteException("Group chat not found: " + groupID);
        }

        ClientCallbackInterface callback = clientCallbacks.get(username);
        if (callback == null) {
            return groupChatWith;
        }

        GroupChat finalGroupChatWith = groupChatWith;
        callbackExecutor.execute(() -> {
            try {
                callback.onGetGroupChatHistoryWith(finalGroupChatWith);
            } catch (RemoteException e) {
                System.err.println("Failed to send group chat history to " + username + ": " + e.getMessage());
                e.printStackTrace();
            }
        });

        return groupChatWith;
    }
    @Override
    public String getChatHistoryOfUser(String jsonString) throws RemoteException {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            String username = jsonObject.get("username").getAsString();
            String admin = jsonObject.has("admin") ? jsonObject.get("admin").getAsString() : "";

            AdminCallbackInterface adminCallback = adminCallbacks.get(admin);

            fileLock.readLock().lock();
            try {
                Client client = allUsers.get(username);
                if (client == null) {
                    return "{}";
                }

                Gson gson = new Gson();
                String chatHistory = gson.toJson(client.getConversations());

                if (adminCallback != null) {
                    callbackExecutor.execute(() -> {
                        try {
                            adminCallback.onGetChatHistoryOfUser();
                        } catch (RemoteException e) {
                            System.err.println("Failed to notify admin: " + e.getMessage());
                        }
                    });
                }

                return chatHistory;

            } finally {
                fileLock.readLock().unlock();
            }

        } catch (Exception e) {
            System.err.println("Error in getChatHistoryOfUser: " + e.getMessage());
            e.printStackTrace();
            return "{}";
        }
    }

    @Override
    public String getChatHistoryOfGroup(String jsonString) throws RemoteException {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            String groupName = jsonObject.get("groupName").getAsString();
            String admin = jsonObject.has("admin") ? jsonObject.get("admin").getAsString() : "";

            AdminCallbackInterface adminCallback = adminCallbacks.get(admin);

            fileLock.readLock().lock();
            try {
                GroupChat foundGroup = null;
                for (Client client : allUsers.values()) {
                    if (client.getGroupChats() != null) {
                        for (GroupChat group : client.getGroupChats()) {
                            if (group.getGroupName().equals(groupName)) {
                                foundGroup = group;
                                break;
                            }
                        }
                        if (foundGroup != null) break;
                    }
                }

                if (foundGroup == null) {
                    return "{}";
                }

                Gson gson = new Gson();
                String chatHistory = gson.toJson(foundGroup.getMessages());

                if (adminCallback != null) {
                    callbackExecutor.execute(() -> {
                        try {
                            adminCallback.onGetChatHistoryOfGroup();
                        } catch (RemoteException e) {
                            System.err.println("Failed to notify admin: " + e.getMessage());
                        }
                    });
                }

                return chatHistory;

            } finally {
                fileLock.readLock().unlock();
            }

        } catch (Exception e) {
            System.err.println("Error in getChatHistoryOfGroup: " + e.getMessage());
            e.printStackTrace();
            return "{}";
        }
    }

    @Override
    public String getChatHistoryOfCommunity(String jsonString) throws RemoteException {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            String admin = jsonObject.has("admin") ? jsonObject.get("admin").getAsString() : "";

            AdminCallbackInterface adminCallback = adminCallbacks.get(admin);

            if (adminCallback != null) {
                callbackExecutor.execute(() -> {
                    try {
                        adminCallback.onGetChatHistoryOfCommunity();
                    } catch (RemoteException e) {
                        System.err.println("Failed to notify admin: " + e.getMessage());
                    }
                });
            }

            return "{}";

        } catch (Exception e) {
            System.err.println("Error in getChatHistoryOfCommunity: " + e.getMessage());
            e.printStackTrace();
            return "{}";
        }
    }

    @Override
    public void joinCommunityChat(String username, AdminCallbackInterface callback) throws RemoteException {
        try {
            communityChatActiveAdmins.add(username);
            Message systemMsg = new Message("SYSTEM", "", "Admin " + username + " joined the community chat");
            Gson gson = new Gson();
            String messageJson = gson.toJson(systemMsg);
            for (String user : communityChatActiveUsers) {
                ClientCallbackInterface cb = clientCallbacks.get(user);
                if (cb != null) {
                    try {
                        cb.onCommunityMessageReceived(messageJson, "");
                    } catch (RemoteException e) {
                        System.err.println("Failed to notify client " + user + ": " + e.getMessage());
                        communityChatActiveUsers.remove(user);
                    }
                }
            }

            for (String admin : communityChatActiveAdmins) {
                if (admin.equals(username)) continue;

                AdminCallbackInterface adminCb = adminCallbacks.get(admin);
                if (adminCb != null) {
                    try {
                        adminCb.onCommunityMessageReceived();
                    } catch (RemoteException e) {
                        System.err.println("Failed to notify admin " + admin + ": " + e.getMessage());
                        communityChatActiveAdmins.remove(admin);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error joining community chat: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Failed to join community chat", e);
        }
    }

    //USER/ADMIN PRIVATE CHATTING METHODS
    @Override
    public void sendPrivateMessage(String sender, String receiver, String message, String jsonString) throws RemoteException {

        Client senderClient = allUsers.get(sender);
        Client receiverClient = allUsers.get(receiver);

        Message msg = new Message(sender, receiver, message);

        if (senderClient.getConversations() == null) {
            senderClient.setConversations(new ArrayList<>());
        }
        if (receiverClient.getConversations() == null) {
            receiverClient.setConversations(new ArrayList<>());
        }

        Conversation senderConvo = null;
        Conversation receiverConvo = null;

        for (Conversation conv : senderClient.getConversations()) {
            if (conv.getRecipient() != null && conv.getRecipient().equals(receiver)) {
                senderConvo = conv;
                break;
            }
        }

        for (Conversation conv : receiverClient.getConversations()) {
            if (conv.getRecipient() != null && conv.getRecipient().equals(sender)) {
                receiverConvo = conv;
                break;
            }
        }

        if (senderConvo == null) {
            senderConvo = new Conversation(receiver, new ArrayList<>());
            senderClient.getConversations().add(senderConvo);
        }

        if (receiverConvo == null) {
            receiverConvo = new Conversation(sender, new ArrayList<>());
            receiverClient.getConversations().add(receiverConvo);
        }

        if (senderConvo.getMessages() == null) {
            senderConvo.setMessages(new ArrayList<>());
        }
        if (receiverConvo.getMessages() == null) {
            receiverConvo.setMessages(new ArrayList<>());
        }

        String conversationKey = serverJSONHandler.createConversationHashMapEntryString(sender, receiver);
        ReadWriteLock convoLock = conversationLocks.computeIfAbsent(conversationKey, k -> new ReentrantReadWriteLock());

        convoLock.writeLock().lock();
        try {
            senderConvo.getMessages().add(msg);
            receiverConvo.getMessages().add(msg);
            fileLock.writeLock().lock();
            try {
                serverJSONHandler.writeClientAccountFile("src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList);
            } finally {
                fileLock.writeLock().unlock();
            }
        } finally {
            convoLock.writeLock().unlock();
        }

        Gson gson = new Gson();
        String messageJson = gson.toJson(msg);

        // Notify sender
        ClientCallbackInterface senderCallback = clientCallbacks.get(sender);
        if (senderCallback != null) {
            final String finalMessageJson = messageJson;
            callbackExecutor.execute(() -> {
                try {
                    senderCallback.onPrivateMessageSent(
                            finalMessageJson,
                            serverJSONHandler.createJSONStringResponse("response", "SENT_MESSAGE", sender)
                    );
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            });
        } else {
        }

        ClientCallbackInterface receiverCallback = clientCallbacks.get(receiver);
        if (receiverCallback != null) {
            final String finalMessageJson = messageJson;
            callbackExecutor.execute(() -> {
                try{
                    receiverCallback.onPrivateMessageReceived(
                            finalMessageJson,
                            serverJSONHandler.createJSONStringResponse("response", "RECEIVED_MESSAGE", receiver)
                    );
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public void sendPrivateFile(String sender, String receiver, String fileName, String fileData, String jsonString) throws RemoteException {
        if (sender == null || receiver == null || fileName == null || fileData == null) {
            throw new RemoteException("Sender, receiver, fileName, or fileData cannot be null");
        }

        Client senderClient = allUsers.get(sender);
        Client receiverClient = allUsers.get(receiver);

        if (senderClient == null) {
            throw new RemoteException("Sender user not found: " + sender);
        }

        if (receiverClient == null) {
            throw new RemoteException("Receiver user not found: " + receiver);
        }

        Message fileMsg = new Message(sender, receiver, fileName, fileData, true);

        if (senderClient.getConversations() == null) {
            senderClient.setConversations(new ArrayList<>());
        }
        if (receiverClient.getConversations() == null) {
            receiverClient.setConversations(new ArrayList<>());
        }

        Conversation senderConvo = null;
        Conversation receiverConvo = null;

        for (Conversation conv : senderClient.getConversations()) {
            if (conv.getRecipient() != null && conv.getRecipient().equals(receiver)) {
                senderConvo = conv;
                break;
            }
        }

        for (Conversation conv : receiverClient.getConversations()) {
            if (conv.getRecipient() != null && conv.getRecipient().equals(sender)) {
                receiverConvo = conv;
                break;
            }
        }

        if (senderConvo == null) {
            senderConvo = new Conversation(receiver, new ArrayList<>());
            senderClient.getConversations().add(senderConvo);
        }

        if (receiverConvo == null) {
            receiverConvo = new Conversation(sender, new ArrayList<>());
            receiverClient.getConversations().add(receiverConvo);
        }

        if (senderConvo.getMessages() == null) {
            senderConvo.setMessages(new ArrayList<>());
        }
        if (receiverConvo.getMessages() == null) {
            receiverConvo.setMessages(new ArrayList<>());
        }

        String conversationKey = serverJSONHandler.createConversationHashMapEntryString(sender, receiver);
        ReadWriteLock convoLock = conversationLocks.computeIfAbsent(conversationKey, k -> new ReentrantReadWriteLock());

        convoLock.writeLock().lock();
        try {
            senderConvo.getMessages().add(fileMsg);
            receiverConvo.getMessages().add(fileMsg);

            fileLock.writeLock().lock();
            try {
                serverJSONHandler.writeClientAccountFile("src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList);
            } finally {
                fileLock.writeLock().unlock();
            }
        } finally {
            convoLock.writeLock().unlock();
        }

        Gson gson = new Gson();
        String messageJson = gson.toJson(fileMsg);

        ClientCallbackInterface senderCallback = clientCallbacks.get(sender);
        if (senderCallback != null) {
            final String finalMessageJson = messageJson;
            callbackExecutor.execute(() -> {
                try {
                    senderCallback.onPrivateFileSent(finalMessageJson, jsonString);
                } catch (RemoteException e) {
                    System.out.println("Failed to notify sender: " + e.getMessage());
                }
            });
        }

        // Notify receiver
        ClientCallbackInterface receiverCallback = clientCallbacks.get(receiver);
        if (receiverCallback != null) {
            final String finalMessageJson = messageJson;
            callbackExecutor.execute(() -> {
                try{
                    receiverCallback.onPrivateFileReceived(finalMessageJson, jsonString);
                } catch (RemoteException e) {
                }
            });
        }

    }

    //USER/ADMIN GROUP CHATTING METHODS
    @Override
    public void sendGroupMessage(String sender, String groupID, String message, String jsonString) throws RemoteException {
        if (sender == null || groupID == null || message == null) {
            throw new RemoteException("Sender, groupID, or message cannot be null");
        }

        Client senderClient = allUsers.get(sender);
        if (senderClient == null) {
            throw new RemoteException("Sender user not found: " + sender);
        }

        GroupChat senderGroup = null;
        for (GroupChat gc : senderClient.getGroupChats()) {
            if (gc.getGroupID().equals(groupID)) {
                senderGroup = gc;
                break;
            }
        }

        if (senderGroup == null) {
            throw new RemoteException("Group not found: " + groupID);
        }

        if (!senderGroup.isMember(sender)) {
            throw new RemoteException("User " + sender + " is not a member of group " + groupID);
        }

        List<String> receivers = new ArrayList<>(senderGroup.getReceivers());
        Message msg = new Message(sender, receivers, message);

        fileLock.writeLock().lock();
        try {
            for (String memberUsername : receivers) {
                Client memberClient = allUsers.get(memberUsername);
                if (memberClient != null && memberClient.getGroupChats() != null) {
                    // Find the group in this member's groupChats
                    for (GroupChat memberGroup : memberClient.getGroupChats()) {
                        if (memberGroup.getGroupID().equals(groupID)) {
                            if (memberGroup.getMessages() == null) {
                                memberGroup.setMessages(new ArrayList<>());
                            }
                            memberGroup.getMessages().add(msg);
                            break;
                        }
                    }
                }
            }
            serverJSONHandler.writeClientAccountFile("src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList);
        } finally {
            fileLock.writeLock().unlock();
        }

        Gson gson = new Gson();
        String messageJson = gson.toJson(msg);

        for (String memberUsername : receivers) {
            ClientCallbackInterface memberCallback = clientCallbacks.get(memberUsername);
            if (memberCallback != null) {
                final String finalMessageJson = messageJson;
                callbackExecutor.execute(() -> {
                    try {
                        if (memberUsername.equals(sender)) {
                            memberCallback.onGroupMessageSent(finalMessageJson, jsonString);
                        } else {
                            memberCallback.onGroupMessageReceived(finalMessageJson, jsonString);
                        }
                    } catch (RemoteException e) {
                        System.out.println("Failed to notify member " + memberUsername + ": " + e.getMessage());
                    }
                });
            }
        }

    }

    @Override
    public void sendGroupFile(String sender, String groupID, String fileName, String fileData, String jsonString) throws RemoteException {
        if (sender == null || groupID == null || fileName == null || fileData == null) {
            throw new RemoteException("Sender, groupID, fileName, or fileData cannot be null");
        }

        Client senderClient = allUsers.get(sender);
        if (senderClient == null) {
            throw new RemoteException("Sender user not found: " + sender);
        }

        // Find the group
        GroupChat senderGroup = null;
        for (GroupChat gc : senderClient.getGroupChats()) {
            if (gc.getGroupID().equals(groupID)) {
                senderGroup = gc;
                break;
            }
        }

        if (senderGroup == null) {
            throw new RemoteException("Group not found: " + groupID);
        }

        if (!senderGroup.isMember(sender)) {
            throw new RemoteException("User " + sender + " is not a member of group " + groupID);
        }

        List<String> receivers = new ArrayList<>(senderGroup.getReceivers());
        Message fileMsg = new Message(sender, receivers, fileName, fileData, true);

        fileLock.writeLock().lock();
        try {
            for (String memberUsername : receivers) {
                Client memberClient = allUsers.get(memberUsername);
                if (memberClient != null && memberClient.getGroupChats() != null) {
                    for (GroupChat memberGroup : memberClient.getGroupChats()) {
                        if (memberGroup.getGroupID().equals(groupID)) {
                            if (memberGroup.getMessages() == null) {
                                memberGroup.setMessages(new ArrayList<>());
                            }
                            memberGroup.getMessages().add(fileMsg);
                            break;
                        }
                    }
                }
            }

            serverJSONHandler.writeClientAccountFile("src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList);
        } finally {
            fileLock.writeLock().unlock();
        }

        Gson gson = new Gson();
        String messageJson = gson.toJson(fileMsg);
        for (String memberUsername : receivers) {
            ClientCallbackInterface memberCallback = clientCallbacks.get(memberUsername);
            if (memberCallback != null) {
                final String finalMessageJson = messageJson;
                callbackExecutor.execute(() -> {
                    try {
                        if (memberUsername.equals(sender)) {
                            memberCallback.onGroupFileSent(finalMessageJson, jsonString);
                        } else {
                            memberCallback.onGroupFileReceived(finalMessageJson, jsonString);
                        }
                    } catch (RemoteException e) {
                        System.out.println("Failed to notify member: " + e.getMessage());
                    }
                });
            }
        }

    }

    @Override
    public void createGroupChat(String jsonString) throws RemoteException{
        String creator;
        String groupName;
        java.util.List<String> requestedMembers = new java.util.ArrayList<>();

        try {
            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
            creator = json.get("creator").getAsString();
            groupName = json.get("groupName").getAsString();
            if (json.has("members") && json.get("members").isJsonArray()) {
                for (var el : json.get("members").getAsJsonArray()) {
                    String m = el.getAsString();
                    if (m != null && !m.trim().isEmpty()) requestedMembers.add(m.trim());
                }
            }

        } catch (Exception e) {
            System.err.println("Server Error: Failed to parse createGroupChat JSON: " + jsonString);
            throw new RemoteException("Invalid create group format.", e);
        }

        String groupID = "grp-" + System.currentTimeMillis();

        fileLock.writeLock().lock();
        try {
            Client creatorClient = findClient(creator);
            if (creatorClient == null) {
                throw new RemoteException("Creator not found: " + creator);
            }

            if (creatorClient.getGroupChats() == null) {
                creatorClient.setGroupChats(new ArrayList<>());
            }

            // Prevent duplicate group names GLOBALLY (case-sensitive)
            for (Client acc : clientAccountsList.getClients()) {
                if (acc.getGroupChats() == null) continue;
                for (GroupChat existing : acc.getGroupChats()) {
                    if (existing.getGroupName() != null && existing.getGroupName().equals(groupName)) {
                        ClientCallbackInterface cb = clientCallbacks.get(creator);
                        if (cb != null) {
                            callbackExecutor.execute(() -> {
                                try { cb.onFailedGroupCreation(); } catch (RemoteException ignored) {}
                            });
                        }
                        return;
                    }
                }
            }

            // Enforce: creator can only add their friends
            java.util.Set<String> friendSet = new java.util.HashSet<>();
            if (creatorClient.getFriends() != null) {
                friendSet.addAll(creatorClient.getFriends());
            }

            java.util.List<String> finalMembers = new java.util.ArrayList<>();
            finalMembers.add(creator);
            for (String req : requestedMembers) {
                if (friendSet.contains(req) && !finalMembers.contains(req)) {
                    finalMembers.add(req);
                }
            }

            // Create group for each member (including creator)
            for (String member : finalMembers) {
                Client memberClient = findClient(member);
                if (memberClient == null) continue;
                if (memberClient.getGroupChats() == null) {
                    memberClient.setGroupChats(new ArrayList<>());
                }
                java.util.List<String> receivers = new java.util.ArrayList<>(finalMembers);
                GroupChat gc = new GroupChat(groupID, groupName, receivers, new ArrayList<>());
                memberClient.getGroupChats().add(gc);
            }

            // Persist to file
            serverJSONHandler.writeClientAccountFile("src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList);

        } catch (Exception e) {
            System.err.println("Error during createGroupChat: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Failed to create group", e);
        } finally {
            fileLock.writeLock().unlock();
        }

        // Notify creator only
        ClientCallbackInterface cb = clientCallbacks.get(creator);
        if (cb != null) {
            callbackExecutor.execute(() -> {
                try { cb.onSuccessfulGroupCreation(); } catch (RemoteException ignored) {}
            });
        }
    }

    @Override
    public void addUserToGroup(String jsonString) throws RemoteException{

    }

    @Override
    public void removeUserFromGroup(String jsonString) throws RemoteException{

    }

    //USER/ADMIN COMMUNITY CHATTING METHODS
    @Override
    public void joinCommunityChat(String username, ClientCallbackInterface callback) throws RemoteException {
        try {

            communityChatActiveUsers.add(username);

            // Notify all OTHER users that someone joined
            Message systemMsg = new Message("SYSTEM", "", username + " joined the community chat");
            Gson gson = new Gson();
            String messageJson = gson.toJson(systemMsg);

            for (String user : communityChatActiveUsers) {
                if (user.equals(username)) continue; // Skip the person who just joined

                ClientCallbackInterface cb = clientCallbacks.get(user);
                if (cb != null) {
                    try {
                        cb.onCommunityMessageReceived(messageJson, "");
                    } catch (RemoteException e) {
                        System.err.println("Failed to notify " + user + ": " + e.getMessage());
                        communityChatActiveUsers.remove(user);
                    }
                }
            }


        } catch (Exception e) {
            System.err.println("Error joining community chat: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Failed to join community chat", e);
        }
    }

    @Override
    public void leaveCommunityChat(String username) throws RemoteException {
        try {

            communityChatActiveUsers.remove(username);

            // Notify remaining users
            Message systemMsg = new Message("SYSTEM", "", username + " left the community chat");
            Gson gson = new Gson();
            String messageJson = gson.toJson(systemMsg);

            for (String user : communityChatActiveUsers) {
                ClientCallbackInterface cb = clientCallbacks.get(user);
                if (cb != null) {
                    try {
                        cb.onCommunityMessageReceived(messageJson, "");
                    } catch (RemoteException e) {
                        System.err.println("Failed to notify " + user + ": " + e.getMessage());
                        communityChatActiveUsers.remove(user);
                    }
                }
            }


        } catch (Exception e) {
            System.err.println("Error leaving community chat: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void sendCommunityMessage(String sender, String message, String jsonString) throws RemoteException {
        // Check if sender is an admin
        if (communityChatActiveAdmins.contains(sender)) {
            sendCommunityMessageFromAdmin(sender, message, jsonString);
            return;
        }

        // Otherwise, handle as client message (existing code)
        try {

            if (!communityChatActiveUsers.contains(sender)) {
                System.err.println("User " + sender + " is not in community chat");
                return;
            }

            Message msg = new Message(sender, "", message);
            Gson gson = new Gson();
            String messageJson = gson.toJson(msg);

            // Broadcast to ALL CLIENTS
            for (String user : communityChatActiveUsers) {
                ClientCallbackInterface cb = clientCallbacks.get(user);
                if (cb != null) {
                    try {
                        cb.onCommunityMessageReceived(messageJson, "");
                    } catch (RemoteException e) {
                        System.err.println("Failed to send to client " + user + ": " + e.getMessage());
                        communityChatActiveUsers.remove(user);
                    }
                }
            }

            // Broadcast to ALL ADMINS
            for (String admin : communityChatActiveAdmins) {
                AdminCallbackInterface adminCb = adminCallbacks.get(admin);
                if (adminCb != null) {
                    try {
                        adminCb.onReceiveCommunityMessage(messageJson);
                    } catch (RemoteException e) {
                        System.err.println("Failed to send to admin " + admin + ": " + e.getMessage());
                        communityChatActiveAdmins.remove(admin);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error sending community message: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Failed to send community message", e);
        }
    }

    @Override
    public void sendCommunityFile(String sender, String fileName, String fileData, String jsonString) throws RemoteException {
        try {

            if (!communityChatActiveUsers.contains(sender)) {
                System.err.println("User " + sender + " is not in community chat");
                return;
            }

            Message msg = new Message(sender, "", fileName, fileData, true);
            Gson gson = new Gson();
            String messageJson = gson.toJson(msg);

            // Broadcast to ALL users in community chat
            for (String user : communityChatActiveUsers) {
                ClientCallbackInterface cb = clientCallbacks.get(user);
                if (cb != null) {
                    try {
                        cb.onCommunityMessageReceived(messageJson, "");
                    } catch (RemoteException e) {
                        System.err.println("Failed to send to " + user + ": " + e.getMessage());
                        communityChatActiveUsers.remove(user);
                    }
                }
            }


        } catch (Exception e) {
            System.err.println("Error sending community file: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Failed to send community file", e);
        }
    }

    //HELPERS
    public void sendCommunityMessageFromAdmin(String sender, String message, String jsonString) throws RemoteException {
        try {

            if (!communityChatActiveAdmins.contains(sender)) {
                System.err.println("Admin " + sender + " is not in community chat");
                return;
            }

            Message msg = new Message("Admin-" + sender, "", message);
            Gson gson = new Gson();
            String messageJson = gson.toJson(msg);

            // Broadcast to ALL CLIENTS in community chat
            for (String user : communityChatActiveUsers) {
                ClientCallbackInterface cb = clientCallbacks.get(user);
                if (cb != null) {
                    try {
                        cb.onCommunityMessageReceived(messageJson, "");
                    } catch (RemoteException e) {
                        System.err.println("Failed to send to client " + user + ": " + e.getMessage());
                        communityChatActiveUsers.remove(user);
                    }
                }
            }

            // Broadcast to ALL ADMINS in community chat
            for (String admin : communityChatActiveAdmins) {
                AdminCallbackInterface adminCb = adminCallbacks.get(admin);
                if (adminCb != null) {
                    try {
                        // Send the formatted message
                        adminCb.onReceiveCommunityMessage(messageJson);
                    } catch (RemoteException e) {
                        System.err.println("Failed to send to admin " + admin + ": " + e.getMessage());
                        communityChatActiveAdmins.remove(admin);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error sending admin community message: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Failed to send admin community message", e);
        }
    }
    public void leaveCommunityChat(String username, boolean isAdmin) throws RemoteException {
        try {
            if (isAdmin) {
                communityChatActiveAdmins.remove(username);
            } else {
                communityChatActiveUsers.remove(username);
            }

            Message systemMsg = new Message("SYSTEM", "",
                    (isAdmin ? "Admin " : "") + username + " left the community chat");
            Gson gson = new Gson();
            String messageJson = gson.toJson(systemMsg);

            for (String user : communityChatActiveUsers) {
                ClientCallbackInterface cb = clientCallbacks.get(user);
                if (cb != null) {
                    try {
                        cb.onCommunityMessageReceived(messageJson, "");
                    } catch (RemoteException e) {
                        System.err.println("Failed to notify " + user + ": " + e.getMessage());
                        communityChatActiveUsers.remove(user);
                    }
                }
            }

            // Notify admins
            for (String admin : communityChatActiveAdmins) {
                AdminCallbackInterface adminCb = adminCallbacks.get(admin);
                if (adminCb != null) {
                    try {
                        adminCb.onCommunityMessageReceived();
                    } catch (RemoteException e) {
                        System.err.println("Failed to notify admin " + admin + ": " + e.getMessage());
                        communityChatActiveAdmins.remove(admin);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error leaving community chat: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void registerAdminCallback(String username, AdminCallbackInterface callback) throws RemoteException {
        if (username == null || callback == null) {
            throw new RemoteException("Username and callback cannot be null");
        }
        adminCallbacks.put(username, callback);
    }

    public void unregisterAdminCallback(String username) throws RemoteException {
        if (username == null) {
            throw new RemoteException("Username cannot be null");
        }
        adminCallbacks.remove(username);
    }
    private Client findClient(String username) {
        if (clientAccountsList != null && clientAccountsList.getClients() != null) {
            for (Client c : clientAccountsList.getClients()) {
                if (c.getUsername().equalsIgnoreCase(username)) {
                    return c;
                }
            }
        }
        return null;
    }

    private boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        if (username.length() < 3) {
            return false;
        }

        // Only letters and numbers allowed
        return username.matches("^[a-zA-Z0-9]+$");
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }

        if (password.length() < 4) {
            return false;
        }

        // Only letters and numbers allowed
        return password.matches("^[a-zA-Z0-9]+$");
    }

    //FILE UPDATER
    private void updateClientJSONFile(String filePath, ClientAccountsList fileUpdates) throws RemoteException{

    }
    private void updateAdminJSONFile(String filePath, AdminAccountsList fileUpdates) throws RemoteException{

    }

    @Override
    public void registerClientCallback(String username, ClientCallbackInterface callback) throws RemoteException {
        if (username == null || callback == null) {
            throw new RemoteException("Username and callback cannot be null");
        }
        clientCallbacks.put(username, callback);
    }

    @Override
    public void unregisterClientCallback(String username) throws RemoteException {
        if (username == null) {
            throw new RemoteException("Username cannot be null");
        }
        clientCallbacks.remove(username);
    }

    @Override
    public List<String> getAllUsernames() throws RemoteException {
        List<String> allUsernames = new ArrayList<>(allUsers.keySet());
        return allUsernames;
    }
}