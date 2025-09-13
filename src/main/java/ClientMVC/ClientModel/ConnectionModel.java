package ClientMVC.ClientModel;

import ClientMVC.ClientController.LoginController;
import ClientMVC.ClientView.Login.LoginView;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class ConnectionModel {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String ip;
    private int port;
    private boolean connected;

    // Enum for connection results
    public enum ConnectionStatus {
        SUCCESS,
        EMPTY_FIELDS,
        INVALID_PORT_FORMAT,
        INVALID_IP_FORMAT,
        INVALID_PORT_RANGE,
        CONNECTION_FAILED
    }

    // Result wrapper class
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

    //MVC METHODS
    public ConnectionResult connect(String ip, String portText) {
        // Reset state before new attempt
        this.connected = false;
        this.socket = null;
        this.in = null;
        this.out = null;

        // Validate empty fields
        if (ip == null || ip.trim().isEmpty()) {
            return new ConnectionResult(ConnectionStatus.EMPTY_FIELDS, "IP address cannot be empty");
        }
        if (portText == null || portText.trim().isEmpty()) {
            return new ConnectionResult(ConnectionStatus.EMPTY_FIELDS, "Port cannot be empty");
        }

        // Clean inputs
        ip = ip.trim();
        portText = portText.trim();

        // Validate port format
        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            return new ConnectionResult(ConnectionStatus.INVALID_PORT_FORMAT, "Port must be a valid number");
        }

        // Validate port range
        if (port < 1 || port > 65535) {
            return new ConnectionResult(ConnectionStatus.INVALID_PORT_RANGE, "Port must be between 1 and 65535");
        }

        // Validate IP format
        String ipv4Pattern = "^(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)" +
                "(\\.(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)){3}$";

        if (!ip.matches(ipv4Pattern)) {
            return new ConnectionResult(ConnectionStatus.INVALID_IP_FORMAT, "Invalid IP address format");
        }

        // Attempt connection
        try {
            socket = new Socket(ip, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            this.ip = ip;
            this.port = port;
            this.connected = true;

            // Launch login view on successful connection
            Platform.runLater(() -> {
                try {
                    LoginView lView = new LoginView();
                    Stage stage = new Stage();
                    lView.start(stage);
                    LoginModel lModel = new LoginModel(getSocket());
                    new LoginController(lView, lModel, lView.getFXController());
                    new Thread(lModel, "Logged In").start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return new ConnectionResult(ConnectionStatus.SUCCESS, "Connected successfully");

        } catch (IOException e) {
            this.connected = false;
            this.socket = null;
            this.in = null;
            this.out = null;
            return new ConnectionResult(ConnectionStatus.CONNECTION_FAILED,
                    "Failed to connect: " + e.getMessage());
        }
    }

    // Legacy method for backward compatibility
    public boolean connect(String ip, int port) {
        ConnectionResult result = connect(ip, String.valueOf(port));
        return result.getStatus() == ConnectionStatus.SUCCESS;
    }

    //SETTERS AND GETTERS
    public Socket getSocket() { return socket; }
    public boolean isConnected() { return connected; }
    public String getIp() { return ip; }
    public int getPort() { return port; }
}