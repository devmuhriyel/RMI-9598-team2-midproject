package ClientMVC.ClientController;


import ClientMVC.ClientModel.ClientModel;
import ClientMVC.ClientView.Chat.ClientView;
import ClientMVC.ClientView.Chat.ClientJFXController;

public class ClientController {
    private ClientModel mdl;
    private ClientView vw;
    private ClientJFXController clientJFXController;

    public ClientController(ClientModel model, ClientView view, ClientJFXController uiElements) {
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
