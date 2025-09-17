package ClientMVC.ClientController;

import ClientMVC.ClientModel.ChatModel;
import ClientMVC.ClientModel.HomeModel;
import ClientMVC.ClientModel.LoginModel;
import ClientMVC.ClientView.Chat.ChatView;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.ClientView.Login.LoginJFXController;
import ClientMVC.ClientView.Login.LoginView;
import ClientMVC.UserXMLHandler;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    private final LoginJFXController loginJFXController;
    private final LoginView view;
    private final LoginModel model;
    private final Thread modelThread;
    private UserXMLHandler userXMLHandler = new UserXMLHandler();

    public LoginController(LoginView loginView, LoginModel loginModel, LoginJFXController loginJFXController) {
        this.loginJFXController = loginJFXController;
        this.view = loginView;
        this.model = loginModel;
        model.setMessageListener(this::login);
        modelThread = new Thread(model, "Login Listener");
        modelThread.start();
        initialize();
    }

    private void initialize() {
        loginJFXController.setLoginButtonListener(() -> {
            String username = loginJFXController.getUsername();
            String password = loginJFXController.getPassword();
            model.login(username, password);
            model.setName(username);
        });
    }

    public void login(String msg) {
        try {
            String reply = userXMLHandler.getReply(msg.trim());
            if (reply != null && reply.equals("LOGIN_SUCCESS")) {
                view.login();
                Platform.runLater(() -> {
                    try {
                        HomeView homeView = new HomeView();
                        Stage stage1 = new Stage();
                        homeView.start(stage1);

                        HomeModel homeModel = new HomeModel(model.getSocket(), loginJFXController.getUsername());
                        new HomeController(homeModel, homeView, homeView.getFXController());
                        new Thread(homeModel,"Home thread").start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                });
            } else if (reply != null && reply.equals("LOGIN_FAILED")) {
                System.out.println("Login failed!");
                Platform.runLater(() -> {
                    System.out.println("Invalid username or password");
                });
            }
        } catch (Exception e) {
            System.out.println("Error processing login response: " + e.getMessage());
            e.printStackTrace();
        }
    }
}