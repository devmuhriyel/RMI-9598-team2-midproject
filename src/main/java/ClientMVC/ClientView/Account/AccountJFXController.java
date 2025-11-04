package ClientMVC.ClientView.Account;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

public class AccountJFXController {

    @FXML
    private AnchorPane titleBar;

    @FXML
    private Button minimizeButton, closeButton;

    @FXML
    private AnchorPane profileView, requestsView, friendsView;

    @FXML
    private VBox friendsListVBox;

    @FXML
    private AnchorPane changePasswordFrame, changeUsernameFrame;

    @FXML
    private Button editUsernameButton, editPasswordButton,
            saveUsernameButton, cancelUsernameButton,
            savePasswordButton, cancelPasswordButton;

    @FXML
    private TextField newUsernameTextField;

    @FXML
    private PasswordField oldPasswordTextField, newPasswordTextField, confirmPasswordTextField;

    @FXML
    private Label usernameStatusLabel, passwordStatusLabel;

    @FXML
    private Label usernameLabel, currentUsernameLabel, passwordLabel;

    @FXML
    private Button logoutAccountButton;

    @FXML
    private Button backButton;

    @FXML
    private Button profileAccountButton, requestsAccountButton, friendsAccountButton;

    @FXML
    private Button receivedRequestsButton, sentRequestButton;

    private Runnable backButtonListener;
    private Runnable LogoutAccountButtonListener;
    private Runnable usernameChangeListener;
    private Runnable passwordChangeListener;
    private Consumer<String> acceptRequestListener;
    private Consumer<String> declineRequestListener;
    private boolean showingSentRequests = false;
    private Consumer<Void> receivedRequestsListener;
    private Runnable sentRequestButtonListener;
    private Consumer<String> cancelSentRequestListener;
    private Consumer<String> friendClickListener;
    private Consumer<String> unfriendListener;
    private Runnable friendsButtonListener;
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    private VBox requestList;

    public void loadFriendRequests() {

        VBox foundVBox = findRequestList(requestsView);
        if (foundVBox != null) {
            requestList = foundVBox;
        } else {
            return;
        }

        requestList.getChildren().clear();
        String[] friendRequests = {"Alice Johnson", "Bob Smith", "Charlie Brown", "Steven Charz", "Joshua Flores"};

        for (String friendName : friendRequests) {
            AnchorPane requestFrame = createFriendRequestFrame(friendName);
            requestList.getChildren().add(requestFrame);
        }

    }

