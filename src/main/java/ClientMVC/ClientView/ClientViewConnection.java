package ClientMVC.ClientView;

import ClientMVC.ClientController.ClientControllerConnection;
import ClientMVC.NetworkManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientViewConnection extends Application {

    @Override
    public void start(Stage stage) {
        try {
            // Load the FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientConnection.fxml"));
            Parent root = loader.load();

            // Get the View controller (from FXML)
            ClientViewConnectionController viewController = loader.getController();

            // Create the Model
            NetworkManager network = new NetworkManager();

            // Give the model directly to the ViewController
            viewController.setNetworkManager(network);

            // (Optional) also wire Controller layer if you want extra logic
            new ClientControllerConnection(viewController, network);

            // Show the scene
            stage.setScene(new Scene(root));
            stage.setTitle("Client Connection");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
