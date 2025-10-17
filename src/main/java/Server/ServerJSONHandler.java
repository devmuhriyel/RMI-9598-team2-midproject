package Server;

import Common.Objects.AdminObjects.Admin;
import Common.Objects.AdminObjects.AdminAccountsList;
import Common.Objects.ClientObjects.*;
import Common.Objects.LogObjects.LogsList;
import com.google.gson.Gson;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ServerJSONHandler {
    //VARIABLES
    private Gson gsonObject = new Gson();

    //CONSTRUCTORS
    public ServerJSONHandler() {}

    //READER METHODS
    public ClientAccountsList readClientAccountFile(String fPath){
        FileReader jsonReader;
        ClientAccountsList accounts = null;
        try{
            jsonReader = new FileReader(fPath);
            accounts = gsonObject.fromJson(jsonReader, ClientAccountsList.class);
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        }
        return accounts;
    }

    public AdminAccountsList readAdminAccountFile(String fPath){
        FileReader jsonReader;
        AdminAccountsList accounts = null;
        try{
            jsonReader = new FileReader(fPath);
            accounts = gsonObject.fromJson(jsonReader, AdminAccountsList.class);
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        }
        return accounts;
    }

    //UPDATE METHODS
    public void writeClientAccountFile(String fPath, ClientAccountsList accounts){
        try(FileWriter writer = new FileWriter(fPath)){
            gsonObject.toJson(accounts, writer);
        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage());
        }
    }

    public LogsList readLogsListFile(String fPath){
        FileReader jsonReader;
        LogsList logsList = null;
        try{
            jsonReader = new FileReader(fPath);
            logsList = gsonObject.fromJson(jsonReader, LogsList.class);
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        }
        return logsList;
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
        List<ChatRecords> chatRecords = getAllConversationsOfUser(accountsList, sender);
        if(chatRecords != null){
            for(ChatRecords chatRecord : chatRecords){
                List<Conversation> conversations = chatRecord.getMessages();

                if(conversations != null){
                    for(Conversation conversation : conversations){
                        if(conversation.getRecipient().equals(recipient)){
                            return conversation;
                        }
                    }
                }
            }
        }
        return null;
    }

    public List<ChatRecords> getAllConversationsOfUser(ClientAccountsList clientAccountsList, String user){
        if(clientAccountsList != null){
            for(Client c: clientAccountsList.getClients()){
                if(c.getUsername().equals(user)){
                    return c.getChatRecords();
                }
            }
        }
        return null;
    }

    public void updateUserConversationJSON(ClientAccountsList accountsList, String username, String recipient, List<Message> updatedMessages){
        if(accountsList != null){
            for(Client client : accountsList.getClients()){
                if(client.getUsername().equals(username)){
                    List<ChatRecords> chatRecords = client.getChatRecords();
                    if(chatRecords != null){
                        for(ChatRecords chatRecord : chatRecords){
                            List<Conversation> conversations = chatRecord.getMessages();
                            if(conversations != null){
                                for(Conversation conversation : conversations){
                                    if(conversation.getRecipient().equals(recipient)){
                                        conversation.setMessages(updatedMessages);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
//    public Requests decodeClientRequest(LogsList logsList, String jsonString){
//
//        if(logsList == null){
//
//        }
//
//    }

}
