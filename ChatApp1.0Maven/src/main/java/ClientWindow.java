import java.awt.EventQueue;
import java.awt.event.*;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.JButton;
import javax.swing.UIManager;

public class ClientWindow extends JFrame implements  Runnable{

    private JPanel contentPane;
    private JTextField txtConnectedUsers;
    private JTextField txtMessage;
    private JTextArea txtHistoryPanel;
    private JTextArea connectedUsersPanel;

    private Client client;

    private Thread listen, run;

    private boolean running = false;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    ClientWindow frame = new ClientWindow("");
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public ClientWindow(String clientHandle) {
        client = new Client(clientHandle);
        boolean connect = client.openConnection();

        if (!connect) {
            System.err.println("Connection failed!");
            printToConsole("Connection failed!");
        }

        createClientWindow(clientHandle);

        client.clientToServerCommunication("User @" + client.getHandle() + " is connected");
        client.clientToServerCommunication("/r/" + (new Message(clientHandle, " ", " ")).encode() + "/e/");
        client.clientToServerCommunication("/l/" + (new Message(clientHandle, " ", " ")).encode() + "/e/");

        running = true;
        run = new Thread(this, "Running");
        run.start();
    }

    public void createClientWindow(String clientHandle) {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setTitle("ChatApp Inc. - " + clientHandle);
        setBounds(100, 100, 800, 411);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        connectedUsersPanel = new JTextArea();
        connectedUsersPanel.setBackground(UIManager.getColor("Panel.background"));
        JScrollPane scrollConnectedUsers = new JScrollPane(connectedUsersPanel);
        scrollConnectedUsers.setBounds(10, 29, 131, 332);
        contentPane.add(scrollConnectedUsers);

        txtConnectedUsers = new JTextField();
        txtConnectedUsers.setBackground(UIManager.getColor("Panel.background"));
        txtConnectedUsers.setHorizontalAlignment(SwingConstants.CENTER);
        txtConnectedUsers.setText("Connected Users");
        txtConnectedUsers.setBounds(10, 6, 131, 20);
        contentPane.add(txtConnectedUsers);
        txtConnectedUsers.setColumns(10);

        txtHistoryPanel = new JTextArea();
        txtHistoryPanel.setEditable(false);
        JScrollPane scroll = new JScrollPane(txtHistoryPanel);
        scroll.setBounds(151, 6, 623, 318);
        contentPane.add(scroll);

        txtMessage = new JTextField();
        txtMessage.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent arg0) {
                if(arg0.getKeyCode() == KeyEvent.VK_ENTER) {
                    String outgoingMessage = txtMessage.getText();
                    txtMessage.setText("");
                    String formattedMessage = formatOutgoingMessage(outgoingMessage);
                    if(formattedMessage == null) {
                        printToConsole("Initialize messages with @handle.");
                    }
                    else {
                        formattedMessage = "/m/" + formattedMessage + "/e/";
                        printToConsole("Me: " + outgoingMessage);
                        client.clientToServerCommunication(formattedMessage);
                    }
                }
            }
        });
        txtMessage.setBounds(151, 335, 547, 26);
        contentPane.add(txtMessage);
        txtMessage.setColumns(10);

        JButton btnSend = new JButton("Send");
        btnSend.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent arg0) {
                String outgoingMessage = txtMessage.getText();
                txtMessage.setText("");
                String formattedMessage = formatOutgoingMessage(outgoingMessage);
                if(formattedMessage == null) {
                    printToConsole("Initialize messages with @handle.");
                }
                else {
                    formattedMessage = "/m/" + formattedMessage + "/e/";
                    printToConsole("Me: " + outgoingMessage);
                    client.clientToServerCommunication(formattedMessage);
                }
            }
        });
        btnSend.setBounds(708, 335, 66, 26);
        contentPane.add(btnSend);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                Message message = new Message(clientHandle, "", "");
                String outgoingMessage = "/lo/" + message.encode() + "/e/";
                client.clientToServerCommunication(outgoingMessage);
                running = false;
                client.close();
            }
        });

        setVisible(true);
        txtMessage.requestFocusInWindow();
    }

    /*
    Creates a message object from a string and then encodes the same in a JSON string.
    Formats only the /m/ messages.
     */
    public String formatOutgoingMessage(String text) {
        if(text.startsWith("@") == false) {
            printToConsole("No recipient selected for message: " + text);
            return null;
        }

        String[] messageComponentArray = text.split(" ");

        String receiver = messageComponentArray[0].substring(1);
        String sender = client.getHandle();
        String content = "";

        for(int i = 1; i < messageComponentArray.length; i++) {
            content += messageComponentArray[i] + " ";
        }
        content = content.trim();

        Message outgoingMessage = new Message(sender, receiver, content);
        String outgoingEncodedMessage = outgoingMessage.encode();

        return outgoingEncodedMessage;
    }

    public void printToConsole(String text) {
        txtHistoryPanel.append(text + "\n");
    }

    private void printToConnectivityPanel(String clientHandles) {
        connectedUsersPanel.append(clientHandles);
    }

    public void listen() {
        listen = new Thread("Listen") {
            public void run() {
                while(running) {
                    String incomingData = client.receivePacket();
                    handleDataFromServer(incomingData);
                }
            }
        };
        listen.start();
    }

    /*
    Method is called to redirect incoming messages to different methods based on their identifiers.

    /logged/ messages originate from the server and their content is displayed on the active user's pane.
    /m/ messages can originate either from the server or another client and are displayed on the chat console.
     */
    public void handleDataFromServer(String incomingString) {
        if(incomingString.startsWith("/m/")) {                              // /m/ for typical message from client to client redirected via server
            incomingString = incomingString.split("/m/")[1];
            incomingString = incomingString.split("/e/")[0];
            receivedGeneralMessage(incomingString);
        }
        if(incomingString.startsWith("/logged/")) {                         // /logged/ for communicating the list of active users.
            incomingString = incomingString.split("/logged/")[1];
            incomingString = incomingString.split("/e/")[0];
            receivedLoggedInMessage(incomingString);
        }
    }


    /*
    Receives a list of logged in queries from the server in the form of a string.
    Gets called in case of a server to client communication where string sent by the server is flagged with /logged/ identifier

    Client logs in -> /l/ message sent from the client to the server.
    Server receives the /l/ message from a client -> updates the database of logged-in users.
    Server -> sends /logged/ message to every connected client for displaying the list on their consoles.
     */
    private void receivedLoggedInMessage(String incomingString) {
        connectedUsersPanel.setText("");
        printToConnectivityPanel(Message.decode(incomingString).getContent());
    }

    /*
    Receives encoded JSON string containing a message from the server.
    This method is called for displaying messages on the active chat console.
    Every message has a sender, a receiver and content.
    Messages from the server are marked with sender = Server

    User types something in -> formatOutgoingMessage method converts the simple string into an encoded JSON string.
    Since it is a general message it is appended with a /m/ identifier.
    Server receives a data packet containing a string with /m/ identifier.
    Server deconstructs the string into a Message object to get the receiver's information.
    Server then repackages the Message object into an encoded string and sends it to the intended receiver.
     */
    private void receivedGeneralMessage(String incomingString) {        // Can be message from another client or from the server.
        Message message = Message.decode(incomingString);
        if(message.getSender().equals("Server")) {
            printMessageFromServer(message);
            return;
        }
        String displayMessage = "@" + message.getSender() + " says: " + message.getContent();   //messages from another client
        printToConsole(displayMessage);
    }

    private void printMessageFromServer(Message message) {              // messages from the server are redirected here.
        String displayMessage = "[SERVER: " + message.getContent() + "]";
        printToConsole(displayMessage);
    }

    @Override
    public void run() {
        listen();
    }
}
