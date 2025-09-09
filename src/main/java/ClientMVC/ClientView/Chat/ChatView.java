package ClientMVC.ClientView.Chat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ChatView extends Application {
    private Button sendButton;
    private TextField messageField;
    private VBox messagesArea;
    private ClientJFXController clientJFXController;
    private static ChatView instance;

    public ChatView() {
        instance = this;
    }

    @Override
    public void start(Stage stage){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientView.fxml"));
            Parent root = loader.load();
            clientJFXController = loader.getController();
            stage.setTitle("Client Application");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ClientJFXController getFXController() {
        return clientJFXController;
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
    public static ChatView getInstance() {
        return instance;
    }
}