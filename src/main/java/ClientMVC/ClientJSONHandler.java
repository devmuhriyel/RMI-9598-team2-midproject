package ClientMVC;

import Common.Objects.ClientObjects.Client;
import Common.Objects.ClientObjects.ClientAccountsList;
import Common.Objects.ClientObjects.FriendRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.io.FileWriter;

public class ClientJSONHandler {

    private static final String FILE_PATH = "src/main/resources/JSONFiles/ClientAccounts.json";
    private static String currentUser;

    public String createJSONStringRequest(String logType, String msgType, String sender){
        return "{ \\n \" logType \": \"" + logType + "\", \n \" messageType \": \"" + msgType + "\", \n \" receiver \": \n " + sender + " \n}";
    }

    public String createLoginJSON(String username, String password){
        Gson gson = new Gson();
        java.util.Map<String, String> loginData = new java.util.HashMap<>();
        loginData.put("username", username);
        loginData.put("password", password);
        return gson.toJson(loginData);
    }

    //SETTERS AND GETTERS
    public static void setCurrentUser(String username) {
        currentUser = username;
    }

    public static String getCurrentUser() {
        return currentUser;
    }
}
