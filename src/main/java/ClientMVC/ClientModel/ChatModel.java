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
    private Consumer<String> groupMessageListener;
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
            System.out.println("Sending message: " + xmlMessage);
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
            System.out.println("Sending group message: " + xmlMessage);
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

    public void setGroupMessageListener(Consumer<String> listener) {
        this.groupMessageListener = listener;
    }

    @Override
    public void run() {
        String msg;
        try {
            while ((msg = readMSG.readLine()) != null) {
                System.out.println("HomeModel received: " + msg); // Debug log

                if (msg.startsWith("<")) {
                    String trimmed = msg.trim();
                    String reply = xmlHandler.getReply(msg);
                    System.out.println("Reply type: " + reply); // Debug log

                    if (reply != null) {
                        switch (reply) {
                            case "GET_CHAT_HISTORY":
                                String body = xmlHandler.getBody(msg);
                                if (body != null && !body.isEmpty() && body.equals("CHAT")) {

                                }
                                break;
                            case "FORCE_LOGOUT":
                                System.out.println("Logged out from another location");
                                break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error in HomeModel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isValidBase64(String base64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            // Additional check: try to parse as UTF-8 string
            String xmlContent = new String(decoded, "UTF-8");
            return xmlContent.startsWith("<") && xmlContent.contains("</");
        } catch (Exception e) {
            System.err.println("Base64 validation failed: " + e.getMessage());
            return false;
        }
    }

    private String extractLatestMessageFromBase64(String base64Content) {
        try {
            // Decode the Base64 content
            byte[] decodedXML = Base64.getDecoder().decode(base64Content);
            String xmlContent = new String(decodedXML, "UTF-8");
            System.out.println("Decoded XML content preview: " + xmlContent.substring(0, Math.min(200, xmlContent.length())));

            // Parse the XML to find the latest message
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes("UTF-8")));
            doc.getDocumentElement().normalize();

            // Get all chat elements from all friends and groups
            org.w3c.dom.NodeList allChatNodes = doc.getElementsByTagName("chat");

            if (allChatNodes.getLength() > 0) {
                // Get the last chat element (most recent message)
                org.w3c.dom.Element lastChat = (org.w3c.dom.Element) allChatNodes.item(allChatNodes.getLength() - 1);

                // Extract sender and message
                String sender = lastChat.getElementsByTagName("sender").item(0).getTextContent();
                String message = lastChat.getElementsByTagName("msg").item(0).getTextContent();

                System.out.println("Latest message found - Sender: " + sender + ", Message: " + message + ", Current user: " + name);

                // Only return messages that are NOT from the current user (to avoid showing own messages again)
                if (!sender.equals(name)) {
                    return sender + ": " + message;
                } else {
                    System.out.println("Skipping own message");
                }
            } else {
                System.out.println("No chat messages found in XML");
            }
        } catch (Exception e) {
            System.err.println("Error extracting latest message: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Helper method to get the current username
    public String getName() {
        return name;
    }

    // Helper method to close connection
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}