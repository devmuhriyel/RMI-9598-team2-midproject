package Common.Interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientCallbackInterface extends Remote {

    //USER LOG IN, SIGN UP, AND OTHER RELATED METHODS
    void onSuccessfulSignUp() throws RemoteException;
    void onFailedSignUp() throws RemoteException;
    void onSuccessfulLogin() throws RemoteException;
    void onFailedLogin() throws RemoteException;
    void onSuccessfulLogout() throws RemoteException;
    void onSuccessfulPasswordChange() throws RemoteException;
    void onFailedPasswordChange() throws RemoteException;
    void onSuccessfulCodeVerification() throws RemoteException;
    void onFailedCodeVerification() throws RemoteException;

    //USER HOME UI METHODS
    void onSearchUserResults() throws RemoteException;
    void onSearchGroupResults() throws RemoteException;
    void onFriendRequestSent() throws RemoteException;
    void onGetOnlineFriends() throws RemoteException;

    //USER ACCOUNT SETTINGS UI METHODS
    void onSuccessfulUsernameChange() throws RemoteException;
    void onFailedUsernameChange() throws RemoteException;
    void onSuccessfulPasswordChangeWithOldPassword() throws RemoteException;
    void onFailedPasswordChangeWithOldPassword() throws RemoteException;

    //USER FRIEND REQUESTS UI METHODS
    void onFriendRequestReceived() throws RemoteException;
    void onFriendAccepted() throws RemoteException;
    void onFriendDeclined() throws RemoteException;
    void onFriendDeleted() throws RemoteException;
    void onCancelFriendRequest() throws RemoteException;

    //USER COMMON CHATTING METHODS
    void onGetChatHistories() throws RemoteException;
    void onGetHistory() throws RemoteException;
    void onGetUnreadMessages() throws RemoteException;

    //USER PRIVATE CHATTING METHODS
    void onPrivateMessageSent(String msg, String jsonString) throws RemoteException;
    void onPrivateMessageReceived(String msg, String jsonString) throws RemoteException;
    void onPrivateFileSent() throws RemoteException;
    void onPrivateFileReceived() throws RemoteException;

    //USER GROUP CHATTING METHODS
    void onGroupMessageSent() throws RemoteException;
    void onGroupMessageReceived() throws RemoteException;
    void onGroupFileSent() throws RemoteException;
    void onGroupFileReceived() throws RemoteException;
    void onSuccessfulGroupCreation() throws RemoteException;
    void onFailedGroupCreation() throws RemoteException;
    void onSuccessfulMemberAdded() throws RemoteException;
    void onFailedMemberAdded() throws RemoteException;
    void onSuccessfulMemberRemoved() throws RemoteException;
    void onFailedMemberRemoved() throws RemoteException;

    //USER COMMUNITY CHATTING METHODS
    void onCommunityMessageSent() throws RemoteException;
    void onCommunityMessageReceived() throws RemoteException;


}
