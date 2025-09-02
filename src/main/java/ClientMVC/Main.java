package ClientMVC;

import javafx.application.Application;

import java.io.IOException;
import java.net.Socket;

public class Main{

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 1234);

        ClientView view = new ClientView();
        ClientModel model = new ClientModel(socket, "User123");
        ClientController controller = new ClientController(model, view);

        new Thread(model).start();
        Application.launch(ClientView.class, args);
    }

}

