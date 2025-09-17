package ClientMVC.ClientView.Connection;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ConnectionView extends Application {
    private ConnectionJFXController jfxController;

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXMLFiles/ClientConnection.fxml"));
            Parent root = loader.load();
            jfxController = loader.getController();

            Scene scene = new Scene(root);
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED); // Removes title bar
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setResizable(false);
            stage.setScene(scene);
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
