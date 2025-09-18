package Server;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;


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

                        // Set this client's name and send success response
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
                        if (xmlHandler.joinGroup(joinGroupContent[0], joinGroupContent[1])) {
                            sendMessage("<Response status=\"GROUP_JOINED\"><body>Joined group " + joinGroupContent[0] + "</body></Response>");
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

                        // Send updated chat history to all group members
                        java.util.List<String> groupMembers = xmlHandler.getGroupMembers(targetGroup);
                        for (String member : groupMembers) {
                            for (ClientThread client : clients) {
                                if (client.socket.isConnected() && client.name != null && client.name.equals(member)) {
                                    try {
                                        File userFile = new File("src/main/resources/XMLFiles/" + client.name + "Account.xml");
                                        if (userFile.exists()) {
                                            byte[] fileContent = java.nio.file.Files.readAllBytes(userFile.toPath());
                                            String encodedContent = java.util.Base64.getEncoder().encodeToString(fileContent);
                                            client.sendMessage("<Response><reply>NEW_GROUP_MESSAGE</reply><body>" + encodedContent + "</body></Response>");
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        break;

                    case "SEND_MESSAGE":
                        String sender = getSender(xmlMsg);
                        String receiver = getReceiver(xmlMsg);

                        // Send updated chat history to both participants
                        for (ClientThread client : clients) {
                            if (client.socket.isConnected() &&
                                    (client.name.equals(sender) || client.name.equals(receiver))) {

                                try {
                                    // Send the updated chat history for the specific conversation
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

                        // Broadcast community message to all connected clients (no history saved)
                        String communityBroadcast = "<Response><reply>COMMUNITY_MESSAGE</reply>" +
                                "<body>" + communitySender + "![]!" + communityMessage + "</body></Response>";
                        broadcastCommunityMessage(communityBroadcast);
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

    // Broadcasts community message to all connected clients
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

    // Broadcasts message to all connected clients
    private void broadcast(String message) {
        synchronized (clients) {
            for (ClientThread c : clients) {
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

    // Sends a direct message to this client
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
        return content[1]; // Group name is at index 1
    }

    private String getGroupMessage(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
        String[] content = document.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");
        return content[2]; // Message is at index 2
    }

    private String getCommunityMessage(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
        String[] content = document.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");
        return content[1]; // Community message is at index 1
    }

    // Extract username from <content> (split by "waw")
    private String extractUsername(String xmlMsg) throws Exception {
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document doc = builder.parse(new ByteArrayInputStream(xmlMsg.getBytes("UTF-8")));
        String[] content = doc.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");
        return content[0]; // username
    }

    // Extract message body from <content> (for SEND_MESSAGE)
    private String extractBody(String xmlMsg) throws Exception {
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document doc = builder.parse(new ByteArrayInputStream(xmlMsg.getBytes("UTF-8")));
        String[] content = doc.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");
        return content.length > 1 ? content[1] : ""; // return the message part after "waw"
    }

    // Logout and notify others
    public void logOut() throws IOException {
        synchronized (clients) {
            clients.remove(this);
        }
        socket.close();
    }

    private void debugClientState() {
        System.out.println("=== CLIENT DEBUG INFO ===");
        System.out.println("Client name: " + this.name);
        System.out.println("Socket connected: " + !socket.isClosed());
        System.out.println("Total connected clients: " + clients.size());
        synchronized (clients) {
            for (ClientThread c : clients) {
                System.out.println("  - Client: " + c.name + " (connected: " + !c.socket.isClosed() + ")");
            }
        }
        System.out.println("========================");
    }

    //This for the Friend Getting index 0 as requester and then 1index 1 as friend
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

    // Broadcast message to all members of a specific group
    private void broadcastToGroup(String groupName, String message) {
        synchronized (clients) {
            for (ClientThread c : new ArrayList<>(clients)) {
                try {
                    // Check if this client is a member of the group
                    if (xmlHandler.isUserInGroup(c.name, groupName)) {
                        c.writeMSG.write(message);
                        c.writeMSG.newLine();
                        c.writeMSG.flush();
                    }
                } catch (IOException e) {
                    try {
                        c.logOut();
                    } catch (IOException ignored) {}
                }
            }
        }
    }
}