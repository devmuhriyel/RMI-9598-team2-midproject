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

    public String readMessage(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
        doc.getDocumentElement().normalize();
        String type = doc.getElementsByTagName("type").item(0).getTextContent();
        String reply = "";

        // FIX: Use the correct delimiter consistently
        String[] content = doc.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");

        switch (type){
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

                        // Store file data for BOTH sender and receiver
                        boolean senderFileStored = storeFileData(fileSender, fileSender, fileName, fileExtension, fileContent);
                        boolean receiverFileStored = storeFileData(fileReceiver, fileSender, fileName, fileExtension, fileContent);

                        if (senderFileStored && receiverFileStored) {
                            // Add file message to both sender and receiver chat history
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

                        // Process file for all group members (including sender)
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

    private boolean storeFileData(String username, String sender, String fileName, String fileExtension, String base64Content) {
        try {

            // Validate inputs
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

    // delete user and groups
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

