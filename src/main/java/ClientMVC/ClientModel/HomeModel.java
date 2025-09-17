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
                if (msg.startsWith("<")) {
                    String trimmed = msg.trim();
                    String reply = userXMLHandler.getReply(msg);
                    if (reply != null) {
                        switch (reply) {
                            case "GET_CHAT_HISTORY":
                                // Handle the response containing the updated XML file
                                String body = userXMLHandler.getBody(msg);
                                if (body != null && !body.isEmpty()) {
                                    // Decode the Base64 encoded XML content and save it locally
                                    userXMLHandler.saveUpdatedAccountXML(username, body);
                                }
                                break;
                            case "GET_GROUP_CHAT_HISTORY":
                                // Handle group chat history response
                                String groupBody = userXMLHandler.getBody(msg);
                                if (groupBody != null && !groupBody.isEmpty()) {
                                    userXMLHandler.saveUpdatedAccountXML(username, groupBody);
                                }
                                break;
                            case "SEND_MESSAGE":
                                // Handle incoming messages - update local XML
                                String msgBody = userXMLHandler.getBody(msg);
                                if (msgBody != null && !msgBody.isEmpty()) {
                                    userXMLHandler.saveUpdatedAccountXML(username, msgBody);
                                }
                                // Also notify message listener for UI updates
                                if (messageListener != null) {
                                    messageListener.accept(msg);
                                }
                                break;
                            case "SEND_GROUP_MESSAGE":
                                // Handle incoming group messages - update local XML
                                String groupMsgBody = userXMLHandler.getBody(msg);
                                if (groupMsgBody != null && !groupMsgBody.isEmpty()) {
                                    userXMLHandler.saveUpdatedAccountXML(username, groupMsgBody);
                                }
                                // Also notify message listener for UI updates
                                if (messageListener != null) {
                                    messageListener.accept(msg);
                                }
                                break;
                            case "FORCE_LOGOUT":
                                // Handle force logout
                                System.out.println("Logged out from another location");
                                break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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


