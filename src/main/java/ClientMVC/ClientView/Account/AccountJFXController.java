package ClientMVC.ClientView.Account;

import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class AccountJFXController {

    @FXML
    private AnchorPane rightFrame, titleBar;

    @FXML
    private Button minimizeButton, closeButton;

    // Profile frame switching components
    @FXML
    private AnchorPane changePasswordFrame, changeUsernameFrame;

    @FXML
    private Button editUsernameButton, editPasswordButton,
            saveUsernameButton, cancelUsernameButton,
            savePasswordButton, cancelPasswordButton;

    @FXML
    private TextField newUsernameTextField, oldPasswordTextField,
            newPasswordTextField, confirmPasswordTextField;

    @FXML
    private Label usernameStatusLabel, passwordStatusLabel;

    @FXML
    private Button logoutAccountButton;

    @FXML
    private Button backButton;


    // Runnables
    private Runnable backButtonListener;
    private Runnable LogoutAccountButtonListener;

    private Runnable usernameChangeListener;
    private Runnable passwordChangeListener;

    // Cache for loaded content
    private Parent profileView;
    private Parent requestsView;
    private Parent settingsView;

    // Top Section for Draggable
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    private VBox requestList;

    // Update your loadFriendRequests method:
    public void loadFriendRequests() {
        System.out.println("loadFriendRequests() called");

        // Find the VBox by searching through the current view
        VBox foundVBox = findRequestList(rightFrame);
        if (foundVBox != null) {
            requestList = foundVBox;
            System.out.println("Found requestList manually!");
        } else {
            System.out.println("Could not find requestList VBox anywhere!");
            return;
        }
        // Clear and populate as before...
        // Sample Data
        requestList.getChildren().clear();
        String[] friendRequests = {"Alice Johnson", "Bob Smith", "Charlie Brown", "Steven Charz", "Joshua Flores"};

        for (String friendName : friendRequests) {
            AnchorPane requestFrame = createFriendRequestFrame(friendName);
            requestList.getChildren().add(requestFrame);
        }

        System.out.println("Total children in requestList: " + requestList.getChildren().size());
    }

    // Helper method to find the VBox manually
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

    // Method to create individual friend request frame
    private AnchorPane createFriendRequestFrame(String friendName) {
        System.out.println("Creating request frame for: " + friendName);

        // Create the main container (same as your FXML design)
        AnchorPane friendRequestFrame = new AnchorPane();
        friendRequestFrame.setPrefHeight(65.0);
        friendRequestFrame.setPrefWidth(372.0);
        friendRequestFrame.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 10;");

        // Create the username label
        Label userLabel = new Label(friendName + " wants to be your friend");
        userLabel.setLayoutX(14.0);
        userLabel.setLayoutY(24.0);
        userLabel.setPrefHeight(17.0);
        userLabel.setPrefWidth(182.0);
        userLabel.setTextFill(javafx.scene.paint.Color.WHITE);
        userLabel.setFont(javafx.scene.text.Font.font("Gill Sans MT", 12.0));

        // Create Accept button (same styling as your FXML)
        Button acceptButton = new Button("Confirm");
        acceptButton.setLayoutX(200.0);
        acceptButton.setLayoutY(20.0);
        acceptButton.setStyle("-fx-background-color: #8700b1; -fx-text-fill: white;");

        // Set accept button action
        acceptButton.setOnAction(e -> handleAcceptRequest(friendName, friendRequestFrame));

        // Create Decline button (same styling as your FXML)
        Button declineButton = new Button("Decline");
        declineButton.setLayoutX(270.0);
        declineButton.setLayoutY(20.0);
        declineButton.setStyle("-fx-background-color: #525252; -fx-text-fill: white;");

        // Set decline button action
        declineButton.setOnAction(e -> handleDeclineRequest(friendName, friendRequestFrame));

        // Add all components to the frame
        friendRequestFrame.getChildren().addAll(userLabel, acceptButton, declineButton);

        System.out.println("Request frame created successfully");
        return friendRequestFrame;
    }

    // Handle accept request
    private void handleAcceptRequest(String friendName, AnchorPane requestFrame) {
        System.out.println("Accepting friend request from: " + friendName);

        // TODO: Send accept request to server

        // Remove the request frame from UI
        requestList.getChildren().remove(requestFrame);

        // Show success message (you can use your existing status methods)
        // showFriendRequestAccepted(friendName);
    }

    // Handle decline request
    private void handleDeclineRequest(String friendName, AnchorPane requestFrame) {
        System.out.println("Declining friend request from: " + friendName);

        // TODO: Send decline request to server

        // Remove the request frame from UI
        requestList.getChildren().remove(requestFrame);

        // Show message (you can use your existing status methods)
        // showFriendRequestDeclined(friendName);
    }

    @FXML
    public void initialize() {
        // Be applied to all upcoming FXML sub-frames
        setupWindowControls();
        setUpButtons();
    }

    private void setUpButtons() {

        // Only set up backButton if it exists (main view only)
        // Only set up backButton if it exists (main view only)
        if (backButton != null) {
            backButton.setOnAction(event -> {
                if (backButtonListener != null) {
                    backButtonListener.run();
                }
            });
        }

        // Only set up logoutButton if it exists (main view only)
        if (logoutAccountButton != null) {
            logoutAccountButton.setOnAction(event -> {
                if (LogoutAccountButtonListener != null) {
                    LogoutAccountButtonListener.run();
                }
            });
        }

        // Set up save buttons programmatically
        if (saveUsernameButton != null) {
            System.out.println("Setting up saveUsernameButton to use listener");
            saveUsernameButton.setOnAction(event -> {
                System.out.println("*** Save username button clicked! ***");
                if (usernameChangeListener != null) {
                    System.out.println("Using username change listener");
                    usernameChangeListener.run();
                } else {
                    System.out.println("Username listener is null, calling saveUsername() directly");
                    saveUsername();
                }
            });
        } else {
            System.out.println("ERROR: saveUsernameButton is null!");
        }

        // FIXED: Set up password save button to use listener
        if (savePasswordButton != null) {
            System.out.println("Setting up savePasswordButton to use listener");
            savePasswordButton.setOnAction(event -> {
                System.out.println("*** Save password button clicked! ***");
                if (passwordChangeListener != null) {
                    System.out.println("Using password change listener");
                    passwordChangeListener.run();
                } else {
                    System.out.println("Password listener is null, calling savePassword() directly");
                    savePassword();
                }
            });
        } else {
            System.out.println("ERROR: savePasswordButton is null!");
        }

        // Set up cancel buttons
        if (cancelPasswordButton != null) {
            cancelPasswordButton.setOnAction(event -> {
                clearPasswordFields();
                hideChangePassword();
                clearPasswordStatus();
            });
        }
    }

        private void setupWindowControls() {
        // Only set up window controls if titleBar exists (main view only)
        if (titleBar != null) {
            // Drag the window when mouse is pressed and dragged
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

        // Frameless Buttons - only if they exist
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
    public void showProfile() throws IOException {
        if (profileView == null) {
            profileView = FXMLLoader.load(getClass().getResource("/FXMLFiles/AccountView/ClientAccountProfile.fxml"));
            setupViewAnchors(profileView);
        }
        switchToView(profileView);
    }

    @FXML
    private void showRequests() throws IOException {
        System.out.println("showRequests() called");

        if (requestsView == null) {
            System.out.println("Loading requests view for first time");
            requestsView = FXMLLoader.load(getClass().getResource("/FXMLFiles/AccountView/ClientAccountRequests.fxml"));
            setupViewAnchors(requestsView);
        }

        System.out.println("Switching to requests view");
        switchToView(requestsView);

        // Load friend requests after view is switched
        javafx.application.Platform.runLater(() -> {
            System.out.println("Platform.runLater executing...");
            loadFriendRequests();
        });
    }

    @FXML
    private void showSettings() throws IOException {
        if (settingsView == null) {
            settingsView = FXMLLoader.load(getClass().getResource("/FXMLFiles/AccountView/ClientAccountSettings.fxml"));
            setupViewAnchors(settingsView);
        }
        switchToView(settingsView);
    }

    // Simple frame switching methods (no IOException needed)
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
        System.out.println("*** FXML saveUsername() method called! ***");

        String newUsername = getNewUsername();
        System.out.println("New username: '" + newUsername + "'");

        if (newUsername == null || newUsername.trim().isEmpty()) {
            showUsernameEmpty();
            return;
        }

        if (usernameChangeListener != null) {
            System.out.println("Using username change listener");
            usernameChangeListener.run();
        } else {
            System.out.println("No listener available - cannot process username change");
            showUsernameUpdateFailed();
        }
    }

    @FXML
    public void savePassword() {
        System.out.println("*** FXML savePassword() method called! ***");
        System.out.println("Password save button clicked!");

        String oldPassword = getOldPassword();
        String newPassword = getNewPassword();
        String confirmPassword = getConfirmPassword();

        System.out.println("Password fields validation...");

        // Validation
        if (oldPassword == null || oldPassword.trim().isEmpty() ||
                newPassword == null || newPassword.trim().isEmpty() ||
                confirmPassword == null || confirmPassword.trim().isEmpty()) {
            System.out.println("Password fields are empty, showing error");
            showPasswordEmptyFields();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            System.out.println("Passwords don't match, showing error");
            showPasswordMismatch();
            return;
        }

        if (oldPassword.equals(newPassword)) {
            System.out.println("New password same as old, showing error");
            showPasswordSameAsOld();
            return;
        }

        System.out.println("Password validation passed");
        System.out.println("Checking if passwordChangeListener exists: " + (passwordChangeListener != null));

        if (passwordChangeListener != null) {
            System.out.println("Calling password change listener...");
            passwordChangeListener.run();
        } else {
            System.out.println("ERROR: Password change listener is null!");
            showPasswordUpdateFailed();
        }
    }

    @FXML
    public void hideChangePassword() {
        if (changePasswordFrame != null) {
            changePasswordFrame.setVisible(false);
        }
    }

    @FXML
    public void hideChangeUsername() {
        if (changeUsernameFrame != null) {
            changeUsernameFrame.setVisible(false);
        }
    }

    private void setupViewAnchors(Parent view) {
        AnchorPane.setTopAnchor(view, 0.0);
        AnchorPane.setBottomAnchor(view, 0.0);
        AnchorPane.setLeftAnchor(view, 0.0);
        AnchorPane.setRightAnchor(view, 0.0);
    }

    private void switchToView(Parent view) {
        rightFrame.getChildren().clear();
        rightFrame.getChildren().add(view);
    }

    // USERNAME FEEDBACK
    public void showUsernameUpdated() {
        updateStatus(usernameStatusLabel, "Username updated!", Color.rgb(0, 245, 83), false);
    }

    public void showUsernameEmpty() {
        updateStatus(usernameStatusLabel, "Username cannot be empty!", Color.rgb(255, 165, 0), true);
    }

    public void showUsernameUpdateFailed() {
        updateStatus(usernameStatusLabel, "Username update failed!", Color.rgb(217, 27, 36), true);
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

    // REUSABLE STATUS UPDATER FOR ANY LABELS
    // REMODELED UPDATE STATUS - MUCH CLEANER
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

    // GETTERS FOR INPUTS
    public String getNewUsername() {
        String value = (newUsernameTextField != null) ? newUsernameTextField.getText() : null;
        System.out.println("getNewUsername() returning: '" + value + "'");
        return value;
    }

    public String getOldPassword() {
        return oldPasswordTextField.getText();
    }

    public String getNewPassword() {
        return newPasswordTextField.getText();
    }

    public String getConfirmPassword() {
        return confirmPasswordTextField.getText();
    }

    public void setBackButtonListener(Runnable listener) {
        this.backButtonListener = listener;
    }

    public void setLogoutAccountButtonListener(Runnable listener) {
        this.LogoutAccountButtonListener = listener;
    }


    // =-=-=-=-=-=-=-=-CHANGES-=-=-=-=-=-=-=-=\\
    // All the status and UI methods stay the same:
    public void showUsernameAlreadyExists() {
        updateStatus(usernameStatusLabel, "Username already exists!", javafx.scene.paint.Color.rgb(217, 27, 36), true);
    }

    public void showInvalidOldPassword() {
        updateStatus(passwordStatusLabel, "Current password is incorrect!", javafx.scene.paint.Color.rgb(217, 27, 36), true);
    }

    // Field clearing methods
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

    // Setter methods for the listeners
    public void setUsernameChangeListener(Runnable listener) {
        System.out.println("Setting username change listener: " + (listener != null));
        this.usernameChangeListener = listener;
    }

    public void setPasswordChangeListener(Runnable listener) {
        System.out.println("Setting password change listener: " + (listener != null));
        this.passwordChangeListener = listener;
    }


}