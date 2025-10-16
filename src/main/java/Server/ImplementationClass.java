package Server;

import CommonInterface.AdminInterface;
import CommonInterface.ClientInterface;
import Server.ServerClasses.AdminObjects.Admin;
import Server.ServerClasses.AdminObjects.AdminAccountsList;
import Server.ServerClasses.ClientObjects.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ImplementationClass extends UnicastRemoteObject implements ClientInterface, AdminInterface {
    //VARIABLES
    private ConcurrentHashMap<String, Client> onlineUsers;
    private ConcurrentHashMap<String, Client> allUsers;
    private ConcurrentHashMap<String, Admin> onlineAdmins;
    private ConcurrentHashMap<String, Admin> allAdmins;
    private AdminAccountsList adminAccountsList;
    private ClientAccountsList clientAccountsList;
    private ServerJSONHandler serverJSONHandler;

    //CONSTRUCTORS
    public ImplementationClass() throws RemoteException {
        super();
        serverJSONHandler = new ServerJSONHandler();
        adminAccountsList = serverJSONHandler.readAdminAccountFile("src/main/resources/JSONFiles/AdminAccountsList.json");
        clientAccountsList = serverJSONHandler.readClientAccountFile("src/main/resources/JSONFiles/ClientAccountsList.json");
    }

    //USER/ADMIN LOG IN, SIGN UP, AND OTHER RELATED METHODS
    @Override
    public void signUp(String jsonString) throws RemoteException{

    }

    @Override
    public void logIn(String jsonString) throws RemoteException{

    }

    @Override
    public void logOut(String jsonString) throws RemoteException{

    }

    @Override
    public void changePassword(String jsonString) throws RemoteException{

    }

    @Override
    public void verifyCode(String jsonString) throws RemoteException{

    }

    //USER/ADMIN HOME UI METHODS
    @Override
    public void searchUsers(String jsonString) throws RemoteException{

    }

    @Override
    public void searchGroups(String jsonString) throws RemoteException{

    }

    @Override
    public void deleteGroup(String jsonString) throws RemoteException{

    }

    @Override
    public void getUnreadMessages(String jsonString) throws RemoteException{

    }

    @Override
    public void sendFriendRequest(String jsonString) throws RemoteException{

    }

    @Override
    public void getOnlineFriends(String jsonString) throws RemoteException{

    }

    //USER/ADMIN ACCOUNT SETTINGS UI METHODS
    @Override
    public void changeUsername(String jsonString) throws RemoteException{

    }

    @Override
    public void changePasswordWithOldPassword(String jsonString) throws RemoteException{

    }

    //USER/ADMIN FRIEND REQUESTS UI METHODS
    @Override
    public void cancelFriendRequest(String jsonString) throws RemoteException{

    }

    @Override
    public void acceptFriendRequest(String jsonString) throws RemoteException{

    }

    @Override
    public void rejectFriendRequest(String jsonString) throws RemoteException{

    }

    @Override
    public void removeFromFriendsList(String jsonString) throws RemoteException{

    }

    //USER/ADMIN COMMON CHATTING METHODS
    @Override
    public String getChatHistories(String jsonString) throws RemoteException{
        return "";
    }

    @Override
    public String getChatHistory(String jsonString) throws RemoteException{
        return "";
    }
    @Override
    public String getChatHistoryOfUser(String jsonString) throws RemoteException{
        return "";
    }

    @Override
    public String getChatHistoryOfGroup(String jsonString) throws RemoteException{
        return "";
    }

    @Override
    public String getChatHistoryOfCommunity(String jsonString) throws RemoteException{
        return "";
    }

    //USER/ADMIN PRIVATE CHATTING METHODS
    @Override
    public void sendPrivateMessage(String sender, String receiver, String message, String jsonString) throws RemoteException{
        /*
            To append a new message record to the JSON file:
            A.
                1. Create a Message object using the variables the client sent
                2. Get the Conversation object between the sender and the receiver
                3. Get the LIST of Message objects in the Conversation objects of the sender and receiver
                4. Append the newly created Message object to the list of Message objects of the sender and receiver
                5. Set the updated LIST of Message objects in the Conversation object of the sender and receiver
                6. Append the changes to the JSON snippets of the sender and receiver
            B.
                7. Extract the JSON string for logType, messageType, and sender
                8. Create a new Requests object with the extracted information
                9. Get the LogsList object
                10. Get the list of Requests objects from the LogsList object
                11. Append the newly created Requests object to the LIST of Request objects
                12. Set the updated LIST of Requests objects to the LogsList object

        */
        Message msg = new Message(sender, receiver, message);
        Conversation receiverConvo = serverJSONHandler.getConversationWithSender(clientAccountsList, receiver, sender);
        Conversation senderConvo = serverJSONHandler.getConversationWithRecipient(clientAccountsList, sender, receiver);
        List<Message> receiverHistory = receiverConvo.getMessages();
        List<Message> senderHistory= senderConvo.getMessages();
        receiverHistory.add(msg);
        senderHistory.add(msg);
        receiverConvo.setMessages(receiverHistory);
        senderConvo.setMessages(senderHistory);
        serverJSONHandler.updateUserConversation(clientAccountsList, sender, receiver, senderHistory);
        serverJSONHandler.updateUserConversation(clientAccountsList, receiver, sender, receiverHistory);
        serverJSONHandler.writeClientAccountFile("src/main/resources/JSONFiles/ClientAccountsList.json", clientAccountsList);
    }

    @Override
    public void sendPrivateFile(String jsonString) throws RemoteException{

    }

    //USER/ADMIN GROUP CHATTING METHODS
    @Override
    public void sendGroupMessage(String jsonString) throws RemoteException{

    }

    @Override
    public void sendGroupFile(String jsonString) throws RemoteException{

    }

    @Override
    public void createGroupChat(String jsonString) throws RemoteException{

    }

    @Override
    public void addUserToGroup(String jsonString) throws RemoteException{

    }

    @Override
    public void removeUserFromGroup(String jsonString) throws RemoteException{

    }

    //USER/ADMIN COMMUNITY CHATTING METHODS
    @Override
    public void sendCommunityMessage(String jsonString) throws RemoteException{

    }
}