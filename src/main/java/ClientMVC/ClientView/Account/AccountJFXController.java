package ClientMVC.ClientView.Account;

import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Controller for the Account View FXML.
 * Handles user account management including profile editing, friend requests, and settings.
 * This controller manages three embedded views: Profile, Requests, and Settings.
 */
public class AccountJFXController {

    // Window controls
    @FXML
    private AnchorPane titleBar;
    @FXML
    private Button minimizeButton, closeButton;

    // Main view containers - embedded in the same FXML
    @FXML
    private AnchorPane profileView, requestsView, settingsView;

    // Profile editing frames (initially hidden)
    @FXML
    private AnchorPane changePasswordFrame, changeUsernameFrame;

    // Profile editing buttons
    @FXML
    private Button editUsernameButton, editPasswordButton,
            saveUsernameButton, cancelUsernameButton,
            savePasswordButton, cancelPasswordButton;

    // Input fields for profile changes
    @FXML
    private TextField newUsernameTextField;
    @FXML
    private PasswordField oldPasswordTextField, newPasswordTextField, confirmPasswordTextField;

    // Status labels for user feedback
    @FXML
    private Label usernameStatusLabel, passwordStatusLabel;

    // Display labels for current user info
    @FXML
    private Label usernameLabel, currentUsernameLabel, passwordLabel;

    // Navigation and control buttons
    @FXML
    private Button logoutAccountButton, backButton;
    @FXML
    private Button profileAccountButton, requestsAccountButton, settingsAccountButton;

    // Callback listeners for MVC communication
    private Runnable backButtonListener;
    private Runnable LogoutAccountButtonListener;
    private Runnable usernameChangeListener;
    private Runnable passwordChangeListener;

    // Window dragging coordinates
    private double xOffset = 0;
    private double yOffset = 0;

    // Friend requests container
    @FXML
    private VBox requestList;

    /**
     * Loads friend requests into the UI.
     * Currently populated with sample data - should be connected to server data.
     */
    public void loadFriendRequests() {
        // Find the request list container
        VBox foundVBox = findRequestList(requestsView);
        if (foundVBox != null) {
            requestList = foundVBox;
        } else {
            return;
        }

        // Clear existing requests
        requestList.getChildren().clear();

        // TODO: Replace with actual server data
        String[] friendRequests = {"Alice Johnson", "Bob Smith", "Charlie Brown", "Steven Charz", "Joshua Flores"};

        // Create UI elements for each friend request
        for (String friendName : friendRequests) {
            AnchorPane requestFrame = createFriendRequestFrame(friendName);
            requestList.getChildren().add(requestFrame);
        }
    }

    /**
     * Recursively searches for the requestList VBox within a parent container.
     * @param parent The parent node to search in
     * @return The VBox with id "requestList" or null if not found
     */
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

    /**
     * Creates a UI frame for displaying a friend request.
     * @param friendName The name of the person sending the friend request
     * @return AnchorPane containing the friend request UI elements
     */
    private AnchorPane createFriendRequestFrame(String friendName) {
        // Main container
        AnchorPane friendRequestFrame = new AnchorPane();
        friendRequestFrame.setPrefHeight(65.0);
        friendRequestFrame.setPrefWidth(372.0);
        friendRequestFrame.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 10;");

        // Friend request message
        Label userLabel = new Label(friendName + " wants to be your friend");
        userLabel.setLayoutX(14.0);
        userLabel.setLayoutY(24.0);
        userLabel.setPrefHeight(17.0);
        userLabel.setPrefWidth(182.0);
        userLabel.setTextFill(javafx.scene.paint.Color.WHITE);
        userLabel.setFont(javafx.scene.text.Font.font("Gill Sans MT", 12.0));

        // Accept button
        Button acceptButton = new Button("Confirm");
        acceptButton.setLayoutX(200.0);
        acceptButton.setLayoutY(20.0);
        acceptButton.setStyle("-fx-background-color: #8700b1; -fx-text-fill: white;");
        acceptButton.setOnAction(e -> handleAcceptRequest(friendName, friendRequestFrame));

        // Decline button
        Button declineButton = new Button("Decline");
        declineButton.setLayoutX(270.0);
        declineButton.setLayoutY(20.0);
        declineButton.setStyle("-fx-background-color: #525252; -fx-text-fill: white;");
        declineButton.setOnAction(e -> handleDeclineRequest(friendName, friendRequestFrame));

        friendRequestFrame.getChildren().addAll(userLabel, acceptButton, declineButton);
        return friendRequestFrame;
    }

