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
    private static final String FILE_PATH = "src/main/resources/XMLFiles/Users.xml";
    private static final String ADMIN_FILE_PATH = "src/main/resources/XMLFiles/AdminCredentials.xml";
    private static final List<String> communityMessages = new ArrayList<>();
    private static final List<String> activeUsers = new ArrayList<>();

    public void registerUser(String username, String password) throws Exception {
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

    public static boolean validateUser(String username, String password) throws Exception {
        File file = new File(FILE_PATH);

        if (!file.exists()) return false;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);

        NodeList users = doc.getElementsByTagName("user");
        for (int i = 0; i < users.getLength(); i++) {
            Node user = users.item(i);
            if (user.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) user;

                String uname = e.getElementsByTagName("username").item(0).getTextContent();
                String pass = e.getElementsByTagName("password").item(0).getTextContent();

                if (uname.equals(username) && pass.equals(password)) {
                    return true;
                }
            }
        }
        return false;
    }
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
                } else {
                    reply = "PASSWORD_CHANGE_FAILED";
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
                try {
                    registerUser(newUsername, newUserPassword);
                    reply = "USER_CREATED";
                } catch (Exception e) {
                    reply = "USER_CREATE_FAILED";
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
                username = (content.length >= 2)
                        ? content[0].trim()
                        : (doc.getElementsByTagName("username").getLength() > 0
                        ? doc.getElementsByTagName("username").item(0).getTextContent().trim()
                        : null);
                password = (content.length >= 2)
                        ? content[1].trim()
                        : (doc.getElementsByTagName("password").getLength() > 0
                        ? doc.getElementsByTagName("password").item(0).getTextContent().trim()
                        : null);
                if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                    reply = "REGISTER_FAILED";
                    break;
                }
                synchronized (this) {
                    if (userExists(username)) {
                        reply = "USER_EXISTS";
                    } else {
                        boolean saved = addUserToDocument(username, password);
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

            case "REQUEST_PASSWORD_RESET":
                String resetUsername = content[0];
                String verificationCode = generateVerificationCode(resetUsername);

                if (verificationCode != null) {
                    reply = "PASSWORD_RESET_REQUESTED![]!" + resetUsername + "![]!" + verificationCode;
                } else {
                    reply = "PASSWORD_RESET_FAILED";
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


        }
        return reply;
    }

    public synchronized boolean createGroupInUserAccounts(String groupName, String creator, String membersStr) {
        try {
            // Build member list
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

            // Create group in each member's account
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
    public String getUserStats(String username) {
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
                communityMessages.remove(0);
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

    private Document loadDocument() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new File(FILE_PATH));
    }

    private void saveDocument(Document doc, File file) throws TransformerException {
        // Remove empty text nodes to prevent extra whitespace
        removeEmptyTextNodes(doc);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        // Ensure clean formatting
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

    public String getSender(String xmlString) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory stringFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder stringBuilder = stringFactory.newDocumentBuilder();
        Document stringDoc = stringBuilder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
        stringDoc.getDocumentElement().normalize();
        String[] contentTag = stringDoc.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");
        return contentTag[0];
    }

    public String getReceiver(String xmlString) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory stringFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder stringBuilder = stringFactory.newDocumentBuilder();
        Document stringDoc = stringBuilder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
        stringDoc.getDocumentElement().normalize();
        String[] contentTag = stringDoc.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");
        return contentTag[1];
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
                    if (uname.equalsIgnoreCase(username.trim())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean addUserToDocument(String username, String password) {
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

            // Collect all groups with their owners and member counts
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
            return verificationCode;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public boolean verifyResetCode(String username, String providedCode) {
        try {
            File file = new File("src/main/resources/XMLFiles/" + username + "Account.xml");
            if (!file.exists()) {
                return false;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList codeNodes = doc.getElementsByTagName("verificationCode");
            NodeList timestampNodes = doc.getElementsByTagName("verificationTimestamp");

            if (codeNodes.getLength() == 0 || timestampNodes.getLength() == 0) {
                return false;
            }

            String storedCode = codeNodes.item(0).getTextContent();
            long timestamp = Long.parseLong(timestampNodes.item(0).getTextContent());
            long currentTime = System.currentTimeMillis();

            // Check if code is expired (10 minutes = 600,000 milliseconds)
            if (currentTime - timestamp > 600000) {
                return false;
            }

            return storedCode.equals(providedCode);
        } catch (Exception e) {
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
            System.out.println("=== STARTING USERNAME CHANGE PROCESS ===");
            System.out.println("Changing username from '" + oldUsername + "' to '" + newUsername + "'");

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
            System.out.println("Step 1: Updating main Users.xml file");
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
            System.out.println("Users.xml updated successfully");

            // Step 2: Update ALL account XML files BEFORE renaming (this is the key fix)
            System.out.println("Step 2: Updating all account XML files (before renaming)");
            updateUsernameInAllAccountFiles(oldUsername, newUsername);

            // Step 3: Rename the user's account XML file AFTER updating content
            System.out.println("Step 3: Renaming user account file");
            renameUserAccountFile(oldUsername, newUsername);

            // Step 4: Update file references and directories
            System.out.println("Step 4: Updating file references");
            updateFileReferencesAfterUsernameChange(oldUsername, newUsername);

            System.out.println("=== USERNAME CHANGE PROCESS COMPLETE ===");
            System.out.println("Username successfully changed from " + oldUsername + " to " + newUsername);
            return "USERNAME_CHANGED";

        } catch (Exception e) {
            System.err.println("Error changing username: " + e.getMessage());
            e.printStackTrace();
            return "USERNAME_CHANGE_FAILED";
        }
    }

    /**
     * Updates username references in all account XML files
     * This includes chat histories, group memberships, and friend relationships
     */
    // Enhanced version with better debugging and file handling
    private void updateUsernameInAllAccountFiles(String oldUsername, String newUsername) {
        try {
            System.out.println("=== STARTING CASCADE UPDATE ===");
            System.out.println("Updating all references from '" + oldUsername + "' to '" + newUsername + "'");

            File xmlFilesDir = new File("src/main/resources/XMLFiles/");
            if (!xmlFilesDir.exists()) {
                System.err.println("XMLFiles directory not found: " + xmlFilesDir.getAbsolutePath());
                return;
            }

            File[] accountFiles = xmlFilesDir.listFiles((dir, name) -> name.endsWith("Account.xml"));

            if (accountFiles == null || accountFiles.length == 0) {
                System.err.println("No account files found for username update");
                return;
            }

            System.out.println("Found " + accountFiles.length + " account files to update:");
            for (File f : accountFiles) {
                System.out.println("  - " + f.getName() + " (exists: " + f.exists() + ", size: " + f.length() + " bytes)");
            }

            for (File accountFile : accountFiles) {
                try {
                    System.out.println("\n--- Processing: " + accountFile.getName() + " ---");

                    // Force refresh - create new factory and builder for each file
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();

                    boolean fileModified = false;
                    Document doc = builder.parse(accountFile);
                    Element root = doc.getDocumentElement();

                    if (root == null) {
                        System.err.println("ERROR: No root element found in " + accountFile.getName());
                        continue;
                    }

                    int totalUpdates = 0;

                    // 1. Update ALL sender tags in ALL chat histories
                    NodeList allSenderNodes = root.getElementsByTagName("sender");
                    System.out.println("Found " + allSenderNodes.getLength() + " sender elements");

                    for (int i = 0; i < allSenderNodes.getLength(); i++) {
                        Element senderElement = (Element) allSenderNodes.item(i);
                        String senderText = senderElement.getTextContent();

                        // More detailed debugging
                        System.out.println("  Sender[" + i + "]: '" + senderText + "' (trimmed: '" + senderText.trim() + "')");
                        System.out.println("    Comparing '" + senderText.trim() + "' with '" + oldUsername + "'");
                        System.out.println("    Equals check: " + oldUsername.equals(senderText.trim()));

                        if (oldUsername.equals(senderText.trim())) {
                            System.out.println("    -> MATCH FOUND! Updating to '" + newUsername + "'");
                            senderElement.setTextContent(newUsername);
                            fileModified = true;
                            totalUpdates++;

                            // Verify the update worked
                            String updatedText = senderElement.getTextContent();
                            System.out.println("    -> Verification: new value is '" + updatedText + "'");
                        } else {
                            System.out.println("    -> No match, skipping");
                        }
                    }

                    // 2. Update username tags in friend lists
                    NodeList friendNodes = root.getElementsByTagName("friend");
                    System.out.println("Found " + friendNodes.getLength() + " friend elements");
                    for (int i = 0; i < friendNodes.getLength(); i++) {
                        Element friendElement = (Element) friendNodes.item(i);
                        NodeList friendUsernameNodes = friendElement.getElementsByTagName("username");
                        for (int j = 0; j < friendUsernameNodes.getLength(); j++) {
                            Element usernameElement = (Element) friendUsernameNodes.item(j);
                            String usernameText = usernameElement.getTextContent().trim();
                            System.out.println("  Friend username: '" + usernameText + "'");
                            if (oldUsername.equals(usernameText)) {
                                usernameElement.setTextContent(newUsername);
                                fileModified = true;
                                totalUpdates++;
                                System.out.println("    -> UPDATED friend username: '" + oldUsername + "' to '" + newUsername + "'");
                            }
                        }
                    }

                    // 3. Update username tags in group members
                    NodeList groupNodes = root.getElementsByTagName("group");
                    System.out.println("Found " + groupNodes.getLength() + " group elements");
                    for (int i = 0; i < groupNodes.getLength(); i++) {
                        Element groupElement = (Element) groupNodes.item(i);
                        NodeList groupUsernameNodes = groupElement.getElementsByTagName("username");
                        for (int j = 0; j < groupUsernameNodes.getLength(); j++) {
                            Element usernameElement = (Element) groupUsernameNodes.item(j);
                            String usernameText = usernameElement.getTextContent().trim();
                            System.out.println("  Group member: '" + usernameText + "'");
                            if (oldUsername.equals(usernameText)) {
                                usernameElement.setTextContent(newUsername);
                                fileModified = true;
                                totalUpdates++;
                                System.out.println("    -> UPDATED group member: '" + oldUsername + "' to '" + newUsername + "'");
                            }
                        }
                    }

                    // 4. Update group owner tags
                    NodeList groupOwnerNodes = root.getElementsByTagName("groupOwner");
                    System.out.println("Found " + groupOwnerNodes.getLength() + " groupOwner elements");
                    for (int i = 0; i < groupOwnerNodes.getLength(); i++) {
                        Element groupOwner = (Element) groupOwnerNodes.item(i);
                        String ownerText = groupOwner.getTextContent().trim();
                        System.out.println("  Group owner: '" + ownerText + "'");
                        if (oldUsername.equals(ownerText)) {
                            groupOwner.setTextContent(newUsername);
                            fileModified = true;
                            totalUpdates++;
                            System.out.println("    -> UPDATED group owner: '" + oldUsername + "' to '" + newUsername + "'");
                        }
                    }

                    System.out.println("Summary for " + accountFile.getName() + ":");
                    System.out.println("  - Total updates made: " + totalUpdates);
                    System.out.println("  - File modified: " + fileModified);

                    // Save the file if it was modified
                    if (fileModified) {
                        try {
                            System.out.println("  -> Attempting to save " + accountFile.getName());
                            saveDocument(doc, accountFile);
                            System.out.println("  -> SUCCESSFULLY SAVED " + accountFile.getName());

                            // Verify the file was saved by checking its timestamp
                            long newTimestamp = accountFile.lastModified();
                            System.out.println("  -> File timestamp: " + newTimestamp);

                        } catch (Exception saveException) {
                            System.err.println("  -> ERROR SAVING FILE: " + saveException.getMessage());
                            saveException.printStackTrace();
                        }
                    } else {
                        System.out.println("  -> NO CHANGES needed for " + accountFile.getName());
                    }

                } catch (Exception e) {
                    System.err.println("ERROR processing account file " + accountFile.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                    // Continue with other files even if one fails
                }
            }

            System.out.println("\n=== CASCADE UPDATE COMPLETE ===");

        } catch (Exception e) {
            System.err.println("FATAL ERROR in updateUsernameInAllAccountFiles: " + e.getMessage());
            e.printStackTrace();
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

    private void updateUserOwnAccountFileAfterRename(String newUsername, String oldUsername) {
        try {
            String accountFilePath = "src/main/resources/XMLFiles/" + newUsername + "Account.xml";
            File accountFile = new File(accountFilePath);

            if (!accountFile.exists()) {
                System.err.println("User's renamed account file not found: " + accountFilePath);
                return;
            }

            System.out.println("Updating user's own renamed account file: " + accountFilePath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(accountFile);
            Element root = doc.getDocumentElement();

            boolean fileModified = false;
            int totalUpdates = 0;

            // Update ALL sender tags where the user was the sender
            NodeList allSenderNodes = root.getElementsByTagName("sender");
            for (int i = 0; i < allSenderNodes.getLength(); i++) {
                Element senderElement = (Element) allSenderNodes.item(i);
                String senderText = senderElement.getTextContent().trim();
                if (oldUsername.equals(senderText)) {
                    senderElement.setTextContent(newUsername);
                    fileModified = true;
                    totalUpdates++;
                    System.out.println("  Updated sender in own account: '" + oldUsername + "' -> '" + newUsername + "'");
                }
            }

            // Update group owner references where this user was the owner
            NodeList groupOwnerNodes = root.getElementsByTagName("groupOwner");
            for (int i = 0; i < groupOwnerNodes.getLength(); i++) {
                Element groupOwner = (Element) groupOwnerNodes.item(i);
                String ownerText = groupOwner.getTextContent().trim();
                if (oldUsername.equals(ownerText)) {
                    groupOwner.setTextContent(newUsername);
                    fileModified = true;
                    totalUpdates++;
                    System.out.println("  Updated group owner in own account: '" + oldUsername + "' -> '" + newUsername + "'");
                }
            }

            System.out.println("Total updates in user's own account: " + totalUpdates);

            if (fileModified) {
                saveDocument(doc, accountFile);
                System.out.println("Successfully updated user's own account file: " + accountFilePath);
            } else {
                System.out.println("No changes needed in user's own account file");
            }

        } catch (Exception e) {
            System.err.println("Error updating user's own account file: " + e.getMessage());
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


    /**
     * Updates any file path references that might contain the old username
     */
    private void updateFileReferencesAfterUsernameChange(String oldUsername, String newUsername) {
        try {
            // This method can be extended to update any file path references
            // that might be stored in XML files or configuration
            System.out.println("File references updated for username change: " + oldUsername + " -> " + newUsername);
        } catch (Exception e) {
            System.err.println("Error updating file references: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Add this temporary method for testing
    public void testUsernameUpdate() {
        System.out.println("=== MANUAL TEST CASCADE UPDATE ===");
        updateUsernameInAllAccountFiles("tiben", "bob"); // reverse it back
        System.out.println("=== MANUAL TEST COMPLETE ===");
    }
}

