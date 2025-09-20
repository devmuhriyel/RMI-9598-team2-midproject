package Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ChatServer {

    private final ServerSocket serverSocket;
    private boolean running = true;

    public ChatServer(ServerSocket serverSckt) {
        this.serverSocket = serverSckt;
    }

    public void start() {
        try{
            ServerXMLHandler xmlHandler = new ServerXMLHandler();
            xmlHandler.fixExistingGroupMemberships();
            while(!serverSocket.isClosed()){
                Socket socket = serverSocket.accept();
                System.out.println("A new client has connected");

                ClientThread clientThread = new ClientThread(socket);
                Thread thread = new Thread(clientThread);
                thread.start();
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            System.out.println("Server stopped.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws IOException {
        String ip = "";
        InetAddress bindAddress = InetAddress.getByName(ip);
        ServerSocket serverSocket = new ServerSocket(1234, 50, bindAddress);
        ChatServer chatServer = new ChatServer(serverSocket);
        chatServer.start();
        Scanner scanner = new Scanner(System.in);
        String command = "";
        while (!command.equals("exit")) {
            command = scanner.nextLine();
            if (command.equalsIgnoreCase("exit")) {
                chatServer.stop();
                break;
            }
        }
    }
}
