package AdminMVC;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.List;

public class AdminXMLHandler {

    // ========== ADMIN REQUEST BUILDERS ==========

    /**
     * Creates an admin login request
     */
    public String createAdminLoginRequest(String username, String password) {
        return "<Request><type>ADMIN_LOGIN</type><content>" +
                username + "![]!" + password + "</content></Request>";
    }

    /**
     * Creates a request to get all users
     */
    public String createGetAllUsersRequest() {
        return "<Request><type>GET_ALL_USERS</type><content></content></Request>";
    }

    /**
     * Creates a request to get all groups
     */
    public String createGetAllGroupsRequest() {
        return "<Request><type>GET_ALL_GROUPS</type><content></content></Request>";
    }

    /**
     * Creates a request to delete a user
     */
    public String createDeleteUserRequest(String username) {
        return "<Request><type>DELETE_USER</type><content>" + username + "</content></Request>";
    }

    /**
     * Creates a request to delete a group
     */
    public String createDeleteGroupRequest(String groupName, String owner) {
        return "<Request><type>DELETE_GROUP</type><content>" +
                groupName + "![]!" + owner + "</content></Request>";
    }

    /**
     * Creates a request to change user password
     */
    public String createChangePasswordRequest(String username, String newPassword) {
        return "<Request><type>CHANGE_USER_PASSWORD</type><content>" +
                username + "![]!" + newPassword + "</content></Request>";
    }

    /**
     * Creates a request to get user statistics
     */
    public String createGetUserStatsRequest(String username) {
        return "<Request><type>GET_USER_STATS</type><content>" + username + "</content></Request>";
    }

    /**
     * Creates a request to search users
     */
    public String createSearchUsersRequest(String keyword) {
        return "<Request><type>SEARCH_USERS</type><content>" + keyword + "</content></Request>";
    }

    /**
     * Creates a request to search groups
     */
    public String createSearchGroupsRequest(String keyword) {
        return "<Request><type>SEARCH_GROUPS</type><content>" + keyword + "</content></Request>";
    }

    /**
     * Creates a request to create a new user account
     */
    public String createNewUserRequest(String username, String password) {
        return "<Request><type>CREATE_USER</type><content>" +
                username + "![]!" + password + "</content></Request>";
    }

    /**
     * Creates a request to create a new group
     */
    public String createNewGroupRequest(String groupName, String creator) {
        return "<Request><type>CREATE_GROUP_ADMIN</type><content>" +
                groupName + "![]!" + creator + "</content></Request>";
    }

    /**
     * Creates a request to get active users
     */
    public String createGetActiveUsersRequest() {
        return "<Request><type>GET_ACTIVE_USERS</type><content></content></Request>";
    }

    /**
     * Creates a request to get community chat messages
     */
    public String createGetCommunityMessagesRequest() {
        return "<Request><type>GET_COMMUNITY_MESSAGES</type><content></content></Request>";
    }

    // ========== RESPONSE PARSERS ==========

    /**
     * Parses server response to extract reply type
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

    /**
     * Extracts the body content from server response
     */
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

    // ========== UTILITY METHODS ==========

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

    // ========== VALIDATION METHODS ==========

    /**
     * Validates if admin credentials are properly formatted
     */
    public boolean validateAdminCredentials(String username, String password) {
        return username != null && !username.trim().isEmpty() &&
                password != null && !password.trim().isEmpty() &&
                !username.contains("![]!") && !password.contains("![]!");
    }

    /**
     * Validates if username is properly formatted
     */
    public boolean validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        // Check for minimum length
        if (username.length() < 3) {
            return false;
        }

        // Check for forbidden characters: #$%^?"}|_/\{}
        String forbiddenChars = "#$%^?\"}|_/\\{}";
        for (char c : forbiddenChars.toCharArray()) {
            if (username.contains(String.valueOf(c))) {
                return false;
            }
        }

        // Check for spaces
        if (username.contains(" ")) {
            return false;
        }

        // Check for our internal delimiter
        if (username.contains("![]!")) {
            return false;
        }

        return true;
    }

    /**
     * Validates if group name is properly formatted
     */
    public boolean validateGroupName(String groupName) {
        return groupName != null && !groupName.trim().isEmpty() &&
                !groupName.contains("![]!") && groupName.length() >= 2;
    }

    /**
     * Validates if password is properly formatted
     */
    public boolean validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }

        // Check for minimum length
        if (password.length() < 4) {
            return false;
        }

        // Check for forbidden characters: #$%^?"}|_/\{}
        String forbiddenChars = "#$%^?\"}|_/\\{}";
        for (char c : forbiddenChars.toCharArray()) {
            if (password.contains(String.valueOf(c))) {
                return false;
            }
        }

        // Check for spaces
        if (password.contains(" ")) {
            return false;
        }

        // Check for our internal delimiter
        if (password.contains("![]!")) {
            return false;
        }

        return true;
    }

    public String createGetAllUsersWithStatusRequest() {
        return "<Request><type>GET_ALL_USERS_WITH_STATUS</type><content></content></Request>";
    }

    /**
     * Creates a request to get only active users
     */
    public String createGetActiveUsersOnlyRequest() {
        return "<Request><type>GET_ACTIVE_USERS_ONLY</type><content></content></Request>";
    }

    /**
     * Creates a request to get all groups with enhanced information (member count, unique groups)
     */
    public String createGetAllGroupsEnhancedRequest() {
        return "<Request><type>GET_ALL_GROUPS_ENHANCED</type><content></content></Request>";
    }

    /**
     * Creates a request to create a new group with multiple members
     */
    public String createNewGroupWithMembersRequest(String groupName, String creator, String membersStr) {
        return "<Request><type>CREATE_GROUP_WITH_MEMBERS</type><content>" +
                groupName + "![]!" + creator + "![]!" + membersStr + "</content></Request>";
    }

    /**
     * Creates a request to add members to an existing group
     */
    public String createAddMembersToGroupRequest(String groupName, String membersStr) {
        return "<Request><type>ADD_MEMBERS_TO_GROUP</type><content>" +
                groupName + "![]!" + membersStr + "</content></Request>";
    }

    /**
     * Creates a request for password reset (initiated by user)
     */
    public String createPasswordResetRequest(String username) {
        return "<Request><type>REQUEST_PASSWORD_RESET</type><content>" + username + "</content></Request>";
    }

    /**
     * Creates a request to verify password reset code
     */
    public String createVerifyResetCodeRequest(String username, String verificationCode) {
        return "<Request><type>VERIFY_RESET_CODE</type><content>" +
                username + "![]!" + verificationCode + "</content></Request>";
    }

    /**
     * Creates a request to get password reset requests for admin
     */
    public String createGetPasswordResetRequestsRequest() {
        return "<Request><type>GET_PASSWORD_RESET_REQUESTS</type><content></content></Request>";
    }

    /**
     * Validates group creation input including members
     */
    public boolean validateGroupCreation(String groupName, String creator, List<String> members) {
        if (!validateGroupName(groupName) || !validateUsername(creator)) {
            return false;
        }

        if (members != null) {
            for (String member : members) {
                if (!validateUsername(member)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Validates verification code format
     */
    public boolean validateVerificationCode(String code) {
        return code != null && code.matches("\\d{6}");
    }
}