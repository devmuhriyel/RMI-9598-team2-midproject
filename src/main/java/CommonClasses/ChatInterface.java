package CommonClasses;

import java.rmi.Remote;

public interface ChatInterface extends Remote {
    //USER LOG IN, SIGN UP, AND OTHER RELATED METHODS
    public void signUp();
    public void logIn();
    public void logOut();
    public void changePassword();
    public void verifyCode();

    //USER HOME UI METHODS
    public void searchUsers();
    public void searchGroups();
    public void sendFriendRequest();
    public void getOnlineFriends();

    //USER ACCOUNT SETTINGS UI METHODS
    public void changeUsername();
    public void changePasswordWithOldPassword();

    //USER FRIEND REQUESTS UI METHODS
    public void cancelFriendRequest();
    public void acceptFriendRequest();
    public void rejectFriendRequest();
    public void removeFromFriendsList();

    //USER COMMON CHATTING METHODS
    public void getChatHistories();
    public void getChatHistory();
    public void getUnreadMessages();

    //USER PRIVATE CHATTING METHODS
    public void sendPrivateMessage();
    public void sendPrivateFile();

    //USER GROUP CHATTING METHODS
    public void sendGroupMessage();
    public void sendGroupFile();
    public void createGroupChat();
    public void addUserToGroup();
    public void removeUserFromGroup();

    //USER COMMUNITY CHATTING METHODS
    public void sendCommunityMessage();
}
