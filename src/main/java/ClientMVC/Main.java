package ClientMVC;

import ClientMVC.ClientController.ClientController;
import ClientMVC.ClientModel.ClientModel;
import ClientMVC.ClientView.ClientView;
import javafx.application.Application;

import ClientMVC.ClientView.ClientViewConnection;


import java.io.IOException;
import java.net.Socket;

public class Main{

    public static void main(String[] args) throws IOException {
        //WAIT LANG HUHHUHUHUUH DI KO GETS LEMMME just comment it for a while

        /*
        new Thread(() -> Application.launch(ClientView.class)).start();

        // Wait for ClientView instance to be ready
        while (ClientView.getInstance() == null || ClientView.getInstance().getFxController() == null) {
            try {
                Thread.sleep(100); // small delay to avoid busy loop
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Now that UI is ready, create the model and controller
        try {
            Socket socket = new Socket("localhost", 1234);
            ClientView view = ClientView.getInstance();
            ClientModel model = new ClientModel(socket, "User123");
            new ClientController(model, view, view.getFxController());

            new Thread(model).start();
        } catch (IOException e) {
            e.printStackTrace();
        }


         */
        //Flores

        new Thread(new Runnable() {
            @Override
            public void run() {
                new Thread(() -> Application.launch(ClientViewConnection.class, args));
            }
        }).start();

        Application.launch(ClientViewConnection.class, args);
    }

}

