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
import java.util.ArrayList;
import java.util.List;

public class ServerXMLHandler {
    private static final String FILE_PATH = "src/main/resources/XMLFiles/Users.xml";

    /*
     * This method creates a new XML file for a user account if it does not already exist.
     * It builds a basic XML structure with a <user> root element and saves it.
     * In short: ensures each user has their own account XML file created once.
     */
    private void createUserAccountXML(String username) throws Exception {
        File accountFile = new File("src/main/resources/XMLFiles/" + username + "Account.xml");
        if (accountFile.exists()) {
            return;
    private static final String ADMIN_FILE_PATH = "src/main/resources/XMLFiles/AdminCredentials.xml";
    private static List<String> communityMessages = new ArrayList<>();
    private static List<String> activeUsers = new ArrayList<>();
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
            // First try to validate from Users.xml (for users with admin role)
            File file = new File(ADMIN_FILE_PATH);
            if (file.exists()) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(file);

                NodeList users = doc.getElementsByTagName("user");
                for (int i = 0; i < users.getLength(); i++) {
                    Element user = (Element) users.item(i);
                    String uname = user.getElementsByTagName("username").item(0).getTextContent();
                    String pass = user.getElementsByTagName("password").item(0).getTextContent();

                    // Check if user has admin role
                    NodeList roleNodes = user.getElementsByTagName("role");
                    boolean hasAdminRole = false;
                    if (roleNodes.getLength() > 0) {
                        String role = roleNodes.item(0).getTextContent();
                        hasAdminRole = "admin".equals(role);
                    }

                    if (uname.equals(username) && pass.equals(password) && hasAdminRole) {
                        System.out.println("Admin validated from Users.xml: " + username);
                        return true;
                    }
                }
            }

            // Fallback to separate admin credentials file
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

            System.out.println("Admin validation failed for: " + username);
            return false;

        } catch (Exception e) {
            System.err.println("Error validating admin credentials: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static void createDefaultAdminFile() {
        try {
            // Create directory if it doesn't exist
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
            System.out.println("Default admin credentials: admin/admin123");
        } catch (Exception e) {
            System.err.println("Error creating default admin file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /*
     * This method reads and processes an incoming XML message.
     * It first extracts the <type> and <content> to decide what action to perform.
     * Depending on the type, it handles login, registration, group creation/joining, listing groups,
     * sending private messages, sending group/community messages, retrieving chat history,
     * and file transfers (both private and group).
     * It returns a reply string that indicates the result (e.g., LOGIN_SUCCESS, REGISTER_FAILED, SEND_FILE_SUCCESS).
     * In short: it is the main message handler that interprets requests and gives the right response.
     */
    public String readMessage(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
        doc.getDocumentElement().normalize();
        String type = doc.getElementsByTagName("type").item(0).getTextContent();
        String reply = "";

        String[] content = doc.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");

        switch (type) {
            // **CHANGED: Added admin login case**
            case "ADMIN_LOGIN":
                String adminUsername = content[0];
                String adminPassword = content[1];
                if (validateAdmin(adminUsername, adminPassword)) {
                    reply = "ADMIN_LOGIN_SUCCESS";
                } else {
                    reply = "ADMIN_LOGIN_FAILED";
                }
                break;

            // **CHANGED: Added admin operation cases**
            case "GET_ALL_USERS":
                reply = "ALL_USERS";
                break;

            case "GET_ALL_GROUPS":
                reply = "ALL_GROUPS";
                break;

            case "DELETE_USER":
                String userToDelete = content[0];
                if (deleteUserFromXML(FILE_PATH, userToDelete)) {
                    reply = "USER_DELETED";
                } else {
                    reply = "USER_DELETE_FAILED";
                }
                break;

            case "DELETE_GROUP":
                String groupToDelete = content[0];
                String groupOwner = content[1];
                if (deleteGroupFromXML(FILE_PATH, groupToDelete, groupOwner)) {
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
                reply = "USER_STATS";
                break;

            case "SEARCH_USERS":
                String userKeyword = content[0];
                reply = "SEARCH_USERS_RESULT";
                break;

            case "SEARCH_GROUPS":
                String groupKeyword = content[0];
                reply = "SEARCH_GROUPS_RESULT";
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
                if (createGroup(newGroupName, groupCreator)) {
                    reply = "GROUP_CREATED";
                } else {
                    reply = "GROUP_CREATE_FAILED";
                }
                break;

            case "GET_ACTIVE_USERS":
                reply = "ACTIVE_USERS";
                break;

            case "GET_COMMUNITY_MESSAGES":
                reply = "COMMUNITY_MESSAGES";
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
                            // **CHANGED: Add user to active users list**
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
                // **CHANGED: Handle community messages for admin monitoring**
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

        }
        return reply;
    }
    /*
     * This method stores a file (in Base64 format) for a specific user.
     * It validates the username, file name, and file content before proceeding.
     * Then it decodes the Base64 to ensure the content is valid and creates a user-specific folder if needed.
     * A unique file name is generated using a timestamp, and the file’s metadata (sender, original name, extension, size, etc.)
     * along with the Base64 content is written into a text file.
     * Finally, it checks if the file was saved successfully and returns true or false.
     * In short: it safely saves file data and metadata for each user.
     */
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

            // Validate base64 content
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

            // Create user directory
            File userDir = new File("src/main/resources/XMLFiles/userFiles/" + username.trim());
            if (!userDir.exists()) {
                boolean created = userDir.mkdirs();
                if (!created) {
                    System.err.println("Failed to create directory: " + userDir.getAbsolutePath());
                    // Try alternative path
                    userDir = new File("XMLFiles/userFiles/" + username.trim());
                    created = userDir.mkdirs();
                    if (!created) {
                        System.err.println("Failed to create alternative directory: " + userDir.getAbsolutePath());
                        return false;
                    }
                }
            }

            // Generate unique filename with original name preserved
            String timestamp = String.valueOf(System.currentTimeMillis());
            String uniqueFileName = timestamp + "_" + fileName.trim();
            File fileDataFile = new File(userDir, uniqueFileName);

            // Write file metadata and content with proper format
            try (PrintWriter writer = new PrintWriter(fileDataFile, "UTF-8")) {
                writer.println("SENDER:" + sender);
                writer.println("ORIGINAL_NAME:" + fileName.trim());
                writer.println("EXTENSION:" + (fileExtension != null ? fileExtension : ""));
                writer.println("TIMESTAMP:" + timestamp);
                writer.println("SIZE:" + base64Content.length());
                writer.println("CONTENT:" + base64Content);
                writer.flush();
            }

            // Verify file was created successfully
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
            Document doc = loadDocument();
            NodeList groups = doc.getElementsByTagName("group");
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < groups.getLength(); i++) {
                Element group = (Element) groups.item(i);
                String groupName = group.getAttribute("name");
                String creator = group.getAttribute("creator");

                if (i > 0) result.append(",");
                result.append(groupName).append("![]!").append(creator);
            }

            return result.toString();
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

            // Count total messages sent
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
    /*
     * This method reads the group chat history of a specific member from their account XML file.
     * It checks if the account file exists, then parses the XML to find the target group.
     * If the group is found, it retrieves all chat messages inside it, combining each sender with their message.
     * Any errors during the process are caught and logged.
     * In short: it gets a member’s saved group chat messages from their XML file.
     */
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
    /*
     * This method creates a group entry with chat history inside a user’s account XML file.
     * It first checks if the user’s XML exists, and if not, creates a new one with a <user> root.
     * Then it adds a <group> with the group’s name, members, and chat history.
     * Each chat message is split into sender and message, then stored as <chat> elements.
     * Finally, the group is saved to the XML file.
     * In short: it builds and saves a group with members and past messages for the user.
     */
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
    /*
     * This method checks all groups in the main XML and makes sure that each user
     * (both creator and members) has the group properly listed in their account file.
     * It loops through every group, gets its creator and members, then calls a helper
     * to ensure their account XML reflects the group.
     * In short: it fixes and syncs group memberships so no user is missing the group.
     */
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
    /*
     * This method makes sure a specific user has a record of a given group in their account XML.
     * It first checks if the group already exists for the user — if yes, nothing is added.
     * If the group is missing, it creates a new group entry with its name, members,
     * and available chat history (copied from another member’s account).
     * Finally, it saves the updated file so the user’s account stays consistent with group memberships.
     */

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
    /*
     * This method collects all members of a given group.
     * It looks into the main XML to find the group, then adds the creator and each member to a list.
     * If no members are found there, it checks user account XML files as a backup.
     * In the end, it returns the full list of members so the group can be managed properly.
     */
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
    /*
     * This method searches through all user account XML files to find users who belong to a specific group.
     * It adds any user with the group in their account to a list.
     * This acts as a fallback to ensure no group members are missed.
     * Finally, it returns the complete list of group members.
     */
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
    /*
     * This method checks if a specific user has a particular group in their account XML file.
     * It reads the user's XML, searches for the group, and returns true if found, false otherwise.
     * Any errors during parsing are caught and reported.
     */
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
    /*
     * This method updates a user's account XML by adding a new message to a specific group's chat history.
     * It ensures the user's XML exists, finds or creates the target group, and appends the sender's message.
     * Group members are included when creating a new group entry, and the XML is saved afterward.
     * Any necessary parsing or IO exceptions are propagated to the caller.
     */
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

    private void removeEmptyTextNodes(Document doc) {
        NodeList list = doc.getElementsByTagName("*");
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            Node child = node.getFirstChild();
            while (child != null) {
                Node next = child.getNextSibling();
                if (child.getNodeType() == Node.TEXT_NODE && child.getTextContent().trim().isEmpty()) {
                    node.removeChild(child);
                }
                child = next;
            }
        }
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
    /*
     * This method processes accepting a friend request between two users.
     * It searches the XML for a matching request and removes it, then adds a new friendship entry under the friends section.
     * The XML document is saved afterward, and true is returned if successful; otherwise, false.
     */
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
    /*
     * This method handles declining a friend request.
     * It searches the XML document for a request from the specified friend to the user,
     * removes it if found, and saves the updated document.
     * Returns true if a request was successfully declined, false otherwise.
     */
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
    /*
     * These methods handle loading and saving XML documents.
     * `loadDocument` reads the XML from the predefined file path and returns a Document object.
     * `saveDocument` writes the Document back to the file, either to the default path or a specified file,
     * formatting the XML properly and ensuring empty text nodes are removed.
     */
    private Document loadDocument() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new File(FILE_PATH));
    }

    private void saveDocument(Document doc) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.transform(new DOMSource(doc), new StreamResult(new File(FILE_PATH)));
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

        transformer.transform(new DOMSource(doc), new StreamResult(file));
    }
    /*
     * This method creates a new group in the XML document.
     * It ensures the <groups> element exists, adds a new <group> with the specified name and creator,
     * and then saves the document. Returns true if successful, false otherwise.
     */
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

            saveDocument(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    /*
     * This method allows a user to join an existing group in the XML document.
     * It first checks if the group exists and whether the user is already a member.
     * If not, it adds the user as a new <member>, updates the user's account,
     * saves the document, and returns true. Returns false if the group doesn't exist or on error.
     */
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
    /*
     * These methods handle updating a user's account XML when they join a group
     * and retrieving a list of all existing groups.
     * updateUserAccountWithGroup ensures the group and its chat history are added to the user's account,
     * avoiding duplicates, while listGroups simply reads and returns all group names from the XML.
     */
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
    /*
     * This method retrieves all groups that a user belongs to,
     * including groups they created and groups where they are listed as a member.
     * It reads the XML, checks each group's creator and member list,
     * and collects matching group names into a list to return.
     */
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
    /*
     * This method checks if a user is part of a specific group,
     * either as the creator or as a listed member.
     * It reads the XML data and returns true if the user is found, otherwise false.
     */
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
    /*
     * This method checks whether a given username already exists in the system.
     * It reads the XML file containing user accounts and iterates through each user element.
     * Compares the provided username with stored usernames in a case-insensitive manner.
     * Returns true if a matching username is found, otherwise returns false.
     * Includes exception handling to catch and print any errors during XML parsing or file access.
     * Ensures input is trimmed to avoid issues with leading/trailing spaces.
     */
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
    /*
     * This method adds a new user with a specified username and password to the XML document.
     * It first checks if the XML file exists; if not, it creates a new document with a root element.
     * Verifies that the username does not already exist to prevent duplicates.
     * Creates a new <user> element containing <username> and <password> child elements with trimmed text.
     * Appends the new user to the root element and saves the document back to the file.
     * Uses a transformer with indentation settings for readable XML formatting.
     * Returns true if the user was successfully added, otherwise false.
     * Includes exception handling to catch and print errors during parsing, transformation, or file access.
     */
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

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /*
     * This method deletes a user from the specified XML file based on the username.
     * It first checks if the file exists; if not, it returns false immediately.
     * Loads the XML document and normalizes it for consistent structure handling.
     * Iterates through all <user> elements to find a match with the given username.
     * If a match is found, removes the corresponding <user> element from the document.
     * Saves the updated XML back to the same file with proper indentation for readability.
     * Returns true if a user was successfully deleted, otherwise false.
     * Includes exception handling to catch and print any parsing, transformation, or I/O errors.
     */

    public static boolean deleteUserFromXML(String filePath, String username) {
        try {
            File xmlFile = new File(filePath);
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

                    // Save back to file
                    TransformerFactory tf = TransformerFactory.newInstance();
                    Transformer transformer = tf.newTransformer();
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.transform(new DOMSource(doc), new StreamResult(xmlFile));
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    /*
     * This method deletes a group from the specified XML file based on the group name and owner.
     * It first loads and normalizes the XML document to ensure consistent parsing.
     * Iterates through all <group> elements to find one that matches both the given group name and owner.
     * If a matching group is found, it is removed from the document.
     * The updated XML is then saved back to the same file with proper transformation.
     * Returns true if the group was successfully deleted, otherwise returns false.
     * Includes exception handling to catch and print any parsing, transformation, or I/O errors.
     */
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
            // Keep only last 100 messages
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
}

