package ClientMVC.ClientView.Account;

import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
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

    // Cache for loaded content
    private Parent profileView;
    private Parent requestsView;
    private Parent settingsView;

    // Top Section for Draggable
    private double xOffset = 0;
    private double yOffset = 0;

    // Runnables
    private Runnable saveUsernameButtonListener;

    @FXML
    public void initialize() {
        // Be applied to all upcoming FXML sub-frames
        setupWindowControls();
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

    // Main navigation methods
    @FXML
    public void showProfile() throws IOException {
        if (profileView == null) {
            profileView = FXMLLoader.load(getClass().getResource("/FXMLFiles/AccountView/ClientAccountProfile.fxml"));
            setupViewAnchors(profileView);
        }
        switchToView(profileView);
    }

    @FXML
    public void showRequests() throws IOException {
        if (requestsView == null) {
            requestsView = FXMLLoader.load(getClass().getResource("/FXMLFiles/AccountView/ClientAccountRequests.fxml"));
            setupViewAnchors(requestsView);
        }
        switchToView(requestsView);
    }

    @FXML
    public void showSettings() throws IOException {
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

    // GETTERS - INCLUDED THE CANCEL BUTTONS
    public Button getEditUsernameButton() {
        return editUsernameButton;
    }

    public Button getEditPasswordButton() {
        return editPasswordButton;
    }

    public Button getSaveUsernameButton() {
        return saveUsernameButton;
    }

    public Button getCancelUsernameButton() {
        return cancelUsernameButton;
    }

    public Button getSavePasswordButton() {
        return savePasswordButton;
    }

    public Button getCancelPasswordButton() {
        return cancelPasswordButton;
    }

    // GETTERS FOR INPUTS
    public String getNewUsername() {
        return newUsernameTextField.getText();
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

}