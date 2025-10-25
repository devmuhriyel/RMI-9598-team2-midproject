package ClientMVC;

import ClientMVC.ClientController.ConnectionController;
import ClientMVC.ClientModel.ConnectionModel;
import ClientMVC.ClientView.Connection.ConnectionView;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.IOException;

public class Main{

    public static void main(String[] args) throws IOException {
        Platform.startup(() -> {
            try {
                ConnectionView view = new ConnectionView();
                Stage stage = new Stage();
                view.start(stage);
                ConnectionModel model = new ConnectionModel();
                new ConnectionController(model, view, view.getFXController());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}