package Common.Interfaces;

import java.rmi.RemoteException;

public interface AdminInterface {
    //ADMIN LOG IN, SIGN UP, AND OTHER RELATED METHODS
    void signUp(String username, String password, String verificationCode, String jsonString, AdminCallbackInterface adminCallback) throws RemoteException;
    void logIn(String username, String jsonString, AdminCallbackInterface adminCallBbck) throws RemoteException;
    void logOut(String username, String jsonString) throws RemoteException;
    void changePassword(String jsonString) throws RemoteException;
    void verifyCode(String jsonString) throws RemoteException;

    //ADMIN HOME UI METHODS
    void searchUsers(String jsonString) throws RemoteException;
    void searchGroups(String jsonString) throws RemoteException;

    //ADMIN ACCOUNT AND GROUP UI METHODS
    void deleteUser(String jsonString) throws RemoteException;
    void deleteGroup(String jsonString) throws RemoteException;

    //ADMIN COMMON CHATTING METHODS
    String getChatHistoryOfUser(String jsonString) throws RemoteException;
    String getChatHistoryOfGroup(String jsonString) throws RemoteException;
    String getChatHistoryOfCommunity(String jsonString) throws RemoteException;

    //ADMIN COMMUNITY CHATTING METHODS
    void sendCommunityMessage(String jsonString) throws RemoteException;
}
