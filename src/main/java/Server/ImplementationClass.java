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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ImplementationClass extends UnicastRemoteObject implements ClientInterface, AdminInterface {
    //VARIABLES
    private ConcurrentHashMap<String, Client> onlineUsers;
    private ConcurrentHashMap<String, Client> allUsers;
    private ConcurrentHashMap<String, Admin> onlineAdmins;
    private ConcurrentHashMap<String, Admin> allAdmins;
    private AdminAccountsList adminAccountsList;
    private ClientAccountsList clientAccountsList;
    private ServerJSONHandler serverJSONHandler;

    private ConcurrentHashMap<String, ClientCallbackInterface> clientCallbacks;
    private ConcurrentHashMap<String, AdminCallbackInterface> adminCallbacks;
    private ExecutorService callbackExecutor;
    private ReadWriteLock conversationLock;
    private ReadWriteLock userAccountsLock;

    //CONSTRUCTORS
    public ImplementationClass() throws RemoteException {
        super();
        serverJSONHandler = new ServerJSONHandler();
        adminAccountsList = serverJSONHandler.readAdminAccountFile("src/main/resources/JSONFiles/AdminAccountsList.json");
        clientAccountsList = serverJSONHandler.readClientAccountFile("src/main/resources/JSONFiles/ClientAccountsList.json");
        clientCallbacks = new ConcurrentHashMap<>();
        adminCallbacks = new ConcurrentHashMap<>();
        callbackExecutor = Executors.newFixedThreadPool(50);
        conversationLock = new ReentrantReadWriteLock();
        userAccountsLock = new ReentrantReadWriteLock();
        onlineUsers = new ConcurrentHashMap<>();
        onlineAdmins = new ConcurrentHashMap<>();
    }

    //USER/ADMIN LOG IN, SIGN UP, AND OTHER RELATED METHODS
    @Override
    public void signUp(String jsonString) throws RemoteException{

    }

    @Override
    public void logIn(String jsonString) throws RemoteException{

    }

    @Override
    public void logOut(String jsonString) throws RemoteException{

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
        userAccountsLock.writeLock().lock();
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
            userAccountsLock.writeLock().unlock();
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

        userAccountsLock.writeLock().lock();
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
            userAccountsLock.writeLock().unlock();
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

        userAccountsLock.writeLock().lock();
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
            userAccountsLock.writeLock().unlock();
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
            A.
                1. Create a Message object using the variables the client sent
                2. Get the Conversation object between the sender and the receiver
                3. Get the LIST of Message objects in the Conversation objects of the sender and receiver
                4. Append the newly created Message object to the list of Message objects of the sender and receiver
                5. Set the updated LIST of Message objects in the Conversation object of the sender and receiver
                6. Append the changes to the JSON snippets of the sender and receiver
            B.
                7. Extract the JSON string for logType, messageType, and sender
                8. Create a new Requests object with the extracted information
                9. Get the LogsList object
                10. Get the list of Requests objects from the LogsList object
                11. Append the newly created Requests object to the LIST of Request objects
                12. Set the updated LIST of Requests objects to the LogsList object

        */
        conversationLock.writeLock().lock();
        try {
            Message msg = new Message(sender, receiver, message);
            Conversation receiverConvo = serverJSONHandler.getConversation(clientAccountsList, receiver, sender);
            Conversation senderConvo = serverJSONHandler.getConversation(clientAccountsList, sender, receiver);
            receiverConvo.getMessages().add(msg);
            senderConvo.getMessages().add(msg);
//            serverJSONHandler.updateUserConversationJSON(clientAccountsList, sender, receiver, senderHistory);
//            serverJSONHandler.updateUserConversationJSON(clientAccountsList, receiver, sender, receiverHistory);
        } finally {
            conversationLock.writeLock().unlock();
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
}