    private VBox findRequestList(javafx.scene.Parent parent) {
        if (parent == null) return null;

        for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof VBox && child.getId() != null && child.getId().equals("requestList")) {
                return (VBox) child;
            } else if (child instanceof javafx.scene.Parent) {
                VBox found = findRequestList((javafx.scene.Parent) child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private AnchorPane createFriendRequestFrame(String friendName) {
        AnchorPane friendRequestFrame = new AnchorPane();
        friendRequestFrame.setPrefHeight(65.0);
        friendRequestFrame.setPrefWidth(372.0);
        friendRequestFrame.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 10;");
        Label userLabel = new Label(friendName + " wants to be your friend");
        userLabel.setLayoutX(14.0);
        userLabel.setLayoutY(24.0);
        userLabel.setPrefHeight(17.0);
        userLabel.setPrefWidth(182.0);
        userLabel.setTextFill(javafx.scene.paint.Color.WHITE);
        userLabel.setFont(javafx.scene.text.Font.font("Gill Sans MT", 12.0));
        Button acceptButton = new Button("Confirm");
        acceptButton.setLayoutX(200.0);
        acceptButton.setLayoutY(20.0);
        acceptButton.setStyle("-fx-background-color: #8700b1; -fx-text-fill: white;");
        acceptButton.setOnAction(e -> handleAcceptRequest(friendName, friendRequestFrame));
        Button declineButton = new Button("Decline");
        declineButton.setLayoutX(270.0);
        declineButton.setLayoutY(20.0);
        declineButton.setStyle("-fx-background-color: #525252; -fx-text-fill: white;");
        declineButton.setOnAction(e -> handleDeclineRequest(friendName, friendRequestFrame));
        friendRequestFrame.getChildren().addAll(userLabel, acceptButton, declineButton);
        return friendRequestFrame;
    }

    public void setFriendClickListener(Consumer<String> friendClickListener) {
        this.friendClickListener = friendClickListener;
    }

    public void setFriendsButtonListener(Runnable listener) {
        this.friendsButtonListener = listener;
    }

    private void handleAcceptRequest(String friendName, AnchorPane requestFrame) {
        requestList.getChildren().remove(requestFrame);

        // Notify controller (you'll need to add this callback)
        if (acceptRequestListener != null) {
            acceptRequestListener.accept(friendName);
        }
    }

    private void handleDeclineRequest(String friendName, AnchorPane requestFrame) {
        requestList.getChildren().remove(requestFrame);
        if (declineRequestListener != null) {
            declineRequestListener.accept(friendName);
        }
    }

    @FXML
    public void initialize() {
        setupWindowControls();
        setUpButtons();
    }

    private void setUpButtons() {
        if (receivedRequestsButton != null) {
            receivedRequestsButton.setOnAction(event -> {
                showingSentRequests = false;
                receivedRequestsButton.setStyle("-fx-background-color: #8700b1;"); // Active style
                if (sentRequestButton != null) {
                    sentRequestButton.setStyle("-fx-background-color: #525252;"); // Inactive style
                }

                if (receivedRequestsListener != null) {
                    receivedRequestsListener.accept(null);
                }
            });
        }
        if (sentRequestButton != null) {
            sentRequestButton.setOnAction(event -> {
                showingSentRequests = true;
                sentRequestButton.setStyle("-fx-background-color: #8700b1;"); // Active style
                if (receivedRequestsButton != null) {
                    receivedRequestsButton.setStyle("-fx-background-color: #525252;"); // Inactive style
                }
                if (sentRequestButtonListener != null) {
                    sentRequestButtonListener.run();
                }
            });
        }
        if (friendsAccountButton != null) {
            friendsAccountButton.setOnAction(event -> {
                showFriends(); // Navigates to the view
                if (friendsButtonListener != null) {
                    friendsButtonListener.run();
                }
            });
        }
        if (backButton != null) {
            backButton.setOnAction(event -> {
                if (backButtonListener != null) {
                    backButtonListener.run();
                }
            });
        }
        if (logoutAccountButton != null) {
            logoutAccountButton.setOnAction(event -> {
                if (LogoutAccountButtonListener != null) {
                    LogoutAccountButtonListener.run();
                }
            });
        }
        if (saveUsernameButton != null) {
            saveUsernameButton.setOnAction(event -> {
                saveUsername();
            });
        } else {
        }

        if (savePasswordButton != null) {
            savePasswordButton.setOnAction(event -> {
                savePassword();
            });
        } else {
        }
        if (cancelPasswordButton != null) {
            cancelPasswordButton.setOnAction(event -> {
                clearPasswordFields();
                hideChangePassword();
                clearPasswordStatus();
            });
        }
    }

    private void setupWindowControls() {
        if (titleBar != null) {
            titleBar.setOnMousePressed(event -> {
                Stage stage = (Stage) titleBar.getScene().getWindow();
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            titleBar.setOnMouseDragged(event -> {
                Stage stage = (Stage) titleBar.getScene().getWindow();
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });
        }

        if (closeButton != null) {
            closeButton.setOnMouseClicked(event -> {
                ((Stage) closeButton.getScene().getWindow()).close();
            });
        }

        if (minimizeButton != null) {
            minimizeButton.setOnMouseClicked(event -> {
                ((Stage) minimizeButton.getScene().getWindow()).setIconified(true);
            });
        }
    }

    @FXML
    public void showProfile() {
        hideAllViews();
        if (profileView != null) {
            profileView.setVisible(true);
        }
    }

    @FXML
    public void showRequests() {
        hideAllViews();
        if (requestsView != null) {
            requestsView.setVisible(true);
            showingSentRequests = false;
            if (receivedRequestsButton != null) {
                receivedRequestsButton.setStyle("-fx-background-color: #8700b1;"); // Active
            }
            if (sentRequestButton != null) {
                sentRequestButton.setStyle("-fx-background-color: #525252;"); // Inactive
            }

            // Load received requests by default
            javafx.application.Platform.runLater(() -> {
                if (receivedRequestsListener != null) {
                    receivedRequestsListener.accept(null);
                }
            });
        }
    }

    @FXML
    public void showFriends() {
        hideAllViews();
        if (friendsView != null) {
            friendsView.setVisible(true);
        }
    }

    private void hideAllViews() {
        if (profileView != null) profileView.setVisible(false);
        if (requestsView != null) requestsView.setVisible(false);
        if (friendsView != null) friendsView.setVisible(false);
    }

    @FXML
    public void showChangePassword() {
        if (changePasswordFrame != null) {
            changePasswordFrame.setVisible(true);
        }
    }

    @FXML
    public void showChangeUsername() {
        if (changeUsernameFrame != null) {
            changeUsernameFrame.setVisible(true);
        }
    }

    @FXML
    public void saveUsername() {
        String newUsername = getNewUsername();
        if (newUsername == null || newUsername.trim().isEmpty()) {
            showUsernameEmpty();
            return;
        }
        if (usernameChangeListener != null) {
            usernameChangeListener.run();
        } else {
            tryDirectUsernameChange(newUsername.trim());
        }
    }

    private void tryDirectUsernameChange(String newUsername) {
        showUsernameUpdated();
        clearUsernameField();
        hideChangeUsername();
    }

    @FXML
    public void savePassword() {
        String oldPassword = getOldPassword();
        String newPassword = getNewPassword();
        String confirmPassword = getConfirmPassword();


        // Validation
        if (oldPassword == null || oldPassword.trim().isEmpty() ||
                newPassword == null || newPassword.trim().isEmpty() ||
                confirmPassword == null || confirmPassword.trim().isEmpty()) {
            showPasswordEmptyFields();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showPasswordMismatch();
            return;
        }

        if (oldPassword.equals(newPassword)) {
            showPasswordSameAsOld();
            return;
        }

        if (passwordChangeListener != null) {
            passwordChangeListener.run();
        } else {
            showPasswordUpdateFailed();
        }
    }

    @FXML
    public void hideChangePassword() {
        if (changePasswordFrame != null) {
            changePasswordFrame.setVisible(false);
        }
    }

    public void showUsernameUpdated() {
        hideUsernameProcessing();
        updateStatus(usernameStatusLabel, "Username updated successfully!", Color.rgb(0, 245, 83), false);
        if (usernameStatusLabel != null) {
            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), usernameStatusLabel);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        }
    }

    public void showUsernameEmpty() {
        updateStatus(usernameStatusLabel, "Username cannot be empty!", Color.rgb(255, 165, 0), true);
    }

    public void showUsernameUpdateFailed() {
        hideUsernameProcessing();
        updateStatus(usernameStatusLabel, "Username update failed! Please try again.", Color.rgb(217, 27, 36), true);
    }

    // PASSWORD FEEDBACK
    public void showPasswordChanged() {
        updateStatus(passwordStatusLabel, "Password changed!", Color.rgb(0, 245, 83), false);
    }

    public void showPasswordEmptyFields() {
        updateStatus(passwordStatusLabel, "All fields are required!", Color.rgb(255, 165, 0), true);
    }

    public void showPasswordMismatch() {
        updateStatus(passwordStatusLabel, "Passwords do not match!", Color.rgb(217, 27, 36), true);
    }

    public void showPasswordSameAsOld() {
        updateStatus(passwordStatusLabel, "New password must differ from old one!", Color.rgb(217, 27, 36), true);
    }

    public void showPasswordUpdateFailed() {
        updateStatus(passwordStatusLabel, "Password update failed!", Color.rgb(217, 27, 36), true);
    }

    // All the status and UI methods stay the same:
    public void showUsernameAlreadyExists() {
        updateStatus(usernameStatusLabel, "Username already exists!", javafx.scene.paint.Color.rgb(217, 27, 36), true);
    }

    public void showUsernameSameAsCurrent() {
        updateStatus(usernameStatusLabel, "New username must be different!", javafx.scene.paint.Color.rgb(217, 27, 36), true);
    }

    public void showInvalidOldPassword() {
        updateStatus(passwordStatusLabel, "Current password is incorrect!", javafx.scene.paint.Color.rgb(217, 27, 36), true);
    }

    private void updateStatus(Label label, String message, Color color, boolean shake) {
        if (label == null) return;

        label.setText(message);
        label.setTextFill(color);
        label.setStyle("-fx-font-size: 12px;");

        if (shake) {
            TranslateTransition animation = new TranslateTransition(Duration.millis(50), label);
            animation.setFromX(0);
            animation.setByX(10);
            animation.setCycleCount(4);
            animation.setAutoReverse(true);
            animation.play();
        }
    }

    public String getNewUsername() {
        String value = (newUsernameTextField != null) ? newUsernameTextField.getText() : null;
        return value;
    }

    public String getOldPassword() {
        return (oldPasswordTextField != null) ? oldPasswordTextField.getText() : "";
    }

    public String getNewPassword() {
        return (newPasswordTextField != null) ? newPasswordTextField.getText() : "";
    }

    public String getConfirmPassword() {
        return (confirmPasswordTextField != null) ? confirmPasswordTextField.getText() : "";
    }

    public void setBackButtonListener(Runnable listener) {
        this.backButtonListener = listener;
    }

    public void setLogoutAccountButtonListener(Runnable listener) {
        this.LogoutAccountButtonListener = listener;
    }

    public void clearUsernameField() {
        if (newUsernameTextField != null) {
            newUsernameTextField.clear();
        }
    }

    public void clearPasswordFields() {
        if (oldPasswordTextField != null) {
            oldPasswordTextField.clear();
        }
        if (newPasswordTextField != null) {
            newPasswordTextField.clear();
        }
        if (confirmPasswordTextField != null) {
            confirmPasswordTextField.clear();
        }
    }

    private void clearUsernameStatus() {
        if (usernameStatusLabel != null) {
            usernameStatusLabel.setText("");
        }
    }

    private void clearPasswordStatus() {
        if (passwordStatusLabel != null) {
            passwordStatusLabel.setText("");
        }
    }

    public void setUsernameChangeListener(Runnable listener) {
        this.usernameChangeListener = listener;
    }

    public void setPasswordChangeListener(Runnable listener) {
        this.passwordChangeListener = listener;
    }

    public void setReceivedRequestsListener(Consumer<Void> listener) {
        this.receivedRequestsListener = listener;
    }

    public void setSentRequestButtonListener(Runnable listener) {
        this.sentRequestButtonListener = listener;
    }

    public void updateDisplayedUsername(String newUsername) {
        if (usernameLabel != null) {
            usernameLabel.setText(newUsername);
        }
        if (currentUsernameLabel != null) {
            currentUsernameLabel.setText(newUsername);
        }
    }

    public void renderFriendsList(List<String> friends) {
        Platform.runLater(() -> {
            if (friendsListVBox == null) {
                System.err.println("FATAL ERROR: friendsListVBox is null! Check FXML fx:id and controller initialization.");
                return;
            }

            friendsListVBox.getChildren().clear();

            if (friends == null || friends.isEmpty()) {
                javafx.scene.control.Label emptyLabel = new javafx.scene.control.Label("You have no friends yet.");
                emptyLabel.setTextFill(javafx.scene.paint.Color.WHITE);
                emptyLabel.setStyle("-fx-font-size: 14px; -fx-padding: 20px;");
                friendsListVBox.getChildren().add(emptyLabel);
                return;
            }

            for (String friend : friends) {
                // 1. Create Friend Name Label
                javafx.scene.layout.HBox nameContainer = new javafx.scene.layout.HBox();
                javafx.scene.control.Label friendLabel = new javafx.scene.control.Label("👤 " + friend);
                friendLabel.setPrefHeight(30.0);
                friendLabel.setPrefWidth(200.0); // Allocate width for the name
                friendLabel.setTextFill(javafx.scene.paint.Color.WHITE);
                friendLabel.setStyle("-fx-padding: 0 10 0 0;");
                nameContainer.getChildren().add(friendLabel);
                nameContainer.setAlignment(Pos.CENTER_LEFT);

                // 2. Create Unfriend Button
                javafx.scene.control.Button unfriendBtn = new javafx.scene.control.Button("Unfriend");
                unfriendBtn.setStyle("-fx-background-color: #b32626; -fx-text-fill: white; -fx-background-radius: 5;");

                // 3. Attach Unfriend Action
                final String friendName = friend;
                unfriendBtn.setOnAction(e -> {
                    if (unfriendListener != null) {
                        unfriendListener.accept(friendName);
                    }
                });

                // 4. Create Horizontal Container (HBox)
                javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(15); // 15px spacing
                hbox.setMaxWidth(Double.MAX_VALUE);
                hbox.setPrefHeight(40.0);
                hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                hbox.setStyle("-fx-background-color: #3e3e3e; -fx-background-radius: 5; -fx-padding: 5 15 5 15;");

                // Add Label and Button to HBox
                hbox.getChildren().addAll(nameContainer, unfriendBtn);

                friendsListVBox.getChildren().add(hbox);
            }
        });
    }

    public void setUnfriendListener(Consumer<String> listener) {
        this.unfriendListener = listener;
    }

    public void renderRequestList(List<String> requests, boolean showingSentRequests) {
        Platform.runLater(() -> {
            requestList.getChildren().clear();

            if (requests == null || requests.isEmpty()) {
                String emptyMessage = showingSentRequests ? "No sent requests" : "No friend requests";
                javafx.scene.control.Label emptyLabel = new javafx.scene.control.Label(emptyMessage);
                emptyLabel.setTextFill(javafx.scene.paint.Color.WHITE);
                emptyLabel.setStyle("-fx-font-size: 14px; -fx-padding: 20px;");
                requestList.getChildren().add(emptyLabel);
                return;
            }

            for (String friendName : requests) {
                javafx.scene.layout.AnchorPane requestFrame;

                if (showingSentRequests) {
                    requestFrame = createSentRequestFrame(friendName);
                } else {
                    requestFrame = createReceivedRequestFrame(friendName);
                }

                requestList.getChildren().add(requestFrame);
            }
        });
    }

    private javafx.scene.layout.AnchorPane createSentRequestFrame(String friendName) {
        javafx.scene.layout.AnchorPane requestFrame = new javafx.scene.layout.AnchorPane();
        requestFrame.setPrefHeight(65.0);
        requestFrame.setPrefWidth(372.0);
        requestFrame.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 5, 0, 0, 2);");

        javafx.scene.control.Label userLabel = new javafx.scene.control.Label("Request sent to " + friendName);
        userLabel.setLayoutX(14.0);
        userLabel.setLayoutY(24.0);
        userLabel.setTextFill(javafx.scene.paint.Color.WHITE);
        userLabel.setFont(javafx.scene.text.Font.font("Gill Sans MT", 12.0));
        userLabel.setPrefWidth(200.0);

        javafx.scene.control.Label statusLabel = new javafx.scene.control.Label("Pending");
        statusLabel.setLayoutX(220.0);
        statusLabel.setLayoutY(24.0);
        statusLabel.setTextFill(javafx.scene.paint.Color.ORANGE);
        statusLabel.setFont(javafx.scene.text.Font.font("Gill Sans MT", 10.0));

        javafx.scene.control.Button cancelButton = new javafx.scene.control.Button("Cancel");
        cancelButton.setLayoutX(290.0);
        cancelButton.setLayoutY(20.0);
        cancelButton.setStyle("-fx-background-color: #b32626; -fx-text-fill: white; -fx-background-radius: 5;");
        cancelButton.setPrefWidth(60.0);
        cancelButton.setPrefHeight(25.0);

        cancelButton.setOnAction(e -> {
            requestList.getChildren().remove(requestFrame);
            if (cancelSentRequestListener != null) {
                cancelSentRequestListener.accept(friendName);
            }
        });

        requestFrame.getChildren().addAll(userLabel, statusLabel, cancelButton);
        return requestFrame;
    }

    private javafx.scene.layout.AnchorPane createReceivedRequestFrame(String friendName) {
        javafx.scene.layout.AnchorPane requestFrame = new javafx.scene.layout.AnchorPane();
        requestFrame.setPrefHeight(65.0);
        requestFrame.setPrefWidth(372.0);
        requestFrame.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 5, 0, 0, 2);");

        javafx.scene.control.Label userLabel = new javafx.scene.control.Label(friendName + " wants to be your friend");
        userLabel.setLayoutX(14.0);
        userLabel.setLayoutY(24.0);
        userLabel.setTextFill(javafx.scene.paint.Color.WHITE);
        userLabel.setFont(javafx.scene.text.Font.font("Gill Sans MT", 12.0));
        userLabel.setPrefWidth(180.0);

        javafx.scene.control.Button acceptButton = new javafx.scene.control.Button("Accept");
        acceptButton.setLayoutX(200.0);
        acceptButton.setLayoutY(20.0);
        acceptButton.setStyle("-fx-background-color: #8700b1; -fx-text-fill: white; -fx-background-radius: 5;");
        acceptButton.setPrefWidth(60.0);
        acceptButton.setPrefHeight(25.0);

        acceptButton.setOnAction(e -> {

            requestList.getChildren().remove(requestFrame);

            if (acceptRequestListener != null) {
                acceptRequestListener.accept(friendName);
            }
        });

        // Decline button
        javafx.scene.control.Button declineButton = new javafx.scene.control.Button("Decline");
        declineButton.setLayoutX(270.0);
        declineButton.setLayoutY(20.0);
        declineButton.setStyle("-fx-background-color: #525252; -fx-text-fill: white; -fx-background-radius: 5;");
        declineButton.setPrefWidth(60.0);
        declineButton.setPrefHeight(25.0);

        declineButton.setOnAction(e -> {
            requestList.getChildren().remove(requestFrame);
            if (declineRequestListener != null) {
                declineRequestListener.accept(friendName);
            }
        });

        requestFrame.getChildren().addAll(userLabel, acceptButton, declineButton);
        return requestFrame;
    }

