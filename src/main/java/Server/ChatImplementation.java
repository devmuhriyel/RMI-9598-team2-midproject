package Server;

import CommonClasses.ChatInterface;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ChatImplementation extends UnicastRemoteObject implements ChatInterface {

    public ChatImplementation() throws RemoteException {
        super();
    }
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
    public void getOnlineFriends() {

    }

    @Override
    public void changeUsername() {

    }

    @Override
    public void changePasswordWithOldPassword() {

    }

    @Override
    public void getChatHistories() {

    }

    @Override
    public void getChatHistory() {

    }

    @Override
    public void getUnreadMessages() {

    }

    @Override
    public void sendPrivateMessage() {

    }

    @Override
    public void sendPrivateFile() {

    }

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

    @Override
    public void sendCommunityMessage() {

    }
}
