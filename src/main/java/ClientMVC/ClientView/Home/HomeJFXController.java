package ClientMVC.ClientView.Home;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class HomeJFXController {

    @FXML
    private Button userProfileButton;

    @FXML
    private VBox friendsList; // private messages

    @FXML
    private VBox messagesArea1; // "Groups" messages area

    @FXML
    private Label userLabel;

    private Runnable userProfileButtonListener;
    private Consumer<String> friendClickListener;


    @FXML
    private void initialize() {
        if (userProfileButton != null) {
            userProfileButton.setOnAction(event -> {
                if (userProfileButtonListener != null) {
                    userProfileButtonListener.run();
                }
            });
        }
    }

    // Getters for MVC access
    public Button getUserProfileButton() {
        return userProfileButton;
    }

    public void setFriendClickListener(Consumer<String> friendClickListener) {
        this.friendClickListener = friendClickListener;
    }


    // Method to update user label with username
    public void setUserLabel(String username) {
        if (userLabel != null) {
            userLabel.setText(username);
        }
    }

    public void setFriends(List<String> friends) {
        Platform.runLater(() -> {
            friendsList.getChildren().clear();
            for (String friend : friends) {
                Button friendBtn = new Button(friend);
                friendBtn.setMaxWidth(Double.MAX_VALUE);
                friendBtn.setOnAction(event -> {
                    if (friendClickListener != null) {
                        friendClickListener.accept(friend);
                    }
                });
                friendsList.getChildren().add(friendBtn);
            }
        });
    }

    public void setGroups(List<String> groups) {
        Platform.runLater(() -> {
            messagesArea1.getChildren().clear();
            java.util.List<String> groupsToRender = (groups != null && !groups.isEmpty())
                    ? new java.util.ArrayList<>(groups)
                    : new java.util.ArrayList<>(java.util.Arrays.asList("General Chat", "Project Team", "Study Group"));
            for (String group : groupsToRender) {
                Button groupBtn = new Button("📁 " + group);
                groupBtn.setMaxWidth(Double.MAX_VALUE);
                groupBtn.setOnAction(event -> {
                    System.out.println("Selected group: " + group);
                });
                messagesArea1.getChildren().add(groupBtn);
            }
            Button createGroupBtn = new Button("+ Create New Group");
            createGroupBtn.setMaxWidth(Double.MAX_VALUE);
            createGroupBtn.setStyle("-fx-background-color: #dedcff; -fx-text-fill: black; -fx-padding: 10;");
            createGroupBtn.setOnAction(event -> {
                System.out.println("Create new group clicked");
            });
            messagesArea1.getChildren().add(createGroupBtn);
        });
    }
}
