package ClientMVC.ClientController;

import ClientMVC.ClientModel.ConnectionModel;
import ClientMVC.ClientView.Connection.ConnectionView;
import ClientMVC.ClientView.Connection.ConnectionJFXController;
import ClientMVC.ClientView.Login.LoginView;
import Common.Interfaces.ClientInterface; // 💡 Import the service interface
import javafx.application.Platform;
import javafx.stage.Stage;

public class ConnectionController {
    private final ConnectionJFXController connectionJFXController;
    private final ConnectionModel model;
    private final ConnectionView view; // Assumed to be the container for the JFXController

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
                        // 1. Get the RMI Stub from the Model
                        ClientInterface clientService = model.getRemoteServiceStub();

                        // 2. Close the current Connection window
                        Stage stage = (Stage) connectionJFXController.getConnectButton().getScene().getWindow();
                        stage.close();

                        // 3. Launch the Login View
                        LoginView lView = new LoginView();
                        Stage loginStage = new Stage();
                        lView.start(loginStage);

                        // 4. Initialize LoginController, passing the RMI stub
                        // The LoginController is now responsible for handling user authentication
                        new LoginController(lView, clientService, lView.getFXController());

                    } catch (Exception e) {
                        e.printStackTrace();
                        //connectionJFXController.showError("Application launch error: " + e.getMessage());
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
            case RMI_LOOKUP_FAILED: // Handle all fatal connection errors with a generic input label or a specific message
                connectionJFXController.showInvalidInputLabel(); // or use a dedicated method: connectionJFXController.showError(result.getMessage());
                break;
        }
    }

    private void setupListeners(){
        connectionJFXController.setConnectButtonListener(() -> {
            connect();
        });
    }
}