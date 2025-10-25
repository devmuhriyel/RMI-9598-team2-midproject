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
        
        // Connect to RMI server and setup controller
        connectToRMIServer();
    }
    
    private void connectToRMIServer() {
        try {
            // Connect to RMI registry
            Registry registry = LocateRegistry.getRegistry(serverHost, serverPort);
            ClientInterface serverStub = (ClientInterface) registry.lookup("ChatServer");
            
            // Create callback implementation
            ClientCallbackImplementation callback = new ClientCallbackImplementation();
            
            // Create client model
            UnifiedClientModel model = new UnifiedClientModel(serverStub, callback);
            
            // Create signup controller with RMI connection
            new SignupController(model, this, fxController);
            
            System.out.println("Connected to RMI server at " + serverHost + ":" + serverPort);
            
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connection Error");
            alert.setHeaderText("Failed to connect to server");
            alert.setContentText("Could not connect to RMI server at " + serverHost + ":" + serverPort + "\n" + e.getMessage());
            alert.showAndWait();
            stage.close();
            e.printStackTrace();
        }
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
