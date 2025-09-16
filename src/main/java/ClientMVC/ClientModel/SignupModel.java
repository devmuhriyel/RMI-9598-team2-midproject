package ClientMVC.ClientModel;

import ClientMVC.ClientController.SignupController;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class SignupModel {
    private final Socket socket;
    private PrintWriter out;

    public SignupModel(Socket socket) {
        this.socket = socket;
        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void signup(String username, String password) {
        String msg = "<Request type=\"SIGNUP\"><content>" + username + "waw" + password + "</content></Request>";
        out.println(msg);
    }

    public Socket getSocket() {
        return socket;
    }
}
