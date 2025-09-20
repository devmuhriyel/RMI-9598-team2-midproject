package ClientMVC.ClientView.Home;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class HomeJFXController {

    @FXML
    private Button userProfileButton;

    @FXML
    private VBox friendsList;

    @FXML
    private VBox messagesArea1;

    @FXML
    private Label userLabel;

    private Consumer<String> groupClickListener;
    private Runnable userProfileButtonListener;
    private Consumer<String> friendClickListener;

    @FXML
    private TextField searchField;
    private List<String> originalFriends = Collections.emptyList();
    private List<String> originalGroups = Collections.emptyList();
    private Consumer<String> searchListener;

    @FXML
    private void initialize() {
        if (userProfileButton != null) {
            userProfileButton.setOnAction(event -> {
                if (userProfileButtonListener != null) {
                    userProfileButtonListener.run();
                }
            });
        }
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (searchListener != null) {
                    searchListener.accept(newValue); // send text to controller
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
    public void setGroupClickListener(Consumer<String> groupClickListener) {
        this.groupClickListener = groupClickListener;
    }

    public void setUserLabel(String username) {
        if (userLabel != null) {
            userLabel.setText(username);
        }
    }

    public void setFriends(List<String> friends) {
        if (friends == null) {
            originalFriends = Collections.emptyList();
        } else {
            originalFriends = new ArrayList<>(friends);
        }
        renderFriends(originalFriends);
    }

    public void setGroups(List<String> groups) {
        if (groups == null) {
            originalGroups = Collections.emptyList();
        } else {
            originalGroups = new ArrayList<>(groups);
        }
        renderGroups(originalGroups);
    }

    public List<String> getOriginalFriends() {
        return originalFriends;
    }
    public List<String> getOriginalGroups() {
        return originalGroups;
    }
    public void setSearchListener(Consumer<String> listener) {
        this.searchListener = listener;
    }


    //search integrated, alanganin, lilipat ba to sa HomeController TwT
    public void renderFriends(List<String> friends) {
        Platform.runLater(() -> {
            friendsList.getChildren().clear();
            if (friends == null || friends.isEmpty()) {
                friendsList.getChildren().add(new Label("No friends match your search"));
                return;
            }
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

    public void renderGroups(List<String> groups) {
        Platform.runLater(() -> {
            messagesArea1.getChildren().clear();

            // Always show Community Chat first (this is the global chat)
            Button communityBtn = new Button("Community Chat");
            communityBtn.setMaxWidth(Double.MAX_VALUE);
            communityBtn.setStyle("-fx-background-color: #e8f5e8; -fx-text-fill: black; -fx-padding: 10;");
            communityBtn.setOnAction(event -> {
                if (groupClickListener != null) {
                    groupClickListener.accept("Community Chat");
                }
                System.out.println("Selected Community Chat");
            });
            messagesArea1.getChildren().add(communityBtn);
            if (groups != null && !groups.isEmpty()) {
                for (String group : groups) {
                    Button groupBtn = new Button("👥 " + group);
                    groupBtn.setMaxWidth(Double.MAX_VALUE);
                    groupBtn.setOnAction(event -> {
                        if (groupClickListener != null) {
                            groupClickListener.accept(group);
                        }
                        System.out.println("Selected group: " + group);
                    });
                    messagesArea1.getChildren().add(groupBtn);
                }
            }

            Button createGroupBtn = new Button("+ Create New Group");
            createGroupBtn.setMaxWidth(Double.MAX_VALUE);
            createGroupBtn.setStyle("-fx-background-color: #dedcff; -fx-text-fill: black; -fx-padding: 10;");
            createGroupBtn.setOnAction(event -> {
                System.out.println("Create new group clicked");
                // TODO: Implement group creation functionality
            });
            messagesArea1.getChildren().add(createGroupBtn);
        });
    }
}