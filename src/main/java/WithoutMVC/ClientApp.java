package WithoutMVC;

import ClientMVC.ClientController.ClientController;
import ClientMVC.ClientModel.ClientModel;
import ClientMVC.ClientView.Chat.ClientView;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientApp {
    private Socket socket;
    private BufferedReader readMSG;
    private BufferedWriter writeMSG;
    private String name;

    public ClientApp(Socket sckt, String name) {
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

    public static void main(String[] args) throws IOException {
        Scanner in = new Scanner(System.in);
        System.out.println("Please enter your name: ");
        String name = in.nextLine();
        Socket sckt = new Socket("localhost", 1234);
        ClientApp clientApp = new ClientApp(sckt, name);
        clientApp.receiveMSG();
        clientApp.sendMSG();

        ClientView view = new ClientView();
        ClientModel model = new ClientModel(sckt, name);
        ClientController controller = new ClientController(model, view, view.getFxController());
    }
}
