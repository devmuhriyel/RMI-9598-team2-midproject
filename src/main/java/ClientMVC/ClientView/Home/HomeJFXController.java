package ClientMVC.ClientView.Home;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
    private Consumer<String> createGroupListener;
    private java.util.function.BiConsumer<String, List<String>> createGroupWithMembersListener;
    private java.util.function.BiConsumer<String, String> addMemberToGroupListener;
    private java.util.function.BiConsumer<String, String> removeMemberFromGroupListener;
    private java.util.function.Function<String, List<String>> groupMembersProvider;

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

                    // FIXED: Check if listener exists and add debug logging
                    addBtn.setOnAction(event -> {
                        System.out.println("Add button clicked for user: " + user);
                        System.out.println("addFriendListener is null: " + (addFriendListener == null));

                        if (addFriendListener != null) {
                            System.out.println("Calling addFriendListener for user: " + user);
                            addFriendListener.accept(user);

                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle(null);
                            alert.setHeaderText(null);
                            alert.setContentText("Request sent to " + user + "!");
                            alert.getDialogPane().setPrefSize(250, 100);
                            alert.showAndWait();
                        } else {
                            System.err.println("ERROR: addFriendListener is null!");
                            // Show error to user
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText(null);
                            alert.setContentText("Unable to send friend request. Please try again.");
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
                    groupBtn.setPrefWidth(Double.MAX_VALUE);
                    groupBtn.setMinHeight(28);
                    groupBtn.setOnAction(event -> {
                        if (groupClickListener != null) {
                            groupClickListener.accept(group);
                        }
                        System.out.println("Selected group: " + group);
                    });

                    // Add/Remove member controls
                    Button addBtn = new Button("+");
                    addBtn.setOnAction(e -> {
                        List<String> friends = getOriginalFriends();
                        List<String> currentMembers = groupMembersProvider != null ? groupMembersProvider.apply(group) : new ArrayList<>();
                        List<String> candidates = new ArrayList<>();
                        if (friends != null) {
                            for (String f : friends) {
                                if (f != null && !currentMembers.contains(f)) candidates.add(f);
                            }
                        }
                        if (candidates.isEmpty()) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle(null);
                            alert.setHeaderText(null);
                            alert.setContentText("No friends available to add.");
                            alert.showAndWait();
                            return;
                        }
                        javafx.scene.control.ChoiceDialog<String> dialog = new javafx.scene.control.ChoiceDialog<>(candidates.get(0), candidates);
                        dialog.setTitle("Add Member");
                        dialog.setHeaderText("Add a member to '" + group + "'");
                        dialog.setContentText("Choose friend:");
                        dialog.showAndWait().ifPresent(selected -> {
                            if (addMemberToGroupListener != null) {
                                addMemberToGroupListener.accept(group, selected);
                            }
                        });
                    });

                    Button removeBtn = new Button("-");
                    removeBtn.setOnAction(e -> {
                        List<String> currentMembers = groupMembersProvider != null ? groupMembersProvider.apply(group) : new ArrayList<>();
                        if (currentMembers == null || currentMembers.isEmpty()) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle(null);
                            alert.setHeaderText(null);
                            alert.setContentText("No members to remove.");
                            alert.showAndWait();
                            return;
                        }
                        javafx.scene.control.ChoiceDialog<String> dialog = new javafx.scene.control.ChoiceDialog<>(currentMembers.get(0), currentMembers);
                        dialog.setTitle("Remove Member");
                        dialog.setHeaderText("Remove a member from '" + group + "'");
                        dialog.setContentText("Choose member:");
                        dialog.showAndWait().ifPresent(selected -> {
                            if (removeMemberFromGroupListener != null) {
                                removeMemberFromGroupListener.accept(group, selected);
                            }
                        });
                    });

                    // Tighten spacing and match heights to the group button
                    addBtn.setFocusTraversable(false);
                    removeBtn.setFocusTraversable(false);
                    addBtn.setStyle("-fx-padding: 2 8 2 8; -fx-background-insets: 0; -fx-font-weight: bold; -fx-font-size: 14px;");
                    removeBtn.setStyle("-fx-padding: 2 8 2 8; -fx-background-insets: 0; -fx-font-weight: bold; -fx-font-size: 14px;");
                    addBtn.minHeightProperty().bind(groupBtn.heightProperty());
                    addBtn.maxHeightProperty().bind(groupBtn.heightProperty());
                    removeBtn.minHeightProperty().bind(groupBtn.heightProperty());
                    removeBtn.maxHeightProperty().bind(groupBtn.heightProperty());

                    // Use a StackPane to have a full-width group bar centered, with +/− aligned to the right
                    javafx.scene.layout.StackPane row = new javafx.scene.layout.StackPane();
                    row.setAlignment(javafx.geometry.Pos.CENTER);
                    javafx.scene.layout.StackPane.setAlignment(groupBtn, javafx.geometry.Pos.CENTER);
                    javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                    spacer.setMinWidth(14);
                    spacer.setPrefWidth(14);
                    spacer.setMaxWidth(14);
                    javafx.scene.layout.HBox controls = new javafx.scene.layout.HBox(0, addBtn, spacer, removeBtn);
                    controls.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                    controls.setPadding(new javafx.geometry.Insets(0, 8, 0, 0));
                    controls.setPickOnBounds(false);
                    javafx.scene.layout.StackPane.setAlignment(controls, javafx.geometry.Pos.CENTER_RIGHT);

                    javafx.scene.layout.StackPane.setMargin(controls, new javafx.geometry.Insets(0, 0, 0, 0));
                    javafx.scene.layout.VBox.setVgrow(row, javafx.scene.layout.Priority.NEVER);
                    javafx.scene.layout.HBox.setHgrow(groupBtn, javafx.scene.layout.Priority.ALWAYS);
                    row.getChildren().addAll(groupBtn, controls);
                    messagesArea1.getChildren().add(row);
                }
            }

            Button createGroupBtn = new Button("+ Create New Group");
            createGroupBtn.setMaxWidth(Double.MAX_VALUE);
            createGroupBtn.setStyle("-fx-background-color: #dedcff; -fx-text-fill: black; -fx-padding: 10;");
            createGroupBtn.setOnAction(event -> {
                System.out.println("Create new group clicked");
                // Prompt for the group name
                TextInputDialog dialog = new TextInputDialog("");
                dialog.setTitle("Create Group");
                dialog.setHeaderText("Create a new group chat");
                dialog.setContentText("Enter group name:");

                Optional<String> result = dialog.showAndWait();
                if (result.isPresent() && !result.get().trim().isEmpty()) {
                    String groupName = result.get().trim();
                    // Next: prompt to select members from friends
                    List<String> friends = getOriginalFriends();
                    Dialog<List<String>> membersDialog = new Dialog<>();
                    membersDialog.setTitle("Select Group Members");
                    membersDialog.setHeaderText("Select friends to add to '" + groupName + "' (optional)");

                    VBox box = new VBox(6);
                    List<CheckBox> checkBoxes = new ArrayList<>();
                    if (friends != null) {
                        for (String friend : friends) {
                            CheckBox cb = new CheckBox(friend);
                            checkBoxes.add(cb);
                            box.getChildren().add(cb);
                        }
                    }
                    membersDialog.getDialogPane().setContent(box);
                    membersDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

                    membersDialog.setResultConverter(button -> {
                        if (button == ButtonType.OK) {
                            List<String> selected = new ArrayList<>();
                            for (CheckBox cb : checkBoxes) {
                                if (cb.isSelected()) {
                                    selected.add(cb.getText());
                                }
                            }
                            return selected;
                        }
                        return null;
                    });

                    Optional<List<String>> selectedMembersOpt = membersDialog.showAndWait();
                    List<String> selectedMembers = selectedMembersOpt.orElse(Collections.emptyList());

                    if (createGroupWithMembersListener != null) {
                        createGroupWithMembersListener.accept(groupName, selectedMembers);
                    } else if (createGroupListener != null) {
                        // Fallback to legacy behavior if new listener not set
                        createGroupListener.accept(groupName);
                    }
                }
            });
            messagesArea1.getChildren().add(createGroupBtn);
        });
    }

    public void setCreateGroupListener(Consumer<String> listener) {
        this.createGroupListener = listener;
    }

    public void setCreateGroupWithMembersListener(java.util.function.BiConsumer<String, List<String>> listener) {
        this.createGroupWithMembersListener = listener;
    }

    public void setAccountButtonListener(Runnable listener) {
        this.accountButtonListener = listener;
    }


    private List<String> allUsers = Collections.emptyList();

    public void setAllUsers(List<String> users) {
        if (users == null) {
            this.allUsers = Collections.emptyList();
        } else {
            this.allUsers = new ArrayList<>(users);
        }
    }

    public List<String> getAllUsers() {
        return allUsers;
    }

    public void setAddMemberToGroupListener(java.util.function.BiConsumer<String, String> listener) {
        this.addMemberToGroupListener = listener;
    }

    public void setRemoveMemberFromGroupListener(java.util.function.BiConsumer<String, String> listener) {
        this.removeMemberFromGroupListener = listener;
    }

    public void setGroupMembersProvider(java.util.function.Function<String, List<String>> provider) {
        this.groupMembersProvider = provider;
    }
}