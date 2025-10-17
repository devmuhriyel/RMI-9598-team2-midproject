package AdminMVC;

import Common.Interfaces.AdminCallbackInterface;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class AdminCallbackImplementation extends UnicastRemoteObject implements AdminCallbackInterface {
    public AdminCallbackImplementation() throws RemoteException {
        super();
    }

    @Override
    public void onSuccessfulSignUp() throws RemoteException {

    }

    @Override
    public void onFailedSignUp() throws RemoteException {

    }

    @Override
    public void onSuccessfulLogin() throws RemoteException {

    }

    @Override
    public void onFailedLogin() throws RemoteException {

    }

    @Override
    public void onSuccessfulLogout() throws RemoteException {

    }

    @Override
    public void onSuccessfulPasswordChange() throws RemoteException {

    }

    @Override
    public void onFailedPasswordChange() throws RemoteException {

    }

    @Override
    public void onSuccessfulCodeVerification() throws RemoteException {

    }

    @Override
    public void onFailedCodeVerification() throws RemoteException {

    }

    @Override
    public void onSearchUserResults() throws RemoteException {

    }

    @Override
    public void onSearchGroupResults() throws RemoteException {

    }

    @Override
    public void onDeleteUser() throws RemoteException {

    }

    @Override
    public void onDeleteGroup() throws RemoteException {

    }

    @Override
    public void onGetChatHistoryOfUser() throws RemoteException {

    }

    @Override
    public void onGetChatHistoryOfGroup() throws RemoteException {

    }

    @Override
    public void onGetChatHistoryOfCommunity() throws RemoteException {

    }

    @Override
    public void onCommunityMessageSent() throws RemoteException {

    }

    @Override
    public void onCommunityMessageReceived() throws RemoteException {

    }
}
