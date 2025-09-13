package ClientMVC.ClientController;

import ClientMVC.ClientModel.ChatModel;
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
    private UserXMLHandler userXMLHandler = new UserXMLHandler();

    public LoginController(LoginView loginView, LoginModel loginModel, LoginJFXController loginJFXController) {
        this.loginJFXController = loginJFXController;
        this.view = loginView;
        this.model = loginModel;
        model.setMessageListener(this::login);
        new Thread(model, "Login Listener").start();
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
            System.out.println("Processing login response: " + msg);
            String reply = userXMLHandler.getReply(msg.trim());

            if (reply != null && reply.equals("LOGIN_SUCCESS")) {
                System.out.println("Login successful!");
                view.login();
                Platform.runLater(() -> {
                    try {
                        HomeView homeView = new HomeView();
                        Stage stage1 = new Stage();
                        homeView.start(stage1);


                        homeView.setUsername(model.getUsername());
                        homeView.getFXController().loadFriends(model.getUsername());

                        ChatView cView = new ChatView();
                        Stage stage = new Stage();
//                        cView.start(stage);
                        ChatModel cModel = new ChatModel(model.getSocket(), loginJFXController.getUsername());
                        new ChatController(cModel, cView, cView.getFXController());
                        new Thread(cModel, "Chat Listener").start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else if (reply != null && reply.equals("LOGIN_FAILED")) {
                System.out.println("Login failed!");
                Platform.runLater(() -> {
                    System.out.println("Invalid username or password");
                });
            } else {
                System.out.println("Received non-login response or invalid XML: " + msg);
            }
        } catch (Exception e) {
            System.out.println("Error processing login response: " + e.getMessage());
            e.printStackTrace();
        }
    }
}