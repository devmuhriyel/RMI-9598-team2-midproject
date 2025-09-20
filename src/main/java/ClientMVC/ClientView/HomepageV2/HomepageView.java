package ClientMVC.ClientView.HomepageV2;

import ClientMVC.ClientView.Account.AccountJFXController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class HomepageView {
    public class AccountView extends Application {
        private HomepageJFXController homepageJFXController;

        @Override
        public void start(Stage stage) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXMLFiles/homepage-version2.fxml"));
                Parent root = loader.load();
                homepageJFXController = loader.getController();
//                homepageJFXController = loader.showFriendsTab();

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

        public HomepageJFXController getFXController() {
            return homepageJFXController;
        }
    }
}
