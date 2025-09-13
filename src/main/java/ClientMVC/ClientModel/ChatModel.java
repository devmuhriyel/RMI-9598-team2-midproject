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
        for (ChatModel c : clients) {
            c.writeMSG.write("SERVER: " + this.name + " has connected to the chat");
            c.writeMSG.newLine();
            c.writeMSG.flush();
        }
    }

    public void sendMSG(String message) {
        try {
            // Send message in XML format as expected by server
            String xmlMessage = "<Request><action>SEND_MESSAGE</action><content>" + name + "waw" + message + "</content></Request>";
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
                    // Parse XML response to extract the actual message
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
            
            // Check if it's a message response
            org.w3c.dom.NodeList bodyNodes = doc.getElementsByTagName("body");
            if (bodyNodes.getLength() > 0) {
                return bodyNodes.item(0).getTextContent();
            }
        } catch (Exception e) {
            // If XML parsing fails, return the raw message
            return xmlMsg;
        }
        return null;
    }
}
