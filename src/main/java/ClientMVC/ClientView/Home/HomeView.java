package ClientMVC.ClientView.Home;

import ClientMVC.ClientView.Login.LoginJFXController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HomeView extends Application {
    private Button sendButton;
    private TextField messageField;
    private TextField searchField;
    private TextField searchButton;
    private VBox messagesArea;
    private HomeJFXController homeJFXController;
    private LoginJFXController loginJFXController;
    private static HomeView instance;
    private Stage stage;
    private static Stage primaryStage;


    public HomeView() {
        instance = this;
    }

    @Override
    public void start(Stage stage){
        this.stage = stage;
        try {
            primaryStage = stage;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXMLFiles/Home.fxml"));
            Parent root = loader.load();
            homeJFXController = loader.getController();
            stage.setTitle("Client Application");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public HomeJFXController getFXController() {
        return homeJFXController;
    }

    public void openChatWithFriend(){
        Platform.runLater(() -> {
            if (stage != null) {
                stage.close();
            }
        });
    }
    public void setUsername(String username) {
        if (homeJFXController != null) {
            homeJFXController.setUserLabel(username);
        }
    }

    // expose stage for reuse
    public Stage getStage() {
        return primaryStage;
    }

    // static access (optional, for global usage)
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

}