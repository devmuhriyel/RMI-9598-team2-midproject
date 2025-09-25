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

    // Callback interfaces
    public interface PasswordResetNotificationCallback {
        void onPasswordResetNotification(String username, String newPassword);

        // Add overloaded method with message parameter
        default void onPasswordResetNotification(String username, String newPassword, String message) {
            // Default implementation calls the two-parameter version
            onPasswordResetNotification(username, newPassword);
        }
    }

    public interface GeneralNotificationCallback {
        void onGeneralNotification(String type, String message);
    }

    // Callback instances
    private Consumer<String> messageListener;
    private Consumer<String> chatHistoryListener;
    private Consumer<String> forceLogoutListener;
    private Consumer<String> communityMessageListener;
    private Consumer<String> fileReceivedListener;
    private Consumer<String> accountUpdateListener;
    private Consumer<String> fileStatusListener;
    private Consumer<String> verificationCodeValidListener;
    private PasswordResetNotificationCallback passwordResetNotificationCallback;
    private GeneralNotificationCallback generalNotificationCallback;

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
            if (reply == null) {
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

                case "USERNAME_CHANGED":
                case "USERNAME_CHANGE_FAILED":
                case "USERNAME_EXISTS":
                case "PASSWORD_CHANGED":
                case "PASSWORD_CHANGE_FAILED":
                case "INVALID_OLD_PASSWORD":
                    if (accountUpdateListener != null) {
                        Platform.runLater(() -> accountUpdateListener.accept(xmlResponse));
                    }
                    break;

                // NEW PASSWORD RESET NOTIFICATION HANDLING
                case "PASSWORD_RESET_NOTIFICATION":
                    handlePasswordResetNotificationForRequester(xmlResponse);
                    break;

                case "VERIFICATION_CODE_VALID":
                    if (verificationCodeValidListener != null) {
                        Platform.runLater(() -> verificationCodeValidListener.accept(xmlResponse));
                    }
                    if (messageListener != null) {
                        Platform.runLater(() -> messageListener.accept(xmlResponse));
                    }
                    break;

                case "VERIFICATION_CODE_INVALID":
                    if (messageListener != null) {
                        Platform.runLater(() -> messageListener.accept(xmlResponse));
                    }
                    break;

                case "PASSWORD_RESET_REQUESTED":
                    handlePasswordResetRequested(xmlResponse);
                    break;

                case "PASSWORD_RESET_SUCCESS":
                    handlePasswordResetSuccess(xmlResponse);
                    break;

                case "PASSWORD_RESET_FAILED":
                    handlePasswordResetFailed(xmlResponse);
                    break;

                case "PASSWORD_RESET_REJECTED":
                    handlePasswordResetRejected(xmlResponse);
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

    // NEW METHOD FOR PASSWORD RESET NOTIFICATION
    private void handlePasswordResetNotificationForRequester(String xmlResponse) {
        try {
            String body = xmlHandler.getResponseBody(xmlResponse);
            if (body != null && !body.isEmpty()) {
                String[] parts = body.split("!\\[]!");

                if (parts.length >= 2) {
                    String username = parts[0];
                    String newPassword = parts[1];

                    System.out.println("[DEBUG] Received password reset notification for: " + username);

                    // Use callback to show UI with new password
                    if (passwordResetNotificationCallback != null) {
                        Platform.runLater(() -> {
                            passwordResetNotificationCallback.onPasswordResetNotification(username, newPassword);
                        });
                    } else {
                        System.err.println("[ERROR] No password reset notification callback set");
                    }

                } else {
                    System.err.println("[ERROR] Invalid password reset notification format: " + body);
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling password reset notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handlePasswordResetRequested(String xmlResponse) {
        try {
            String body = xmlHandler.getResponseBody(xmlResponse);
            if (body != null && !body.isEmpty()) {
                String[] parts = body.split("!\\[]!");
                if (parts.length >= 2) {
                    String username = parts[0];
                    String verificationCode = parts[1];

                    System.out.println("[DEBUG] Password reset request confirmed for: " + username);

                    if (generalNotificationCallback != null) {
                        String message = "Password reset request confirmed for: " + username +
                                ". Your verification code has been validated. An administrator will review your request.";
                        Platform.runLater(() -> generalNotificationCallback.onGeneralNotification("PASSWORD_RESET_REQUESTED", message));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling password reset requested response: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handlePasswordResetRejected(String xmlResponse) {
        System.out.println("[DEBUG] Password reset request was rejected");

        if (generalNotificationCallback != null) {
            String message = "Your password reset request was rejected by the administrator. " +
                    "This could be due to incorrect verification code or security concerns. " +
                    "Please contact the administrator directly for assistance.";
            Platform.runLater(() -> generalNotificationCallback.onGeneralNotification("PASSWORD_RESET_REJECTED", message));
        }
    }

    private void handlePasswordResetSuccess(String xmlResponse) {
        try {
            String body = xmlHandler.getResponseBody(xmlResponse);
            if (body != null && !body.isEmpty()) {
                String[] parts = body.split("!\\[]!");

                if (parts.length >= 3) {
                    String username = parts[0];
                    String newPassword = parts[1];
                    String message = parts[2];

                    System.out.println("[DEBUG] Password reset notification received for user: " + username);

                    if (passwordResetNotificationCallback != null) {
                        Platform.runLater(() -> passwordResetNotificationCallback.onPasswordResetNotification(username, newPassword, message));
                    }

                } else if (parts.length == 2) {
                    String username = parts[0];
                    String newPassword = parts[1];
                    String defaultMessage = "Your password has been successfully reset by an administrator.";

                    System.out.println("[DEBUG] Password reset notification received for user: " + username);

                    if (passwordResetNotificationCallback != null) {
                        Platform.runLater(() -> passwordResetNotificationCallback.onPasswordResetNotification(username, newPassword, defaultMessage));
                    }
                } else {
                    System.out.println("[DEBUG] Password reset notification with unexpected format: " + body);

                    if (generalNotificationCallback != null) {
                        Platform.runLater(() -> generalNotificationCallback.onGeneralNotification("PASSWORD_RESET_SUCCESS", body));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling password reset success response: " + e.getMessage());
            e.printStackTrace();

            if (generalNotificationCallback != null) {
                Platform.runLater(() -> generalNotificationCallback.onGeneralNotification("PASSWORD_RESET_ERROR",
                        "Error processing password reset notification"));
            }
        }
    }


    private void handlePasswordResetFailed(String xmlResponse) {
        try {
            String body = xmlHandler.getResponseBody(xmlResponse);
            String message;

            if (body != null && !body.isEmpty()) {
                if (body.contains("No verification code found")) {
                    message = "Your account does not have a verification code set up. " +
                            "Please contact an administrator to set up account recovery options.";
                } else {
                    message = "Password reset failed: " + body + ". Please contact an administrator for assistance.";
                }
            } else {
                message = "Your password reset request failed.";
            }

            System.out.println("[DEBUG] Password reset failed: " + message);

            if (generalNotificationCallback != null) {
                Platform.runLater(() -> generalNotificationCallback.onGeneralNotification("PASSWORD_RESET_FAILED", message));
            }
        } catch (Exception e) {
            if (generalNotificationCallback != null) {
                Platform.runLater(() -> generalNotificationCallback.onGeneralNotification("PASSWORD_RESET_FAILED",
                        "Your password reset request could not be processed. Please contact an administrator."));
            }
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

    // FILE TRANSFER METHODS
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

            String fileMessage = "FILE_TRANSFER![]!" + fileName + "![]!" + fileExtension + "![]!" + encodedContent;
            String xmlMessage = "<Request><type>SEND_FILE</type><content>" +
                    username + "![]!" + receiver + "![]!" + fileMessage + "</content></Request>";

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
                System.err.println("User files directory not found: " + userFilesPath);
                return false;
            }

            File[] files = userDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().contains(fileName)) {
                        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                            reader.readLine(); // SENDER line
                            String originalNameLine = reader.readLine(); // ORIGINAL_NAME line
                            reader.readLine(); // EXTENSION line
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
            String downloadsPath = System.getProperty("user.home") + "/Downloads/ChatApp";
            File downloadsDir = new File(downloadsPath);
            if (!downloadsDir.exists()) {
                boolean created = downloadsDir.mkdirs();
                if (!created) {
                    System.err.println("Failed to create downloads directory: " + downloadsPath);
                    return false;
                }
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
                            return openFileWithSystem(file);
                        }
                    }
                }
            }

            System.out.println("File not found in downloads, attempting to retrieve from server storage...");
            boolean downloaded = saveReceivedFile(fileName);

            if (downloaded) {
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

    // MESSAGING METHODS
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

    public synchronized void changeUsername(String newUsername) {
        try {
            String xmlMessage = "<Request><type>CHANGE_USERNAME</type><content>" +
                    username + "![]!" + newUsername + "</content></Request>";
            System.out.println("Sending username change request: " + xmlMessage);
            writeMSG.write(xmlMessage);
            writeMSG.newLine();
            writeMSG.flush();
        } catch (IOException e) {
            System.err.println("Error sending username change request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void changePasswordDirect(String newPassword) {
        try {
            String xmlMessage = "<Request><type>CHANGE_PASSWORD</type><content>" +
                    username + "![]!dummy![]!" + newPassword + "</content></Request>";
            System.out.println("Sending direct password change request: " + xmlMessage);
            writeMSG.write(xmlMessage);
            writeMSG.newLine();
            writeMSG.flush();
        } catch (IOException e) {
            System.err.println("Error sending direct password change request: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public synchronized void changePassword(String oldPassword, String newPassword) {
        try {
            String xmlMessage = "<Request><type>CHANGE_PASSWORD</type><content>" +
                    username + "![]!" + oldPassword + "![]!" + newPassword + "</content></Request>";
            System.out.println("Sending password change request: " + xmlMessage);
            writeMSG.write(xmlMessage);
            writeMSG.newLine();
            writeMSG.flush();
        } catch (IOException e) {
            System.err.println("Error sending password change request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addFriend(String friendUsername) {
        System.out.println("UnifiedClientModel: Adding friend " + friendUsername);
        UserXMLHandler.addFriendRequest(this.username, friendUsername);
    }

    public synchronized void requestPasswordResetWithCode(String username, String verificationCode) {
        try {
            String xmlMessage = "<Request><type>REQUEST_PASSWORD_RESET_WITH_CODE</type><content>" +
                    username + "![]!" + verificationCode + "</content></Request>";
            System.out.println("Sending password reset request with code: " + xmlMessage);
            writeMSG.write(xmlMessage);
            writeMSG.newLine();
            writeMSG.flush();
        } catch (IOException e) {
            System.err.println("Error sending password reset request with code: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void resetPasswordOnServer(String username, String newPassword) {
        try {
            // Since this is a password reset via verification code, we need to send a direct password change
            String xmlMessage = "<Request><type>RESET_PASSWORD_CLIENT</type><content>" +
                    username + "![]!" + newPassword + "</content></Request>";
            System.out.println("Sending password reset to server: " + xmlMessage);
            writeMSG.write(xmlMessage);
            writeMSG.newLine();
            writeMSG.flush();
        } catch (IOException e) {
            System.err.println("Error sending password reset to server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void signup(String username, String password, String verification) {
        String xmlMessage = "<Request><type>REGISTER</type><content>" +
                username + "![]!" + password + "![]!" + verification + "</content></Request>";
        sendRawMessage(xmlMessage);
    }


    // SETTERS FOR CALLBACKS
    public void setPasswordResetNotificationCallback(PasswordResetNotificationCallback callback) {
        this.passwordResetNotificationCallback = callback;
    }

    public void setGeneralNotificationCallback(GeneralNotificationCallback callback) {
        this.generalNotificationCallback = callback;
    }

    public void setVerificationCodeValidListener(Consumer<String> verificationCodeValidListener) {
        this.verificationCodeValidListener = verificationCodeValidListener;
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

    public void setAccountUpdateListener(Consumer<String> accountUpdateListener) {
        this.accountUpdateListener = accountUpdateListener;
    }

    public void setFileStatusListener(Consumer<String> fileStatusListener) {
        this.fileStatusListener = fileStatusListener;
    }

    // GETTERS
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