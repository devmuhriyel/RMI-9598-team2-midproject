package ClientMVC.ClientModel;

import ClientMVC.ClientController.LoginController;
import ClientMVC.ClientView.Login.LoginView;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class ConnectionModel {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String ip;
    private int port;
    private boolean connected;

    //MVC METHODS
    public boolean connect(String ip, int port) {
        // reset state before new attempt
        this.connected = false;
        this.socket = null;
        this.in = null;
        this.out = null;

        try {
            String ipv4Pattern =
                    "^(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)" +
                            "(\\.(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)){3}$";

            if (ip.matches(ipv4Pattern) && port >= 1024 && port <= 65535) {
                socket = new Socket(ip, port);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                this.ip = ip;
                this.port = port;
                this.connected = true;

                Platform.runLater(() -> {
                    try {
                        LoginView lView = new LoginView();
                        Stage stage = new Stage();
                        lView.start(stage);
                        LoginModel lModel = new LoginModel(getSocket());
                        new LoginController(lView, lModel, lView.getFXController());
                        new Thread(lModel, "Logged In").start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                return true;
            } else {
                return false;
            }

        } catch (IOException e) {
            this.connected = false;
            this.socket = null;
            this.in = null;
            this.out = null;
            return false;
        }
    }

    //SETTERS AND GETTERS
    public Socket getSocket() { return socket; }
}

