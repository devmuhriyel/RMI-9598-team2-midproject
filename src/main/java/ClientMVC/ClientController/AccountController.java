package ClientMVC.ClientController;

import ClientMVC.ClientCallbackImplementation;
import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Account.AccountView;
import ClientMVC.ClientView.Account.AccountJFXController;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.ClientView.Login.LoginView;
import Common.Interfaces.ClientInterface;
import javafx.application.Platform;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class AccountController {
    private final UnifiedClientModel model;
    private final AccountView view;
    private final AccountJFXController accountJFXController;
    private final ClientCallbackImplementation clientCallback;

    public AccountController(UnifiedClientModel model, AccountView view, AccountJFXController accountJFXController, ClientCallbackImplementation callback) {
        this.view = view;
        this.model = model;
        this.accountJFXController = accountJFXController;
        this.clientCallback = callback;
        callback.setClientAccountController(this);
        initialize();
    }

    private void initialize() {
        accountJFXController.updateDisplayedUsername(model.getUsername());
        accountJFXController.setSentRequestButtonListener(() -> {
            try {
                model.getSentFriendRequests();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });
        accountJFXController.setAcceptRequestListener(friendName -> {
            try {
                model.acceptFriendRequest(friendName);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });
        accountJFXController.setDeclineRequestListener(friendName -> {
            try {
                model.rejectFriendRequest(friendName);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });

        accountJFXController.setBackButtonListener(() -> {
            view.getStage().close();
            HomeView homeView = new HomeView();
            Stage homeStage = new Stage();
            homeView.start(homeStage);
            new HomeController(model, homeView, homeView.getFXController(), clientCallback);
        });

        accountJFXController.setLogoutAccountButtonListener(() -> {
            view.getStage().close();
            LoginView loginView = new LoginView();
            Stage loginStage = new Stage();
            try {
                loginView.start(loginStage);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            //new LoginController(loginView, clientService, loginView.getFXController(), clientCallback);

        });
        accountJFXController.setUsernameChangeListener(this::handleUsernameChange);
//        accountJFXController.setPasswordChangeListener(this::handlePasswordChange);
        try {
            accountJFXController.showProfile();
        } catch (Exception e) {
            System.err.println("Error loading profile view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void handleUsernameChange() {
        String newUsername = accountJFXController.getNewUsername();
        String currentUsername = model.getUsername();

        // Input validation
        if (newUsername == null || newUsername.trim().isEmpty()) {
            accountJFXController.showUsernameEmpty();
            return;
        }

        newUsername = newUsername.trim();

        if (newUsername.equals(currentUsername)) {
            accountJFXController.showUsernameSameAsCurrent();
            return;
        }

        // Additional validation rules (if needed)
        if (newUsername.length() < 3) {
            accountJFXController.showUsernameValidationError("Username must be at least 3 characters long");
            return;
        }

        if (newUsername.length() > 20) {
            accountJFXController.showUsernameValidationError("Username must be no more than 20 characters long");
            return;
        }

        if (!newUsername.matches("^[a-zA-Z0-9_]+$")) {
            accountJFXController.showUsernameValidationError("Username can only contain letters, numbers, and underscores");
            return;
        }

        // Show loading/processing state
        accountJFXController.showUsernameProcessing();

        // Send the change request to the server
        try {
            model.changeUsername(newUsername);
            System.out.println("Username change request sent: " + currentUsername + " -> " + newUsername);
        } catch (Exception e) {
            System.err.println("Error sending username change request: " + e.getMessage());
            accountJFXController.showUsernameUpdateFailed();
            accountJFXController.hideChangeUsername();
        }
    }

    private void showSuccessAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setPrefSize(300, 120);
        alert.showAndWait();
    }
}