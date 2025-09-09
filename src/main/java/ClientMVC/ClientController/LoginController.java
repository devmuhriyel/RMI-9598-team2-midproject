package ClientMVC.ClientController;

import ClientMVC.ClientModel.LoginModel;
import ClientMVC.ClientView.Login.LoginJFXController;
import ClientMVC.ClientView.Login.LoginView;

public class LoginController {
    private final LoginJFXController loginJFXController;
    private final LoginView view;
    private final LoginModel model;

    public LoginController(LoginView loginView, LoginModel loginModel, LoginJFXController loginJFXController) {
        this.loginJFXController = loginJFXController;
        this.view = loginView;
        this.model = loginModel;

        setupListeners();
    }

    private void setupListeners() {
        loginJFXController.setLoginButtonListener(() -> {
            String username = loginJFXController.getUsername();
            String password = loginJFXController.getPassword();

            boolean success = model.login(username, password);
            if (success) {
                System.out.println("[DEBUG] Sent login request for user: " + username);
            } else {
                loginJFXController.showAlert("❌ Failed to send login request.");
            }
        });

        // Handle messages from the server
        model.setMessageListener(msg -> {
            // Run on UI thread to safely update UI
            javafx.application.Platform.runLater(() -> {
                if (msg.equals("LOGIN_SUCCESS")) {
                    loginJFXController.showAlert("✅ Login successful!");
                } else if (msg.equals("LOGIN_FAILED")) {
                    loginJFXController.showAlert("❌ Invalid credentials.");
                } else {
                    System.out.println("[SERVER MESSAGE] " + msg);
                }
            });
        });
    }
}
