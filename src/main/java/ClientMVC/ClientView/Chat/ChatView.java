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
    private Button sendButton;
    private TextField messageField;
    private VBox messagesArea;
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
    
    // Navigation method to go to HomeView
    public void goToHome() {
        Platform.runLater(() -> {
            try {
                // Close current chat window
                Stage currentStage = (Stage) chatJFXController.getHomeButton().getScene().getWindow();
                currentStage.close();
                
                // Open HomeView
                HomeView homeView = new HomeView();
                Stage homeStage = new Stage();
                homeView.start(homeStage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}