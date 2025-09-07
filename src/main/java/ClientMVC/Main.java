package ClientMVC;

import ClientMVC.ClientController.ClientController;
import ClientMVC.ClientModel.ClientModel;
import ClientMVC.ClientView.ClientView;
import ClientMVC.ClientView.ClientViewConnection;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;


import java.io.IOException;
import java.net.Socket;

public class Main{

    public static void main(String[] args) throws IOException {
        //Start JavaFX on an FX thread
        Platform.startup(() -> {
            try {
                ClientView view = new ClientView();
                Stage stage = new Stage();
                view.start(stage);
                Socket socket = new Socket("localhost", 1234);
                ClientModel model = new ClientModel(socket, "User" + (int)(Math.random()*1000));
                new ClientController(model, view, view.getFxController());
                new Thread(model, "Client-Receiver").start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // testing
        new Thread(new Runnable() {
            @Override
            public void run() {
                new Thread(() -> Application.launch(ClientViewConnection.class, args));
            }
        }).start();

        Application.launch(ClientViewConnection.class, args);
    }

}

