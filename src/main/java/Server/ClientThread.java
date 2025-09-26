package Server;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientThread implements Runnable {
    //VARIABLES
    private Socket socket;
    private BufferedReader readMSG;
    private BufferedWriter writeMSG;
    private String name;
    private boolean isAdmin = false;
    private boolean isPasswordResetRequester;
    private static final Map<String, ClientThread> passwordResetRequesters = new ConcurrentHashMap<>();
    private static final ArrayList<ClientThread> clients = new ArrayList<>();
    private ServerXMLHandler xmlHandler = new ServerXMLHandler();

    //METHODS
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
            String[] createGroupContent;
            String newUsername;
            String[] resetParts;
            while ((xmlMsg = readMSG.readLine()) != null) {
                System.out.println("Received XML: " + xmlMsg);
                String reply = xmlHandler.readMessage(xmlMsg);
                if (reply.startsWith("SEARCH_USERS_RESULT![]!")) {
                    String[] parts = reply.split("!\\[]!", 2);
                    String searchResults = parts.length > 1 ? parts[1] : "";
                    sendMessage("<Response><reply>SEARCH_USERS_RESULT</reply><body>" + searchResults + "</body></Response>");
                    continue;
                }
                if (reply.startsWith("SEARCH_GROUPS_RESULT![]!")) {
                    String[] parts = reply.split("!\\[]!", 2);
                    String searchResults = parts.length > 1 ? parts[1] : "";
                    sendMessage("<Response><reply>SEARCH_GROUPS_RESULT</reply><body>" + searchResults + "</body></Response>");
                    continue;
                }
                if (reply.startsWith("USER_STATS![]!")) {
                    String[] parts = reply.split("!\\[]!", 2);
                    String userStats = parts.length > 1 ? parts[1] : "";
                    sendMessage("<Response><reply>USER_STATS</reply><body>" + userStats + "</body></Response>");
                    continue;
                }
                if (reply.startsWith("ACTIVE_USERS![]!")) {
                    String[] parts = reply.split("!\\[]!", 2);
                    String activeUsersStr = parts.length > 1 ? parts[1] : "";
                    sendMessage("<Response><reply>ACTIVE_USERS</reply><body>" + activeUsersStr + "</body></Response>");
                    continue;
                }
                if (reply.startsWith("COMMUNITY_MESSAGES![]!")) {
                    String[] parts = reply.split("!\\[]!", 2);
                    String messagesStr = parts.length > 1 ? parts[1] : "";
                    sendMessage("<Response><reply>COMMUNITY_MESSAGES</reply><body>" + messagesStr + "</body></Response>");
                    continue;
                }
                switch (reply) {
                    //ADMIN LOGIN CASES
                    case "ADMIN_LOGIN_SUCCESS":
                        String adminUsername = extractUsername(xmlMsg);
                        ClientThread existingAdmin = null;
                        synchronized (clients) {
                            for (ClientThread clientThread : clients) {
                                if (clientThread.name != null && clientThread.name.equals(adminUsername)
                                        && clientThread.isAdmin && clientThread != this) {
                                    existingAdmin = clientThread;
                                    break;
                                }
                            }
                        }
                        if (existingAdmin != null) {
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
                        String welcomeMessage = "<Response><reply>ADMIN_LOGIN_SUCCESS</reply><body>Welcome Admin " + this.name + "!</body></Response>";
                        sendMessage(welcomeMessage);
                        break;
                    case "ADMIN_LOGIN_FAILED":
                        sendMessage("<Response><reply>ADMIN_LOGIN_FAILED</reply><body>Invalid admin credentials</body></Response>");
                        break;
                    case "ADMIN_VERIFICATION_CODE_VALID":
                        String[] adminVerificationParts = reply.split("!\\[]!");
                        String verifiedAdminUsername = adminVerificationParts.length > 1 ? adminVerificationParts[1] : extractUsername(xmlMsg);
                        sendMessage("<Response><reply>ADMIN_VERIFICATION_CODE_VALID</reply><body>" + verifiedAdminUsername + "</body></Response>");
                        System.out.println("[DEBUG] Admin verification code valid sent to client");
                        break;

                    case "ADMIN_VERIFICATION_CODE_INVALID":
                        sendMessage("<Response><reply>ADMIN_VERIFICATION_CODE_INVALID</reply><body>Invalid admin verification code</body></Response>");
                        System.out.println("[DEBUG] Admin verification code invalid sent to client");
                        break;

                    //ADMIN LOGIC CASES
                    case "ALL_USERS":
                        String usersData = xmlHandler.getAllUsersData();
                        sendMessage("<Response><reply>ALL_USERS</reply><body>" + usersData + "</body></Response>");
                        break;
                    case "ALL_GROUPS":
                        String groupsData = xmlHandler.getAllGroupsData();
                        sendMessage("<Response><reply>ALL_GROUPS</reply><body>" + groupsData + "</body></Response>");
                        break;
                    case "SEARCH_USERS_RESULT":
                        String[] parts = reply.split("!\\[]!", 2);
                        String searchResults = parts.length > 1 ? parts[1] : "";
                        sendMessage("<Response><reply>SEARCH_USERS_RESULT</reply><body>" + searchResults + "</body></Response>");
                        break;
                    case "SEARCH_GROUPS_RESULT":
                        String[] groupParts = reply.split("!\\[]!", 2);
                        String groupResults = groupParts.length > 1 ? groupParts[1] : "";
                        sendMessage("<Response><reply>SEARCH_GROUPS_RESULT</reply><body>" + groupResults + "</body></Response>");
                        break;
                    case "USER_DELETED":
                        String[] deleteUserContent = extractContent(xmlMsg);
                        sendMessage("<Response><reply>USER_DELETED</reply><body>User " + deleteUserContent[0] + " deleted successfully</body></Response>");
                        break;
                    case "USER_DELETE_FAILED":
                        sendMessage("<Response><reply>USER_DELETE_FAILED</reply><body>Failed to delete user</body></Response>");
                        break;
                    case "GROUP_DELETED":
                        sendMessage("<Response><reply>GROUP_DELETED</reply><body>Group deleted successfully</body></Response>");
                        break;
                    case "GROUP_DELETE_FAILED":
                        sendMessage("<Response><reply>GROUP_DELETE_FAILED</reply><body>Failed to delete group</body></Response>");
                        break;
                    case "PASSWORD_CHANGED":
                        String[] passwordChangeContent = extractContent(xmlMsg);
                        String passwordUsername = passwordChangeContent.length > 0 ? passwordChangeContent[0] : this.name;

                        // Send success response
                        sendMessage("<Response status=\"PASSWORD_CHANGED\"><body>Password changed successfully</body></Response>");

                        // Send updated account XML (password changes don't affect the account XML content directly,
                        // but this ensures client has the latest data)
                        if (passwordUsername != null) {
                            sendUpdatedAccountXML(passwordUsername);
                        }
                        break;

                    case "PASSWORD_CHANGE_FAILED":
                        sendMessage("<Response status=\"PASSWORD_CHANGE_FAILED\"><body>Failed to change password. Please try again.</body></Response>");
                        break;

                    case "USER_CREATED":
                        String[] createUserContent = extractContent(xmlMsg);
                        sendMessage("<Response><reply>USER_CREATED</reply><body>User " + createUserContent[0] + " created successfully</body></Response>");
                        break;
                    case "USER_CREATE_FAILED":
                        sendMessage("<Response><reply>USER_CREATE_FAILED</reply><body>Failed to create user</body></Response>");
                        break;
                    case "GROUP_CREATED":
                        createGroupContent = extractContent(xmlMsg);
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
                    case "GET_ALL_USERS_WITH_STATUS":
                        String usersDataWithStatus = xmlHandler.getAllUsersDataWithStatus();
                        sendMessage("<Response><reply>USERS_WITH_STATUS</reply><body>" + usersDataWithStatus + "</body></Response>");
                        break;
                    case "GET_ACTIVE_USERS_ONLY":
                        String activeUsersData = xmlHandler.getActiveUsersData();
                        sendMessage("<Response><reply>ACTIVE_USERS_ONLY</reply><body>" + activeUsersData + "</body></Response>");
                        break;
                    case "GET_ALL_GROUPS_ENHANCED":
                        String groupsDataEnhanced = xmlHandler.getAllGroupsDataEnhanced();
                        sendMessage("<Response><reply>GROUPS_ENHANCED</reply><body>" + groupsDataEnhanced + "</body></Response>");
                        break;
                    case "CREATE_GROUP_WITH_MEMBERS":
                        createGroupContent = extractContent(xmlMsg);
                        String groupName = createGroupContent[0];
                        String creator = createGroupContent[1];
                        String membersStr = createGroupContent.length > 2 ? createGroupContent[2] : "";
                        List<String> members = new ArrayList<>();
                        if (!membersStr.isEmpty()) {
                            members = Arrays.asList(membersStr.split(","));
                        }
                        if (xmlHandler.createGroupWithMembers(groupName, creator, members)) {
                            sendMessage("<Response><reply>GROUP_CREATED</reply><body>Group created with members</body></Response>");
                        } else {
                            sendMessage("<Response><reply>GROUP_CREATE_FAILED</reply><body>Failed to create group</body></Response>");
                        }
                        break;

                    //USER LOGIN AND SIGNUP CASES
                    case "LOGIN_SUCCESS":
                        newUsername = extractUsername(xmlMsg);
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

                    //FORGOT PASSWORD CASES
                    case "PASSWORD_RESET_SUCCESS":
                        resetParts = reply.split("!\\[]!");
                        if (resetParts.length >= 4) {
                            String resetUsername = resetParts[1];
                            String newPassword = resetParts[2];
                            String message = resetParts[3];
                            sendMessage("<Response><reply>PASSWORD_RESET_SUCCESS</reply><body>" +
                                    resetUsername + "![]!" + newPassword + "![]!" + message + "</body></Response>");
                            removePasswordResetRequester(resetUsername);
                            this.name = null;
                            this.isPasswordResetRequester = false;
                        }
                        break;
                    case "PASSWORD_RESET_FAILED":
                        resetParts = reply.split("!\\[]!");
                        if (resetParts.length >= 3) {
                            String resetUsername = resetParts[1];
                            String errorMessage = resetParts[2];
                            sendMessage("<Response><reply>PASSWORD_RESET_FAILED</reply><body>" + errorMessage + "</body></Response>");
                            removePasswordResetRequester(resetUsername);
                            this.name = null;
                            this.isPasswordResetRequester = false;
                        }
                        break;
                    case "VERIFICATION_CODE_INVALID":
                        sendMessage("<Response><reply>VERIFICATION_CODE_INVALID</reply><body>Invalid verification code</body></Response>");
                        break;
                    case "VERIFICATION_CODE_VALID":
                        String[] verificationParts = reply.split("!\\[]!");
                        String verifiedUsername = verificationParts.length > 1 ? verificationParts[1] : extractUsername(xmlMsg);
                        this.name = verifiedUsername;
                        this.isPasswordResetRequester = true;
                        registerPasswordResetRequester(verifiedUsername, this);
                        sendMessage("<Response><reply>VERIFICATION_CODE_VALID</reply><body>" + verifiedUsername + "</body></Response>");
                        break;

                    //USER FUNCTIONS CASES
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
                    case "FRIEND_REQUEST_SENT":
                        sendMessage("<Response><reply>FRIEND_REQUEST_SENT</reply><body>Friend request sent successfully</body></Response>");
                        break;
                    case "FRIEND_REQUEST_FAILED":
                        sendMessage("<Response><reply>FRIEND_REQUEST_FAILED</reply><body>Failed to send friend request</body></Response>");
                        break;
                    case "FRIEND_REQUESTS":
                        String[] requestParts = reply.split("!\\[]!", 2);
                        String requestData = requestParts.length > 1 ? requestParts[1] : "";
                        sendMessage("<Response><reply>FRIEND_REQUESTS</reply><body>" + requestData + "</body></Response>");
                        break;
                    case "FRIEND_REQUEST_ACCEPTED":
                        sendMessage("<Response><reply>FRIEND_REQUEST_ACCEPTED</reply><body>Friend request accepted</body></Response>");
                        break;
                    case "FRIEND_REQUEST_DECLINED":
                        sendMessage("<Response><reply>FRIEND_REQUEST_DECLINED</reply><body>Friend request declined</body></Response>");
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
                        String fileResponse = "<Response><reply>FILE_SENT</reply><body>File sent successfully</body></Response>";
                        sendMessage(fileResponse);
                        for (ClientThread client : clients) {
                            if (client.socket.isConnected() && (client.name.equals(fileSender) || client.name.equals(fileReceiver))) {
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
                        break;
                    case "SEND_GROUP_FILE":
                        String groupFileSender = getSender(xmlMsg);
                        String targetGroupForFile = getGroupName(xmlMsg);
                        System.out.println("Processing group file transfer from " + groupFileSender + " to group " + targetGroupForFile);
                        String groupFileResponse = "<Response><reply>GROUP_FILE_SEND_FAILED</reply><body>Failed to send file to group</body></Response>";
                        sendMessage(groupFileResponse);
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

                    //ACCOUNT FUNCTION CASES
                    case "REQUEST_PASSWORD_RESET":
                        String[] resetContent = extractContent(xmlMsg);
                        String resetUsername = resetContent[0];
                        String verificationCode = xmlHandler.generateVerificationCode(resetUsername);
                        if (verificationCode != null) {
                            sendMessage("<Response><reply>PASSWORD_RESET_REQUESTED</reply><body>" + resetUsername + "![]!" + verificationCode + "</body></Response>");
                        } else {
                            sendMessage("<Response><reply>PASSWORD_RESET_FAILED</reply><body>User not found</body></Response>");
                        }
                        break;
                    case "USERNAME_CHANGED":
                        String[] usernameChangeContent = extractContent(xmlMsg);

                        if (usernameChangeContent.length > 1) {
                            newUsername = usernameChangeContent[1];
                            this.name = newUsername;

                            // Send success response
                            sendMessage("<Response status=\"USERNAME_CHANGED\"><body>Username successfully changed to " + newUsername + "</body></Response>");

                            // Send updated account XML
                            sendUpdatedAccountXML(newUsername);
                        } else {
                            sendMessage("<Response status=\"USERNAME_CHANGED\"><body>Username successfully changed</body></Response>");
                        }
                        break;
                    case "USERNAME_CHANGE_FAILED":
                        sendMessage("<Response><type> USERNAME_CHANGE_FAILED</type><body>Failed to change username. Please try again.</body></Response>");
                        break;
                    case "USERNAME_EXISTS":
                        sendMessage("<Response><type> USERNAME_EXISTS</type><body>Username already exists. Please choose a different username.</body></Response>");
                        break;
                    case "USER_EXISTS":
                        sendMessage("<Response><reply>USER_EXISTS</reply><body>Username already exists. Please choose a different username.</body></Response>");
                        break;
                    case "INVALID_OLD_PASSWORD":
                        sendMessage("<Response><type> INVALID_PASSWORD</type><body>Current password is incorrect.</body></Response>");
                        break;
                    case "FRIEND_REQUEST_ACCEPTED_NEW":
                        // Send success response and updated account data
                        sendMessage("<Response><reply>FRIEND_REQUEST_ACCEPTED_NEW</reply><body>Friend request accepted successfully</body></Response>");

                        // Send updated account XML to both users involved
                        acceptContent = extractContent(xmlMsg);
                        if (acceptContent.length >= 2) {
                            String acceptingUser = acceptContent[0];
                            requestingUser = acceptContent[1];

                            sendUpdatedAccountXMLToUser(acceptingUser);
                            sendUpdatedAccountXMLToUser(requestingUser);
                        }
                        break;
                    case "FRIEND_REQUEST_DECLINED_NEW":
                        // Send success response and updated account data
                        sendMessage("<Response><reply>FRIEND_REQUEST_DECLINED_NEW</reply><body>Friend request declined</body></Response>");

                        // Send updated account XML to both users involved
                        declineContent = extractContent(xmlMsg);
                        if (declineContent.length >= 2) {
                            String decliningUser = declineContent[0];
                            String requestingUserDecline = declineContent[1];

                            sendUpdatedAccountXMLToUser(decliningUser);
                            sendUpdatedAccountXMLToUser(requestingUserDecline);
                        }
                        break;
                    case "FRIEND_REQUEST_ADDED":
                        sendMessage("<Response><reply>FRIEND_REQUEST_ADDED</reply><body>Friend request sent successfully</body></Response>");

                        // Send updated account XML to both users
                        String[] addRequestContent = extractContent(xmlMsg);
                        if (addRequestContent.length >= 2) {
                            sendUpdatedAccountXMLToUser(addRequestContent[0]); // sender
                            sendUpdatedAccountXMLToUser(addRequestContent[1]); // receiver
                        }
                        break;

                    case "SENT_REQUESTS":
                        String[] sentParts = reply.split("!\\[]!", 2);
                        String sentRequestData = sentParts.length > 1 ? sentParts[1] : "";
                        sendMessage("<Response><reply>SENT_REQUESTS</reply><body>" + sentRequestData + "</body></Response>");
                        break;

                    case "RECEIVED_REQUESTS":
                        String[] receivedParts = reply.split("!\\[]!", 2);
                        String receivedRequestData = receivedParts.length > 1 ? receivedParts[1] : "";
                        sendMessage("<Response><reply>RECEIVED_REQUESTS</reply><body>" + receivedRequestData + "</body></Response>");
                        break;


                    //to validate


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
    private void sendUpdatedAccountXMLToUser(String username) {
        try {
            // Get the client thread for this user
            ClientThread targetClient = null;
            synchronized (clients) {
                for (ClientThread client : clients) {
                    if (client.name != null && client.name.equals(username)) {
                        targetClient = client;
                        break;
                    }
                }
            }

            if (targetClient != null) {
                // Read the user's account XML file
                File userFile = new File("src/main/resources/XMLFiles/" + username + "Account.xml");
                if (userFile.exists()) {
                    byte[] fileContent = java.nio.file.Files.readAllBytes(userFile.toPath());
                    String encodedContent = java.util.Base64.getEncoder().encodeToString(fileContent);

                    // Send the updated XML to the client
                    String response = "<Response><reply>ACCOUNT_UPDATED</reply><body>" + encodedContent + "</body></Response>";
                    targetClient.sendMessage(response);

                    System.out.println("Sent updated account XML to user: " + username);
                }
            } else {
                System.out.println("User " + username + " is not currently online");
            }
        } catch (Exception e) {
            System.err.println("Error sending updated XML to user " + username + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    //HELPER METHODS FOR SENDING MESSAGES
    private void sendMessage(String msg) {
        try {
            writeMSG.write(msg.trim());
            writeMSG.newLine();
            writeMSG.flush();
        } catch (IOException e) {
            System.out.println("Failed to send message to " + name);
        }
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
    private String getSender(String xmlString) throws Exception {
        DocumentBuilderFactory senderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder senderBuilder = senderFactory.newDocumentBuilder();
        Document senderDocument = senderBuilder.parse(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)));
        String[] content = senderDocument.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");
        return content[0];
    }

    private String getReceiver(String xmlString) throws Exception {
        DocumentBuilderFactory senderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder senderBuilder = senderFactory.newDocumentBuilder();
        Document senderDocument = senderBuilder.parse(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)));
        String[] content = senderDocument.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");
        return content[1];
    }

    private String getGroupName(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)));
        String[] content = document.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");
        return content[1];
    }

    private String getCommunityMessage(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)));
        String[] content = document.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");
        return content[1];
    }

    //HELPER METHODS FOR HANDLING PASSWORD CHANGES
    public static void registerPasswordResetRequester(String username, ClientThread clientThread) {
        synchronized (passwordResetRequesters) {
            passwordResetRequesters.put(username, clientThread);
            clientThread.isPasswordResetRequester = true;
            clientThread.name = username;
            System.out.println("[DEBUG] Registered password reset requester: " + username);
        }
    }
    public static ClientThread getPasswordResetRequester(String username) {
        synchronized (passwordResetRequesters) {
            return passwordResetRequesters.get(username);
        }
    }
    public static void removePasswordResetRequester(String username) {
        synchronized (passwordResetRequesters) {
            ClientThread client = passwordResetRequesters.remove(username);
            if (client != null) {
                client.isPasswordResetRequester = false;
                System.out.println("[DEBUG] Removed password reset requester: " + username);
            }
        }
    }
    public static boolean notifyPasswordResetRequester(String username, String newPassword) {
        ClientThread requester = getPasswordResetRequester(username);
        if (requester != null && requester.socket != null && !requester.socket.isClosed()) {
            try {
                String notification = "<Response><reply>PASSWORD_RESET_NOTIFICATION</reply><body>" +
                        username + "![]!" + newPassword + "</body></Response>";
                requester.sendMessage(notification);
                System.out.println("[DEBUG] Successfully notified password reset requester: " + username);
                removePasswordResetRequester(username);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    //HELPER METHODS FOR ADMIN FUNCTIONS
    private String extractUsername(String xmlMsg) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlMsg.getBytes(StandardCharsets.UTF_8)));
        String[] content = doc.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");
        return content[0];
    }
    private String[] extractContent(String xmlMsg) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new java.io.ByteArrayInputStream(xmlMsg.getBytes(StandardCharsets.UTF_8)));
        if (doc.getElementsByTagName("content").getLength() > 0) {
            return doc.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");
        } else {
            return doc.getDocumentElement().getTextContent().split("!\\[]!");
        }
    }

    public void logOut() throws IOException {
        if (name != null && !isAdmin) {
            ServerXMLHandler.removeActiveUser(name);
        }
        synchronized (clients) {
            clients.remove(this);
        }
        socket.close();
    }

    private void sendUpdatedAccountXML(String username) {
        try {
            File userFile = new File("src/main/resources/XMLFiles/" + username + "Account.xml");
            if (userFile.exists()) {
                byte[] fileContent = java.nio.file.Files.readAllBytes(userFile.toPath());
                String encodedContent = java.util.Base64.getEncoder().encodeToString(fileContent);
                sendMessage("<Response><reply>GET_CHAT_HISTORY</reply><body>" + encodedContent + "</body></Response>");
                System.out.println("Sent updated account XML to user: " + username);
            } else {
                System.err.println("Account file not found for user: " + username);
            }
        } catch (Exception e) {
            System.err.println("Error sending updated account XML: " + e.getMessage());
            e.printStackTrace();
        }
    }


}