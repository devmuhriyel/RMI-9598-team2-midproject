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
    public String readMessage(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
        doc.getDocumentElement().normalize();
        String type = doc.getElementsByTagName("type").item(0).getTextContent();
        String reply = "";
        String[] content = doc.getElementsByTagName("content").item(0).getTextContent().split("waw");;
        switch (type){
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
                Element uname = registerDoc.createElement("username"); // Fixed: was using 'doc'
                uname.appendChild(registerDoc.createTextNode(content[0])); // Fixed: was using 'doc'
                user.appendChild(uname);
                Element pass = registerDoc.createElement("password"); // Fixed: was using 'doc'
                pass.appendChild(registerDoc.createTextNode(content[1])); // Fixed: was using 'doc'
                user.appendChild(pass);
                root.appendChild(user);
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.transform(new DOMSource(registerDoc), new StreamResult(file)); // Fixed: was using 'doc'
                reply = "REGISTER_SUCCESS";
                break;
            case "SEND_MESSAGE":
                break;
        }
        return reply;
    }

    // Updated method to prioritize <reply> format over status attribute format
    public String getReply(String xmlString) throws Exception {
        // Clean and validate
        xmlString = xmlString.trim();
        if (xmlString.startsWith("\uFEFF")) {
            xmlString = xmlString.substring(1);
        }

        // Check if it's actually XML
        if (!xmlString.startsWith("<") || !xmlString.endsWith(">")) {
            System.out.println("Not a valid XML message: " + xmlString);
            return null;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();

            // First try to get <reply> tag (preferred format)
            NodeList replyNodes = doc.getElementsByTagName("reply");
            if (replyNodes.getLength() > 0) {
                return replyNodes.item(0).getTextContent();
            }

            // Fall back to status attribute if needed
            if (root.hasAttribute("status")) {
                return root.getAttribute("status");
            }

            return null; // no reply tag or status attribute found
        } catch (Exception e) {
            System.out.println("Error parsing XML: " + xmlString);
            System.out.println("Error message: " + e.getMessage());
            return null;
        }
    }

    // New method to get the message body from server responses
    public String getResponseBody(String xmlString) throws Exception {
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

            NodeList bodyNodes = doc.getElementsByTagName("body");
            if (bodyNodes.getLength() > 0) {
                return bodyNodes.item(0).getTextContent();
            }

            return null;
        } catch (Exception e) {
            System.out.println("Error parsing XML body: " + xmlString);
            return null;
        }
    }

    // Enhanced method to handle complete server response parsing
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

    // Add these methods to your UserXMLHandler class

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

    public List<String> getGroupChatHistory(String username, String groupName) {
        List<String> messages = new ArrayList<>();
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
                        messages.add(sender + ": " + msg);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }

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

    // Inner class to hold server response data
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

            // Root <user>
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

    public List<String> getUserGroups(String username) {
        List<String> groups = new ArrayList<>();
        try {
            File file = new File("src/main/resources/XMLFiles/" + username + "Account.xml");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            NodeList groupList = doc.getElementsByTagName("group");
            for (int i = 0; i < groupList.getLength(); i++) {
                Element group = (Element) groupList.item(i);
                NodeList members = group.getElementsByTagName("member");

                for (int j = 0; j < members.getLength(); j++) {
                    Element member = (Element) members.item(j);
                    if (member.getAttribute("username").equals(username)) {
                        groups.add(group.getAttribute("name"));
                        break;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return groups;
    }
    public List<String> getChatHistory(String username, String friendName) {
        List<String> messages = new ArrayList<>();
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
                        messages.add(sender + ": " + msg);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }
    public String getBody(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
        doc.getDocumentElement().normalize();

        NodeList bodyNodes = doc.getElementsByTagName("body");
        if (bodyNodes.getLength() > 0) {
            return bodyNodes.item(0).getTextContent();
        }
        return null;
    }

    public void saveUpdatedAccountXML(String username, String base64Content) {
        try {
            byte[] xmlContent = Base64.getDecoder().decode(base64Content);
            String filePath = "src/main/resources/XMLFiles/" + username + "Account.xml";

            FileOutputStream fos = new FileOutputStream(filePath);
            fos.write(xmlContent);
            fos.close();

            System.out.println("Updated " + username + "Account.xml received from server");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}