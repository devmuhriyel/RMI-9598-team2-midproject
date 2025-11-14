package ClientMVC.ClientView.Signup;

import ClientMVC.ClientCallbackImplementation;
import ClientMVC.ClientController.SignupController;
import ClientMVC.ClientModel.UnifiedClientModel;
import Common.Interfaces.ClientInterface;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class SignupView {
    private Stage stage;
    private SignupJFXController fxController;
    private static String serverHost = "localhost";
    private static int serverPort = 1099;

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
    
    // Static methods to configure server connection
    public static void setServerHost(String host) {
        serverHost = host;
    }
    
    public static void setServerPort(int port) {
        serverPort = port;
    }
}
