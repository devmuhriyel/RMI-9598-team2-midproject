package ClientMVC.ClientController;

import ClientMVC.ClientModel.AccountModel;
import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Account.AccountView;
import ClientMVC.ClientView.Account.AccountJFXController;
import ClientMVC.ClientModel.AccountModel;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.ClientView.Login.LoginView;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.IOException;

public class AccountController {
    private final UnifiedClientModel model;
    private final AccountView view;
    private final AccountJFXController accountJFXController;
    private BufferedWriter serverWriter;

    //This for the Friend Function
    public AccountController(UnifiedClientModel model, AccountView view, AccountJFXController accountJFXController) {
        this.view = view;
        this.model = model;
        this.accountJFXController = accountJFXController;
        initialize();
    }

    private void initialize() {
        // Handle back button click
        accountJFXController.setBackButtonListener(() -> {
            view.getStage().close();

            HomeView homeView = new HomeView();
            Stage homeStage = new Stage();
            homeView.start(homeStage);


            new HomeController(model, homeView, homeView.getFXController());

        });
        //handle logout

        accountJFXController.setLogoutAccountButtonListener(() -> {
            view.getStage().close();

            LoginView loginView = new LoginView();
            Stage loginStage = new Stage();
            try {
                loginView.start(loginStage);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }


            new LoginController(loginView, model, loginView.getFXController());

        });
    }


//    public void listRequests(String user) throws IOException {
//        String msg = "<Request type=\"LIST_REQUESTS\"><content>" + user + "</content></Request>";
//        sendMessage(msg);
//    }
//
//    public void acceptRequest(String user, String friend) throws IOException {
//        String msg = "<Request type=\"ACCEPT_REQUEST\"><content>" + user + "waw" + friend + "</content></Request>";
//        sendMessage(msg);
//    }
//
//    public void declineRequest(String user, String friend) throws IOException {
//        String msg = "<Request type=\"DECLINE_REQUEST\"><content>" + user + "waw" + friend + "</content></Request>";
//        sendMessage(msg);
//    }
//
//    public void updateRequests(java.util.List<String> requests) {
//        model.setFriendRequests(requests);
//    }

    private void sendMessage(String msg) throws IOException {
        serverWriter.write(msg);
        serverWriter.newLine();
        serverWriter.flush();
    }
}


