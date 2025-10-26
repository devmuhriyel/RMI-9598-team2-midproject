package Common.Interfaces;

import Common.Objects.ClientObjects.Conversation;
import Common.Objects.ClientObjects.GroupChat;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ClientInterface extends Remote {
    //USER LOG IN, SIGN UP, AND OTHER RELATED METHODS
    void signUp(String username, String password, String verificationCode, String jsonString, ClientCallbackInterface clientCallback) throws RemoteException;
    void logIn(String username, String jsonString, ClientCallbackInterface clientCalllback) throws RemoteException;
    void logOut(String username, String jsonString) throws RemoteException;
    void changePassword(String jsonString) throws RemoteException;
    void verifyCode(String jsonString) throws RemoteException;

    //USER HOME UI METHODS
    void searchUsers(String jsonString) throws RemoteException;
    void searchGroups(String jsonString) throws RemoteException;
    void sendFriendRequest(String jsonString) throws RemoteException;
    void getOnlineFriends(String jsonString) throws RemoteException;

    //USER ACCOUNT SETTINGS UI METHODS
    void changeUsername(String jsonString) throws RemoteException;
    void changePasswordWithOldPassword(String jsonString) throws RemoteException;

    //USER FRIEND REQUESTS UI METHODS
    void cancelFriendRequest(String jsonString) throws RemoteException;
    void acceptFriendRequest(String jsonString) throws RemoteException;
    void rejectFriendRequest(String jsonString) throws RemoteException;
    void removeFromFriendsList(String jsonString) throws RemoteException;

    //USER COMMON CHATTING METHODS
    void getChatHistories(String jsonString, String sender) throws RemoteException;
    void getChatHistoryWith(String jsonString, String sender, String receiver) throws RemoteException;
    List<GroupChat> getGroupChatHistories(String jsonString, String username) throws RemoteException;
    GroupChat getGroupChatHistoryWith(String jsonString, String username, String groupID) throws RemoteException;
    void getUnreadMessages(String jsonString) throws RemoteException;

    //USER PRIVATE CHATTING METHODS
    void sendPrivateMessage(String sender, String receiver, String message, String jsonString) throws RemoteException;
    void sendPrivateFile(String sender, String receiver, String fileName, String fileData, String jsonString) throws RemoteException;

    //USER GROUP CHATTING METHODS
    void sendGroupMessage(String sender, String groupID, String message, String jsonString) throws RemoteException;
    void sendGroupFile(String sender, String groupID, String fileName, String fileData, String jsonString) throws RemoteException;
    void createGroupChat(String jsonString) throws RemoteException;
    void addUserToGroup(String jsonString) throws RemoteException;
    void removeUserFromGroup(String jsonString) throws RemoteException;

    //USER COMMUNITY CHATTING METHODS
    void sendCommunityMessage(String jsonString) throws RemoteException;

    // CALLBACK REGISTRATION
    void registerClientCallback(String username, ClientCallbackInterface callback) throws RemoteException;
    void unregisterClientCallback(String username) throws RemoteException;
}