package ClientMVC.ClientModel;

import ClientMVC.UserXMLHandler;
import javafx.application.Platform;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;

public class SignupModel implements Runnable {
    private final Socket socket;
    private final BufferedReader readMSG;
    private final BufferedWriter writeMSG;
    private Consumer<String> messageListener;
    private String name;
    private static ArrayList<SignupModel> clients = new ArrayList<>();
    private UserXMLHandler userXMLHandler;
    private volatile boolean shouldStop = false;

    public SignupModel(Socket socket) throws IOException {
        this.socket = socket;
        this.readMSG = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writeMSG = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        clients.add(this);
    }

    // === MVC METHODS ===
    public void register(String username, String password) {
        try {
            String message = "<Request><type>REGISTER</type><content>" + username + "![]!" + password + "</content></Request>";
            writeMSG.write(message);
            writeMSG.newLine();
            writeMSG.flush();
            name = username;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopListening() {
        shouldStop = true;
    }

    @Override
    public void run() {
        UserXMLHandler xmlHandler = new UserXMLHandler();
        try {
            String msg;
            while ((msg = readMSG.readLine()) != null) {
                if (msg.trim().startsWith("<")) {
                    String finalMsg = msg.trim();

                    try {
                        String reply = xmlHandler.getReply(finalMsg);
                        if (reply != null) {
                            switch (reply) {
                                case "REGISTER_SUCCESS":
                                    System.out.println("Registration successful for: " + xmlHandler.getResponseBody(finalMsg));
                                    shouldStop = true;
                                    break;

                                case "USER_EXISTS":
                                    System.out.println("Registration failed: Username already exists.");
                                    break;

                                case "REGISTER_FAILED":
                                    System.out.println("Registration failed due to server error.");
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error parsing server response: " + e.getMessage());
                    }

                    if (messageListener != null) {
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

    // === SETTERS AND GETTERS ===
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
    public void stopReadWrite() throws IOException {
        readMSG.close();
        writeMSG.close();
    }
}
