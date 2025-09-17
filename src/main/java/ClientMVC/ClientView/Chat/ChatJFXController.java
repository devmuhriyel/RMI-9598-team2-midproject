package ClientMVC.ClientView.Chat;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.AnchorPane;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.geometry.Insets;
import javafx.scene.layout.Priority;

public class ChatJFXController {
    @FXML
    private TextField messageField;
    @FXML
    private Button sendButton;
    @FXML
    private VBox messagesArea;

    @FXML
    private Button home;
    @FXML
    private Label label;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private Rectangle rectangle;

    @FXML
    private AnchorPane anchorPane;

    private Runnable sendButtonListener;
    private Runnable homeButtonListener;

    @FXML
    public void initialize() {
        if (sendButton != null) {
            sendButton.setOnAction(event -> {
                if (sendButtonListener != null) {
                    sendButtonListener.run();
                }
            });
        }

        if (home != null) {
            home.setOnAction(event -> {
                if (homeButtonListener != null) {
                    homeButtonListener.run();
                }
            });
        }
    }

    public void addMessage(String message) {
        addMessage(message, true);
    }

    public void addMessage(String message, boolean isSender) {
        // Use "You" for sender, or extract from message if it contains sender info
        String senderName = isSender ? "You" : "Friend";
        addMessageWithSender(senderName, message, isSender);
    }

    public void addMessageWithSender(String senderName, String message, boolean isSender) {
        javafx.application.Platform.runLater(() -> {
            double maxBubbleWidth = 580.0 / 3.0;

            // Create sender label
            javafx.scene.control.Label senderLabel = new javafx.scene.control.Label(senderName);
            senderLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666; -fx-font-weight: bold;");
            senderLabel.setPadding(new Insets(0, 15, 2, 15));

            // Create message label
            javafx.scene.control.Label messageLabel = new javafx.scene.control.Label(message);
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(maxBubbleWidth);
            messageLabel.setPrefWidth(Region.USE_COMPUTED_SIZE);
            messageLabel.setMinHeight(Region.USE_PREF_SIZE);
            messageLabel.setPadding(new Insets(10, 15, 10, 15));

            // Create container for sender label and message
            VBox messageWithSender = new VBox(2);
            messageWithSender.setAlignment(isSender ? Pos.TOP_RIGHT : Pos.TOP_LEFT);

            // Create message container
            javafx.scene.layout.HBox messageContainer = new javafx.scene.layout.HBox();
            messageContainer.setPadding(new Insets(5, 10, 5, 10));
            messageContainer.setFillHeight(false);
            messageContainer.setAlignment(Pos.TOP_CENTER);

            if (isSender) {
                // Sender messages (right side) - blue bubble
                messageLabel.setStyle(
                        "-fx-background-color: #007AFF; " +
                                "-fx-text-fill: white; " +
                                "-fx-background-radius: 18; " +
                                "-fx-font-size: 14px;"
                );

                // Add sender label and message to container, aligned right
                messageWithSender.getChildren().addAll(senderLabel, messageLabel);
                messageWithSender.setAlignment(Pos.TOP_RIGHT);

                // Create spacer to push message to the right
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                messageContainer.getChildren().addAll(spacer, messageWithSender);

            } else {
                // Receiver messages (left side) - gray bubble
                messageLabel.setStyle(
                        "-fx-background-color: #E5E5EA; " +
                                "-fx-text-fill: black; " +
                                "-fx-background-radius: 18; " +
                                "-fx-font-size: 14px;"
                );

                // Add sender label and message to container, aligned left
                messageWithSender.getChildren().addAll(senderLabel, messageLabel);
                messageWithSender.setAlignment(Pos.TOP_LEFT);

                // Create spacer to keep message on the left
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                messageContainer.getChildren().addAll(messageWithSender, spacer);
            }

            if (messagesArea != null) {
                messagesArea.getChildren().add(messageContainer);
                // Auto-scroll to bottom when new message is added
                javafx.application.Platform.runLater(() -> {
                    scrollPane.setVvalue(1.0);
                });
            }
        });
    }

    public void addChatMessage(String senderName, String message, String currentUser) {
        boolean isSender = senderName.equals(currentUser);
        addMessageWithSender(senderName, message, isSender);
    }

    public void clearMessages() {
        javafx.application.Platform.runLater(() -> {
            if (messagesArea != null) {
                messagesArea.getChildren().clear();
            }
        });
    }

    public void setSendButtonListener(Runnable sendButtonListener) {
        this.sendButtonListener = sendButtonListener;
    }

    public void setHomeButtonListener(Runnable homeButtonListener) {
        this.homeButtonListener = homeButtonListener;
    }

    // Getters for MVC access

    public void setLabel(String label) {
        this.label.setText(label);
    }
    public TextField getMessageField() { return messageField; }
    public VBox getMessagesArea() { return messagesArea; }
    public Button getSendButton() { return sendButton; }
    public Button getHomeButton() { return home; }
    public Label getReceiver() { return label; }
}