    /**
     * Handles accepting a friend request.
     * @param friendName Name of the friend to accept
     * @param requestFrame UI frame to remove from display
     */
    private void handleAcceptRequest(String friendName, AnchorPane requestFrame) {
        // TODO: Send accept request to server
        requestList.getChildren().remove(requestFrame);
    }

    /**
     * Handles declining a friend request.
     * @param friendName Name of the friend to decline
     * @param requestFrame UI frame to remove from display
     */
    private void handleDeclineRequest(String friendName, AnchorPane requestFrame) {
        // TODO: Send decline request to server
        requestList.getChildren().remove(requestFrame);
    }

    /**
     * JavaFX initialization method called when FXML is loaded.
     */
    @FXML
    public void initialize() {
        setupWindowControls();
        setUpButtons();
    }

    /**
     * Sets up event handlers for all interactive buttons.
     */
    private void setUpButtons() {
        // Navigation buttons
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

        // Profile editing buttons
        if (saveUsernameButton != null) {
            saveUsernameButton.setOnAction(event -> saveUsername());
        }

        if (savePasswordButton != null) {
            savePasswordButton.setOnAction(event -> savePassword());
        }

        if (cancelPasswordButton != null) {
            cancelPasswordButton.setOnAction(event -> {
                clearPasswordFields();
                hideChangePassword();
                clearPasswordStatus();
            });
        }
    }

    /**
     * Sets up window controls for the frameless window (drag, minimize, close).
     */
    private void setupWindowControls() {
        // Window dragging functionality
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

        // Close button
        if (closeButton != null) {
            closeButton.setOnMouseClicked(event -> {
                ((Stage) closeButton.getScene().getWindow()).close();
            });
        }

        // Minimize button
        if (minimizeButton != null) {
            minimizeButton.setOnMouseClicked(event -> {
                ((Stage) minimizeButton.getScene().getWindow()).setIconified(true);
            });
        }
    }

    // View Navigation Methods

    /**
     * Shows the Profile view and hides other views.
     */
    @FXML
    public void showProfile() {
        hideAllViews();
        if (profileView != null) {
            profileView.setVisible(true);
        }
    }

    /**
     * Shows the Requests view and loads friend requests.
     */
    @FXML
    public void showRequests() {
        hideAllViews();
        if (requestsView != null) {
            requestsView.setVisible(true);
            // Load friend requests asynchronously to avoid blocking UI
            javafx.application.Platform.runLater(this::loadFriendRequests);
        }
    }

    /**
     * Shows the Settings view and hides other views.
     */
    @FXML
    public void showSettings() {
        hideAllViews();
        if (settingsView != null) {
            settingsView.setVisible(true);
        }
    }

    /**
     * Hides all main views (Profile, Requests, Settings).
     */
    private void hideAllViews() {
        if (profileView != null) profileView.setVisible(false);
        if (requestsView != null) requestsView.setVisible(false);
        if (settingsView != null) settingsView.setVisible(false);
    }

    // Profile Editing Methods

    /**
     * Shows the password change form.
     */
    @FXML
    public void showChangePassword() {
        if (changePasswordFrame != null) {
            changePasswordFrame.setVisible(true);
        }
    }

    /**
     * Shows the username change form.
     */
    @FXML
    public void showChangeUsername() {
        if (changeUsernameFrame != null) {
            changeUsernameFrame.setVisible(true);
        }
    }

    /**
     * Handles username save button click.
     * Validates input and triggers username change through listener.
     */
    @FXML
    public void saveUsername() {
        String newUsername = getNewUsername();

        // Check if username field is empty
        if (newUsername == null || newUsername.trim().isEmpty()) {
            showUsernameFieldRequired();
            return;
        }

        if (usernameChangeListener != null) {
            usernameChangeListener.run();
        } else {
            // Fallback for testing without server connection
            tryDirectUsernameChange(newUsername.trim());
        }
    }

