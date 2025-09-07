package Server;

import WithoutMVC.ClientThread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ChatServer {

    private ServerSocket serverSocket;
    private boolean running = true;

    public ChatServer(ServerSocket serverSckt) {
        this.serverSocket = serverSckt;
    }

    public void start() {
        try{
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


    //TYPE EXIT IF CLOSEEEE OR PWEDE NAMAN YUNG STOP BUTTON (Wala maangas lang kaya i added it :>)
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(1234);
        ChatServer chatServer = new ChatServer(serverSocket);
        chatServer.start();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            String command = scanner.nextLine();
            if (command.equalsIgnoreCase("exit")) {
                chatServer.stop();
                break;
            }
        }
    }
}
