package ClientMVC;

import javafx.application.Platform;
import javafx.stage.Stage;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
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
    }
}
