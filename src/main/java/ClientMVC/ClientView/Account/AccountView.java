package ClientMVC.ClientView.Account;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AccountView extends Application {
    private AccountJFXController accountJFXController;
    private static Stage primaryStage;


    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXMLFiles/AccountView/ClientAccountView.fxml"));
            Parent root = loader.load();
            primaryStage = stage;
            accountJFXController = loader.getController();
            accountJFXController.showProfile(); // opens default

            Scene scene = new Scene(root);
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED); // Removes title bar
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setResizable(false);
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public AccountJFXController getFXController() {
        return accountJFXController;
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