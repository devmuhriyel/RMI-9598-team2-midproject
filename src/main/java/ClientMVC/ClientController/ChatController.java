package ClientMVC.ClientController;

import ClientMVC.ClientCallbackImplementation;
import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Chat.ChatView;
import ClientMVC.ClientView.Chat.ChatJFXController;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.UserXMLHandler;
import java.io.File;

import Common.Objects.ClientObjects.Message;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.rmi.RemoteException;
import java.util.List;

public class ChatController {
    private UnifiedClientModel model;
    private ChatView view;
    private ChatJFXController chatJFXController;
    private ClientCallbackImplementation clientCallback;
    private String currentChatPartner;
    private boolean isGroupChat = false;
    private boolean isCommunityChat = false;

    public ChatController(UnifiedClientModel model, ChatView view, ChatJFXController uiElements, ClientCallbackImplementation callback) {
        this.model = model;
        this.view = view;
        this.chatJFXController = uiElements;
        this.clientCallback = callback;
        setupListeners();
    }

    private void setupListeners() {
        chatJFXController.setSendButtonListener(() -> {});
        chatJFXController.setHomeButtonListener(() -> {});
        chatJFXController.setFileButtonListener(() -> {});
        chatJFXController.setFileOpenListener(fileName -> {});
    }

    //Messaging Methods
    public void sendPrivateMessage() throws RemoteException {
        model.sendPrivateMessage(model.getUsername(), chatJFXController.getReceiver().getText(), chatJFXController.getMessageField().getText(), model.createJSONStringRequest("request", "SEND_MESSAGE", model.getUsername()));
        chatJFXController.getMessageField().clear();
    }

    //Callback Methods
    public void onPrivateMessageSent(Message message) throws RemoteException {
        chatJFXController.addChatMessage(
                message.getSender(),
                message.getContent(),
                model.getUsername()
        );    }


    public void onPrivateMessageReceived(Message msg) {
    }
}