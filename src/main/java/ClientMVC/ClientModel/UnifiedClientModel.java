package ClientMVC.ClientModel;

import ClientMVC.UserXMLHandler;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
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

    /*
     * handleXMLResponse:
     * 1. Get the reply from the XML, or fallback to status if reply is null.
     * 2. Check the reply type using switch.
     * 3. Call the correct handler based on reply (chat, group, file, login, etc.).
     * 4. Handle unknown replies with a default case.
     * 5. Catch errors and print them if something goes wrong.
     */
    private void handleXMLResponse(String xmlResponse) {
        try {
            String reply = xmlHandler.getReply(xmlResponse);
            if (reply == null) {
                // Check for status attribute as fallback
                UserXMLHandler.ServerResponse response = xmlHandler.parseServerResponse(xmlResponse);
                if (response != null && response.getStatus() != null) {
                    reply = response.getStatus();
                } else {
                    return;
                }
            }

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

                // File transfer response handling
                case "FILE_SENT":
                    handleFileTransferSuccess(xmlResponse);
                    break;

                case "FILE_SEND_FAILED":
                    handleFileTransferFailure(xmlResponse);
                    break;

                case "GROUP_FILE_SENT":
                    handleGroupFileTransferSuccess(xmlResponse);
                    break;

                case "GROUP_FILE_SEND_FAILED":
                case "GROUP_ACCESS_DENIED":
                    handleGroupFileTransferFailure(xmlResponse);
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
    /*
     * handleCommunityMessageResponse:
     * 1. Get the response body from the XML.
     * 2. Check if the body is not empty.
     * 3. Split the body into sender and message parts.
     * 4. If valid, format the message as "sender: message".
     * 5. Pass it to the communityMessageListener on the UI thread.
     * 6. Catch and print any errors if something goes wrong.
     */
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
    /*
     * handleChatHistoryResponse:
     * 1. Get the response body from the XML.
     * 2. Check if the body is not empty.
     * 3. If the body is valid Base64 → save the updated account XML.
     * 4. If chatHistoryListener exists:
     *    - If it's a new message → extract the latest message and send it.
     *    - If no latest message found → send "MESSAGE_RECEIVED".
     *    - If not a new message → always send "MESSAGE_RECEIVED".
     * 5. Catch and print errors if something goes wrong.
     */
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

    /*
     * handleNewMessageResponse:
     * 1. Print debug info about the response and body length.
     * 2. Get the response body from the XML.
     * 3. Check if the body is not empty.
     * 4. If body is valid Base64 → save it to account XML.
     * 5. Extract the latest message; if it’s a file message, handle it separately.
     * 6. If chatHistoryListener exists → notify with "MESSAGE_RECEIVED".
     * 7. If listener is missing or body is invalid → log an error.
     * 8. Catch and print errors if something goes wrong.
     */
    private void handleNewMessageResponse(String xmlResponse) {
        try {
            System.out.println("handleNewMessageResponse called");
            System.out.println("Response length: " + xmlResponse.length());

            String body = xmlHandler.getResponseBody(xmlResponse);
            if (body != null && !body.isEmpty()) {
                System.out.println("Body length: " + body.length());

                if (isValidBase64(body)) {
                    System.out.println("Valid base64 content, saving to account XML");
                    xmlHandler.saveUpdatedAccountXML(username, body);

                    String latestMessage = extractLatestMessageFromBase64(body);
                    if (latestMessage != null && isFileMessage(latestMessage)) {
                        System.out.println("File message detected: " + latestMessage);
                        handleFileMessage(latestMessage);
                    }

                    if (chatHistoryListener != null) {
                        System.out.println("NEW_MESSAGE received, triggering chat refresh");
                        Platform.runLater(() -> chatHistoryListener.accept("MESSAGE_RECEIVED"));
                    } else {
                        System.err.println("chatHistoryListener is null!");
                    }
                } else {
                    System.err.println("Invalid base64 content in response body");
                }
            } else {
                System.err.println("Empty or null response body");
            }
        } catch (Exception e) {
            System.err.println("Error handling new message response: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /*
     * handleFileTransferSuccess:
     * 1. Get the response body from the XML.
     * 2. Print a success message with the body.
     * 3. If fileStatusListener exists → notify with "SUCCESS: body".
     * 4. Catch and print any errors if something goes wrong.
     */
    private void handleFileTransferSuccess(String xmlResponse) {
        try {
            String body = xmlHandler.getResponseBody(xmlResponse);
            System.out.println("File transfer successful: " + body);

            if (fileStatusListener != null) {
                Platform.runLater(() -> fileStatusListener.accept("SUCCESS: " + body));
            }
        } catch (Exception e) {
            System.err.println("Error handling file transfer success: " + e.getMessage());
        }
    }
    /*
     * handleFileTransferFailure:
     * 1. Get the response body from the XML.
     * 2. Print a failure message with the body.
     * 3. Use body as error message if available, otherwise use "Unknown error occurred".
     * 4. If fileStatusListener exists → notify with "FAILURE: errorMessage".
     * 5. Catch and print errors, and notify listener about processing error if needed.
     */
    private void handleFileTransferFailure(String xmlResponse) {
        try {
            String body = xmlHandler.getResponseBody(xmlResponse);
            System.err.println("File transfer failed: " + body);

            String errorMessage = (body != null && !body.isEmpty()) ? body : "Unknown error occurred";

            if (fileStatusListener != null) {
                Platform.runLater(() -> fileStatusListener.accept("FAILURE: " + errorMessage));
            }
        } catch (Exception e) {
            System.err.println("Error handling file transfer failure: " + e.getMessage());
            if (fileStatusListener != null) {
                Platform.runLater(() -> fileStatusListener.accept("FAILURE: Error processing server response"));
            }
        }
    }

    private void handleGroupFileTransferSuccess(String xmlResponse) {
        try {
            String body = xmlHandler.getResponseBody(xmlResponse);
            System.out.println("Group file transfer successful: " + body);

            if (fileStatusListener != null) {
                Platform.runLater(() -> fileStatusListener.accept("SUCCESS: " + body));
            }
        } catch (Exception e) {
            System.err.println("Error handling group file transfer success: " + e.getMessage());
        }
    }
    /*
     * handleGroupFileTransferSuccess:
     * 1. Get the response body from the XML.
     * 2. Print a success message with the body.
     * 3. If fileStatusListener exists → notify with "SUCCESS: body".
     * 4. Catch and print any errors.
     */
    private void handleGroupFileTransferFailure(String xmlResponse) {
        try {
            String body = xmlHandler.getResponseBody(xmlResponse);
            System.err.println("Group file transfer failed: " + body);

            if (fileStatusListener != null) {
                Platform.runLater(() -> fileStatusListener.accept("FAILURE: " + body));
            }
        } catch (Exception e) {
            System.err.println("Error handling group file transfer failure: " + e.getMessage());
        }
    }
    private Consumer<String> fileStatusListener;

    public void setFileStatusListener(Consumer<String> fileStatusListener) {
        this.fileStatusListener = fileStatusListener;
    }

    /*
     * handleGroupFileTransferFailure:
     * 1. Get the response body from the XML.
     * 2. Print a failure message with the body.
     * 3. If fileStatusListener exists → notify with "FAILURE: body".
     * 4. Catch and print any errors.
     *
     * fileStatusListener:
     * - A listener (callback) to receive file transfer status messages.
     * - Can be set using setFileStatusListener().
     */
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
    /*
     * extractLatestMessageFromBase64:
     * 1. Decode the Base64 string into XML content.
     * 2. Parse the XML and find all <chat> nodes.
     * 3. Get the last <chat> element (latest message).
     * 4. Extract sender and message text.
     * 5. Return "sender: message" format.
     * 6. If an error occurs → print it and return null.
     */
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
    /*
     * sendFile:
     * 1. Check if the file exists, is valid, and not empty.
     * 2. Validate file size (must be <= 10MB).
     * 3. Read file content and encode it into Base64.
     * 4. Build an XML request with file details (name, extension, content).
     * 5. Send the XML message to the server.
     * 6. Notify listener of success or failure.
     */
    public synchronized boolean sendFile(String filePath, String receiver) {
        try {
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                System.err.println("File not found or is not a valid file: " + filePath);
                if (fileStatusListener != null) {
                    Platform.runLater(() -> fileStatusListener.accept("FAILURE: File not found"));
                }
                return false;
            }

            long fileSize = file.length();
            System.out.println("Preparing to send file: " + file.getName() + " (" + fileSize + " bytes)");

            if (fileSize > 10 * 1024 * 1024) {
                System.err.println("File too large. Maximum size is 10MB");
                if (fileStatusListener != null) {
                    Platform.runLater(() -> fileStatusListener.accept("FAILURE: File too large (max 10MB)"));
                }
                return false;
            }

            if (fileSize == 0) {
                System.err.println("Cannot send empty file");
                if (fileStatusListener != null) {
                    Platform.runLater(() -> fileStatusListener.accept("FAILURE: Empty file"));
                }
                return false;
            }

            byte[] fileContent = Files.readAllBytes(file.toPath());
            String encodedContent = Base64.getEncoder().encodeToString(fileContent);
            String fileName = file.getName();
            String fileExtension = getFileExtension(fileName);

            System.out.println("File encoded successfully:");
            System.out.println("- Original size: " + fileSize + " bytes");
            System.out.println("- Encoded size: " + encodedContent.length() + " characters");

            // Build the file message with proper delimiters
            String fileMessage = "FILE_TRANSFER![]!" + fileName + "![]!" + fileExtension + "![]!" + encodedContent;
            String xmlMessage = "<Request><type>SEND_FILE</type><content>" +
                    username + "![]!" + receiver + "![]!" + fileMessage + "</content></Request>";
            System.out.println("Full XML message structure:");
            System.out.println("- Username: " + username);
            System.out.println("- Receiver: " + receiver);
            System.out.println("- File message starts with: " + fileMessage.substring(0, Math.min(50, fileMessage.length())));
            System.out.println("- Total XML length: " + xmlMessage.length());
            System.out.println("Sending file message (length: " + xmlMessage.length() + ")");

            writeMSG.write(xmlMessage);
            writeMSG.newLine();
            writeMSG.flush();

            System.out.println("File message sent to server");
            return true;

        } catch (IOException e) {
            System.err.println("Error sending file: " + e.getMessage());
            e.printStackTrace();
            if (fileStatusListener != null) {
                Platform.runLater(() -> fileStatusListener.accept("FAILURE: " + e.getMessage()));
            }
            return false;
        }
    }
    /*
     * sendGroupFile:
     * 1. Check if the user has access to the group.
     * 2. Validate the file (must exist, be valid, and <= 10MB).
     * 3. Read and encode file content into Base64.
     * 4. Build an XML request with group details, file info, and encoded content.
     * 5. Send the XML message to the server.
     * 6. Return true if sent successfully, otherwise false.
     */
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
    /*
     * saveReceivedFile:
     * 1. Locate the user's storage directory.
     * 2. Search for the file matching the given fileName.
     * 3. If found → read metadata and extract Base64 content.
     * 4. Decode and save the file to the downloads folder.
     * 5. Return true if saved successfully, otherwise false.
     */
    public boolean saveReceivedFile(String fileName) {
        try {
            // First, check if file exists in server storage for this user
            String userFilesPath = "src/main/resources/XMLFiles/userFiles/" + username;
            File userDir = new File(userFilesPath);

            if (!userDir.exists()) {
                System.err.println("User files directory not found: " + userFilesPath);
                return false;
            }

            File[] files = userDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    // Check if the file matches the requested filename
                    if (file.getName().contains(fileName)) {
                        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                            reader.readLine(); // SENDER line
                            String originalNameLine = reader.readLine(); // ORIGINAL_NAME line
                            String extensionLine = reader.readLine(); // EXTENSION line
                            reader.readLine(); // TIMESTAMP line
                            reader.readLine(); // SIZE line
                            String contentLine = reader.readLine(); // CONTENT line

                            if (contentLine != null && contentLine.startsWith("CONTENT:")) {
                                String base64Content = contentLine.substring(8);
                                String originalName = originalNameLine.split(":", 2)[1];

                                return saveFileToDownloads(originalName, base64Content);
                            }
                        }
                    }
                }
            }

            System.err.println("File not found in server storage: " + fileName);
            return false;

        } catch (Exception e) {
            System.err.println("Error saving received file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean saveFileToDownloads(String fileName, String base64Content) {
        try {
            // Create Downloads/ChatApp directory
            String downloadsPath = System.getProperty("user.home") + "/Downloads/ChatApp";
            File downloadsDir = new File(downloadsPath);
            if (!downloadsDir.exists()) {
                boolean created = downloadsDir.mkdirs();
                if (!created) {
                    System.err.println("Failed to create downloads directory: " + downloadsPath);
                    return false;
                }
            }

            // Handle duplicate filenames
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

            // Decode and save file
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
            // First, try to find the file in Downloads
            String downloadsPath = System.getProperty("user.home") + "/Downloads/ChatApp";
            File downloadsDir = new File(downloadsPath);

            if (downloadsDir.exists()) {
                File[] files = downloadsDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().startsWith(fileName) || file.getName().equals(fileName)) {
                            return openFileWithSystem(file);
                        }
                    }
                }
            }

            // If not found in Downloads, try to download it first
            System.out.println("File not found in downloads, attempting to retrieve from server storage...");
            boolean downloaded = saveReceivedFile(fileName);

            if (downloaded) {
                // Try to open the newly downloaded file
                if (downloadsDir.exists()) {
                    File[] files = downloadsDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.getName().startsWith(fileName) || file.getName().equals(fileName)) {
                                return openFileWithSystem(file);
                            }
                        }
                    }
                }
            }

            System.err.println("Could not find or download file: " + fileName);
            return false;

        } catch (Exception e) {
            System.err.println("Error opening file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    private boolean openFileWithSystem(File file) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
                    desktop.open(file);
                    System.out.println("Opening file: " + file.getAbsolutePath());
                    return true;
                }
            }
            System.err.println("Desktop operations not supported");
            return false;
        } catch (Exception e) {
            System.err.println("Error opening file with system: " + e.getMessage());
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
    public List<String> getAllUsers() {
        return xmlHandler.getAllUsers();
    }

    public String getUsername() {
        return username;
    }

}