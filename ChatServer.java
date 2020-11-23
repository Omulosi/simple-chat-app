package chat;

import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.util.*;


public class ChatServer {
    // setup the database connection
    ChatDB db = new ChatDB("root", "");
    public static ArrayList<Socket> clientSockets = new ArrayList<>();
    ArrayList<String> messageHistory;

    public ChatServer() throws ClassNotFoundException {
        // Get all messages from the databases
        messageHistory = new ArrayList<>();
        try {
            messageHistory = db.getMessages();
            System.out.println(messageHistory);
        } catch (SQLException ex) {
            System.out.println("Problem loading message history...");
            System.out.println(ex.getMessage());
        }

    }

    public static void main (String[] args) throws ClassNotFoundException {
        new ChatServer().run();
    }

    public void run () {
        System.out.println("Listening on port 5000...");

        try {
            // Create server port 5000
            ServerSocket serverSocket = new ServerSocket(5000);

            // Continuously listen to client requests
            while (true){
                // get socket connection for current client
                Socket clientSocket = serverSocket.accept();

                // send history of chat to connected client
                sendChatHistory(clientSocket);

                clientSockets.add(clientSocket);

                // Create a Thread for handling response to the client
                //
                // The job passed to the Thread is an instance of
                // the ClientHandler class defined within this class.
                //
                Thread t = new Thread(new ClientHandler(clientSocket));
                t.start();

                System.out.println("Got connection...");
            }
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    // sends chat messages/history to all clients currently connected to
    // the server.
    // Each client is represented by the argument 's', a Socket.
    public void broadcast(Socket s, String msg) throws IOException {
        PrintWriter writer = new PrintWriter(s.getOutputStream());
        writer.println(msg); // send message to the client
        writer.flush();
        System.out.println(msg);

    }

    /* Send the chat history to client connected to Socket s */
    public void sendChatHistory (Socket s) {
        try {
            // send history of chat to that client
            if (! messageHistory.isEmpty()) {
                for (String m: messageHistory) {
                    broadcast(s, m);
                }
            }
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public class ClientHandler implements Runnable {
        BufferedReader reader;
        Socket sock;

        // Constructor - sets up reader and sock fields needed for reading data sent by client.
        public ClientHandler (Socket clientSocket) {
            try {
                // set up client socket
                sock = clientSocket;

                // Create a BufferedReader object for reading messages from client
                InputStreamReader isReader = new InputStreamReader(sock.getInputStream());
                reader = new BufferedReader(isReader);

            }  catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
        }

        @Override
        public void run() {
            String message;
            try {
                while ((message = reader.readLine()) != null) {
                    // Extract username and message from text sent by client
                    // That message is sent with the format: '[username] : message'
                    String[] parts = message.split(":");
                    String username = parts[0];
                    String msg = parts[1];

                    // store the client's message in the database
                    db.addMessage(msg, username);

                    // send message to all clients
                    String msgToSend = String.format("[%s]: %s", username, msg);

                    System.out.println(msgToSend);

                    for (Socket s: clientSockets) {
                        broadcast(s, msgToSend);
                    }

                }
            } catch (SQLException ex) {
                System.err.println(ex.getMessage());;
            }
            catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
        }
    }
}
