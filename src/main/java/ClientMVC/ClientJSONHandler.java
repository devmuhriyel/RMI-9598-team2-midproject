package ClientMVC;

public class ClientJSONHandler {
    private static String currentUser;

    //SETTERS AND GETTERS
    public static void setCurrentUser(String username) {
        currentUser = username;
    }

    public static String getCurrentUser() {
        return currentUser;
    }
}
