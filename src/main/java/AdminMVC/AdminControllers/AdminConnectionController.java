package AdminMVC.AdminControllers;

import AdminMVC.AdminCallbackImplementation;
import AdminMVC.AdminModel.ConnectionModel;
import AdminMVC.AdminModel.UnifiedAdminModel;
import AdminMVC.AdminView.Connection.AdminConnectionJFXController;
import AdminMVC.AdminView.Connection.AdminConnectionView;
import AdminMVC.AdminView.Login.LoginView;
import Common.Interfaces.AdminInterface;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class AdminConnectionController {
    private final ConnectionModel model;
    private final AdminConnectionView view;
    private final AdminConnectionJFXController ui;
    private final AdminCallbackImplementation adminCallback;

    public AdminConnectionController(ConnectionModel model, AdminConnectionView view, AdminConnectionJFXController ui) {
        this.view = view;
        this.model = model;
        this.ui = ui;

        try {
            this.adminCallback = new AdminCallbackImplementation();
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to export admin callback", e);
        }

        initialize();
    }

    private void initialize() {
        ui.setConnectButtonListener(this::handleConnect);
    }

    private void handleConnect() {
        String ip = ui.getIpAddress();
        String portText = ui.getPortText();

        ConnectionModel.ConnectionResult result = model.connect(ip, portText);

        Platform.runLater(() -> {
            switch (result.getStatus()) {
                case SUCCESS:
                    connectToRMIServer(ip);
                    break;
                case EMPTY_FIELDS:
                    ui.showEmptyFieldsLabel();
                    break;
                case INVALID_PORT_FORMAT:
                    ui.showInvalidPortLabel();
                    break;
                case INVALID_PORT_RANGE:
                    ui.showInvalidPortRangeLabel();
                    break;
                case INVALID_IP_FORMAT:
                    ui.showInvalidIpFormatLabel();
                    break;
                case CONNECTION_FAILED:
                    ui.showConnectionFailedLabel();
                    break;
            }
        });
    }

    private void connectToRMIServer(String ip) {
        try {
            // Connect to RMI registry
            Registry registry = LocateRegistry.getRegistry(ip, 1099);
            AdminInterface server = (AdminInterface) registry.lookup("ChatServer");

            // Close connection view
            view.connect();

            // Open login view with RMI connection
            Platform.runLater(() -> {
                try {
                    LoginView loginView = new LoginView();
                    Stage loginStage = new Stage();
                    loginView.start(loginStage);

                    // Create unified model with RMI server
                    UnifiedAdminModel unifiedModel = new UnifiedAdminModel(server, adminCallback);

                    // Create login controller
                    new AdminLoginController(loginView, unifiedModel, loginView.getFXController());

                } catch (Exception e) {
                    e.printStackTrace();
                    ui.showConnectionFailedLabel();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> ui.showConnectionFailedLabel());
        }
    }
}