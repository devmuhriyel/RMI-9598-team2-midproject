package ClientMVC.ClientView.homepageVersion2;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class homepageJFXController {


    @FXML
    private AnchorPane listContainterFrame, titleBar;
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

        // Load default content (Profile)
        try {
            showFriendsLists(); // This will load the profile by default
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load default profile view: " + e.getMessage());
        }
    }
    public void showFriendsLists() throws IOException {
        loadContent("/FXMLFiles/FriendsHomeTab.fxml");
    }

    public void showGroupsLists() throws IOException {
        loadContent("/FXMLFiles/FriendsHomeTab.fxml");
    }


    private void loadContent(String fxmlPath) throws IOException {
        Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
        // Use rightFrame directly
        // This already close the existing FXML then it adds the new one
        listContainterFrame.getChildren().setAll(view);

        // Ensure child fills entire content container
        AnchorPane.setTopAnchor(view, 0.0);
        AnchorPane.setBottomAnchor(view, 0.0);
        AnchorPane.setLeftAnchor(view, 0.0);
        AnchorPane.setRightAnchor(view, 0.0);
    }

}
