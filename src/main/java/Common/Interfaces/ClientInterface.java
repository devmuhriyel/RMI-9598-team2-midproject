package Common.Interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientInterface extends Remote {
    //USER LOG IN, SIGN UP, AND OTHER RELATED METHODS
    public void signUp(String jsonString) throws RemoteException;
    public void logIn(String jsonString) throws RemoteException;
    public void logOut(String jsonString) throws RemoteException;
    public void changePassword(String jsonString) throws RemoteException;
    public void verifyCode(String jsonString) throws RemoteException;

    //USER HOME UI METHODS
    public void searchUsers(String jsonString) throws RemoteException;
    public void searchGroups(String jsonString) throws RemoteException;
    public void sendFriendRequest(String jsonString) throws RemoteException;
    public void getOnlineFriends(String jsonString) throws RemoteException;

    //USER ACCOUNT SETTINGS UI METHODS
    public void changeUsername(String jsonString) throws RemoteException;
    public void changePasswordWithOldPassword(String jsonString) throws RemoteException;

    //USER FRIEND REQUESTS UI METHODS
    public void cancelFriendRequest(String jsonString) throws RemoteException;
    public void acceptFriendRequest(String jsonString) throws RemoteException;
    public void rejectFriendRequest(String jsonString) throws RemoteException;
    public void removeFromFriendsList(String jsonString) throws RemoteException;

    //USER COMMON CHATTING METHODS
    public String getChatHistories(String jsonString) throws RemoteException;
    public String getChatHistory(String jsonString) throws RemoteException;
    public void getUnreadMessages(String jsonString) throws RemoteException;

    //USER PRIVATE CHATTING METHODS
    public void sendPrivateMessage(String sender, String receiver, String message, String jsonString) throws RemoteException;
    public void sendPrivateFile(String jsonString) throws RemoteException;

    //USER GROUP CHATTING METHODS
    public void sendGroupMessage(String jsonString) throws RemoteException;
    public void sendGroupFile(String jsonString) throws RemoteException;
    public void createGroupChat(String jsonString) throws RemoteException;
    public void addUserToGroup(String jsonString) throws RemoteException;
    public void removeUserFromGroup(String jsonString) throws RemoteException;

    //USER COMMUNITY CHATTING METHODS
    public void sendCommunityMessage(String jsonString) throws RemoteException;
}
