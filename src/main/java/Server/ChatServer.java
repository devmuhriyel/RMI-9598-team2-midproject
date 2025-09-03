package Server;

import WithoutMVC.ClientThread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer {
    private ServerSocket serverSocket;

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

    public void close(){
        try{
            while(serverSocket != null){
                serverSocket.close();
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(1234);
        ChatServer chatServer = new ChatServer(serverSocket);
        chatServer.start();
    }
}
