package ClientMVC.ClientController;


import ClientMVC.ClientModel.ChatModel;
import ClientMVC.ClientView.Chat.ChatView;
import ClientMVC.ClientView.Chat.ClientJFXController;

public class ChatController {
    private ChatModel mdl;
    private ChatView vw;
    private ClientJFXController clientJFXController;

    public ChatController(ChatModel model, ChatView view, ClientJFXController uiElements) {
        this.mdl = model;
        this.vw = view;
        this.clientJFXController = uiElements;

        setupListeners();
        mdl.setMessageListener(msg -> {
            javafx.application.Platform.runLater(() -> {
                clientJFXController.addMessage(msg);
            });
        });
    }

    private void setupListeners(){
        clientJFXController.setSendButtonListener(() -> sendMSG());
    }

    private void sendMSG(){
        String textToSend = clientJFXController.getMessageField().getText();
        if (!textToSend.isEmpty()) {
            mdl.sendMSG(textToSend);
            clientJFXController.getMessageField().clear();
        }
    }
}
