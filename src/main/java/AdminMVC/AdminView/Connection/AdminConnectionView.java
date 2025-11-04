package AdminMVC.AdminView.Connection;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AdminConnectionView extends Application {

    private AdminConnectionJFXController fxController;

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXMLFiles/AdminConnection.fxml"));
            Parent root = loader.load();
            fxController = loader.getController();

            Scene scene = new Scene(root);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.initStyle(StageStyle.TRANSPARENT);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setResizable(false);
            stage.setScene(scene);
            stage.setTitle("Admin Connection");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //MVC METHODS
    public void connect(){
        Platform.runLater(() -> {
            Stage stage = (Stage) fxController.getConnectButton().getScene().getWindow();
            stage.close();
        });
    }

    //SETTERS AND GETTERS
    public AdminConnectionJFXController getFXController() {
        return fxController;
    }
}