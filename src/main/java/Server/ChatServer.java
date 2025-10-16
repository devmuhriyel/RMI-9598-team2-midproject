package Server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ChatServer {
    public static void main(String[] args) {
        try{
            ClientImplementation implementation = new ClientImplementation();
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("ChatServer", implementation);
            System.out.println("Server ready");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
