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
    private VBox friendsList; // private messages

    @FXML
    private VBox messagesArea1; // "Groups" messages area

    @FXML
    private Label userLabel;

    private Consumer<String> groupClickListener;
    private Runnable userProfileButtonListener;
    private Consumer<String> friendClickListener;

    @FXML
    private TextField searchField;
    @FXML
    private Button searchButton;

    private List<String> originalFriends = Collections.emptyList();
    private List<String> originalGroups = Collections.emptyList();

    private static final List<String> PLACEHOLDER_GROUPS = Arrays.asList("Community Chat", "General Chat", "Project Team", "Study Group");

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

        if (searchButton != null) {
            searchButton.setOnAction(event -> {
                if (searchListener != null && searchField != null) {
                    searchListener.accept(searchField.getText());
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


    // Method to update user label with username
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

    // Expose originals for controller-side search
    public List<String> getOriginalFriends() {
        return originalFriends;
    }

    public List<String> getOriginalGroups() {
        return originalGroups;
    }

    // Allow controller to listen for search actions
    public void setSearchListener(Consumer<String> listener) {
        this.searchListener = listener;
    }



    //search integrated, alanganin, lilipat ba to sa HomeController TwT

    // Rendering helpers used by controller
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

            // Always show Community Chat first
            Button communityBtn = new Button("🌐 Community Chat");
            communityBtn.setMaxWidth(Double.MAX_VALUE);
            communityBtn.setStyle("-fx-background-color: #e8f5e8; -fx-text-fill: black; -fx-padding: 10;");
            communityBtn.setOnAction(event -> {
                if (groupClickListener != null) {
                    groupClickListener.accept("Community Chat");
                }
                System.out.println("Selected Community Chat");
            });
            messagesArea1.getChildren().add(communityBtn);

            // Add user's actual groups if any
            if (groups != null && !groups.isEmpty()) {
                for (String group : groups) {
                    Button groupBtn = new Button("📁 " + group);
                    groupBtn.setMaxWidth(Double.MAX_VALUE);
                    groupBtn.setOnAction(event -> {
                        if (groupClickListener != null) {
                            groupClickListener.accept(group);
                        }
                        System.out.println("Selected group: " + group);
                    });
                    messagesArea1.getChildren().add(groupBtn);
                }
            } else {
                // Show placeholder groups (excluding Community Chat since it's already added)
                List<String> otherPlaceholders = Arrays.asList("General Chat", "Project Team", "Study Group");
                for (String group : otherPlaceholders) {
                    Button groupBtn = new Button("📁 " + group);
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

            // Add create group button
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