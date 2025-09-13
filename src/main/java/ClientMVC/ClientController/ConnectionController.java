package ClientMVC.ClientController;

import ClientMVC.ClientView.Connection.ConnectionView;
import ClientMVC.ClientModel.ConnectionModel;
import ClientMVC.ClientModel.ConnectionModel.ConnectionResult;
import ClientMVC.ClientModel.ConnectionModel.ConnectionStatus;
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
    public void connect() {
        String ip = connectionJFXController.getIpAddress();
        String portText = connectionJFXController.getPortText();

        // Let the model handle all validation
        ConnectionResult result = model.connect(ip, portText);

        // Controller only handles UI updates based on model results
        switch(result.getStatus()) {
            case SUCCESS:
                connectionJFXController.showConectedLabel();
                break;
            case EMPTY_FIELDS:
                connectionJFXController.showEmptyFieldsLabel();
                break;
            case INVALID_PORT_FORMAT:
                connectionJFXController.showInvalidPortLabel();
                break;
            case INVALID_PORT_RANGE:
            case INVALID_IP_FORMAT:
            case CONNECTION_FAILED:
                connectionJFXController.showInvalidInputLabel();
                break;
        }
    }

    //LISTENER SETUP
    private void setupListeners(){
        connectionJFXController.setConnectButtonListener(() -> {
            connect();
        });
    }
}