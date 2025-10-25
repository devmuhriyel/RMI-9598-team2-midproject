package ClientMVC;

import AdminMVC.AdminControllers.AdminConnectionController;
import AdminMVC.AdminControllers.AdminController;
import ClientMVC.ClientController.*;
import AdminMVC.AdminControllers.AdminLoginController;
import Common.Interfaces.ClientCallbackInterface;
import Common.Objects.ClientObjects.Conversation;
import Common.Objects.ClientObjects.Message;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class ClientCallbackImplementation extends UnicastRemoteObject implements ClientCallbackInterface {
    private String name;

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
    }


    @Override
    public void onSuccessfulSignUp() throws RemoteException {
        System.out.println("Client signup successful");
        if (onSuccessfulSignUpListener != null) {
            onSuccessfulSignUpListener.run();
        }
    }

    @Override
    public void onFailedSignUp() throws RemoteException {
        System.out.println("Client signup failed");
        if (onFailedSignUpListener != null) {
            onFailedSignUpListener.run();
        }
    }

    @Override
    public void onSuccessfulLogin() throws RemoteException {
        System.out.println("Client login successful");
        if (onSuccessfulLoginListener != null) {
            onSuccessfulLoginListener.run();
        }
    }

    @Override
    public void onFailedLogin() throws RemoteException {
        System.out.println("Client login failed");
        if (onFailedLoginListener != null) {
            onFailedLoginListener.run();
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

    }

    @Override
    public void onSearchGroupResults() throws RemoteException {

    }

    @Override
    public void onFriendRequestSent() throws RemoteException {

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

    }

    @Override
    public void onFriendAccepted() throws RemoteException {

    }

    @Override
    public void onFriendDeclined() throws RemoteException {

    }

    @Override
    public void onFriendDeleted() throws RemoteException {

    }

    @Override
    public void onCancelFriendRequest() throws RemoteException {

    }

    @Override
    public void onGetChatHistories(List<Conversation> allChats) throws RemoteException {

    }

    @Override
    public void onGetChatHistoryWith(Conversation chatWith) throws RemoteException {

    }


    @Override
    public void onGetUnreadMessages() throws RemoteException {

    }

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
    public void onPrivateFileSent() throws RemoteException {

    }

    @Override
    public void onPrivateFileReceived() throws RemoteException {

    }

    @Override
    public void onGroupMessageSent() throws RemoteException {

    }

    @Override
    public void onGroupMessageReceived() throws RemoteException {

    }

    @Override
    public void onGroupFileSent() throws RemoteException {

    }

    @Override
    public void onGroupFileReceived() throws RemoteException {

    }

    @Override
    public void onSuccessfulGroupCreation() throws RemoteException {

    }

    @Override
    public void onFailedGroupCreation() throws RemoteException {

    }

    @Override
    public void onSuccessfulMemberAdded() throws RemoteException {

    }

    @Override
    public void onFailedMemberAdded() throws RemoteException {

    }

    @Override
    public void onSuccessfulMemberRemoved() throws RemoteException {

    }

    @Override
    public void onFailedMemberRemoved() throws RemoteException {

    }

    @Override
    public void onCommunityMessageSent() throws RemoteException {

    }

    @Override
    public void onCommunityMessageReceived() throws RemoteException {

    }

    public String getName(){
        return name;
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
        return null;
    }
    
    // Listener setters for login and signup
    private Runnable onSuccessfulLoginListener;
    private Runnable onFailedLoginListener;
    private Runnable onSuccessfulSignUpListener;
    private Runnable onFailedSignUpListener;
    
    public void setOnSuccessfulLoginListener(Runnable listener) {
        this.onSuccessfulLoginListener = listener;
    }
    
    public void setOnFailedLoginListener(Runnable listener) {
        this.onFailedLoginListener = listener;
    }
    
    public void setOnSuccessfulSignUpListener(Runnable listener) {
        this.onSuccessfulSignUpListener = listener;
    }
    
    public void setOnFailedSignUpListener(Runnable listener) {
        this.onFailedSignUpListener = listener;
    }
}
