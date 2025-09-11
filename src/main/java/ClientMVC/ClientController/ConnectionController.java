package ClientMVC.ClientController;

import ClientMVC.ClientView.Connection.ConnectionView;

import ClientMVC.ClientModel.ConnectionModel;
import ClientMVC.ClientView.Connection.ConnectionJFXController;
import javafx.application.Platform;
import javafx.stage.Stage;

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

    //MVC METHODS
    public void connect(String ip, int port) {
        boolean success = model.connect(ip, port);
        if (success) {
            view.connect();
        } else {


            //Paayos nito


            connectionJFXController.getStatusLabel().setText("Connection failed. Your IP and/or port number is invalid.");





        }
    }

    //LISTENER SETUP
    private void setupListeners(){
        connectionJFXController.setConnectButtonListener(() -> {
            connect(connectionJFXController.getIpAddress(), connectionJFXController.getPort());
        });
    }
}


