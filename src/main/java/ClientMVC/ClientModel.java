package ClientMVC;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;

public class ClientModel implements Runnable {
    private final Socket socket;
    private final BufferedReader readMSG;
    private final BufferedWriter writeMSG;
    private Consumer<String> messageListener;
    private String name;
    private static ArrayList<ClientModel> clients = new ArrayList<ClientModel>();

    public ClientModel(Socket socket, String username) throws IOException {
        this.socket = socket;
        this.name = username;
        this.readMSG = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writeMSG = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        writeMSG.write(name);
        writeMSG.newLine();
        writeMSG.flush();
    }

    public void sendMSG(String msg) {
        try {
            writeMSG.write(name + ": " + msg);
            writeMSG.newLine();
            writeMSG.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ✅ background thread for receiving
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

    public void setMessageListener(Consumer<String> listener) {
        this.messageListener = listener;
    }
}
