package Server;

import CommonClasses.ChatInterface;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ChatImplementation extends UnicastRemoteObject implements ChatInterface {

    public ChatImplementation() throws RemoteException {
        super();
    }

    //USER LOG IN, SIGN UP, AND OTHER RELATED METHODS
    @Override
    public void signUp() {

    }

    @Override
    public void logIn() {

    }

    @Override
    public void logOut() {

    }

    @Override
    public void changePassword() {

    }

    @Override
    public void verifyCode() {

    }

    //USER HOME UI METHODS
    @Override
    public void searchUsers() {

    }

    @Override
    public void searchGroups() {

    }

    @Override
    public void sendFriendRequest() {

    }

    @Override
    public void getOnlineFriends() {

    }

    //USER ACCOUNT SETTINGS UI METHODS
    @Override
    public void changeUsername() {

    }

    @Override
    public void changePasswordWithOldPassword() {

    }

    //USER FRIEND REQUESTS UI METHODS
    @Override
    public void cancelFriendRequest() {

    }

    @Override
    public void acceptFriendRequest() {

    }

    @Override
    public void rejectFriendRequest() {

    }

    @Override
    public void removeFromFriendsList() {

    }

    @Override
    public void getChatHistories() {

    }

    //USER COMMON CHATTING METHODS
    @Override
    public void getChatHistory() {

    }

    @Override
    public void getUnreadMessages() {

    }

    //USER PRIVATE CHATTING METHODS
    @Override
    public void sendPrivateMessage() {

    }

    @Override
    public void sendPrivateFile() {

    }

    //USER GROUP CHATTING METHODS
    @Override
    public void sendGroupMessage() {

    }

    @Override
    public void sendGroupFile() {

    }

    @Override
    public void createGroupChat() {

    }

    @Override
    public void addUserToGroup() {

    }

    @Override
    public void removeUserFromGroup() {

    }

    //USER COMMUNITY CHATTING METHODS
    @Override
    public void sendCommunityMessage() {

    }
}