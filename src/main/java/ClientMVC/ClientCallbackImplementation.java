package ClientMVC;

import AdminMVC.AdminControllers.AdminConnectionController;
import AdminMVC.AdminControllers.AdminController;
import ClientMVC.ClientController.*;
import AdminMVC.AdminControllers.AdminLoginController;
import Common.Interfaces.ClientCallbackInterface;
import Common.Objects.ClientObjects.Conversation;
import Common.Objects.ClientObjects.GroupChat;
import Common.Objects.ClientObjects.Message;
import com.google.gson.Gson;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class ClientCallbackImplementation extends UnicastRemoteObject implements ClientCallbackInterface {
    private String username;
    private Gson gson;

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

    public ClientCallbackImplementation() throws RemoteException {
        super();
        this.gson = new Gson();
    }

    @Override
    public void onSuccessfulSignUp() throws RemoteException {

    }

    @Override
    public void onFailedSignUp() throws RemoteException {

    }

    @Override
    public void onSuccessfulLogin() throws RemoteException {
        ClientJSONHandler.setCurrentUser(username);
        System.out.println("Server says login success!");
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
        // Notify HomeController if it's active
        if (clientHomeController != null) {
            clientHomeController.handleFriendRequestSent();
        }
    }

    @Override
    public void onGetOnlineFriends() throws RemoteException {

    }

    @Override
    public void onSuccessfulUsernameChange() throws RemoteException {

    }

    @Override
    public void onFailedUsernameChange() throws RemoteException {

    }

    @Override
    public void onSuccessfulPasswordChangeWithOldPassword() throws RemoteException {

    }

    @Override
    public void onFailedPasswordChangeWithOldPassword() throws RemoteException {

    }

    @Override
    public void onFriendRequestReceived() throws RemoteException {
        System.out.println("Callback: New friend request received!");

        // Notify AccountController to refresh the received requests list
        if (clientAccountController != null) {
            // TODO: Optionally show a notification to the user
            // clientAccountController.showNewFriendRequestNotification();
        }

        // Also notify HomeController if active (to show notification badge, etc.)
        if (clientHomeController != null) {
            // clientHomeController.showFriendRequestNotification();
        }
    }

    @Override
    public void onFriendAccepted() throws RemoteException {
        System.out.println("Callback: Friend request accepted!");

        // Notify AccountController
        if (clientAccountController != null) {
            // TODO: Extract friend name from server response
            // For now, just refresh
            // clientAccountController.handleFriendAccepted(friendName);
        }

        // Refresh HomeController's friends list
        if (clientHomeController != null) {
            clientHomeController.refreshUserData();
        }
    }

    @Override
    public void onFriendDeclined() throws RemoteException {
        System.out.println("Callback: Friend request declined!");

        // Notify AccountController
        if (clientAccountController != null) {
            // TODO: Extract friend name from server response
            // clientAccountController.handleFriendDeclined(friendName);
        }
    }

    @Override
    public void onFriendDeleted() throws RemoteException {
        System.out.println("Callback: Friend removed from list!");

        // Notify AccountController
        if (clientAccountController != null) {
            // TODO: Extract friend name from server response
            // clientAccountController.handleFriendRemoved(friendName);
        }

        // Refresh HomeController's friends list
        if (clientHomeController != null) {
            clientHomeController.refreshUserData();
        }
    }

    @Override
    public void onCancelFriendRequest() throws RemoteException {
        System.out.println("Callback: Friend request cancelled!");

        // Notify AccountController
        if (clientAccountController != null) {
            // TODO: Extract friend name from server response
            // clientAccountController.handleFriendRequestCancelled(friendName);
        }
    }

    @Override
    public void onGetSentFriendRequests(List<String> sentRequests) throws RemoteException {

    }

    @Override
    public void onGetReceivedFriendRequests(List<String> receivedRequests) throws RemoteException {

    }

    @Override
    public void onGetFriendsList(List<String> friends) throws RemoteException {

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
    public void onSuccessfulGroupCreation() throws RemoteException {}

    @Override
    public void onFailedGroupCreation() throws RemoteException {}

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

    public String getUsername(){
        return username;
    }

    public void setUsername(String username){
        this.username = username;
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
}
