package Server;

import Server.ServerClasses.AdminObjects.Admin;
import Server.ServerClasses.AdminObjects.AdminAccounts;
import Server.ServerClasses.ClientObjects.ChatHistory;
import Server.ServerClasses.ClientObjects.Client;
import Server.ServerClasses.ClientObjects.ClientAccounts;
import Server.ServerClasses.ClientObjects.FriendRequest;
import Server.ServerClasses.LogObjects.LogsList;
import com.google.gson.Gson;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

public class ServerJSONHandler {
    //VARIABLES
    private Gson gsonObject = new Gson();

    //CONSTRUCTORS
    public ServerJSONHandler() {}

    //READER METHODS
    public ClientAccounts readClientAccountFile(String fPath){
        FileReader jsonReader;
        ClientAccounts accounts = null;
        try{
            jsonReader = new FileReader(fPath);
            accounts = gsonObject.fromJson(jsonReader, ClientAccounts.class);
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        }
        return accounts;
    }

    public AdminAccounts readAdminAccountFile(String fPath){
        FileReader jsonReader;
        AdminAccounts accounts = null;
        try{
            jsonReader = new FileReader(fPath);
            accounts = gsonObject.fromJson(jsonReader, AdminAccounts.class);
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        }
        return accounts;
    }

    //GETTER METHODS
    public List<Admin> getAllAdmins(AdminAccounts adminAccounts){
        if(adminAccounts != null){
            return adminAccounts.getAdmins();
        }
        return null;
    }
    public List<Client> getAllClients(ClientAccounts clientAccounts){
        if(clientAccounts != null){
            return clientAccounts.getClients();
        }
        return null;
    }
    public List<ChatHistory> getAllChatHistoryOfUser(ClientAccounts clientAccounts, String username){
        List<Client> accounts = getAllClients(clientAccounts);
        for(Client account: accounts){
            if(account.getUsername().equals(username)){
                return account.getChatHistory();
            }
        }
        return null;
    }
//    public LogsList decodeClientRequest(String jsonString, String fPath){
//        FileReader jsonReader;
//        try{
//            jsonReader = new FileReader(fPath);
//            LogsList logs = gsonObject.fromJson(jsonReader, LogsList.class);
//            if (request == null) {
//
//            }
//        } catch (FileNotFoundException e) {
//            System.out.println("File not found");
//        }
//        List<LogsList> logs = null;
//
//        return logs;
//    }

}
