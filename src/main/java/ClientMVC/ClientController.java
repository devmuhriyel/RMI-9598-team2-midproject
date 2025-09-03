package ClientMVC;

import javafx.collections.FXCollections;

public class ClientController {
    private ClientModel mdl;
    private ClientView vw;
    private FXController fController;

    public ClientController(ClientModel model, ClientView view, FXController uiElements) {
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
