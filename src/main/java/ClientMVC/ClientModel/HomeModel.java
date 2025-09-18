package ClientMVC.ClientModel;

import ClientMVC.UserXMLHandler;
import eu.hansolo.tilesfx.tools.DoubleExponentialSmoothingForLinearSeries;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

public class HomeModel implements Runnable{
    private final UserXMLHandler userXMLHandler;
    private Socket socket;
    private String username;
    private final BufferedReader readMSG;
    private final BufferedWriter writeMSG;
    private Consumer<String> messageListener;

    public HomeModel(Socket socket, String username) throws IOException {
        this.userXMLHandler = new UserXMLHandler();
        this.socket = socket;
        readMSG = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writeMSG = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.username = username;
    }

    // Replace the run() method in HomeModel.java
    @Override
    public void run() {
        String msg;
        try {
            while ((msg = readMSG.readLine()) != null) {
                System.out.println("HomeModel received: " + msg); // Debug log

                if (msg.startsWith("<")) {
                    String trimmed = msg.trim();
                    String reply = userXMLHandler.getReply(msg);
                    System.out.println("Reply type: " + reply); // Debug log

                    if (reply != null) {
                        switch (reply) {
                            case "GET_CHAT_HISTORY":
                                String body = userXMLHandler.getBody(msg);
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

    private String extractLatestMessageFromBase64(String base64Content) {
        try {
            byte[] decodedXML = java.util.Base64.getDecoder().decode(base64Content);
            String xmlContent = new String(decodedXML, "UTF-8");

            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xmlContent.getBytes("UTF-8")));

            org.w3c.dom.NodeList chatNodes = doc.getElementsByTagName("chat");

            if (chatNodes.getLength() > 0) {
                org.w3c.dom.Element lastChat = (org.w3c.dom.Element) chatNodes.item(chatNodes.getLength() - 1);
                String sender = lastChat.getElementsByTagName("sender").item(0).getTextContent();
                String message = lastChat.getElementsByTagName("msg").item(0).getTextContent();

                // Only return messages that are NOT from the current user
                if (!sender.equals(username)) {
                    return message;
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting latest message: " + e.getMessage());
        }
        return null;
    }

    //MVC Methods
    public void requestChatHistory(String friendName) {
        try {
            String xmlMessage = "<Request><type>GET_CHAT_HISTORY</type><content>" +
                    username + "![]!" + friendName + "</content></Request>";
            System.out.println(xmlMessage);
            writeMSG.write(xmlMessage);
            writeMSG.newLine();
            writeMSG.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public List<String> getFriends() {
        return userXMLHandler.getFriends(username);
    }

    public List<String> getGroups() {
        return userXMLHandler.getUserGroups(username);
    }

    public void setMessageListener(Consumer<String> messageListener) {
        this.messageListener = messageListener;
    }

    // Add this method to your HomeModel class

    public void requestGroupChatHistory(String groupName) {
        try {
            String xmlMessage = "<Request><type>GET_GROUP_CHAT_HISTORY</type><content>" +
                    username + "![]!" + groupName + "</content></Request>";
            System.out.println(xmlMessage);
            writeMSG.write(xmlMessage);
            writeMSG.newLine();
            writeMSG.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //GETTERS AND SETTERS
    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}


