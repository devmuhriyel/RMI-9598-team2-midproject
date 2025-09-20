package ClientMVC.ClientModel;

import ClientMVC.UserXMLHandler;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

public class UnifiedClientModel implements Runnable {
    private final Socket socket;
    private final BufferedReader readMSG;
    private final BufferedWriter writeMSG;
    private String username;
    private UserXMLHandler xmlHandler;

    private Consumer<String> messageListener;
    private Consumer<String> chatHistoryListener;
    private Consumer<String> forceLogoutListener;
    private Consumer<String> newMessageListener;
    private Consumer<String> communityMessageListener;
    private Consumer<String> fileReceivedListener;

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

                case "NEW_GROUP_MESSAGE":
                    handleNewGroupMessageResponse(xmlResponse);
                    break;

                case "COMMUNITY_MESSAGE":
                    handleCommunityMessageResponse(xmlResponse);
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

    private void handleCommunityMessageResponse(String xmlResponse) {
        try {
            String body = xmlHandler.getResponseBody(xmlResponse);
            if (body != null && !body.isEmpty()) {
                String[] parts = body.split("!\\[]!", 2);
                if (parts.length == 2) {
                    String sender = parts[0];
                    String message = parts[1];

                    if (communityMessageListener != null) {
                        Platform.runLater(() -> communityMessageListener.accept(sender + ": " + message));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling community message response: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleChatHistoryResponse(String xmlResponse, boolean isNewMessage) {
        try {
            String body = xmlHandler.getResponseBody(xmlResponse);
            if (body != null && !body.isEmpty()) {
                if (isValidBase64(body)) {
                    xmlHandler.saveUpdatedAccountXML(username, body);

                    if (chatHistoryListener != null) {
                        if (isNewMessage) {
                            String latestMessage = extractLatestMessageFromBase64(body);
                            if (latestMessage != null) {
                                Platform.runLater(() -> chatHistoryListener.accept(latestMessage));
                            } else {
                                Platform.runLater(() -> chatHistoryListener.accept("MESSAGE_RECEIVED"));
                            }
                        } else {
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
                    xmlHandler.saveUpdatedAccountXML(username, body);
                    String latestMessage = extractLatestMessageFromBase64(body);
                    if (latestMessage != null && isFileMessage(latestMessage)) {
                        handleFileMessage(latestMessage);
                    }

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

    private void handleNewGroupMessageResponse(String xmlResponse) {
        try {
            String body = xmlHandler.getResponseBody(xmlResponse);
            if (body != null && !body.isEmpty()) {
                if (isValidBase64(body)) {
                    xmlHandler.saveUpdatedAccountXML(username, body);
                    String latestMessage = extractLatestMessageFromBase64(body);
                    if (latestMessage != null && isFileMessage(latestMessage)) {
                        handleFileMessage(latestMessage);
                    }

                    if (chatHistoryListener != null) {
                        System.out.println("NEW_GROUP_MESSAGE received, triggering chat refresh");
                        Platform.runLater(() -> chatHistoryListener.accept("MESSAGE_RECEIVED"));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling new group message response: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isFileMessage(String message) {
        return message.contains("[FILE: ") && message.contains("]");
    }

    private void handleFileMessage(String message) {
        if (fileReceivedListener != null) {
            int startIndex = message.indexOf("[FILE: ") + 7;
            int endIndex = message.lastIndexOf("]");
            if (startIndex < endIndex) {
                String fileName = message.substring(startIndex, endIndex);
                Platform.runLater(() -> fileReceivedListener.accept(fileName));
            }
        }
    }

    private String extractLatestMessageFromBase64(String base64Content) {
        try {
            byte[] decodedXML = Base64.getDecoder().decode(base64Content);
            String xmlContent = new String(decodedXML, "UTF-8");

            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xmlContent.getBytes("UTF-8")));

            org.w3c.dom.NodeList chatNodes = doc.getElementsByTagName("chat");

            if (chatNodes.getLength() > 0) {
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
            byte[] decoded = Base64.getDecoder().decode(base64);
            String xmlContent = new String(decoded, "UTF-8");
            return xmlContent.contains("<") && xmlContent.contains(">");
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized boolean sendFile(String filePath, String receiver) {
        try {
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                System.err.println("File not found or is not a valid file: " + filePath);
                return false;
            }
            long fileSize = file.length();
            if (fileSize > 10 * 1024 * 1024) {
                System.err.println("File too large. Maximum size is 10MB");
                return false;
            }
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String encodedContent = Base64.getEncoder().encodeToString(fileContent);
            String fileName = file.getName();
            String fileExtension = getFileExtension(fileName);
            String fileMessage = "FILE_TRANSFER![]!" + fileName + "![]!" + fileExtension + "![]!" + encodedContent;
            String xmlMessage = "<Request><type>SEND_FILE</type><content>" +
                    username + "![]!" + receiver + "![]!" + fileMessage + "</content></Request>";

            System.out.println("Sending file: " + fileName + " (" + fileSize + " bytes)");
            writeMSG.write(xmlMessage);
            writeMSG.newLine();
            writeMSG.flush();

            return true;

        } catch (IOException e) {
            System.err.println("Error sending file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public synchronized boolean sendGroupFile(String filePath, String groupName) {
        try {
            if (!canAccessGroup(groupName)) {
                System.err.println("Access denied: User " + username + " is not a member of group " + groupName);
                return false;
            }
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                System.err.println("File not found or is not a valid file: " + filePath);
                return false;
            }
            long fileSize = file.length();
            if (fileSize > 10 * 1024 * 1024) {
                System.err.println("File too large. Maximum size is 10MB");
                return false;
            }
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String encodedContent = Base64.getEncoder().encodeToString(fileContent);

            String fileName = file.getName();
            String fileExtension = getFileExtension(fileName);

            String fileMessage = "FILE_TRANSFER![]!" + fileName + "![]!" + fileExtension + "![]!" + encodedContent;
            String xmlMessage = "<Request><type>SEND_GROUP_FILE</type><content>" +
                    username + "![]!" + groupName + "![]!" + fileMessage + "</content></Request>";
            System.out.println("Sending file to group: " + fileName + " (" + fileSize + " bytes)");
            writeMSG.write(xmlMessage);
            writeMSG.newLine();
            writeMSG.flush();

            return true;

        } catch (IOException e) {
            System.err.println("Error sending group file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveReceivedFile(String fileName) {
        try {
            String userFilesPath = "src/main/resources/XMLFiles/userFiles/" + username;
            File userDir = new File(userFilesPath);
            if (!userDir.exists()) {
                userDir.mkdirs();
            }
            File[] files = userDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().contains(fileName)) {
                        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                            reader.readLine();
                            String originalNameLine = reader.readLine();
                            String extensionLine = reader.readLine();
                            reader.readLine();
                            String contentLine = reader.readLine();
                            if (contentLine != null && contentLine.startsWith("CONTENT:")) {
                                String base64Content = contentLine.substring(8);
                                String originalName = originalNameLine.split(":", 2)[1];

                                return saveFileToDownloads(originalName, base64Content);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error saving received file: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private boolean saveFileToDownloads(String fileName, String base64Content) {
        try {
            String downloadsPath = System.getProperty("user.home") + "/Downloads/ChatApp";
            File downloadsDir = new File(downloadsPath);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }
            File outputFile = new File(downloadsDir, fileName);
            int counter = 1;
            String baseName = fileName;
            String extension = getFileExtension(fileName);

            if (!extension.isEmpty()) {
                baseName = fileName.substring(0, fileName.lastIndexOf('.'));
            }

            while (outputFile.exists()) {
                String newFileName = baseName + "(" + counter + ")" + extension;
                outputFile = new File(downloadsDir, newFileName);
                counter++;
            }
            byte[] fileContent = Base64.getDecoder().decode(base64Content);
            Files.write(outputFile.toPath(), fileContent);

            System.out.println("File saved to: " + outputFile.getAbsolutePath());
            return true;

        } catch (Exception e) {
            System.err.println("Error saving file to downloads: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean openFile(String fileName) {
        try {
            String downloadsPath = System.getProperty("user.home") + "/Downloads/ChatApp";
            File downloadsDir = new File(downloadsPath);

            if (downloadsDir.exists()) {
                File[] files = downloadsDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().startsWith(fileName) || file.getName().equals(fileName)) {
                            if (java.awt.Desktop.isDesktopSupported()) {
                                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                                if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
                                    desktop.open(file);
                                    System.out.println("Opening file: " + file.getAbsolutePath());
                                    return true;
                                }
                            }
                        }
                    }
                }
            }

            System.err.println("File not found in downloads: " + fileName);
            return false;

        } catch (Exception e) {
            System.err.println("Error opening file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot > 0) ? fileName.substring(lastDot) : "";
    }

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
            List<String> userGroups = xmlHandler.getUserGroups(username);

            if (!groupName.equals("Community Chat") && !userGroups.contains(groupName)) {
                System.err.println("Access denied: User " + username + " is not a member of group " + groupName);
                return;
            }

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

    public boolean canAccessGroup(String groupName) {
        if ("Community Chat".equals(groupName)) {
            return true;
        }

        List<String> userGroups = xmlHandler.getUserGroups(username);
        return userGroups != null && userGroups.contains(groupName);
    }

    public synchronized void sendCommunityMessage(String message) {
        try {
            String xmlMessage = "<Request><type>SEND_COMMUNITY_MESSAGE</type><content>" +
                    username + "![]!" + message + "</content></Request>";
            System.out.println("Sending community message: " + xmlMessage);
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
    public void setUsername(String username) {
        this.username = username;
    }

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

    public void setCommunityMessageListener(Consumer<String> communityMessageListener) {
        this.communityMessageListener = communityMessageListener;
    }

    public void setFileReceivedListener(Consumer<String> fileReceivedListener) {
        this.fileReceivedListener = fileReceivedListener;
    }

    public List<String> getFriends() {
        return xmlHandler.getFriends(username);
    }

    public List<String> getGroups() {
        return xmlHandler.getUserGroups(username);
    }

    public Socket getSocket() {
        return socket;
    }

    public String getUsername() {
        return username;
    }

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