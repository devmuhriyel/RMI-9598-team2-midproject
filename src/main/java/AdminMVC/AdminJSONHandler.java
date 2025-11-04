package AdminMVC;

import Common.Objects.AdminObjects.Admin;
import Common.Objects.AdminObjects.AdminAccountsList;
import Common.Objects.ClientObjects.Client;
import Common.Objects.ClientObjects.ClientAccountsList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
        System.out.println("DEBUG: Created JSON request: " + json);
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
                System.out.println("Successfully wrote admin accounts to file");
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
                System.err.println("Client accounts file not found: " + CLIENT_FILE_PATH);
                return null;
            }

            try (FileReader reader = new FileReader(file)) {
                ClientAccountsList accounts = gson.fromJson(reader, ClientAccountsList.class);

                if (accounts == null) {
                    System.err.println("Failed to parse client accounts file");
                    return null;
                }

                System.out.println("Successfully read " +
                        (accounts.getClients() != null ? accounts.getClients().size() : 0) +
                        " client accounts");

                return accounts;
            }
        } catch (IOException e) {
            System.err.println("Error reading client accounts file: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void writeClientAccountFile(ClientAccountsList clientAccountsList) {
        try {
            File file = new File(CLIENT_FILE_PATH);

            // Create parent directories if they don't exist
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(clientAccountsList, writer);
                writer.flush();
                System.out.println("Successfully wrote client accounts to file");
            }
        } catch (IOException e) {
            System.err.println("Error writing client accounts file: " + e.getMessage());
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
            System.out.println("Client deleted: " + username);
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
            System.out.println("Client password changed: " + username);
            return true;
        }

        return false;
    }

    public boolean createClient(String username, String password, String verificationCode) {
        if (!validateUsername(username) || !validatePassword(password)) {
            return false;
        }

        ClientAccountsList clientAccountsList = readClientAccountFile();
        if (clientAccountsList == null) {
            clientAccountsList = new ClientAccountsList();
        }
        if (clientAccountsList.getClients() == null) {
            clientAccountsList.setClients(new java.util.ArrayList<>());
        }

        // Check if user already exists
        for (Client client : clientAccountsList.getClients()) {
            if (client.getUsername().equals(username)) {
                System.err.println("Client already exists: " + username);
                return false;
            }
        }

        // Create new client
        Client newClient = new Client(username, password, verificationCode);
        clientAccountsList.getClients().add(newClient);

        writeClientAccountFile(clientAccountsList);
        System.out.println("Client created: " + username);
        return true;
    }

    // ========== VALIDATION METHODS (UPDATED) ==========

    public boolean validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        // Must be at least 3 characters
        if (username.length() < 3) {
            return false;
        }

        // Only letters and numbers allowed (no symbols, no spaces)
        return username.matches("^[a-zA-Z0-9]+$");
    }

    public boolean validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }

        // Must be at least 4 characters
        if (password.length() < 4) {
            return false;
        }

        // Only letters and numbers allowed (no symbols, no spaces)
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