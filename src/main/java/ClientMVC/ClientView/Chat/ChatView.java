package ClientMVC.ClientView.Chat;

import ClientMVC.ClientView.Home.HomeView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ChatView extends Application {
    private ChatJFXController chatJFXController;
    private static ChatView instance;

    public ChatView() {
        instance = this;
    }

    @Override
    public void start(Stage stage){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXMLFiles/ChatView.fxml"));
            Parent root = loader.load();
            chatJFXController = loader.getController();
            stage.setTitle("Client Application");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ChatJFXController getFXController() {
        return chatJFXController;
    }

}