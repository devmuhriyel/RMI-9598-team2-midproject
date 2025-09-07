package ClientMVC.ClientView;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ClientView extends Application {
    private Button sendButton;
    private TextField messageField;
    private VBox messagesArea;
    private ClientViewController clientViewController;
    private static ClientView instance;

    public ClientView() {
        instance = this;
    }

    @Override
    public void start(Stage stage){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientView.fxml"));
            Parent root = loader.load();
            clientViewController = loader.getController();
            stage.setTitle("Client Application");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ClientViewController getFxController() {
        return clientViewController;
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
    public static ClientView getInstance() {
        return instance;
    }
}