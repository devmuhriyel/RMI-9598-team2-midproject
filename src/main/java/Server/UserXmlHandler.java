package Server;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

public class UserXmlHandler {

    // Temporary location for the XML file.
    private static final String FILE_PATH = "users.xml";

    public static void registerUser(String username, String firstname, String lastname, String password) throws Exception {
        File file = new File(FILE_PATH);

        // Set up the XML builder used for creating or parsing XML
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc;

        if (file.exists()) {
            // If file already exists, load it
            doc = builder.parse(file);
        } else {
            // Otherwise, create XML document with <users> as the root
            doc = builder.newDocument();
            Element root = doc.createElement("users");
            doc.appendChild(root);
        }

        Element root = doc.getDocumentElement();
        Element user = doc.createElement("user");

        // Add <username>
        Element uname = doc.createElement("username");
        uname.appendChild(doc.createTextNode(username));
        user.appendChild(uname);

        // Add <firstname>
        Element fname = doc.createElement("firstname");
        fname.appendChild(doc.createTextNode(firstname));
        user.appendChild(fname);

        // Add <lastname>
        Element lname = doc.createElement("lastname");
        lname.appendChild(doc.createTextNode(lastname));
        user.appendChild(lname);

        // Add <password>
        Element pass = doc.createElement("password");
        pass.appendChild(doc.createTextNode(password));
        user.appendChild(pass);

        // Append the new user to the root
        root.appendChild(user);

        // Save the updated XML document back to the file
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(doc), new StreamResult(file));
    }

    public static boolean validateUser(String username, String password) throws Exception {
        File file = new File(FILE_PATH);

        // If the file does not exist yet, no users are registered
        if (!file.exists()) return false;

        // Load and parse the XML file
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);

        NodeList users = doc.getElementsByTagName("user");

        // Loop through each <user> and check credentials
        for (int i = 0; i < users.getLength(); i++) {
            Node user = users.item(i);
            if (user.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) user;

                String uname = e.getElementsByTagName("username").item(0).getTextContent();
                String pass = e.getElementsByTagName("password").item(0).getTextContent();

                // If both username and password match → login successful
                if (uname.equals(username) && pass.equals(password)) {
                    return true;
                }
            }
        }
        // If no match found, login fails
        return false;
    }
}