    public void showUsernameValidationError(String message) {
        updateStatus(usernameStatusLabel, message, javafx.scene.paint.Color.rgb(255, 165, 0), true);
    }

    public void showUsernameProcessing() {
        updateStatus(usernameStatusLabel, "Processing username change...", javafx.scene.paint.Color.rgb(0, 123, 255), false);

        if (saveUsernameButton != null) {
            saveUsernameButton.setDisable(true);
            saveUsernameButton.setText("Processing...");
        }
    }

    public void hideUsernameProcessing() {
        if (saveUsernameButton != null) {
            saveUsernameButton.setDisable(false);
            saveUsernameButton.setText("Save");
        }
    }

    @FXML
    public void hideChangeUsername() {
        hideUsernameProcessing();
        if (changeUsernameFrame != null) {
            changeUsernameFrame.setVisible(false);
        }
        clearUsernameStatus();
    }

    public void setAcceptRequestListener(Consumer<String> listener) {
        this.acceptRequestListener = listener;
    }

    public void setDeclineRequestListener(Consumer<String> listener) {
        this.declineRequestListener = listener;
    }

    public void setCancelSentRequestListener(Consumer<String> listener) {
        this.cancelSentRequestListener = listener;
    }
    public Consumer<Void> getReceivedRequestsListener() {
        return receivedRequestsListener;
    }
    public Runnable getFriendsButtonListener() {
        return friendsButtonListener;
    }

    private void showSuccessAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setPrefSize(300, 120);
        alert.showAndWait();
    }


}