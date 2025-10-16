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
    public void signUp(String jsonString) throws RemoteException{

    }

    @Override
    public void logIn(String jsonString) throws RemoteException{

    }

    @Override
    public void logOut(String jsonString) throws RemoteException{

    }

    @Override
    public void changePassword(String jsonString) throws RemoteException{

    }

    @Override
    public void verifyCode(String jsonString) throws RemoteException{

    }

    //USER/ADMIN HOME UI METHODS
    @Override
    public void searchUsers(String jsonString) throws RemoteException{

    }

    @Override
    public void searchGroups(String jsonString) throws RemoteException{

    }

    @Override
    public void deleteGroup(String jsonString) throws RemoteException{

    }

    @Override
    public void getUnreadMessages(String jsonString) throws RemoteException{

    }

    @Override
    public void sendFriendRequest(String jsonString) throws RemoteException{

    }

    @Override
    public void getOnlineFriends(String jsonString) throws RemoteException{

    }

    //USER/ADMIN ACCOUNT SETTINGS UI METHODS
    @Override
    public void changeUsername(String jsonString) throws RemoteException{

    }

    @Override
    public void changePasswordWithOldPassword(String jsonString) throws RemoteException{

    }

    //USER/ADMIN FRIEND REQUESTS UI METHODS
    @Override
    public void cancelFriendRequest(String jsonString) throws RemoteException{

    }

    @Override
    public void acceptFriendRequest(String jsonString) throws RemoteException{

    }

    @Override
    public void rejectFriendRequest(String jsonString) throws RemoteException{

    }

    @Override
    public void removeFromFriendsList(String jsonString) throws RemoteException{

    }

    //USER/ADMIN COMMON CHATTING METHODS
    @Override
    public String getChatHistories(String jsonString) throws RemoteException{
        return "";
    }

    @Override
    public String getChatHistory(String jsonString) throws RemoteException{
        return "";
    }
    @Override
    public String getChatHistoryOfUser(String jsonString) throws RemoteException{
        return "";
    }

    @Override
    public String getChatHistoryOfGroup(String jsonString) throws RemoteException{
        return "";
    }

    @Override
    public String getChatHistoryOfCommunity(String jsonString) throws RemoteException{
        return "";
    }

    //USER/ADMIN PRIVATE CHATTING METHODS
    @Override
    public void sendPrivateMessage(String jsonString) throws RemoteException{

    }

    @Override
    public void sendPrivateFile(String jsonString) throws RemoteException{

    }

    //USER/ADMIN GROUP CHATTING METHODS
    @Override
    public void sendGroupMessage(String jsonString) throws RemoteException{

    }

    @Override
    public void sendGroupFile(String jsonString) throws RemoteException{

    }

    @Override
    public void createGroupChat(String jsonString) throws RemoteException{

    }

    @Override
    public void addUserToGroup(String jsonString) throws RemoteException{

    }

    @Override
    public void removeUserFromGroup(String jsonString) throws RemoteException{

    }

    //USER/ADMIN COMMUNITY CHATTING METHODS
    @Override
    public void sendCommunityMessage(String jsonString) throws RemoteException{

    }
}