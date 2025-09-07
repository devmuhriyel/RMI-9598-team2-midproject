package ClientMVC.ClientController;


import ClientMVC.ClientModel.ClientModel;
import ClientMVC.ClientView.ClientView;
import ClientMVC.ClientView.ClientViewController;

public class ClientController {
    private ClientModel mdl;
    private ClientView vw;
    private ClientViewController clientViewController;

    public ClientController(ClientModel model, ClientView view, ClientViewController uiElements) {
        this.mdl = model;
        this.vw = view;
        this.clientViewController = uiElements;

        setupListeners();
        mdl.setMessageListener(msg -> {
            javafx.application.Platform.runLater(() -> {
                clientViewController.addMessage(msg);
            });
        });
    }

    private void setupListeners(){
        clientViewController.setSendButtonListener(() -> sendMSG());
    }

    private void sendMSG(){
        String textToSend = clientViewController.getMessageField().getText();
        if (!textToSend.isEmpty()) {
            mdl.sendMSG(textToSend);
            clientViewController.getMessageField().clear();
        }
    }
}
