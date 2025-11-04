package ClientMVC.ClientModel;

import Common.Interfaces.ClientInterface;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionModel {

    // Stores the remote service stub (the client's proxy for the server)
    private volatile ClientInterface remoteServiceStub;
    private volatile String ip;
    private volatile int port;
    private volatile boolean connected;
    private final ReentrantLock connectionLock = new ReentrantLock();

    // The binding name must be exactly what the server uses to bind its service
    private static final String RMI_BINDING_NAME = "ChatServer";

    public ConnectionModel() {
        // --- NEW: Auto-Configure RMI Hostname ---
        // We only run this if the user hasn't already set the property manually
        if (System.getProperty("java.rmi.server.hostname") == null) {
            if (!configureRmiHostname()) {
                System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.err.println("FATAL: Could not automatically find a suitable IP for RMI.");
                System.err.println("The application will likely fail to receive callbacks.");
                System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                // You could show an error Alert here
            }
        }
        // --- End New Logic ---
    }

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

    // --- NEW: IP Auto-Finder Logic ---

    /**
     * Tries to find the correct IP and set the RMI hostname property.
     * @return true if a valid IP was found and set, false otherwise.
     */
    private boolean configureRmiHostname() {
        // Priority 1: Find Radmin VPN IP (which starts with 26.)
        String radminIp = findIpByPrefix("26.");
        if (radminIp != null) {
            System.setProperty("java.rmi.server.hostname", radminIp);
            System.out.println("✓ RMI Hostname auto-set to Radmin IP: " + radminIp);
            return true;
        }

        // Priority 2: If no Radmin, find a standard LAN IP (192.168.x.x, 10.x.x.x, etc.)
        String lanIp = findSiteLocalIp();
        if (lanIp != null) {
            System.setProperty("java.rmi.server.hostname", lanIp);
            System.out.println("✓ RMI Hostname auto-set to LAN IP: " + lanIp);
            return true;
        }

        System.err.println("Warning: No Radmin or LAN IP found.");
        return false;
    }

    /**
     * Scans all network interfaces for an IPv4 address starting with the given prefix.
     */
    private String findIpByPrefix(String prefix) {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                // We only want interfaces that are "up" and not loopback
                if (!ni.isUp() || ni.isLoopback()) continue;

                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith(prefix)) {
                            return ip; // Found it
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error while scanning network interfaces: " + e.getMessage());
        }
        return null; // Not found
    }

    /**
     * Scans all network interfaces for a private "site-local" (LAN) IPv4 address.
     */
    private String findSiteLocalIp() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                // Ignore down, loopback, and virtual adapters
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;

                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    // We only want IPv4 addresses that are "site-local" (e.g., 192.168.x.x)
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress(); // Found it
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error while scanning network interfaces: " + e.getMessage());
        }
        return null; // Not found
    }
}