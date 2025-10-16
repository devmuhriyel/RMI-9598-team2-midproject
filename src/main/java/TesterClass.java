import Server.ServerClasses.*;
import Server.ServerJSONHandler;

import java.util.List;

public class TesterClass {
    public static void main(String[] args) {
        ServerJSONHandler handler = new ServerJSONHandler();
        AdminAccounts adminAccounts = handler.readAdminAccountFile("src/main/resources/JSONFiles/AdminAccounts.json");
        ClientAccounts clientAccounts = handler.readClientAccountFile("src/main/resources/JSONFiles/ClientAccounts.json");
        List<Admin> allAdmins = handler.getAllAdmins(adminAccounts);
        List<Client> allClients = handler.getAllClients(clientAccounts);
        List<ChatHistory> chats = handler.getAllChatHistoryOfUser(clientAccounts, "alice");

        if (allAdmins != null) {
            for (Admin admin : allAdmins) {
                System.out.println("Username: " + admin.getUsername());
                System.out.println("Password: " + admin.getPassword());
                System.out.println("Code: " + admin.getVerificationCode());
                System.out.println("---");
            }
        }
        if (allClients != null) {
            for (Client client : allClients) {
                System.out.println("Username: " + client.getUsername());
                System.out.println("Password: " + client.getPassword());
                System.out.println("Code: " + client.getVerificationCode());
                System.out.println("---");
            }
        }

        if (chats != null) {
            for (ChatHistory chat : chats) {
                if(chat.getMessages() != null) {
                    for (Message message : chat.getMessages()) {
                        System.out.println("Sender: " + message.getSender());
                        System.out.println("Receiver: " + message.getReceiver());
                        System.out.println("Content: " + message.getContent());
                    }
                }
            }
        }
    }
}
