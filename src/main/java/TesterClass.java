import Common.Objects.AdminObjects.Admin;
import Common.Objects.AdminObjects.AdminAccountsList;
import Common.Objects.ClientObjects.ChatRecords;
import Common.Objects.ClientObjects.Client;
import Common.Objects.ClientObjects.ClientAccountsList;
import Common.Objects.ClientObjects.Message;
import Server.ServerJSONHandler;

import java.util.List;

public class TesterClass {
    public static void main(String[] args) {
        ServerJSONHandler handler = new ServerJSONHandler();
        AdminAccountsList adminAccountsList = handler.readAdminAccountFile("src/main/resources/JSONFiles/AdminAccountsList.json");
        ClientAccountsList clientAccountsList = handler.readClientAccountFile("src/main/resources/JSONFiles/ClientAccountsList.json");
        List<Admin> allAdmins = handler.getAllAdmins(adminAccountsList);
        List<Client> allClients = handler.getAllClients(clientAccountsList);
//        List<ChatRecords> chats = handler.getAllChatHistoryOfUser(clientAccountsList, "alice");

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
//
//        if (chats != null) {
//            for (ChatRecords chat : chats) {
//                if(chat.getMessages() != null) {
//                    for (Message message : chat.getMessages()) {
//                        System.out.println("Sender: " + message.getSender());
//                        System.out.println("Receiver: " + message.getReceiver());
//                        System.out.println("Content: " + message.getContent());
//                    }
//                }
//            }
//        }
    }
}
