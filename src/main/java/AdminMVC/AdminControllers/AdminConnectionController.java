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
            // The RMI IP property has already been set by the ConnectionModel's constructor
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

        // The model now handles all RMI connection logic
        ConnectionModel.ConnectionResult result = model.connect(ip, portText);

        Platform.runLater(() -> {
            switch (result.getStatus()) {
                case SUCCESS:
                    // Connection was successful, get the stub and proceed
                    try {
                        AdminInterface serverStub = model.getRemoteServiceStub();

                        // Close connection view
                        view.connect(); // Assuming this closes the stage

                        // Open login view
                        LoginView loginView = new LoginView();
                        Stage loginStage = new Stage();
                        loginView.start(loginStage);

                        // Create unified model with RMI server stub
                        UnifiedAdminModel unifiedModel = new UnifiedAdminModel(serverStub, adminCallback);

                        // Create login controller
                        new AdminLoginController(loginView, unifiedModel, loginView.getFXController());

                    } catch (Exception e) {
                        e.printStackTrace();
                        ui.showConnectionFailedLabel();
                    }
                    break;

                // --- Error Handling ---
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
                case RMI_LOOKUP_FAILED:
                    ui.showConnectionFailedLabel();
                    break;
            }
        });
    }
}