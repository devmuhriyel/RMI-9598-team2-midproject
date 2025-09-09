package WithoutMVC;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ClientThread implements Runnable {
    private Socket socket;
    private BufferedReader readMSG;
    private BufferedWriter writeMSG;
    private String name;
    private static ArrayList<ClientThread> clients = new ArrayList<>();

    private static final Map<String, String> userDatabase = new HashMap<>();
    static {
        userDatabase.put("testuser", "password");
        userDatabase.put("alice", "12345");
        userDatabase.put("bob", "qwerty");
    }
    public ClientThread(Socket socket) {
        try {
            this.socket = socket;
            this.writeMSG = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.readMSG = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            // 🔑 Step 1: wait for login message
            String firstMsg = readMSG.readLine();
            if (firstMsg != null && firstMsg.startsWith("LOGIN:")) {
                String[] parts = firstMsg.split(":");
                if (parts.length == 3) {
                    String username = parts[1];
                    String password = parts[2];

                    if (authenticate(username, password)) {
                        this.name = username;
                        sendMessage("LOGIN_SUCCESS");
                        System.out.println("User logged in: " + username);

                        synchronized (clients) {
                            clients.add(this);
                        }
                        broadcast("SERVER: " + username + " has connected.");
                    } else {
                        sendMessage("LOGIN_FAILED");
                        socket.close();
                        return; // end thread
                    }
                } else {
                    sendMessage("LOGIN_FAILED");
                    socket.close();
                    return;
                }
            } else {
                sendMessage("LOGIN_FAILED");
                socket.close();
                return;
            }

            // 🔑 Step 2: handle chat messages
            String msgFromClient;
            while ((msgFromClient = readMSG.readLine()) != null) {
                broadcast(this.name + ": " + msgFromClient);
            }

        } catch (IOException e) {
            // client disconnected
        } finally {
            try {
                logOut();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcast(String message) {
        synchronized (clients) {
            for (ClientThread c : clients) {
                try {
                    c.writeMSG.write(message);
                    c.writeMSG.newLine();
                    c.writeMSG.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean authenticate(String username, String password) {
        return userDatabase.containsKey(username) &&
                userDatabase.get(username).equals(password);
    }

    private void sendMessage(String msg) {
        try {
            writeMSG.write(msg);
            writeMSG.newLine();
            writeMSG.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logOut() throws IOException {
        synchronized (clients) {
            clients.remove(this);
        }
        broadcast("SERVER: " + this.name + " has disconnected.");
        socket.close();
    }
}
