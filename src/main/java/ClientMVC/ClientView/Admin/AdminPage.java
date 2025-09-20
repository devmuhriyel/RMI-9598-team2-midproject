package ClientMVC.ClientView.Admin;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AdminPage extends Application {

    private AdminPageJFXController jfxController;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXMLFiles/AdminView/Admin.fxml"));
        Parent root = loader.load();
        jfxController = loader.getController();

        // Initialize Controller
        new ClientMVC.ClientController.AdminController(jfxController);

        stage.setTitle("Admin Page");
        stage.setScene(new Scene(root, 800, 600));
        stage.setResizable(true);
        stage.show();
    }

    public AdminPageJFXController getFXController() {
        return jfxController;
    }
}
