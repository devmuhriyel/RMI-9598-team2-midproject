package ClientMVC.ClientController;

import ClientMVC.ClientView.ClientViewConnectionController;
import ClientMVC.NetworkManager;

public class ClientControllerConnection {

    private final ClientViewConnectionController view;
    private final NetworkManager network;

    public ClientControllerConnection(ClientViewConnectionController view, NetworkManager network) {
        this.view = view;
        this.network = network;

        // Wire the view back to this controller
        view.setController(this);
    }

    public boolean connectToServer(String ip, int port) {
        return network.connect(ip, port);
    }

    private void handleConnect() {
        String ip = view.getIpAddress();
        String port = view.getPort();

        try {
            network.connect(ip, Integer.parseInt(port));
            view.getStatusLabel().setText("Connected successfully!");
        } catch (Exception e) {
            view.getStatusLabel().setText("Failed: " + e.getMessage());
        }
    }
}

