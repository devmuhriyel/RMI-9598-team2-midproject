package Common.Interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AdminInterface extends Remote {
    // Admin authentication
    void signUp(String username, String password, String verificationCode, String jsonString, AdminCallbackInterface adminCallback) throws RemoteException;
    void logIn(String username, String jsonString, AdminCallbackInterface adminCallback) throws RemoteException;
    void logOut(String username, String jsonString) throws RemoteException;
    void changePassword(String jsonString) throws RemoteException;
    void verifyCode(String jsonString) throws RemoteException;

    // Admin operations
    void searchUsers(String jsonString) throws RemoteException;
    void searchGroups(String jsonString) throws RemoteException;
    void deleteUser(String jsonString) throws RemoteException;
    void deleteGroup(String jsonString) throws RemoteException;

    // Admin chat history
    String getChatHistoryOfUser(String jsonString) throws RemoteException;
    String getChatHistoryOfGroup(String jsonString) throws RemoteException;
    String getChatHistoryOfCommunity(String jsonString) throws RemoteException;

    // Community messaging
    void sendCommunityMessage(String jsonString) throws RemoteException;
}