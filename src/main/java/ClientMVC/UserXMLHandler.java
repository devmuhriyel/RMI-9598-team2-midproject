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
import java.util.ArrayList;
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
                break;
        }
        return reply;
    }

    public String getReply(String xmlString) throws Exception {
        // Clean and validate the XML string
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

            NodeList replyNodes = doc.getElementsByTagName("reply");
            if (replyNodes.getLength() > 0) {
                return replyNodes.item(0).getTextContent();
            }
            return null; // no reply tag found
        } catch (Exception e) {
            System.out.println("Error parsing XML: " + xmlString);
            System.out.println("Error message: " + e.getMessage());
            return null;
        }
    }


    public List<String> getFriends(String username) {
        List<String> friends = new ArrayList<>();
        try {
            File file = new File("src/main/resources/XMLFiles/UserAccount.xml");
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
}