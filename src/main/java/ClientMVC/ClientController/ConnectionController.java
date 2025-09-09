package ClientMVC.ClientController;

import ClientMVC.ClientView.Connection.ConnectionView;

import ClientMVC.ClientModel.ConnectionModel;
import ClientMVC.ClientView.Connection.ConnectionJFXController;

public class ConnectionController {
    private final ConnectionJFXController connectionJFXController;
    private final ConnectionModel model;
    private final ConnectionView view;

    public ConnectionController(ConnectionModel model, ConnectionView view, ConnectionJFXController jfxController) {
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


