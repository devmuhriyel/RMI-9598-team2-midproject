package Common.Interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AdminCallbackInterface extends Remote {
    //ADMIN LOG IN, SIGN UP, AND OTHER RELATED METHODS
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
