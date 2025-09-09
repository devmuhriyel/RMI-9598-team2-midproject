package ClientMVC.ClientView.Connection;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ConnectionView extends Application {
    private ConnectionJFXController jfxController;
    private static ConnectionView instance;

    public ConnectionView() {
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

    public ConnectionJFXController getFXController() {
        return jfxController;
    }

    public static ConnectionView getInstance() {
        return instance;
    }
}
