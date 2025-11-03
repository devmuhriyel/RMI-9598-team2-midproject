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
        // Admin signup not implemented
        System.out.println("Admin signup not implemented");
        adminCallback.onFailedSignUp();
    }

    @Override
    public void logIn(String username, String jsonString, AdminCallbackInterface adminCallback) throws RemoteException {
        System.out.println(">>> Admin login attempt for: " + username);

        try {
            // Parse JSON to extract password
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            String password = jsonObject.get("password").getAsString();

            System.out.println("  - Looking up admin: " + username);

            // Look up admin
            Admin admin = allAdmins.get(username);
            if (admin == null) {
                System.out.println("  ✗ Admin not found: " + username);
                adminCallback.onFailedLogin();
                return;
            }

            System.out.println("  ✓ Admin found: " + username);

            // Compare password
            if (!admin.getPassword().equals(password)) {
                System.out.println("  ✗ Incorrect password for admin: " + username);
                adminCallback.onFailedLogin();
                return;
            }

            System.out.println("  ✓ Password correct");

            // Register callback
            adminCallbacks.put(username, adminCallback);

            System.out.println("  ✓ Admin login successful: " + username);
            adminCallback.onSuccessfulLogin();

        } catch (Exception e) {
            System.err.println("  ✗ Error during admin login: " + e.getMessage());
            e.printStackTrace();
            adminCallback.onFailedLogin();
        }
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
            
            // Register the client's callback on the server
            clientCallbacks.put(username, clientCallback);
            System.out.println("✓ Callback registered for new user: " + username);
            
            System.out.println("Client signup successful: " + username);
            
            // Send the client's data via callback (notify client of successful signup)
            // The callback is now registered, so server can send data back to client
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
            // Parse JSON to extract username and password
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            String jsonUsername = jsonObject.get("username").getAsString();
            String password = jsonObject.get("password").getAsString();

            // Verify username from parameter matches username in JSON
            if (!username.equals(jsonUsername)) {
                System.out.println("Login failed: Username mismatch");
                clientCallback.onFailedLogin();
                return;
            }

            // Look up client
            Client client = allUsers.get(username);
            if (client == null) {
                System.out.println("Login failed: Client username not found - " + username);
                clientCallback.onFailedLogin();
                return;
            }

            // Compare password
            if (!client.getPassword().equals(password)) {
                System.out.println("Login failed: Incorrect password for " + username);
                clientCallback.onFailedLogin();
                return;
            }

            // Register callback for this user
            clientCallbacks.put(username, clientCallback);
            System.out.println("✓ Registered callback for user: " + username);

            // Serialize client data to JSON
            Gson gson = new Gson();
            String clientDataJson = gson.toJson(client);

            System.out.println("✓ Client login successful: " + username);
            System.out.println("  - Sending client data (" + clientDataJson.length() + " chars)");

            // Send success with client data
            clientCallback.onSuccessfulLogin(clientDataJson);

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
            // Note: JsonParser is deprecated, switching to new Gson().fromJson or JsonParser.parseString().getAsJsonObject()
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

            requesterUsername = jsonObject.get("requester").getAsString();
            targetUsername = jsonObject.get("responder").getAsString(); // FRIEND REQUEST JSON uses 'responder'

        } catch (Exception e) {
            System.err.println("Server Error: Failed to parse friend request JSON: " + jsonString);
            // TODO: Add callback to client for failed request
            throw new RemoteException("Invalid request format.", e);
        }

        // Validation check for self-request
        if (requesterUsername.equalsIgnoreCase(targetUsername)) {
            System.err.println("Friend request failed: Self-request is not allowed.");
            // TODO: Add callback to client for failed request
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
                System.out.println("Friend request saved: " + requesterUsername + " -> " + targetUsername);

                // RMI Callback to the sender
                ClientCallbackInterface senderCallback = clientCallbacks.get(requesterUsername);
                if(senderCallback != null) {
                    senderCallback.onFriendRequestSent();
                }

                // RMI Callback to the receiver
                ClientCallbackInterface receiverCallback = clientCallbacks.get(targetUsername);
                if(receiverCallback != null) {
                    receiverCallback.onFriendRequestReceived();
                }

            } else {
                System.err.println("Friend request failed (logic check failed, possibly already sent): " + requesterUsername + " -> " + targetUsername);
                // TODO: Add callback to client for failure
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

        // 1. Check if they are already friends
        if (requesterClient.getFriends() != null && requesterClient.getFriends().contains(target)) {
            System.err.println("Friend Request Logic Failed: Users are already friends.");
            return false;
        }

        // 2. Check if request is already pending (outgoing)
        if (requesterClient.getOutgoingFriendRequests() != null) {
            for (FriendRequest fr : requesterClient.getOutgoingFriendRequests()) {
                if (fr.getResponder().equalsIgnoreCase(target) && fr.getStatus().equalsIgnoreCase("pending")) {
                    System.err.println("Friend Request Logic Failed: Request already pending (outgoing).");
                    return false;
                }
            }
        }

        // 3. Check if target has already sent a request to requester (incoming) - Should be handled as an ACCEPT option in client UI
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

            System.out.println(">>> Username change request: " + currentUsername + " -> " + newUsername);

        } catch (Exception e) {
            System.err.println("Server Error: Failed to parse username change JSON: " + jsonString);
            throw new RemoteException("Invalid username change format.", e);
        }

        // Find client in allUsers map
        Client targetClient = allUsers.get(currentUsername);

        if (targetClient == null) {
            System.out.println("✗ Username change failed: User not found - " + currentUsername);
            notifyUsernameChangeFailed(currentUsername);
            return;
        }

        // Check if new username already exists
        if (allUsers.containsKey(newUsername)) {
            System.out.println("✗ Username change failed: Username already exists - " + newUsername);
            notifyUsernameExists(currentUsername);
            return;
        }

        fileLock.writeLock().lock();
        try {
            // Update username in client object
            targetClient.setUsername(newUsername);

            // Update all references in clientAccountsList
            updateUsernameReferences(currentUsername, newUsername);

            // Update allUsers map
            allUsers.remove(currentUsername);
            allUsers.put(newUsername, targetClient);

            // Update callback mapping
            ClientCallbackInterface callback = clientCallbacks.remove(currentUsername);
            if (callback != null) {
                clientCallbacks.put(newUsername, callback);
            }

            // Sync to JSON immediately for critical changes
            serverJSONHandler.writeClientAccountFile("src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList);

            System.out.println("✓ Username changed successfully: " + currentUsername + " -> " + newUsername);

            // Notify client of success
            notifyUsernameChangeSuccess(newUsername);

        } finally {
            fileLock.writeLock().unlock();
        }
    }

    private void updateUsernameReferences(String oldUsername, String newUsername) {
        System.out.println("  → Updating username references in all data...");

        for (Client client : clientAccountsList.getClients()) {
            // Update friends list
            if (client.getFriends() != null) {
                for (int i = 0; i < client.getFriends().size(); i++) {
                    if (client.getFriends().get(i).equals(oldUsername)) {
                        client.getFriends().set(i, newUsername);
                    }
                }
            }

            // Update outgoing friend requests
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

            // Update incoming friend requests
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

            // Update conversations
            if (client.getConversations() != null) {
                for (Conversation conv : client.getConversations()) {
                    if (conv.getRecipient() != null && conv.getRecipient().equals(oldUsername)) {
                        conv.setRecipient(newUsername);
                    }

                    // Update messages in conversation
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
        System.out.println("  ✓ All references updated");
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

            System.out.println(">>> Password change request for user: " + username);

        } catch (Exception e) {
            System.err.println("Server Error: Failed to parse password change JSON: " + jsonString);
            throw new RemoteException("Invalid password change format.", e);
        }

        // Find client
        Client targetClient = allUsers.get(username);

        if (targetClient == null) {
            System.out.println("✗ Password change failed: User not found - " + username);
            notifyPasswordChangeFailed(username);
            return;
        }

        // Verify old password
        if (!targetClient.getPassword().equals(oldPassword)) {
            System.out.println("✗ Password change failed: Invalid old password for - " + username);
            notifyInvalidOldPassword(username);
            return;
        }

        fileLock.writeLock().lock();
        try {
            // Update password
            targetClient.setPassword(newPassword);

            // Sync to JSON immediately
            serverJSONHandler.writeClientAccountFile("src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList);

            System.out.println("✓ Password changed successfully for user: " + username);

            // Notify client of success
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
            // Assuming the client sends the requester and the target (responder) in the JSON
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

            if (requesterClient == null || targetClient == null) {
                System.out.println("Cancel Failed: One or both users not found.");
                return;
            }

            // Remove from requester's outgoing list
            boolean removedOutgoing = requesterClient.getOutgoingFriendRequests().removeIf(
                    req -> req.getResponder().equals(target) && req.getStatus().equals("pending")
            );

            // Remove from target's incoming list
            boolean removedIncoming = targetClient.getIncomingFriendRequests().removeIf(
                    req -> req.getRequester().equals(requester) && req.getStatus().equals("pending")
            );

            if (removedOutgoing && removedIncoming) {
                serverJSONHandler.writeClientAccountFile(
                        "src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList // Corrected file path
                );
                System.out.println("Friend request cancelled: " + requester + " -> " + target);

                // RMI Callback to the client who cancelled
                ClientCallbackInterface senderCallback = clientCallbacks.get(requester);
                if(senderCallback != null) {
                    senderCallback.onCancelFriendRequest();
                }

            } else {
                System.out.println("Cancel Failed: Request was not found in pending state.");
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
                System.out.println("Accept Failed: One or both users not found.");
                return;
            }

            // 1. Remove pending requests
            responderClient.getIncomingFriendRequests().removeIf(
                    req -> req.getRequester().equals(requester) && req.getStatus().equals("pending")
            );
            requesterClient.getOutgoingFriendRequests().removeIf(
                    req -> req.getResponder().equals(responder) && req.getStatus().equals("pending")
            );

            // 2. Add to friends lists
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
            System.out.println("Friendship established: " + requester + " and " + responder);

            // 3. RMI Callback to the accepted friend (requester)
            ClientCallbackInterface requesterCallback = clientCallbacks.get(requester);
            if(requesterCallback != null) {
                requesterCallback.onFriendAccepted();
            }

            // 4. RMI Callback to the current user (responder)
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
                System.out.println("Reject Failed: One or both users not found.");
                return;
            }

            // Remove from responder's incoming list
            boolean removedIncoming = responderClient.getIncomingFriendRequests().removeIf(
                    req -> req.getRequester().equals(requester) && req.getStatus().equals("pending")
            );

            // Remove from requester's outgoing list
            boolean removedOutgoing = requesterClient.getOutgoingFriendRequests().removeIf(
                    req -> req.getResponder().equals(responder) && req.getStatus().equals("pending")
            );

            if (removedIncoming && removedOutgoing) {
                serverJSONHandler.writeClientAccountFile(
                        "src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList // Corrected file path
                );
                System.out.println("Friend request rejected: " + requester + " -> " + responder);

                // RMI Callback to the client who rejected (responder)
                ClientCallbackInterface responderCallback = clientCallbacks.get(responder);
                if(responderCallback != null) {
                    responderCallback.onFriendDeclined();
                }

                // RMI Callback to the requester (the person whose request was rejected)
                ClientCallbackInterface requesterCallback = clientCallbacks.get(requester);
                if(requesterCallback != null) {
                    requesterCallback.onFriendDeclined(); // Or a more specific callback like onFriendRejected
                }

            } else {
                System.out.println("Reject Failed: Request was not found in pending state.");
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
                System.out.println("Unfriend Failed: One or both users not found.");
                return;
            }
            unfrienderClient.getFriends().remove(friendToRemove);
            friendToRemoveClient.getFriends().remove(unfriender);
            serverJSONHandler.writeClientAccountFile(
                    "src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList
            );
            System.out.println("Friendship removed: " + unfriender + " unfriended " + friendToRemove);
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
            System.out.println("Warning: Client or callback not found for " + username);
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
                System.out.println("Sent " + sentRequests.size() + " outgoing requests to " + username);
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
            System.out.println("Warning: Client or callback not found for " + username);
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
                System.out.println("✓ Sent " + receivedRequests.size() + " incoming requests to " + username);
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
            System.out.println("Warning: Client or callback not found for " + username);
            return;
        }
        java.util.List<String> friends = client.getFriends() != null ? client.getFriends() : new java.util.ArrayList<>();
        callbackExecutor.execute(() -> {
            try {
                callback.onGetFriendsList(friends);
                System.out.println("✓ Sent " + friends.size() + " friends to " + username);
            } catch (RemoteException e) {
                System.err.println("Failed to send friends list to " + username + ": " + e.getMessage());
            }
        });
    }

    //USER/ADMIN COMMON CHATTING METHODS
    @Override
    public List<GroupChat> getGroupChatHistories(String jsonString, String username) throws RemoteException {
        System.out.println("getGroupChatHistories called for: " + username);

        // Validation
        if (username == null || username.trim().isEmpty()) {
            throw new RemoteException("Username cannot be null or empty");
        }

        Client client = allUsers.get(username);
        if (client == null) {
            throw new RemoteException("User not found: " + username);
        }

        // Initialize group chats if null
        if (client.getGroupChats() == null) {
            client.setGroupChats(new ArrayList<>());
        }

        // Get callback
        ClientCallbackInterface callback = clientCallbacks.get(username);
        if (callback == null) {
            System.out.println("Warning: No callback registered for user: " + username);
            return client.getGroupChats();
        }

        // Notify client with group chat histories
        List<GroupChat> groupChats = client.getGroupChats();
        callbackExecutor.execute(() -> {
            try {
                callback.onGetGroupChatHistories(groupChats);
                System.out.println("✓ Group chat histories sent to " + username + ": " + groupChats.size() + " groups");
            } catch (RemoteException e) {
                System.err.println("Failed to send group chat histories to " + username + ": " + e.getMessage());
                e.printStackTrace();
            }
        });

        return groupChats;
    }

    @Override
    public void getChatHistories(String jsonString, String sender) throws RemoteException {
        System.out.println("getChatHistories called for: " + sender);

        // Validation
        if (sender == null || sender.trim().isEmpty()) {
            throw new RemoteException("Sender cannot be null or empty");
        }

        Client client = allUsers.get(sender);
        if (client == null) {
            throw new RemoteException("User not found: " + sender);
        }

        // Initialize conversations if null
        if (client.getConversations() == null) {
            client.setConversations(new ArrayList<>());
        }

        // Get callback
        ClientCallbackInterface callback = clientCallbacks.get(sender);
        if (callback == null) {
            System.out.println("Warning: No callback registered for user: " + sender);
            return;
        }

        // Notify client with chat histories
        List<Conversation> conversations = client.getConversations();
        callbackExecutor.execute(() -> {
            try {
                callback.onGetChatHistories(conversations);
                System.out.println("✓ Chat histories sent to " + sender + ": " + conversations.size() + " conversations");
            } catch (RemoteException e) {
                System.err.println("Failed to send chat histories to " + sender + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public void getChatHistoryWith(String jsonString, String sender, String receiver) throws RemoteException {
        System.out.println("getChatHistoryWith called: " + sender + " with " + receiver);

        // Validation
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

        // Initialize conversations if null
        if (client.getConversations() == null) {
            client.setConversations(new ArrayList<>());
        }

        // Find conversation with specific receiver
        List<Conversation> conversations = client.getConversations();
        Conversation conversationWith = null;

        for (Conversation conversation : conversations) {
            if (conversation.getRecipient() != null && conversation.getRecipient().equals(receiver)) {
                conversationWith = conversation;
                break;
            }
        }

        // If no conversation exists, create an empty one
        if (conversationWith == null) {
            System.out.println("No existing conversation found between " + sender + " and " + receiver);
            conversationWith = new Conversation(receiver, new ArrayList<>());
        }

        // Get callback
        ClientCallbackInterface callback = clientCallbacks.get(sender);
        if (callback == null) {
            System.out.println("Warning: No callback registered for user: " + sender);
            return;
        }

        // Notify client with specific chat history
        Conversation finalConversationWith = conversationWith;
        callbackExecutor.execute(() -> {
            try {
                callback.onGetChatHistoryWith(finalConversationWith);
                int messageCount = finalConversationWith.getMessages() != null ? finalConversationWith.getMessages().size() : 0;
                System.out.println("✓ Chat history sent to " + sender + " for conversation with " + receiver + ": " + messageCount + " messages");
            } catch (RemoteException e) {
                System.err.println("Failed to send chat history to " + sender + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public GroupChat getGroupChatHistoryWith(String jsonString, String username, String groupID) throws RemoteException {
        System.out.println("getGroupChatHistoryWith called: " + username + " for group " + groupID);

        // Validation
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

        // Initialize group chats if null
        if (client.getGroupChats() == null) {
            client.setGroupChats(new ArrayList<>());
        }

        // Find group chat with specific groupID
        List<GroupChat> groupChats = client.getGroupChats();
        GroupChat groupChatWith = null;

        for (GroupChat groupChat : groupChats) {
            if (groupChat.getGroupID() != null && groupChat.getGroupID().equals(groupID)) {
                groupChatWith = groupChat;
                break;
            }
        }

        // If no group chat exists
        if (groupChatWith == null) {
            System.out.println("No group chat found for " + username + " with groupID: " + groupID);
            throw new RemoteException("Group chat not found: " + groupID);
        }

        // Get callback
        ClientCallbackInterface callback = clientCallbacks.get(username);
        if (callback == null) {
            System.out.println("Warning: No callback registered for user: " + username);
            return groupChatWith;
        }

        // Notify client with specific group chat history
        GroupChat finalGroupChatWith = groupChatWith;
        callbackExecutor.execute(() -> {
            try {
                callback.onGetGroupChatHistoryWith(finalGroupChatWith);
                int messageCount = finalGroupChatWith.getMessages() != null ? finalGroupChatWith.getMessages().size() : 0;
                System.out.println("✓ Group chat history sent to " + username + " for group " + groupID + ": " + messageCount + " messages");
            } catch (RemoteException e) {
                System.err.println("Failed to send group chat history to " + username + ": " + e.getMessage());
                e.printStackTrace();
            }
        });

        return groupChatWith;
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
    public void sendPrivateFile(String sender, String receiver, String fileName, String fileData, String jsonString) throws RemoteException {
        System.out.println("sendPrivateFile called: " + sender + " -> " + receiver + ": " + fileName);

        // Validation
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

        // Create file message
        Message fileMsg = new Message(sender, receiver, fileName, fileData, true);

        // Initialize conversations if needed
        if (senderClient.getConversations() == null) {
            senderClient.setConversations(new ArrayList<>());
        }
        if (receiverClient.getConversations() == null) {
            receiverClient.setConversations(new ArrayList<>());
        }

        // Find or create conversations
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
                System.out.println("File message saved to file");
            } finally {
                fileLock.writeLock().unlock();
            }
        } finally {
            convoLock.writeLock().unlock();
        }

        // Serialize message to JSON
        Gson gson = new Gson();
        String messageJson = gson.toJson(fileMsg);

        // Notify sender
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
                    System.out.println("Failed to notify receiver: " + e.getMessage());
                }
            });
        }

        System.out.println("File message processing completed successfully");
    }

    //USER/ADMIN GROUP CHATTING METHODS
    @Override
    public void sendGroupMessage(String sender, String groupID, String message, String jsonString) throws RemoteException {
        System.out.println("sendGroupMessage called: " + sender + " -> group " + groupID + ": " + message);

        // Validation
        if (sender == null || groupID == null || message == null) {
            throw new RemoteException("Sender, groupID, or message cannot be null");
        }

        Client senderClient = allUsers.get(sender);
        if (senderClient == null) {
            throw new RemoteException("Sender user not found: " + sender);
        }

        // Find the group in sender's groupChats
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

        // Check if sender is a member
        if (!senderGroup.isMember(sender)) {
            throw new RemoteException("User " + sender + " is not a member of group " + groupID);
        }

        // Get list of receivers
        List<String> receivers = new ArrayList<>(senderGroup.getReceivers());

        // Create group message
        Message msg = new Message(sender, receivers, message);

        fileLock.writeLock().lock();
        try {
            // Add message to all members' group chats
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

            // Save to file
            serverJSONHandler.writeClientAccountFile("src/main/resources/JSONFiles/ClientAccounts.json", clientAccountsList);
            System.out.println("Group message saved to file");

        } finally {
            fileLock.writeLock().unlock();
        }

        // Serialize message to JSON
        Gson gson = new Gson();
        String messageJson = gson.toJson(msg);

        // Notify all online members
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

        System.out.println("Group message processing completed successfully");
    }

    @Override
    public void sendGroupFile(String sender, String groupID, String fileName, String fileData, String jsonString) throws RemoteException {
        System.out.println("sendGroupFile called: " + sender + " -> group " + groupID + ": " + fileName);

        // Validation
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

        // Create group file message
        Message fileMsg = new Message(sender, receivers, fileName, fileData, true);

        fileLock.writeLock().lock();
        try {
            // Add file message to all members' group chats
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
            System.out.println("Group file saved to file");

        } finally {
            fileLock.writeLock().unlock();
        }

        // Serialize message to JSON
        Gson gson = new Gson();
        String messageJson = gson.toJson(fileMsg);

        // Notify all online members
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

        System.out.println("Group file processing completed successfully");
    }

    @Override
    public void createGroupChat(String jsonString) throws RemoteException{
        String creator;
        String groupName;
        java.util.List<String> members = new java.util.ArrayList<>();

        try {
            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
            creator = json.get("creator").getAsString();
            groupName = json.get("groupName").getAsString();

            if (json.has("members") && json.get("members").isJsonArray()) {
                for (var el : json.get("members").getAsJsonArray()) {
                    String m = el.getAsString().trim();
                    if (!m.isEmpty() && !members.contains(m)) {
                        members.add(m);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Server Error: Failed to parse createGroupChat JSON: " + jsonString);
            throw new RemoteException("Invalid create group format.", e);
        }

        // Ensure creator is included
        if (!members.contains(creator)) {
            members.add(creator);
        }

        String groupID = "grp-" + System.currentTimeMillis();

        fileLock.writeLock().lock();
        try {
            // Create GroupChat object for each member
            for (String member : members) {
                Client client = findClient(member);
                if (client == null) {
                    System.out.println("Warning: Skipping unknown member in group creation: " + member);
                    continue;
                }
                if (client.getGroupChats() == null) {
                    client.setGroupChats(new ArrayList<>());
                }
                // Build receivers list (all members)
                java.util.List<String> receivers = new java.util.ArrayList<>(members);
                GroupChat gc = new GroupChat(groupID, groupName, receivers, new ArrayList<>());
                client.getGroupChats().add(gc);
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

        // Notify online members
        for (String member : members) {
            ClientCallbackInterface cb = clientCallbacks.get(member);
            if (cb != null) {
                callbackExecutor.execute(() -> {
                    try {
                        cb.onSuccessfulGroupCreation();
                    } catch (RemoteException e) {
                        System.err.println("Failed to notify group creation to " + member + ": " + e.getMessage());
                    }
                });
            }
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