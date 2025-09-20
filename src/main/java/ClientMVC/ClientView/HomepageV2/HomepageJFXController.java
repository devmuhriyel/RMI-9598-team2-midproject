package ClientMVC.ClientView.HomepageV2;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class HomepageJFXController {


    @FXML
    private AnchorPane listContainterFrame, titleBar;
    @FXML
    private Label userLabel;

    @FXML
    private Button minimizeButton, closeButton;

    private Runnable set;

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
    public void showFriendsLists() throws IOException {
        displayContent("/FXMLFiles/FriendsHomeTab.fxml");
    }

    public void showGroupsLists() throws IOException {
        displayContent("/FXMLFiles/GroupsHomeTab.fxml");
    }

    public void displayContent(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            // calls the fxml
            // closes the
            listContainterFrame.getChildren().setAll(view);
            //
            // making sure that every fxml is aligned
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
