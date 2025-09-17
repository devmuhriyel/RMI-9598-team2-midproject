package ClientMVC.ClientView.Login;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LoginView extends Application {

    private static Stage primaryStage;
    private LoginJFXController jfxController;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage; // store reference
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXMLFiles/Login.fxml"));
        Parent root = loader.load();
        jfxController = loader.getController();
        primaryStage.setTitle("Login");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    // === expose stage for reuse ===
    public Stage getStage() {
        return primaryStage;
    }

    // === static access (optional, for global usage) ===
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    // === when login is successful, close this window ===
    public void login() {
        Platform.runLater(() -> {
            if (primaryStage != null) {
                primaryStage.close();
            }
        });
    }

    public LoginJFXController getFXController() {
        return jfxController;
    }
}
