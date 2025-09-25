package Server;

import org.w3c.dom.*;
import org.xml.sax.SAXException;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class ServerXMLHandler {
    //VARIABLES
    private static final String FILE_PATH = "src/main/resources/XMLFiles/Users.xml";
    private static final String ADMIN_FILE_PATH = "src/main/resources/XMLFiles/AdminCredentials.xml";
    private static final List<String> communityMessages = new ArrayList<>();
    private static final List<String> activeUsers = new ArrayList<>();

    //REQUEST HANDLING METHOD
    public String readMessage(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
        doc.getDocumentElement().normalize();
        String type = doc.getElementsByTagName("type").item(0).getTextContent();
        String reply = "";

        String[] content = doc.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");

        switch (type) {
            case "ADMIN_LOGIN":
                String adminUsername = content[0];
                String adminPassword = content[1];
                if (validateAdmin(adminUsername, adminPassword)) {
                    reply = "ADMIN_LOGIN_SUCCESS";
                } else {
                    reply = "ADMIN_LOGIN_FAILED";
                }
                break;
            case "GET_ALL_USERS":
                reply = getAllUsersData();
                break;
            case "GET_ALL_GROUPS":
                reply = getAllGroupsData();
                break;
            case "DELETE_USER":
                String userToDelete = content[0];
                if (deleteUserFromXML(userToDelete)) {
                    reply = "USER_DELETED";
                } else {
                    reply = "USER_DELETE_FAILED";
                }
                break;
            case "DELETE_GROUP":
                String groupToDelete = content[0];
                String groupOwner = content[1];
                if (deleteGroupFromXML(groupToDelete, groupOwner)) {
                    reply = "GROUP_DELETED";
                } else {
                    reply = "GROUP_DELETE_FAILED";
                }
                break;

            case "CHANGE_USER_PASSWORD":
                String targetUser = content[0];
                String newPassword = content[1];
                if (changeUserPassword(targetUser, newPassword)) {
                    reply = "PASSWORD_CHANGED";
                    System.out.println("[DEBUG] Password change completed for user: " + targetUser);
                } else {
                    reply = "PASSWORD_CHANGE_FAILED";
                    System.out.println("[DEBUG] Password change failed for user: " + targetUser);
                }
                break;
            case "GET_USER_STATS":
                String statsUser = content[0];
                String userStats = getUserStats(statsUser);
                reply = "USER_STATS![]!" + userStats;
                break;
            case "SEARCH_USERS":
                String userKeyword = content[0];
                String searchResults = searchUsersData(userKeyword);
                reply = "SEARCH_USERS_RESULT![]!" + searchResults; // Remove the ![]! format
                break;

            case "SEARCH_GROUPS":
                String groupKeyword = content[0];
                String groupSearchResults = searchGroupsData(groupKeyword);
                reply = "SEARCH_GROUPS_RESULT![]!" + groupSearchResults; // Remove the ![]! format
                break;

            case "CREATE_USER":
                String newUsername = content[0];
                String newUserPassword = content[1];

                // First check if user already exists
                if (userExists(newUsername)) {
                    reply = "USER_EXISTS";
                } else {
                    try {
                        registerUser(newUsername, newUserPassword);
                        reply = "USER_CREATED";
                    } catch (Exception e) {
                        reply = "USER_CREATE_FAILED";
                    }
                }
                break;

            case "CREATE_GROUP_ADMIN":
                String newGroupName = content[0];
                String groupCreator = content[1];
                String membersStr = content.length > 2 ? content[2] : "";

                if (createGroupInUserAccounts(newGroupName, groupCreator, membersStr)) {
                    reply = "GROUP_CREATED";
                } else {
                    reply = "GROUP_CREATE_FAILED";
                }
                break;

            case "GET_ACTIVE_USERS":
                String activeUsersStr = String.join(",", getActiveUsers());
                reply = "ACTIVE_USERS![]!" + activeUsersStr;
                break;
            case "GET_COMMUNITY_MESSAGES":
                String messagesStr = String.join("![]!", getCommunityMessages());
                reply = "COMMUNITY_MESSAGES![]!" + messagesStr;
                break;
            case "LOGIN":
                String username = content[0];
                String password = content[1];
                boolean found = false;
                File userAccounts = new File(FILE_PATH);
                DocumentBuilderFactory loginFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder loginBuilder = loginFactory.newDocumentBuilder();
                Document loginDoc = loginBuilder.parse(userAccounts);
                NodeList users = loginDoc.getElementsByTagName("user");
                for (int i = 0; i < users.getLength(); i++) {
                    Node user = users.item(i);
                    if (user.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) user;
                        String uname = e.getElementsByTagName("username").item(0).getTextContent();
                        String pass = e.getElementsByTagName("password").item(0).getTextContent();
                        if (uname.equals(username) && pass.equals(password)) {
                            reply = "LOGIN_SUCCESS";
                            found = true;
                            addActiveUser(username);
                            break;
                        }
                    }
                }
                if (!found) {
                    reply = "LOGIN_FAILED";
                }
                break;
            case "REGISTER": {
                username = (content.length >= 3)
                        ? content[0].trim()
                        : (doc.getElementsByTagName("username").getLength() > 0
                        ? doc.getElementsByTagName("username").item(0).getTextContent().trim()
                        : null);

                password = (content.length >= 3)
                        ? content[1].trim()
                        : (doc.getElementsByTagName("password").getLength() > 0
                        ? doc.getElementsByTagName("password").item(0).getTextContent().trim()
                        : null);

                String verification = (content.length >= 3)
                        ? content[2].trim()
                        : (doc.getElementsByTagName("verification").getLength() > 0
                        ? doc.getElementsByTagName("verification").item(0).getTextContent().trim()
                        : null);

                if (username == null || username.isEmpty() ||
                        password == null || password.isEmpty() ||
                        verification == null || verification.isEmpty()) {
                    reply = "REGISTER_FAILED";
                    break;
                }

                synchronized (this) {
                    if (userExists(username)) {
                        reply = "USER_EXISTS";
                    } else {
                        boolean saved = addUserToDocument(username, password, verification); // update method
                        if (saved) {
                            try {
                                createUserAccountXML(username);
                                reply = "REGISTER_SUCCESS";
                            } catch (Exception e) {
                                System.err.println("Failed to create account XML for " + username + ": " + e.getMessage());
                                reply = "REGISTER_SUCCESS";
                            }
                        } else {
                            reply = "REGISTER_FAILED";
                        }
                    }
                }
                break;
            }
            case "CREATE_GROUP":
                reply = "CREATE_GROUP";
                break;
            case "JOIN_GROUP":
                reply = "JOIN_GROUP";
                break;
            case "LIST_GROUPS":
                reply = "LIST_GROUPS";
                break;
            case "SEND_MESSAGE":
                String sender = content[0];
                String receiver = content[1];
                String message = content[2];
                updateXMLs(sender, sender, receiver, message);
                updateXMLs(receiver, sender, receiver, message);
                reply = "SEND_MESSAGE";
                break;
            case "SEND_GROUP_MESSAGE":
                String groupSender = content[0];
                String targetGroup = content[1];
                String groupMessage = content[2];
                handleGroupMessageForAllMembers(groupSender, targetGroup, groupMessage);
                reply = "SEND_GROUP_MESSAGE";
                break;
            case "SEND_COMMUNITY_MESSAGE":
                String communitySender = content[0];
                String communityMsg = content[1];
                addCommunityMessage(communitySender, communityMsg);
                reply = "SEND_COMMUNITY_MESSAGE";
                break;
            case "GET_CHAT_HISTORY":
                String requestingUser = content[0];
                try {
                    File userFile = new File("src/main/resources/XMLFiles/" + requestingUser + "Account.xml");
                    if (userFile.exists()) {
                        reply = "GET_CHAT_HISTORY";
                    } else {
                        reply = "FILE_NOT_FOUND";
                    }
                } catch (Exception e) {
                    reply = e.getStackTrace().toString();
                    e.printStackTrace();
                }
                break;

            case "SEND_FILE":
                String fileSender = content[0];
                String fileReceiver = content[1];

                System.out.println("Processing SEND_FILE request:");
                System.out.println("Sender: " + fileSender);
                System.out.println("Receiver: " + fileReceiver);
                System.out.println("Content array length: " + content.length);
                try {
                    if (content.length >= 6 && content[2].equals("FILE_TRANSFER")) {
                        String fileName = content[3];
                        String fileExtension = content[4];
                        String fileContent = content[5];
                        if (fileName.isEmpty() || fileContent.isEmpty()) {
                            System.err.println("Invalid file data: missing filename or content");
                            reply = "SEND_FILE_FAILED";
                            break;
                        }
                        String fileMessage = "[FILE: " + fileName + "]";

                        System.out.println("Processing file: " + fileName + " (" + fileExtension + ")");
                        System.out.println("Content length: " + fileContent.length() + " characters");

                        boolean senderFileStored = storeFileData(fileSender, fileSender, fileName, fileExtension, fileContent);
                        boolean receiverFileStored = storeFileData(fileReceiver, fileSender, fileName, fileExtension, fileContent);

                        if (senderFileStored && receiverFileStored) {
                            System.out.println("Adding file message to sender: " + fileSender);
                            updateXMLs(fileSender, fileSender, fileReceiver, fileMessage);

                            System.out.println("Adding file message to receiver: " + fileReceiver);
                            updateXMLs(fileReceiver, fileSender, fileReceiver, fileMessage);

                            System.out.println("File transfer processed successfully: " + fileName);
                            reply = "SEND_FILE_SUCCESS";
                        } else {
                            System.err.println("Failed to store file data for sender or receiver");
                            System.err.println("Sender file stored: " + senderFileStored);
                            System.err.println("Receiver file stored: " + receiverFileStored);
                            reply = "SEND_FILE_FAILED";
                        }
                    } else {
                        System.err.println("Invalid file data format. Expected FILE_TRANSFER at index 2, got: " +
                                (content.length > 2 ? content[2] : "insufficient content"));
                        reply = "SEND_FILE_FAILED";
                    }
                } catch (Exception e) {
                    System.err.println("Error processing file transfer: " + e.getMessage());
                    e.printStackTrace();
                    reply = "SEND_FILE_FAILED";
                }
                break;
            case "SEND_GROUP_FILE":
                String groupFileSender = content[0];
                String targetGroupForFile = content[1];

                System.out.println("Processing SEND_GROUP_FILE request:");
                System.out.println("Sender: " + groupFileSender);
                System.out.println("Target Group: " + targetGroupForFile);

                try {
                    if (content.length >= 6 && content[2].equals("FILE_TRANSFER")) {
                        String groupFileName = content[3];
                        String groupFileExtension = content[4];
                        String groupFileContent = content[5];

                        if (groupFileName.isEmpty() || groupFileContent.isEmpty()) {
                            System.err.println("Invalid group file data: missing filename or content");
                            reply = "SEND_GROUP_FILE_FAILED";
                            break;
                        }

                        String fileMessage = "[FILE: " + groupFileName + "]";

                        List<String> groupMembers = getGroupMembers(targetGroupForFile);
                        if (!groupMembers.contains(groupFileSender)) {
                            System.err.println("Access denied: " + groupFileSender + " is not a member of group " + targetGroupForFile);
                            reply = "GROUP_ACCESS_DENIED";
                            break;
                        }

                        boolean allFilesStored = true;
                        for (String member : groupMembers) {
                            boolean memberFileStored = storeFileData(member, groupFileSender, groupFileName, groupFileExtension, groupFileContent);
                            if (!memberFileStored) {
                                System.err.println("Failed to store file for member: " + member);
                                allFilesStored = false;
                            }
                        }

                        if (allFilesStored) {
                            handleGroupMessageForAllMembers(groupFileSender, targetGroupForFile, fileMessage);
                            System.out.println("Group file transfer processed successfully: " + groupFileName);
                            reply = "SEND_GROUP_FILE_SUCCESS";
                        } else {
                            System.err.println("Failed to store file for some group members");
                            reply = "SEND_GROUP_FILE_FAILED";
                        }
                    } else {
                        System.err.println("Invalid group file data format. Expected FILE_TRANSFER at index 2, got: " +
                                (content.length > 2 ? content[2] : "insufficient content"));
                        reply = "SEND_GROUP_FILE_FAILED";
                    }
                } catch (Exception e) {
                    System.err.println("Error processing group file transfer: " + e.getMessage());
                    e.printStackTrace();
                    reply = "SEND_GROUP_FILE_FAILED";
                }
                break;
            case "GET_ALL_USERS_WITH_STATUS":
                reply = "GET_ALL_USERS_WITH_STATUS";
                break;

            case "GET_ACTIVE_USERS_ONLY":
                reply = "GET_ACTIVE_USERS_ONLY";
                break;

            case "GET_ALL_GROUPS_ENHANCED":
                reply = "GET_ALL_GROUPS_ENHANCED";
                break;

            case "CREATE_GROUP_WITH_MEMBERS":
                String groupName = content[0];
                String creator = content[1];
                membersStr = content.length > 2 ? content[2] : "";

                List<String> members = new ArrayList<>();
                if (!membersStr.isEmpty()) {
                    members = Arrays.asList(membersStr.split(","));
                    // Trim all member names
                    for (int i = 0; i < members.size(); i++) {
                        members.set(i, members.get(i).trim());
                    }
                }

                if (createGroupWithMembers(groupName, creator, members)) {
                    reply = "GROUP_CREATED";
                } else {
                    reply = "GROUP_CREATE_FAILED";
                }
                break;

            case "ADD_MEMBERS_TO_GROUP":
                String targetGroupName = content[0];
                String newMembersStr = content[1];

                if (!newMembersStr.isEmpty()) {
                    List<String> newMembers = Arrays.asList(newMembersStr.split(","));
                    // Here you could implement adding members to existing group
                    // For now, just return success
                    reply = "MEMBERS_ADDED_TO_GROUP";
                } else {
                    reply = "GROUP_UPDATE_FAILED";
                }
                break;

            case "REQUEST_PASSWORD_RESET_WITH_CODE":
                String resetUsernameWithCode = content[0];
                String providedVerificationCode = content[1];

                System.out.println("[DEBUG] Password reset request with verification code from: " + resetUsernameWithCode);


                if (verifyResetCode(resetUsernameWithCode, providedVerificationCode)) {
                    reply = "VERIFICATION_CODE_VALID";
                } else {
                    reply = "VERIFICATION_CODE_INVALID";
                }
                break;
            case "RESET_PASSWORD_CLIENT":
                String clientResetUsername = content[0];
                String clientNewPassword = content[1];
                if (changeUserPasswordDirectly(clientResetUsername, clientNewPassword)) {
                    reply = "PASSWORD_RESET_SUCCESS![]!" + clientResetUsername + "![]!" + clientNewPassword + "![]!Your password has been successfully reset.";
                } else {
                    System.out.println("[DEBUG] Failed to reset password for user: " + clientResetUsername);
                }
                break;

            case "ADMIN_VERIFY_AND_CHANGE_PASSWORD":
                String adminResetUsername = content[0];
                String adminVerificationCode = content[1];
                String adminNewPassword = content[2];
                if (verifyResetCode(adminResetUsername, adminVerificationCode)) {
                    if (changeUserPassword(adminResetUsername, adminNewPassword)) {
                        System.out.println("[DEBUG] Password changed by admin for user: " + adminResetUsername);
                        reply = "ADMIN_PASSWORD_RESET_SUCCESS![]!" + adminResetUsername + "![]!Password successfully changed";
                    } else {
                        System.out.println("[DEBUG] Failed to change password for user: " + adminResetUsername);
                        reply = "ADMIN_PASSWORD_RESET_FAILED![]!" + adminResetUsername + "![]!Failed to change password";
                    }
                } else {
                    System.out.println("[DEBUG] Invalid verification code for user: " + adminResetUsername);

                    // Notify client that verification code is invalid
                    notifyClientOfInvalidCode(adminResetUsername);

                    reply = "ADMIN_PASSWORD_RESET_FAILED![]!" + adminResetUsername + "![]!Invalid verification code";
                }
                break;
            case "REJECT_PASSWORD_RESET":
                String rejectedUser = content[0];
                System.out.println("[DEBUG] Admin rejected password reset for user: " + rejectedUser);

                // Notify client that their password reset was rejected
                notifyClientOfRejectedReset(rejectedUser);

                reply = "PASSWORD_RESET_REJECTED![]!" + rejectedUser + "![]!Password reset request rejected";
                break;
            case "ADMIN_REQUEST_PASSWORD_RESET_WITH_CODE":
                String adminResetUsernameWithCode = content[0];
                String adminProvidedVerificationCode = content[1];

                System.out.println("[DEBUG] Admin password reset request with verification code from: " + adminResetUsernameWithCode);

                if (verifyAdminResetCode(adminResetUsernameWithCode, adminProvidedVerificationCode)) {
                    reply = "ADMIN_VERIFICATION_CODE_VALID";
                    System.out.println("[DEBUG] Admin verification code valid for: " + adminResetUsernameWithCode);
                } else {
                    reply = "ADMIN_VERIFICATION_CODE_INVALID";
                    System.out.println("[DEBUG] Admin verification code invalid for: " + adminResetUsernameWithCode);
                }
                break;
            case "ADMIN_RESET_PASSWORD":
                String adminUsernameForReset = content[0];
                adminNewPassword = content[1];

                if (changeAdminPassword(adminUsernameForReset, adminNewPassword)) {
                    reply = "ADMIN_PASSWORD_RESET_SUCCESS![]!" + adminUsernameForReset + "![]!" + adminNewPassword + "![]!Your administrator password has been successfully reset.";
                } else {
                    System.out.println("[DEBUG] Failed to reset admin password for user: " + adminUsernameForReset);
                    reply = "ADMIN_PASSWORD_RESET_FAILED![]!" + adminUsernameForReset + "![]!Failed to reset administrator password";
                }
                break;

            case "VERIFY_RESET_CODE":
                String verifyUsername = content[0];
                String providedCode = content[1];

                if (verifyResetCode(verifyUsername, providedCode)) {
                    reply = "RESET_CODE_VERIFIED";
                } else {
                    reply = "RESET_CODE_INVALID";
                }
                break;

            case "CHANGE_USERNAME":
                String currentUsername = content[0];
                String requestedNewUsername = content[1];
                reply = changeUsername(currentUsername, requestedNewUsername);
                break;

            case "CHANGE_PASSWORD":
                String userForPasswordChange = content[0];
                String oldPasswordProvided = content[1];
                String newPasswordRequested = content[2];
                reply = changePassword(userForPasswordChange, oldPasswordProvided, newPasswordRequested);
                break;
            case "ADD_FRIEND_REQUEST":
                String fromUser = content[0];
                String toUser = content[1];
                if (addFriendRequestToServer(fromUser, toUser)) {
                    reply = "FRIEND_REQUEST_SENT";
                } else {
                    reply = "FRIEND_REQUEST_FAILED";
                }
                break;
            case "GET_FRIEND_REQUESTS":
                String requestUser = content[0];
                String requests = getFriendRequestsForUser(requestUser);
                reply = "FRIEND_REQUESTS![]!" + requests;
                break;
            case "ACCEPT_FRIEND_REQUEST":
                String acceptingUser = content[0];
                requestingUser = content[1];
                if (acceptFriendRequestOnServer(acceptingUser, requestingUser)) {
                    reply = "FRIEND_REQUEST_ACCEPTED";
                } else {
                    reply = "FRIEND_REQUEST_ACCEPT_FAILED";
                }
                break;
            case "DECLINE_FRIEND_REQUEST":
                String decliningUser = content[0];
                String requestingUserDecline = content[1];
                if (declineFriendRequestOnServer(decliningUser, requestingUserDecline)) {
                    reply = "FRIEND_REQUEST_DECLINED";
                } else {
                    reply = "FRIEND_REQUEST_DECLINE_FAILED";
                }
                break;
        }
        return reply;
    }
    private boolean addFriendRequestToServer(String fromUser, String toUser) {
        try {
            File file = new File("src/main/resources/XMLFiles/Requests.xml");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc;

            if (file.exists()) {
                doc = builder.parse(file);
            } else {
                file.getParentFile().mkdirs();
                doc = builder.newDocument();
                Element root = doc.createElement("requests");
                doc.appendChild(root);
            }

            // Check if request already exists
            NodeList requests = doc.getElementsByTagName("request");
            for (int i = 0; i < requests.getLength(); i++) {
                Element request = (Element) requests.item(i);
                String existingFrom = request.getElementsByTagName("from").item(0).getTextContent();
                String existingTo = request.getElementsByTagName("to").item(0).getTextContent();
                String status = request.getElementsByTagName("status").item(0).getTextContent();

                if (existingFrom.equals(fromUser) && existingTo.equals(toUser) && status.equals("pending")) {
                    return false; // Request already exists
                }
            }

            // Add new request
            Element root = doc.getDocumentElement();
            Element request = doc.createElement("request");

            Element from = doc.createElement("from");
            from.appendChild(doc.createTextNode(fromUser));
            request.appendChild(from);

            Element to = doc.createElement("to");
            to.appendChild(doc.createTextNode(toUser));
            request.appendChild(to);

            Element status = doc.createElement("status");
            status.appendChild(doc.createTextNode("pending"));
            request.appendChild(status);

            root.appendChild(request);

            saveDocument(doc, file);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private String getFriendRequestsForUser(String username) {
        try {
            File file = new File("src/main/resources/XMLFiles/Requests.xml");
            if (!file.exists()) return "";

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList requests = doc.getElementsByTagName("request");
            List<String> userRequests = new ArrayList<>();

            for (int i = 0; i < requests.getLength(); i++) {
                Element request = (Element) requests.item(i);
                String to = request.getElementsByTagName("to").item(0).getTextContent();
                String from = request.getElementsByTagName("from").item(0).getTextContent();
                String status = request.getElementsByTagName("status").item(0).getTextContent();

                if (to.equals(username) && status.equals("pending")) {
                    userRequests.add(from);
                }
            }

            return String.join(",", userRequests);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private boolean acceptFriendRequestOnServer(String acceptingUser, String requestingUser) {
        try {
            File file = new File("src/main/resources/XMLFiles/Requests.xml");
            if (!file.exists()) return false;

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList requests = doc.getElementsByTagName("request");
            for (int i = 0; i < requests.getLength(); i++) {
                Element request = (Element) requests.item(i);
                String from = request.getElementsByTagName("from").item(0).getTextContent();
                String to = request.getElementsByTagName("to").item(0).getTextContent();
                String status = request.getElementsByTagName("status").item(0).getTextContent();

                if (from.equals(requestingUser) && to.equals(acceptingUser) && status.equals("pending")) {
                    // Remove the request
                    request.getParentNode().removeChild(request);

                    // Add friendship to both users' account files
                    addFriendshipToUserAccounts(acceptingUser, requestingUser);

                    saveDocument(doc, file);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private boolean declineFriendRequestOnServer(String decliningUser, String requestingUser) {
        try {
            File file = new File("src/main/resources/XMLFiles/Requests.xml");
            if (!file.exists()) return false;

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList requests = doc.getElementsByTagName("request");
            for (int i = 0; i < requests.getLength(); i++) {
                Element request = (Element) requests.item(i);
                String from = request.getElementsByTagName("from").item(0).getTextContent();
                String to = request.getElementsByTagName("to").item(0).getTextContent();
                String status = request.getElementsByTagName("status").item(0).getTextContent();

                if (from.equals(requestingUser) && to.equals(decliningUser) && status.equals("pending")) {
                    request.getParentNode().removeChild(request);
                    saveDocument(doc, file);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private void addFriendshipToUserAccounts(String user1, String user2) {
        addFriendToUserAccount(user1, user2);
        addFriendToUserAccount(user2, user1);
    }
    private void addFriendToUserAccount(String username, String friendName) {
        try {
            File file = new File("src/main/resources/XMLFiles/" + username + "Account.xml");
            if (!file.exists()) return;

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            Element root = doc.getDocumentElement();

            // Check if friend already exists
            NodeList friends = root.getElementsByTagName("friend");
            for (int i = 0; i < friends.getLength(); i++) {
                Element friend = (Element) friends.item(i);
                String existingFriend = friend.getElementsByTagName("username").item(0).getTextContent();
                if (existingFriend.equals(friendName)) {
                    return; // Friend already exists
                }
            }

            // Add new friend
            Element friend = doc.createElement("friend");
            Element usernameElement = doc.createElement("username");
            usernameElement.appendChild(doc.createTextNode(friendName));
            friend.appendChild(usernameElement);

            Element chatHistory = doc.createElement("chathistory");
            friend.appendChild(chatHistory);

            root.appendChild(friend);
            saveDocument(doc, file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    //HELPER METHODS FOR SIGNING UP
    private void registerUser(String username, String password) throws Exception {
        File file = new File(FILE_PATH);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc;

        if (file.exists()) {
            doc = builder.parse(file);
        } else {
            doc = builder.newDocument();
            Element root = doc.createElement("users");
            doc.appendChild(root);
        }

        Element root = doc.getDocumentElement();
        Element user = doc.createElement("user");

        Element uname = doc.createElement("username");
        uname.appendChild(doc.createTextNode(username));
        user.appendChild(uname);

        Element pass = doc.createElement("password");
        pass.appendChild(doc.createTextNode(password));
        user.appendChild(pass);

        root.appendChild(user);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(doc), new StreamResult(file));
        createUserAccountXML(username);
    }

    //HELPER METHOD FOR ADMIN LOGIN
    public static boolean validateAdmin(String username, String password) {
        try {
            // Check AdminCredentials.xml
            File adminFile = new File(ADMIN_FILE_PATH);
            if (!adminFile.exists()) {
                createDefaultAdminFile();
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(adminFile);

            NodeList admins = doc.getElementsByTagName("admin");
            for (int i = 0; i < admins.getLength(); i++) {
                Element admin = (Element) admins.item(i);
                String adminUsername = admin.getElementsByTagName("username").item(0).getTextContent();
                String adminPassword = admin.getElementsByTagName("password").item(0).getTextContent();

                if (adminUsername.equals(username) && adminPassword.equals(password)) {
                    System.out.println("Admin validated from AdminCredentials.xml: " + username);
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            System.err.println("Error validating admin credentials: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    private static void createDefaultAdminFile() {
        try {
            File adminFile = new File(ADMIN_FILE_PATH);
            adminFile.getParentFile().mkdirs();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("admins");
            doc.appendChild(root);

            Element admin = doc.createElement("admin");

            Element username = doc.createElement("username");
            username.appendChild(doc.createTextNode("admin"));
            admin.appendChild(username);

            Element password = doc.createElement("password");
            password.appendChild(doc.createTextNode("admin123"));
            admin.appendChild(password);

            root.appendChild(admin);

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(doc), new StreamResult(adminFile));

            System.out.println("Created default admin credentials file: " + ADMIN_FILE_PATH);
        } catch (Exception e) {
            System.err.println("Error creating default admin file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public boolean verifyAdminResetCode(String username, String providedCode) {
        try {
            System.out.println("[DEBUG] Verifying admin reset code for user: " + username);
            System.out.println("[DEBUG] Provided admin code: " + providedCode);

            File file = new File(ADMIN_FILE_PATH);
            if (!file.exists()) {
                System.out.println("[DEBUG] AdminCredentials.xml file does not exist: " + file.getAbsolutePath());
                return false;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList admins = doc.getElementsByTagName("admin");
            for (int i = 0; i < admins.getLength(); i++) {
                Element admin = (Element) admins.item(i);
                String adminUsername = admin.getElementsByTagName("username").item(0).getTextContent();

                if (adminUsername.equals(username)) {
                    System.out.println("[DEBUG] Found admin user: " + username);

                    // Look for 'verification' element (permanent security code)
                    NodeList codeNodes = admin.getElementsByTagName("verification");
                    if (codeNodes.getLength() > 0) {
                        String storedCode = codeNodes.item(0).getTextContent().trim();
                        System.out.println("[DEBUG] Stored admin code: " + storedCode + ", Provided code: " + providedCode);

                        boolean codeMatches = storedCode.equals(providedCode);
                        System.out.println("[DEBUG] Admin code verification result: " + codeMatches);
                        return codeMatches;
                    } else {
                        System.out.println("[DEBUG] No verification code found for admin user: " + username);
                        return false;
                    }
                }
            }

            System.out.println("[DEBUG] Admin user not found: " + username);
            return false;
        } catch (Exception e) {
            System.err.println("[ERROR] Error verifying admin reset code: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    public boolean changeAdminPassword(String username, String newPassword) {
        try {
            File file = new File(ADMIN_FILE_PATH);
            if (!file.exists()) {
                System.out.println("[DEBUG] AdminCredentials.xml file does not exist");
                return false;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList admins = doc.getElementsByTagName("admin");
            for (int i = 0; i < admins.getLength(); i++) {
                Element admin = (Element) admins.item(i);
                String adminUsername = admin.getElementsByTagName("username").item(0).getTextContent();

                if (adminUsername.equals(username)) {
                    admin.getElementsByTagName("password").item(0).setTextContent(newPassword);

                    // Save the updated document
                    removeEmptyTextNodes(doc);
                    TransformerFactory tf = TransformerFactory.newInstance();
                    Transformer transformer = tf.newTransformer();
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
                    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                    transformer.transform(new DOMSource(doc), new StreamResult(file));

                    System.out.println("[DEBUG] Admin password changed successfully for user: " + username);
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Error changing admin password: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    //HELPER METHODS FOR PASSWORD CHANGE
    private void notifyClientOfInvalidCode(String username) {
        try {
            Class<?> clientThreadClass = Class.forName("Server.ClientThread");
            java.lang.reflect.Field clientsField = clientThreadClass.getDeclaredField("clients");
            clientsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.ArrayList<Object> clients = (java.util.ArrayList<Object>) clientsField.get(null);

            System.out.println("[DEBUG] Notifying client about invalid verification code: " + username);

            synchronized (clients) {
                for (Object client : clients) {
                    try {
                        java.lang.reflect.Field nameField = client.getClass().getDeclaredField("name");
                        nameField.setAccessible(true);
                        String clientName = (String) nameField.get(client);

                        if (username.equals(clientName)) {
                            java.lang.reflect.Method sendMessageMethod = client.getClass().getDeclaredMethod("sendMessage", String.class);
                            sendMessageMethod.setAccessible(true);

                            String notification = "<Response><reply>VERIFICATION_CODE_INVALID</reply><body>" +
                                    "Your verification code is invalid or does not match your account records.</body></Response>";

                            sendMessageMethod.invoke(client, notification);
                            System.out.println("[DEBUG] Successfully notified client about invalid verification code: " + username);
                            break;
                        }
                    } catch (Exception e) {
                        System.err.println("[ERROR] Error notifying client about invalid code: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Error accessing clients list for invalid code notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void notifyClientOfRejectedReset(String username) {
        try {
            Class<?> clientThreadClass = Class.forName("Server.ClientThread");
            java.lang.reflect.Field clientsField = clientThreadClass.getDeclaredField("clients");
            clientsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.ArrayList<Object> clients = (java.util.ArrayList<Object>) clientsField.get(null);

            System.out.println("[DEBUG] Notifying client about rejected password reset: " + username);

            synchronized (clients) {
                for (Object client : clients) {
                    try {
                        java.lang.reflect.Field nameField = client.getClass().getDeclaredField("name");
                        nameField.setAccessible(true);
                        String clientName = (String) nameField.get(client);

                        if (username.equals(clientName)) {
                            java.lang.reflect.Method sendMessageMethod = client.getClass().getDeclaredMethod("sendMessage", String.class);
                            sendMessageMethod.setAccessible(true);

                            String notification = "<Response><reply>PASSWORD_RESET_REJECTED</reply><body>" +
                                    "Your password reset request has been rejected by admin.</body></Response>";

                            sendMessageMethod.invoke(client, notification);
                            System.out.println("[DEBUG] Successfully notified client about rejected password reset: " + username);
                            break;
                        }
                    } catch (Exception e) {
                        System.err.println("[ERROR] Error notifying client about rejected reset: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Error accessing clients list for rejection notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //HELPER METHODS FOR SENDING MESSAGES
    public synchronized boolean createGroupInUserAccounts(String groupName, String creator, String membersStr) {
        try {
            List<String> allMembers = new ArrayList<>();
            allMembers.add(creator);
            if (!membersStr.isEmpty()) {
                String[] members = membersStr.split(",");
                for (String member : members) {
                    String trimmedMember = member.trim();
                    if (!trimmedMember.isEmpty() && !allMembers.contains(trimmedMember)) {
                        allMembers.add(trimmedMember);
                    }
                }
            }
            for (String member : allMembers) {
                List<String> otherMembers = new ArrayList<>(allMembers);
                otherMembers.remove(member);
                addGroupToUserAccount(member, groupName, otherMembers);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private void addGroupToUserAccount(String username, String groupName, List<String> otherMembers) {
        try {
            File file = new File("src/main/resources/XMLFiles/" + username + "Account.xml");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc;
            if (file.exists()) {
                doc = builder.parse(file);
            } else {
                doc = builder.newDocument();
                Element root = doc.createElement("user");
                doc.appendChild(root);
            }
            Element root = doc.getDocumentElement();
            Element groupElement = doc.createElement("group");
            Element groupNameElement = doc.createElement("groupname");
            groupNameElement.appendChild(doc.createTextNode(groupName));
            groupElement.appendChild(groupNameElement);
            for (String member : otherMembers) {
                Element memberElement = doc.createElement("username");
                memberElement.appendChild(doc.createTextNode(member));
                groupElement.appendChild(memberElement);
            }
            Element chatHistory = doc.createElement("chathistory");
            groupElement.appendChild(chatHistory);

            root.appendChild(groupElement);
            saveDocument(doc, file);

        } catch (Exception e) {
            System.err.println("Error adding group to " + username + "'s account: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private boolean storeFileData(String username, String sender, String fileName, String fileExtension, String base64Content) {
        try {
            if (username == null || username.trim().isEmpty()) {
                System.err.println("Invalid username for file storage");
                return false;
            }
            if (fileName == null || fileName.trim().isEmpty()) {
                System.err.println("Invalid filename for file storage");
                return false;
            }
            if (base64Content == null || base64Content.trim().isEmpty()) {
                System.err.println("Invalid file content for file storage");
                return false;
            }
            try {
                byte[] testDecode = java.util.Base64.getDecoder().decode(base64Content);
                if (testDecode.length == 0) {
                    System.err.println("Empty file content after decoding");
                    return false;
                }
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid base64 content: " + e.getMessage());
                return false;
            }
            File userDir = new File("src/main/resources/XMLFiles/userFiles/" + username.trim());
            if (!userDir.exists()) {
                boolean created = userDir.mkdirs();
                if (!created) {
                    System.err.println("Failed to create directory: " + userDir.getAbsolutePath());
                    userDir = new File("XMLFiles/userFiles/" + username.trim());
                    created = userDir.mkdirs();
                    if (!created) {
                        System.err.println("Failed to create alternative directory: " + userDir.getAbsolutePath());
                        return false;
                    }
                }
            }
            String timestamp = String.valueOf(System.currentTimeMillis());
            String uniqueFileName = timestamp + "_" + fileName.trim();
            File fileDataFile = new File(userDir, uniqueFileName);
            try (PrintWriter writer = new PrintWriter(fileDataFile, "UTF-8")) {
                writer.println("SENDER:" + sender);
                writer.println("ORIGINAL_NAME:" + fileName.trim());
                writer.println("EXTENSION:" + (fileExtension != null ? fileExtension : ""));
                writer.println("TIMESTAMP:" + timestamp);
                writer.println("SIZE:" + base64Content.length());
                writer.println("CONTENT:" + base64Content);
                writer.flush();
            }
            if (fileDataFile.exists() && fileDataFile.length() > 0) {
                System.out.println("Successfully stored file data for user " + username + ": " + uniqueFileName);
                System.out.println("File path: " + fileDataFile.getAbsolutePath());
                System.out.println("File size: " + fileDataFile.length() + " bytes");
                return true;
            } else {
                System.err.println("File was not created properly: " + fileDataFile.getAbsolutePath());
                return false;
            }

        } catch (Exception e) {
            System.err.println("Error storing file data for user " + username + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    public synchronized boolean joinGroupWithHistory(String groupName, String username) {
        try {
            boolean joinSuccess = joinGroup(groupName, username);
            if (!joinSuccess) {
                return false;
            }
            copyGroupChatHistoryToNewMember(groupName, username);
            return true;
        } catch (Exception e) {
            System.err.println("Error joining group with history: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    //HELPER METHODS FOR ADMIN FUNCTIONS
    public String getAllUsersData() {
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) return "";

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList users = doc.getElementsByTagName("user");
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < users.getLength(); i++) {
                Element user = (Element) users.item(i);
                String username = user.getElementsByTagName("username").item(0).getTextContent();
                String password = user.getElementsByTagName("password").item(0).getTextContent();

                if (i > 0) result.append(",");
                result.append(username).append("![]!").append(password);
            }

            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    public String getAllGroupsData() {
        try {
            Set<String> uniqueGroups = new HashSet<>();
            File xmlFilesDir = new File("src/main/resources/XMLFiles/");
            File[] userFiles = xmlFilesDir.listFiles((dir, name) -> name.endsWith("Account.xml"));

            if (userFiles == null) return "";

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            for (File userFile : userFiles) {
                try {
                    Document doc = builder.parse(userFile);
                    NodeList groups = doc.getElementsByTagName("group");

                    for (int i = 0; i < groups.getLength(); i++) {
                        Element group = (Element) groups.item(i);
                        NodeList groupNameNodes = group.getElementsByTagName("groupname");
                        if (groupNameNodes.getLength() > 0) {
                            String groupName = groupNameNodes.item(0).getTextContent();
                            String username = userFile.getName().replace("Account.xml", "");
                            uniqueGroups.add(groupName + "![]!" + username);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error reading " + userFile.getName() + ": " + e.getMessage());
                }
            }

            return String.join(",", uniqueGroups);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }public String getUserStats(String username) {
        try {
            File accountFile = new File("src/main/resources/XMLFiles/" + username + "Account.xml");
            if (!accountFile.exists()) {
                return "User account not found";
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(accountFile);

            int friendCount = doc.getElementsByTagName("friend").getLength();
            int groupCount = doc.getElementsByTagName("group").getLength();

            int messagesSent = 0;
            NodeList chatHistories = doc.getElementsByTagName("chathistory");
            for (int i = 0; i < chatHistories.getLength(); i++) {
                Element chatHistory = (Element) chatHistories.item(i);
                NodeList chats = chatHistory.getElementsByTagName("chat");
                for (int j = 0; j < chats.getLength(); j++) {
                    Element chat = (Element) chats.item(j);
                    String sender = chat.getElementsByTagName("sender").item(0).getTextContent();
                    if (sender.equals(username)) {
                        messagesSent++;
                    }
                }
            }

            return "Friends: " + friendCount + ", Groups: " + groupCount + ", Messages Sent: " + messagesSent;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error retrieving stats";
        }
    }
    public String searchUsersData(String keyword) {
        try {
            String allUsers = getAllUsersData();
            if (allUsers.isEmpty()) return "";

            String[] users = allUsers.split(",");
            StringBuilder result = new StringBuilder();
            boolean first = true;

            for (String user : users) {
                String[] parts = user.split("!\\[]!");
                if (parts.length >= 2 && parts[0].toLowerCase().contains(keyword.toLowerCase())) {
                    if (!first) result.append(",");
                    result.append(user);
                    first = false;
                }
            }

            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    public String searchGroupsData(String keyword) {
        try {
            String allGroups = getAllGroupsData();
            if (allGroups.isEmpty()) return "";

            String[] groups = allGroups.split(",");
            StringBuilder result = new StringBuilder();
            boolean first = true;

            for (String group : groups) {
                String[] parts = group.split("!\\[]!");
                if (parts.length >= 2 && parts[0].toLowerCase().contains(keyword.toLowerCase())) {
                    if (!first) result.append(",");
                    result.append(group);
                    first = false;
                }
            }

            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    public boolean changeUserPasswordDirectly(String username, String newPassword) {
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) return false;

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList users = doc.getElementsByTagName("user");
            for (int i = 0; i < users.getLength(); i++) {
                Element user = (Element) users.item(i);
                String uname = user.getElementsByTagName("username").item(0).getTextContent();

                if (uname.equals(username)) {
                    user.getElementsByTagName("password").item(0).setTextContent(newPassword);

                    // Save the updated document
                    removeEmptyTextNodes(doc);
                    TransformerFactory tf = TransformerFactory.newInstance();
                    Transformer transformer = tf.newTransformer();
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
                    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                    transformer.transform(new DOMSource(doc), new StreamResult(file));

                    System.out.println("[DEBUG] Password changed directly for user: " + username);
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Error changing password directly: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    public boolean changeUserPassword(String username, String newPassword) {
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) return false;

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList users = doc.getElementsByTagName("user");
            for (int i = 0; i < users.getLength(); i++) {
                Element user = (Element) users.item(i);
                String uname = user.getElementsByTagName("username").item(0).getTextContent();

                if (uname.equals(username)) {
                    user.getElementsByTagName("password").item(0).setTextContent(newPassword);
                    TransformerFactory tf = TransformerFactory.newInstance();
                    Transformer transformer = tf.newTransformer();
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.transform(new DOMSource(doc), new StreamResult(file));
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean deleteUserFromXML(String username) {
        try {
            File xmlFile = new File(FILE_PATH);
            if (!xmlFile.exists()) return false;

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList userList = doc.getElementsByTagName("user");
            for (int i = 0; i < userList.getLength(); i++) {
                Element userElement = (Element) userList.item(i);
                String uname = userElement.getElementsByTagName("username").item(0).getTextContent();
                if (uname.equals(username)) {
                    userElement.getParentNode().removeChild(userElement);

                    TransformerFactory tf = TransformerFactory.newInstance();
                    Transformer transformer = tf.newTransformer();
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.transform(new DOMSource(doc), new StreamResult(xmlFile));

                    // Also delete user's account file
                    File userAccountFile = new File("src/main/resources/XMLFiles/" + username + "Account.xml");
                    if (userAccountFile.exists()) {
                        userAccountFile.delete();
                    }

                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public static boolean deleteGroupFromXML(String groupName, String owner) {
        try {
            File xmlFile = new File(FILE_PATH);
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList groups = doc.getElementsByTagName("groups");
            if (groups.getLength() > 0) {
                Element groupsElement = (Element) groups.item(0);
                NodeList groupList = groupsElement.getElementsByTagName("group");

                for (int i = 0; i < groupList.getLength(); i++) {
                    Element group = (Element) groupList.item(i);
                    String currentGroupName = group.getAttribute("name");
                    String currentOwner = group.getAttribute("creator");

                    if (currentGroupName.equals(groupName) && currentOwner.equals(owner)) {
                        group.getParentNode().removeChild(group);

                        TransformerFactory tf = TransformerFactory.newInstance();
                        Transformer transformer = tf.newTransformer();
                        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                        transformer.transform(new DOMSource(doc), new StreamResult(xmlFile));

                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public static void addActiveUser(String username) {
        synchronized (activeUsers) {
            if (!activeUsers.contains(username)) {
                activeUsers.add(username);
            }
        }
    }

    public static void removeActiveUser(String username) {
        synchronized (activeUsers) {
            activeUsers.remove(username);
        }
    }

    public static List<String> getActiveUsers() {
        synchronized (activeUsers) {
            return new ArrayList<>(activeUsers);
        }
    }
    public static void addCommunityMessage(String sender, String message) {
        synchronized (communityMessages) {
            String formattedMessage = sender + ": " + message + " [" +
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "]";
            communityMessages.add(formattedMessage);
            if (communityMessages.size() > 100) {
                communityMessages.removeFirst();
            }
        }
    }
    public static List<String> getCommunityMessages() {
        synchronized (communityMessages) {
            return new ArrayList<>(communityMessages);
        }
    }
    private void createUserAccountXML(String username) throws Exception {
        File accountFile = new File("src/main/resources/XMLFiles/" + username + "Account.xml");
        if (accountFile.exists()) {
            return;
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        Element root = doc.createElement("user");
        doc.appendChild(root);
        saveDocument(doc, accountFile);
        System.out.println("Created account XML file for user: " + username);
    }
    public synchronized boolean createGroup(String groupName, String creator) {
        try {
            Document doc = loadDocument();
            Element root = doc.getDocumentElement();

            Element groups = (Element) root.getElementsByTagName("groups").item(0);
            if (groups == null) {
                groups = doc.createElement("groups");
                root.appendChild(groups);
            }

            Element group = doc.createElement("group");
            group.setAttribute("name", groupName);
            group.setAttribute("creator", creator);
            groups.appendChild(group);

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(doc), new StreamResult(new File(FILE_PATH)));

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void copyGroupChatHistoryToNewMember(String groupName, String newMemberUsername) {
        try {
            List<String> existingMembers = getGroupMembers(groupName);
            existingMembers.remove(newMemberUsername);

            if (existingMembers.isEmpty()) {
                System.out.println("No existing members to copy history from for group: " + groupName);
                return;
            }

            String sourceMember = existingMembers.get(0);
            List<String> groupHistory = getGroupChatHistoryFromMember(sourceMember, groupName);

            if (groupHistory.isEmpty()) {
                System.out.println("No existing chat history found for group: " + groupName);
                return;
            }

            createGroupWithHistoryForUser(newMemberUsername, groupName, groupHistory, existingMembers);
            System.out.println("Copied " + groupHistory.size() + " messages to new member: " + newMemberUsername);

        } catch (Exception e) {
            System.err.println("Error copying group chat history: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<String> getGroupChatHistoryFromMember(String memberUsername, String groupName) {
        List<String> messages = new ArrayList<>();
        try {
            File file = new File("src/main/resources/XMLFiles/" + memberUsername + "Account.xml");
            if (!file.exists()) {
                return messages;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            NodeList groupNodes = doc.getElementsByTagName("group");
            for (int i = 0; i < groupNodes.getLength(); i++) {
                Element groupElement = (Element) groupNodes.item(i);
                NodeList groupNameNodes = groupElement.getElementsByTagName("groupname");

                if (groupNameNodes.getLength() > 0) {
                    String gName = groupNameNodes.item(0).getTextContent();
                    if (gName.equals(groupName)) {
                        NodeList chatNodes = groupElement.getElementsByTagName("chat");
                        for (int j = 0; j < chatNodes.getLength(); j++) {
                            Element chatElement = (Element) chatNodes.item(j);
                            String sender = chatElement.getElementsByTagName("sender").item(0).getTextContent();
                            String msg = chatElement.getElementsByTagName("msg").item(0).getTextContent();
                            messages.add(sender + "![]!" + msg);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading group history from member " + memberUsername + ": " + e.getMessage());
            e.printStackTrace();
        }
        return messages;
    }

    private void createGroupWithHistoryForUser(String username, String groupName, List<String> history, List<String> otherMembers) {
        try {
            File file = new File("src/main/resources/XMLFiles/" + username + "Account.xml");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc;

            if (file.exists()) {
                doc = builder.parse(file);
            } else {
                doc = builder.newDocument();
                Element root = doc.createElement("user");
                doc.appendChild(root);
            }

            Element root = doc.getDocumentElement();
            Element targetGroup = doc.createElement("group");

            Element groupNameElement = doc.createElement("groupname");
            groupNameElement.appendChild(doc.createTextNode(groupName));
            targetGroup.appendChild(groupNameElement);

            for (String member : otherMembers) {
                Element memberElement = doc.createElement("username");
                memberElement.appendChild(doc.createTextNode(member));
                targetGroup.appendChild(memberElement);
            }

            Element chatHistory = doc.createElement("chathistory");

            for (String chatMessage : history) {
                String[] parts = chatMessage.split("!\\[]!", 2);
                if (parts.length == 2) {
                    Element chat = doc.createElement("chat");

                    Element sender = doc.createElement("sender");
                    sender.appendChild(doc.createTextNode(parts[0]));
                    chat.appendChild(sender);

                    Element msg = doc.createElement("msg");
                    msg.appendChild(doc.createTextNode(parts[1]));
                    chat.appendChild(msg);

                    chatHistory.appendChild(chat);
                }
            }

            targetGroup.appendChild(chatHistory);
            root.appendChild(targetGroup);

            saveDocument(doc, file);
            System.out.println("Created group with history for user: " + username + " in group: " + groupName);

        } catch (Exception e) {
            System.err.println("Error creating group with history for user " + username + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void fixExistingGroupMemberships() {
        try {
            System.out.println("Starting to fix existing group memberships...");
            Document doc = loadDocument();
            NodeList groupList = doc.getElementsByTagName("group");

            for (int i = 0; i < groupList.getLength(); i++) {
                Element group = (Element) groupList.item(i);
                String groupName = group.getAttribute("name");
                String creator = group.getAttribute("creator");

                System.out.println("Processing group: " + groupName);
                if (creator != null && !creator.isEmpty()) {
                    ensureUserHasGroupInAccount(creator, groupName);
                }
                NodeList members = group.getElementsByTagName("member");
                for (int j = 0; j < members.getLength(); j++) {
                    Element member = (Element) members.item(j);
                    String memberName = member.getAttribute("username");
                    ensureUserHasGroupInAccount(memberName, groupName);
                }
            }

            System.out.println("Finished fixing existing group memberships.");

        } catch (Exception e) {
            System.err.println("Error fixing existing group memberships: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void ensureUserHasGroupInAccount(String username, String groupName) {
        try {
            File file = new File("src/main/resources/XMLFiles/" + username + "Account.xml");
            if (!file.exists()) {
                System.out.println("Account file not found for user: " + username);
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            Element root = doc.getDocumentElement();
            NodeList existingGroups = root.getElementsByTagName("group");
            for (int i = 0; i < existingGroups.getLength(); i++) {
                Element existingGroup = (Element) existingGroups.item(i);
                NodeList groupNameNodes = existingGroup.getElementsByTagName("groupname");
                if (groupNameNodes.getLength() > 0) {
                    String existingGroupName = groupNameNodes.item(0).getTextContent();
                    if (existingGroupName.equals(groupName)) {
                        System.out.println("User " + username + " already has group " + groupName + " in account");
                        return;
                    }
                }
            }

            System.out.println("Adding missing group " + groupName + " to user " + username + "'s account");
            List<String> allMembers = getGroupMembers(groupName);
            allMembers.remove(username);
            List<String> chatHistory = new ArrayList<>();
            if (!allMembers.isEmpty()) {
                chatHistory = getGroupChatHistoryFromMember(allMembers.get(0), groupName);
            }
            Element groupElement = doc.createElement("group");
            Element groupNameElement = doc.createElement("groupname");
            groupNameElement.appendChild(doc.createTextNode(groupName));
            groupElement.appendChild(groupNameElement);
            for (String member : allMembers) {
                Element memberElement = doc.createElement("username");
                memberElement.appendChild(doc.createTextNode(member));
                groupElement.appendChild(memberElement);
            }
            Element chatHistoryElement = doc.createElement("chathistory");
            for (String message : chatHistory) {
                String[] parts = message.split("!\\[]!", 2);
                if (parts.length == 2) {
                    Element chat = doc.createElement("chat");

                    Element sender = doc.createElement("sender");
                    sender.appendChild(doc.createTextNode(parts[0]));
                    chat.appendChild(sender);

                    Element msg = doc.createElement("msg");
                    msg.appendChild(doc.createTextNode(parts[1]));
                    chat.appendChild(msg);

                    chatHistoryElement.appendChild(chat);
                }
            }

            groupElement.appendChild(chatHistoryElement);
            root.appendChild(groupElement);

            saveDocument(doc, file);
            System.out.println("Successfully added group " + groupName + " to " + username + "'s account with " + chatHistory.size() + " messages");

        } catch (Exception e) {
            System.err.println("Error ensuring user " + username + " has group " + groupName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleGroupMessageForAllMembers(String sender, String groupName, String message) {
        try {
            List<String> groupMembers = getGroupMembers(groupName);
            System.out.println("Handling group message for group: " + groupName + " with " + groupMembers.size() + " members");

            for (String member : groupMembers) {
                System.out.println("Updating XML for member: " + member);
                updateGroupXMLWithHistory(member, sender, groupName, message);
            }

            System.out.println("Group message sent from " + sender + " to group " + groupName + ": " + message);

        } catch (Exception e) {
            System.err.println("Error handling group message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<String> getGroupMembers(String groupName) {
        List<String> members = new ArrayList<>();
        try {
            Document doc = loadDocument();
            NodeList groupList = doc.getElementsByTagName("group");

            for (int i = 0; i < groupList.getLength(); i++) {
                Element group = (Element) groupList.item(i);
                if (group.getAttribute("name").equals(groupName)) {
                    String creator = group.getAttribute("creator");
                    if (creator != null && !creator.isEmpty()) {
                        members.add(creator);
                        System.out.println("Added creator to group members: " + creator);
                    }

                    NodeList memberList = group.getElementsByTagName("member");
                    for (int j = 0; j < memberList.getLength(); j++) {
                        Element member = (Element) memberList.item(j);
                        String memberName = member.getAttribute("username");
                        if (!members.contains(memberName)) {
                            members.add(memberName);
                            System.out.println("Added member to group: " + memberName);
                        }
                    }
                    break;
                }
            }

            if (members.isEmpty()) {
                members = findGroupMembersFromAccountXMLs(groupName);
            }

            System.out.println("Total members found for group " + groupName + ": " + members.size());
        } catch (Exception e) {
            System.err.println("Error getting group members: " + e.getMessage());
            e.printStackTrace();
        }
        return members;
    }

    private List<String> findGroupMembersFromAccountXMLs(String groupName) {
        List<String> members = new ArrayList<>();

        try {
            Document doc = loadDocument();
            NodeList users = doc.getElementsByTagName("user");

            for (int i = 0; i < users.getLength(); i++) {
                Element user = (Element) users.item(i);
                String username = user.getElementsByTagName("username").item(0).getTextContent();

                if (hasGroupInAccountXML(username, groupName)) {
                    members.add(username);
                }
            }
        } catch (Exception e) {
            System.err.println("Error in fallback group member search: " + e.getMessage());
        }

        return members;
    }

    private boolean hasGroupInAccountXML(String username, String groupName) {
        try {
            File accountFile = new File("src/main/resources/XMLFiles/" + username + "Account.xml");
            if (!accountFile.exists()) {
                return false;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(accountFile);

            NodeList groups = doc.getElementsByTagName("group");
            for (int i = 0; i < groups.getLength(); i++) {
                Element group = (Element) groups.item(i);
                NodeList groupNameNodes = group.getElementsByTagName("groupname");
                if (groupNameNodes.getLength() > 0) {
                    String gName = groupNameNodes.item(0).getTextContent();
                    if (gName.equals(groupName)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking group in account XML for " + username + ": " + e.getMessage());
        }

        return false;
    }

    public void updateGroupXMLWithHistory(String owner, String sender, String groupName, String msg) throws IOException, SAXException, ParserConfigurationException, TransformerException {
        File file = new File("src/main/resources/XMLFiles/" + owner + "Account.xml");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc;

        if (file.exists()) {
            doc = builder.parse(file);
        } else {
            System.out.println("Creating new account XML for user: " + owner);
            doc = builder.newDocument();
            Element root = doc.createElement("user");
            doc.appendChild(root);
        }

        Element root = doc.getDocumentElement();

        NodeList groups = root.getElementsByTagName("group");
        Element targetGroup = null;
        for (int i = 0; i < groups.getLength(); i++) {
            Element g = (Element) groups.item(i);
            NodeList groupNameNodes = g.getElementsByTagName("groupname");
            if (groupNameNodes.getLength() > 0) {
                String gName = groupNameNodes.item(0).getTextContent();
                if (gName.equals(groupName)) {
                    targetGroup = g;
                    break;
                }
            }
        }

        if (targetGroup == null) {
            targetGroup = doc.createElement("group");

            Element groupNameElement = doc.createElement("groupname");
            groupNameElement.appendChild(doc.createTextNode(groupName));
            targetGroup.appendChild(groupNameElement);

            List<String> members = getGroupMembers(groupName);
            for (String member : members) {
                if (!member.equals(owner)) {
                    Element memberElement = doc.createElement("username");
                    memberElement.appendChild(doc.createTextNode(member));
                    targetGroup.appendChild(memberElement);
                }
            }

            root.appendChild(targetGroup);
        }

        Element chatHistory = null;
        NodeList chList = targetGroup.getElementsByTagName("chathistory");
        if (chList.getLength() > 0) {
            chatHistory = (Element) chList.item(0);
        } else {
            chatHistory = doc.createElement("chathistory");
            targetGroup.appendChild(chatHistory);
        }

        Element chat = doc.createElement("chat");

        Element s = doc.createElement("sender");
        s.appendChild(doc.createTextNode(sender));
        chat.appendChild(s);

        Element m = doc.createElement("msg");
        m.appendChild(doc.createTextNode(msg));
        chat.appendChild(m);

        chatHistory.appendChild(chat);

        saveDocument(doc, file);

        System.out.println("Updated group XML for user: " + owner + " in group: " + groupName);
    }

    private Document loadDocument() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new File(FILE_PATH));
    }

    private void saveDocument(Document doc, File file) throws TransformerException {
        removeEmptyTextNodes(doc);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);
    }
    private void removeEmptyTextNodes(Document doc) {
        NodeList list = doc.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            removeEmptyTextNodesRecursive(list.item(i));
        }
    }
    private void removeEmptyTextNodesRecursive(Node node) {
        NodeList list = node.getChildNodes();
        List<Node> nodesToRemove = new ArrayList<>();

        for (int i = 0; i < list.getLength(); i++) {
            Node child = list.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                if (child.getTextContent().trim().isEmpty()) {
                    nodesToRemove.add(child);
                }
            } else {
                removeEmptyTextNodesRecursive(child);
            }
        }

        for (Node nodeToRemove : nodesToRemove) {
            node.removeChild(nodeToRemove);
        }
    }

    public void updateXMLs(String owner, String sender, String receiver, String msg) throws IOException, SAXException, ParserConfigurationException, TransformerException {
        File file = new File("src/main/resources/XMLFiles/" + owner + "Account.xml");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc;

        if (file.exists()) {
            doc = builder.parse(file);
        } else {
            doc = builder.newDocument();
            Element root = doc.createElement("user");
            doc.appendChild(root);
        }

        Element root = doc.getDocumentElement();

        String otherPerson = owner.equals(sender) ? receiver : sender;

        NodeList friends = root.getElementsByTagName("friend");
        Element targetFriend = null;
        for (int i = 0; i < friends.getLength(); i++) {
            Element f = (Element) friends.item(i);
            String friendName = f.getElementsByTagName("username").item(0).getTextContent();
            if (friendName.equals(otherPerson)) {
                targetFriend = f;
                break;
            }
        }

        if (targetFriend == null) {
            targetFriend = doc.createElement("friend");
            Element username = doc.createElement("username");
            username.appendChild(doc.createTextNode(otherPerson));
            targetFriend.appendChild(username);
            root.appendChild(targetFriend);
        }

        Element chatHistory = null;
        NodeList chList = targetFriend.getElementsByTagName("chathistory");
        if (chList.getLength() > 0) {
            chatHistory = (Element) chList.item(0);
        } else {
            chatHistory = doc.createElement("chathistory");
            targetFriend.appendChild(chatHistory);
        }

        Element chat = doc.createElement("chat");

        Element s = doc.createElement("sender");
        s.appendChild(doc.createTextNode(sender));
        chat.appendChild(s);

        Element m = doc.createElement("msg");
        m.appendChild(doc.createTextNode(msg));
        chat.appendChild(m);

        chatHistory.appendChild(chat);

        saveDocument(doc, file);
    }
    public synchronized boolean addFriendRequest(String fromUser, String toUser) {
        try {
            Document doc = loadDocument();
            Element root = doc.getDocumentElement();

            Element requests = (Element) root.getElementsByTagName("friendRequests").item(0);
            if (requests == null) {
                requests = doc.createElement("friendRequests");
                root.appendChild(requests);
            }

            Element req = doc.createElement("request");
            req.setAttribute("from", fromUser);
            req.setAttribute("to", toUser);
            requests.appendChild(req);

            saveDocument(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public synchronized List<String> listFriendRequests(String username) {
        List<String> requests = new ArrayList<>();
        try {
            Document doc = loadDocument();
            NodeList requestList = doc.getElementsByTagName("request");

            for (int i = 0; i < requestList.getLength(); i++) {
                Element req = (Element) requestList.item(i);
                if (req.getAttribute("to").equals(username)) {
                    requests.add(req.getAttribute("from"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return requests;
    }

    public synchronized boolean acceptFriendRequest(String user, String friend) {
        try {
            Document doc = loadDocument();
            NodeList requestList = doc.getElementsByTagName("request");
            for (int i = 0; i < requestList.getLength(); i++) {
                Element req = (Element) requestList.item(i);
                if (req.getAttribute("from").equals(friend) && req.getAttribute("to").equals(user)) {
                    req.getParentNode().removeChild(req);
                    break;
                }
            }
            Element root = doc.getDocumentElement();
            Element friendsElement = (Element) root.getElementsByTagName("friends").item(0);
            if (friendsElement == null) {
                friendsElement = doc.createElement("friends");
                root.appendChild(friendsElement);
            }
            Element friendship = doc.createElement("friendship");
            friendship.setAttribute("user1", user);
            friendship.setAttribute("user2", friend);
            friendsElement.appendChild(friendship);

            saveDocument(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public synchronized boolean declineFriendRequest(String user, String friend) {
        try {
            Document doc = loadDocument();
            NodeList requestList = doc.getElementsByTagName("request");

            for (int i = 0; i < requestList.getLength(); i++) {
                Element req = (Element) requestList.item(i);
                if (req.getAttribute("from").equals(friend) && req.getAttribute("to").equals(user)) {
                    req.getParentNode().removeChild(req);
                    saveDocument(doc);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    private void saveDocument(Document doc) throws TransformerException {
        removeEmptyTextNodes(doc);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(FILE_PATH));
        transformer.transform(source, result);
    }
    public synchronized boolean joinGroup(String groupName, String username) {
        try {
            Document doc = loadDocument();
            Element root = doc.getDocumentElement();

            Element groups = (Element) root.getElementsByTagName("groups").item(0);
            if (groups == null) {
                return false;
            }

            NodeList groupList = groups.getElementsByTagName("group");
            boolean groupFound = false;

            for (int i = 0; i < groupList.getLength(); i++) {
                Element group = (Element) groupList.item(i);
                if (group.getAttribute("name").equals(groupName)) {
                    NodeList existingMembers = group.getElementsByTagName("member");
                    for (int j = 0; j < existingMembers.getLength(); j++) {
                        Element existingMember = (Element) existingMembers.item(j);
                        if (existingMember.getAttribute("username").equals(username)) {
                            System.out.println("User " + username + " is already a member of " + groupName);
                            return true;
                        }
                    }

                    Element member = doc.createElement("member");
                    member.setAttribute("username", username);
                    group.appendChild(member);
                    groupFound = true;
                    break;
                }
            }

            if (!groupFound) {
                return false;
            }

            saveDocument(doc);
            updateUserAccountWithGroup(username, groupName);

            System.out.println("User " + username + " successfully joined group " + groupName);
            return true;

        } catch (Exception e) {
            System.err.println("Error in joinGroup: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void updateUserAccountWithGroup(String username, String groupName) {
        try {
            File file = new File("src/main/resources/XMLFiles/" + username + "Account.xml");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc;

            if (file.exists()) {
                doc = builder.parse(file);
            } else {
                doc = builder.newDocument();
                Element root = doc.createElement("user");
                doc.appendChild(root);
            }

            Element root = doc.getDocumentElement();

            NodeList existingGroups = root.getElementsByTagName("group");
            for (int i = 0; i < existingGroups.getLength(); i++) {
                Element existingGroup = (Element) existingGroups.item(i);
                NodeList groupNameNodes = existingGroup.getElementsByTagName("groupname");
                if (groupNameNodes.getLength() > 0) {
                    String existingGroupName = groupNameNodes.item(0).getTextContent();
                    if (existingGroupName.equals(groupName)) {
                        System.out.println("Group " + groupName + " already exists in " + username + "'s account");
                        return;
                    }
                }
            }

            List<String> existingMembers = getGroupMembers(groupName);
            existingMembers.remove(username);

            Element groupElement = doc.createElement("group");

            Element groupNameElement = doc.createElement("groupname");
            groupNameElement.appendChild(doc.createTextNode(groupName));
            groupElement.appendChild(groupNameElement);

            for (String member : existingMembers) {
                Element memberElement = doc.createElement("username");
                memberElement.appendChild(doc.createTextNode(member));
                groupElement.appendChild(memberElement);
            }

            Element chatHistory = doc.createElement("chathistory");

            if (!existingMembers.isEmpty()) {
                List<String> groupHistory = getGroupChatHistoryFromMember(existingMembers.get(0), groupName);
                for (String chatMessage : groupHistory) {
                    String[] parts = chatMessage.split("!\\[]!", 2);
                    if (parts.length == 2) {
                        Element chat = doc.createElement("chat");

                        Element sender = doc.createElement("sender");
                        sender.appendChild(doc.createTextNode(parts[0]));
                        chat.appendChild(sender);

                        Element msg = doc.createElement("msg");
                        msg.appendChild(doc.createTextNode(parts[1]));
                        chat.appendChild(msg);

                        chatHistory.appendChild(chat);
                    }
                }
            }

            groupElement.appendChild(chatHistory);
            root.appendChild(groupElement);

            saveDocument(doc, file);
            System.out.println("Updated account XML for user: " + username + " with group: " + groupName);

        } catch (Exception e) {
            System.err.println("Error updating user account with group: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized List<String> listGroups() {
        List<String> groups = new ArrayList<>();
        try {
            Document doc = loadDocument();
            NodeList groupList = doc.getElementsByTagName("group");

            for (int i = 0; i < groupList.getLength(); i++) {
                Element group = (Element) groupList.item(i);
                groups.add(group.getAttribute("name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return groups;
    }

    public synchronized List<String> getUserGroups(String username) {
        List<String> userGroups = new ArrayList<>();
        try {
            Document doc = loadDocument();
            NodeList groupList = doc.getElementsByTagName("group");

            for (int i = 0; i < groupList.getLength(); i++) {
                Element group = (Element) groupList.item(i);

                if (group.getAttribute("creator").equals(username)) {
                    userGroups.add(group.getAttribute("name"));
                    continue;
                }

                NodeList members = group.getElementsByTagName("member");
                for (int j = 0; j < members.getLength(); j++) {
                    Element member = (Element) members.item(j);
                    if (member.getAttribute("username").equals(username)) {
                        userGroups.add(group.getAttribute("name"));
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userGroups;
    }

    public synchronized boolean isUserInGroup(String username, String groupName) {
        try {
            Document doc = loadDocument();
            NodeList groupList = doc.getElementsByTagName("group");

            for (int i = 0; i < groupList.getLength(); i++) {
                Element group = (Element) groupList.item(i);
                if (group.getAttribute("name").equals(groupName)) {

                    if (group.getAttribute("creator").equals(username)) {
                        return true;
                    }

                    NodeList members = group.getElementsByTagName("member");
                    for (int j = 0; j < members.getLength(); j++) {
                        Element member = (Element) members.item(j);
                        if (member.getAttribute("username").equals(username)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean userExists(String username) {
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) return false;

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            NodeList users = doc.getElementsByTagName("user");

            for (int i = 0; i < users.getLength(); i++) {
                Node userNode = users.item(i);
                if (userNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element userElement = (Element) userNode;
                    String uname = userElement.getElementsByTagName("username")
                            .item(0).getTextContent()
                            .trim();
                    if (uname.equals(username.trim())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean addUserToDocument(String username, String password, String verification) {
        try {
            File file = new File(FILE_PATH);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc;

            if (file.exists()) {
                doc = builder.parse(file);
            } else {
                doc = builder.newDocument();
                Element root = doc.createElement("users");
                doc.appendChild(root);
            }

            Element root = doc.getDocumentElement();

            if (userExists(username)) {
                return false;
            }

            Element userElement = doc.createElement("user");

            Element uname = doc.createElement("username");
            uname.appendChild(doc.createTextNode(username.trim()));
            userElement.appendChild(uname);

            Element pass = doc.createElement("password");
            pass.appendChild(doc.createTextNode(password.trim()));
            userElement.appendChild(pass);

            Element verify = doc.createElement("verification");
            verify.appendChild(doc.createTextNode(verification.trim()));
            userElement.appendChild(verify);

            root.appendChild(userElement);

            // FIXED: Use consistent transformer settings like other methods
            removeEmptyTextNodes(doc);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean deleteGroupFromXML(String filePath, String groupName, String owner) {
        try {
            File xmlFile = new File(filePath);
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList list = doc.getElementsByTagName("group");
            for (int i = 0; i < list.getLength(); i++) {
                Element elem = (Element) list.item(i);
                String currentGroupName = elem.getElementsByTagName("groupName").item(0).getTextContent();
                String currentOwner = elem.getElementsByTagName("owner").item(0).getTextContent();

                if (currentGroupName.equals(groupName) && currentOwner.equals(owner)) {
                    elem.getParentNode().removeChild(elem);

                    // Save back to file
                    javax.xml.transform.Transformer transformer =
                            javax.xml.transform.TransformerFactory.newInstance().newTransformer();
                    transformer.transform(
                            new javax.xml.transform.dom.DOMSource(doc),
                            new javax.xml.transform.stream.StreamResult(xmlFile)
                    );

                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public String getAllUsersDataWithStatus() {
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) return "";

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList users = doc.getElementsByTagName("user");
            StringBuilder result = new StringBuilder();
            List<String> activeUsersList = getActiveUsers();

            for (int i = 0; i < users.getLength(); i++) {
                Element user = (Element) users.item(i);
                String username = user.getElementsByTagName("username").item(0).getTextContent();
                String password = user.getElementsByTagName("password").item(0).getTextContent();
                boolean isOnline = activeUsersList.contains(username);

                if (i > 0) result.append(",");
                result.append(username).append("![]!").append(password).append("![]!").append(isOnline);
            }

            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    public String getActiveUsersData() {
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) return "";

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList users = doc.getElementsByTagName("user");
            StringBuilder result = new StringBuilder();
            List<String> activeUsersList = getActiveUsers();
            boolean first = true;

            for (int i = 0; i < users.getLength(); i++) {
                Element user = (Element) users.item(i);
                String username = user.getElementsByTagName("username").item(0).getTextContent();
                String password = user.getElementsByTagName("password").item(0).getTextContent();

                if (activeUsersList.contains(username)) {
                    if (!first) result.append(",");
                    result.append(username).append("![]!").append(password).append("![]!true");
                    first = false;
                }
            }

            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    public String getAllGroupsDataEnhanced() {
        try {
            Map<String, GroupInfo> uniqueGroups = new HashMap<>();
            File xmlFilesDir = new File("src/main/resources/XMLFiles/");
            File[] userFiles = xmlFilesDir.listFiles((dir, name) -> name.endsWith("Account.xml"));

            if (userFiles == null) return "";

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            for (File userFile : userFiles) {
                try {
                    Document doc = builder.parse(userFile);
                    NodeList groups = doc.getElementsByTagName("group");
                    String username = userFile.getName().replace("Account.xml", "");

                    for (int i = 0; i < groups.getLength(); i++) {
                        Element group = (Element) groups.item(i);
                        NodeList groupNameNodes = group.getElementsByTagName("groupname");
                        if (groupNameNodes.getLength() > 0) {
                            String groupName = groupNameNodes.item(0).getTextContent();

                            // Check if this user is the group owner
                            NodeList ownerNodes = group.getElementsByTagName("groupOwner");
                            if (ownerNodes.getLength() > 0) {
                                String owner = ownerNodes.item(0).getTextContent();

                                if (!uniqueGroups.containsKey(groupName)) {
                                    uniqueGroups.put(groupName, new GroupInfo(owner, 1));
                                } else {
                                    uniqueGroups.get(groupName).incrementMemberCount();
                                }
                            } else {
                                // If no explicit owner, try to determine from group structure
                                if (!uniqueGroups.containsKey(groupName)) {
                                    // Count members in this group
                                    NodeList members = group.getElementsByTagName("username");
                                    int memberCount = members.getLength() + 1; // +1 for the current user
                                    uniqueGroups.put(groupName, new GroupInfo(username, memberCount));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error reading " + userFile.getName() + ": " + e.getMessage());
                }
            }

            // Format result
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, GroupInfo> entry : uniqueGroups.entrySet()) {
                if (!first) result.append(",");
                result.append(entry.getKey()).append("![]!")
                        .append(entry.getValue().getOwner()).append("![]!")
                        .append(entry.getValue().getMemberCount());
                first = false;
            }

            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    public synchronized boolean createGroupWithMembers(String groupName, String creator, List<String> memberUsernames) {
        try {
            // Build complete member list
            List<String> allMembers = new ArrayList<>();
            allMembers.add(creator);

            if (memberUsernames != null) {
                for (String member : memberUsernames) {
                    String trimmedMember = member.trim();
                    if (!trimmedMember.isEmpty() && !allMembers.contains(trimmedMember)) {
                        allMembers.add(trimmedMember);
                    }
                }
            }

            // Create group in each member's account with group owner tag
            for (String member : allMembers) {
                List<String> otherMembers = new ArrayList<>(allMembers);
                otherMembers.remove(member);
                addGroupToUserAccountWithOwner(member, groupName, creator, otherMembers);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private void addGroupToUserAccountWithOwner(String username, String groupName, String groupOwner, List<String> otherMembers) {
        try {
            File file = new File("src/main/resources/XMLFiles/" + username + "Account.xml");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc;

            if (file.exists()) {
                doc = builder.parse(file);
            } else {
                doc = builder.newDocument();
                Element root = doc.createElement("user");
                doc.appendChild(root);
            }

            Element root = doc.getDocumentElement();
            Element groupElement = doc.createElement("group");

            // Add group name
            Element groupNameElement = doc.createElement("groupname");
            groupNameElement.appendChild(doc.createTextNode(groupName));
            groupElement.appendChild(groupNameElement);

            // Add group owner tag
            Element groupOwnerElement = doc.createElement("groupOwner");
            groupOwnerElement.appendChild(doc.createTextNode(groupOwner));
            groupElement.appendChild(groupOwnerElement);

            // Add other members
            for (String member : otherMembers) {
                Element memberElement = doc.createElement("username");
                memberElement.appendChild(doc.createTextNode(member));
                groupElement.appendChild(memberElement);
            }

            // Add chat history
            Element chatHistory = doc.createElement("chathistory");
            groupElement.appendChild(chatHistory);

            root.appendChild(groupElement);
            saveDocument(doc, file);

        } catch (Exception e) {
            System.err.println("Error adding group to " + username + "'s account: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public String generateVerificationCode(String username) {
        try {
            // Generate 6-digit verification code
            String verificationCode = String.format("%06d", new java.util.Random().nextInt(1000000));

            // Add verification code to user's account XML
            File file = new File("src/main/resources/XMLFiles/" + username + "Account.xml");
            if (!file.exists()) {
                return null; // User doesn't exist
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            Element root = doc.getDocumentElement();

            // Remove existing verification code if present
            NodeList existingCodes = root.getElementsByTagName("verificationCode");
            for (int i = existingCodes.getLength() - 1; i >= 0; i--) {
                root.removeChild(existingCodes.item(i));
            }

            // Add new verification code
            Element codeElement = doc.createElement("verificationCode");
            codeElement.appendChild(doc.createTextNode(verificationCode));
            root.appendChild(codeElement);

            // Add timestamp for expiration (valid for 10 minutes)
            Element timestampElement = doc.createElement("verificationTimestamp");
            timestampElement.appendChild(doc.createTextNode(String.valueOf(System.currentTimeMillis())));
            root.appendChild(timestampElement);

            saveDocument(doc, file);

            // Also add verification code to Users.xml
            addVerificationCodeToUsersXML(username, verificationCode);

            return verificationCode;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Add verification code to Users.xml file
     */
    private void addVerificationCodeToUsersXML(String username, String verificationCode) {
        try {
            File usersFile = new File(FILE_PATH); // FILE_PATH = "src/main/resources/XMLFiles/Users.xml"
            if (!usersFile.exists()) {
                System.err.println("Users.xml file does not exist");
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(usersFile);
            doc.getDocumentElement().normalize();

            NodeList users = doc.getElementsByTagName("user");
            for (int i = 0; i < users.getLength(); i++) {
                Element user = (Element) users.item(i);
                String uname = user.getElementsByTagName("username").item(0).getTextContent();

                if (uname.equals(username)) {
                    // Remove existing verification code if present
                    NodeList existingCodes = user.getElementsByTagName("verificationCode");
                    for (int j = existingCodes.getLength() - 1; j >= 0; j--) {
                        user.removeChild(existingCodes.item(j));
                    }

                    // Add new verification code
                    Element codeElement = doc.createElement("verificationCode");
                    codeElement.appendChild(doc.createTextNode(verificationCode));
                    user.appendChild(codeElement);

                    // Add timestamp for expiration
                    NodeList existingTimestamps = user.getElementsByTagName("verificationTimestamp");
                    for (int j = existingTimestamps.getLength() - 1; j >= 0; j--) {
                        user.removeChild(existingTimestamps.item(j));
                    }

                    Element timestampElement = doc.createElement("verificationTimestamp");
                    timestampElement.appendChild(doc.createTextNode(String.valueOf(System.currentTimeMillis())));
                    user.appendChild(timestampElement);

                    // Save the updated Users.xml
                    TransformerFactory tf = TransformerFactory.newInstance();
                    Transformer transformer = tf.newTransformer();
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.transform(new DOMSource(doc), new StreamResult(usersFile));

                    System.out.println("Added verification code to Users.xml for user: " + username);
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error adding verification code to Users.xml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean verifyResetCode(String username, String providedCode) {
        try {
            System.out.println("[DEBUG] Verifying reset code for user: " + username);
            System.out.println("[DEBUG] Provided code: " + providedCode);

            File file = new File(FILE_PATH);
            if (!file.exists()) {
                System.out.println("[DEBUG] Users.xml file does not exist: " + file.getAbsolutePath());
                return false;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList users = doc.getElementsByTagName("user");
            for (int i = 0; i < users.getLength(); i++) {
                Element user = (Element) users.item(i);
                String uname = user.getElementsByTagName("username").item(0).getTextContent();

                if (uname.equals(username)) {
                    System.out.println("[DEBUG] Found user: " + username);

                    // Look for 'verification' element (permanent security code)
                    NodeList codeNodes = user.getElementsByTagName("verification");
                    if (codeNodes.getLength() > 0) {
                        String storedCode = codeNodes.item(0).getTextContent().trim();
                        System.out.println("[DEBUG] Stored code: " + storedCode + ", Provided code: " + providedCode);

                        boolean codeMatches = storedCode.equals(providedCode);
                        System.out.println("[DEBUG] Code verification result: " + codeMatches);
                        return codeMatches;
                    } else {
                        System.out.println("[DEBUG] No verification code found for user: " + username);
                        return false;
                    }
                }
            }

            System.out.println("[DEBUG] User not found: " + username);
            return false;
        } catch (Exception e) {
            System.err.println("[ERROR] Error verifying reset code: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static class GroupInfo {
        private String owner;
        private int memberCount;

        public GroupInfo(String owner, int memberCount) {
            this.owner = owner;
            this.memberCount = memberCount;
        }

        public String getOwner() {
            return owner;
        }

        public int getMemberCount() {
            return memberCount;
        }

        public void incrementMemberCount() {
            this.memberCount++;
        }
    }
    public synchronized String changeUsername(String oldUsername, String newUsername) {
        try {
            // Validate inputs
            if (newUsername == null || newUsername.trim().isEmpty()) {
                return "USERNAME_CHANGE_FAILED";
            }

            newUsername = newUsername.trim();

            // Check if new username already exists
            if (userExists(newUsername)) {
                return "USERNAME_EXISTS";
            }

            File file = new File(FILE_PATH);
            if (!file.exists()) {
                return "USERNAME_CHANGE_FAILED";
            }

            // Step 1: Update main Users.xml file
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList users = doc.getElementsByTagName("user");
            boolean userFound = false;

            for (int i = 0; i < users.getLength(); i++) {
                Node user = users.item(i);
                if (user.getNodeType() == Node.ELEMENT_NODE) {
                    Element userElement = (Element) user;
                    String currentUsername = userElement.getElementsByTagName("username")
                            .item(0).getTextContent().trim();

                    if (currentUsername.equals(oldUsername)) {
                        userElement.getElementsByTagName("username")
                                .item(0).setTextContent(newUsername);
                        userFound = true;
                        break;
                    }
                }
            }

            if (!userFound) {
                return "USERNAME_CHANGE_FAILED";
            }

            // Save updated Users.xml
            removeEmptyTextNodes(doc);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.transform(new DOMSource(doc), new StreamResult(file));

            // Step 2: Update all account XML files before renaming
            updateUsernameInAllAccountFiles(oldUsername, newUsername);

            // Step 3: Rename the user's account XML file
            renameUserAccountFile(oldUsername, newUsername);

            return "USERNAME_CHANGED";

        } catch (Exception e) {
            System.err.println("Error changing username: " + e.getMessage());
            e.printStackTrace();
            return "USERNAME_CHANGE_FAILED";
        }
    }
    public synchronized String changePassword(String username, String oldPassword, String newPassword) {
        try {
            // Validate inputs
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return "PASSWORD_CHANGE_FAILED";
            }

            File file = new File(FILE_PATH);
            if (!file.exists()) {
                return "PASSWORD_CHANGE_FAILED";
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList users = doc.getElementsByTagName("user");
            boolean userFound = false;

            for (int i = 0; i < users.getLength(); i++) {
                Node user = users.item(i);
                if (user.getNodeType() == Node.ELEMENT_NODE) {
                    Element userElement = (Element) user;
                    String currentUsername = userElement.getElementsByTagName("username")
                            .item(0).getTextContent().trim();
                    String currentPassword = userElement.getElementsByTagName("password")
                            .item(0).getTextContent().trim();

                    if (currentUsername.equals(username)) {
                        // Verify old password
                        if (!currentPassword.equals(oldPassword)) {
                            return "INVALID_OLD_PASSWORD";
                        }

                        // Check if new password is same as old
                        if (oldPassword.equals(newPassword)) {
                            return "PASSWORD_CHANGE_FAILED";
                        }

                        // Update password
                        userElement.getElementsByTagName("password")
                                .item(0).setTextContent(newPassword);
                        userFound = true;
                        break;
                    }
                }
            }

            if (!userFound) {
                return "PASSWORD_CHANGE_FAILED";
            }

            // FIXED: Use consistent transformer settings
            removeEmptyTextNodes(doc);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.transform(new DOMSource(doc), new StreamResult(file));

            System.out.println("Password changed for user: " + username);
            return "PASSWORD_CHANGED";

        } catch (Exception e) {
            System.err.println("Error changing password: " + e.getMessage());
            e.printStackTrace();
            return "PASSWORD_CHANGE_FAILED";
        }
    }
    /**
     * Updates username references in all account XML files
     * This includes chat histories, group memberships, and friend relationships
     */
    // Enhanced version with better debugging and file handling
    private void updateUsernameInAllAccountFiles(String oldUsername, String newUsername) {
        try {
            File xmlFilesDir = new File("src/main/resources/XMLFiles/");
            if (!xmlFilesDir.exists()) {
                return;
            }

            File[] accountFiles = xmlFilesDir.listFiles((dir, name) -> name.endsWith("Account.xml"));
            if (accountFiles == null || accountFiles.length == 0) {
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            for (File accountFile : accountFiles) {
                try {
                    boolean fileModified = false;
                    Document doc = builder.parse(accountFile);
                    Element root = doc.getDocumentElement();

                    // Update sender tags in chat histories
                    NodeList allSenderNodes = root.getElementsByTagName("sender");
                    for (int i = 0; i < allSenderNodes.getLength(); i++) {
                        Element senderElement = (Element) allSenderNodes.item(i);
                        String senderText = senderElement.getTextContent().trim();
                        if (oldUsername.equals(senderText)) {
                            senderElement.setTextContent(newUsername);
                            fileModified = true;
                        }
                    }

                    // Update username tags in friend lists
                    NodeList friendNodes = root.getElementsByTagName("friend");
                    for (int i = 0; i < friendNodes.getLength(); i++) {
                        Element friendElement = (Element) friendNodes.item(i);
                        NodeList friendUsernameNodes = friendElement.getElementsByTagName("username");
                        for (int j = 0; j < friendUsernameNodes.getLength(); j++) {
                            Element usernameElement = (Element) friendUsernameNodes.item(j);
                            String usernameText = usernameElement.getTextContent().trim();
                            if (oldUsername.equals(usernameText)) {
                                usernameElement.setTextContent(newUsername);
                                fileModified = true;
                            }
                        }
                    }

                    // Update username tags in group members
                    NodeList groupNodes = root.getElementsByTagName("group");
                    for (int i = 0; i < groupNodes.getLength(); i++) {
                        Element groupElement = (Element) groupNodes.item(i);
                        NodeList groupUsernameNodes = groupElement.getElementsByTagName("username");
                        for (int j = 0; j < groupUsernameNodes.getLength(); j++) {
                            Element usernameElement = (Element) groupUsernameNodes.item(j);
                            String usernameText = usernameElement.getTextContent().trim();
                            if (oldUsername.equals(usernameText)) {
                                usernameElement.setTextContent(newUsername);
                                fileModified = true;
                            }
                        }
                    }

                    // Update group owner tags
                    NodeList groupOwnerNodes = root.getElementsByTagName("groupOwner");
                    for (int i = 0; i < groupOwnerNodes.getLength(); i++) {
                        Element groupOwner = (Element) groupOwnerNodes.item(i);
                        String ownerText = groupOwner.getTextContent().trim();
                        if (oldUsername.equals(ownerText)) {
                            groupOwner.setTextContent(newUsername);
                            fileModified = true;
                        }
                    }

                    // Save the file if it was modified
                    if (fileModified) {
                        saveDocument(doc, accountFile);
                    }

                } catch (Exception e) {
                    System.err.println("Error updating account file " + accountFile.getName() + ": " + e.getMessage());
                    // Continue with other files even if one fails
                }
            }

        } catch (Exception e) {
            System.err.println("Error in updateUsernameInAllAccountFiles: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * rename user account file and associated directories
     */
    private void renameUserAccountFile(String oldUsername, String newUsername) {
        try {
            // Rename main account XML file
            String oldAccountPath = "src/main/resources/XMLFiles/" + oldUsername + "Account.xml";
            String newAccountPath = "src/main/resources/XMLFiles/" + newUsername + "Account.xml";

            File oldFile = new File(oldAccountPath);
            File newFile = new File(newAccountPath);

            if (oldFile.exists()) {
                boolean renamed = oldFile.renameTo(newFile);
                if (renamed) {
                    System.out.println("Account file renamed from " + oldUsername + "Account.xml to " + newUsername + "Account.xml");
                } else {
                    System.err.println("Failed to rename account file from " + oldAccountPath + " to " + newAccountPath);
                }
            } else {
                System.err.println("Old account file not found: " + oldAccountPath);
            }

            // Rename user files directory if it exists
            String oldUserFilesDirPath = "src/main/resources/XMLFiles/userFiles/" + oldUsername;
            String newUserFilesDirPath = "src/main/resources/XMLFiles/userFiles/" + newUsername;

            File oldUserFilesDir = new File(oldUserFilesDirPath);
            File newUserFilesDir = new File(newUserFilesDirPath);

            if (oldUserFilesDir.exists()) {
                boolean dirRenamed = oldUserFilesDir.renameTo(newUserFilesDir);
                if (dirRenamed) {
                    System.out.println("User files directory renamed from " + oldUsername + " to " + newUsername);
                } else {
                    System.err.println("Failed to rename user files directory from " + oldUserFilesDirPath + " to " + newUserFilesDirPath);
                }
            } else {
                System.out.println("User files directory not found (this is normal): " + oldUserFilesDirPath);
            }

        } catch (Exception e) {
            System.err.println("Error renaming user account file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private String getUpdatedAccountXML(String username) {
        try {
            File userFile = new File("src/main/resources/XMLFiles/" + username + "Account.xml");
            if (userFile.exists()) {
                byte[] fileContent = java.nio.file.Files.readAllBytes(userFile.toPath());
                return java.util.Base64.getEncoder().encodeToString(fileContent);
            }
        } catch (Exception e) {
            System.err.println("Error getting updated account XML: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }
}

