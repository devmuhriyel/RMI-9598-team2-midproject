package AdminMVC.AdminModel;

import Common.Interfaces.AdminInterface;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionModel {
    // Stores the remote service stub
    private volatile AdminInterface remoteServiceStub;
    private volatile String ip;
    private volatile int port;
    private volatile boolean connected;
    private final ReentrantLock connectionLock = new ReentrantLock();

    // The binding name must be exactly what the server uses
    private static final String RMI_BINDING_NAME = "ChatServer";

    public ConnectionModel() {
        // Check if user manually set the hostname
        String manualHostname = System.getProperty("java.rmi.server.hostname");

        if (manualHostname == null || manualHostname.trim().isEmpty()) {
            System.out.println("⚠ No manual RMI hostname set, attempting auto-configuration...");
            if (!configureRmiHostname()) {
                System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.err.println("FATAL: Could not automatically find a suitable IP for RMI.");
                System.err.println("Please set -Djava.rmi.server.hostname=YOUR_IP manually");
                System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            } else {
                System.out.println("✓ Auto-configured RMI hostname: " +
                        System.getProperty("java.rmi.server.hostname"));
            }
        } else {
            System.out.println("✓ Using manual RMI hostname: " + manualHostname);
        }
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

            if (!ip.matches(ipv4Pattern) && !ip.equalsIgnoreCase("localhost")) {
                return new ConnectionResult(ConnectionStatus.INVALID_IP_FORMAT, "Invalid IP address format");
            }

            // --- RMI Connection Logic ---
            try {
                // 1. Get the reference to the RMI Registry
                Registry registry = LocateRegistry.getRegistry(ip, port);

                // 2. Look up the remote object and cast it
                remoteServiceStub = (AdminInterface) registry.lookup(RMI_BINDING_NAME);

                this.connected = true;
                this.ip = ip;
                this.port = port;
                return new ConnectionResult(ConnectionStatus.SUCCESS, "RMI service successfully retrieved.");

            } catch (java.rmi.ConnectException e) {
                return new ConnectionResult(ConnectionStatus.CONNECTION_FAILED,
                        "Failed to connect to RMI Registry: " + e.getMessage());
            } catch (java.rmi.NotBoundException e) {
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

    /** Returns the remote service stub. */
    public AdminInterface getRemoteServiceStub() {
        connectionLock.lock();
        try {
            return remoteServiceStub;
        } finally {
            connectionLock.unlock();
        }
    }

    public int getPort() { return port; }


    private boolean configureRmiHostname() {
        // Try to get the actual IP that can reach the server
        String detectedIp = null;

        // First try: Get IP from network interfaces
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;

                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        System.out.println("  Found network interface IP: " + ip + " on " + ni.getName());

                        // Prefer 192.168.x.x or 10.x.x.x ranges
                        if (ip.startsWith("192.168.") || ip.startsWith("10.")) {
                            detectedIp = ip;
                            break;
                        }

                        // Fallback to any non-loopback
                        if (detectedIp == null) {
                            detectedIp = ip;
                        }
                    }
                }
                if (detectedIp != null && (detectedIp.startsWith("192.168.") || detectedIp.startsWith("10."))) {
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error scanning network interfaces: " + e.getMessage());
        }

        if (detectedIp != null) {
            System.setProperty("java.rmi.server.hostname", detectedIp);
            System.out.println("✓ Set RMI hostname to: " + detectedIp);
            return true;
        }

        return false;
    }

    private String findIpByPrefix(String prefix) {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith(prefix)) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error while scanning network interfaces: " + e.getMessage());
        }
        return null;
    }

    private String findSiteLocalIp() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error while scanning network interfaces: " + e.getMessage());
        }
        return null;
    }
}