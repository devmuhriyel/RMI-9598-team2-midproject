package Common.Interfaces;

import Common.Objects.ClientObjects.Conversation;
import Common.Objects.ClientObjects.GroupChat;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

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
    //UI for friends
    void onGetSentFriendRequests(java.util.List<String> sentRequests) throws RemoteException; // Added in previous step, confirming use
    void onGetReceivedFriendRequests(java.util.List<String> receivedRequests) throws RemoteException; // Added in previous step, confirming use
    void onGetFriendsList(java.util.List<String> friends) throws RemoteException;
    void onFriendRemoved() throws RemoteException; // To notify the user who initiated the unfriend
    void onFriendDeletedByOther() throws RemoteException; // To notify the user who was unfriended


    //USER COMMON CHATTING METHODS
    void onGetChatHistories(List<Conversation> allChats) throws RemoteException;
    void onGetChatHistoryWith(Conversation chatWith) throws RemoteException;
    void onGetGroupChatHistories(List<GroupChat> allGroupChats) throws RemoteException;
    void onGetGroupChatHistoryWith(GroupChat groupChatWith) throws RemoteException;
    void onGetUnreadMessages() throws RemoteException;

    //USER PRIVATE CHATTING METHODS
    void onPrivateMessageSent(String message, String jsonString) throws RemoteException;
    void onPrivateMessageReceived(String message, String jsonString) throws RemoteException;
    void onPrivateFileSent(String message, String jsonString) throws RemoteException;
    void onPrivateFileReceived(String message, String jsonString) throws RemoteException;

    //USER GROUP CHATTING METHODS
    void onGroupMessageSent(String message, String jsonString) throws RemoteException;
    void onGroupMessageReceived(String message, String jsonString) throws RemoteException;
    void onGroupFileSent(String message, String jsonString) throws RemoteException;
    void onGroupFileReceived(String message, String jsonString) throws RemoteException;
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