package ClientMVC.ClientModel;

import ClientMVC.ClientCallbackImplementation;
import ClientMVC.ClientJSONHandler;
import ClientMVC.UserXMLHandler;
import Common.Interfaces.ClientCallbackInterface;
import Common.Objects.AdminObjects.AdminAccountsList;
import Common.Objects.ClientObjects.Client;
import Common.Objects.ClientObjects.Conversation;
import javafx.application.Platform;

import Common.Interfaces.ClientInterface;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.function.Consumer;
import javafx.application.Platform;
import java.rmi.RemoteException;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

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

    //USER LOG IN, SIGN UP, AND OTHER RELATED METHODS
    public void signUp(String username, String password, String verificationCode, String jsonString, ClientCallbackInterface clientCallback) throws RemoteException{}
    public void logIn(String username, String jsonString, ClientCallbackInterface clientCalllback) throws RemoteException{}
    public void logOut(String username, String jsonString) throws RemoteException{}
    public void changePassword(String username, String password, String jsonString) throws RemoteException{}
    public void verifyCode(String username, String jsonString) throws RemoteException{}

    //USER HOME UI METHODS
    public void searchUsers(String jsonString) throws RemoteException{}
    public void searchGroups(String jsonString) throws RemoteException{}
    //public void sendFriendRequest(String jsonString) throws RemoteException{}
    public void getOnlineFriends(String jsonString) throws RemoteException{}
    public void getAllUsers() throws RemoteException{}

    //USER ACCOUNT SETTINGS UI METHODS
    public void changeUsername(String jsonString) throws RemoteException{}
    public void changePasswordWithOldPassword(String jsonString) throws RemoteException{}

    //USER FRIEND REQUESTS UI METHODS
    public void sendFriendRequest(String jsonString) throws RemoteException{}
    public void cancelFriendRequest(String jsonString) throws RemoteException{}
    //public void acceptFriendRequest(String jsonString) throws RemoteException{}
    public void rejectFriendRequest(String jsonString) throws RemoteException{}
    public void removeFromFriendsList(String jsonString) throws RemoteException{}
    public void getFriendRequests() throws RemoteException{};
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