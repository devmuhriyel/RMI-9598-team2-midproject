package ClientMVC.ClientController;

import ClientMVC.ClientModel.LoginModel;
import ClientMVC.ClientModel.SignupModel;
import ClientMVC.ClientView.Login.LoginView;
import ClientMVC.ClientView.Signup.SignupJFXController;
import ClientMVC.ClientView.Signup.SignupView;
import ClientMVC.UserXMLHandler;
import javafx.application.Platform;
import javafx.stage.Stage;

public class SignupController {
    private final SignupJFXController signupJFXController;
    private final SignupView view;
    private final SignupModel model;
    private final UserXMLHandler userXMLHandler = new UserXMLHandler();

    public SignupController(SignupModel signupModel, SignupView signupView, SignupJFXController signupJFXController) {
        this.signupJFXController = signupJFXController;
        this.view = signupView;
        this.model = signupModel;

        // listen for server responses
        model.setMessageListener(this::handleServerResponse);

        // run the model in a thread
        new Thread(model, "Signup Listener").start();

        initialize();
    }

    private void initialize() {
        // SIGNUP BUTTON
        signupJFXController.setSignupButtonListener(() -> {
            String username = signupJFXController.getUsername();
            String password = signupJFXController.getPassword();
            String confirmPassword = signupJFXController.getConfirmPassword();

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                signupJFXController.setMessage("All fields are required.");
                return;
            }
            if (!password.equals(confirmPassword)) {
                signupJFXController.setMessage("Passwords do not match.");
                return;
            }

            // send register request to server
            model.register(username, password);
        });

        // SIGN IN BUTTON
        signupJFXController.setSignInListener(() -> {
            Platform.runLater(() -> {
                try {
                    view.getPrimaryStage().close();

                    LoginView loginView = new LoginView();
                    Stage loginStage = new Stage();
                    loginView.start(loginStage);

                    LoginModel loginModel = new LoginModel(model.getSocket()); // reuse same socket
                    new LoginController(loginView, loginModel, loginView.getFXController());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        });
    }

    private void handleServerResponse(String msg) {
        try {
            String reply = userXMLHandler.getReply(msg.trim());
            if (reply != null) {
                switch (reply) {
                    case "REGISTER_SUCCESS":
                        Platform.runLater(() -> {
                            signupJFXController.setMessage("Account created! Please login.");
                            try {
                                view.getPrimaryStage().close();
                                LoginView loginView = new LoginView();
                                Stage loginStage = new Stage();
                                loginView.start(loginStage);

                                LoginModel loginModel = new LoginModel(model.getSocket());
                                new LoginController(loginView, loginModel, loginView.getFXController());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                        break;

                    case "USER_EXISTS":
                        Platform.runLater(() ->
                                signupJFXController.setMessage("Username already exists.")
                        );
                        break;

                    case "REGISTER_FAILED":
                        Platform.runLater(() ->
                                signupJFXController.setMessage("Registration failed. Try again.")
                        );
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("Error processing signup response: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
