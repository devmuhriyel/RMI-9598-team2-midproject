package Common.Interfaces;

import java.rmi.RemoteException;

public interface AdminInterface {
    //ADMIN LOG IN, SIGN UP, AND OTHER RELATED METHODS
    public void signUp(String jsonString) throws RemoteException;
    public void logIn(String jsonString) throws RemoteException;
    public void logOut(String jsonString) throws RemoteException;
    public void changePassword(String jsonString) throws RemoteException;
    public void verifyCode(String jsonString) throws RemoteException;

    //ADMIN HOME UI METHODS
    public void searchUsers(String jsonString) throws RemoteException;
    public void searchGroups(String jsonString) throws RemoteException;

    //ADMIN ACCOUNT AND GROUP UI METHODS
    void deleteUser(String jsonString) throws RemoteException;
    void deleteGroup(String jsonString) throws RemoteException;

    //ADMIN COMMON CHATTING METHODS
    public String getChatHistoryOfUser(String jsonString) throws RemoteException;
    public String getChatHistoryOfGroup(String jsonString) throws RemoteException;
    public String getChatHistoryOfCommunity(String jsonString) throws RemoteException;

    //ADMIN COMMUNITY CHATTING METHODS
    public void sendCommunityMessage(String jsonString) throws RemoteException;
}
