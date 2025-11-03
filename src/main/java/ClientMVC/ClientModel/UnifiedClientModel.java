package ClientMVC.ClientModel;

import ClientMVC.ClientCallbackImplementation;
import ClientMVC.ClientJSONHandler;
import Common.Interfaces.ClientCallbackInterface;
import Common.Interfaces.ClientInterface;
import Common.Objects.ClientObjects.Conversation;
import Common.Objects.ClientObjects.GroupChat;

import java.io.*;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.util.Base64;
import java.util.List;

public class UnifiedClientModel {
    private final ClientInterface serverConnection;
    private final ClientCallbackImplementation callback;
    private String username;
    private ClientJSONHandler jsonHandler;

    public UnifiedClientModel(ClientInterface serverStub, ClientCallbackImplementation callback) throws IOException {
        this.serverConnection = serverStub;
        this.callback = callback;
        this.jsonHandler = new ClientJSONHandler();
    }

    //USER LOG IN, SIGN UP, AND OTHER RELATED METHODS
    public void signUp(String username, String password, String verificationCode, String jsonString, ClientCallbackInterface clientCallback) throws RemoteException{
        serverConnection.signUp(username, password, verificationCode, jsonString, clientCallback);
    }

    public void logIn(String username, String jsonString, ClientCallbackInterface clientCallback) throws RemoteException{
        serverConnection.logIn(username, jsonString, clientCallback);
    }

    public void logOut(String username, String jsonString) throws RemoteException{
        serverConnection.logOut(username, jsonString);
    }

    public void changePassword(String username, String password, String jsonString) throws RemoteException{
        serverConnection.changePassword(jsonString);
    }

    public void verifyCode(String username, String jsonString) throws RemoteException{
        serverConnection.verifyCode(jsonString);
    }

    //USER HOME UI METHODS
    public void searchUsers(String jsonString) throws RemoteException{
        serverConnection.searchUsers(jsonString);
    }

    public void searchGroups(String jsonString) throws RemoteException{
        serverConnection.searchGroups(jsonString);
    }

    public void getOnlineFriends(String jsonString) throws RemoteException{
        serverConnection.getOnlineFriends(jsonString);
    }

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
    public void changePasswordWithOldPassword(String oldPassword, String newPassword) throws RemoteException {
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

    public void sendFriendRequest(String jsonString) throws RemoteException {
        serverConnection.sendFriendRequest(jsonString);
    }
    public void acceptFriendRequest(String jsonString) throws RemoteException {
        serverConnection.acceptFriendRequest(jsonString);
    }

    public void getSentFriendRequests(String jsonString) throws RemoteException {
        System.out.println("Model: Requesting sent friend requests");
         serverConnection.getSentFriendRequests(jsonString);
    }

    public void getReceivedFriendRequests(String jsonString) throws RemoteException {
        System.out.println("Model: Requesting received friend requests");
        serverConnection.getReceivedFriendRequests(jsonString);
    }
    public void getFriendsList(String jsonString) throws RemoteException {
        System.out.println("Model: Requesting friends list");
        serverConnection.getFriends(jsonString);
    }

    public void cancelFriendRequest(String jsonString) throws RemoteException{
        serverConnection.cancelFriendRequest(jsonString);
    }

    public void rejectFriendRequest(String jsonString) throws RemoteException{
        serverConnection.rejectFriendRequest(jsonString);
    }

    public void removeFromFriendsList(String jsonString) throws RemoteException{
        serverConnection.removeFromFriendsList(jsonString);
    }

    //USER COMMON CHATTING METHODS
    public void getChatHistories(String jsonString, String sender) throws RemoteException{
        serverConnection.getChatHistories(jsonString, sender);
    }

    public void getChatHistoryWith(String jsonString, String sender, String receiver) throws RemoteException{
        serverConnection.getChatHistoryWith(jsonString, sender, receiver);
    }

    public List<GroupChat> getGroupChatHistories(String jsonString, String username) throws RemoteException{
        return serverConnection.getGroupChatHistories(jsonString, username);
    }

    public GroupChat getGroupChatHistoryWith(String jsonString, String username, String groupID) throws RemoteException{
        return serverConnection.getGroupChatHistoryWith(jsonString, username, groupID);
    }

    public void getUnreadMessages(String jsonString) throws RemoteException{
        serverConnection.getUnreadMessages(jsonString);
    }

    public void sendPrivateMessage(String sender, String receiver, String message, String jsonString) throws RemoteException{
        serverConnection.sendPrivateMessage(sender, receiver, message, jsonString);
    }

    public void sendPrivateFile(String sender, String receiver, File file, String jsonString) throws RemoteException{
        try {
            // Read file and encode to Base64
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            String fileData = Base64.getEncoder().encodeToString(fileBytes);
            String fileName = file.getName();

            serverConnection.sendPrivateFile(sender, receiver, fileName, fileData, jsonString);
        } catch (IOException e) {
            throw new RemoteException("Failed to read file", e);
        }
    }

    //USER GROUP CHATTING METHODS
    public void sendGroupMessage(String sender, String groupID, String message, String jsonString) throws RemoteException{
        serverConnection.sendGroupMessage(sender, groupID, message, jsonString);
    }

    public void sendGroupFile(String sender, String groupID, File file, String jsonString) throws RemoteException{
        try {
            // Read file and encode to Base64
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            String fileData = Base64.getEncoder().encodeToString(fileBytes);
            String fileName = file.getName();

            serverConnection.sendGroupFile(sender, groupID, fileName, fileData, jsonString);
        } catch (IOException e) {
            throw new RemoteException("Failed to read file", e);
        }
    }

    public void createGroupChat(String jsonString) throws RemoteException{
        serverConnection.createGroupChat(jsonString);
    }

    public void addUserToGroup(String jsonString) throws RemoteException{
        serverConnection.addUserToGroup(jsonString);
    }

    public void removeUserFromGroup(String jsonString) throws RemoteException{
        serverConnection.removeUserFromGroup(jsonString);
    }

    // GETTERS
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    //HELPER METHODS
    public String createJSONStringRequest(String logType, String msgContent, String sender){
        return jsonHandler.createJSONStringRequest(logType, msgContent, sender);
    }
    // Helper method to save received file
    public void saveReceivedFile(String fileName, String fileData, String savePath) throws IOException {
        byte[] fileBytes = Base64.getDecoder().decode(fileData);
        File outputFile = new File(savePath, fileName);
        Files.write(outputFile.toPath(), fileBytes);
    }

    public void registerClientCallback(String username, ClientCallbackInterface callback) throws RemoteException {
        serverConnection.registerClientCallback(username, callback);
    }

    public ClientCallbackImplementation getCallback() {
        return callback;
    }


    public List<String> fetchAllUsernames() throws RemoteException {
        if (serverConnection != null) {
            return serverConnection.getAllUsernames();
        } else {
            throw new RemoteException("RMI Server Service is not available.");
        }
    }
}