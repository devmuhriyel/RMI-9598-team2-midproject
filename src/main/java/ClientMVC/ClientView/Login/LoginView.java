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
    private static LoginView instance;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXMLFiles/Login.fxml"));
        Parent root = loader.load();
        jfxController = loader.getController();
        primaryStage.setTitle("Login");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    //MVC METHODS
    public void login(){
        Platform.runLater(() -> {
            Stage stage =  (Stage) jfxController.getLoginButton().getScene().getWindow();
            stage.close();
        });
    }

    //SETTERS AND GETTERS
    public LoginJFXController getFXController() {
        return jfxController;
    }
}
