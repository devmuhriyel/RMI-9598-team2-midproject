package ClientMVC.ClientController;

import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Admin.AdminPage;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.ClientView.Login.LoginJFXController;
import ClientMVC.ClientView.Login.LoginView;
import ClientMVC.ClientView.Signup.SignupView;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class LoginController {
    private final LoginJFXController loginJFXController;
    private final LoginView view;
    private final UnifiedClientModel model;

    public LoginController(LoginView loginView, UnifiedClientModel unifiedModel, LoginJFXController loginJFXController) {
        this.loginJFXController = loginJFXController;
        this.view = loginView;
        this.model = unifiedModel;

        model.setMessageListener(this::handleLoginResponse);
        initialize();
    }

    private void initialize() {
        loginJFXController.setLoginButtonListener(() -> {
            loginJFXController.getLoginButton().setDisable(true);

            String username = loginJFXController.getUsername();
            String password = loginJFXController.getPassword();

            // Instead of model.login → directly check here
            String role = checkCredentials(username, password);

            if (role != null) {
                handleLoginResponse(role); // role will be "admin" or "user"
            } else {
                Platform.runLater(() -> {
                    System.out.println("Invalid username or password");
                    loginJFXController.getLoginButton().setDisable(false);
                });
            }
        });

        loginJFXController.setSignupLinkListener(() -> {
            Platform.runLater(() -> {
                try {
                    LoginView.getPrimaryStage().close();

                    SignupView signupView = new SignupView();
                    Stage signupStage = new Stage();
                    signupView.start(signupStage);

                    new SignupController(model, signupView, signupView.getFXController());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        });
    }

    // ✅ This method reads User.xml and checks credentials
    private String checkCredentials(String username, String password) {
        try {
            File file = new File("src/main/resources/XMLFiles/Users.xml"); // adjust path if needed
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();

            NodeList userList = doc.getElementsByTagName("user");

            for (int i = 0; i < userList.getLength(); i++) {
                Node node = userList.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element userElement = (Element) node;

                    String xmlUsername = userElement.getElementsByTagName("username").item(0).getTextContent();
                    String xmlPassword = userElement.getElementsByTagName("password").item(0).getTextContent();

                    if (xmlUsername.equalsIgnoreCase(username) && xmlPassword.equals(password)) {
                        if (userElement.getElementsByTagName("role").getLength() > 0) {
                            return userElement.getElementsByTagName("role").item(0).getTextContent();
                        } else {
                            return "user"; // default role
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // invalid login
    }

    // ✅ Simplified handleLoginResponse: uses role instead of XML handler
    public void handleLoginResponse(String role) {
        try {
            if ("admin".equalsIgnoreCase(role)) {
                Platform.runLater(() -> {
                    try {
                        AdminPage adminPage = new AdminPage();
                        Stage stage = new Stage();
                        adminPage.start(stage);

                        new AdminController(adminPage.getFXController());

                        LoginView.getPrimaryStage().close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else if ("user".equalsIgnoreCase(role)) {
                view.login();
                Platform.runLater(() -> {
                    try {
                        HomeView homeView = new HomeView();
                        Stage stage1 = new Stage();
                        homeView.start(stage1);

                        new HomeController(model, homeView, homeView.getFXController());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            System.out.println("Error processing login response: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
