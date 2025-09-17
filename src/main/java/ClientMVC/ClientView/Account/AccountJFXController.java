package ClientMVC.ClientView.Account;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class AccountJFXController {

    @FXML
    private AnchorPane rightFrame, titleBar;
    @FXML
    private Label userLabel;

    @FXML
    private Button minimizeButton, closeButton;

    // Top Section for Draggable
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        // Existing drag and window control code
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

        closeButton.setOnMouseClicked(event -> {
            ((Stage) closeButton.getScene().getWindow()).close();
        });

        minimizeButton.setOnMouseClicked(event -> {
            ((Stage) minimizeButton.getScene().getWindow()).setIconified(true);
        });
    }

    public void showProfile() {
        displayContent("/FXMLFiles/ClientAccountProfile.fxml");
    }

    public void showRequests() {
        displayContent("/FXMLFiles/ClientAccountRequests.fxml");
    }

    public void showSettings() {
        displayContent("/FXMLFiles/ClientAccountSettings.fxml");
    }

    public void displayContent(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            rightFrame.getChildren().setAll(view);
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
