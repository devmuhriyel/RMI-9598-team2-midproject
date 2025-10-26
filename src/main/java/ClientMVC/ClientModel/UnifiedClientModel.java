package ClientMVC.ClientModel;

import ClientMVC.ClientCallbackImplementation;
import ClientMVC.ClientJSONHandler;
import Common.Interfaces.ClientCallbackInterface;
import Common.Objects.ClientObjects.Client;
import Common.Objects.ClientObjects.Conversation;

import Common.Interfaces.ClientInterface;
import java.io.IOException;
import java.util.function.Consumer;
import java.rmi.RemoteException;

import java.io.*;
import java.util.List;

public class UnifiedClientModel {
    private final ClientInterface serverConnection;
    private final ClientCallbackImplementation callback;
    private Client client;
    private String username;
    private ClientJSONHandler jsonHandler;
    public  ClientInterface serverRMI;
    private Consumer<String> friendRequestListener;


    public UnifiedClientModel(ClientInterface serverStub, ClientCallbackImplementation callback) throws IOException {
        this.serverConnection = serverStub;
        this.callback = callback;
        this.jsonHandler = new ClientJSONHandler();
    }
    
    public ClientCallbackImplementation getCallback() {
        return callback;
    }

//    //USER LOG IN, SIGN UP, AND OTHER RELATED METHODS
//    public void signUp(String username, String password, String verificationCode, String jsonString, ClientCallbackInterface clientCallback) throws RemoteException{}
//    public void logIn(String username, String jsonString, ClientCallbackInterface clientCalllback) throws RemoteException{}
//    public void logOut(String username, String jsonString) throws RemoteException{}
//    public void changePassword(String username, String password, String jsonString) throws RemoteException{}
//    public void verifyCode(String username, String jsonString) throws RemoteException{}

    //USER LOG IN, SIGN UP, AND OTHER RELATED METHODS
    public void signUp(String username, String password, String verificationCode, String jsonString, ClientCallbackInterface clientCallback) throws RemoteException {
        try {
            serverConnection.signUp(username, password, verificationCode, jsonString, clientCallback);
        } catch (RemoteException e) {
            System.err.println("Error during signup: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    public void logIn(String username, String password, String jsonString, ClientCallbackInterface clientCallback) throws RemoteException {
        try {
            // Create JSON string with password
            String loginJson = "{\"password\":\"" + password + "\"}";
            serverConnection.logIn(username, loginJson, clientCallback);
        } catch (RemoteException e) {
            System.err.println("Error during login: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    public void logOut(String username, String jsonString) throws RemoteException {
        try {
            serverConnection.logOut(username, jsonString);
        } catch (RemoteException e) {
            System.err.println("Error during logout: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    public void changePassword(String username, String password, String jsonString) throws RemoteException {
        try {
            serverConnection.changePassword(jsonString);
        } catch (RemoteException e) {
            System.err.println("Error during password change: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    public void verifyCode(String username, String jsonString) throws RemoteException {
        try {
            serverConnection.verifyCode(jsonString);
        } catch (RemoteException e) {
            System.err.println("Error during code verification: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    //USER HOME UI METHODS
    public void searchUsers(String jsonString) throws RemoteException{}
    public void searchGroups(String jsonString) throws RemoteException{}
    //public void sendFriendRequest(String jsonString) throws RemoteException{}
    public void getOnlineFriends(String jsonString) throws RemoteException{}
    public void getAllUsers() throws RemoteException{}

    //USER ACCOUNT SETTINGS UI METHODS
    // -=-=-=-=-=Change Username=-=-=-=-=- \\
    public void changeUsername(String newUsername) throws RemoteException {
        String jsonRequest = String.format(
                "{\"currentUsername\":\"%s\",\"newUsername\":\"%s\"}",
                username,
                newUsername
        );

        System.out.println("Model: Sending username change request to server");
        serverConnection.changeUsername(jsonRequest);
    }

    // -=-=-=-=-=Change Password=-=-=-=-=- \\
    public void changePassword(String oldPassword, String newPassword) throws RemoteException {
        String jsonRequest = String.format(
                "{\"username\":\"%s\",\"oldPassword\":\"%s\",\"newPassword\":\"%s\"}",
                username,
                oldPassword,
                newPassword
        );

        System.out.println("Model: Sending password change request to server");
        serverConnection.changePasswordWithOldPassword(jsonRequest);
    }
    //USER FRIEND REQUESTS UI METHODS
    public void sendFriendRequest(String jsonString) throws RemoteException{}
    public void cancelFriendRequest(String jsonString) throws RemoteException{}
    //public void acceptFriendRequest(String jsonString) throws RemoteException{}
    public void rejectFriendRequest(String jsonString) throws RemoteException{}
    public void removeFromFriendsList(String jsonString) throws RemoteException{}
    public void getFriendRequests() throws RemoteException{};

    // -=-=-=-=-=Accept Friend Requests=-=-=-=-=- \\
    public void acceptFriendRequest(String jsonString){
        try {
            String jsonPayload = "{\"requester\":\"" + jsonString + "\", \"target\":\"" + username + "\"}";
            serverRMI.acceptFriendRequest(jsonPayload);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //USER COMMON CHATTING METHODS
    public List<Conversation> getChatHistories(String jsonString, String sender) throws RemoteException{
        return serverConnection.getChatHistories(jsonString, sender);
    }
    public Conversation getChatHistoryWith(String jsonString, String sender, String receiver) throws RemoteException{
        return serverConnection.getChatHistoryWith(jsonString, sender, receiver);
    }
    public void getUnreadMessages(String jsonString) throws RemoteException{}

    //USER PRIVATE CHATTING METHODS
    public void sendPrivateMessage(String sender, String receiver, String message, String jsonString) throws RemoteException{
        serverConnection.sendPrivateMessage(sender, receiver, message, jsonString);
    }
    public void sendPrivateFile(String jsonString) throws RemoteException{}

    //USER GROUP CHATTING METHODS
    //public void sendGroupMessage(String jsonString) throws RemoteException{}
    //public void sendGroupFile(String jsonString) throws RemoteException{}
    public void createGroupChat(String jsonString) throws RemoteException{}
    public void addUserToGroup(String jsonString) throws RemoteException{}
    public void removeUserFromGroup(String jsonString) throws RemoteException{}

    //USER COMMUNITY CHATTING METHODS
    //public void sendCommunityMessage(String jsonString) throws RemoteException{}

    // GETTERS
    public String getUsername() {
        return username;
    }

    //HELPER METHODS
    public String createJSONStringRequest(String logType, String msgContent, String sender){
        return jsonHandler.createJSONStringRequest(logType, msgContent, sender);
    }
}