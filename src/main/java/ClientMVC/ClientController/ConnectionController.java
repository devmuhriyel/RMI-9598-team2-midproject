package ClientMVC.ClientController;

import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Connection.ConnectionView;
import ClientMVC.ClientModel.ConnectionModel;
import ClientMVC.ClientView.Connection.ConnectionJFXController;
import ClientMVC.ClientView.Login.LoginView;
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
                    Stage stage = (Stage) connectionJFXController.getConnectButton().getScene().getWindow();
                    stage.close();

                    try {
                        LoginView lView = new LoginView();
                        Stage loginStage = new Stage();
                        lView.start(loginStage);
                        UnifiedClientModel unifiedModel = new UnifiedClientModel(model.getSocket(), "");
                        new LoginController(lView, unifiedModel, lView.getFXController());
                        new Thread(unifiedModel, "Client Communication").start();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
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

    private void setupListeners(){
        connectionJFXController.setConnectButtonListener(() -> {
            connect();
        });
    }
}