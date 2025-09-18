package ClientMVC.ClientController;

import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Admin.AdminPage;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.ClientView.Login.LoginJFXController;
import ClientMVC.ClientView.Login.LoginView;
import ClientMVC.ClientView.Signup.SignupView;
import ClientMVC.UserXMLHandler;
import javafx.application.Platform;
import javafx.stage.Stage;

public class LoginController {
    private final LoginJFXController loginJFXController;
    private final LoginView view;
    private final UnifiedClientModel model;
    private UserXMLHandler userXMLHandler = new UserXMLHandler();

    public LoginController(LoginView loginView, UnifiedClientModel unifiedModel, LoginJFXController loginJFXController) {
        this.loginJFXController = loginJFXController;
        this.view = loginView;
        this.model = unifiedModel;

        // Set up message listener for login responses
        model.setMessageListener(this::handleLoginResponse);
        initialize();
    }

    private void initialize() {
        loginJFXController.setLoginButtonListener(() -> {
            loginJFXController.getLoginButton().setDisable(true);

            String username = loginJFXController.getUsername();
            String password = loginJFXController.getPassword();
            model.login(username, password);
        });

        loginJFXController.setSignupLinkListener(() -> {
            Platform.runLater(() -> {
                try {
                    LoginView.getPrimaryStage().close();

                    SignupView signupView = new SignupView();
                    Stage signupStage = new Stage();
                    signupView.start(signupStage);

                    // Pass the unified model to signup controller
                    new SignupController(model, signupView, signupView.getFXController());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        });
    }

    public void handleLoginResponse(String msg) {
        try {
            String reply = userXMLHandler.getReply(msg.trim());

            if (reply != null && reply.equals("LOGIN_SUCCESS")) {
                String username = loginJFXController.getUsername();

                if (username.equalsIgnoreCase("admin")) {

                    Platform.runLater(() -> {
                        try {
                            AdminPage adminPage = new AdminPage();
                            Stage stage = new Stage();
                            adminPage.start(stage);

                            // Hook controller if needed
                            new AdminController(adminPage.getFXController());

                            // Close login window
                            LoginView.getPrimaryStage().close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    view.login();
                    Platform.runLater(() -> {
                        try {
                            HomeView homeView = new HomeView();
                            Stage stage1 = new Stage();
                            homeView.start(stage1);

                            // Pass the same unified model to home controller
                            new HomeController(model, homeView, homeView.getFXController());

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            } else if (reply != null && reply.equals("LOGIN_FAILED")) {
                Platform.runLater(() -> {
                    System.out.println("Invalid username or password");
                    loginJFXController.getLoginButton().setDisable(false);
                });
            }
        } catch (Exception e) {
            System.out.println("Error processing login response: " + e.getMessage());
            e.printStackTrace();
        }
    }
}