package AdminMVC;

import Common.Interfaces.AdminCallbackInterface;
import javafx.application.Platform;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.function.Consumer;

public class AdminCallbackImplementation extends UnicastRemoteObject implements AdminCallbackInterface {

    private Consumer<String> loginListener;
    private AdminMVC.AdminControllers.AdminLoginController adminLoginController;

    public AdminCallbackImplementation() throws RemoteException {
        super();
    }

    public void setLoginListener(Consumer<String> listener) {
        this.loginListener = listener;
    }

    public void setAdminLoginController(AdminMVC.AdminControllers.AdminLoginController controller) {
        this.adminLoginController = controller;
    }

    @Override
    public void onSuccessfulLogin() throws RemoteException {
        System.out.println("Admin login successful via RMI");
        if (adminLoginController != null) {
            Platform.runLater(() -> adminLoginController.handleLoginResponse("LOGIN_SUCCESS"));
        }
        if (loginListener != null) {
            Platform.runLater(() -> loginListener.accept("LOGIN_SUCCESS"));
        }
    }

    @Override
    public void onFailedLogin() throws RemoteException {
        System.out.println("Admin login failed via RMI");
        if (adminLoginController != null) {
            Platform.runLater(() -> adminLoginController.handleLoginResponse("LOGIN_FAILED"));
        }
        if (loginListener != null) {
            Platform.runLater(() -> loginListener.accept("LOGIN_FAILED"));
        }
    }

    @Override
    public void onSuccessfulSignUp() throws RemoteException {
        // Not used for admin
    }

    @Override
    public void onFailedSignUp() throws RemoteException {
        // Not used for admin
    }

    @Override
    public void onSuccessfulLogout() throws RemoteException {
        System.out.println("Admin logout successful");
    }

    @Override
    public void onSuccessfulPasswordChange() throws RemoteException {
        System.out.println("Password changed successfully");
    }

    @Override
    public void onFailedPasswordChange() throws RemoteException {
        System.out.println("Password change failed");
    }

    @Override
    public void onSuccessfulCodeVerification() throws RemoteException {
        System.out.println("Verification code valid");
    }

    @Override
    public void onFailedCodeVerification() throws RemoteException {
        System.out.println("Verification code invalid");
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