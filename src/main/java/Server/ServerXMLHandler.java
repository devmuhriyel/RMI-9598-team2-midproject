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


    public String sendMessage(String xmlString) throws Exception {
        return "";
    }

    public String readMessage(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
        doc.getDocumentElement().normalize();
        String type = doc.getElementsByTagName("type").item(0).getTextContent();
        String reply = "";
        String[] content = doc.getElementsByTagName("content").item(0).getTextContent().split("!\\[]!");;
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
                // Get username and password either from content or XML tags
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
                        reply = saved ? "REGISTER_SUCCESS" : "REGISTER_FAILED";
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
                //Parse the xml string
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

                handleGroupMessage(groupSender, targetGroup, groupMessage);
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
        }
        return reply;
    }

    private void handleGroupMessage(String sender, String groupName, String message) {
        try {
            List<String> groupMembers = getGroupMembers(groupName);
            for (String member : groupMembers) {
                updateGroupXML(member, sender, groupName, message);
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
                    // Add group creator
                    String creator = group.getAttribute("creator");
                    if (creator != null && !creator.isEmpty()) {
                        members.add(creator);
                    }

                    // Add all members
                    NodeList memberList = group.getElementsByTagName("member");
                    for (int j = 0; j < memberList.getLength(); j++) {
                        Element member = (Element) memberList.item(j);
                        String memberName = member.getAttribute("username");
                        if (!members.contains(memberName)) { // Avoid duplicates
                            members.add(memberName);
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting group members: " + e.getMessage());
            e.printStackTrace();
        }
        return members;
    }

    public void updateGroupXML(String owner, String sender, String groupName, String msg) throws IOException, SAXException, ParserConfigurationException, TransformerException {
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

        // Find or create <group> element for this group
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

        // If group doesn't exist in user's XML, create it
        if (targetGroup == null) {
            targetGroup = doc.createElement("group");

            Element groupNameElement = doc.createElement("groupname");
            groupNameElement.appendChild(doc.createTextNode(groupName));
            targetGroup.appendChild(groupNameElement);

            // Add other group members (optional - for reference)
            List<String> members = getGroupMembers(groupName);
            for (String member : members) {
                if (!member.equals(owner)) { // Don't add self
                    Element memberElement = doc.createElement("username");
                    memberElement.appendChild(doc.createTextNode(member));
                    targetGroup.appendChild(memberElement);
                }
            }

            root.appendChild(targetGroup);
        }

        // Find or create <chathistory> in the group
        Element chatHistory = null;
        NodeList chList = targetGroup.getElementsByTagName("chathistory");
        if (chList.getLength() > 0) {
            chatHistory = (Element) chList.item(0);
        } else {
            chatHistory = doc.createElement("chathistory");
            targetGroup.appendChild(chatHistory);
        }

        // Add new chat message
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

        // Determine who the "other person" is from the owner's perspective
        String otherPerson = owner.equals(sender) ? receiver : sender;

        // find or create <friend>
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

        // If friend doesn't exist, create it
        if (targetFriend == null) {
            targetFriend = doc.createElement("friend");
            Element username = doc.createElement("username");
            username.appendChild(doc.createTextNode(otherPerson));
            targetFriend.appendChild(username);
            root.appendChild(targetFriend);
        }

        // find or create <chathistory>
        Element chatHistory = null;
        NodeList chList = targetFriend.getElementsByTagName("chathistory");
        if (chList.getLength() > 0) {
            chatHistory = (Element) chList.item(0);
        } else {
            chatHistory = doc.createElement("chathistory");
            targetFriend.appendChild(chatHistory);
        }

        // add new chat
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
        NodeList list = doc.getElementsByTagName("*"); // all elements
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

    // FRIEND REQUEST FUNCTIONS

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

            // Remove request
            NodeList requestList = doc.getElementsByTagName("request");
            for (int i = 0; i < requestList.getLength(); i++) {
                Element req = (Element) requestList.item(i);
                if (req.getAttribute("from").equals(friend) && req.getAttribute("to").equals(user)) {
                    req.getParentNode().removeChild(req);
                    break;
                }
            }

            // Add to <friends>
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

    // GROUP MANAGEMENT FUNCTIONS

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
            for (int i = 0; i < groupList.getLength(); i++) {
                Element group = (Element) groupList.item(i);
                if (group.getAttribute("name").equals(groupName)) {
                    Element member = doc.createElement("member");
                    member.setAttribute("username", username);
                    group.appendChild(member);
                    saveDocument(doc);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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

    //This forda checker ng Signup
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

    //Pati This forda checker of the Signup
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

            // Double check before inserting
            if (userExists(username)) {
                return false; // already exists
            }

            // Create new <user>
            Element userElement = doc.createElement("user");

            Element uname = doc.createElement("username");
            uname.appendChild(doc.createTextNode(username.trim()));
            userElement.appendChild(uname);

            Element pass = doc.createElement("password");
            pass.appendChild(doc.createTextNode(password.trim()));
            userElement.appendChild(pass);

            root.appendChild(userElement);

            // Save back to file (no ugly whitespace)
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2"); // clean indent
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}

//    public static void main(String[] args) throws Exception {
//        System.out.println(validateUser("bob", "qwerty456"));
//    }
//}
