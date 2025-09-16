package ClientMVC.ClientController;

import ClientMVC.ClientModel.LoginModel;
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

    //MVC METHODS
    public void connect() {
        String ip = connectionJFXController.getIpAddress();
        String portText = connectionJFXController.getPortText();

        // Let the model handle all validation
        ConnectionModel.ConnectionResult result = model.connect(ip, portText);

        // Controller only handles UI updates based on model results
        switch(result.getStatus()) {
            case SUCCESS:
                Platform.runLater(() -> {
                    // Close the connection window
                    Stage stage = (Stage) connectionJFXController.getConnectButton().getScene().getWindow();
                    stage.close();

                    // Launch Login GUI
                    try {
                        LoginView lView = new LoginView();
                        Stage loginStage = new Stage();
                        lView.start(loginStage);

                        LoginModel lModel = new LoginModel(model.getSocket());
                        new LoginController(lView, lModel, lView.getFXController());
                        new Thread(lModel, "Logged In").start();

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

    //LISTENER SETUP
    private void setupListeners(){
        connectionJFXController.setConnectButtonListener(() -> {
            connect();
        });
    }
}