package AdminMVC;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.List;

public class AdminXMLHandler {

    public String createAdminLoginRequest(String username, String password) {
        return "<Request><type>ADMIN_LOGIN</type><content>" +
                username + "![]!" + password + "</content></Request>";
    }

    public String createDeleteUserRequest(String username) {
        return "<Request><type>DELETE_USER</type><content>" + username + "</content></Request>";
    }

    public String createDeleteGroupRequest(String groupName, String owner) {
        return "<Request><type>DELETE_GROUP</type><content>" +
                groupName + "![]!" + owner + "</content></Request>";
    }

    public String createChangePasswordRequest(String username, String newPassword) {
        return "<Request><type>CHANGE_USER_PASSWORD</type><content>" +
                username + "![]!" + newPassword + "</content></Request>";
    }

    public String createSearchUsersRequest(String keyword) {
        return "<Request><type>SEARCH_USERS</type><content>" + keyword + "</content></Request>";
    }

    public String createSearchGroupsRequest(String keyword) {
        return "<Request><type>SEARCH_GROUPS</type><content>" + keyword + "</content></Request>";
    }

    public String createNewUserRequest(String username, String password) {
        return "<Request><type>CREATE_USER</type><content>" +
                username + "![]!" + password + "</content></Request>";
    }

    public String createGetActiveUsersRequest() {
        return "<Request><type>GET_ACTIVE_USERS</type><content></content></Request>";
    }

    public String createGetCommunityMessagesRequest() {
        return "<Request><type>GET_COMMUNITY_MESSAGES</type><content></content></Request>";
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
            Document doc = builder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
            doc.getDocumentElement().normalize();

            NodeList replyNodes = doc.getElementsByTagName("reply");
            if (replyNodes.getLength() > 0) {
                return replyNodes.item(0).getTextContent();
            }

            return null;
        } catch (Exception e) {
            System.err.println("Error parsing XML reply: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public String getResponseBody(String xmlString) {
        try {
            xmlString = xmlString.trim();
            if (xmlString.startsWith("\uFEFF")) {
                xmlString = xmlString.substring(1);
            }

            if (!xmlString.startsWith("<") || !xmlString.endsWith(">")) {
                System.out.println("Not valid XML format: " + xmlString.substring(0, Math.min(100, xmlString.length())));
                return null;
            }

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
            System.err.println("Error parsing XML body: " + e.getMessage());
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

    public boolean validateAdminCredentials(String username, String password) {
        return username != null && !username.trim().isEmpty() &&
                password != null && !password.trim().isEmpty() &&
                !username.contains("![]!") && !password.contains("![]!");
    }

    public boolean validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        if (username.length() < 3) {
            return false;
        }

        String forbiddenChars = "#$%^?\"}|_/\\{}";
        for (char c : forbiddenChars.toCharArray()) {
            if (username.contains(String.valueOf(c))) {
                return false;
            }
        }

        if (username.contains(" ")) {
            return false;
        }

        if (username.contains("![]!")) {
            return false;
        }

        return true;
    }

    public boolean validateGroupName(String groupName) {
        return groupName != null && !groupName.trim().isEmpty() &&
                !groupName.contains("![]!") && groupName.length() >= 2;
    }

    public boolean validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }

        if (password.length() < 4) {
            return false;
        }

        String forbiddenChars = "#$%^?\"}|_/\\{}";
        for (char c : forbiddenChars.toCharArray()) {
            if (password.contains(String.valueOf(c))) {
                return false;
            }
        }

        if (password.contains(" ")) {
            return false;
        }

        if (password.contains("![]!")) {
            return false;
        }

        return true;
    }

    public String createGetAllUsersWithStatusRequest() {
        return "<Request><type>GET_ALL_USERS_WITH_STATUS</type><content></content></Request>";
    }

    public String createGetActiveUsersOnlyRequest() {
        return "<Request><type>GET_ACTIVE_USERS_ONLY</type><content></content></Request>";
    }

    public String createGetAllGroupsEnhancedRequest() {
        return "<Request><type>GET_ALL_GROUPS_ENHANCED</type><content></content></Request>";
    }

    public String createNewGroupWithMembersRequest(String groupName, String creator, String membersStr) {
        return "<Request><type>CREATE_GROUP_WITH_MEMBERS</type><content>" +
                groupName + "![]!" + creator + "![]!" + membersStr + "</content></Request>";
    }

    public String createAddMembersToGroupRequest(String groupName, String membersStr) {
        return "<Request><type>ADD_MEMBERS_TO_GROUP</type><content>" +
                groupName + "![]!" + membersStr + "</content></Request>";
    }
}