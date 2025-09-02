package ClientMVC;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class ClientModel implements Runnable {
    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private Consumer<String> messageListener;

    public ClientModel(Socket socket, String username) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public void sendMSG(String message) {
        try {
            writer.write(message);
            writer.newLine();
            writer.flush();
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
            while ((msg = reader.readLine()) != null) {
                if (messageListener != null) {
                    messageListener.accept(msg);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
