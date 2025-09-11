package ClientMVC.ClientController;


import ClientMVC.ClientModel.ChatModel;
import ClientMVC.ClientView.Chat.ChatView;
import ClientMVC.ClientView.Chat.ChatJFXController;

public class ChatController {
    private ChatModel mdl;
    private ChatView vw;
    private ChatJFXController chatJFXController;

    public ChatController(ChatModel model, ChatView view, ChatJFXController uiElements) {
        this.mdl = model;
        this.vw = view;
        this.chatJFXController = uiElements;

        setupListeners();
        mdl.setMessageListener(msg -> {
            javafx.application.Platform.runLater(() -> {
                chatJFXController.addMessage(msg);
            });
        });
    }

    private void setupListeners(){
        chatJFXController.setSendButtonListener(() -> sendMSG());
    }

    private void sendMSG(){
        String textToSend = chatJFXController.getMessageField().getText();
        if (!textToSend.isEmpty()) {
            mdl.sendMSG(textToSend);
            chatJFXController.getMessageField().clear();
        }
    }
}
