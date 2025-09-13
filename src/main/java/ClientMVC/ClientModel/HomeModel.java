package ClientMVC.ClientModel;

import ClientMVC.UserXMLHandler;

import java.net.Socket;
import java.util.List;

public class HomeModel {
    private final UserXMLHandler userXMLHandler;
    private Socket socket;
    private String username;

    public HomeModel() {
        this.userXMLHandler = new UserXMLHandler();
    }

    public HomeModel(Socket socket, String username) {
        this.userXMLHandler = new UserXMLHandler();
        this.socket = socket;
        this.username = username;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getFriends() {
        if (username == null) return java.util.Collections.emptyList();
        return userXMLHandler.getFriends(username);
    }

    public List<String> getGroups() {
        if (username == null) return java.util.Collections.emptyList();
        return userXMLHandler.getUserGroups(username);
    }

    public List<String> getPrivateHistory(String friendName) {
        if (username == null) return java.util.Collections.emptyList();
        return userXMLHandler.getPrivateHistory(username, friendName);
    }
}


