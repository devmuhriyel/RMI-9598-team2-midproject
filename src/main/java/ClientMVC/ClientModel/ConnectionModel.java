package ClientMVC.ClientModel;

import ClientMVC.ClientCallbackImplementation;

import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.util.concurrent.locks.ReentrantLock;

import Common.Interfaces.ClientInterface;

public class ConnectionModel {

    private volatile ClientInterface remoteServiceStub;
    private volatile ClientCallbackImplementation callback;
    private volatile String ip;
    private volatile int port;
    private volatile boolean connected;
    private final ReentrantLock connectionLock = new ReentrantLock();

    private static final String RMI_BINDING_NAME = "ChatServer";

    public enum ConnectionStatus {
        SUCCESS,
        EMPTY_FIELDS,
        INVALID_PORT_FORMAT,
        INVALID_IP_FORMAT,
        INVALID_PORT_RANGE,
        CONNECTION_FAILED,
        RMI_LOOKUP_FAILED
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
            this.remoteServiceStub = null;
            this.callback = null;

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
            if (!ip.matches(ipv4Pattern) && !ip.equalsIgnoreCase("localhost")) {
                return new ConnectionResult(ConnectionStatus.INVALID_IP_FORMAT, "Invalid IP address format.");
            }

            // --- RMI Connection Logic ---
            try {
                // 1. Get the reference to the RMI Registry
                Registry registry = LocateRegistry.getRegistry(ip, port);

                // 2. Look up the remote object
                remoteServiceStub = (ClientInterface) registry.lookup(RMI_BINDING_NAME);

                // 3. Create the callback implementation (ONCE per connection)
                try {
                    callback = new ClientCallbackImplementation();
                    System.out.println("Callback created successfully!");
                } catch (RemoteException e) {
                    return new ConnectionResult(ConnectionStatus.CONNECTION_FAILED,
                            "Failed to create callback: " + e.getMessage());
                }

                this.connected = true;
                this.ip = ip;
                this.port = port;
                return new ConnectionResult(ConnectionStatus.SUCCESS, "RMI service successfully retrieved.");

            } catch (ConnectException e) {
                return new ConnectionResult(ConnectionStatus.CONNECTION_FAILED,
                        "Failed to connect to RMI Registry: " + e.getMessage());
            } catch (NotBoundException e) {
                return new ConnectionResult(ConnectionStatus.RMI_LOOKUP_FAILED,
                        "Service '" + RMI_BINDING_NAME + "' not found in Registry.");
            } catch (RemoteException e) {
                return new ConnectionResult(ConnectionStatus.CONNECTION_FAILED,
                        "RMI Communication Error: " + e.getMessage());
            } catch (ClassCastException e) {
                return new ConnectionResult(ConnectionStatus.RMI_LOOKUP_FAILED,
                        "RMI Service found but has wrong type (check stub versions).");
            }

        } finally {
            connectionLock.unlock();
        }
    }

    /** Returns the remote service stub */
    public ClientInterface getRemoteServiceStub() {
        connectionLock.lock();
        try {
            return remoteServiceStub;
        } finally {
            connectionLock.unlock();
        }
    }

    /** Returns the callback implementation */
    public ClientCallbackImplementation getCallback() {
        connectionLock.lock();
        try {
            return callback;
        } finally {
            connectionLock.unlock();
        }
    }

    public boolean isConnected() {
        connectionLock.lock();
        try {
            return connected;
        } finally {
            connectionLock.unlock();
        }
    }
}