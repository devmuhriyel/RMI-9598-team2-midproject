package Server;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;

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

                    case "SEND_MESSAGE":
                        String senderName = xmlHandler.getSender(xmlMsg);
                        String receiverName = xmlHandler.getReceiver(xmlMsg);

                        byte[] senderFileContent = Files.readAllBytes(Paths.get("src/main/resources/XMLFiles/" + senderName + "Account.xml"));
                        sendMessage("<Response><reply>SEND_MESSAGE</reply><body>" +
                                Base64.getEncoder().encodeToString(senderFileContent) + "</body></Response>");

                        ClientThread receiverClient = null;
                        synchronized (clients) {
                            for (ClientThread c : clients) {
                                if (c.name != null && c.name.equals(receiverName)) {
                                    receiverClient = c;
                                    break;
                                }
                            }
                        }

                        if (receiverClient != null) {
                            byte[] receiverFileContent = Files.readAllBytes(Paths.get("src/main/resources/XMLFiles/" + receiverName + "Account.xml"));
                            receiverClient.sendMessage("<Response><reply>SEND_MESSAGE</reply><body>" +
                                    Base64.getEncoder().encodeToString(receiverFileContent) + "</body></Response>");
                        } else {
                            System.out.println(receiverName + " is offline. Message saved in their Account.xml");
                        }
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
// Replace the SEND_GROUP_MESSAGE case in ClientThread.java
                    case "SEND_GROUP_MESSAGE":
                        String[] groupMsgContent = extractContent(xmlMsg);
                        String groupSender = groupMsgContent[0];    // sender
                        String groupName = groupMsgContent[1];      // group name
                        String groupMessage = groupMsgContent[2];   // message

                        // Get all group members and update their XML files
                        java.util.List<String> groupMembers = xmlHandler.getGroupMembers(groupName);
                        for (String member : groupMembers) {
                            try {
                                byte[] memberFileContent = Files.readAllBytes(Paths.get("src/main/resources/XMLFiles/" + member + "Account.xml"));

                                // Send updated XML to member if they're online
                                ClientThread memberClient = null;
                                synchronized (clients) {
                                    for (ClientThread c : clients) {
                                        if (c.name != null && c.name.equals(member)) {
                                            memberClient = c;
                                            break;
                                        }
                                    }
                                }

                                if (memberClient != null) {
                                    memberClient.sendMessage("<Response><reply>SEND_GROUP_MESSAGE</reply><body>" +
                                            Base64.getEncoder().encodeToString(memberFileContent) + "</body></Response>");
                                }
                            } catch (Exception e) {
                                System.out.println("Failed to update XML for group member: " + member);
                                e.printStackTrace();
                            }
                        }
                        break;

                    default:
                        sendMessage("<Response status=\"ERROR\"><body>Unknown request</body></Response>");
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

    // Broadcasts message to all connected clients
    private void broadcast(String message) {
        synchronized (clients) {
            for (ClientThread c : new ArrayList<>(clients)) {
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
        if (name != null) {
            broadcast("SERVER: " + this.name + " has disconnected.");
        }
        socket.close();
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
