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
    private volatile ClientInterface remoteServiceStub;
    private volatile String ip;
    private volatile int port;
    private volatile boolean connected;
    private final ReentrantLock connectionLock = new ReentrantLock();
    private static final String RMI_BINDING_NAME = "ChatServer";

    public ConnectionModel() {

        if (System.getProperty("java.rmi.server.hostname") == null) {
            if (!configureRmiHostname()) {
                System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.err.println("FATAL: Could not automatically find a suitable IP for RMI.");
                System.err.println("The application will likely fail to receive callbacks.");
                System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

            }
        }
    }

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
            this.remoteServiceStub = null; // Reset stub

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

            try {
                Registry registry = LocateRegistry.getRegistry(ip, port);
                remoteServiceStub = (ClientInterface) registry.lookup(RMI_BINDING_NAME);
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

    public ClientInterface getRemoteServiceStub() {
        connectionLock.lock();
        try {
            return remoteServiceStub;
        } finally {
            connectionLock.unlock();
        }
    }

    private boolean configureRmiHostname() {
        String radminIp = findIpByPrefix("26.");
        if (radminIp != null) {
            System.setProperty("java.rmi.server.hostname", radminIp);
            return true;
        }

        String lanIp = findSiteLocalIp();
        if (lanIp != null) {
            System.setProperty("java.rmi.server.hostname", lanIp);
            return true;
        }

        System.err.println("Warning: No Radmin or LAN IP found.");
        return false;
    }

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
        return null;
    }
    private static String findSiteLocalIp() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;

                String niName = ni.getName().toLowerCase();
                String displayName = ni.getDisplayName() != null ? ni.getDisplayName().toLowerCase() : "";

                // ⚠️ EXPLICITLY FILTER Hyper-V adapters
                if (niName.contains("vethernet") ||      // Hyper-V vEthernet
                        displayName.contains("vethernet") ||
                        displayName.contains("hyper-v") ||
                        displayName.contains("default switch")) {
                    System.out.println("⊗ Skipping virtual adapter: " + ni.getName());
                    continue; // Skip this interface
                }

                // Also filter other virtual adapters
                if (niName.contains("vbox") || niName.contains("vmware") ||
                        niName.contains("docker") || niName.contains("wsl") ||
                        displayName.contains("virtual")) {
                    System.out.println("⊗ Skipping virtual adapter: " + ni.getName());
                    continue;
                }

                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress()) {
                        String ip = addr.getHostAddress();

                        // Prioritize 10.x.x.x and 192.168.x.x
                        if (ip.startsWith("10.") || ip.startsWith("192.168.")) {
                            System.out.println("✓ Selected physical adapter: " + ni.getName() + " → " + ip);
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error finding LAN IP: " + e.getMessage());
        }
        return null;
    }
}