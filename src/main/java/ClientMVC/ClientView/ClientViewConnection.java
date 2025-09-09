package ClientMVC.ClientView;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientViewConnection extends Application {
    private ClientViewConnectionController jfxController;
    private static ClientViewConnection instance;

    public ClientViewConnection() {
        instance = this;
    }

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientConnection.fxml"));
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

    public ClientViewConnectionController getFxController() {
        return jfxController;
    }

    public static ClientViewConnection getInstance() {
        return instance;
    }
}
