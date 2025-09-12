package ClientMVC.ClientModel;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;

public class ChatModel implements Runnable {
    private final Socket socket;
    private final BufferedReader readMSG;
    private final BufferedWriter writeMSG;
    private Consumer<String> messageListener;
    private String name;
    private static ArrayList<ChatModel> clients = new ArrayList<ChatModel>();

    public ChatModel(Socket socket, String username) throws IOException {
        this.socket = socket;
        this.readMSG = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writeMSG = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.name = username;
        clients.add(this);
        for (ChatModel c : clients) {
            c.writeMSG.write("SERVER: " + this.name + " has connected to the chat");
            c.writeMSG.newLine();
            c.writeMSG.flush();
        }
    }

    public void sendMSG(String message) {
        try {
            writeMSG.write(message);
            writeMSG.newLine();
            writeMSG.flush();
        } catch (IOException e) {
            e.printStackTrace();
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
}
