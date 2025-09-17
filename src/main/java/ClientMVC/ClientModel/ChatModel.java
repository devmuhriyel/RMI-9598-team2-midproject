package ClientMVC.ClientModel;

import ClientMVC.UserXMLHandler;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.function.Consumer;

public class ChatModel implements Runnable {
    private final Socket socket;
    private final BufferedReader readMSG;
    private final BufferedWriter writeMSG;
    private Consumer<String> messageListener;
    private String name;
    private static ArrayList<ChatModel> clients = new ArrayList<ChatModel>();
    private UserXMLHandler xmlHandler = new UserXMLHandler();

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
                System.out.println("ChatModel received: " + msg);

                if (msg.startsWith("<")) {
                    String reply = xmlHandler.getReply(msg);

                    if ("SEND_MESSAGE".equals(reply) || "SEND_GROUP_MESSAGE".equals(reply)) {
                        // Get the Base64 encoded XML content
                        String base64Content = xmlHandler.getBody(msg);

                        if (base64Content != null && !base64Content.isEmpty()) {
                            // Save the updated XML file
                            xmlHandler.saveUpdatedAccountXML(name, base64Content);

                            // Extract the latest message from the decoded XML and notify UI
                            String latestMessage = extractLatestMessage(base64Content);
                            if (latestMessage != null && messageListener != null) {
                                messageListener.accept(latestMessage);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractLatestMessage(String base64Content) {
        try {
            // Decode the Base64 content
            byte[] decodedXML = Base64.getDecoder().decode(base64Content);
            String xmlContent = new String(decodedXML, "UTF-8");

            // Parse the XML to find the latest message
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes("UTF-8")));

            // Get all chat elements
            org.w3c.dom.NodeList chatNodes = doc.getElementsByTagName("chat");

            if (chatNodes.getLength() > 0) {
                // Get the last chat element (most recent message)
                org.w3c.dom.Element lastChat = (org.w3c.dom.Element) chatNodes.item(chatNodes.getLength() - 1);

                // Extract sender and message
                String sender = lastChat.getElementsByTagName("sender").item(0).getTextContent();
                String message = lastChat.getElementsByTagName("msg").item(0).getTextContent();

                // Only return messages that are NOT from the current user (to avoid showing own messages again)
                if (!sender.equals(name)) {
                    return message;
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting latest message: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
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