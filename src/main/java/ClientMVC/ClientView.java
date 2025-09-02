package ClientMVC;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ClientView extends Application {
    private Button sendButton;
    private TextField messageField;
    private VBox messagesArea;
    @Override
    public void start(Stage stage){
        try {
            // Create an FXMLLoader instance
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientView.fxml"));
            Parent root = loader.load();

            // Access nodes by fx:id
            sendButton = (Button) root.lookup("#sendButton");
            messageField = (TextField) root.lookup("#messageField");
            messagesArea = (VBox) root.lookup("#messagesArea");

            // Set up stage
            Image icon = new Image("SampleIcon.png");
            stage.getIcons().add(icon);
            stage.setTitle("Client Application");
            stage.setResizable(false);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Button getSendButton() {
        return sendButton;
    }
    public TextField getMessageField() {
        return messageField;
    }
    public VBox getMessagesArea() {
        return messagesArea;
    }
}
