package ClientMVC.ClientView.Signup;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SignupView extends Application {
    private static Stage primaryStage;
    private SignupJFXController fxController;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXMLFiles/Signup.fxml"));
        Parent root = loader.load();
        fxController = loader.getController();

        primaryStage.setTitle("Sign Up");
        primaryStage.setScene(new Scene(root, 400, 450));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public SignupJFXController getFXController() {
        return fxController;
    }
}
