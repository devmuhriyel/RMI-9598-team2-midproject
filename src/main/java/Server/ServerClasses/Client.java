package Server.ServerClasses;

import java.util.ArrayList;
import java.util.List;

public class Client {
    private String username;
    private String password;
    private String verificationCode;
    private List<FriendRequest> incomingFriendRequests;
    private List<FriendRequest> outgoingFriendRequests;
    private List<ChatHistory> chatHistory;

    public Client(String uName, String pWord, String vCode, List<FriendRequest> inFriends, List<FriendRequest> outFriends, List<ChatHistory> chatHistory) {
        this.username = uName;
        this.password = pWord;
        this.verificationCode = vCode;
        this.incomingFriendRequests = inFriends;
        this.outgoingFriendRequests = outFriends;
        this.chatHistory = chatHistory;
    }

    //SETTERS
    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }
    public void setIncomingFriendRequests(List<FriendRequest> incomingFriendRequests) {
        this.incomingFriendRequests = incomingFriendRequests;
    }
    public void setOutgoingFriendRequests(List<FriendRequest> outgoingFriendRequests) {
        this.outgoingFriendRequests = outgoingFriendRequests;
    }
    public void setChatHistory(List<ChatHistory> chatHistory) {
        this.chatHistory = chatHistory;
    }

    //GETTERS
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public String getVerificationCode() {
        return verificationCode;
    }
    public List<FriendRequest> getIncomingFriendRequests() {
        return incomingFriendRequests;
    }
    public List<FriendRequest> getOutgoingFriendRequests() {
        return outgoingFriendRequests;
    }
    public List<ChatHistory> getChatHistory() {
        return chatHistory;
    }
}
