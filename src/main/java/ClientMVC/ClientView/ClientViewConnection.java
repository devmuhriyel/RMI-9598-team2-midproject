package ClientMVC.ClientView;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientViewConnection extends Application {

    private ClientViewConnectionController connectionController;

    @Override
    public void start(Stage stage){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientConnection.fxml"));
            Parent root = loader.load();

            connectionController = loader.getController(); // Get the FXController instance

            stage.setTitle("Client Application");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
