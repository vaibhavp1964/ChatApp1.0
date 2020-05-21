import com.google.gson.Gson;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

class Server implements Runnable{
    private int port;
    private DatagramSocket socket;

    private Thread run;
    private Thread send, receive;
    private boolean isServerRunning = false;

    private static Gson gson = new Gson();

    public static PrintWriter ru, lu;
    private static String registeredUsersFile = "registeredUsers.txt";
    private static String loggedInUsersFile = "loggedInUsers.txt";

    private static int messageLimit = 2;

    private HashMap<String, ClientContainer> registeredClients = new HashMap<String, ClientContainer>();
    private List<String> connectedUsers = new ArrayList<String>();
    private HashMap<String, Queue<String>> storedMessage = new HashMap<String, Queue<String>>();

    public Server(int port) {
        this.port = port;
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }

        run = new Thread(this, "Server");
        run.start();
    }

    public void run() {
        isServerRunning = true;
        System.out.println("Server started on port = " + port);
        receive();
    }

    public void receive() {
        receive = new Thread("receive") {
            public void run() {
                while (isServerRunning != false) {
                    byte[] inboundData = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(inboundData, inboundData.length);

                    try {
                        socket.receive(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    int port = packet.getPort();
                    InetAddress address = packet.getAddress();
                    String receivedString = new String(packet.getData());

                    try {
                        processIncomingMessageText(receivedString, address, port);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        };
        receive.start();
    }

    /*
    Processes incoming client request based on identifiers at the start of the incoming string
    Identifiers ->  /m/ -> general message query intended for client to client communication
                    /r/ -> register query from clients requesting registration by the server
                    /l/ -> login query from clients requesting the server to mark them logged in
                    /lo/ -> logout query from clients requesting the server to mark them logged out
     */
    public void processIncomingMessageText(String incomingString, InetAddress address, int port) throws IOException {
        if(incomingString.startsWith("/m/")) {                          // /m/ for typical message from client to client redirected via server
            incomingString = incomingString.split("/m/")[1];
            incomingString = incomingString.split("/e/")[0];
            handleGeneralMessageQuery(incomingString);
        }
        if(incomingString.startsWith("/r/")) {                          // /r/ for registering a user
            incomingString = incomingString.split("/r/")[1];
            incomingString = incomingString.split("/e/")[0];
            registerUser(Message.decode(incomingString), address, port);
        }
        if(incomingString.startsWith("/l/")) {                          // /l/ for logging a user in
            incomingString = incomingString.split("/l/")[1];
            incomingString = incomingString.split("/e/")[0];
            loginRequest(Message.decode(incomingString), address, port);
        }
        if(incomingString.startsWith("/lo/")) {                          // /lo/ for logging a user out
            incomingString = incomingString.split("/lo/")[1];
            incomingString = incomingString.split("/e/")[0];
            logoutRequest(Message.decode(incomingString));
        }
    }

    /*
    Broadcasts information about online availability of all clients to all clients.
    Marked with a /logged/ identifier
     */
    public void broadcastConnectedUsers() {
        for (int i = 0; i < connectedUsers.size(); i++) {
            ClientContainer client = registeredClients.get(connectedUsers.get(i));
            String broadcast = "";
            for (int j = 0; j < connectedUsers.size(); j++) {
                if(connectedUsers.get(j).equals(client.getClientHandle()) == false) {
                    broadcast += connectedUsers.get(j) + "\n";
                }
            }
            Message message = new Message("Server", connectedUsers.get(i), broadcast);
            broadcast = "/logged/" + message.encode();
            messageFromServerToClient(registeredClients.get(connectedUsers.get(i)), broadcast);
        }
    }

    /*
    Handles general messages sent by one client to another client
    Handles cases where ->  Recipient is logged in
                            Recipient is logged out but registered
                                -> Space is available in recipient's queue
                                -> Recipient's queue is full
                            Recipient is not registered
     */
    private void handleGeneralMessageQuery(String encodedString) {
        Message message = Message.decode(encodedString);

        String sender = message.getSender();
        String receiver = message.getReceiver();

        if(sender.equals(receiver) == true) return;

        if(connectedUsers.contains(receiver) == true) {                 // if intended receiver is logged in
            messageFromServerToClient(registeredClients.get(receiver), "/m/" + encodedString);
        }
        else {
            if(registeredClients.containsKey(receiver) == true) {       // if intended receiver is registered but not logged in
                if(storedMessage.get(receiver).size() < messageLimit) { // if queue storing messages is not full
                    storedMessage.get(receiver).add("/m/" + encodedString);
                    sendOfflineInfoToSender(sender, receiver);
                }
                else {                                                  // if queue storing messages is full and earlier messages have to be purged
                    storedMessage.get(receiver).poll();
                    storedMessage.get(receiver).add("/m/" + encodedString);
                }
            }
            else {
                noSuchUser(sender, receiver);                           // if no such user is registered
            }
        }
    }

    /*
    Sends an informational message when a client tries to send a message to an unregistered user.
     */
    private void noSuchUser(String sender, String receiver) {
        Message message = new Message("Server", sender, "No such registered user exists.");
        messageFromServerToClient(registeredClients.get(sender), "/m/" + message.encode());
    }

    /*
    Sends an informational message when a client sends a message to an offline client.
     */
    private void sendOfflineInfoToSender(String sender, String receiver) {
        Message message = new Message("Server", sender, receiver + " is currently logged out. Enqueuing this message.");
        messageFromServerToClient(registeredClients.get(sender), "/m/" + message.encode());
    }

    /*
    Request initiated by a new instance of client with already registered clientHandle marked with a /l/ identifier.
    Actions ->  Mark the client logged in
                Update client container's information with new port number
                Update map of client containers with the new port information
                Broadcast availability of client to all clients
                Send messages addressed to this client when it was offline
                Update database of logged in users for login window verification
     */
    private void loginRequest(Message message, InetAddress address, int port) throws IOException {
        // if client is registered AND not logged in
        if(registeredClients.containsKey(message.getSender()) == true && connectedUsers.contains(message.getSender()) == false) {
            connectedUsers.add(message.getSender());                                            // mark client logged in

            ClientContainer clientContainer = registeredClients.get(message.getSender());
            clientContainer.setPort(port);                                                      // updating port which is auto generated for every client window
            registeredClients.put(message.getSender(), clientContainer);                        // updating information about client in the list of registered clients

            broadcastConnectedUsers();                                                          // broadcast message to all clients about new clients availability

            System.out.println("User logged in with name: " + message.getSender() + ", address: " + address + ", port: " + port);   // For server's log

            sendStoredMessages(message.getSender());                                            // send messages which have been sent to client when it was offline

            lu = new PrintWriter(new FileWriter("loggedInUsers.txt"));
            for (int i = 0; i < connectedUsers.size(); i++) {
                lu.println(connectedUsers.get(i));                                              // update database of logged in users
            }
            lu.flush();
            lu.close();
        }
    }

    /*
        Sends messages received by the client when it was offline one by one
        The messages are prefixed with a header and suffixed with a footer for easier identification.
     */
    private void sendStoredMessages(String sender) {
        if(storedMessage.get(sender).isEmpty() == true) return;     // return if no messages were sent to it during offline mode

        Message header = new Message("Server", registeredClients.get(sender).getClientHandle(), "Retrieving enqueued messages.");
        messageFromServerToClient(registeredClients.get(sender), "/m/" + header.encode());  // server header message

        Queue<String> queue = storedMessage.get(sender);
        while(queue != null && queue.isEmpty() != true) {
            messageFromServerToClient(registeredClients.get(sender), queue.poll());                 // send queued messages one by one
        }

        Message footer = new Message("Server", registeredClients.get(sender).getClientHandle(), "Retrieved all messages.");
        messageFromServerToClient(registeredClients.get(sender), "/m/" + footer.encode());  // server footer message
    }

    /*
    Handles request for logging off
    Actions ->  Updates the list of logged in users maintained by the server
                Broadcasts client's unavailability to all online clients.
                Updates database of logged in users for purpose of login window verification
     */
    private void logoutRequest(Message message) throws IOException {
        String sender = message.getSender();

        for (int i = 0; i < connectedUsers.size(); i++) {
            if(connectedUsers.get(i).equals(sender)) {
                connectedUsers.remove(i);                       // update list of logged in users
            }
        }
        broadcastConnectedUsers();                              // broadcast client's unavailability to all online users
        System.out.println("User logged out with name: " + message.getSender());    // For server's log

        lu = new PrintWriter(new FileWriter("loggedInUsers.txt"));      // updating database of logged in users for login window verification
        for (int i = 0; i < connectedUsers.size(); i++) {
            lu.println(connectedUsers.get(i));
        }
        lu.flush();
        lu.close();
    }

    /*
    Actions ->  Creates a new client container and updates the map which maps clientHandle with client container.
                Updates the database of registered users for the purpose of login window verification.
                Initializes the message queue for the particular client for storing messages received when it is offline.
     */
    private void registerUser(Message message, InetAddress address, int port) throws IOException {
        if(registeredClients.containsKey(message.getSender()) == false) {
            System.out.println("User registered with name: " + message.getSender() + ", address: " + address + ", port: " + port);  // For server's log

            registeredClients.put(message.getSender(), new ClientContainer(message.getSender(), port, address));

            ru = new PrintWriter(new FileWriter("registeredUsers.txt", true));
            if (message.getSender().equals("null") == false) ru.println(message.getSender());
            ru.flush();
            ru.close();

            storedMessage.put(message.getSender(), new ArrayBlockingQueue<String>(messageLimit));
        }
    }

    /*
    Sends information to clients in the form of a byte array using the clients IP address and port.
     */
    private void sendByteArrayToClient(final InetAddress ip, final int port, final String encodedMessage) {
        byte[] dataArray = encodedMessage.getBytes();

        send = new Thread("send") {
            public void run() {
                DatagramPacket packet = new DatagramPacket(dataArray, dataArray.length, ip, port);
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("SendByteArrayToClient");
                }
            }
        };
        send.start();
    }

    /*
    Wrapper method for sendByteArrayToClient method
     */
    private void messageFromServerToClient(ClientContainer client, String message) {
        message += "/e/";
        sendByteArrayToClient(client.getIp(), client.getPort(), message);
    }
}