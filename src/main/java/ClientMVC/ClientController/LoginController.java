package ClientMVC.ClientController;

import ClientMVC.ClientCallbackImplementation;
import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.ClientView.Login.LoginJFXController;
import ClientMVC.ClientView.Login.LoginView;
import ClientMVC.ClientView.Signup.SignupView;
import Common.Interfaces.ClientInterface;
import Common.Interfaces.ClientCallbackInterface;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.rmi.RemoteException;

public class LoginController {
    private final LoginJFXController loginJFXController;
    private final LoginView view;
    private final UnifiedClientModel model;
    private final ClientCallbackImplementation clientCallback;

    public LoginController(LoginView loginView, ClientInterface remoteService, LoginJFXController loginJFXController, ClientCallbackInterface callback) throws Exception {

        this.view = loginView;
        this.loginJFXController = loginJFXController;
        this.clientCallback = (ClientCallbackImplementation) callback;
        this.clientCallback.setClientLoginController(this);
        this.model = new UnifiedClientModel(remoteService, clientCallback);

        initialize();
    }

    private void initialize() {
        loginJFXController.setLoginButtonListener(() -> {
            String username = loginJFXController.getUsername();
            String password = loginJFXController.getPassword();
            attemptLogin(username, password);
        });

        loginJFXController.setSignupLinkListener(() -> {
            Platform.runLater(() -> {
                try {
                    LoginView.getPrimaryStage().close();
                    new SignupView().getStage().show();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        });
    }

    private void attemptLogin(String username, String password) {
        clientCallback.setUsername(username);

        new Thread(() -> {
            try {
                model.logIn(username, password, clientCallback);
            } catch (RemoteException e) {
                Platform.runLater(() ->
                        loginJFXController.showAlert(Alert.AlertType.ERROR,
                                "Connection Error", "Lost connection to server")
                );
                e.printStackTrace();
            }
        }).start();
    }

    public void onSuccessfulLoginFromRemote() {
        Platform.runLater(() -> {
            view.login();
            try {
                HomeView homeView = new HomeView();
                Stage stage = new Stage();
                homeView.start(stage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void onFailedLoginFromRemote() {
        Platform.runLater(() ->
                loginJFXController.showAlert(Alert.AlertType.ERROR,
                        "Login Failed", "Invalid username or password")
        );
    }
}
