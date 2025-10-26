package ClientMVC.ClientController;

import ClientMVC.ClientCallbackImplementation;
import ClientMVC.ClientModel.ConnectionModel;
import ClientMVC.ClientView.Connection.ConnectionView;
import ClientMVC.ClientView.Connection.ConnectionJFXController;
import ClientMVC.ClientView.Login.LoginView;
import Common.Interfaces.ClientInterface;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ConnectionController {
    private final ConnectionJFXController connectionJFXController;
    private final ConnectionModel model;
    private final ConnectionView view;

    private final ClientCallbackImplementation clientCallback;

    public ConnectionController(ConnectionModel model, ConnectionView view, ConnectionJFXController jfxController) {
        this.view = view;
        this.model = model;
        this.connectionJFXController = jfxController;

        try {
            this.clientCallback = new ClientCallbackImplementation();
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to export client callback", e);
        }

        setupListeners();
    }

    public void connect() {
        String ip = connectionJFXController.getIpAddress();
        String portText = connectionJFXController.getPortText();

        ConnectionModel.ConnectionResult result = model.connect(ip, portText);

        switch (result.getStatus()) {
            case SUCCESS:
                Platform.runLater(() -> {
                    try {
                        // 1) Get remote service
                        ClientInterface clientService = model.getRemoteServiceStub();

                        // 2) Close connection window
                        Stage stage = (Stage) connectionJFXController.getConnectButton().getScene().getWindow();
                        stage.close();

                        // 3) Open Login View
                        LoginView loginView = new LoginView();
                        Stage loginStage = new Stage();
                        loginView.start(loginStage);

                        // 4) Pass the EXISTING EXPORTED callback to LoginController
                        new LoginController(
                                loginView,
                                clientService,
                                loginView.getFXController(),
                                this.clientCallback
                        );

                    } catch (Exception e) {
                        e.printStackTrace();
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

    private void setupListeners() {
        connectionJFXController.setConnectButtonListener(this::connect);
    }
}
