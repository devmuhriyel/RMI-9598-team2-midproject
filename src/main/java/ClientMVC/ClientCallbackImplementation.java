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
        System.out.println("Server says signup success!");
        if (clientSignupController != null) {
            clientSignupController.onSuccessfulSignUpFromRemote();
        }
    }

    @Override
    public void onFailedSignUp() throws RemoteException {
        System.out.println("Server says signup failed!");
        if (clientSignupController != null) {
            clientSignupController.onFailedSignUpFromRemote();
        }
    }

    @Override
    public void onSuccessfulLogin(String clientDataJson) throws RemoteException {
        System.out.println("Server says login success!");
        System.out.println("Received client data JSON");

        // Parse and store the client data
        try {
            this.clientData = gson.fromJson(clientDataJson, Client.class);
            this.username = clientData.getUsername();
            ClientJSONHandler.setCurrentUser(username);

            System.out.println("✓ Parsed client data for: " + clientData.getUsername());
            System.out.println("  - Friends: " + (clientData.getFriends() != null ? clientData.getFriends().size() : 0));
            System.out.println("  - Groups: " + (clientData.getGroupChats() != null ? clientData.getGroupChats().size() : 0));
            System.out.println("  - Conversations: " + (clientData.getConversations() != null ? clientData.getConversations().size() : 0));

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
        System.out.println("Callback: Search user results received!");
        // TODO: Parse results and notify home controller
    }

    @Override
    public void onSearchGroupResults() throws RemoteException {
        System.out.println("Callback: Search group results received!");
        // TODO: Parse results and notify home controller
    }

    @Override
    public void onFriendRequestSent() throws RemoteException {
        System.out.println("Callback: Friend request sent successfully!");
        if (clientHomeController != null) {
            clientHomeController.handleFriendRequestSent();
        }
    }

    @Override
    public void onGetOnlineFriends() throws RemoteException {

    }

    @Override
    public void onSuccessfulUsernameChange() throws RemoteException {
        System.out.println("✓ Callback received: Username change successful");

        // Update the stored client data
        if (this.clientData != null && clientAccountController != null) {
            String newUsername = clientAccountController.getNewUsernameFromView();
            this.clientData.setUsername(newUsername);
            this.username = newUsername;
            ClientJSONHandler.setCurrentUser(newUsername);
        }

        // Run existing handler
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
        if (clientAccountController != null) {
            // TODO: Optionally show a notification to the user
        }
        if (clientHomeController != null) {
            // TODO: Show notification badge
        }
    }

    @Override
    public void onFriendAccepted() throws RemoteException {
        System.out.println("Callback: Friend request accepted!");
        if (clientAccountController != null) {
            // TODO: Handle friend accepted
        }
        if (clientHomeController != null) {
            clientHomeController.refreshUserData();
        }
    }

    @Override
    public void onFriendDeclined() throws RemoteException {
        System.out.println("Callback: Friend request declined!");
        if (clientAccountController != null) {
            // TODO: Handle friend declined
        }
    }

    @Override
    public void onFriendDeleted() throws RemoteException {
        System.out.println("Callback: Friend removed from list!");
        if (clientAccountController != null) {
            // TODO: Handle friend removed
        }
        if (clientHomeController != null) {
            clientHomeController.refreshUserData();
        }
    }

    @Override
    public void onCancelFriendRequest() throws RemoteException {
        System.out.println("Callback: Friend request cancelled!");
        if (clientAccountController != null) {
            // TODO: Handle request cancelled
        }
    }

    @Override
    public void onGetFriendsList(List<String> friends) throws RemoteException {
        System.out.println("Callback: Received friends list of size: " + (friends != null ? friends.size() : 0));
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

    }

    @Override
    public void onGetChatHistories(List<Conversation> allChats) throws RemoteException {
        if (clientChatController != null) {
            System.out.println("Received " + allChats.size() + " conversations");
            clientChatController.onGetChatHistories(allChats);
        }
    }
// In ClientCallbackImplementation.java

    @Override
    public void onGetSentFriendRequests(List<String> sentRequests) throws RemoteException {
        if (clientAccountController != null) {
            System.out.println("Callback: Received sent friend requests of size: " + (sentRequests != null ? sentRequests.size() : 0));
            // Cast and call the new method in the controller
            clientAccountController.handleSentFriendRequests(sentRequests);
        }
    }

    @Override
    public void onGetReceivedFriendRequests(List<String> receivedRequests) throws RemoteException {
        if (clientAccountController != null) {
            System.out.println("Callback: Received received friend requests of size: " + (receivedRequests != null ? receivedRequests.size() : 0));
            // Cast and call the new method in the controller
            clientAccountController.handleReceivedFriendRequests(receivedRequests);
        }
    }
    @Override
    public void onGetChatHistoryWith(Conversation chatWith) throws RemoteException {
        if (clientChatController != null) {
            int messageCount = chatWith.getMessages() != null ? chatWith.getMessages().size() : 0;
            System.out.println("Received conversation with " + chatWith.getRecipient() + ": " + messageCount + " messages");
            clientChatController.onGetChatHistoryWith(chatWith);
        }
    }

    @Override
    public void onGetGroupChatHistories(List<GroupChat> allGroupChats) throws RemoteException {
        if (clientChatController != null) {
            System.out.println("Received " + allGroupChats.size() + " group chats");
            clientChatController.onGetGroupChatHistories(allGroupChats);
        }
    }

    @Override
    public void onGetGroupChatHistoryWith(GroupChat groupChatWith) throws RemoteException {
        if (clientChatController != null) {
            int messageCount = groupChatWith.getMessages() != null ? groupChatWith.getMessages().size() : 0;
            System.out.println("Received group chat " + groupChatWith.getGroupName() + ": " + messageCount + " messages");
            clientChatController.onGetGroupChatHistoryWith(groupChatWith);
        }
    }

    @Override
    public void onGetUnreadMessages() throws RemoteException {}

    @Override
    public void onPrivateMessageSent(String message, String jsonString) throws RemoteException {
        if (clientChatController != null) {
            Message msg = messageStringToObject(message);
            clientChatController.onPrivateMessageSent(msg);
        }
    }

    @Override
    public void onPrivateMessageReceived(String message, String jsonString) throws RemoteException {
        if (clientChatController != null) {
            Message msg = messageStringToObject(message);
            clientChatController.onPrivateMessageReceived(msg);
        }
    }

    @Override
    public void onPrivateFileSent(String message, String jsonString) throws RemoteException {
        if (clientChatController != null) {
            Message msg = messageStringToObject(message);
            clientChatController.onPrivateFileSent(msg);
        }
    }

    @Override
    public void onPrivateFileReceived(String message, String jsonString) throws RemoteException {
        if (clientChatController != null) {
            Message msg = messageStringToObject(message);
            clientChatController.onPrivateFileReceived(msg);
        }
    }

    @Override
    public void onGroupMessageSent(String message, String jsonString) throws RemoteException {
        if (clientChatController != null) {
            Message msg = messageStringToObject(message);
            clientChatController.onGroupMessageSent(msg);
        }
    }

    @Override
    public void onGroupMessageReceived(String message, String jsonString) throws RemoteException {
        if (clientChatController != null) {
            Message msg = messageStringToObject(message);
            clientChatController.onGroupMessageReceived(msg);
        }
    }

    @Override
    public void onGroupFileSent(String message, String jsonString) throws RemoteException {
        if (clientChatController != null) {
            Message msg = messageStringToObject(message);
            clientChatController.onGroupFileSent(msg);
        }
    }

    @Override
    public void onGroupFileReceived(String message, String jsonString) throws RemoteException {
        if (clientChatController != null) {
            Message msg = messageStringToObject(message);
            clientChatController.onGroupFileReceived(msg);
        }
    }

    @Override
    public void onSuccessfulGroupCreation() throws RemoteException {
        System.out.println("Callback: Group created successfully. Refreshing home data.");
        if (clientHomeController != null) {
            clientHomeController.refreshUserData();
        }
    }

    @Override
    public void onFailedGroupCreation() throws RemoteException {
        System.out.println("Callback: Group creation failed.");
        Platform.runLater(() -> {
            try {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Group Creation Failed");
                alert.setHeaderText(null);
                alert.setContentText("Unable to create the group. Try a different name.");
                alert.showAndWait();
            } catch (Exception ignored) {}
        });
    }

    @Override
    public void onSuccessfulMemberAdded() throws RemoteException {}

    @Override
    public void onFailedMemberAdded() throws RemoteException {}

    @Override
    public void onSuccessfulMemberRemoved() throws RemoteException {}

    @Override
    public void onFailedMemberRemoved() throws RemoteException {}

    @Override
    public void onCommunityMessageSent() throws RemoteException {}

    @Override
    public void onCommunityMessageReceived() throws RemoteException {}



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

    public void setOnUsernameChangeSuccess(Runnable handler) {
        System.out.println("  → Setting username change success handler");
        this.onUsernameChangeSuccess = handler;
    }

    public void setOnUsernameChangeFailed(Runnable handler) {
        System.out.println("  → Setting username change failed handler");
        this.onUsernameChangeFailed = handler;
    }

    public void setOnPasswordChangeSuccess(Runnable handler) {
        System.out.println("  → Setting password change success handler");
        this.onPasswordChangeSuccess = handler;
    }

    public void setOnPasswordChangeFailed(Runnable handler) {
        System.out.println("  → Setting password change failed handler");
        this.onPasswordChangeFailed = handler;
    }


}