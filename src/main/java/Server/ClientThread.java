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
    // **CHANGED: Added admin flag**
    private boolean isAdmin = false;
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
                    // **CHANGED: Added admin login cases**
                    case "ADMIN_LOGIN_SUCCESS":
                        String adminUsername = extractUsername(xmlMsg);
                        System.out.println("[DEBUG] Admin login successful for: " + adminUsername);

                        // Check for existing admin sessions
                        ClientThread existingAdmin = null;
                        synchronized (clients) {
                            for (ClientThread clientThread : clients) {
                                if (clientThread.name != null && clientThread.name.equals(adminUsername)
                                        && clientThread.isAdmin && clientThread != this) {
                                    existingAdmin = clientThread;
                                    System.out.println("[DEBUG] Found existing admin session: " + clientThread.name);
                                    break;
                                }
                            }
                        }

                        if (existingAdmin != null) {
                            System.out.println("[DEBUG] Forcing logout of existing admin: " + existingAdmin.name);
                            existingAdmin.sendMessage("<Response><reply>FORCE_LOGOUT</reply><body>Admin logged in from another location</body></Response>");
                            try {
                                existingAdmin.logOut();
                            } catch (IOException e) {
                                System.err.println("[ERROR] Failed to logout existing admin: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }

                        this.name = adminUsername;
                        this.isAdmin = true;
                        System.out.println("[DEBUG] Set admin flags - name: " + this.name + ", isAdmin: " + this.isAdmin);

                        String welcomeMessage = "<Response><reply>ADMIN_LOGIN_SUCCESS</reply><body>Welcome Admin " + this.name + "!</body></Response>";
                        System.out.println("[DEBUG] Sending welcome message: " + welcomeMessage);
                        sendMessage(welcomeMessage);
                        break;

                    case "ADMIN_LOGIN_FAILED":
                        System.out.println("[DEBUG] Admin login failed for credentials");
                        sendMessage("<Response><reply>ADMIN_LOGIN_FAILED</reply><body>Invalid admin credentials</body></Response>");
                        break;

                    // **CHANGED: Added admin operation cases**
                    case "ALL_USERS":
                        String usersData = xmlHandler.getAllUsersData();
                        sendMessage("<Response><reply>ALL_USERS</reply><body>" + usersData + "</body></Response>");
                        break;

                    case "ALL_GROUPS":
                        String groupsData = xmlHandler.getAllGroupsData();
                        sendMessage("<Response><reply>ALL_GROUPS</reply><body>" + groupsData + "</body></Response>");
                        break;

                    case "SEARCH_USERS_RESULT":
                        String[] searchContent = extractContent(xmlMsg);
                        String userSearchResults = xmlHandler.searchUsersData(searchContent[0]);
                        sendMessage("<Response><reply>SEARCH_USERS_RESULT</reply><body>" + userSearchResults + "</body></Response>");
                        break;

                    case "SEARCH_GROUPS_RESULT":
                        String[] groupSearchContent = extractContent(xmlMsg);
                        String groupSearchResults = xmlHandler.searchGroupsData(groupSearchContent[0]);
                        sendMessage("<Response><reply>SEARCH_GROUPS_RESULT</reply><body>" + groupSearchResults + "</body></Response>");
                        break;

                    case "USER_DELETED":
                        String[] deleteUserContent = extractContent(xmlMsg);
                        sendMessage("<Response><reply>USER_DELETED</reply><body>User " + deleteUserContent[0] + " deleted successfully</body></Response>");
                        break;

                    case "USER_DELETE_FAILED":
                        sendMessage("<Response><reply>USER_DELETE_FAILED</reply><body>Failed to delete user</body></Response>");
                        break;

                    case "GROUP_DELETED":
                        String[] deleteGroupContent = extractContent(xmlMsg);
                        sendMessage("<Response><reply>GROUP_DELETED</reply><body>Group deleted successfully</body></Response>");
                        break;

                    case "GROUP_DELETE_FAILED":
                        sendMessage("<Response><reply>GROUP_DELETE_FAILED</reply><body>Failed to delete group</body></Response>");
                        break;

                    case "PASSWORD_CHANGED":
                        String[] passwordContent = extractContent(xmlMsg);
                        sendMessage("<Response><reply>PASSWORD_CHANGED</reply><body>Password changed for " + passwordContent[0] + "</body></Response>");
                        break;

                    case "PASSWORD_CHANGE_FAILED":
                        sendMessage("<Response><reply>PASSWORD_CHANGE_FAILED</reply><body>Failed to change password</body></Response>");
                        break;

                    case "USER_CREATED":
                        String[] createUserContent = extractContent(xmlMsg);
                        sendMessage("<Response><reply>USER_CREATED</reply><body>User " + createUserContent[0] + " created successfully</body></Response>");
                        break;

                    case "USER_CREATE_FAILED":
                        sendMessage("<Response><reply>USER_CREATE_FAILED</reply><body>Failed to create user</body></Response>");
                        break;

                    case "GROUP_CREATED":
                        String[] createGroupContent = extractContent(xmlMsg);
                        sendMessage("<Response><reply>GROUP_CREATED</reply><body>Group " + createGroupContent[0] + " created successfully</body></Response>");
                        break;

                    case "GROUP_CREATE_FAILED":
                        sendMessage("<Response><reply>GROUP_CREATE_FAILED</reply><body>Failed to create group</body></Response>");
                        break;

                    case "USER_STATS":
                        String[] statsContent = extractContent(xmlMsg);
                        String userStats = xmlHandler.getUserStats(statsContent[0]);
                        sendMessage("<Response><reply>USER_STATS</reply><body>" + userStats + "</body></Response>");
                        break;

                    case "ACTIVE_USERS":
                        List<String> activeUsers = ServerXMLHandler.getActiveUsers();
                        String activeUsersStr = String.join(",", activeUsers);
                        sendMessage("<Response><reply>ACTIVE_USERS</reply><body>" + activeUsersStr + "</body></Response>");
                        break;

                    case "COMMUNITY_MESSAGES":
                        List<String> communityMessages = ServerXMLHandler.getCommunityMessages();
                        String messagesStr = String.join("![]!", communityMessages);
                        sendMessage("<Response><reply>COMMUNITY_MESSAGES</reply><body>" + messagesStr + "</body></Response>");
                        break;

                    case "LOGIN_SUCCESS":
                        String newUsername = extractUsername(xmlMsg);
                        ClientThread existingClient = null;
                        synchronized (clients) {
                            for (ClientThread clientThread : clients) {
                                if (clientThread.name != null && clientThread.name.equals(newUsername)
                                        && !clientThread.isAdmin && clientThread != this) {
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
                        this.isAdmin = false;
                        sendMessage("<Response><reply>LOGIN_SUCCESS</reply><body>Welcome " + this.name + "!</body></Response>");
                        break;

                    case "LOGIN_FAILED":
                        sendMessage("<Response><reply>LOGIN_FAILED</reply><body>Invalid username or password</body></Response>");
                        break;

                    case "REGISTER_SUCCESS":
                        sendMessage("<Response><reply>REGISTER_SUCCESS</reply><body>Registration successful</body></Response>");
                        break;

                    case "REGISTER_FAILED":
                        sendMessage("<Response><reply>REGISTER_FAILED</reply><body>Registration failed</body></Response>");
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
                            sendMessage("<Response><reply>FRIEND_REQUEST_SENT</reply><body>Request sent to " + addContent[1] + "</body></Response>");
                        } else {
                            sendMessage("<Response><reply>FRIEND_REQUEST_FAILED</reply><body>Failed to send request</body></Response>");
                        }
                        break;

                    case "LIST_REQUESTS":
                        String[] listContent = extractContent(xmlMsg);
                        java.util.List<String> requests = xmlHandler.listFriendRequests(listContent[0]);
                        sendMessage("<Response><reply>REQUEST_LIST</reply><body>" + String.join(",", requests) + "</body></Response>");
                        break;

                    case "ACCEPT_REQUEST":
                        String[] acceptContent = extractContent(xmlMsg);
                        if (xmlHandler.acceptFriendRequest(acceptContent[0], acceptContent[1])) {
                            sendMessage("<Response><reply>REQUEST_ACCEPTED</reply><body>You are now friends with " + acceptContent[1] + "</body></Response>");
                        } else {
                            sendMessage("<Response><reply>REQUEST_ACCEPT_FAILED</reply><body>Failed to accept request</body></Response>");
                        }
                        break;

                    case "DECLINE_REQUEST":
                        String[] declineContent = extractContent(xmlMsg);
                        if (xmlHandler.declineFriendRequest(declineContent[0], declineContent[1])) {
                            sendMessage("<Response><reply>REQUEST_DECLINED</reply><body>Request from " + declineContent[1] + " declined</body></Response>");
                        } else {
                            sendMessage("<Response><reply>REQUEST_DECLINE_FAILED</reply><body>Failed to decline request</body></Response>");
                        }
                        break;

                    case "CREATE_GROUP":
                        createGroupContent = extractContent(xmlMsg);
                        if (xmlHandler.createGroup(createGroupContent[0], createGroupContent[1])) {
                            sendMessage("<Response><reply>GROUP_CREATED</reply><body>Group " + createGroupContent[0] + " created successfully</body></Response>");
                        } else {
                            sendMessage("<Response><reply>GROUP_CREATE_FAILED</reply><body>Failed to create group</body></Response>");
                        }
                        break;

                    case "JOIN_GROUP":
                        String[] joinGroupContent = extractContent(xmlMsg);
                        if (xmlHandler.joinGroupWithHistory(joinGroupContent[0], joinGroupContent[1])) {
                            sendMessage("<Response><reply>GROUP_JOINED</reply><body>Joined group " + joinGroupContent[0] + " with full chat history</body></Response>");
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
                            sendMessage("<Response><reply>GROUP_JOIN_FAILED</reply><body>Failed to join group</body></Response>");
                        }
                        break;

                    case "LIST_GROUPS":
                        String[] listGroupsContent = extractContent(xmlMsg);
                        java.util.List<String> groups = xmlHandler.getUserGroups(listGroupsContent[0]);
                        sendMessage("<Response><reply>GROUPS_LIST</reply><body>" + String.join(",", groups) + "</body></Response>");
                        break;

                    case "SEND_GROUP_MESSAGE":
                        String groupSender = getSender(xmlMsg);
                        String targetGroup = getGroupName(xmlMsg);
                        String groupMessage = getGroupMessage(xmlMsg);

                        System.out.println("Processing group message from " + groupSender + " to group " + targetGroup);
                        if (!xmlHandler.isUserInGroup(groupSender, targetGroup)) {
                            sendMessage("<Response><reply>GROUP_ACCESS_DENIED</reply><body>You are not a member of group " + targetGroup + "</body></Response>");
                            System.out.println("Access denied: " + groupSender + " is not a member of group " + targetGroup);
                            break;
                        }

                        if (targetGroup.equals("Project Team") || targetGroup.equals("Study Group")) {
                            List<String> actualUserGroups = xmlHandler.getUserGroups(groupSender);
                            if (!actualUserGroups.contains(targetGroup)) {
                                sendMessage("<Response><reply>GROUP_NOT_FOUND</reply><body>Group " + targetGroup + " does not exist or you are not a member</body></Response>");
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

                        String fileResponse = reply.equals("SEND_FILE_SUCCESS") ?
                                "<Response><reply>FILE_SENT</reply><body>File sent successfully</body></Response>" :
                                "<Response><reply>FILE_SEND_FAILED</reply><body>Failed to send file</body></Response>";

                        sendMessage(fileResponse);

                        if (reply.equals("SEND_FILE_SUCCESS")) {
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
                if (c.socket.isConnected() && c.name != null && !c.isAdmin) {
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
        // **CHANGED: Remove user from active users when logging out**
        if (name != null && !isAdmin) {
            ServerXMLHandler.removeActiveUser(name);
        }
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