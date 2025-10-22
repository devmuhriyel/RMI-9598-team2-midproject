package ClientMVC.ClientController;

import ClientMVC.ClientCallbackImplementation;
import ClientMVC.ClientModel.ConnectionModel;
import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Connection.ConnectionView;
import ClientMVC.ClientView.Connection.ConnectionJFXController;
import ClientMVC.ClientView.Login.LoginView;
import Common.Interfaces.ClientInterface;
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

    public void connect() {
        String ip = connectionJFXController.getIpAddress();
        String portText = connectionJFXController.getPortText();
        ConnectionModel.ConnectionResult result = model.connect(ip, portText);

        switch(result.getStatus()) {
            case SUCCESS:
                Platform.runLater(() -> {
                    try {
                        // 1. Get the RMI Stub and Callback from the ConnectionModel
                        ClientInterface clientService = model.getRemoteServiceStub();
                        ClientCallbackImplementation callback = model.getCallback();

                        // 2. Create UnifiedClientModel (wraps RMI stub and callback)
                        UnifiedClientModel unifiedModel = new UnifiedClientModel(clientService, callback);

                        // 3. Close the current Connection window
                        Stage stage = (Stage) connectionJFXController.getConnectButton().getScene().getWindow();
                        stage.close();

                        // 4. Launch the Login View
                        LoginView lView = new LoginView();
                        Stage loginStage = new Stage();
                        lView.start(loginStage);

                        // 5. Initialize LoginController with UnifiedClientModel
                        // Now your existing LoginController works without changes!
                        new LoginController(lView, unifiedModel, lView.getFXController());

                    } catch (Exception e) {
                        e.printStackTrace();
                        // Could show error dialog here
                    }
                });
                break;

            // --- Error Handling ---
            case EMPTY_FIELDS:
                connectionJFXController.showEmptyFieldsLabel();
                break;
            case INVALID_PORT_FORMAT:
                connectionJFXController.showInvalidPortLabel();
                break;
            case INVALID_PORT_RANGE:
            case INVALID_IP_FORMAT:
            case CONNECTION_FAILED:
            case RMI_LOOKUP_FAILED:
                connectionJFXController.showInvalidInputLabel();
                break;
        }
    }

    private void setupListeners(){
        connectionJFXController.setConnectButtonListener(() -> {
            connect();
        });
    }
}