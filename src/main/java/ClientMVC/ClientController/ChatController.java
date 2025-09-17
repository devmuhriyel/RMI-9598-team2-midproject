package ClientMVC.ClientController;


import ClientMVC.ClientModel.ChatModel;
import ClientMVC.ClientView.Chat.ChatView;
import ClientMVC.ClientView.Chat.ChatJFXController;
import ClientMVC.UserXMLHandler;

import java.util.List;

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
        chatJFXController.setHomeButtonListener(() -> goToHome());
    }

    private void goToHome() {
        vw.goToHome();
    }

    private void sendMSG(){
        String textToSend = chatJFXController.getMessageField().getText();
        String receiver = chatJFXController.getReceiver().getText();
        if (!textToSend.isEmpty()) {
            chatJFXController.addMessage(" " + textToSend);
            mdl.sendMSG(textToSend, chatJFXController.getReceiver().getText());
            chatJFXController.getMessageField().clear();
        }
    }
    public void loadChatHistory(String currentUser, String friend) {
        UserXMLHandler xmlHandler = new UserXMLHandler();
        List<String> history = xmlHandler.getChatHistory(currentUser, friend);

        chatJFXController.clearMessages();
        for (String msg : history) {
            chatJFXController.addMessage(msg);
        }
    }
}
