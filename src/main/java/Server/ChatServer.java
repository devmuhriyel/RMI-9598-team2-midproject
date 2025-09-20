package Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ChatServer {

    private final ServerSocket serverSocket;
    private boolean running = true;

    public ChatServer(ServerSocket serverSckt) {
        this.serverSocket = serverSckt;
    }

    public void start() {
        try{
            ServerXMLHandler xmlHandler = new ServerXMLHandler();
            xmlHandler.fixExistingGroupMemberships();
            while(!serverSocket.isClosed()){
                Socket socket = serverSocket.accept();
                System.out.println("A new client has connected");

                ClientThread clientThread = new ClientThread(socket);
                Thread thread = new Thread(clientThread);
                thread.start();
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            System.out.println("Server stopped.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Validates if the given string is a valid IP address
     * @param ip the IP address string to validate
     * @return true if valid, false otherwise
     */
    private static boolean isValidIPAddress(String ip) {
        // Regular expression for valid IPv4 address
        String ipPattern = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";
        return Pattern.matches(ipPattern, ip);
    }

    /**
     * Prompts the operator for an IP address and validates it
     * @param scanner the Scanner object for input
     * @return a valid IP address string
     */
    private static String getValidIPAddress(Scanner scanner) {
        String ipAddress;
        while (true) {
            System.out.print("Please enter the IP address to bind the server to: ");
            ipAddress = scanner.nextLine().trim();

            if (isValidIPAddress(ipAddress)) {
                try {
                    // Additional validation by trying to create InetAddress
                    InetAddress.getByName(ipAddress);
                    System.out.println("Valid IP address: " + ipAddress);
                    return ipAddress;
                } catch (UnknownHostException e) {
                    System.out.println("Error: Invalid IP address format. Please try again.");
                }
            } else {
                System.out.println("Error: Invalid IP address format. Please enter a valid IPv4 address (e.g., 192.168.1.1)");
            }
        }
    }

    //TYPE EXIT IF CLOSEEEE OR PWEDE NAMAN YUNG STOP BUTTON (Wala maangas lang kaya i added it :>)
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        // Get and validate IP address from operator
        String ipAddress = getValidIPAddress(scanner);

        try {
            InetAddress bindAddress = InetAddress.getByName(ipAddress);
            ServerSocket serverSocket = new ServerSocket(1234, 50, bindAddress);

            System.out.println("Server started on IP: " + ipAddress + " Port: 1234");
            System.out.println("Type 'exit' to stop the server.");

            ChatServer chatServer = new ChatServer(serverSocket);

            // Start server in a separate thread so we can still accept commands
            Thread serverThread = new Thread(() -> chatServer.start());
            serverThread.start();

            // Command loop
            while (true) {
                String command = scanner.nextLine();
                if (command.equalsIgnoreCase("exit")) {
                    chatServer.stop();
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error starting server on IP " + ipAddress + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}
