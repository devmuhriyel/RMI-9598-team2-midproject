package ClientMVC;

import ClientMVC.ClientController.ClientController;
import ClientMVC.ClientController.LoginController;
import ClientMVC.ClientModel.ClientModel;
import ClientMVC.ClientModel.LoginModel;
import ClientMVC.ClientView.Login.LoginView;
import javafx.application.Platform;
import javafx.stage.Stage;


import java.io.IOException;
import java.net.Socket;

public class Main{

    public static void main(String[] args) throws IOException {
        //Start JavaFX on an FX thread
        Platform.startup(() -> {
            try {
                LoginView view = new LoginView();
                Stage stage = new Stage();
                view.start(stage);
                Socket socket = new Socket("localhost", 1234);
                LoginModel model = new LoginModel(socket);
                new LoginController(view, model, view.getFXController());
                new Thread(model, "Client-Receiver").start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // testing

    }

}

