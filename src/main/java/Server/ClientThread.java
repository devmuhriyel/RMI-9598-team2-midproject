package Server;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class ClientThread implements Runnable {
    private Socket socket;
    private BufferedReader readMSG;
    private BufferedWriter writeMSG;
    private String name;
    private static final ArrayList<ClientThread> clients = new ArrayList<>();
    private ServerXMLHandler xmlHandler = new ServerXMLHandler();

    public ClientThread(Socket socket) {
        try {
            this.socket = socket;
            this.writeMSG = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.readMSG = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            synchronized (clients) {
                clients.add(this);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*
     * run():
     * ------
     * Purpose:
     *   - Continuously listens for incoming XML messages from a connected client.
     *
     * How it works:
     *   1. Reads each XML message from the client.
     *   2. Uses xmlHandler to determine the type of request (login, register, chat, file, group, etc.).
     *   3. Handles different cases such as:
     *        - LOGIN / REGISTER (success or failed)
     *        - Sending and receiving messages
     *        - Friend requests (add, accept, decline, list)
     *        - Group management (create, join, list, send messages/files)
     *        - File transfers (private and group)
     *   4. Sends back XML responses to the client with status and message.
     *   5. Updates account XML files when needed (chat history, group history, file transfers).
     *   6. Ensures only one active session per user (forces logout of duplicate logins).
     *   7. Handles errors safely and logs them.
     *   8. Cleans up resources and logs out the client when the connection ends.
     */
    @Override
    public void run() {
        try {
            String xmlMsg;
            while ((xmlMsg = readMSG.readLine()) != null) {
                System.out.println("Received XML: " + xmlMsg);
                String reply = xmlHandler.readMessage(xmlMsg);

                switch (reply) {
                    case "LOGIN_SUCCESS":
                        String newUsername = extractUsername(xmlMsg);
                        ClientThread existingClient = null;
                        synchronized (clients) {
                            for (ClientThread clientThread : clients) {
                                if (clientThread.name != null && clientThread.name.equals(newUsername) && clientThread != this) {
                                    existingClient = clientThread;
                                    break;
                                }
                            }
                        }

                        if (existingClient != null) {
                            existingClient.sendMessage("<Response><reply>FORCE_LOGOUT</reply><body>You have been logged in from another location</body></Response>");
                            try {
                                existingClient.logOut();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        this.name = newUsername;
                        sendMessage("<Response><reply>LOGIN_SUCCESS</reply><body>Welcome " + this.name + "!</body></Response>");
                        break;

                    case "LOGIN_FAILED":
                        sendMessage("<Response status=\"LOGIN_FAILED\"><body>Invalid username or password</body></Response>");
                        break;

                    case "REGISTER_SUCCESS":
                        sendMessage("<Response status=\"REGISTER_SUCCESS\"><body>Registration successful</body></Response>");
                        break;

                    case "REGISTER_FAILED":
                        sendMessage("<Response status=\"REGISTER_FAILED\"><body>Registration failed</body></Response>");
                        break;

                    case "GET_CHAT_HISTORY":
                        String[] chatHistoryContent = extractContent(xmlMsg);
                        String requestingUser = chatHistoryContent[0];

                        try {
                            File userFile = new File("src/main/resources/XMLFiles/" + requestingUser + "Account.xml");
                            if (userFile.exists()) {
                                byte[] fileContent = java.nio.file.Files.readAllBytes(userFile.toPath());
                                String encodedContent = java.util.Base64.getEncoder().encodeToString(fileContent);
                                sendMessage("<Response><reply>GET_CHAT_HISTORY</reply><body>" + encodedContent + "</body></Response>");
                            } else {
                                sendMessage("<Response><reply>GET_CHAT_HISTORY</reply><body>FILE_NOT_FOUND</body></Response>");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    case "ADD_FRIEND_REQUEST":
                        String[] addContent = extractContent(xmlMsg);
                        if (xmlHandler.addFriendRequest(addContent[0], addContent[1])) {
                            sendMessage("<Response status=\"FRIEND_REQUEST_SENT\"><body>Request sent to " + addContent[1] + "</body></Response>");
                        } else {
                            sendMessage("<Response status=\"FRIEND_REQUEST_FAILED\"><body>Failed to send request</body></Response>");
                        }
                        break;

                    case "LIST_REQUESTS":
                        String[] listContent = extractContent(xmlMsg);
                        java.util.List<String> requests = xmlHandler.listFriendRequests(listContent[0]);
                        sendMessage("<Response status=\"REQUEST_LIST\"><body>" + String.join(",", requests) + "</body></Response>");
                        break;

                    case "ACCEPT_REQUEST":
                        String[] acceptContent = extractContent(xmlMsg);
                        if (xmlHandler.acceptFriendRequest(acceptContent[0], acceptContent[1])) {
                            sendMessage("<Response status=\"REQUEST_ACCEPTED\"><body>You are now friends with " + acceptContent[1] + "</body></Response>");
                        } else {
                            sendMessage("<Response status=\"REQUEST_ACCEPT_FAILED\"><body>Failed to accept request</body></Response>");
                        }
                        break;

                    case "DECLINE_REQUEST":
                        String[] declineContent = extractContent(xmlMsg);
                        if (xmlHandler.declineFriendRequest(declineContent[0], declineContent[1])) {
                            sendMessage("<Response status=\"REQUEST_DECLINED\"><body>Request from " + declineContent[1] + " declined</body></Response>");
                        } else {
                            sendMessage("<Response status=\"REQUEST_DECLINE_FAILED\"><body>Failed to decline request</body></Response>");
                        }
                        break;

                    case "CREATE_GROUP":
                        String[] createGroupContent = extractContent(xmlMsg);
                        if (xmlHandler.createGroup(createGroupContent[0], createGroupContent[1])) {
                            sendMessage("<Response status=\"GROUP_CREATED\"><body>Group " + createGroupContent[0] + " created successfully</body></Response>");
                        } else {
                            sendMessage("<Response status=\"GROUP_CREATE_FAILED\"><body>Failed to create group</body></Response>");
                        }
                        break;

                    case "JOIN_GROUP":
                        String[] joinGroupContent = extractContent(xmlMsg);
                        if (xmlHandler.joinGroupWithHistory(joinGroupContent[0], joinGroupContent[1])) {
                            sendMessage("<Response status=\"GROUP_JOINED\"><body>Joined group " + joinGroupContent[0] + " with full chat history</body></Response>");
                            try {
                                File userFile = new File("src/main/resources/XMLFiles/" + joinGroupContent[1] + "Account.xml");
                                if (userFile.exists()) {
                                    byte[] fileContent = java.nio.file.Files.readAllBytes(userFile.toPath());
                                    String encodedContent = java.util.Base64.getEncoder().encodeToString(fileContent);
                                    sendMessage("<Response><reply>GET_CHAT_HISTORY</reply><body>" + encodedContent + "</body></Response>");
                                }
                            } catch (Exception e) {
                                System.err.println("Error sending updated chat history to new group member: " + e.getMessage());
                                e.printStackTrace();
                            }
                        } else {
                            sendMessage("<Response status=\"GROUP_JOIN_FAILED\"><body>Failed to join group</body></Response>");
                        }
                        break;

                    case "LIST_GROUPS":
                        String[] listGroupsContent = extractContent(xmlMsg);
                        java.util.List<String> groups = xmlHandler.getUserGroups(listGroupsContent[0]);
                        sendMessage("<Response status=\"GROUPS_LIST\"><body>" + String.join(",", groups) + "</body></Response>");
                        break;

                    case "SEND_GROUP_MESSAGE":
                        String groupSender = getSender(xmlMsg);
                        String targetGroup = getGroupName(xmlMsg);
                        String groupMessage = getGroupMessage(xmlMsg);

                        System.out.println("Processing group message from " + groupSender + " to group " + targetGroup);
                        if (!xmlHandler.isUserInGroup(groupSender, targetGroup)) {
                            sendMessage("<Response status=\"GROUP_ACCESS_DENIED\"><body>You are not a member of group " + targetGroup + "</body></Response>");
                            System.out.println("Access denied: " + groupSender + " is not a member of group " + targetGroup);
                            break;
                        }

                        if (targetGroup.equals("Project Team") || targetGroup.equals("Study Group")) {
                            List<String> actualUserGroups = xmlHandler.getUserGroups(groupSender);
                            if (!actualUserGroups.contains(targetGroup)) {
                                sendMessage("<Response status=\"GROUP_NOT_FOUND\"><body>Group " + targetGroup + " does not exist or you are not a member</body></Response>");
                                System.out.println("Group access denied: " + targetGroup + " is not a real group for user " + groupSender);
                                break;
                            }
                        }

                        try {
                            java.util.List<String> groupMembers = xmlHandler.getGroupMembers(targetGroup);
                            System.out.println("Found " + groupMembers.size() + " members in group " + targetGroup);
                            for (String member : groupMembers) {
                                ClientThread memberClient = findClientByName(member);
                                if (memberClient != null && memberClient.socket.isConnected()) {
                                    try {
                                        File userFile = new File("src/main/resources/XMLFiles/" + member + "Account.xml");
                                        if (userFile.exists()) {
                                            byte[] fileContent = java.nio.file.Files.readAllBytes(userFile.toPath());
                                            String encodedContent = java.util.Base64.getEncoder().encodeToString(fileContent);
                                            memberClient.sendMessage("<Response><reply>NEW_GROUP_MESSAGE</reply><body>" + encodedContent + "</body></Response>");
                                            System.out.println("Sent group message update to: " + member);
                                        } else {
                                            System.out.println("Account XML not found for member: " + member);
                                        }
                                    } catch (Exception e) {
                                        System.err.println("Error sending group message to " + member + ": " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                } else {
                                    System.out.println("Member " + member + " is not online or client not found");
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Error processing group message: " + e.getMessage());
                            e.printStackTrace();
                        }
                        break;

                    case "SEND_MESSAGE":
                        String sender = getSender(xmlMsg);
                        String receiver = getReceiver(xmlMsg);
                        for (ClientThread client : clients) {
                            if (client.socket.isConnected() &&
                                    (client.name.equals(sender) || client.name.equals(receiver))) {

                                try {
                                    File userFile = new File("src/main/resources/XMLFiles/" + client.name + "Account.xml");
                                    if (userFile.exists()) {
                                        byte[] fileContent = java.nio.file.Files.readAllBytes(userFile.toPath());
                                        String encodedContent = java.util.Base64.getEncoder().encodeToString(fileContent);
                                        client.sendMessage("<Response><reply>NEW_MESSAGE</reply><body>" + encodedContent + "</body></Response>");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        break;

                    case "SEND_COMMUNITY_MESSAGE":
                        String communitySender = getSender(xmlMsg);
                        String communityMessage = getCommunityMessage(xmlMsg);
                        String communityBroadcast = "<Response><reply>COMMUNITY_MESSAGE</reply>" +
                                "<body>" + communitySender + "![]!" + communityMessage + "</body></Response>";
                        broadcastCommunityMessage(communityBroadcast);
                        break;

                    case "SEND_FILE":
                        String fileSender = getSender(xmlMsg);
                        String fileReceiver = getReceiver(xmlMsg);
                        System.out.println("Processing file transfer from " + fileSender + " to " + fileReceiver);

                        // Send success/failure response to sender
                        String fileResponse = reply.equals("SEND_FILE_SUCCESS") ?
                                "<Response><reply>FILE_SENT</reply><body>File sent successfully</body></Response>" :
                                "<Response><reply>FILE_SEND_FAILED</reply><body>Failed to send file</body></Response>";

                        sendMessage(fileResponse);

                        // Only update XMLs if transfer was successful
                        if (reply.equals("SEND_FILE_SUCCESS")) {
                            // Update both sender and receiver XMLs with new message
                            for (ClientThread client : clients) {
                                if (client.socket.isConnected() &&
                                        (client.name.equals(fileSender) || client.name.equals(fileReceiver))) {
                                    try {
                                        File userFile = new File("src/main/resources/XMLFiles/" + client.name + "Account.xml");
                                        if (userFile.exists()) {
                                            byte[] fileContent = java.nio.file.Files.readAllBytes(userFile.toPath());
                                            String encodedContent = java.util.Base64.getEncoder().encodeToString(fileContent);
                                            client.sendMessage("<Response><reply>NEW_MESSAGE</reply><body>" + encodedContent + "</body></Response>");
                                            System.out.println("Sent file update to: " + client.name);
                                        } else {
                                            System.err.println("Account file not found for: " + client.name);
                                        }
                                    } catch (Exception e) {
                                        System.err.println("Error sending file update to " + client.name + ": " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        break;
                    case "SEND_GROUP_FILE":
                        String groupFileSender = getSender(xmlMsg);
                        String targetGroupForFile = getGroupName(xmlMsg);

                        System.out.println("Processing group file transfer from " + groupFileSender + " to group " + targetGroupForFile);

                        // Send response to sender
                        String groupFileResponse;
                        switch (reply) {
                            case "SEND_GROUP_FILE_SUCCESS":
                                groupFileResponse = "<Response><reply>GROUP_FILE_SENT</reply><body>File sent successfully</body></Response>";
                                break;
                            case "GROUP_ACCESS_DENIED":
                                groupFileResponse = "<Response><reply>GROUP_ACCESS_DENIED</reply><body>You are not a member of this group</body></Response>";
                                break;
                            default:
                                groupFileResponse = "<Response><reply>GROUP_FILE_SEND_FAILED</reply><body>Failed to send file to group</body></Response>";
                                break;
                        }

                        sendMessage(groupFileResponse);

                        // Only update XMLs if transfer was successful
                        if (reply.equals("SEND_GROUP_FILE_SUCCESS")) {
                            if (!xmlHandler.isUserInGroup(groupFileSender, targetGroupForFile)) {
                                System.out.println("Access denied: " + groupFileSender + " is not a member of group " + targetGroupForFile);
                                break;
                            }
                            try {
                                java.util.List<String> groupMembers = xmlHandler.getGroupMembers(targetGroupForFile);
                                System.out.println("Found " + groupMembers.size() + " members in group " + targetGroupForFile);

                                for (String member : groupMembers) {
                                    ClientThread memberClient = findClientByName(member);
                                    if (memberClient != null && memberClient.socket.isConnected()) {
                                        try {
                                            File userFile = new File("src/main/resources/XMLFiles/" + member + "Account.xml");
                                            if (userFile.exists()) {
                                                byte[] fileContent = java.nio.file.Files.readAllBytes(userFile.toPath());
                                                String encodedContent = java.util.Base64.getEncoder().encodeToString(fileContent);
                                                memberClient.sendMessage("<Response><reply>NEW_GROUP_MESSAGE</reply><body>" + encodedContent + "</body></Response>");
                                                System.out.println("Sent group file update to: " + member);
                                            }
                                        } catch (Exception e) {
                                            System.err.println("Error sending group file to " + member + ": " + e.getMessage());
                                            e.printStackTrace();
                                        }
                                    } else {
                                        System.out.println("Member " + member + " is not online");
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("Error processing group file message: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                        break;
                    case "FILE_SENT":
                        sendMessage("<Response><reply>FILE_SENT</reply><body>File sent successfully</body></Response>");
                        break;

                    case "FILE_SEND_FAILED":
                        sendMessage("<Response><reply>FILE_SEND_FAILED</reply><body>Failed to send file</body></Response>");
                        break;

                    case "GROUP_FILE_SENT":
                        sendMessage("<Response><reply>GROUP_FILE_SENT</reply><body>File sent to group successfully</body></Response>");
                        break;

                    case "GROUP_FILE_SEND_FAILED":
                        sendMessage("<Response><reply>GROUP_FILE_SEND_FAILED</reply><body>Failed to send file to group</body></Response>");
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                logOut();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ClientThread findClientByName(String username) {
        synchronized (clients) {
            for (ClientThread client : clients) {
                if (client.name != null && client.name.equals(username)) {
                    return client;
                }
            }
        }
        return null;
    }

    private void broadcastCommunityMessage(String message) {
        synchronized (clients) {
            for (ClientThread c : clients) {
                if (c.socket.isConnected() && c.name != null) {
                    try {
                        c.writeMSG.write(message);
                        c.writeMSG.newLine();
                        c.writeMSG.flush();
                    } catch (IOException e) {
                        try {
                            c.logOut();
                        } catch (IOException ignored) {}
                    }
                }
            }
        }
    }

    /*
     * Helper Methods Overview:
     * ------------------------
     * - sendMessage(msg): Sends a message to the client safely.
     * - getSender(xml): Extracts the sender’s username from an XML message.
     * - getReceiver(xml): Extracts the receiver’s username from an XML message.
     * - getGroupName(xml): Extracts the group name from a group message.
     * - getGroupMessage(xml): Extracts the actual message content from a group message.
     * - getCommunityMessage(xml): Extracts the community message content.
     * - extractUsername(xml): Extracts only the username part from the XML message.
     * - extractContent(xml): Returns all parts of the <content> element as an array.
     * - logOut(): Removes the client from the connected list and closes the socket.
     *
     * In short:
     * These methods parse XML strings, extract useful info (like sender, receiver,
     * group name, or message), and handle client communication and logout.
     */
    private void sendMessage(String msg) {
        try {
            writeMSG.write(msg.trim());
            writeMSG.newLine();
            writeMSG.flush();
        } catch (IOException e) {
            System.out.println("Failed to send message to " + name);
        }
    }

    private String getSender(String xmlString) throws Exception {
        DocumentBuilderFactory senderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder senderBuilder = senderFactory.newDocumentBuilder();
        Document senderDocument = (Document) senderBuilder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
        String[] content = senderDocument.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");
        String sender = content[0];
        return sender;
    }

    private String getReceiver(String xmlString) throws Exception {
        DocumentBuilderFactory senderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder senderBuilder = senderFactory.newDocumentBuilder();
        Document senderDocument = (Document) senderBuilder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
        String[] content = senderDocument.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");
        String receiver = content[1];
        return receiver;
    }

    private String getGroupName(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
        String[] content = document.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");
        return content[1];
    }

    private String getGroupMessage(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
        String[] content = document.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");
        return content[2];
    }

    private String getCommunityMessage(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
        String[] content = document.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");
        return content[1];
    }

    private String extractUsername(String xmlMsg) throws Exception {
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document doc = builder.parse(new ByteArrayInputStream(xmlMsg.getBytes("UTF-8")));
        String[] content = doc.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");
        return content[0];
    }

    public void logOut() throws IOException {
        synchronized (clients) {
            clients.remove(this);
        }
        socket.close();
    }

    private String[] extractContent(String xmlMsg) throws Exception {
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xmlMsg.getBytes("UTF-8")));

        if (doc.getElementsByTagName("content").getLength() > 0) {
            return doc.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");
        } else {
            return doc.getDocumentElement().getTextContent().split("!\\[]!");
        }
    }
}