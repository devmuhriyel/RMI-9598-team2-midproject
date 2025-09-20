package AdminMVC;

import AdminMVC.AdminModel.ConnectionModel;
import AdminMVC.AdminView.AdminConnectionView;
import AdminMVC.AdminControllers.AdminConnectionController;
import javafx.application.Platform;
import javafx.stage.Stage;

public class AdminMain {
    public static void main(String[] args) {
        Platform.startup(() -> {
            try {
                AdminConnectionView view = new AdminConnectionView();
                Stage stage = new Stage();
                view.start(stage);
                ConnectionModel model = new ConnectionModel();
                new AdminConnectionController(model, view, view.getFXController());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}