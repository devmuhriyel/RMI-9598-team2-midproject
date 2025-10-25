package ClientMVC.ClientView.Login;

import ClientMVC.ClientCallbackImplementation;
import ClientMVC.ClientController.LoginController;
import ClientMVC.ClientModel.UnifiedClientModel;
import Common.Interfaces.ClientInterface;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class LoginView extends Application {

    private static Stage primaryStage;
    private LoginJFXController jfxController;
    private static String serverHost = "localhost";
    private static int serverPort = 1099;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage; // store reference
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXMLFiles/Login.fxml"));
        Parent root = loader.load();
        jfxController = loader.getController();
        primaryStage.setTitle("Login");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        
        // Connect to RMI server and setup controller
        connectToRMIServer();
        
        primaryStage.show();
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
            
            // Create login controller with RMI connection
            new LoginController(this, model, jfxController);
            
            System.out.println("Connected to RMI server at " + serverHost + ":" + serverPort);
            
        } catch (Exception e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Connection Error");
                alert.setHeaderText("Failed to connect to server");
                alert.setContentText("Could not connect to RMI server at " + serverHost + ":" + serverPort + "\n" + e.getMessage());
                alert.showAndWait();
                System.exit(1);
            });
            e.printStackTrace();
        }
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
    
    // Static methods to configure server connection
    public static void setServerHost(String host) {
        serverHost = host;
    }
    
    public static void setServerPort(int port) {
        serverPort = port;
    }
}
