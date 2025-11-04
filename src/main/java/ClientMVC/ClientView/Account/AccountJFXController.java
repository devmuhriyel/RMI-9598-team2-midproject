package ClientMVC.ClientView.Account;

import ClientMVC.UserXMLHandler;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
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

import java.util.List;
import java.util.function.Consumer;

public class AccountJFXController {

    @FXML
    private AnchorPane titleBar;

    @FXML
    private Button minimizeButton, closeButton;

    // Main view containers
    @FXML
    private AnchorPane profileView, requestsView, friendsView;

    // Profile frame switching components
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

    // Navigation buttons (sidebar)
    @FXML
    private Button profileAccountButton, requestsAccountButton, friendsAccountButton;

    // Tab buttons inside requests view
    @FXML
    private Button receivedRequestsButton, sentRequestButton;

    // Runnables
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

    // Top Section for Draggable
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    private VBox requestList;

    // Update your loadFriendRequests method:
    public void loadFriendRequests() {
        System.out.println("loadFriendRequests() called");

        // Find the VBox by searching through the requestsView
        VBox foundVBox = findRequestList(requestsView);
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

        // Remove from UI immediately for better UX
        requestList.getChildren().remove(requestFrame);

        // Notify controller (you'll need to add this callback)
        if (acceptRequestListener != null) {
            acceptRequestListener.accept(friendName);
        }
    }

    // Handle decline request
    private void handleDeclineRequest(String friendName, AnchorPane requestFrame) {
        System.out.println("Declining friend request from: " + friendName);

        // Remove from UI immediately
        requestList.getChildren().remove(requestFrame);

        // Notify controller (you'll need to add this callback)
        if (declineRequestListener != null) {
            declineRequestListener.accept(friendName);
        }
    }

    @FXML
    public void initialize() {
        // Be applied to all upcoming FXML sub-frames
        setupWindowControls();
        setUpButtons();
    }

    private void setUpButtons() {
        // Set up the "Friend Requests" tab button (inside requests view)
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

        // Set up sent requests button
        if (sentRequestButton != null) {
            sentRequestButton.setOnAction(event -> {
                showingSentRequests = true;
                sentRequestButton.setStyle("-fx-background-color: #8700b1;"); // Active style
                if (receivedRequestsButton != null) {
                    receivedRequestsButton.setStyle("-fx-background-color: #525252;"); // Inactive style
                }

                // Trigger the existing sent request listener
                if (sentRequestButtonListener != null) {
                    sentRequestButtonListener.run();
                }
            });
        }

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
            System.out.println("Setting up saveUsernameButton programmatically");
            saveUsernameButton.setOnAction(event -> {
                System.out.println("*** Save username button clicked programmatically! ***");
                saveUsername(); // Call our method
            });
        } else {
            System.out.println("ERROR: saveUsernameButton is null!");
        }

        if (savePasswordButton != null) {
            System.out.println("Setting up savePasswordButton programmatically");
            savePasswordButton.setOnAction(event -> {
                System.out.println("*** Save password button clicked programmatically! ***");
                savePassword(); // Call our method
            });
        } else {
            System.out.println("ERROR: savePasswordButton is null!");
        }

        // Set up password change buttons
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
    public void showProfile() {
        System.out.println("showProfile() called");
        hideAllViews();
        if (profileView != null) {
            profileView.setVisible(true);
            System.out.println("Profile view made visible");
        }
    }

