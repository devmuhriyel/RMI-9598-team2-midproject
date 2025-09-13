package ClientMVC.ClientModel;

import javafx.application.Platform;
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

    //MVC METHODS
    public void login(String username, String password) {
        try {
            String message = "<Request><type>LOGIN</type><content>" + username + "waw" + password + "</content></Request>";
            writeMSG.write(message);
            writeMSG.newLine();
            writeMSG.flush();
            name = username;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String msg;
            while ((msg = readMSG.readLine()) != null) {
                System.out.println("Received: " + msg);
                if (msg.trim().startsWith("<")) {
                    if (messageListener != null) {
                        String finalMsg = msg.trim();
                        Platform.runLater(() -> messageListener.accept(finalMsg));
                    }
                } else {
                    System.out.println("Server message: " + msg);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //SETTERS AND GETTERS
    public void setMessageListener(Consumer<String> listener) {
        this.messageListener = listener;
    }
    public Socket getSocket() {
        return socket;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getUsername() {
        return name;
    }

}