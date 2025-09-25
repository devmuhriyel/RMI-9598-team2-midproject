package ClientMVC.ClientModel;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionModel {
    private volatile Socket socket;
    private volatile String ip;
    private volatile int port;
    private volatile boolean connected;
    private final ReentrantLock connectionLock = new ReentrantLock();

    public enum ConnectionStatus {
        SUCCESS,
        EMPTY_FIELDS,
        INVALID_PORT_FORMAT,
        INVALID_IP_FORMAT,
        INVALID_PORT_RANGE,
        CONNECTION_FAILED
    }

    public static class ConnectionResult {
        private final ConnectionStatus status;
        private final String message;

        public ConnectionResult(ConnectionStatus status, String message) {
            this.status = status;
            this.message = message;
        }

        public ConnectionStatus getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }
    /*
     * connect method:
     * 1. Lock the connection to avoid race conditions between threads.
     * 2. Reset the state and close any existing socket before starting a new connection attempt.
     * 3. Validate that the IP address and port text fields are not empty.
     * 4. Trim input values to remove accidental whitespace.
     * 5. Validate that the port is a number; if not, return an INVALID_PORT_FORMAT error.
     * 6. Check that the port number is within the valid range (1–65535).
     * 7. Validate that the IP matches a valid IPv4 address pattern.
     * 8. If validations pass, attempt to create a new socket connection with the given IP and port.
     * 9. If successful → set connected state to true and return SUCCESS.
     * 10. If connection fails → return CONNECTION_FAILED with the error message.
     * 11. Finally, unlock the connection lock whether success or failure.
     */
    public ConnectionResult connect(String ip, String portText) {
        connectionLock.lock();
        try {
            this.connected = false;
            if (this.socket != null && !this.socket.isClosed()) {
                try {
                    this.socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            this.socket = null;
            if (ip == null || ip.trim().isEmpty()) {
                return new ConnectionResult(ConnectionStatus.EMPTY_FIELDS, "IP address cannot be empty");
            }
            if (portText == null || portText.trim().isEmpty()) {
                return new ConnectionResult(ConnectionStatus.EMPTY_FIELDS, "Port cannot be empty");
            }
            ip = ip.trim();
            portText = portText.trim();
            int port;
            try {
                port = Integer.parseInt(portText);
            } catch (NumberFormatException e) {
                return new ConnectionResult(ConnectionStatus.INVALID_PORT_FORMAT, "Port must be a valid number");
            }
            if (port < 1 || port > 65535) {
                return new ConnectionResult(ConnectionStatus.INVALID_PORT_RANGE, "Port must be between 1 and 65535");
            }
            String ipv4Pattern = "^(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)" +
                    "(\\.(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)){3}$";

            if (!ip.matches(ipv4Pattern)) {
                return new ConnectionResult(ConnectionStatus.INVALID_IP_FORMAT, "Invalid IP address format");
            }
            try {
                socket = new Socket(ip, port);
                this.connected = true;
                this.ip = ip;
                this.port = port;
                return new ConnectionResult(ConnectionStatus.SUCCESS, "Connected successfully");
            } catch (IOException e) {
                this.connected = false;
                this.socket = null;
                return new ConnectionResult(ConnectionStatus.CONNECTION_FAILED,
                        "Failed to connect: " + e.getMessage());
            }
        } finally {
            connectionLock.unlock();
        }
    }

    public Socket getSocket() {
        connectionLock.lock();
        try {
            return socket;
        } finally {
            connectionLock.unlock();
        }
    }
}