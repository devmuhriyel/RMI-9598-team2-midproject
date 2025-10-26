package Server;

import Common.Interfaces.AdminCallbackInterface;
import Common.Interfaces.AdminInterface;
import Common.Interfaces.ClientCallbackInterface;
import Common.Interfaces.ClientInterface;
import Common.Objects.AdminObjects.Admin;
import Common.Objects.AdminObjects.AdminAccountsList;
import Common.Objects.ClientObjects.*;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
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
    private ConcurrentHashMap<String, ReadWriteLock> conversationLocks;

    private ScheduledExecutorService callbackExecutor;
    private ReadWriteLock fileLock;
    private AdminAccountsList adminAccountsList;
    private ClientAccountsList clientAccountsList;
    private ServerJSONHandler serverJSONHandler;

    //CONSTRUCTORS
    public ClientAndAdminImplementation() throws RemoteException {
        super();

        // Initialize all data structures FIRST
        allUsers = new ConcurrentHashMap<>();
        allAdmins = new ConcurrentHashMap<>();
        clientCallbacks = new ConcurrentHashMap<>();
        adminCallbacks = new ConcurrentHashMap<>();
        conversationLocks = new ConcurrentHashMap<>();
        callbackExecutor = Executors.newSingleThreadScheduledExecutor();
        fileLock = new ReentrantReadWriteLock();

        // Initialize JSON handler
        serverJSONHandler = new ServerJSONHandler();

        // Read account files
        adminAccountsList = serverJSONHandler.readAdminAccountFile("src/main/resources/JSONFiles/AdminAccounts.json");
        clientAccountsList = serverJSONHandler.readClientAccountFile("src/main/resources/JSONFiles/ClientAccounts.json");

        // Check if lists were loaded successfully
        if (adminAccountsList == null) {
            System.err.println("Warning: adminAccountsList is null, creating new one");
            adminAccountsList = new AdminAccountsList();
        }
        if (clientAccountsList == null) {
            System.err.println("Warning: clientAccountsList is null, creating new one");
            clientAccountsList = new ClientAccountsList();
        }

        // Populate maps
        List<Client> clients = clientAccountsList.getClients();
        List<Admin> admins = adminAccountsList.getAdmins();

        if (clients != null) {
            for(Client client: clients) {
                allUsers.put(client.getUsername(), client);
                System.out.println("Loaded user: " + client.getUsername());
            }
        } else {
            System.err.println("Warning: No clients loaded");
        }

        if (admins != null) {
            for(Admin admin: admins) {
                allAdmins.put(admin.getUsername(), admin);
                System.out.println("Loaded admin: " + admin.getUsername());
            }
        } else {
            System.err.println("Warning: No admins loaded");
        }

        System.out.println("Server initialized with " + allUsers.size() + " users and " + allAdmins.size() + " admins");
    }

    //USER LOG IN, SIGN UP, AND OTHER RELATED METHODS
    @Override
    public void signUp(String username, String password, String verificationCode, String jsonString, AdminCallbackInterface adminCallback) throws RemoteException {
        // Admin signup not implemented - Admin MVC remains unchanged
    }

    @Override
    public void logIn(String username, String jsonString, AdminCallbackInterface adminCallback) throws RemoteException {
        // Admin login not implemented - Admin MVC remains unchanged
    }

    @Override
    public void signUp(String username, String password, String verificationCode, String jsonString, ClientCallbackInterface clientCallback) throws RemoteException {
        System.out.println("Client signup attempt for: " + username);
        
        fileLock.writeLock().lock();
        try {
            // Check if client already exists
            if (allUsers.containsKey(username)) {
                System.out.println("Client signup failed: Username already exists - " + username);
                clientCallback.onFailedSignUp();
                return;
            }
            
            // Create new client
            Client newClient = new Client(username, password, verificationCode);
            allUsers.put(username, newClient);
            
            // Add to client accounts list
            if (clientAccountsList.getClients() == null) {
                clientAccountsList.setClients(new ArrayList<>());
            }
            clientAccountsList.getClients().add(newClient);
            
            // Save to file
            serverJSONHandler.writeClientAccountFile("src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList);
            
            System.out.println("Client signup successful: " + username);
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
        System.out.println("Client login attempt for: " + username);
        
        try {
            // Parse JSON to get password
            com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(jsonString).getAsJsonObject();
            String password = jsonObject.get("password").getAsString();
            
            // Check if client exists and password matches
            Client client = allUsers.get(username);
            if (client != null && client.getPassword().equals(password)) {
                System.out.println("Client login successful: " + username);
                
                // Register callback for this client
                clientCallbacks.put(username, clientCallback);
                
                clientCallback.onSuccessfulLogin();
            } else {
                System.out.println("Client login failed: Invalid credentials - " + username);
                clientCallback.onFailedLogin();
            }
            
        } catch (Exception e) {
            System.err.println("Error during client login: " + e.getMessage());
            e.printStackTrace();
            clientCallback.onFailedLogin();
        }
    }

    @Override
    public void logOut(String username, String jsonString) throws RemoteException {
        System.out.println("Client logout attempt for: " + username);
        
        try {
            // Unregister client callback if it exists
            if (clientCallbacks.containsKey(username)) {
                clientCallbacks.remove(username);
                System.out.println("Client callback unregistered for: " + username);
            }
            
            System.out.println("Client logout successful for: " + username);
            
        } catch (Exception e) {
            System.err.println("Error during client logout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void changePassword(String jsonString) throws RemoteException{

    }

    @Override
    public void verifyCode(String jsonString) throws RemoteException{

    }

    //USER/ADMIN HOME UI METHODS
    @Override
    public void searchUsers(String jsonString) throws RemoteException{

    }

    @Override
    public void searchGroups(String jsonString) throws RemoteException{

    }

    @Override
    public void deleteUser(String jsonString) throws RemoteException {

    }

    @Override
    public void deleteGroup(String jsonString) throws RemoteException{

    }

    @Override
    public void getUnreadMessages(String jsonString) throws RemoteException{

    }

    @Override
    public void sendFriendRequest(String jsonString) throws RemoteException {
        String requesterUsername;
        String targetUsername;

        try {
            JsonParser parser = new JsonParser();
            com.google.gson.JsonObject jsonObject = parser.parse(jsonString).getAsJsonObject();

            requesterUsername = jsonObject.get("requester").getAsString();
            targetUsername = jsonObject.get("target").getAsString();

        } catch (Exception e) {
            System.err.println("Server Error: Failed to parse friend request JSON: " + jsonString);
            throw new RemoteException("Invalid request format.", e);
        }
        fileLock.writeLock().lock();
        try {
            boolean success = handleFriendRequestLogic(requesterUsername, targetUsername);

            if (success) {
                serverJSONHandler.writeClientAccountFile(
                        "src/main/resources/JSONFiles/ClientAccountsList.json",
                        clientAccountsList
                );
                System.out.println("Friend request saved: " + requesterUsername + " -> " + targetUsername);

            } else {
                System.err.println("Friend request failed for logic: " + requesterUsername + " -> " + targetUsername);
            }

        } finally {
            fileLock.writeLock().unlock();
        }
    }
    private boolean handleFriendRequestLogic(String requester, String target) {
        Client requesterClient = null;
        Client targetClient = null;

        for (Client c : clientAccountsList.getClients()) {
            if (c.getUsername().equalsIgnoreCase(requester)) {
                requesterClient = c;
            } else if (c.getUsername().equalsIgnoreCase(target)) {
                targetClient = c;
            }
        }

        if (requesterClient == null || targetClient == null) {
            return false;
        }

        if (requesterClient.getOutgoingFriendRequests() != null) {
            for (FriendRequest fr : requesterClient.getOutgoingFriendRequests()) {
                if (fr.getResponder().equalsIgnoreCase(target) && fr.getStatus().equalsIgnoreCase("pending")) {
                    return false;
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
    public void changeUsername(String jsonString) throws RemoteException{

    }

    @Override
    public void changePasswordWithOldPassword(String jsonString) throws RemoteException{

    }

    //USER/ADMIN FRIEND REQUESTS UI METHODS
    @Override
    public void cancelFriendRequest(String jsonString) throws RemoteException {
        String requester;
        String target;

        try {
            com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(jsonString).getAsJsonObject();

            requester = jsonObject.get("requester").getAsString();
            target = jsonObject.get("target").getAsString();

        } catch (Exception e) {
            System.err.println("Server Error: Failed to parse JSON for cancelFriendRequest: " + jsonString);
            throw new RemoteException("Invalid request format for cancellation.", e);
        }

        fileLock.writeLock().lock();
        try {
            Client requesterClient = findClient(requester);
            Client targetClient = findClient(target);

            if (requesterClient == null || targetClient == null) {
                System.out.println("Cancel Failed: One or both users not found.");
                return;
            }

            requesterClient.getOutgoingFriendRequests().removeIf(
                    req -> req.getResponder().equals(target) && req.getStatus().equals("pending")
            );

            targetClient.getIncomingFriendRequests().removeIf(
                    req -> req.getRequester().equals(requester) && req.getStatus().equals("pending")
            );

            serverJSONHandler.writeClientAccountFile(
                    "src/main/resources/JSONFiles/ClientAccountsList.json", clientAccountsList
            );
            System.out.println("Friend request cancelled: " + requester + " -> " + target);

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
            responder = jsonObject.get("target").getAsString();

        } catch (Exception e) {
            System.err.println("Server Error: Failed to parse JSON for acceptFriendRequest: " + jsonString);
            throw new RemoteException("Invalid request format for acceptance.", e);
        }

        fileLock.writeLock().lock();
        try {
            Client responderClient = findClient(responder);
            Client requesterClient = findClient(requester);

            if (responderClient == null || requesterClient == null) {
                System.out.println("Accept Failed: One or both users not found.");
                return;
            }

            requesterClient.getOutgoingFriendRequests().removeIf(
                    req -> req.getResponder().equals(responder)
            );
            responderClient.getIncomingFriendRequests().removeIf(
                    req -> req.getRequester().equals(requester)
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
                    "src/main/resources/JSONFiles/ClientAccountsList.json", clientAccountsList
            );
            System.out.println("Friendship established: " + requester + " and " + responder);

            // TODO: Implement RMI Callback to notify the 'requester' (the friend who was accepted)

        } finally {
            fileLock.writeLock().unlock();
        }
    }

    @Override
    public void rejectFriendRequest(String jsonString) throws RemoteException{

    }

    @Override
    public void removeFromFriendsList(String jsonString) throws RemoteException{

    }

    //USER/ADMIN COMMON CHATTING METHODS
    @Override
    public List<Conversation> getChatHistories(String jsonString, String sender) throws RemoteException{
        Client client = allUsers.get(sender);
        clientCallbacks.get(sender).onGetChatHistories(client.getConversations());
        return client.getConversations();
    }

    @Override
    public Conversation getChatHistoryWith(String jsonString, String sender, String receiver) throws RemoteException{
        Client client = allUsers.get(sender);
        List<Conversation> conversations = client.getConversations();
        Conversation conversationWith = null;
        for (Conversation conversation : conversations) {
            if(conversation.getRecipient().equals(receiver)) {
                conversationWith = conversation;
            }
        }
        clientCallbacks.get(sender).onGetChatHistoryWith(conversationWith);
        return conversationWith;
    }
    @Override
    public String getChatHistoryOfUser(String jsonString) throws RemoteException{
        return "";
    }

    @Override
    public String getChatHistoryOfGroup(String jsonString) throws RemoteException{
        return "";
    }

    @Override
    public String getChatHistoryOfCommunity(String jsonString) throws RemoteException{
        return "";
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
                System.out.println("Message saved to file");
            } finally {
                fileLock.writeLock().unlock();
            }
        } finally {
            convoLock.writeLock().unlock();
        }

        Gson gson = new Gson();
        String messageJson = gson.toJson(msg);
        System.out.println("Message JSON: " + messageJson);

        // Notify sender
        ClientCallbackInterface senderCallback = clientCallbacks.get(sender);
        if (senderCallback != null) {
            final String finalMessageJson = messageJson;
            callbackExecutor.execute(() -> {
                try {
                    System.out.println("Notifying sender: " + sender);
                    senderCallback.onPrivateMessageSent(
                            finalMessageJson,
                            serverJSONHandler.createJSONStringResponse("response", "SENT_MESSAGE", sender)
                    );
                } catch (RemoteException e) {
                    System.out.println("Failed to notify sender: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } else {
            System.out.println("Warning: Sender callback not registered for: " + sender);
        }

        ClientCallbackInterface receiverCallback = clientCallbacks.get(receiver);
        if (receiverCallback != null) {
            final String finalMessageJson = messageJson;
            callbackExecutor.execute(() -> {
                try{
                    System.out.println("Notifying receiver: " + receiver);
                    receiverCallback.onPrivateMessageReceived(
                            finalMessageJson,
                            serverJSONHandler.createJSONStringResponse("response", "RECEIVED_MESSAGE", receiver)
                    );
                } catch (RemoteException e) {
                    System.out.println("Failed to notify receiver: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } else {
            System.out.println("Receiver " + receiver + " is not online (no callback registered)");
        }

        System.out.println("Message processing completed successfully");
    }

    @Override
    public void sendPrivateFile(String jsonString) throws RemoteException{

    }

    //USER/ADMIN GROUP CHATTING METHODS
    @Override
    public void sendGroupMessage(String jsonString) throws RemoteException{

    }

    @Override
    public void sendGroupFile(String jsonString) throws RemoteException{

    }

    @Override
    public void createGroupChat(String jsonString) throws RemoteException{

    }

    @Override
    public void addUserToGroup(String jsonString) throws RemoteException{

    }

    @Override
    public void removeUserFromGroup(String jsonString) throws RemoteException{

    }

    //USER/ADMIN COMMUNITY CHATTING METHODS
    @Override
    public void sendCommunityMessage(String jsonString) throws RemoteException{

    }

    //HELPERS
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
        System.out.println("✓ Callback registered for user: " + username);
    }

    @Override
    public void unregisterClientCallback(String username) throws RemoteException {
        if (username == null) {
            throw new RemoteException("Username cannot be null");
        }
        clientCallbacks.remove(username);
        System.out.println("✓ Callback unregistered for user: " + username);
    }
}