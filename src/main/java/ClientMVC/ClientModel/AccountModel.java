package ClientMVC.ClientModel;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AccountModel {
    //This for the Friend Function
    private ObservableList<String> friendRequests;

    public AccountModel() {
        this.friendRequests = FXCollections.observableArrayList();
    }

    public ObservableList<String> getFriendRequests() {
        return friendRequests;
    }

    public void setFriendRequests(java.util.List<String> requests) {
        friendRequests.setAll(requests);
    }

}
