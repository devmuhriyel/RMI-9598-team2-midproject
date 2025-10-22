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

    public boolean addFriend(String friendUsername) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            FileReader reader = new FileReader(FILE_PATH);
            ClientAccountsList data = gson.fromJson(reader, ClientAccountsList.class);
            reader.close();

            Client currentClient = null;
            for (Client c : data.getClients()) {
                if (c.getUsername().equalsIgnoreCase(currentUser)) {
                    currentClient = c;
                    break;
                }
            }

            if (currentClient == null) {
                System.out.println("Current user not found.");
                return false;
            }

            // Check for duplicate friend requests
            for (FriendRequest fr : currentClient.getOutgoingFriendRequests()) {
                if (fr.getResponder().equalsIgnoreCase(friendUsername) &&
                        fr.getStatus().equalsIgnoreCase("pending")) {
                    System.out.println("Friend request already sent to " + friendUsername);
                    return false;
                }
            }

            FriendRequest request = new FriendRequest(currentUser, friendUsername, "pending");
            request.setRequester(currentUser);
            request.setResponder(friendUsername);
            request.setStatus("pending");

            currentClient.getOutgoingFriendRequests().add(request);

            FileWriter writer = new FileWriter(FILE_PATH);
            gson.toJson(data, writer);
            writer.close();

            System.out.println("Friend request added: " + currentUser + " → " + friendUsername);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //SETTERS AND GETTERS
    public static void setCurrentUser(String username) {
        currentUser = username;
    }

    public static String getCurrentUser() {
        return currentUser;
    }
}
