package ClientMVC.ClientModel;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.util.concurrent.locks.ReentrantLock;

// 💡 Import your server's remote service interface
import Common.Interfaces.ClientInterface;

public class ConnectionModel {

    // Stores the remote service stub (the client's proxy for the server)
    private volatile ClientInterface remoteServiceStub;
    private volatile String ip;
    private volatile int port;
    private volatile boolean connected;
    private final ReentrantLock connectionLock = new ReentrantLock();

    // 💡 The binding name must be exactly what the server uses to bind its service
    private static final String RMI_BINDING_NAME = "ChatServer"; // Adjust if your server uses a different name

    public enum ConnectionStatus {
        SUCCESS,
        EMPTY_FIELDS,
        INVALID_PORT_FORMAT,
        INVALID_IP_FORMAT,
        INVALID_PORT_RANGE,
        CONNECTION_FAILED,      // For Registry connection or general RMI network errors
        RMI_LOOKUP_FAILED       // For when the service name isn't found in the Registry
    }

    public static class ConnectionResult {
        private final ConnectionStatus status;
        private final String message;

        public ConnectionResult(ConnectionStatus status, String message) {
            this.status = status;
            this.message = message;
        }

        public ConnectionStatus getStatus() { return status; }
        public String getMessage() { return message; }
    }

    public ConnectionResult connect(String ip, String portText) {
        connectionLock.lock();
        try {
            this.connected = false;
            this.remoteServiceStub = null; // Reset stub

            // --- Input Validation ---
            if (ip == null || ip.trim().isEmpty() || portText == null || portText.trim().isEmpty()) {
                return new ConnectionResult(ConnectionStatus.EMPTY_FIELDS, "IP address and Port cannot be empty.");
            }

            ip = ip.trim();
            portText = portText.trim();
            int port;

            try {
                port = Integer.parseInt(portText);
            } catch (NumberFormatException e) {
                return new ConnectionResult(ConnectionStatus.INVALID_PORT_FORMAT, "Port must be a valid number.");
            }

            if (port < 1 || port > 65535) {
                return new ConnectionResult(ConnectionStatus.INVALID_PORT_RANGE, "Port must be between 1 and 65535.");
            }

            String ipv4Pattern = "^(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)){3}$";
            if (!ip.matches(ipv4Pattern) && !ip.equalsIgnoreCase("localhost")) { // Allow 'localhost' too
                return new ConnectionResult(ConnectionStatus.INVALID_IP_FORMAT, "Invalid IP address format.");
            }

            // --- RMI Connection Logic ---
            try {
                // 1. Get the reference to the RMI Registry
                Registry registry = LocateRegistry.getRegistry(ip, port);

                // 2. Look up the remote object and cast it to the common interface
                remoteServiceStub = (ClientInterface) registry.lookup(RMI_BINDING_NAME);

                this.connected = true;
                this.ip = ip;
                this.port = port;
                return new ConnectionResult(ConnectionStatus.SUCCESS, "RMI service successfully retrieved.");

            } catch (java.rmi.ConnectException e) {
                // Thrown if the Registry service is not running or network is unreachable
                return new ConnectionResult(ConnectionStatus.CONNECTION_FAILED,
                        "Failed to connect to RMI Registry: " + e.getMessage());
            } catch (java.rmi.NotBoundException e) {
                // Thrown if the binding name is incorrect or the service hasn't been started on the server
                return new ConnectionResult(ConnectionStatus.RMI_LOOKUP_FAILED,
                        "Service '" + RMI_BINDING_NAME + "' not found in Registry.");
            } catch (RemoteException e) {
                // General RMI communication error
                return new ConnectionResult(ConnectionStatus.CONNECTION_FAILED,
                        "RMI Communication Error: " + e.getMessage());
            } catch (ClassCastException e) {
                // Should only happen if the stub retrieved is not a ClientInterface
                return new ConnectionResult(ConnectionStatus.RMI_LOOKUP_FAILED,
                        "RMI Service found but has wrong type (check stub versions).");
            }

        } finally {
            connectionLock.unlock();
        }
    }

    /** Returns the remote service stub. The caller (Controller) must use this to call remote methods. */
    public ClientInterface getRemoteServiceStub() {
        connectionLock.lock();
        try {
            return remoteServiceStub;
        } finally {
            connectionLock.unlock();
        }
    }
}