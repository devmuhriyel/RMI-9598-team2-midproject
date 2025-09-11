package ClientMVC.ClientView.Chat;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.AnchorPane;

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

    @FXML
    public void initialize() {
        System.out.println("FXController initialized");
        System.out.println("sendButton is " + (sendButton == null ? "null" : "ready"));

        if (sendButton != null) {
            sendButton.setOnAction(event -> {
                if (sendButtonListener != null) {
                    sendButtonListener.run();
                }
            });
        }
    }

    public void addMessage(String message) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Label messageLabel = new javafx.scene.control.Label(message);
            messageLabel.setStyle("-fx-background-color: lightgray; -fx-padding: 5; -fx-background-radius: 5;");
            if (messagesArea != null) {
                messagesArea.getChildren().add(messageLabel);
            }
        });
    }

    public void setSendButtonListener(Runnable sendButtonListener) {
        this.sendButtonListener = sendButtonListener;
    }

    // Getters for MVC access
    public TextField getMessageField() { return messageField; }
    public VBox getMessagesArea() { return messagesArea; }
    public Button getSendButton() { return sendButton; }
}
