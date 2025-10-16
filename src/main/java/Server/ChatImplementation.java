package Server;

import ClientMVC.UserXMLHandler;
import CommonClasses.ChatInterface;
import Server.ServerClasses.Admin;
import Server.ServerClasses.User;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;

public class ChatImplementation extends UnicastRemoteObject implements ChatInterface {
    private ConcurrentHashMap<String, User> onlineUsers;
    private ConcurrentHashMap<String, User> offlineUsers;
    private ConcurrentHashMap<String, Admin> onlineAdmins;
    private ConcurrentHashMap<String, Admin> offlineAdmins;

    public ChatImplementation() throws RemoteException {
        super();
    }

    //USER LOG IN, SIGN UP, AND OTHER RELATED METHODS
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

    //USER HOME UI METHODS
    @Override
    public void searchUsers(String jsonString) {

    }

    @Override
    public void searchGroups(String jsonString) {

    }

    @Override
    public void sendFriendRequest(String jsonString) {

    }

    @Override
    public void getOnlineFriends(String jsonString) {

    }

    //USER ACCOUNT SETTINGS UI METHODS
    @Override
    public void changeUsername(String jsonString) {

    }

    @Override
    public void changePasswordWithOldPassword(String jsonString) {

    }

    //USER FRIEND REQUESTS UI METHODS
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

    @Override
    public void getChatHistories(String jsonString) {

    }

    //USER COMMON CHATTING METHODS
    @Override
    public void getChatHistory(String jsonString) {

    }

    @Override
    public void getUnreadMessages(String jsonString) {

    }

    //USER PRIVATE CHATTING METHODS
    @Override
    public void sendPrivateMessage(String jsonString) {

    }

    @Override
    public void sendPrivateFile(String jsonString) {

    }

    //USER GROUP CHATTING METHODS
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

    //USER COMMUNITY CHATTING METHODS
    @Override
    public void sendCommunityMessage(String jsonString) {

    }
}