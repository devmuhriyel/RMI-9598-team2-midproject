package ClientMVC.ClientController;

import ClientMVC.ClientView.ClientViewConnection;
import ClientMVC.ClientView.LoginController;
import ClientMVC.ClientModel.ClientModelConnection;
import ClientMVC.ClientView.ClientViewConnectionController;
import ClientMVC.NetworkManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientControllerConnection {
    private final ClientViewConnectionController connectionJFXController;
    private final ClientModelConnection model;
    private final ClientViewConnection view;

    public ClientControllerConnection(ClientModelConnection model, ClientViewConnection view, ClientViewConnectionController jfxController) {
        this.view = view;
        this.model = model;
        this.connectionJFXController = jfxController;

        setupListeners();
    }

    private void setupListeners(){
        connectionJFXController.setConnectButtonListener(() -> {
            connectToServer(connectionJFXController.getIpAddress(), connectionJFXController.getPort());
        });
    }

    public boolean connectToServer(String ip, int port) {
        boolean success = model.connect(ip, port);

        if (success) {
            System.out.println("[DEBUG] Connected to server " + ip + ":" + port);

        } else {
            System.out.println("[DEBUG] Failed to connect to server " + ip + ":" + port);
            connectionJFXController.getStatusLabel().setText("Connection failed.");
        }

        return success;
    }


}


