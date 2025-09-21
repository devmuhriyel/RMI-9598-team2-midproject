package ClientMVC.ClientView.Home;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class HomeJFXController {
    @FXML
    private Button accountButton;

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
    private Runnable accountButtonListener;
    private Consumer<String> friendClickListener;
    private Consumer<String> addFriendListener;
    @FXML
    private TextField searchField;
    private List<String> originalFriends = Collections.emptyList();
    private List<String> originalGroups = Collections.emptyList();
    private Consumer<String> searchListener;

    @FXML
    private void initialize() {
        if (accountButton != null) {
            accountButton.setOnAction(event -> {
                if (accountButtonListener != null) {
                    accountButtonListener.run();
                }
            });
        }
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
    public void setAddFriendListener(Consumer<String> listener) {
        this.addFriendListener = listener;
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
    public void renderFriends(List<String> users) {
        Platform.runLater(() -> {
            friendsList.getChildren().clear();
            if (users == null || users.isEmpty()) {
                friendsList.getChildren().add(new Label("No users match your search"));
                return;
            }

            for (String user : users) {
                boolean isFriend = originalFriends.contains(user);
                Button userBtn = new Button(user);
                userBtn.setMaxWidth(Double.MAX_VALUE);

                if (isFriend) {
                    // Normal style for friends
                    userBtn.setStyle(""); // Default or your preferred style
                    userBtn.setOnAction(event -> {

                        if (friendClickListener != null) {
                            friendClickListener.accept(user);
                        }
                    });

                    friendsList.getChildren().add(userBtn);
                } else {
                    // Gray style for non-friends
                    userBtn.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #888;");
                    userBtn.setPrefWidth(175);
                    // Add "+" button to add as friend
                    Button addBtn = new Button("+");
                    addBtn.setStyle("-fx-background-color: #b3e5fc; -fx-text-fill: #333;");
                        addBtn.setOnAction(event -> {
                            if (addFriendListener != null) {
                                System.out.println("Add friend: " + user);
                                addFriendListener.accept(user);

                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle(null);
                                alert.setHeaderText(null);
                                alert.setContentText("Request sent to " + user +"!");
                                alert.getDialogPane().setPrefSize(250, 100); // smaller popup
                                alert.showAndWait();
                            }
                        });

                    // Layout: horizontal box for userBtn and addBtn
                    javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(5, userBtn, addBtn);
                    friendsList.getChildren().add(hbox);
                }
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
    public void setAccountButtonListener(Runnable listener) {
        this.accountButtonListener = listener;
    }
}