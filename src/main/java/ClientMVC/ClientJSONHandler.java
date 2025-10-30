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

    //SETTERS AND GETTERS
    public static void setCurrentUser(String username) {
        currentUser = username;
    }

    public static String getCurrentUser() {
        return currentUser;
    }
}
