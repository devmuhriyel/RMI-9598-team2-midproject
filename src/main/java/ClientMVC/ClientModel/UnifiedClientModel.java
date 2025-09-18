package ClientMVC.ClientModel;

import ClientMVC.UserXMLHandler;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

public class UnifiedClientModel implements Runnable {
    private final Socket socket;
    private final BufferedReader readMSG;
    private final BufferedWriter writeMSG;
    private String username;
    private UserXMLHandler xmlHandler;

    // Listeners for different types of responses
    private Consumer<String> messageListener;
    private Consumer<String> chatHistoryListener;
    private Consumer<String> forceLogoutListener;
    private Consumer<String> newMessageListener;

    public UnifiedClientModel(Socket socket, String username) throws IOException {
        this.socket = socket;
        this.readMSG = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writeMSG = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.username = username;
        this.xmlHandler = new UserXMLHandler();
    }

    @Override
    public void run() {
        try {
            String msg;
            while ((msg = readMSG.readLine()) != null) {
                System.out.println("UnifiedModel received: " + msg);

                if (msg.startsWith("<")) {
                    handleXMLResponse(msg.trim());
                }
            }
        } catch (Exception e) {
            System.err.println("Error in UnifiedClientModel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleXMLResponse(String xmlResponse) {
        try {
            String reply = xmlHandler.getReply(xmlResponse);
            if (reply == null) return;

            switch (reply) {
                case "GET_CHAT_HISTORY":
                    handleChatHistoryResponse(xmlResponse, false);
                    break;

                case "NEW_MESSAGE":
                    handleNewMessageResponse(xmlResponse);
                    break;

                case "FORCE_LOGOUT":
                    if (forceLogoutListener != null) {
                        Platform.runLater(() -> forceLogoutListener.accept("Logged out from another location"));
                    }
                    break;

                case "LOGIN_SUCCESS":
                case "LOGIN_FAILED":
                case "REGISTER_SUCCESS":
                case "REGISTER_FAILED":
                    // Handle login/register responses
                    if (messageListener != null) {
                        Platform.runLater(() -> messageListener.accept(xmlResponse));
                    }
                    break;

                default:
                    System.out.println("Unhandled response type: " + reply);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error handling XML response: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleChatHistoryResponse(String xmlResponse, boolean isNewMessage) {
        try {
            String body = xmlHandler.getResponseBody(xmlResponse);
            if (body != null && !body.isEmpty()) {
                if (isValidBase64(body)) {
                    // Save the updated XML locally
                    xmlHandler.saveUpdatedAccountXML(username, body);

                    if (chatHistoryListener != null) {
                        if (isNewMessage) {
                            // Extract the latest message for real-time display
                            String latestMessage = extractLatestMessageFromBase64(body);
                            if (latestMessage != null) {
                                Platform.runLater(() -> chatHistoryListener.accept(latestMessage));
                            } else {
                                // If we can't extract the latest message, trigger a refresh
                                Platform.runLater(() -> chatHistoryListener.accept("MESSAGE_RECEIVED"));
                            }
                        } else {
                            // For GET_CHAT_HISTORY, trigger a refresh
                            Platform.runLater(() -> chatHistoryListener.accept("MESSAGE_RECEIVED"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling chat history response: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleNewMessageResponse(String xmlResponse) {
        try {
            String body = xmlHandler.getResponseBody(xmlResponse);
            if (body != null && !body.isEmpty()) {
                if (isValidBase64(body)) {
                    // Save the updated XML locally
                    xmlHandler.saveUpdatedAccountXML(username, body);

                    // Instead of trying to extract the latest message, just trigger a refresh
                    // This ensures the ChatController loads the complete, correct conversation
                    if (chatHistoryListener != null) {
                        System.out.println("NEW_MESSAGE received, triggering chat refresh");
                        Platform.runLater(() -> chatHistoryListener.accept("MESSAGE_RECEIVED"));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling new message response: " + e.getMessage());
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
                // Get the last chat message
                org.w3c.dom.Element lastChat = (org.w3c.dom.Element) chatNodes.item(chatNodes.getLength() - 1);
                String sender = lastChat.getElementsByTagName("sender").item(0).getTextContent();
                String message = lastChat.getElementsByTagName("msg").item(0).getTextContent();

                System.out.println("Extracted latest message - Sender: " + sender + ", Message: " + message);
                return sender + ": " + message;
            }
        } catch (Exception e) {
            System.err.println("Error extracting latest message: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private boolean isValidBase64(String base64) {
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(base64);
            String xmlContent = new String(decoded, "UTF-8");
            return xmlContent.contains("<") && xmlContent.contains(">");
        } catch (Exception e) {
            return false;
        }
    }

    // Communication methods
    public synchronized void sendMessage(String message, String receiver) {
        try {
            String xmlMessage = "<Request><type>SEND_MESSAGE</type><content>" +
                    username + "![]!" + receiver + "![]!" + message + "</content></Request>";
            System.out.println("Sending message: " + xmlMessage);
            writeMSG.write(xmlMessage);
            writeMSG.newLine();
            writeMSG.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void sendGroupMessage(String message, String groupName) {
        try {
            String xmlMessage = "<Request><type>SEND_GROUP_MESSAGE</type><content>" +
                    username + "![]!" + groupName + "![]!" + message + "</content></Request>";
            System.out.println("Sending group message: " + xmlMessage);
            writeMSG.write(xmlMessage);
            writeMSG.newLine();
            writeMSG.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestChatHistory(String friendName) {
        try {
            String xmlMessage = "<Request><type>GET_CHAT_HISTORY</type><content>" +
                    username + "![]!" + friendName + "</content></Request>";
            System.out.println("Requesting chat history: " + xmlMessage);
            writeMSG.write(xmlMessage);
            writeMSG.newLine();
            writeMSG.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestGroupChatHistory(String groupName) {
        try {
            String xmlMessage = "<Request><type>GET_GROUP_CHAT_HISTORY</type><content>" +
                    username + "![]!" + groupName + "</content></Request>";
            System.out.println("Requesting group chat history: " + xmlMessage);
            writeMSG.write(xmlMessage);
            writeMSG.newLine();
            writeMSG.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void login(String username, String password) {
        try {
            String message = "<Request><type>LOGIN</type><content>" + username + "![]!" + password + "</content></Request>";
            writeMSG.write(message);
            writeMSG.newLine();
            writeMSG.flush();
            this.username = username;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Add method for sending raw XML messages
    public synchronized void sendRawMessage(String xmlMessage) {
        try {
            System.out.println("Sending raw message: " + xmlMessage);
            writeMSG.write(xmlMessage);
            writeMSG.newLine();
            writeMSG.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Add method to update username after login
    public void setUsername(String username) {
        this.username = username;
    }

    // Listener setters
    public void setMessageListener(Consumer<String> messageListener) {
        this.messageListener = messageListener;
    }

    public void setChatHistoryListener(Consumer<String> chatHistoryListener) {
        this.chatHistoryListener = chatHistoryListener;
    }

    public void setForceLogoutListener(Consumer<String> forceLogoutListener) {
        this.forceLogoutListener = forceLogoutListener;
    }

    public void setNewMessageListener(Consumer<String> newMessageListener) {
        this.newMessageListener = newMessageListener;
    }

    // Utility methods
    public List<String> getFriends() {
        return xmlHandler.getFriends(username);
    }

    public List<String> getGroups() {
        return xmlHandler.getUserGroups(username);
    }

    // Getters
    public Socket getSocket() { return socket; }
    public String getUsername() { return username; }

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