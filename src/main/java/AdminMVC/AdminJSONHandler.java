package AdminMVC;

import Common.Objects.AdminObjects.Admin;
import Common.Objects.AdminObjects.AdminAccountsList;
import Common.Objects.ClientObjects.Client;
import Common.Objects.ClientObjects.ClientAccountsList;
import Common.Objects.ClientObjects.GroupChat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminJSONHandler {

    private static final String ADMIN_FILE_PATH = "src/main/resources/JSONFiles/AdminAccounts.json";
    private static final String CLIENT_FILE_PATH = "src/main/resources/JSONFiles/ClientAccounts.json";
    private static String currentAdmin;
    private Gson gson;

    public AdminJSONHandler() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public String createLoginJSON(String username, String password) {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", username);
        loginData.put("password", password);
        return gson.toJson(loginData);
    }

    public String createJSONStringRequest(String logType, String msgType, String admin) {
        Map<String, String> request = new HashMap<>();
        request.put("logType", logType);
        request.put("messageType", msgType);
        request.put("admin", admin);
        String json = gson.toJson(request);
        return json;
    }

    // ========== ADMIN ACCOUNT METHODS ==========

    public boolean validateAdminCredentials(String username, String password) {
        try {
            AdminAccountsList adminAccountsList = readAdminAccountFile();
            if (adminAccountsList == null || adminAccountsList.getAdmins() == null) {
                System.err.println("Admin accounts list is null or empty");
                return false;
            }

            List<Admin> admins = adminAccountsList.getAdmins();
            for (Admin admin : admins) {
                if (admin.getUsername().equals(username) && admin.getPassword().equals(password)) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Error validating admin credentials: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public AdminAccountsList readAdminAccountFile() {
        try {
            File file = new File(ADMIN_FILE_PATH);
            if (!file.exists()) {
                System.err.println("Admin accounts file not found: " + ADMIN_FILE_PATH);
                return null;
            }

            try (FileReader reader = new FileReader(file)) {
                AdminAccountsList accounts = gson.fromJson(reader, AdminAccountsList.class);

                if (accounts == null) {
                    System.err.println("Failed to parse admin accounts file");
                    return null;
                }

                return accounts;
            }
        } catch (IOException e) {
            System.err.println("Error reading admin accounts file: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void writeAdminAccountFile(AdminAccountsList adminAccountsList) {
        try {
            File file = new File(ADMIN_FILE_PATH);

            // Create parent directories if they don't exist
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(adminAccountsList, writer);
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("Error writing admin accounts file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Admin getAdminByUsername(String username) {
        AdminAccountsList adminAccountsList = readAdminAccountFile();
        if (adminAccountsList == null || adminAccountsList.getAdmins() == null) {
            return null;
        }

        for (Admin admin : adminAccountsList.getAdmins()) {
            if (admin.getUsername().equals(username)) {
                return admin;
            }
        }
        return null;
    }

    public boolean changeAdminPassword(String username, String newPassword) {
        if (!validatePassword(newPassword)) {
            System.err.println("Invalid password format");
            return false;
        }

        AdminAccountsList adminAccountsList = readAdminAccountFile();
        if (adminAccountsList == null || adminAccountsList.getAdmins() == null) {
            return false;
        }

        boolean found = false;
        for (Admin admin : adminAccountsList.getAdmins()) {
            if (admin.getUsername().equals(username)) {
                admin.setPassword(newPassword);
                found = true;
                break;
            }
        }

        if (found) {
            writeAdminAccountFile(adminAccountsList);
            return true;
        }

        return false;
    }

    // ========== CLIENT ACCOUNT METHODS ==========

    public ClientAccountsList readClientAccountFile() {
        try {
            File file = new File(CLIENT_FILE_PATH);
            if (!file.exists()) {
                System.err.println("✗ Client accounts file not found: " + CLIENT_FILE_PATH);
                return null;
            }

            try (FileReader reader = new FileReader(file)) {
                // Parse JSON with wrapper structure
                JsonObject jsonObject = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();

                ClientAccountsList accounts = new ClientAccountsList();

                if (jsonObject.has("clientAccounts")) {
                    com.google.gson.JsonArray clientsArray = jsonObject.getAsJsonArray("clientAccounts");
                    List<Client> clients = new ArrayList<>();

                    for (com.google.gson.JsonElement element : clientsArray) {
                        Client client = gson.fromJson(element, Client.class);
                        clients.add(client);
                    }

                    accounts.setClients(clients);
                } else {
                    System.err.println("✗ 'clientAccounts' key not found in JSON");
                }

                return accounts;
            }
        } catch (IOException e) {
            System.err.println("✗ Error reading client accounts file: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void writeClientAccountFile(ClientAccountsList clientAccountsList) {
        try {
            File file = new File(CLIENT_FILE_PATH);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (FileWriter writer = new FileWriter(file)) {
                Map<String, Object> wrapper = new HashMap<>();
                wrapper.put("clientAccounts", clientAccountsList.getClients());
                gson.toJson(wrapper, writer);
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("✗ Error writing client accounts file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Client getClientByUsername(String username) {
        ClientAccountsList clientAccountsList = readClientAccountFile();
        if (clientAccountsList == null || clientAccountsList.getClients() == null) {
            return null;
        }

        for (Client client : clientAccountsList.getClients()) {
            if (client.getUsername().equals(username)) {
                return client;
            }
        }
        return null;
    }

    public List<Client> getAllClients() {
        ClientAccountsList clientAccountsList = readClientAccountFile();
        if (clientAccountsList == null || clientAccountsList.getClients() == null) {
            return null;
        }
        return clientAccountsList.getClients();
    }

    public boolean deleteClient(String username) {
        ClientAccountsList clientAccountsList = readClientAccountFile();
        if (clientAccountsList == null || clientAccountsList.getClients() == null) {
            return false;
        }

        boolean removed = clientAccountsList.getClients().removeIf(c -> c.getUsername().equals(username));

        if (removed) {
            writeClientAccountFile(clientAccountsList);
            return true;
        }

        return false;
    }

    public boolean changeClientPassword(String username, String newPassword) {
        if (!validatePassword(newPassword)) {
            System.err.println("Invalid password format");
            return false;
        }

        ClientAccountsList clientAccountsList = readClientAccountFile();
        if (clientAccountsList == null || clientAccountsList.getClients() == null) {
            return false;
        }

        boolean found = false;
        for (Client client : clientAccountsList.getClients()) {
            if (client.getUsername().equals(username)) {
                client.setPassword(newPassword);
                found = true;
                break;
            }
        }

        if (found) {
            writeClientAccountFile(clientAccountsList);
            return true;
        }

        return false;
    }
    public boolean deleteGroup(String groupName) {
        System.out.println(">>> AdminJSONHandler.deleteGroup() called for: " + groupName);

        ClientAccountsList clientAccountsList = readClientAccountFile();
        if (clientAccountsList == null || clientAccountsList.getClients() == null) {
            System.err.println("✗ Failed to read client accounts file");
            return false;
        }

        boolean groupFound = false;
        int totalRemoved = 0;

        // Go through EVERY client and remove this group from their groupChats
        for (Client client : clientAccountsList.getClients()) {
            if (client.getGroupChats() != null) {
                // Count how many groups this client has before removal
                int beforeSize = client.getGroupChats().size();

                // Remove the group if it matches the groupName
                client.getGroupChats().removeIf(group ->
                        group.getGroupName() != null && group.getGroupName().equals(groupName)
                );

                // Check if we actually removed something
                int afterSize = client.getGroupChats().size();
                if (beforeSize > afterSize) {
                    groupFound = true;
                    totalRemoved++;
                    System.out.println("  ✓ Removed group '" + groupName + "' from user: " + client.getUsername());
                }
            }
        }

        if (groupFound) {
            // THIS IS THE KEY PART - Save the changes to the JSON file
            writeClientAccountFile(clientAccountsList);
            System.out.println("✓ SUCCESS: Group '" + groupName + "' deleted from " + totalRemoved + " users and saved to JSON");
            return true;
        } else {
            System.out.println("✗ FAILED: Group '" + groupName + "' not found in any user's group list");
            return false;
        }
    }
    public boolean deleteGroupFromAllUsers(String groupName, String ownerUsername) {
        System.out.println(">>> AdminJSONHandler: deleteGroupFromAllUsers called");
        System.out.println("    Group to delete: " + groupName);
        System.out.println("    Owner: " + ownerUsername);

        ClientAccountsList clientAccountsList = readClientAccountFile();
        if (clientAccountsList == null || clientAccountsList.getClients() == null) {
            System.err.println("✗ Failed to read client accounts file");
            return false;
        }

        boolean groupFound = false;
        int totalRemoved = 0;

        // Iterate through all clients and remove the group from their groupChats
        for (Client client : clientAccountsList.getClients()) {
            if (client.getGroupChats() != null) {
                int initialSize = client.getGroupChats().size();

                // Remove the group if it matches the groupName
                boolean removed = client.getGroupChats().removeIf(group ->
                        group.getGroupName() != null && group.getGroupName().equals(groupName)
                );

                if (removed) {
                    groupFound = true;
                    totalRemoved++;
                    System.out.println("  ✓ Removed group from user: " + client.getUsername());
                }
            }
        }

        if (groupFound) {
            // Write the updated data back to the JSON file
            writeClientAccountFile(clientAccountsList);
            System.out.println("✓ Group '" + groupName + "' deleted from " + totalRemoved + " users");
            return true;
        } else {
            System.out.println("✗ Group '" + groupName + "' not found in any user's group list");
            return false;
        }
    }

    public boolean deleteGroupByID(String groupID) {
        System.out.println(">>> AdminJSONHandler.deleteGroupByID() called for: " + groupID);

        ClientAccountsList clientAccountsList = readClientAccountFile();
        if (clientAccountsList == null || clientAccountsList.getClients() == null) {
            System.err.println("✗ Failed to read client accounts file");
            return false;
        }

        boolean groupFound = false;
        int totalRemoved = 0;

        for (Client client : clientAccountsList.getClients()) {
            if (client.getGroupChats() != null) {
                int beforeSize = client.getGroupChats().size();

                client.getGroupChats().removeIf(group ->
                        group.getGroupID() != null && group.getGroupID().equals(groupID)
                );

                int afterSize = client.getGroupChats().size();
                if (beforeSize > afterSize) {
                    groupFound = true;
                    totalRemoved++;
                    System.out.println("  ✓ Removed group ID '" + groupID + "' from user: " + client.getUsername());
                }
            }
        }

        if (groupFound) {
            writeClientAccountFile(clientAccountsList);
            System.out.println("✓ SUCCESS: Group ID '" + groupID + "' deleted from " + totalRemoved + " users and saved to JSON");
            return true;
        } else {
            System.out.println("✗ FAILED: Group ID '" + groupID + "' not found");
            return false;
        }
    }

    public Map<String, Object> getGroupInfo(String groupName) {
        ClientAccountsList clientAccountsList = readClientAccountFile();
        if (clientAccountsList == null || clientAccountsList.getClients() == null) {
            return null;
        }

        // Find the group in any user's groupChats
        for (Client client : clientAccountsList.getClients()) {
            if (client.getGroupChats() != null) {
                for (GroupChat group : client.getGroupChats()) {
                    if (group.getGroupName() != null && group.getGroupName().equals(groupName)) {
                        Map<String, Object> groupInfo = new HashMap<>();
                        groupInfo.put("groupID", group.getGroupID());
                        groupInfo.put("groupName", group.getGroupName());
                        groupInfo.put("owner", client.getUsername()); // First user found is likely the owner
                        groupInfo.put("members", group.getReceivers());
                        groupInfo.put("memberCount", group.getReceivers() != null ? group.getReceivers().size() : 0);
                        return groupInfo;
                    }
                }
            }
        }

        return null;
    }

    public boolean isGroupOwner(String username, String groupName) {
        ClientAccountsList clientAccountsList = readClientAccountFile();
        if (clientAccountsList == null || clientAccountsList.getClients() == null) {
            return false;
        }

        Client client = getClientByUsername(username);
        if (client == null || client.getGroupChats() == null) {
            return false;
        }

        // Check if this user has the group and is the first to have it (owner)
        for (GroupChat group : client.getGroupChats()) {
            if (group.getGroupName() != null && group.getGroupName().equals(groupName)) {
                // Additional logic: you could check if the user's username matches
                // the group creator stored elsewhere, or use other criteria
                return true;
            }
        }

        return false;
    }

    public boolean createClient(String username, String password, String verificationCode) {
        System.out.println(">>> AdminJSONHandler: createClient called");
        System.out.println("    Username: " + username);
        System.out.println("    Password: " + password);
        System.out.println("    VerificationCode: " + verificationCode);

        if (!validateUsername(username) || !validatePassword(password)) {
            System.err.println("✗ Validation failed in AdminJSONHandler");
            return false;
        }

        ClientAccountsList clientAccountsList = readClientAccountFile();
        if (clientAccountsList == null) {
            System.out.println("Creating new ClientAccountsList");
            clientAccountsList = new ClientAccountsList();
        }
        if (clientAccountsList.getClients() == null) {
            System.out.println("Initializing clients list");
            clientAccountsList.setClients(new java.util.ArrayList<>());
        }

        // Check if user already exists
        for (Client client : clientAccountsList.getClients()) {
            if (client.getUsername().equalsIgnoreCase(username)) {
                System.err.println("✗ Client already exists: " + username);
                return false;
            }
        }

        // Generate verification code if not provided
        if (verificationCode == null || verificationCode.trim().isEmpty()) {
            verificationCode = String.valueOf((int)(Math.random() * 9000) + 1000);
            System.out.println("Generated verification code: " + verificationCode);
        }

        // Create new client with proper initialization
        Client newClient = new Client(username, password, verificationCode);

        // Initialize all collections to prevent null issues
        if (newClient.getFriends() == null) {
            newClient.setFriends(new java.util.ArrayList<>());
        }
        if (newClient.getConversations() == null) {
            newClient.setConversations(new java.util.ArrayList<>());
        }
        if (newClient.getGroupChats() == null) {
            newClient.setGroupChats(new java.util.ArrayList<>());
        }
        if (newClient.getIncomingFriendRequests() == null) {
            newClient.setIncomingFriendRequests(new java.util.ArrayList<>());
        }
        if (newClient.getOutgoingFriendRequests() == null) {
            newClient.setOutgoingFriendRequests(new java.util.ArrayList<>());
        }

        clientAccountsList.getClients().add(newClient);

        System.out.println("Writing to file...");
        writeClientAccountFile(clientAccountsList);
        System.out.println("✓ Client created successfully: " + username);
        return true;
    }

    // ========== VALIDATION METHODS (UPDATED) ==========

    public boolean validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        if (username.length() < 3) {
            return false;
        }

        return username.matches("^[a-zA-Z0-9]+$");
    }

    public boolean validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        if (password.length() < 4) {
            return false;
        }
        return password.matches("^[a-zA-Z0-9]+$");
    }

    // SETTERS AND GETTERS
    public static void setCurrentAdmin(String username) {
        currentAdmin = username;
    }

    public static String getCurrentAdmin() {
        return currentAdmin;
    }

    public static String getAdminFilePath() {
        return ADMIN_FILE_PATH;
    }

    public static String getClientFilePath() {
        return CLIENT_FILE_PATH;
    }
}