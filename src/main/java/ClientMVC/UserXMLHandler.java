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
        String[] content = doc.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");
        ;
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
        }
        return reply;
    }

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