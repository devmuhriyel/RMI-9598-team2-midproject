package ClientMVC.ClientView.Admin;

import ClientMVC.ClientView.Admin.AdminPageJFXController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AdminPage extends Application {

    private static Stage primaryStage;
    private AdminPageJFXController jfxController;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Admin.fxml"));
        Parent root = loader.load();
        jfxController = loader.getController();
        primaryStage.setTitle("Admin Page");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    public AdminPageJFXController getFXController() {
        return jfxController;
    }
}
