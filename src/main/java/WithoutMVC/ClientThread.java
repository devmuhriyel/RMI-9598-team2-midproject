package WithoutMVC;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientThread implements Runnable {

    private Socket socket;
    private BufferedReader readMSG;
    private BufferedWriter writeMSG;
    private String name;
    private static ArrayList<ClientThread> clients = new ArrayList<ClientThread>();

    public ClientThread(Socket socket) {
        try{
            this.socket = socket;
            this.writeMSG = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.readMSG = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.name = readMSG.readLine();
            clients.add(this);
            for (ClientThread c : clients) {
                c.writeMSG.write("SERVER: " + this.name + " has connected to the chat");
                c.writeMSG.newLine();
                c.writeMSG.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        String msgFromClient;

        while(socket.isConnected()) {
            try{
                msgFromClient = readMSG.readLine();
                for (ClientThread c : clients) {
                    c.writeMSG.write(msgFromClient);
                    c.writeMSG.newLine();
                    c.writeMSG.flush();
                }
            } catch (Exception e){
                e.printStackTrace();
                break;
            }
        }
    }

    public void logOut() throws IOException {
        clients.remove(this);
        for (ClientThread c : clients) {
            c.writeMSG.write("SERVER: " + this.name + " has connected to the chat");
            c.writeMSG.newLine();
            c.writeMSG.flush();
        }
    }
}
