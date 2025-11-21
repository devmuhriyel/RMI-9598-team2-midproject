package ClientMVC;

import AdminMVC.AdminControllers.AdminConnectionController;
import AdminMVC.AdminControllers.AdminController;
import ClientMVC.ClientController.*;
import AdminMVC.AdminControllers.AdminLoginController;
import Common.Interfaces.ClientCallbackInterface;
import Common.Objects.ClientObjects.Client;
import Common.Objects.ClientObjects.Conversation;
import Common.Objects.ClientObjects.GroupChat;
import Common.Objects.ClientObjects.Message;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class ClientCallbackImplementation extends UnicastRemoteObject implements ClientCallbackInterface {
    private String username;
    private Gson gson;
    private Client clientData; // ADDED: Store the full client data

    //Admin Controllers
    private AdminLoginController adminLoginController;
    private AdminController adminController;
    private AdminConnectionController adminConnectionController;

    //Client Controllers
    private LoginController clientLoginController;
    private SignupController clientSignupController;
    private AccountController clientAccountController;
    private ChatController clientChatController;
    private ConnectionController clientConnectionController;
    private HomeController clientHomeController;

    // Callback handlers for account operations
    private Runnable onUsernameChangeSuccess;
    private Runnable onUsernameChangeFailed;
    private Runnable onPasswordChangeSuccess;
    private Runnable onPasswordChangeFailed;

    public ClientCallbackImplementation() throws RemoteException {
        super();
        this.gson = new Gson();
    }

    @Override
    public void onSuccessfulSignUp() throws RemoteException {
        if (clientSignupController != null) {
            clientSignupController.onSuccessfulSignUpFromRemote();
        }
    }

    @Override
    public void onFailedSignUp() throws RemoteException {
        if (clientSignupController != null) {
            clientSignupController.onFailedSignUpFromRemote();
        }
    }

    @Override
    public void forceLogout(String reason) throws RemoteException {
        Platform.runLater(() -> {

            if (clientHomeController != null) {
                clientHomeController.onForceLogout(reason);
                return;
            }

            if (clientLoginController != null) {
                clientLoginController.onForceLogout(reason);
                return;
            }

        });
    }

    @Override
    public void onSuccessfulLogin(String clientDataJson) throws RemoteException {
        try {
            this.clientData = gson.fromJson(clientDataJson, Client.class);
            this.username = clientData.getUsername();
            ClientJSONHandler.setCurrentUser(username);
        } catch (Exception e) {
            System.err.println("Error parsing client data: " + e.getMessage());
            e.printStackTrace();
        }

        if(clientLoginController != null){
            clientLoginController.onSuccessfulLoginFromRemote();
        }
    }

    @Override
    public void onFailedLogin() throws RemoteException {
        if(clientLoginController != null){
            clientLoginController.onFailedLoginFromRemote();
        }
    }

    @Override
    public void onSuccessfulLogout() throws RemoteException {
        // Clear client data on logout
        this.clientData = null;
        this.username = null;
        ClientJSONHandler.setCurrentUser(null);
    }

    @Override
    public void onSuccessfulPasswordChange() throws RemoteException {

    }

    @Override
    public void onFailedPasswordChange() throws RemoteException {

    }

    @Override
    public void onSuccessfulCodeVerification() throws RemoteException {

    }

    @Override
    public void onFailedCodeVerification() throws RemoteException {

    }

    @Override
    public void onSearchUserResults() throws RemoteException {
    }

    @Override
    public void onSearchGroupResults() throws RemoteException {
    }

    @Override
    public void onFriendRequestSent() throws RemoteException {
        if (clientHomeController != null) {
            clientHomeController.handleFriendRequestSent();
        }
    }

    @Override
    public void onGetOnlineFriends() throws RemoteException {

    }

    @Override
    public void onSuccessfulUsernameChange() throws RemoteException {
        if (this.clientData != null && clientAccountController != null) {
            String newUsername = clientAccountController.getNewUsernameFromView();
            this.clientData.setUsername(newUsername);
            this.username = newUsername;
            ClientJSONHandler.setCurrentUser(newUsername);
        }

        if (onUsernameChangeSuccess != null) {
            onUsernameChangeSuccess.run();
        }
    }

    @Override
    public void onFailedUsernameChange() throws RemoteException {
        System.out.println("✗ Callback received: Username change failed");
        if (onUsernameChangeFailed != null) {
            onUsernameChangeFailed.run();
        } else {
            System.err.println("⚠ Warning: onUsernameChangeFailed handler is null");
        }
    }

    @Override
    public void onSuccessfulPasswordChangeWithOldPassword() throws RemoteException {
        System.out.println("✓ Callback received: Password change successful");
        if (onPasswordChangeSuccess != null) {
            onPasswordChangeSuccess.run();
        } else {
            System.err.println("⚠ Warning: onPasswordChangeSuccess handler is null");
        }
    }

    @Override
    public void onFailedPasswordChangeWithOldPassword() throws RemoteException {
        System.out.println("✗ Callback received: Password change failed (invalid old password)");
        if (onPasswordChangeFailed != null) {
            onPasswordChangeFailed.run();
        } else {
            System.err.println("⚠ Warning: onPasswordChangeFailed handler is null");
        }
    }

    @Override
    public void onFriendRequestReceived() throws RemoteException {
        System.out.println("Callback: New friend request received!");
        if (clientHomeController != null) {
        }
        if (clientAccountController != null) {
            Platform.runLater(() -> {
                clientAccountController.handleNewFriendRequestReceived();
            });
        }
    }

    @Override
    public void onFriendAccepted() throws RemoteException {
        System.out.println("Callback: Friend request accepted!");
        if (clientAccountController != null) {
            Platform.runLater(() -> {
                clientAccountController.handleFriendAcceptedSuccess();
            });
        }
        if (clientHomeController != null) {
            clientHomeController.refreshUserData();
        }

    }

    @Override
    public void onFriendDeclined() throws RemoteException {
        System.out.println("Callback: Friend request declined!");
        if (clientAccountController != null) {
            Platform.runLater(() -> {
                clientAccountController.handleFriendDeclinedNotification();
            });
        }
    }

    @Override
    public void onFriendDeleted() throws RemoteException {
        System.out.println("Callback: Friend removed from list!");
        if (clientAccountController != null) {
            Platform.runLater(() -> {
                clientAccountController.handleFriendDeletionSuccess();
            });
        }
        if (clientHomeController != null) {
            clientHomeController.refreshUserData();
        }
    }

    @Override
    public void onCancelFriendRequest() throws RemoteException {
        System.out.println("Callback: Friend request cancelled!");
        if (clientAccountController != null) {
            javafx.application.Platform.runLater(() -> {
                clientAccountController.showSuccessAlert("Request Canceled", "Your friend request has been successfully canceled.");
            });
        }
    }

    @Override
    public void onGetFriendsList(List<String> friends) throws RemoteException {
        javafx.application.Platform.runLater(() -> {
            if (clientHomeController != null) {
                clientHomeController.updateFriendsList(friends);
            }
            if (clientAccountController != null) {
                clientAccountController.handleFriendsListUpdate(friends);
            }
        });

    }

    @Override
    public void onFriendRemoved() throws RemoteException {

    }

    @Override
    public void onFriendDeletedByOther() throws RemoteException {
        System.out.println("Callback: Friend removed from list!");
        if (clientAccountController != null) {
            Platform.runLater(() -> {
                clientAccountController.handleFriendDeletionSuccess();
            });
        }
        if (clientHomeController != null) {
            try {
                clientHomeController.refreshUserData();
            } catch (Exception e) {
                System.err.println("Failed to refresh home after unfriend: " + e.getMessage());
            }
        }
    }

    @Override
    public void onGetChatHistories(List<Conversation> allChats) throws RemoteException {
        if (clientChatController != null) {
            clientChatController.onGetChatHistories(allChats);
        }
    }

    @Override
    public void onGetSentFriendRequests(List<String> sentRequests) throws RemoteException {
        if (clientAccountController != null) {
            clientAccountController.handleSentFriendRequests(sentRequests);
        }
    }

    @Override
    public void onGetReceivedFriendRequests(List<String> receivedRequests) throws RemoteException {
        if (clientAccountController != null) {
            clientAccountController.handleReceivedFriendRequests(receivedRequests);
        }
    }
    @Override
    public void onGetChatHistoryWith(Conversation chatWith) throws RemoteException {
        if (clientChatController != null) {
            clientChatController.onGetChatHistoryWith(chatWith);
        }
    }

    @Override
    public void onGetGroupChatHistories(List<GroupChat> allGroupChats) throws RemoteException {
        // Update client data with the latest group chat information
        if (clientData != null) {
            clientData.setGroupChats(allGroupChats);
        }
        
        // Notify chat controller if needed
        if (clientChatController != null) {
            clientChatController.onGetGroupChatHistories(allGroupChats);
        }
        
        // Refresh home UI to show updated group members
        if (clientHomeController != null) {
            clientHomeController.updateGroupsUI(allGroupChats);
        }
    }

    @Override
    public void onGetGroupChatHistoryWith(GroupChat groupChatWith) throws RemoteException {
        if (clientChatController != null) {
            clientChatController.onGetGroupChatHistoryWith(groupChatWith);
        }
    }

    @Override
    public void onGetUnreadMessages() throws RemoteException {}

    @Override
    public void onPrivateMessageSent(String message, String jsonString) throws RemoteException {
        if (clientChatController != null) {
            Message msg = messageStringToObject(message);
            updateLocalConversation(msg);
            clientChatController.onPrivateMessageSent(msg);
        }
    }

    @Override
    public void onPrivateMessageReceived(String message, String jsonString) throws RemoteException {
        if (clientChatController != null) {
            Message msg = messageStringToObject(message);
            updateLocalConversation(msg);
            clientChatController.onPrivateMessageReceived(msg);
        }
    }

    @Override
    public void onPrivateFileSent(String message, String jsonString) throws RemoteException {
        if (clientChatController != null) {
            Message msg = messageStringToObject(message);
            updateLocalConversation(msg);
            clientChatController.onPrivateFileSent(msg);
        }
    }

    @Override
    public void onPrivateFileReceived(String message, String jsonString) throws RemoteException {
        if (clientChatController != null) {
            Message msg = messageStringToObject(message);
            updateLocalConversation(msg);
            clientChatController.onPrivateFileReceived(msg);
        }
    }

    @Override
    public void onGroupMessageSent(String message, String jsonString) throws RemoteException {
        if (clientChatController != null) {
            Message msg = messageStringToObject(message);
            updateLocalGroupChat(msg);
            clientChatController.onGroupMessageSent(msg);
        }
    }

    @Override
    public void onGroupFileSent(String message, String jsonString) throws RemoteException {
        if (clientChatController != null) {
            Message msg = messageStringToObject(message);
            updateLocalGroupChat(msg);
            clientChatController.onGroupFileSent(msg);
        }
    }

    @Override
    public void onGroupMessageReceived(String message, String jsonString) throws RemoteException {
        if (clientChatController != null) {
            Message msg = messageStringToObject(message);
            updateLocalGroupChat(msg);
            clientChatController.onGroupMessageReceived(msg);
        }
    }

    @Override
    public void onGroupFileReceived(String message, String jsonString) throws RemoteException {
        if (clientChatController != null) {
            Message msg = messageStringToObject(message);
            updateLocalGroupChat(msg);
            clientChatController.onGroupFileReceived(msg);
        }
    }


    @Override
    public void onSuccessfulGroupCreation() throws RemoteException {
        if (clientHomeController != null) {
            clientHomeController.refreshUserData();
        }
    }

    @Override
    public void onFailedGroupCreation() throws RemoteException {
        Platform.runLater(() -> {
            try {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Group Name Unavailable");
                alert.setHeaderText(null);
                alert.setContentText("That group name is already taken by another user (case-sensitive). Please choose a different name.");
                alert.showAndWait();
            } catch (Exception ignored) {}
        });
    }

    @Override
    public void onSuccessfulMemberAdded() throws RemoteException {
        System.out.println("Callback: Member added successfully. Refreshing home data.");
        if (clientHomeController != null) {
            javafx.application.Platform.runLater(() -> {
                try {
                    // Refresh will be triggered by onGetGroupChatHistories callback from server
                    // But also trigger immediate refresh to ensure UI updates
                    clientHomeController.refreshUserData();
                } catch (Exception ignored) {}
            });
        }
    }

    @Override
    public void onFailedMemberAdded() throws RemoteException {
        System.out.println("Callback: Failed to add member.");
        javafx.application.Platform.runLater(() -> {
            try {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Add Member Failed");
                alert.setHeaderText(null);
                alert.setContentText("Could not add the selected user to the group. Please check permissions and try again.");
                alert.showAndWait();
            } catch (Exception ignored) {}
        });
    }

    @Override
    public void onSuccessfulMemberRemoved() throws RemoteException {
        System.out.println("Callback: Member removed successfully. Refreshing home data.");
        if (clientHomeController != null) {
            javafx.application.Platform.runLater(() -> {
                try {
                    // Refresh will be triggered by onGetGroupChatHistories callback from server
                    // But also trigger immediate refresh to ensure UI updates
                    clientHomeController.refreshUserData();
                } catch (Exception ignored) {}
            });
        }
    }

    @Override
    public void onFailedMemberRemoved() throws RemoteException {
        System.out.println("Callback: Failed to remove member.");
        javafx.application.Platform.runLater(() -> {
            try {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Remove Member Failed");
                alert.setHeaderText(null);
                alert.setContentText("Could not remove the selected user from the group. Please try again.");
                alert.showAndWait();
            } catch (Exception ignored) {}
        });
    }

    @Override
    public void onCommunityMessageSent(String message, String jsonString) throws RemoteException {
        if (clientChatController != null) {
            Message msg = messageStringToObject(message);
            javafx.application.Platform.runLater(() -> {
                try {
                    clientChatController.onCommunityMessageSent(msg);
                } catch (RemoteException e) {
                    System.err.println("Error handling community message sent: " + e.getMessage());
                }
            });
        }
    }

    @Override
    public void onCommunityMessageReceived(String message, String jsonString) throws RemoteException {
        if (clientChatController != null) {
            Message msg = messageStringToObject(message);
            clientChatController.onCommunityMessageReceived(msg);
        }
    }

    // GETTERS
    public String getUsername(){
        return username;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public Client getClientData() {
        return clientData;
    }

    //ADMIN CONTROLLER SETUP
    public void setAdminConnectionController(AdminConnectionController adminConnectionController){
        this.adminConnectionController = adminConnectionController;
    }
    public void unsetAdminConnectionController(){
        this.adminConnectionController = null;
    }
    public void setAdminLoginController(AdminLoginController adminLoginController){
        this.adminLoginController = adminLoginController;
    }
    public void unsetAdminLoginController(){
        this.adminLoginController = null;
    }
    public void setAdminController(AdminController adminController){
        this.adminController = adminController;
    }
    public void unsetAdminController(){
        this.adminController = null;
    }

    //CLIENT CONTROLLER SETUP
    public void setClientConnectionController(ConnectionController clientConnectionController){
        this.clientConnectionController = clientConnectionController;
    }
    public void unsetClientConnectionController(){
        this.clientConnectionController = null;
    }
    public void setClientLoginController(LoginController clientLoginController){
        this.clientLoginController = clientLoginController;
    }
    public void unsetClientLoginController(){
        this.clientLoginController = null;
    }
    public void setClientSignupController(SignupController clientSignupController){
        this.clientSignupController = clientSignupController;
    }
    public void unsetClientSignupController(){
        this.clientSignupController = null;
    }
    public void setClientAccountController(AccountController clientAccountController){
        this.clientAccountController = clientAccountController;
    }
    public void unsetClientAccountController(){
        this.clientAccountController = null;
    }
    public void setClientHomeController(HomeController clientHomeController){
        this.clientHomeController = clientHomeController;
    }
    public void unsetClientHomeController(){
        this.clientHomeController = null;
    }
    public void setClientChatController(ChatController clientChatController){
        this.clientChatController = clientChatController;
    }
    public void unsetClientChatController(){
        this.clientChatController = null;
    }

    //HELPER METHODS
    public Message messageStringToObject(String message) throws RemoteException {
        try {
            return gson.fromJson(message, Message.class);
        } catch (Exception e) {
            System.err.println("Error parsing message: " + e.getMessage());
            throw new RemoteException("Failed to parse message", e);
        }
    }

    private void updateClientConversation(String otherUser, Message message) {
        if (clientData == null || clientData.getConversations() == null) {
            return;
        }

        // Find the conversation
        for (Conversation conv : clientData.getConversations()) {
            if (conv.getRecipient() != null && conv.getRecipient().equals(otherUser)) {
                if (conv.getMessages() == null) {
                    conv.setMessages(new java.util.ArrayList<>());
                }
                // Add message to local data
                conv.getMessages().add(message);
                return;
            }
        }

        // If conversation doesn't exist, create it
        Conversation newConv = new Conversation(otherUser, new java.util.ArrayList<>());
        newConv.getMessages().add(message);
        clientData.getConversations().add(newConv);
    }

    private void updateClientGroupChat(Message message) {
        if (clientData == null || clientData.getGroupChats() == null ||
                message.getReceivers() == null || message.getReceivers().isEmpty()) {
            return;
        }

        // Find the group by matching receivers
        for (GroupChat group : clientData.getGroupChats()) {
            if (group.getReceivers() != null &&
                    group.getReceivers().containsAll(message.getReceivers()) &&
                    message.getReceivers().containsAll(group.getReceivers())) {

                if (group.getMessages() == null) {
                    group.setMessages(new java.util.ArrayList<>());
                }
                // Add message to local data
                group.getMessages().add(message);
                return;
            }
        }
    }

    private void updateLocalConversation(Message msg) {
        if (clientData == null || msg == null) {
            return;
        }

        try {
            // Determine the conversation partner
            String partner = msg.getSender().equals(username) ? msg.getReceiver() : msg.getSender();

            if (partner == null || partner.isEmpty()) {
                System.err.println("⚠ Warning: Cannot determine conversation partner");
                return;
            }

            // Initialize conversations list if needed
            if (clientData.getConversations() == null) {
                clientData.setConversations(new java.util.ArrayList<>());
            }

            // Find or create conversation
            Conversation targetConversation = null;
            for (Conversation conv : clientData.getConversations()) {
                if (conv.getRecipient() != null && conv.getRecipient().equals(partner)) {
                    targetConversation = conv;
                    break;
                }
            }

            // Create new conversation if not found
            if (targetConversation == null) {
                targetConversation = new Conversation(partner, new java.util.ArrayList<>());
                clientData.getConversations().add(targetConversation);
                System.out.println("✓ Created new conversation with: " + partner);
            }

            // Initialize messages list if needed
            if (targetConversation.getMessages() == null) {
                targetConversation.setMessages(new java.util.ArrayList<>());
            }

            // Add message to local data
            targetConversation.getMessages().add(msg);

            if (msg.isFile()) {
                System.out.println("✓ Added file to local data: " + msg.getFileName());
            } else {
                System.out.println("✓ Added message to local data from: " + partner);
            }

        } catch (Exception e) {
            System.err.println("✗ Failed to update local conversation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateLocalGroupChat(Message msg) {
        if (clientData == null || msg == null) {
            return;
        }

        try {
            // Get group ID from message receivers
            if (msg.getReceivers() == null || msg.getReceivers().isEmpty()) {
                System.err.println("⚠ Warning: Message has no receivers (not a group message?)");
                return;
            }

            // Initialize group chats list if needed
            if (clientData.getGroupChats() == null) {
                clientData.setGroupChats(new java.util.ArrayList<>());
            }

            // Find the group chat by checking if receivers match
            // This is tricky because we need to find which group this message belongs to
            Common.Objects.ClientObjects.GroupChat targetGroup = null;

            for (Common.Objects.ClientObjects.GroupChat group : clientData.getGroupChats()) {
                if (group.getReceivers() != null &&
                        group.getReceivers().containsAll(msg.getReceivers()) &&
                        msg.getReceivers().containsAll(group.getReceivers())) {
                    targetGroup = group;
                    break;
                }
            }

            if (targetGroup == null) {
                System.err.println("⚠ Warning: Could not find matching group for message");
                return;
            }

            // Initialize messages list if needed
            if (targetGroup.getMessages() == null) {
                targetGroup.setMessages(new java.util.ArrayList<>());
            }

            // Add message to local group data
            targetGroup.getMessages().add(msg);

            if (msg.isFile()) {
                System.out.println("✓ Added file to local group data: " + msg.getFileName());
            } else {
                System.out.println("✓ Added message to local group: " + targetGroup.getGroupName());
            }

        } catch (Exception e) {
            System.err.println("✗ Failed to update local group chat: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void setOnUsernameChangeSuccess(Runnable handler) {
        this.onUsernameChangeSuccess = handler;
    }

    public void setOnUsernameChangeFailed(Runnable handler) {
        this.onUsernameChangeFailed = handler;
    }

    public void setOnPasswordChangeSuccess(Runnable handler) {
        this.onPasswordChangeSuccess = handler;
    }

    public void setOnPasswordChangeFailed(Runnable handler) {
        this.onPasswordChangeFailed = handler;
    }
}