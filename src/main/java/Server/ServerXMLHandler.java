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
        }
        return reply;
    }

    //For the Friend Function
    // Add friend
    public boolean addFriend(String user, String friend) {
        try {
            Document doc = loadDocument();
            NodeList users = doc.getElementsByTagName("user");
            for (int i = 0; i < users.getLength(); i++) {
                Element u = (Element) users.item(i);
                if (u.getAttribute("username").equals(user)) {
                    Element friendsTag = (Element) u.getElementsByTagName("friends").item(0);
                    Element newFriend = doc.createElement("friend");
                    newFriend.setTextContent(friend);
                    friendsTag.appendChild(newFriend);
                    saveDocument(doc);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Remove friend
    public boolean removeFriend(String user, String friend) {
        try {
            Document doc = loadDocument();
            NodeList users = doc.getElementsByTagName("user");
            for (int i = 0; i < users.getLength(); i++) {
                Element u = (Element) users.item(i);
                if (u.getAttribute("username").equals(user)) {
                    NodeList friends = u.getElementsByTagName("friend");
                    for (int j = 0; j < friends.getLength(); j++) {
                        Element f = (Element) friends.item(j);
                        if (f.getTextContent().equals(friend)) {
                            f.getParentNode().removeChild(f);
                            saveDocument(doc);
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

    // List friends
    public List<String> listFriends(String user) {
        List<String> friendsList = new ArrayList<>();
        try {
            Document doc = loadDocument();
            NodeList users = doc.getElementsByTagName("user");
            for (int i = 0; i < users.getLength(); i++) {
                Element u = (Element) users.item(i);
                if (u.getAttribute("username").equals(user)) {
                    NodeList friends = u.getElementsByTagName("friend");
                    for (int j = 0; j < friends.getLength(); j++) {
                        friendsList.add(friends.item(j).getTextContent());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return friendsList;
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
}

//    public static void main(String[] args) throws Exception {
//        System.out.println(validateUser("bob", "qwerty456"));
//    }
//}