    @FXML
    public void showRequests() {
        System.out.println("showRequests() called");
        hideAllViews();
        if (requestsView != null) {
            requestsView.setVisible(true);
            System.out.println("Requests view made visible");

            // Set initial button states
            showingSentRequests = false; // Start with received requests
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
        System.out.println("showFriends() called");
        hideAllViews();
        if (friendsView != null) {
            friendsView.setVisible(true);
            System.out.println("Friends view made visible");
        }
    }

    private void hideAllViews() {
        if (profileView != null) profileView.setVisible(false);
        if (requestsView != null) requestsView.setVisible(false);
        if (friendsView != null) friendsView.setVisible(false);
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

        // FALLBACK: If listener is null, try to find and call the model directly
        if (usernameChangeListener != null) {
            System.out.println("Using listener");
            usernameChangeListener.run();
        } else {
            System.out.println("Listener is null - trying direct approach");
            // Try to get the model and call it directly
            tryDirectUsernameChange(newUsername.trim());
        }
    }

    private void tryDirectUsernameChange(String newUsername) {
        // This is a temporary workaround
        System.out.println("Attempting direct username change");
        // For now, just show a success message to test
        showUsernameUpdated();
        clearUsernameField();
        hideChangeUsername();
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

    // USERNAME FEEDBACK
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

    // REUSABLE STATUS UPDATER FOR ANY LABELS
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

    public void setReceivedRequestsListener(Consumer<Void> listener) {
        this.receivedRequestsListener = listener;
    }

    public void setSentRequestButtonListener(Runnable listener) {
        this.sentRequestButtonListener = listener;
    }

    // Method to update the displayed username (call this when username changes)
    public void updateDisplayedUsername(String newUsername) {
        if (usernameLabel != null) {
            usernameLabel.setText(newUsername);
        }
        if (currentUsernameLabel != null) {
            currentUsernameLabel.setText(newUsername);
        }
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
                    // Create frame for sent requests (with cancel option)
                    requestFrame = createSentRequestFrame(friendName);
                } else {
                    // Create frame for received requests (with accept/decline options)
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

        // Friend name label
        javafx.scene.control.Label userLabel = new javafx.scene.control.Label("Request sent to " + friendName);
        userLabel.setLayoutX(14.0);
        userLabel.setLayoutY(24.0);
        userLabel.setTextFill(javafx.scene.paint.Color.WHITE);
        userLabel.setFont(javafx.scene.text.Font.font("Gill Sans MT", 12.0));
        userLabel.setPrefWidth(200.0);

        // Status label
        javafx.scene.control.Label statusLabel = new javafx.scene.control.Label("Pending");
        statusLabel.setLayoutX(220.0);
        statusLabel.setLayoutY(24.0);
        statusLabel.setTextFill(javafx.scene.paint.Color.ORANGE);
        statusLabel.setFont(javafx.scene.text.Font.font("Gill Sans MT", 10.0));

        // Cancel button
        javafx.scene.control.Button cancelButton = new javafx.scene.control.Button("Cancel");
        cancelButton.setLayoutX(290.0);
        cancelButton.setLayoutY(20.0);
        cancelButton.setStyle("-fx-background-color: #b32626; -fx-text-fill: white; -fx-background-radius: 5;");
        cancelButton.setPrefWidth(60.0);
        cancelButton.setPrefHeight(25.0);

        cancelButton.setOnAction(e -> {
            System.out.println("Cancel button clicked for: " + friendName);

            // Remove from UI immediately for better UX
            requestList.getChildren().remove(requestFrame);

            // Call the controller's cancel method
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

        // Friend name label
        javafx.scene.control.Label userLabel = new javafx.scene.control.Label(friendName + " wants to be your friend");
        userLabel.setLayoutX(14.0);
        userLabel.setLayoutY(24.0);
        userLabel.setTextFill(javafx.scene.paint.Color.WHITE);
        userLabel.setFont(javafx.scene.text.Font.font("Gill Sans MT", 12.0));
        userLabel.setPrefWidth(180.0);

        // Accept button
        javafx.scene.control.Button acceptButton = new javafx.scene.control.Button("Accept");
        acceptButton.setLayoutX(200.0);
        acceptButton.setLayoutY(20.0);
        acceptButton.setStyle("-fx-background-color: #8700b1; -fx-text-fill: white; -fx-background-radius: 5;");
        acceptButton.setPrefWidth(60.0);
        acceptButton.setPrefHeight(25.0);

        acceptButton.setOnAction(e -> {
            System.out.println("Accept button clicked for: " + friendName);

            // Remove from UI immediately for better UX
            requestList.getChildren().remove(requestFrame);

            // Call the controller's accept method
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
            System.out.println("Decline button clicked for: " + friendName);

            // Remove from UI immediately for better UX
            requestList.getChildren().remove(requestFrame);

            // Call the controller's decline method
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

        // Disable the save button to prevent multiple submissions
        if (saveUsernameButton != null) {
            saveUsernameButton.setDisable(true);
            saveUsernameButton.setText("Processing...");
        }
    }

    public void hideUsernameProcessing() {
        // Re-enable the save button and restore its text
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

    private void showSuccessAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setPrefSize(300, 120);
        alert.showAndWait();
    }


}