package ClientMVC.ClientController;

import ClientMVC.ClientModel.ClientModel;
import ClientMVC.ClientView.ClientView;
import ClientMVC.ClientView.ClientViewController;

public class ClientController {
    private ClientModel mdl;
    private ClientView vw;
    private ClientViewController fController;

    public ClientController(ClientModel model, ClientView view, ClientViewController uiElements) {
        this.mdl = model;
        this.vw = view;
        this.fController = uiElements;

        setupListeners();
    }

    private void setupListeners(){
        fController.setSendButtonListener(() -> {
            System.out.println("Send button clicked!");
            String textToSend = fController.getMessageField().getText();
            if (!textToSend.isEmpty()) {
                mdl.sendMSG(textToSend);
                fController.addMessage("You: " + textToSend);
                fController.getMessageField().clear();
            }
        });
    }
    private void sendMSG(String msg){

    }
}
