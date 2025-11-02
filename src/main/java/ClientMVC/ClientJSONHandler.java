package ClientMVC;

import Common.Objects.ClientObjects.Client;
import Common.Objects.ClientObjects.ClientAccountsList;
import Common.Objects.ClientObjects.FriendRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class ClientJSONHandler {

    private static final String FILE_PATH = "src/main/resources/JSONFiles/ClientAccounts.json";
    private static String currentUser;
    private Gson gson;

    public ClientJSONHandler() {
        this.gson = new Gson();
    }

    // REMOVED OLD BROKEN METHOD, USE GSON INSTEAD
    public String createLoginJSON(String username, String password){
        java.util.Map<String, String> loginData = new java.util.HashMap<>();
        loginData.put("username", username);
        loginData.put("password", password);
        return gson.toJson(loginData);
    }

    public String createJSONStringRequest(String logType, String msgType, String sender){
        Map<String, String> request = new HashMap<>();
        request.put("logType", logType);
        request.put("messageType", msgType);
        request.put("receiver", sender);
        return gson.toJson(request);
    }

    //SETTERS AND GETTERS
    public static void setCurrentUser(String username) {
        currentUser = username;
    }

    public static String getCurrentUser() {
        return currentUser;
    }
}