    /**
     * Fallback method for username change when no server connection is available.
     * Used primarily for testing purposes.
     */
    private void tryDirectUsernameChange(String newUsername) {
        showUsernameUpdated();
        clearUsernameField();
        hideChangeUsername();
    }

    /**
     * Handles password save button click.
     * Validates inputs and triggers password change through listener.
     */
    @FXML
    public void savePassword() {
        String oldPassword = getOldPassword();
        String newPassword = getNewPassword();
        String confirmPassword = getConfirmPassword();

        // Check if all fields are filled
        if (oldPassword == null || oldPassword.trim().isEmpty() ||
                newPassword == null || newPassword.trim().isEmpty() ||
                confirmPassword == null || confirmPassword.trim().isEmpty()) {
            showAllPasswordFieldsRequired();
            return;
        }

        // Validate password confirmation
        if (!newPassword.equals(confirmPassword)) {
            showPasswordMismatch();
            return;
        }

        // Validate new password is different from old
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

    /**
     * Hides the password change form.
     */
    @FXML
    public void hideChangePassword() {
        if (changePasswordFrame != null) {
            changePasswordFrame.setVisible(false);
        }
    }

    /**
     * Hides the username change form.
     */
    @FXML
    public void hideChangeUsername() {
        if (changeUsernameFrame != null) {
            changeUsernameFrame.setVisible(false);
        }
    }

    // Status Feedback Methods - Username

    public void showUsernameUpdated() {
        updateStatus(usernameStatusLabel, "Username updated!", Color.rgb(0, 245, 83), false);
    }

    public void showUsernameFieldRequired() {
        updateStatus(usernameStatusLabel, "Username field is required!", Color.rgb(255, 165, 0), true);
    }

    public void showUsernameEmpty() {
        updateStatus(usernameStatusLabel, "Username cannot be empty!", Color.rgb(255, 165, 0), true);
    }

    public void showUsernameUpdateFailed() {
        updateStatus(usernameStatusLabel, "Username update failed!", Color.rgb(217, 27, 36), true);
    }

    public void showUsernameAlreadyExists() {
        updateStatus(usernameStatusLabel, "Username already exists!", javafx.scene.paint.Color.rgb(217, 27, 36), true);
    }

    public void showUsernameSameAsCurrent() {
        updateStatus(usernameStatusLabel, "New username must be different!", javafx.scene.paint.Color.rgb(217, 27, 36), true);
    }

    // Status Feedback Methods - Password

    public void showPasswordChanged() {
        updateStatus(passwordStatusLabel, "Password changed!", Color.rgb(0, 245, 83), false);
    }

    public void showAllPasswordFieldsRequired() {
        updateStatus(passwordStatusLabel, "All text fields are required to be filled!", Color.rgb(255, 165, 0), true);
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

    public void showInvalidOldPassword() {
        updateStatus(passwordStatusLabel, "Current password is incorrect!", javafx.scene.paint.Color.rgb(217, 27, 36), true);
    }

    /**
     * Generic status update method for displaying user feedback.
     * @param label The label to update
     * @param message The message to display
     * @param color The text color
     * @param shake Whether to apply shake animation for errors
     */
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

    // Input Getter Methods

    public String getNewUsername() {
        return (newUsernameTextField != null) ? newUsernameTextField.getText() : null;
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

    // Field Management Methods

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

    /**
     * Updates the displayed username in the UI.
     * Call this method when username changes to keep display synchronized.
     * @param newUsername The new username to display
     */
    public void updateDisplayedUsername(String newUsername) {
        if (usernameLabel != null) {
            usernameLabel.setText(newUsername);
        }
        if (currentUsernameLabel != null) {
            currentUsernameLabel.setText(newUsername);
        }
    }

    // Listener Setter Methods - Used by Controller for MVC communication

    public void setBackButtonListener(Runnable listener) {
        this.backButtonListener = listener;
    }

    public void setLogoutAccountButtonListener(Runnable listener) {
        this.LogoutAccountButtonListener = listener;
    }

    public void setUsernameChangeListener(Runnable listener) {
        this.usernameChangeListener = listener;
    }

    public void setPasswordChangeListener(Runnable listener) {
        this.passwordChangeListener = listener;
    }
}