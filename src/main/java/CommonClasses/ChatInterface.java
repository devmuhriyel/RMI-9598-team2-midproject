package CommonClasses;

import java.rmi.Remote;

public interface ChatInterface extends Remote {
    //USER LOG IN, SIGN UP, AND OTHER RELATED METHODS
    public void signUp(String jsonString);
    public void logIn(String jsonString);
    public void logOut(String jsonString);
    public void changePassword(String jsonString);
    public void verifyCode(String jsonString);

    //USER HOME UI METHODS
    public void searchUsers(String jsonString);
    public void searchGroups(String jsonString);
    public void sendFriendRequest(String jsonString);
    public void getOnlineFriends(String jsonString);

    //USER ACCOUNT SETTINGS UI METHODS
    public void changeUsername(String jsonString);
    public void changePasswordWithOldPassword(String jsonString);

    //USER FRIEND REQUESTS UI METHODS
    public void cancelFriendRequest(String jsonString);
    public void acceptFriendRequest(String jsonString);
    public void rejectFriendRequest(String jsonString);
    public void removeFromFriendsList(String jsonString);

    //USER COMMON CHATTING METHODS
    public void getChatHistories(String jsonString);
    public void getChatHistory(String jsonString);
    public void getUnreadMessages(String jsonString);

    //USER PRIVATE CHATTING METHODS
    public void sendPrivateMessage(String jsonString);
    public void sendPrivateFile(String jsonString);

    //USER GROUP CHATTING METHODS
    public void sendGroupMessage(String jsonString);
    public void sendGroupFile(String jsonString);
    public void createGroupChat(String jsonString);
    public void addUserToGroup(String jsonString);
    public void removeUserFromGroup(String jsonString);

    //USER COMMUNITY CHATTING METHODS
    public void sendCommunityMessage(String jsonString);
}
