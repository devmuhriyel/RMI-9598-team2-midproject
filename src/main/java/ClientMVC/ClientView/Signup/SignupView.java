package ClientMVC.ClientView.Signup;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SignupView extends Application {

    private static Stage primaryStage;
    private SignupJFXController jfxController;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage; // keep reference
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXMLFiles/Signup.fxml"));
        Parent root = loader.load();
        jfxController = loader.getController();
        primaryStage.setTitle("Sign Up");
        primaryStage.setScene(new Scene(root, 400, 450));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    // === expose the same stage ===
    public Stage getStage() {
        return primaryStage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public SignupJFXController getFXController() {
        return jfxController;
    }
}
