package ClientMVC.ClientModel;

import ClientMVC.ClientController.ClientController;
import ClientMVC.ClientView.Chat.ClientView;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;

public class LoginModel implements Runnable {
    private final Socket socket;
    private final BufferedReader readMSG;
    private final BufferedWriter writeMSG;
    private Consumer<String> messageListener;
    private String name;
    private static ArrayList<LoginModel> clients = new ArrayList<>();

    public LoginModel(Socket socket) throws IOException {
        this.socket = socket;
        this.readMSG = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writeMSG = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        clients.add(this);
    }

    public boolean login(String username, String password) {
        try {
            String message = "LOGIN:" + username + ":" + password;
            writeMSG.write(message);
            writeMSG.newLine();
            writeMSG.flush();
            Platform.startup(() -> {
                try{
                    ClientView cView = new ClientView();
                    Stage stage = new Stage();
                    cView.start(stage);
                    ClientModel cModel = new ClientModel(getSocket(), username);
                    new ClientController(cModel, cView, cView.getFXController());
                    new Thread(cModel, "Logged In").start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void setMessageListener(Consumer<String> listener) {
        this.messageListener = listener;
    }

    @Override
    public void run() {
        try {
            String msg;
            while ((msg = readMSG.readLine()) != null) {
                if (messageListener != null) {
                    messageListener.accept(msg);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Socket getSocket() {
        return socket;
    }
}
