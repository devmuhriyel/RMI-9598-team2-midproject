package Server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientThread implements Runnable {
    private Socket socket;
    private BufferedReader readMSG;
    private BufferedWriter writeMSG;
    private String name;
    private static final ArrayList<ClientThread> clients = new ArrayList<>();
    private ServerXMLHandler xmlHandler = new ServerXMLHandler();

    public ClientThread(Socket socket) {
        try {
            this.socket = socket;
            this.writeMSG = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.readMSG = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            synchronized (clients) {
                clients.add(this);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String xmlMsg;
            while ((xmlMsg = readMSG.readLine()) != null) {
                System.out.println("Received XML: " + xmlMsg);
                String reply = xmlHandler.readMessage(xmlMsg);

                switch (reply) {
                    case "LOGIN_SUCCESS":
                        this.name = extractUsername(xmlMsg); // store logged-in username
                        sendMessage("<Response><reply>LOGIN_SUCCESS</reply><body>Welcome " + name + "!</body></Response>");
                        broadcast("SERVER: " + name + " has joined the chat.");
                        break;

                    case "LOGIN_FAILED":
                        sendMessage("<Response status=\"LOGIN_FAILED\"><body>Invalid username or password</body></Response>");
                        break;

                    case "REGISTER_SUCCESS":
                        sendMessage("<Response status=\"REGISTER_SUCCESS\"><body>Registration successful</body></Response>");
                        break;

                    case "REGISTER_FAILED":
                        sendMessage("<Response status=\"REGISTER_FAILED\"><body>Registration failed</body></Response>");
                        break;

                    case "SEND_MESSAGE":
                        // Build chat message from xmlMsg
                        String chat = name + ": " + extractBody(xmlMsg);
                        broadcast("<Response status=\"MESSAGE\"><body>" + chat + "</body></Response>");
                        break;

                    case "ADD_FRIEND":
                        String[] addContent = extractContent(xmlMsg);
                        if (xmlHandler.addFriend(addContent[0], addContent[1])) {
                            sendMessage("<Response status=\"ADD_FRIEND_SUCCESS\"><body>" + addContent[1] + " added</body></Response>");
                        } else {
                            sendMessage("<Response status=\"ADD_FRIEND_FAILED\"><body>Failed to add " + addContent[1] + "</body></Response>");
                        }
                        break;

                    case "REMOVE_FRIEND":
                        String[] remContent = extractContent(xmlMsg);
                        if (xmlHandler.removeFriend(remContent[0], remContent[1])) {
                            sendMessage("<Response status=\"REMOVE_FRIEND_SUCCESS\"><body>" + remContent[1] + " removed</body></Response>");
                        } else {
                            sendMessage("<Response status=\"REMOVE_FRIEND_FAILED\"><body>Failed to remove " + remContent[1] + "</body></Response>");
                        }
                        break;

                    case "LIST_FRIENDS":
                        String[] listContent = extractContent(xmlMsg);
                        java.util.List<String> friends = xmlHandler.listFriends(listContent[0]);
                        sendMessage("<Response status=\"FRIEND_LIST\"><body>" + String.join(",", friends) + "</body></Response>");
                        break;

                    default:
                        sendMessage("<Response status=\"ERROR\"><body>Unknown request</body></Response>");
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                logOut();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Broadcasts message to all connected clients
    private void broadcast(String message) {
        synchronized (clients) {
            for (ClientThread c : new ArrayList<>(clients)) {
                try {
                    c.writeMSG.write(message);
                    c.writeMSG.newLine();
                    c.writeMSG.flush();
                } catch (IOException e) {
                    try {
                        c.logOut();
                    } catch (IOException ignored) {}
                }
            }
        }
    }

    // Sends a direct message to this client
    private void sendMessage(String msg) {
        try {
            writeMSG.write(msg.trim());
            writeMSG.newLine();
            writeMSG.flush();
        } catch (IOException e) {
            System.out.println("Failed to send message to " + name);
        }
    }

    // Extract username from <content> (split by "waw")
    private String extractUsername(String xmlMsg) throws Exception {
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document doc = builder.parse(new ByteArrayInputStream(xmlMsg.getBytes("UTF-8")));
        String[] content = doc.getElementsByTagName("content").item(0).getTextContent().split("waw");
        return content[0]; // username
    }

    // Extract message body from <content> (for SEND_MESSAGE)
    private String extractBody(String xmlMsg) throws Exception {
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document doc = builder.parse(new ByteArrayInputStream(xmlMsg.getBytes("UTF-8")));
        String[] content = doc.getElementsByTagName("content").item(0).getTextContent().split("waw");
        return content.length > 1 ? content[1] : ""; // return the message part after "waw"
    }

    // Logout and notify others
    public void logOut() throws IOException {
        synchronized (clients) {
            clients.remove(this);
        }
        if (name != null) {
            broadcast("SERVER: " + this.name + " has disconnected.");
        }
        socket.close();
    }

    //This for the Friend Getting index 0 as requester and then 1index 1 as friend
    private String[] extractContent(String xmlMsg) throws Exception {
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xmlMsg.getBytes("UTF-8")));

        if (doc.getElementsByTagName("content").getLength() > 0) {
            return doc.getElementsByTagName("content").item(0).getTextContent().split("waw");
        } else {
            return doc.getDocumentElement().getTextContent().split("waw");
        }
    }


}
