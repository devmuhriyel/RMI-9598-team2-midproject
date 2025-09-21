package AdminMVC.AdminView;

import AdminMVC.AdminControllers.AdminController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AdminPage extends Application {

    private AdminPageJFXController jfxController;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXMLFiles/NewAdmin.fxml"));
        Parent root = loader.load();
        jfxController = loader.getController();

        // Initialize Controller
        new AdminController(jfxController);

        stage.setTitle("Admin Page");
        stage.setScene(new Scene(root, 865, 620));
        stage.setResizable(true);
        stage.show();
    }

    public AdminPageJFXController getFXController() {
        return jfxController;
    }
}
