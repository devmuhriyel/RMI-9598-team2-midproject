package CommonInterface;

public interface AdminInterface {
    //ADMIN LOG IN, SIGN UP, AND OTHER RELATED METHODS
    public void signUp(String jsonString);
    public void logIn(String jsonString);
    public void logOut(String jsonString);
    public void changePassword(String jsonString);
    public void verifyCode(String jsonString);

    //ADMIN HOME UI METHODS
    public void searchUsers(String jsonString);
    public void searchGroups(String jsonString);

    //ADMIN ACCOUNT SETTINGS UI METHODS
    public void deleteGroup(String jsonString);

    //ADMIN COMMON CHATTING METHODS
    public String getChatHistoryOfUser(String jsonString);
    public String getChatHistoryOfGroup(String jsonString);
    public String getChatHistoryOfCommunity(String jsonString);

    //ADMIN COMMUNITY CHATTING METHODS
    public void sendCommunityMessage(String jsonString);
}
