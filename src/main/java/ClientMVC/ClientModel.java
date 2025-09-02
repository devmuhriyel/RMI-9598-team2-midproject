package ClientMVC;

import WithoutMVC.ClientThread;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class ClientModel implements Runnable {
    private Socket socket;
    private BufferedReader readMSG;
    private BufferedWriter writeMSG;
    private String name;
    private static ArrayList<ClientModel> clients = new ArrayList<ClientModel>();

    public ClientModel(Socket sckt, String name) {
        try{
            this.socket = sckt;
            this.name = name;
            this.readMSG = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writeMSG = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void sendMSG(){
        try{
            writeMSG.write(name);
            writeMSG.newLine();
            writeMSG.flush();

            Scanner in = new Scanner(System.in);
            while(socket.isConnected()){
                String msgToSend = in.nextLine();
                writeMSG.write(name + ": " + msgToSend);
                writeMSG.newLine();
                writeMSG.flush();;
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }
    public void receiveMSG(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msgFromChat;

                while(socket.isConnected()){
                    try {
                        msgFromChat = readMSG.readLine();
                        System.out.println(msgFromChat);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    public void run() {
        String msgFromClient;

        while(socket.isConnected()) {
            try{
                msgFromClient = readMSG.readLine();
                for (ClientModel c : clients) {
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
}
