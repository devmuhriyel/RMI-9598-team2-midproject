package ClientMVC.ClientView.Signup;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SignupView {
    private Stage stage;
    private SignupJFXController fxController;

    public SignupView() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXMLFiles/Signup.fxml"));
        Parent root = loader.load();
        fxController = loader.getController();

        stage = new Stage();
        stage.setTitle("Sign Up");
        stage.setScene(new Scene(root, 400, 450));
        stage.setResizable(false);
    }

    public Stage getStage() {
        return stage;
    }

    public SignupJFXController getFxController() {
        return fxController;
    }
}
