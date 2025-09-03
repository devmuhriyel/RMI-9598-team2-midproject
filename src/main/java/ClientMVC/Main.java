package ClientMVC;

import javafx.application.Application;

import java.io.IOException;
import java.net.Socket;

public class Main{

    public static void main(String[] args) throws IOException {
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
    }
}

