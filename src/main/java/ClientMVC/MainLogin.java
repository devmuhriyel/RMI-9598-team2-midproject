package ClientMVC;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainLogin extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        loadScene("Login.fxml", "Chat App - Login");


    }

    public static void loadScene(String fxmlFile, String title) {
        try {
            Parent root = FXMLLoader.load(MainLogin.class.getResource("/Login.fxml"));
            //Parent root = loader.load();
            primaryStage.setTitle(title);
            primaryStage.setScene(new Scene(root));
            primaryStage.setResizable(false);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
