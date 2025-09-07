package ClientMVC;

import java.io.*;
import java.net.Socket;

public class NetworkManager {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            return true;
        } catch (IOException e) {
            System.out.println("Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

}