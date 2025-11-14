package ClientMVC.ClientController;

import ClientMVC.ClientCallbackImplementation;
import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Account.AccountView;
import ClientMVC.ClientView.Account.AccountJFXController;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.ClientView.Login.LoginView;
import Common.Objects.ClientObjects.FriendRequest;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Optional;

public class AccountController {
    private final UnifiedClientModel model;
    private final AccountView view;
    private final AccountJFXController accountJFXController;
    private final ClientCallbackImplementation clientCallback;
    public HomeController homeController;

    public AccountController(UnifiedClientModel model, AccountView view, AccountJFXController accountJFXController, ClientCallbackImplementation callback) {
        this.view = view;
        this.model = model;
        this.accountJFXController = accountJFXController;
        this.clientCallback = callback;
        callback.setClientAccountController(this);
        initialize();
    }

    Gson gson = new Gson();

    private void initialize() {
        accountJFXController.updateDisplayedUsername(model.getUsername());
        accountJFXController.setFriendsButtonListener(() -> {
            try {
                String jsonString = String.format("{\"username\":\"%s\"}", model.getUsername());
                model.getFriendsList(jsonString);
                homeController.refreshUserData();
            } catch (RemoteException e) {
                System.err.println("Error requesting friends list: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
        accountJFXController.setUnfriendListener(this::handleUnfriendAction);
        accountJFXController.setSentRequestButtonListener(() -> {
            try {
                String jsonString = String.format("{\"username\":\"%s\"}", model.getUsername());
                model.getSentFriendRequests(jsonString);
                homeController.refreshUserData();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });

        accountJFXController.setCancelSentRequestListener(friendName -> {
            try {
                String jsonString = gson.toJson(new FriendRequest(model.getUsername(), friendName, "canceled"));
                model.cancelFriendRequest(jsonString);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });
        accountJFXController.setAcceptRequestListener(friendName -> {
            try {
                // FIX: Construct JSON object with both requester (friendName) and responder (current user)
                String jsonString = String.format(
                        "{\"requester\":\"%s\", \"responder\":\"%s\"}",
                        friendName,
                        model.getUsername()
                );

                System.out.println("DEBUG: Sending Accept Request JSON: " + jsonString);

                // Pass the JSON string to the model/server
                model.acceptFriendRequest(jsonString);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });
        accountJFXController.setDeclineRequestListener(friendName -> {
            try {
                // FIX: Construct JSON object with both requester (friendName) and responder (current user)
                String jsonString = String.format(
                        "{\"requester\":\"%s\", \"responder\":\"%s\"}",
                        friendName,
                        model.getUsername()
                );

                System.out.println("DEBUG: Sending Decline Request JSON: " + jsonString);

                // Pass the JSON string to the model/server
                model.rejectFriendRequest(jsonString);
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
            try {
                String username = model.getUsername();
                if (username != null && !username.isEmpty()) {
                    String jsonString = "{\"action\":\"logout\", \"username\":\"" + username + "\"}";
                    model.logOut(username, jsonString);
                    clientCallback.onSuccessfulLogout();
                }

                view.getStage().hide();
                Stage loginStage = ClientMVC.ClientView.Login.LoginView.getPrimaryStage();
                if (loginStage != null) {
                    loginStage.show();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });


        setupCallbackHandlers();
        setupUsernameChange();
        setupPasswordChange();
        setupNavigationButtons();

        accountJFXController.setUsernameChangeListener(this::handleUsernameChange);
        try {
            accountJFXController.showProfile();
        } catch (Exception e) {
            System.err.println("Error loading profile view: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // ==================== CALLBACK HANDLERS (Callbacks → Controller) ====================
    private void setupCallbackHandlers() {

        clientCallback.setOnUsernameChangeSuccess(() -> {
            Platform.runLater(() -> {
                handleUsernameChangeSuccess();
            });
        });
        clientCallback.setOnUsernameChangeFailed(() -> {
            Platform.runLater(() -> {
                accountJFXController.showUsernameUpdateFailed();
                accountJFXController.hideChangeUsername();
            });
        });
        clientCallback.setOnPasswordChangeSuccess(() -> {
            Platform.runLater(() -> {
                accountJFXController.showPasswordChanged();
                accountJFXController.clearPasswordFields();
                accountJFXController.hideChangePassword();
                showSuccessAlert("Password Changed",
                        "Your password has been successfully changed!");
            });
        });
        clientCallback.setOnPasswordChangeFailed(() -> {
            Platform.runLater(() -> {
                accountJFXController.showInvalidOldPassword();
            });
        });

        accountJFXController.setReceivedRequestsListener(aVoid -> {
            try {
                String jsonString = String.format("{\"username\":\"%s\"}", model.getUsername());
                model.getReceivedFriendRequests(jsonString); // Calls the RMI server method
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });

    }

    // ==================== USERNAME CHANGE ====================
    private void setupUsernameChange() {
        accountJFXController.setUsernameChangeListener(this::handleUsernameChange);
    }

    public void handleUsernameChange() {
        String newUsername = accountJFXController.getNewUsername();
        String currentUsername = model.getUsername();
        if (newUsername == null || newUsername.trim().isEmpty()) {
            accountJFXController.showUsernameEmpty();
            return;
        }

        newUsername = newUsername.trim();

        if (newUsername.equals(currentUsername)) {
            accountJFXController.showUsernameSameAsCurrent();
            return;
        }
        if (newUsername.length() < 3) {
            accountJFXController.showUsernameValidationError(
                    "Username must be at least 3 characters long");
            return;
        }
        if (newUsername.length() > 20) {
            accountJFXController.showUsernameValidationError(
                    "Username must be no more than 20 characters long");
            return;
        }
        if (!newUsername.matches("^[a-zA-Z0-9_]+$")) {
            accountJFXController.showUsernameValidationError(
                    "Username can only contain letters, numbers, and underscores");
            return;
        }
        accountJFXController.showUsernameProcessing();
        try {
            model.changeUsername(newUsername);
        } catch (RemoteException e) {
            System.err.println("✗ Error sending username change request: " + e.getMessage());
            e.printStackTrace();
            accountJFXController.showUsernameUpdateFailed();
            accountJFXController.hideChangeUsername();
        }
    }

    private void handleUsernameChangeSuccess() {
        try {
            String newUsername = accountJFXController.getNewUsername().trim();
            model.setUsername(newUsername);
            accountJFXController.updateDisplayedUsername(newUsername);
            accountJFXController.showUsernameUpdated();
            accountJFXController.clearUsernameField();
            accountJFXController.hideChangeUsername();
            showSuccessAlert("Username Changed",
                    "Your username has been successfully changed to: " + newUsername);

        } catch (Exception e) {
            System.err.println("Error handling username change success: " + e.getMessage());
            e.printStackTrace();
            accountJFXController.showUsernameUpdated();
            accountJFXController.hideChangeUsername();
        }
    }

    // ==================== PASSWORD CHANGE ====================
    private void setupPasswordChange() {
        accountJFXController.setPasswordChangeListener(this::handlePasswordChange);
    }

    public void handlePasswordChange() {
        String oldPassword = accountJFXController.getOldPassword();
        String newPassword = accountJFXController.getNewPassword();
        String confirmPassword = accountJFXController.getConfirmPassword();
        if (oldPassword == null || oldPassword.trim().isEmpty() ||
                newPassword == null || newPassword.trim().isEmpty() ||
                confirmPassword == null || confirmPassword.trim().isEmpty()) {
            accountJFXController.showPasswordEmptyFields();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            accountJFXController.showPasswordMismatch();
            return;
        }

        if (oldPassword.equals(newPassword)) {
            accountJFXController.showPasswordSameAsOld();
            return;
        }
        if (newPassword.length() < 6) {
            accountJFXController.showPasswordUpdateFailed();
            return;
        }
        try {
            model.changePasswordWithOldPassword(oldPassword, newPassword);
        } catch (RemoteException e) {
            System.err.println("✗ Error sending password change request: " + e.getMessage());
            e.printStackTrace();
            accountJFXController.showPasswordUpdateFailed();
        }
    }

    // ==================== NAVIGATION BUTTONS ====================
    private void setupNavigationButtons() {
        accountJFXController.setBackButtonListener(() -> {

            view.getStage().close();
            HomeView homeView = new HomeView();
            Stage homeStage = new Stage();
            homeView.start(homeStage);
            new HomeController(model, homeView, homeView.getFXController(), clientCallback);
        });

        accountJFXController.setDeclineRequestListener(friendName -> {
            try {
                model.rejectFriendRequest(friendName);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    // ==================== UTILITY METHODS ====================
    public void showSuccessAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION
        );
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setPrefSize(300, 120);
        alert.showAndWait();
    }

    // ==================== FRIEND LIST & REQUESTS ====================
    public void handleUnfriendAction(String friendToRemove) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Unfriend");
            alert.setHeaderText("Are you sure you want to unfriend " + friendToRemove + "?");
            alert.setContentText("This action cannot be undone.");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    String jsonString = String.format(
                            "{\"requester\":\"%s\",\"responder\":\"%s\"}",
                            model.getUsername(),
                            friendToRemove
                    );
                    model.removeFromFriendsList(jsonString);
                    showSuccessAlert("Unfriended", friendToRemove + " has been removed from your friends list.");
                    accountJFXController.showFriends();
                } catch (RemoteException e) {
                    System.err.println("Error sending unfriend request: " + e.getMessage());
                    e.printStackTrace();
                    showSuccessAlert("Error", "Failed to remove friend. Please try again.");
                }
            } else {
            }
        });
    }

    public void handleFriendDeletionSuccess() {
        showSuccessAlert("Unfriended Successfully", "The user has been successfully removed from your friends list.");
        accountJFXController.showFriends();
        Runnable fetchFriendsListener = accountJFXController.getFriendsButtonListener();
        if (fetchFriendsListener != null) {
            fetchFriendsListener.run();
        } else {
            System.err.println("Friends button listener is null, cannot refresh list.");
        }
    }

    public void handleFriendsListUpdate(List<String> friends) {
        Platform.runLater(() -> {
            accountJFXController.renderFriendsList(friends);
            homeController.refreshUserData();
        });
    }

    public void handleFriendAcceptedSuccess() {
        showSuccessAlert("Friend Request Accepted", "You are now friends with the requester!");
        if (accountJFXController.getReceivedRequestsListener() != null) {
            System.out.println("Controller: Re-fetching received requests list after acceptance.");
            accountJFXController.getReceivedRequestsListener().accept(null);
            homeController.refreshUserData();
        }
        try {
            String jsonString = String.format("{\"username\":\"%s\"}", model.getUsername());
            System.out.println("Controller: Re-fetching Friends List after acceptance.");
            model.getFriendsList(jsonString);
        } catch (RemoteException e) {
            System.err.println("Error requesting friends list after acceptance: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void handleFriendDeclinedNotification() {
        showSuccessAlert("Request Declined", "A friend request you sent was declined.");
        try {
            String jsonString = String.format("{\"username\":\"%s\"}", model.getUsername());
            System.out.println("Controller: Re-fetching Sent Requests List after decline notification.");
            model.getSentFriendRequests(jsonString);
        } catch (RemoteException e) {
            System.err.println("Error requesting sent requests list after decline: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void handleNewFriendRequestReceived() {
        System.out.println("Controller: Handling new friend request callback.");
        Platform.runLater(() -> {
            showSuccessAlert("New Friend Request", "You have received a new friend request!");
        });
        accountJFXController.getReceivedRequestsListener().accept(null);
    }

    public void handleReceivedFriendRequests(List<String> receivedRequests) {
        Platform.runLater(() -> {
            accountJFXController.renderRequestList(receivedRequests, false); // false for received
        });
    }

    public void handleSentFriendRequests(List<String> sentRequests) {
        Platform.runLater(() -> {
            accountJFXController.renderRequestList(sentRequests, true); // true for sent
        });
    }

    public String getNewUsernameFromView() {
        return accountJFXController.getNewUsername().trim();
    }
}