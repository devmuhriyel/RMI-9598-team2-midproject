package ClientMVC.ClientView.Connection;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ConnectionView extends Application {
    private ConnectionJFXController jfxController;

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXMLFiles/ClientConnection.fxml"));
            Parent root = loader.load();
            jfxController = loader.getController();
            stage.setResizable(false);
            stage.setTitle("Client Connection");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //MVC METHODS
    public void connect(){
        Platform.runLater(() -> {
            Stage stage =  (Stage) jfxController.getConnectButton().getScene().getWindow();
            stage.close();
        });
    }

    //SETTERS AND GETTERS
    public ConnectionJFXController getFXController() {
        return jfxController;
    }
}
