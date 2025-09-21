package AdminMVC.AdminControllers;

import AdminMVC.AdminModel.UnifiedAdminModel;
import AdminMVC.AdminView.Admin.AdminView;
import AdminMVC.AdminView.Login.LoginJFXController;
import AdminMVC.AdminView.Login.LoginView;
import AdminMVC.AdminXMLHandler;

import javafx.application.Platform;
import javafx.stage.Stage;

public class LoginController {
    private final LoginJFXController loginJFXController;
    private final LoginView view;
    private final UnifiedAdminModel model;
    private AdminXMLHandler adminXMLHandler = new AdminXMLHandler();

    public LoginController(LoginView loginView, UnifiedAdminModel unifiedModel, LoginJFXController loginJFXController) {
        this.loginJFXController = loginJFXController;
        this.view = loginView;
        this.model = unifiedModel;
        model.setLoginResponseListener(this::handleLoginResponse);
        initialize();
    }

    private void initialize() {
        loginJFXController.setLoginButtonListener(() -> {
            loginJFXController.getLoginButton().setDisable(true);
            String username = loginJFXController.getUsername();
            String password = loginJFXController.getPassword();
            model.login(username, password);
        });
    }

    public void handleLoginResponse(String response) {
        Platform.runLater(() -> {
            if ("LOGIN_SUCCESS".equals(response)) {
                view.login(); // Close login window
                try {
                    AdminView homeView = new AdminView();
                    Stage stage1 = new Stage();
                    homeView.start(stage1);
                    new AdminController(homeView.getFXController(), model);
                    model.loadData(); // Load admin data
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if ("LOGIN_FAILED".equals(response)) {
                System.out.println("Invalid admin credentials");
                loginJFXController.getLoginButton().setDisable(false);
            }
        });
    }
}