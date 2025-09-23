package AdminMVC;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.List;

public class AdminXMLHandler {

    // ========== ADMIN REQUEST BUILDERS ==========

    /**
     * Creates a request to validate verification code and change password
     */
    public String createAdminPasswordChangeWithVerification(String username, String verificationCode, String newPassword) {
        return "<Request><type>ADMIN_VERIFY_AND_CHANGE_PASSWORD</type><content>" +
                username + "![]!" + verificationCode + "![]!" + newPassword + "</content></Request>";
    }

    public String createSendCommunityMessageRequest(String sender, String message) {
        return "<Request><type>SEND_COMMUNITY_MESSAGE</type><content>" +
                sender + "![]!" + message + "</content></Request>";
    }

    /**
     * Creates a request to reject a password reset request
     */
    public String createRejectPasswordResetRequest(String username) {
        return "<Request><type>REJECT_PASSWORD_RESET</type><content>" + username + "</content></Request>";
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
        return "<Request><type>REQUEST_PASSWORD_RESET_WITH_CODE</type><content>" +
                username + "![]!" + verificationCode + "</content></Request>";
    }

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
     * Enhanced notification result for password reset validation
     */
    public static class PasswordResetValidationData {
        private final String username;
        private final String verificationCode;
        private final boolean isValid;
        private final String message;

        public PasswordResetValidationData(String username, String verificationCode, boolean isValid, String message) {
            this.username = username;
            this.verificationCode = verificationCode;
            this.isValid = isValid;
            this.message = message;
        }
        public String getUsername() {
            return username;
        }
        public String getVerificationCode() {
            return verificationCode;
        }
        public boolean isValid() {
            return isValid;
        }
        public String getMessage() {
            return message;
        }
    }

    /**
     * Parses password reset notification from server
     * Extracts username and verification code from server notification
     */
    public PasswordResetNotification parsePasswordResetNotification(String xmlString) {
        try {
            xmlString = xmlString.trim();
            if (xmlString.startsWith("\uFEFF")) {
                xmlString = xmlString.substring(1);
            }

            if (!xmlString.startsWith("<") || !xmlString.endsWith(">")) {
                System.out.println("Not a valid XML notification: " + xmlString.substring(0, Math.min(100, xmlString.length())));
                return null;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
            doc.getDocumentElement().normalize();

            NodeList bodyNodes = doc.getElementsByTagName("body");
            if (bodyNodes.getLength() > 0) {
                String bodyContent = bodyNodes.item(0).getTextContent();
                String[] parts = bodyContent.split("!\\[\\]!");

                if (parts.length >= 2) {
                    return new PasswordResetNotification(parts[0], parts[1]);
                } else if (parts.length == 1) {
                    // For VERIFICATION_CODE_VALID responses, only username is provided
                    return new PasswordResetNotification(parts[0], null);
                }
            }

            return null;
        } catch (Exception e) {
            System.err.println("Error parsing password reset notification: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    /**
     * Data class for password reset notifications
     */
    public static class PasswordResetNotification {
        private final String username;
        private final String verificationCode;

        public PasswordResetNotification(String username, String verificationCode) {
            this.username = username;
            this.verificationCode = verificationCode;
        }

        public String getUsername() {
            return username;
        }

        public String getVerificationCode() {
            return verificationCode;
        }

        @Override
        public String toString() {
            return "PasswordResetNotification{" +
                    "username='" + username + '\'' +
                    ", verificationCode='" + verificationCode + '\'' +
                    '}';
        }
    }


    /**
     * Parses verification code valid notification from server
     * Used when user's verification code is validated
     */
    public String parseVerificationCodeValidNotification(String xmlString) {
        try {
            String bodyContent = getResponseBody(xmlString);
            if (bodyContent != null && !bodyContent.trim().isEmpty()) {
                return bodyContent.trim();
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error parsing verification code valid notification: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parses password change completed notification from server
     * Used when admin completes a password change
     */
    public PasswordChangeCompletedNotification parsePasswordChangeCompletedNotification(String xmlString) {
        try {
            String bodyContent = getResponseBody(xmlString);
            if (bodyContent != null && !bodyContent.trim().isEmpty()) {
                // Extract username and success message from the notification
                if (bodyContent.contains("Password changed for ")) {
                    String username = bodyContent.replace("Password changed for ", "").trim();
                    return new PasswordChangeCompletedNotification(username, "Password successfully changed");
                } else {
                    return new PasswordChangeCompletedNotification(null, bodyContent);
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error parsing password change completed notification: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Data class for password change completed notifications
     */
    public static class PasswordChangeCompletedNotification {
        private final String username;
        private final String message;

        public PasswordChangeCompletedNotification(String username, String message) {
            this.username = username;
            this.message = message;
        }

        public String getUsername() {
            return username;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "PasswordChangeCompletedNotification{" +
                    "username='" + username + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

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

    /**
     * Creates a request to get password reset requests for admin
     */
    public String createGetPasswordResetRequestsRequest() {
        return "<Request><type>GET_PASSWORD_RESET_REQUESTS</type><content></content></Request>";
    }
    /**
     * Checks if the XML response is a password reset request notification
     */
    public boolean isPasswordResetNotification(String xmlString) {
        String reply = getReply(xmlString);
        return "PASSWORD_RESET_REQUESTED".equals(reply);
    }

    /**
     * Checks if the XML response is a verification code valid notification
     */
    public boolean isVerificationCodeValidNotification(String xmlString) {
        String reply = getReply(xmlString);
        return "VERIFICATION_CODE_VALID".equals(reply);
    }

    /**
     * Checks if the XML response is a password change completed notification
     */
    public boolean isPasswordChangeCompletedNotification(String xmlString) {
        String reply = getReply(xmlString);
        return "PASSWORD_CHANGED".equals(reply) || "PASSWORD_RESET_COMPLETED".equals(reply);
    }

    /**
     * Validates password change notification data
     */
    public boolean validatePasswordChangeNotification(String username, String newPassword) {
        return validateUsername(username) && validatePassword(newPassword);
    }

    public boolean validateCommunityMessage(String message) {
        return message != null && !message.trim().isEmpty() &&
                !message.contains("![]!") && message.length() <= 1000; // Max 1000 characters
    }

    public boolean isCommunityMessageNotification(String xmlString) {
        String reply = getReply(xmlString);
        return "COMMUNITY_MESSAGE".equals(reply);
    }

    public boolean validateAdminCommunityAccess(String adminUsername) {
        return adminUsername != null && !adminUsername.trim().isEmpty() &&
                !adminUsername.contains("![]!") && adminUsername.length() >= 3;
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

    public CommunityMessageNotification parseCommunityMessageNotification(String xmlString) {
        try {
            String body = getResponseBody(xmlString);
            if (body != null && !body.isEmpty()) {
                String[] parts = body.split("!\\[]!", 2);
                if (parts.length >= 2) {
                    return new CommunityMessageNotification(parts[0], parts[1]);
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error parsing community message notification: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    public static class CommunityMessageNotification {
        private final String sender;
        private final String message;

        public CommunityMessageNotification(String sender, String message) {
            this.sender = sender;
            this.message = message;
        }

        public String getSender() {
            return sender;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "CommunityMessageNotification{" +
                    "sender='" + sender + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }


    /**
     * Enhanced method to validate verification code format
     */
    public boolean validateVerificationCode(String code) {
        return code != null && !code.trim().isEmpty();
    }
}