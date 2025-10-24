import ClientMVC.ClientCallbackImplementation;
import Common.Interfaces.ClientCallbackInterface;
import Common.Interfaces.ClientInterface;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MessageTest {
    public static void main(String[] args) {
        try {
            // Connect to server
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            ClientInterface server = (ClientInterface) registry.lookup("ChatServer");

            // Create callback for User1
            ClientCallbackInterface callback1 = new ClientCallbackImplementation() {
                @Override
                public void onPrivateMessageSent(String message, String jsonString) throws RemoteException {
                    System.out.println("✓ User1 - Message sent: " + message);
                }
            };

            // Create callback for User2
            ClientCallbackInterface callback2 = new ClientCallbackImplementation() {
                @Override
                public void onPrivateMessageReceived(String message, String jsonString) throws RemoteException {
                    System.out.println("✓ User2 - Message received: " + message);
                }
            };

            // Register callbacks
            server.registerClientCallback("alice", callback1);
            server.registerClientCallback("gian", callback2);

            // Send test message
            System.out.println("Sending test message...");
            server.sendPrivateMessage("alice", "gian", "Hello from alice!", "{}");

            // Wait for callbacks
            Thread.sleep(2000);

            System.out.println("Test completed!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}