package ClientMVC.ClientModel;

import ClientMVC.ClientController.MainLoginController;
import ClientMVC.ClientView.LoginController;
import ClientMVC.ClientView.MainLogin;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class ClientModelConnection{
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private String ip;
    private int port;
    private boolean connected;


    public boolean connect(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            this.ip = ip;
            this.port = port;
            this.connected = true;
            Platform.startup(() -> {
                try{
                    MainLogin lView = new MainLogin();
                    Stage stage = new Stage();
                    lView.start(stage);
                    MainLoginModel lModel = new MainLoginModel(getSocket());
                    new LoginController(lView, lModel, lView.getFXController());
                    new Thread(lModel, "Logged In").start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            this.connected = false;
            return false;
        }
    }

    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
        this.connected = false;
    }


    // === Getters ===
    public boolean isConnected() { return connected; }
    public String getIp() { return ip; }
    public int getPort() { return port; }
    public Socket getSocket() { return socket; }
}

