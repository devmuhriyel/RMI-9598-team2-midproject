package ClientMVC.ClientController;

import ClientMVC.ClientCallbackImplementation;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.ClientView.Login.LoginJFXController;
import ClientMVC.ClientView.Login.LoginView;
import ClientMVC.ClientView.Signup.SignupView;
import Common.Interfaces.ClientInterface; // ðŸ’¡ NEW: Import the RMI service interface
import Common.Interfaces.ClientCallbackInterface; // ðŸ’¡ NEW: Import the RMI callback interface (needed for later)

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject; // Used for client callback object (if needed)


public class LoginController {
    private final LoginJFXController loginJFXController;
    private final LoginView view;
    // ðŸ’¡ REPLACED: Use the RMI stub directly instead of UnifiedClientModel
    private final ClientInterface remoteService;
    private ClientCallbackImplementation clientCallback;


    // NOTE: For full RMI Callback (like onSuccessfulLogin), you'll need a client-side
    // implementation of ClientCallbackInterface, which is beyond this controller's scope.
    // We will simulate the synchronous RMI call here.

    public LoginController(LoginView loginView, ClientInterface remoteService, LoginJFXController loginJFXController) {
        this.loginJFXController = loginJFXController;
        this.view = loginView;
        this.remoteService = remoteService;
        initialize();
    }

    private void initialize() {
        loginJFXController.setLoginButtonListener(() -> {
            String username = loginJFXController.getUsername();
            String password = loginJFXController.getPassword();
            //  Call the RMI login method
            attemptLogin(username, password);
        });

        loginJFXController.setSignupLinkListener(() -> {
            Platform.runLater(() -> {
                try {
                    LoginView.getPrimaryStage().close();
                    SignupView signupView = new SignupView();

                    // NOTE: SignupController now needs the ClientInterface stub
                    //new SignupController(remoteService, signupView, signupView.getFxController());

                    signupView.getStage().show();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        });

        // RMI method for Forgot Password needs to be synchronous or return an immediate status.
        // We will simplify this to a synchronous call for the example.
    }

    private void attemptLogin(String username, String password) {
        // Set username in callback so it knows who logged in
        clientCallback.setUsername(username);

        // Create JSON
        String jsonString = createLoginJson(username, password);

        new Thread(() -> {
            try {
                // Call RMI with callback
                remoteService.logIn(username, jsonString, clientCallback);

                // Server will call back when done

            } catch (RemoteException e) {
                Platform.runLater(() -> {
                    loginJFXController.showAlert(
                            Alert.AlertType.ERROR,
                            "Connection Error",
                            "Lost connection to server"
                    );
                });
                e.printStackTrace();
            }
        }).start();
    }

    // Called by ClientCallbackImplementation
    public void onSuccessfulLoginFromRemote() {
        Platform.runLater(() -> {
            view.login(); // Close login view
            try {
                HomeView homeView = new HomeView();
                Stage stage = new Stage();
                homeView.start(stage);
                // Pass remoteService and callback to HomeController
                // new HomeController(remoteService, homeView, ...);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void onFailedLoginFromRemote() {
        Platform.runLater(() -> {
            loginJFXController.showAlert(
                    Alert.AlertType.ERROR,
                    "Login Failed",
                    "Invalid username or password"
            );
        });
    }

    private String createLoginJson(String username, String password) {
        return String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}",
                username,
                password
        );
    }
}