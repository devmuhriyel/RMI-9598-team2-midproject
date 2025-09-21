package ClientMVC;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class UserXMLHandler {

    /*
     * readMessage:
     * 1. Parse the incoming XML string and extract <type> and <content>.
     * 2. Depending on the type:
     *    - LOGIN: Check username/password in Users.xml → return LOGIN_SUCCESS or LOGIN_FAILED.
     *    - REGISTER: Add a new user (username + password) to Users.xml → return REGISTER_SUCCESS.
     *    - SEND_MESSAGE: (reserved for sending message handling).
     *    - RECEIVE_REQUEST: (reserved for request handling).
     * 3. Return the reply string based on the operation result.
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
            case "LOGIN":
                String username = content[0];
                String password = content[1];
                boolean found = false;
                File userAccounts = new File("src/main/resources/XMLFiles/Users.xml");
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
            case "REGISTER":
                File file = new File("src/main/resources/XMLFiles/Users.xml");
                DocumentBuilderFactory registerFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder registerBuilder = registerFactory.newDocumentBuilder();
                Document registerDoc;
                if (file.exists()) {
                    registerDoc = registerBuilder.parse(file);
                } else {
                    registerDoc = registerBuilder.newDocument();
                    Element root = registerDoc.createElement("users");
                    registerDoc.appendChild(root);
                }

                Element root = registerDoc.getDocumentElement();
                Element user = registerDoc.createElement("user");
                Element uname = registerDoc.createElement("username");
                uname.appendChild(registerDoc.createTextNode(content[0]));
                user.appendChild(uname);
                Element pass = registerDoc.createElement("password");
                pass.appendChild(registerDoc.createTextNode(content[1]));
                user.appendChild(pass);
                root.appendChild(user);
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.transform(new DOMSource(registerDoc), new StreamResult(file));

                reply = "REGISTER_SUCCESS";
                break;

            case "SEND_MESSAGE":
                break;

            case "RECEIVE_REQUEST":
                break;
        }
        return reply;
    }

    /*
     * getReply:
     * 1. Clean and validate the XML string (remove BOM, check format, ensure balanced XML).
     * 2. Use DocumentBuilder with an error handler to parse the XML safely.
     * 3. Look for <reply> tag and return its text content if found.
     * 4. If not found, check for "status" attribute in the root element and return it.
     * 5. If neither exists or an error occurs, return null (and log errors for debugging).
     */
    public String getReply(String xmlString) {
        try {
            xmlString = xmlString.trim();
            if (xmlString.startsWith("\uFEFF")) {
                xmlString = xmlString.substring(1);
            }
            if (!xmlString.startsWith("<") || !xmlString.endsWith(">")) {
                System.out.println("Not a valid XML message: " + xmlString.substring(0, Math.min(100, xmlString.length())));
                return null;
            }
            if (!isBalancedXML(xmlString)) {
                System.out.println("XML appears to be truncated or malformed");
                return null;
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new org.xml.sax.ErrorHandler() {
                @Override
                public void warning(org.xml.sax.SAXParseException exception) {
                    System.out.println("XML Warning: " + exception.getMessage());
                }

                @Override
                public void error(org.xml.sax.SAXParseException exception) {
                    System.out.println("XML Error: " + exception.getMessage());
                }

                @Override
                public void fatalError(org.xml.sax.SAXParseException exception) {
                    System.out.println("XML Fatal Error: " + exception.getMessage());
                }
            });

            Document doc = builder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();
            NodeList replyNodes = doc.getElementsByTagName("reply");
            if (replyNodes.getLength() > 0) {
                return replyNodes.item(0).getTextContent();
            }
            if (root.hasAttribute("status")) {
                return root.getAttribute("status");
            }

            return null;
        } catch (Exception e) {
            System.out.println("Error parsing XML. Length: " + xmlString.length());
            System.out.println("XML starts with: " + xmlString.substring(0, Math.min(200, xmlString.length())));
            System.out.println("XML ends with: " + xmlString.substring(Math.max(0, xmlString.length() - 200)));
            System.out.println("Error message: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private boolean isBalancedXML(String xml) {
        int openTags = 0;
        int closeTags = 0;
        for (int i = 0; i < xml.length() - 1; i++) {
            if (xml.charAt(i) == '<' && xml.charAt(i + 1) != '/' && xml.charAt(i + 1) != '?') {
                int tagEnd = xml.indexOf('>', i);
                if (tagEnd > i && xml.charAt(tagEnd - 1) != '/') {
                    openTags++;
                }
            } else if (xml.charAt(i) == '<' && xml.charAt(i + 1) == '/') {
                closeTags++;
            }
        }

        return openTags == closeTags;
    }

    /*
     * getResponseBody:
     * 1. Clean and validate the XML string (remove BOM, check format).
     * 2. Parse the XML using DocumentBuilder and normalize it.
     * 3. Search for the <body> tag:
     *      - If found, return its text content.
     *      - If not found, return null with a log message.
     * 4. Handle parsing errors by printing debug info and returning null.
     */
    public String getResponseBody(String xmlString) throws Exception {
        xmlString = xmlString.trim();
        if (xmlString.startsWith("\uFEFF")) {
            xmlString = xmlString.substring(1);
        }

        if (!xmlString.startsWith("<") || !xmlString.endsWith(">")) {
            System.out.println("Not valid XML format: " + xmlString.substring(0, Math.min(100, xmlString.length())));
            return null;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
            doc.getDocumentElement().normalize();

            NodeList bodyNodes = doc.getElementsByTagName("body");
            if (bodyNodes.getLength() > 0) {
                String bodyContent = bodyNodes.item(0).getTextContent();
                System.out.println("Extracted body content length: " + bodyContent.length());
                return bodyContent;
            }

            System.out.println("No <body> tag found in XML");
            return null;
        } catch (Exception e) {
            System.out.println("Error parsing XML body: " + e.getMessage());
            System.out.println("XML content: " + xmlString.substring(0, Math.min(500, xmlString.length())));
            e.printStackTrace();
            return null;
        }
    }
    /*
     * saveUpdatedAccountXML:
     * 1. Decode the given Base64 content into an XML string.
     * 2. Validate the XML structure (must start with '<' and end with '>').
     * 3. Save the decoded XML into a file named <username>Account.xml under resources/XMLFiles.
     * 4. Verify the file was created successfully and log its size.
     * 5. Handle errors gracefully with clear messages.
     */
    public void saveUpdatedAccountXML(String username, String base64Content) {
        try {
            System.out.println("Attempting to save XML for user: " + username);
            System.out.println("Base64 content length: " + base64Content.length());
            byte[] xmlContent = Base64.getDecoder().decode(base64Content);
            String xmlString = new String(xmlContent, "UTF-8");
            System.out.println("Decoded XML length: " + xmlString.length());
            System.out.println("XML preview: " + xmlString.substring(0, Math.min(200, xmlString.length())));
            if (!xmlString.trim().startsWith("<") || !xmlString.trim().endsWith(">")) {
                throw new IllegalArgumentException("Invalid XML structure");
            }

            String filePath = "src/main/resources/XMLFiles/" + username + "Account.xml";
            File file = new File(filePath);
            file.getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(xmlContent);
                fos.flush();
            }
            System.out.println("Successfully updated " + username + "Account.xml");
            if (file.exists() && file.length() > 0) {
                System.out.println("File verification successful. Size: " + file.length() + " bytes");
            } else {
                System.err.println("File verification failed!");
            }

        } catch (Exception e) {
            System.err.println("Error saving updated XML for user " + username + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /*
     * parseServerResponse:
     * 1. Clean the XML string by trimming and removing BOM if present.
     * 2. Validate if the string looks like proper XML (starts/ends with tags).
     * 3. Parse the XML into a DOM Document.
     * 4. Extract the root element and check if it has a "status" attribute.
     * 5. Retrieve the <body> tag content if it exists.
     * 6. Return a ServerResponse object with status and body.
     * 7. If parsing fails, log the error and return null.
     */
    public ServerResponse parseServerResponse(String xmlString) throws Exception {
        xmlString = xmlString.trim();
        if (xmlString.startsWith("\uFEFF")) {
            xmlString = xmlString.substring(1);
        }

        if (!xmlString.startsWith("<") || !xmlString.endsWith(">")) {
            return null;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();
            String status = root.hasAttribute("status") ? root.getAttribute("status") : null;
            String body = "";

            NodeList bodyNodes = doc.getElementsByTagName("body");
            if (bodyNodes.getLength() > 0) {
                body = bodyNodes.item(0).getTextContent();
            }

            return new ServerResponse(status, body);
        } catch (Exception e) {
            System.out.println("Error parsing server response: " + xmlString);
            return null;
        }
    }
    /*
     * getGroupChatHistoryWithSenders:
     * 1. Open the user's account XML file.
     * 2. Parse the XML document and normalize it.
     * 3. Find all <group> elements inside the file.
     * 4. Check each group to see if its name matches the given groupName.
     * 5. If a match is found:
     *      - Loop through all <chat> elements inside the group.
     *      - Extract <sender> and <msg> values for each chat.
     *      - Create ChatMessage objects and add them to the list.
     * 6. Return the full list of chat messages with senders.
     * 7. If an error occurs, print the stack trace and return what was collected.
     */
    public List<ChatMessage> getGroupChatHistoryWithSenders(String username, String groupName) {
        List<ChatMessage> messages = new ArrayList<>();
        try {
            File file = new File("src/main/resources/XMLFiles/" + username + "Account.xml");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            NodeList groupNodes = doc.getElementsByTagName("group");
            for (int i = 0; i < groupNodes.getLength(); i++) {
                Element groupElement = (Element) groupNodes.item(i);
                String gName = groupElement.getElementsByTagName("groupname").item(0).getTextContent();

                if (gName.equals(groupName)) {
                    NodeList chatNodes = groupElement.getElementsByTagName("chat");
                    for (int j = 0; j < chatNodes.getLength(); j++) {
                        Element chatElement = (Element) chatNodes.item(j);
                        String sender = chatElement.getElementsByTagName("sender").item(0).getTextContent();
                        String msg = chatElement.getElementsByTagName("msg").item(0).getTextContent();
                        messages.add(new ChatMessage(sender, msg));
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }
    /*
     * getChatHistoryWithSenders:
     * 1. Open the user's account XML file.
     * 2. Parse and normalize the XML document.
     * 3. Find all <friend> elements inside the file.
     * 4. Loop through each friend and check if the <username> matches the given friendName.
     * 5. If a match is found:
     *      - Loop through all <chat> elements under that friend.
     *      - Extract <sender> and <msg> values for each chat.
     *      - Create ChatMessage objects and add them to the list.
     * 6. Stop searching once the correct friend is found.
     * 7. Return the list of chat messages with senders.
     * 8. If an error occurs, print the stack trace and return what was collected so far.
     */
    public List<ChatMessage> getChatHistoryWithSenders(String username, String friendName) {
        List<ChatMessage> messages = new ArrayList<>();
        try {
            File file = new File("src/main/resources/XMLFiles/" + username + "Account.xml");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            NodeList friendNodes = doc.getElementsByTagName("friend");
            for (int i = 0; i < friendNodes.getLength(); i++) {
                Element friendElement = (Element) friendNodes.item(i);
                String fName = friendElement.getElementsByTagName("username").item(0).getTextContent();

                if (fName.equals(friendName)) {
                    NodeList chatNodes = friendElement.getElementsByTagName("chat");
                    for (int j = 0; j < chatNodes.getLength(); j++) {
                        Element chatElement = (Element) chatNodes.item(j);
                        String sender = chatElement.getElementsByTagName("sender").item(0).getTextContent();
                        String msg = chatElement.getElementsByTagName("msg").item(0).getTextContent();
                        messages.add(new ChatMessage(sender, msg));
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }

    /*
     * ChatMessage and ServerResponse helper classes:
     *
     * ChatMessage:
     * - Stores a chat message with two parts:
     *   1. sender → who sent the message
     *   2. message → the actual text of the message
     * - Provides getter methods to access sender and message.
     *
     * ServerResponse:
     * - Stores the server's reply after processing a request.
     *   1. status → tells what happened (e.g., LOGIN_SUCCESS, LOGIN_FAILED, FORCE_LOGOUT, etc.)
     *   2. body → contains the extra data from the server (like content or details).
     * - Provides getter methods to access status and body.
     * - Has helper methods:
     *      isLoginSuccess() → true if status = LOGIN_SUCCESS
     *      isLoginFailed()  → true if status = LOGIN_FAILED
     *      isForceLogout()  → true if status = FORCE_LOGOUT
     */
    public static class ChatMessage {
        private final String sender;
        private final String message;

        public ChatMessage(String sender, String message) {
            this.sender = sender;
            this.message = message;
        }

        public String getSender() {
            return sender;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class ServerResponse {
        private final String status;
        private final String body;

        public ServerResponse(String status, String body) {
            this.status = status;
            this.body = body;
        }

        public String getStatus() {
            return status;
        }

        public String getBody() {
            return body;
        }

        public boolean isLoginSuccess() {
            return "LOGIN_SUCCESS".equals(status);
        }

        public boolean isLoginFailed() {
            return "LOGIN_FAILED".equals(status);
        }

        public boolean isForceLogout() {
            return "FORCE_LOGOUT".equals(status);
        }
    }

    public List<String> getFriends(String username) {
        List<String> friends = new ArrayList<>();
        try {
            File file = new File("src/main/resources/XMLFiles/" + username + "Account.xml");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();
            NodeList friendNodes = doc.getElementsByTagName("friend");

            for (int i = 0; i < friendNodes.getLength(); i++) {
                Element friendElement = (Element) friendNodes.item(i);
                String friendName = friendElement.getElementsByTagName("username").item(0).getTextContent();
                friends.add(friendName);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return friends;
    }
    /*
     * getUserGroups(username):
     * -------------------------
     * - Purpose: Finds all groups a user belongs to by reading their XML account file.
     * Steps:
     * 1. Check if the user's XML file exists. If not, return an empty list.
     * 2. Load and parse the XML file.
     * 3. Look for all <group> elements in the file.
     * 4. For each <group>, extract the <groupname> text and add it to the list.
     * 5. Print the groups found (for debugging).
     * 6. If an error occurs, print the error and return an empty list.
     * Returns: A list of group names (List<String>) for the given user.
     */
    public List<String> getUserGroups(String username) {
        List<String> groups = new ArrayList<>();
        try {
            File file = new File("src/main/resources/XMLFiles/" + username + "Account.xml");
            if (!file.exists()) {
                System.out.println("Account XML file not found for user: " + username);
                return groups;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();
            NodeList groupList = doc.getElementsByTagName("group");
            for (int i = 0; i < groupList.getLength(); i++) {
                Element group = (Element) groupList.item(i);
                NodeList groupNameNodes = group.getElementsByTagName("groupname");
                if (groupNameNodes.getLength() > 0) {
                    String groupName = groupNameNodes.item(0).getTextContent();
                    groups.add(groupName);
                    System.out.println("Found group for user " + username + ": " + groupName);
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting user groups for " + username + ": " + e.getMessage());
            e.printStackTrace();
        }
        return groups;
    }
    /*
     * getAllUsers():
     * ---------------
     * - Purpose: Reads all usernames from Users.xml and returns them in a list.
     * Steps:
     * 1. Open and parse the Users.xml file.
     * 2. Look for every <user> element.
     * 3. For each <user>, get the <username> value.
     * 4. Add each username to the list.
     * 5. If an error happens, print the stack trace.
     * Returns: A list of all usernames in the system.
     */
    public List<String> getAllUsers() {
        List<String> users = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File("src/main/resources/XMLFiles/Users.xml")); // adjust path if needed
            NodeList nodeList = doc.getElementsByTagName("user");

            for (int i = 0; i < nodeList.getLength(); i++) {
                Element userElement = (Element) nodeList.item(i);
                String username = userElement.getElementsByTagName("username")
                        .item(0)
                        .getTextContent();
                users.add(username);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }

    public static boolean addFriendRequest(String fromUser, String toUser) {
        try {
            File file = new File("src/main/resources/XMLFiles/Requests.xml");
            if (!file.exists()) {
                System.out.println("Requests.xml file not found. Creating a new one...");
                // If the file doesn’t exist, create a basic structure
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.newDocument();

                Element root = doc.createElement("requests");
                doc.appendChild(root);

                // Save the new empty file
                saveDocument(doc, file.getPath());
            }

            // Load the existing document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();

            // Create <request> entry
            Element requestElement = doc.createElement("request");

            Element fromElement = doc.createElement("from");
            fromElement.setTextContent(fromUser);
            requestElement.appendChild(fromElement);

            Element toElement = doc.createElement("to");
            toElement.setTextContent(toUser);
            requestElement.appendChild(toElement);

            Element statusElement = doc.createElement("status");
            statusElement.setTextContent("pending");
            requestElement.appendChild(statusElement);

            root.appendChild(requestElement);

            saveDocument(doc, file.getPath());
            System.out.println("Friend request from " + fromUser + " to " + toUser + " added.");
            return true;

        } catch (Exception e) {
            System.err.println("Error adding friend request from " + fromUser + " to " + toUser + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static List<String> getSentRequests(String sender) {
        List<String> requests = new ArrayList<>();
        try {
            File file = new File("src/main/resources/XMLFiles/Requests.xml");
            if (!file.exists()) return requests;

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            NodeList requestNodes = doc.getElementsByTagName("request");
            for (int i = 0; i < requestNodes.getLength(); i++) {
                Element reqEl = (Element) requestNodes.item(i);
                String from = reqEl.getElementsByTagName("from").item(0).getTextContent();
                String to = reqEl.getElementsByTagName("to").item(0).getTextContent();

                if (from.equals(sender)) {
                    requests.add(to);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return requests;
    }

    private static void saveDocument(Document doc, String filePath) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(filePath));
            transformer.transform(source, result);

            System.out.println("XML saved successfully to " + filePath);
        } catch (Exception e) {
            System.err.println("Error saving XML to " + filePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }


}