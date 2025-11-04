package Common.Interfaces;

import Common.Objects.ClientObjects.Client;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface AdminCallbackInterface extends Remote {
    //ADMIN LOG IN, SIGN UP, AND OTHER RELATED METHODS
    void onReceiveCommunityMessage(String messageJson) throws RemoteException;
    void onSuccessfulSignUp() throws RemoteException;
    void onFailedSignUp() throws RemoteException;
    void onSuccessfulLogin() throws RemoteException;
    void onFailedLogin() throws RemoteException;
    void onSuccessfulLogout() throws RemoteException;
    void onSuccessfulPasswordChange() throws RemoteException;
    void onFailedPasswordChange() throws RemoteException;
    void onSuccessfulCodeVerification() throws RemoteException;
    void onFailedCodeVerification() throws RemoteException;

    //ADMIN HOME UI METHODS
    void onSearchUserResults() throws RemoteException;
    void onSearchGroupResults() throws RemoteException;

    // DATA DELIVERY METHODS
    void onReceiveUsersList(List<Client> users) throws RemoteException;
    void onReceiveGroupsList(String groupsJson) throws RemoteException;
    void onReceiveActiveUsersList(List<String> activeUsernames) throws RemoteException; // NEW

    //ADMIN ACCOUNT AND GROUP UI METHODS
    void onDeleteUser() throws RemoteException;
    void onDeleteGroup() throws RemoteException;

    //ADMIN COMMON CHATTING METHODS
    void onGetChatHistoryOfUser() throws RemoteException;
    void onGetChatHistoryOfGroup() throws RemoteException;
    void onGetChatHistoryOfCommunity() throws RemoteException;

    //ADMIN COMMUNITY CHATTING METHODS
    void onCommunityMessageSent() throws RemoteException;
    void onCommunityMessageReceived() throws RemoteException;
}