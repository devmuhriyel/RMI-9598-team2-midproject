package Server;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.File;
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
        String[] content = doc.getElementsByTagName("content").item(0).getTextContent().split("waw");;
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
            case "REGISTER":
                File file = new File(FILE_PATH);
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
                Element uname = doc.createElement("username");
                uname.appendChild(doc.createTextNode(content[0]));
                user.appendChild(uname);
                Element pass = doc.createElement("password");
                pass.appendChild(doc.createTextNode(content[1]));
                user.appendChild(pass);
                root.appendChild(user);
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.transform(new DOMSource(doc), new StreamResult(file));
                reply = "REGISTER_SUCCESS";
                break;
            case "SEND_MESSAGE":
                reply = "SEND_MESSAGE";
                break;
            case "CREATE_GROUP":
                reply = "CREATE_GROUP";
                break;
            case "JOIN_GROUP":
                reply = "JOIN_GROUP";
                break;
            case "LIST_GROUPS":
                reply = "LIST_GROUPS";
                break;
            case "SEND_GROUP_MESSAGE":
                reply = "SEND_GROUP_MESSAGE";
                break;
        }
        return reply;
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

}

//    public static void main(String[] args) throws Exception {
//        System.out.println(validateUser("bob", "qwerty456"));
//    }
//}
