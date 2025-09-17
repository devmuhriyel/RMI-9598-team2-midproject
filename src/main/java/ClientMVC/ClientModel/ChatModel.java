package ClientMVC.ClientModel;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;

public class ChatModel implements Runnable {
    private final Socket socket;
    private final BufferedReader readMSG;
    private final BufferedWriter writeMSG;
    private Consumer<String> messageListener;
    private String name;
    private static ArrayList<ChatModel> clients = new ArrayList<ChatModel>();

    public ChatModel(Socket socket, String username) throws IOException {
        this.socket = socket;
        this.readMSG = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writeMSG = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.name = username;
        clients.add(this);

    }

    public void sendMSG(String message, String receiver) {
        try {
            String xmlMessage = "<Request><type>SEND_MESSAGE</type><content>" + name + "![]!" + receiver + "![]!" + message + "</content></Request>";
            System.out.println(xmlMessage);
            writeMSG.write(xmlMessage);
            writeMSG.newLine();
            writeMSG.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Add this method to your ChatModel class

    public void sendGroupMessage(String message, String groupName) {
        try {
            String xmlMessage = "<Request><type>SEND_GROUP_MESSAGE</type><content>" + name + "![]!" + groupName + "![]!" + message + "</content></Request>";
            System.out.println(xmlMessage);
            writeMSG.write(xmlMessage);
            writeMSG.newLine();
            writeMSG.flush();
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
            while ((msg = readMSG.readLine()) != null) {
                if (messageListener != null) {
                    String message = parseMessageFromXML(msg);
                    if (message != null && !message.isEmpty()) {
                        messageListener.accept(message);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String parseMessageFromXML(String xmlMsg) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new ByteArrayInputStream(xmlMsg.getBytes("UTF-8")));

            org.w3c.dom.NodeList bodyNodes = doc.getElementsByTagName("body");
            if (bodyNodes.getLength() > 0) {
                return bodyNodes.item(0).getTextContent();
            }
        } catch (Exception e) {
            return xmlMsg;
        }
        return null;
    }
}
