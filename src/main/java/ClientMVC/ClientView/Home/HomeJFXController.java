package ClientMVC.ClientView.Home;

import ClientMVC.UserXMLHandler;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Scanner;

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
    private UserXMLHandler userXMLHandler = new UserXMLHandler();


    @FXML
    private void initialize() {
        System.out.println("HomeJFXController initialized");
        userXMLHandler = new UserXMLHandler(); // <-- add this

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


    // Method to update user label with username
    public void setUserLabel(String username) {
        if (userLabel != null) {
            userLabel.setText(username);
        }
    }

    public void loadFriends(String username) {
        if (userXMLHandler == null) userXMLHandler = new UserXMLHandler();

        Platform.runLater(() -> {
            friendsList.getChildren().clear();
            List<String> friends = userXMLHandler.getFriends(username);
            for (String friend : friends) {
                Button friendBtn = new Button(friend);
                friendBtn.setMaxWidth(Double.MAX_VALUE);
                friendBtn.setStyle("-fx-background-color: #4a4a4a; -fx-text-fill: white; -fx-padding: 10;");
                friendsList.getChildren().add(friendBtn);
            }
        });
    }

    public void loadGroups(String username) {
        if (userXMLHandler == null) userXMLHandler = new UserXMLHandler();

        Platform.runLater(() -> {
            messagesArea1.getChildren().clear();
            List<String> groups = userXMLHandler.getUserGroups(username);
            
            // If no groups found, add some sample groups for demonstration
            if (groups.isEmpty()) {
                groups.add("General Chat");
                groups.add("Project Team");
                groups.add("Study Group");
            }
            
            for (String group : groups) {
                Button groupBtn = new Button("📁 " + group);
                groupBtn.setMaxWidth(Double.MAX_VALUE);
                groupBtn.setStyle("-fx-background-color: #4a4a4a; -fx-text-fill: white; -fx-padding: 10;");
                groupBtn.setOnAction(event -> {
                    // Handle group selection - you can add group chat functionality here
                    System.out.println("Selected group: " + group);
                });
                messagesArea1.getChildren().add(groupBtn);
            }
            
            // Add a "Create Group" button
            Button createGroupBtn = new Button("+ Create New Group");
            createGroupBtn.setMaxWidth(Double.MAX_VALUE);
            createGroupBtn.setStyle("-fx-background-color: #dedcff; -fx-text-fill: black; -fx-padding: 10;");
            createGroupBtn.setOnAction(event -> {
                // Handle create group - you can add group creation dialog here
                System.out.println("Create new group clicked");
            });
            messagesArea1.getChildren().add(createGroupBtn);
        });
    }
}
