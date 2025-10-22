package Server;

import Common.Interfaces.AdminCallbackInterface;
import Common.Interfaces.AdminInterface;
import Common.Interfaces.ClientCallbackInterface;
import Common.Interfaces.ClientInterface;
import Common.Objects.AdminObjects.Admin;
import Common.Objects.AdminObjects.AdminAccountsList;
import Common.Objects.ClientObjects.*;
import com.google.gson.JsonParser;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ImplementationClass extends UnicastRemoteObject implements ClientInterface, AdminInterface {
    //VARIABLES
    private ConcurrentHashMap<String, Client> onlineUsers;
    private ConcurrentHashMap<String, Client> allUsers;
    private ConcurrentHashMap<String, Admin> onlineAdmins;
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
    public ImplementationClass() throws RemoteException {
        super();
        serverJSONHandler = new ServerJSONHandler();
        adminAccountsList = serverJSONHandler.readAdminAccountFile("src/main/resources/JSONFiles/AdminAccountsList.json");
        clientAccountsList = serverJSONHandler.readClientAccountFile("src/main/resources/JSONFiles/ClientAccountsList.json");
        clientCallbacks = new ConcurrentHashMap<>();
        adminCallbacks = new ConcurrentHashMap<>();
        callbackExecutor = Executors.newSingleThreadScheduledExecutor();
        conversationLocks = new ConcurrentHashMap<>();
        onlineUsers = new ConcurrentHashMap<>();
        onlineAdmins = new ConcurrentHashMap<>();
        fileLock = new ReentrantReadWriteLock();

        updateClientJSONFile("src/main/resources/JSONFiles/ClientAccountsList.json", clientAccountsList);
        updateAdminJSONFile("src/main/resources/JSONFiles/AdminAccountsList.json", adminAccountsList);
    }

    //USER/ADMIN LOG IN, SIGN UP, AND OTHER RELATED METHODS
    @Override
    public void signUp(String username, String password, String verificationCode, String jsonString, AdminCallbackInterface adminCallback) throws RemoteException {

    }

    @Override
    public void logIn(String username, String jsonString, AdminCallbackInterface adminCallback) throws RemoteException {

    }

    @Override
    public void signUp(String username, String password, String verificationCode, String jsonString, ClientCallbackInterface clientCallback) throws RemoteException {

    }

    @Override
    public void logIn(String username, String jsonString, ClientCallbackInterface clientCallback) throws RemoteException {

    }

    @Override
    public void logOut(String username, String jsonString) throws RemoteException{

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

                // TODO: Implement RMI Callback to notify the target user (targetUsername) immediately.
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
    public String getChatHistories(String jsonString) throws RemoteException{
        return "";
    }

    @Override
    public String getChatHistory(String jsonString) throws RemoteException{
        return "";
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
    public void sendPrivateMessage(String sender, String receiver, String message, String jsonString) throws RemoteException{
        /*
            To append a new message record to the JSON file:
            B.
                1. Create a Message object using the variables the client sent
                2. Get the Conversation object between the sender and the receiver
                3. Get the LIST of Message objects in the Conversation objects of the sender and receiver
                4. Append the newly created Message object to the list of Message objects of the sender and receiver
                5. Update the JSON file
                6. Append the changes to the JSON snippets of the sender and receiver
            C.
                1. Extract the JSON string for logType, messageType, and sender
                2. Create a new Requests object with the extracted information
                3. Get the LogsList object
                4. Get the list of Requests objects from the LogsList object
                5. Append the newly created Requests object to the LIST of Request objects
                6. Set the updated LIST of Requests objects to the LogsList object

        */
        Message msg = new Message(sender, receiver, message);
        Conversation receiverConvo = serverJSONHandler.getConversation(clientAccountsList, receiver, sender);
        Conversation senderConvo = serverJSONHandler.getConversation(clientAccountsList, sender, receiver);

        String conversationKey = serverJSONHandler.createConversationHashMapEntryString(sender, receiver);
        ReadWriteLock convoLock = conversationLocks.computeIfAbsent(conversationKey, k-> new ReentrantReadWriteLock());
        convoLock.writeLock().lock();

        try {
            receiverConvo.getMessages().add(msg);
            senderConvo.getMessages().add(msg);
            fileLock.writeLock().lock();
            try {
                serverJSONHandler.writeClientAccountFile("src/main/resources/JSONFiles/ClientAccountsList.json", clientAccountsList);
            } finally {
                fileLock.writeLock().unlock();
            }
        } finally {
            convoLock.writeLock().unlock();
        }

        ClientCallbackInterface senderCallback = clientCallbacks.get(sender);
        callbackExecutor.execute(() -> {
            try {
                senderCallback.onPrivateMessageSent(senderConvo.getMessages().getLast().toString(), serverJSONHandler.createJSONStringResponse("response", "SENT_MESSAGE", sender));
            } catch (RemoteException e) {
                System.out.println("Failed to notify sender");
                e.printStackTrace();
            }
        });

        ClientCallbackInterface receiverCallback = clientCallbacks.get(receiver);
        if (clientCallbacks.get(receiver) != null) {
            callbackExecutor.execute(() -> {
                try{
                    receiverCallback.onPrivateMessageReceived(receiverConvo.getMessages().getLast().toString(), serverJSONHandler.createJSONStringResponse("response", "RECEIVED_MESSAGE", receiver));
                } catch (RemoteException e) {
                    System.out.println("Failed to notify receiver");
                    e.printStackTrace();
                }
            });
        }
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
}