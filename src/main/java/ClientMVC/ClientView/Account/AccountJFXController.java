package ClientMVC.ClientView.Account;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class AccountJFXController {

    @FXML
    private AnchorPane rightFrame, titleBar;

    @FXML
    private Button minimizeButton, closeButton;

    // Cache for loaded content
    private Parent profileView;
    private Parent requestsView;
    private Parent settingsView;
    // Top Section for Draggable
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
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
        // Frameless Button
        closeButton.setOnMouseClicked(event -> {
            ((Stage) closeButton.getScene().getWindow()).close();
        });

        minimizeButton.setOnMouseClicked(event -> {
            ((Stage) minimizeButton.getScene().getWindow()).setIconified(true);
        });
    }

    @FXML
    private void showProfile() throws IOException {
        if (profileView == null) {
            profileView = FXMLLoader.load(getClass().getResource("/FXMLFiles/ClientAccountProfile.fxml"));
            setupViewAnchors(profileView);
        }
        switchToView(profileView);
    }

    @FXML
    private void showRequests() throws IOException {
        if (requestsView == null) {
            requestsView = FXMLLoader.load(getClass().getResource("/FXMLFiles/ClientAccountRequests.fxml"));
            setupViewAnchors(requestsView);
        }
        switchToView(requestsView);
    }

    @FXML
    private void showSettings() throws IOException {
        if (settingsView == null) {
            settingsView = FXMLLoader.load(getClass().getResource("/FXMLFiles/ClientAccountSettings.fxml"));
            setupViewAnchors(settingsView);
        }
        switchToView(settingsView);
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
}
