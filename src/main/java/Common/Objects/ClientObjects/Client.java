package Common.Objects.ClientObjects;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Client implements Serializable {
    private String username;
    private String password;
    private String verificationCode;
    private List<String> friends;
    private List<Conversation> conversations;

    @SerializedName("groupchats")  // Match your JSON field name
    private List<GroupChat> groupChats;

    private List<FriendRequest> incomingFriendRequests;
    private List<FriendRequest> outgoingFriendRequests;

    // No-arg constructor for Gson
    public Client() {
        this.friends = new ArrayList<>();
        this.conversations = new ArrayList<>();
        this.groupChats = new ArrayList<>();
        this.incomingFriendRequests = new ArrayList<>();
        this.outgoingFriendRequests = new ArrayList<>();
    }

    public Client(String username, String password, String verificationCode) {
        this();
        this.username = username;
        this.password = password;
        this.verificationCode = verificationCode;
    }

    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public List<String> getFriends() { return friends; }
    public void setFriends(List<String> friends) { this.friends = friends; }

    public List<Conversation> getConversations() { return conversations; }
    public void setConversations(List<Conversation> conversations) { this.conversations = conversations; }

    public List<GroupChat> getGroupChats() { return groupChats; }
    public void setGroupChats(List<GroupChat> groupChats) { this.groupChats = groupChats; }

    public List<FriendRequest> getIncomingFriendRequests() { return incomingFriendRequests; }
    public void setIncomingFriendRequests(List<FriendRequest> incomingFriendRequests) {
        this.incomingFriendRequests = incomingFriendRequests;
    }

    public List<FriendRequest> getOutgoingFriendRequests() { return outgoingFriendRequests; }
    public void setOutgoingFriendRequests(List<FriendRequest> outgoingFriendRequests) {
        this.outgoingFriendRequests = outgoingFriendRequests; }
}