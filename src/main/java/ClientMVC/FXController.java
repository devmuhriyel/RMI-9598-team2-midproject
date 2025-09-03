package ClientMVC;


import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;


public class FXController {

    @FXML
    private Button sendButton;
    @FXML
    private TextField messageField;
    @FXML
    private VBox messagesArea;
    private Runnable sendButtonListener;

    @FXML
    public void initialize() {
        System.out.println("FXController initialized");
        System.out.println("sendButton is " + (sendButton == null ? "null" : "ready"));
        sendButton.setOnAction(event -> {
            if (sendButtonListener != null) {
                sendButtonListener.run();
            }
        });
    }

    public void addMessage(String message) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Label messageLabel = new javafx.scene.control.Label(message);
            messageLabel.setStyle("-fx-background-color: lightgray; -fx-padding: 5; -fx-background-radius: 5;");
            messagesArea.getChildren().add(messageLabel);
        });
    }

    public void setSendButtonListener(Runnable sendButtonListener) {
        sendButton.setOnAction(event -> sendButtonListener.run());    }
    public TextField getMessageField() {
        return messageField;
    }

    public VBox getMessagesArea() {
        return messagesArea;
    }

    public Button getSendButton() {
        return sendButton;
    }
}
