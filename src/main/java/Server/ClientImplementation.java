package Server;

import CommonInterface.AdminInterface;
import CommonInterface.ClientInterface;
import Server.ServerClasses.Admin;
import Server.ServerClasses.Client;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;

public class ClientImplementation extends UnicastRemoteObject implements ClientInterface, AdminInterface {
    //VARIABLES
    private ConcurrentHashMap<String, Client> onlineUsers;
    private ConcurrentHashMap<String, Client> allUsers;
    private ConcurrentHashMap<String, Admin> onlineAdmins;
    private ConcurrentHashMap<String, Admin> allAdmins;
    private ServerJSONHandler serverJSONHandler;

    //CONSTRUCTORS
    public ClientImplementation() throws RemoteException {
        super();
    }

    //USER/ADMIN LOG IN, SIGN UP, AND OTHER RELATED METHODS
    @Override
    public void signUp(String jsonString) {

    }

    @Override
    public void logIn(String jsonString) {

    }

    @Override
    public void logOut(String jsonString) {

    }

    @Override
    public void changePassword(String jsonString) {

    }

    @Override
    public void verifyCode(String jsonString) {

    }

    //USER/ADMIN HOME UI METHODS
    @Override
    public void searchUsers(String jsonString) {

    }

    @Override
    public void searchGroups(String jsonString) {

    }

    @Override
    public void deleteGroup(String jsonString) {

    }

    @Override
    public void getUnreadMessages(String jsonString) {

    }

    @Override
    public void sendFriendRequest(String jsonString) {

    }

    @Override
    public void getOnlineFriends(String jsonString) {

    }

    //USER/ADMIN ACCOUNT SETTINGS UI METHODS
    @Override
    public void changeUsername(String jsonString) {

    }

    @Override
    public void changePasswordWithOldPassword(String jsonString) {

    }

    //USER/ADMIN FRIEND REQUESTS UI METHODS
    @Override
    public void cancelFriendRequest(String jsonString) {

    }

    @Override
    public void acceptFriendRequest(String jsonString) {

    }

    @Override
    public void rejectFriendRequest(String jsonString) {

    }

    @Override
    public void removeFromFriendsList(String jsonString) {

    }

    //USER/ADMIN COMMON CHATTING METHODS
    @Override
    public String getChatHistories(String jsonString) {
        return "";
    }

    @Override
    public String getChatHistory(String jsonString) {
        return "";
    }
    @Override
    public String getChatHistoryOfUser(String jsonString) {
        return "";
    }

    @Override
    public String getChatHistoryOfGroup(String jsonString) {
        return "";
    }

    @Override
    public String getChatHistoryOfCommunity(String jsonString) {
        return "";
    }

    //USER/ADMIN PRIVATE CHATTING METHODS
    @Override
    public void sendPrivateMessage(String jsonString) {

    }

    @Override
    public void sendPrivateFile(String jsonString) {

    }

    //USER/ADMIN GROUP CHATTING METHODS
    @Override
    public void sendGroupMessage(String jsonString) {

    }

    @Override
    public void sendGroupFile(String jsonString) {

    }

    @Override
    public void createGroupChat(String jsonString) {

    }

    @Override
    public void addUserToGroup(String jsonString) {

    }

    @Override
    public void removeUserFromGroup(String jsonString) {

    }

    //USER/ADMIN COMMUNITY CHATTING METHODS
    @Override
    public void sendCommunityMessage(String jsonString) {

    }
}