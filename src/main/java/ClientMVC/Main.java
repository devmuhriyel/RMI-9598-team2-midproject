package ClientMVC;

import ClientMVC.ClientController.ConnectionController;
import ClientMVC.ClientModel.ConnectionModel;
import ClientMVC.ClientView.Connection.ConnectionView;
import javafx.application.Platform;
import javafx.stage.Stage;


import java.io.IOException;
import java.net.Socket;

public class Main{

    public static void main(String[] args) throws IOException {
        //Start JavaFX on an FX thread
        Platform.startup(() -> {
            try {
                ConnectionView view = new ConnectionView();
                Stage stage = new Stage();
                view.start(stage);
                Socket socket = new Socket("localhost", 1234);
                ConnectionModel model = new ConnectionModel();
                new ConnectionController(model, view, view.getFXController());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // testing

    }

}

