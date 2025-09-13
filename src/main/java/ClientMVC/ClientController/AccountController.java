package ClientMVC.ClientController;

import ClientMVC.ClientModel.AccountModel;
import java.io.BufferedWriter;
import java.io.IOException;

public class AccountController {
    private AccountModel model;
    private BufferedWriter serverWriter;

    //This for the Friend Function
    public AccountController(AccountModel model, BufferedWriter serverWriter) {
        this.model = model;
        this.serverWriter = serverWriter;
    }

    public void listRequests(String user) throws IOException {
        String msg = "<Request type=\"LIST_REQUESTS\"><content>" + user + "</content></Request>";
        sendMessage(msg);
    }

    public void acceptRequest(String user, String friend) throws IOException {
        String msg = "<Request type=\"ACCEPT_REQUEST\"><content>" + user + "waw" + friend + "</content></Request>";
        sendMessage(msg);
    }

    public void declineRequest(String user, String friend) throws IOException {
        String msg = "<Request type=\"DECLINE_REQUEST\"><content>" + user + "waw" + friend + "</content></Request>";
        sendMessage(msg);
    }

    public void updateRequests(java.util.List<String> requests) {
        model.setFriendRequests(requests);
    }

    private void sendMessage(String msg) throws IOException {
        serverWriter.write(msg);
        serverWriter.newLine();
        serverWriter.flush();
    }
}


