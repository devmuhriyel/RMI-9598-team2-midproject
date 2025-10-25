package ClientMVC;

import ClientMVC.ClientController.LoginController;
import Common.Interfaces.ClientCallbackInterface;
import Common.Objects.ClientObjects.Conversation;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class ClientCallbackImplementation extends UnicastRemoteObject implements ClientCallbackInterface {
    private LoginController loginController;
    private String username;

    public ClientCallbackImplementation() throws RemoteException {
        super();
    }

    public void setLoginController(LoginController controller) {
        this.loginController = controller;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public void onSuccessfulSignUp() throws RemoteException {

    }

    @Override
    public void onFailedSignUp() throws RemoteException {

    }

    @Override
    public void onSuccessfulLogin() throws RemoteException {
        // Store session
        ClientJSONHandler.setCurrentUser(username);

        // Notify controller if set
        if (loginController != null) {
            loginController.onSuccessfulLoginFromRemote();
        }
    }

    @Override
    public void onFailedLogin() throws RemoteException {
        if (loginController != null) {
            loginController.onFailedLoginFromRemote();
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
}
