package ClientMVC.ClientView.Account;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AccountView extends Application {
    private AccountJFXController accountJFXController;
    @FXML


    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXMLFiles/ClientAccountView.fxml"));
            Parent root = loader.load();
            accountJFXController = loader.getController();
            accountJFXController.showProfile();

            Scene scene = new Scene(root);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.initStyle(StageStyle.TRANSPARENT);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setResizable(false);
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AccountJFXController getFXController() {
        return accountJFXController;
    }
}