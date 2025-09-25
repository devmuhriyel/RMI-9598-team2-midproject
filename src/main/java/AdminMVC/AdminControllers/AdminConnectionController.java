package AdminMVC.AdminControllers;

import AdminMVC.AdminModel.ConnectionModel;
import AdminMVC.AdminModel.UnifiedAdminModel;
import AdminMVC.AdminView.Connection.AdminConnectionView;
import AdminMVC.AdminView.Connection.AdminConnectionJFXController;
import AdminMVC.AdminView.Login.LoginView;
import javafx.application.Platform;
import javafx.stage.Stage;

public class AdminConnectionController {
    //VARIABLES
    private final AdminConnectionJFXController connectionJFXController;
    private final ConnectionModel model;
    private final AdminConnectionView view;

    //CONSTRUCTOR
    public AdminConnectionController(ConnectionModel model, AdminConnectionView view, AdminConnectionJFXController jfxController) {
        this.view = view;
        this.model = model;
        this.connectionJFXController = jfxController;
        setupListeners();
    }

    //METHODS
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
                        LoginView loginView = new LoginView();
                        Stage loginStage = new Stage();
                        loginView.start(loginStage);
                        UnifiedAdminModel unifiedModel = new UnifiedAdminModel(model.getSocket());
                        new LoginController(loginView, unifiedModel, loginView.getFXController());
                        new Thread(unifiedModel, "Admin Communication").start();

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
                connectionJFXController.showInvalidPortRangeLabel();
                break;

            case INVALID_IP_FORMAT:
                connectionJFXController.showInvalidIpFormatLabel();
                break;

            case CONNECTION_FAILED:
                connectionJFXController.showConnectionFailedLabel();
                break;
        }
    }

    //BUTTON LISTENERS
    private void setupListeners() {
        connectionJFXController.setConnectButtonListener(() -> {
            connect();
        });
    }
}