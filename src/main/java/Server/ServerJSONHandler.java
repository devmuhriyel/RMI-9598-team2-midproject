package Server;

import Common.Objects.AdminObjects.Admin;
import Common.Objects.AdminObjects.AdminAccountsList;
import Common.Objects.ClientObjects.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.List;

public class ServerJSONHandler {
    //VARIABLES
    private Gson gsonObject = new Gson();

    //CONSTRUCTORS
    public ServerJSONHandler() {
        // Use pretty printing for readable JSON
        this.gsonObject = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    //READER METHODS

    public ClientAccountsList readClientAccountFile(String fPath){
        FileReader jsonReader;
        ClientAccountsList accounts = null;
        try{
            File file = new File(fPath);
            if (!file.exists()) {
                System.err.println("ERROR: File does not exist: " + fPath);
                System.err.println("Absolute path: " + file.getAbsolutePath());
                return null;
            }
            jsonReader = new FileReader(fPath);
            accounts = gsonObject.fromJson(jsonReader, ClientAccountsList.class);

            if (accounts == null) {
                System.err.println("ERROR: Gson returned null for ClientAccountsList");
            } else if (accounts.getClients() == null) {
                System.err.println("ERROR: ClientAccountsList.getClients() is null");
            } else {
                System.out.println("SUCCESS: Loaded " + accounts.getClients().size() + " clients");
            }
            jsonReader.close();

        } catch (FileNotFoundException e) {
            System.err.println("ERROR: File not found: " + fPath);
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("ERROR: Exception while reading client accounts");
            e.printStackTrace();
        }
        return accounts;
    }

    public AdminAccountsList readAdminAccountFile(String fPath){
        FileReader jsonReader;
        AdminAccountsList accounts = null;
        try{
            File file = new File(fPath);

            if (!file.exists()) {
                System.err.println("ERROR: File does not exist: " + fPath);
                System.err.println("Absolute path: " + file.getAbsolutePath());
                return null;
            }
            jsonReader = new FileReader(fPath);
            accounts = gsonObject.fromJson(jsonReader, AdminAccountsList.class);

            if (accounts == null) {
                System.err.println("ERROR: Gson returned null for AdminAccountsList");
            } else if (accounts.getAdmins() == null) {
                System.err.println("ERROR: AdminAccountsList.getAdmins() is null");
            } else {
                System.out.println("SUCCESS: Loaded " + accounts.getAdmins().size() + " admins");
            }

            jsonReader.close();

        } catch (FileNotFoundException e) {
            System.err.println("ERROR: File not found: " + fPath);
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("ERROR: Exception while reading admin accounts");
            e.printStackTrace();
        }
        return accounts;
    }

    //UPDATE METHODS
    public void writeClientAccountFile(String fPath, ClientAccountsList accounts){
        try {
            File file = new File(fPath);

            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(accounts, writer);
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("ERROR writing client accounts to file: " + fPath);
            System.err.println("Absolute path attempted: " + new File(fPath).getAbsolutePath());
            e.printStackTrace();
        }
    }

    //STATUS TRACKING METHODS
    public List<Admin> getAllAdmins(AdminAccountsList adminAccountsList){
        if(adminAccountsList != null){
            return adminAccountsList.getAdmins();
        }
        return null;
    }

    public List<Client> getAllClients(ClientAccountsList clientAccountsList){
        if(clientAccountsList != null){
            return clientAccountsList.getClients();
        }
        return null;
    }

    //CHATTING METHODS
    public Conversation getConversation(ClientAccountsList accountsList, String sender, String recipient){
        List<Conversation> conversations = getAllConversationsOfUser(accountsList, sender);
        if(conversations != null){
            for(Conversation conversation : conversations){
                if(conversation.getRecipient().equals(recipient)){
                    return conversation;
                }
            }
        }
        return null;
    }

    public List<Conversation> getAllConversationsOfUser(ClientAccountsList clientAccountsList, String user){
        if(clientAccountsList != null){
            for(Client c: clientAccountsList.getClients()){
                if(c.getUsername().equals(user)){
                    return c.getConversations();
                }
            }
        }
        return null;
    }

    //Helper Methods For Updating Files
    public String createConversationHashMapEntryString(String user1, String user2){
        if (user1.compareTo(user2) > 0) {
            return user1 + ":" + user2;
        } else {
            return user2 + ":" + user1;
        }
    }

    public String createJSONStringResponse(String logType, String msgType, String receiver){
        return "{ \\n \" logType \": \"" + logType + "\", \n \" messageType \": \"" + msgType + "\", \n \" receiver \": \n " + receiver + " \n}";
    }
}
