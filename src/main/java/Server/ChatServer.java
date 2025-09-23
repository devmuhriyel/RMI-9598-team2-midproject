package Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ChatServer {

    private final ServerSocket serverSocket;

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
    /*
     * stop() and isValidIPAddress():
     * ------------------------------
     * stop():
     * - Purpose: Safely stops the server by closing the server socket if it’s running.
     * - Steps:
     *   1. Set 'running' to false.
     *   2. Close the server socket if it exists and isn’t already closed.
     *   3. Print "Server stopped." or show error if closing fails.
     *
     * isValidIPAddress(ip):
     * - Purpose: Checks if a given string is a valid IPv4 address.
     * - Logic: Uses a regular expression to match 4 numbers (0–255) separated by dots.
     * - Returns: true if valid, false otherwise.
     */
    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            System.out.println("Server stopped.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*
     * main():
     * -------
     * Purpose:
     *   - Entry point of the server program.
     * How it works:
     *   1. Ask the user for a valid IP address.
     *   2. Bind the server socket to that IP and port 1234.
     *   3. Start the ChatServer in a separate thread so it runs in the background.
     *   4. Provide a command loop where typing "exit" stops the server.
     *   5. Ensure resources like the scanner are closed properly.
     */
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        String ipAddress = getValidIPAddress(scanner);
        try {
            InetAddress bindAddress = InetAddress.getByName(ipAddress);
            ServerSocket serverSocket = new ServerSocket(1234, 50, bindAddress);
            System.out.println("Server started on IP: " + ipAddress + " Port: 1234");
            System.out.println("Type 'exit' to stop the server.");
            ChatServer chatServer = new ChatServer(serverSocket);
            Thread serverThread = new Thread(chatServer::start);
            serverThread.start();
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

    private static boolean isValidIPAddress(String ip) {
        String ipPattern =
                "^(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\." +
                        "(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\." +
                        "(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\." +
                        "(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$";

        return Pattern.matches(ipPattern, ip);
    }
    /*
     * getValidIPAddress(scanner):
     * ---------------------------
     * Purpose:
     *   - Continuously prompts the user to enter a valid IPv4 address.
     *   - Ensures the input is correctly formatted and can be resolved as an IP.
     * How it works:
     *   1. Ask the user for an IP address.
     *   2. Trim the input and check with isValidIPAddress().
     *   3. If valid, also test using InetAddress.getByName() for extra validation.
     *   4. If everything is fine → return the IP address.
     *   5. Otherwise, show error and ask again until a correct IP is provided.
     */

    //HELPER METHODS FOR CHAT SERVER
    private static String getValidIPAddress(Scanner scanner) {
        String ipAddress;
        while (true) {
            System.out.print("Please enter the IP address to bind the server to: ");
            ipAddress = scanner.nextLine().trim();

            if (isValidIPAddress(ipAddress)) {
                System.out.println("Valid IP address: " + ipAddress);
                return ipAddress;
            } else {
                System.out.println("Error: Invalid IP address format. Please enter a valid IPv4 address (e.g., 192.168.1.1)");
            }
        }
    }
}
