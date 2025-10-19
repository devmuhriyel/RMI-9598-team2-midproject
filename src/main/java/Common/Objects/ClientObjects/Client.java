package Common.Objects.ClientObjects;

import java.util.List;
import java.util.ArrayList;

public class Client {
    private String username;
    private String password;
    private String verificationCode;
    private List<FriendRequest> incomingFriendRequests;
    private List<FriendRequest> outgoingFriendRequests;
    private List<ChatRecords> chatRecords;
    private List<String> friends;

    public Client(String uName, String pWord, String vCode, List<FriendRequest> inFriends, List<FriendRequest> outFriends, List<ChatRecords> chatRecords) {
        this.username = uName;
        this.password = pWord;
        this.verificationCode = vCode;
        this.incomingFriendRequests = inFriends;
        this.outgoingFriendRequests = outFriends;
        this.chatRecords = chatRecords;
        this.friends = new ArrayList<>();
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
    public void setChatHistory(List<ChatRecords> chatRecords) {
        this.chatRecords = chatRecords;
    }
    public void setFriends(List<String> friends) {
        this.friends = friends;
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
    public List<ChatRecords> getChatRecords() {
        return chatRecords;
    }
    public List<String> getFriends() {
        if (this.friends == null) {
            this.friends = new ArrayList<>();
        }
        return this.friends;
    }
}
