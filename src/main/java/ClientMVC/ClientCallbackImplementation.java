package ClientMVC;

import Common.Interfaces.ClientCallbackInterface;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClientCallbackImplementation extends UnicastRemoteObject implements ClientCallbackInterface {
    public ClientCallbackImplementation() throws RemoteException {
        super();
    }

    @Override
    public void onSuccessfulSignUp() throws RemoteException {

    }

    @Override
    public void onFailedSignUp() throws RemoteException {

    }

    @Override
    public void onSuccessfulLogin() throws RemoteException {

    }

    @Override
    public void onFailedLogin() throws RemoteException {

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
    public void onGetChatHistories() throws RemoteException {

    }

    @Override
    public void onGetHistory() throws RemoteException {

    }

    @Override
    public void onGetUnreadMessages() throws RemoteException {

    }

    @Override
    public void onPrivateMessageSent() throws RemoteException {

    }

    @Override
    public void onPrivateMessageReceived() throws RemoteException {

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
