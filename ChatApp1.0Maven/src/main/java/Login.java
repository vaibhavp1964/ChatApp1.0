import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

public class Login extends JFrame {
    private JPanel contentPane;
    private JTextField txtLogin;
    private JTextField txtRegister;
    private JTextArea txtrOr;
    private JTextArea prompt;

    private static BufferedReader cu, lu;
    private static String registeredUsersFileName = "registeredUsers.txt";
    private static String loggedUsersFileName = "loggedInUsers.txt";

    /*
    Constructor calling the setting up of the login window
     */
    public Login() {
        createUIComponents();
    }

    private void createUIComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 288, 276);
        setTitle("ChatApp Inc. - Connect");
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        txtLogin = new JTextField();
        txtLogin.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                try {
                    lu = new BufferedReader(new FileReader(loggedUsersFileName));
                    cu = new BufferedReader(new FileReader(registeredUsersFileName));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    if(register(txtLogin.getText()) == false) {
                        prompt.setText("User not registered. Register first");
                        txtLogin.setText("");
                    }
                    else {
                        if (login(txtLogin.getText()) == true) txtLogin.setText("");
                        else renderClientWindow(txtLogin.getText());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        txtLogin.setBounds(84, 25, 166, 20);
        contentPane.add(txtLogin);
        txtLogin.setColumns(10);

        txtRegister = new JTextField();
        txtRegister.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                try {
                    lu = new BufferedReader(new FileReader(loggedUsersFileName));
                    cu = new BufferedReader(new FileReader(registeredUsersFileName));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {
                   if (register(txtRegister.getText()) == true) txtRegister.setText("");
                   else renderClientWindow(txtRegister.getText());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        txtRegister.setBounds(84, 102, 166, 20);
        contentPane.add(txtRegister);
        txtRegister.setColumns(10);

        JTextArea login = new JTextArea();
        login.setEditable(false);
        login.setBackground(UIManager.getColor("Panel.background"));
        login.setText("Login");
        login.setBounds(21, 23, 68, 22);
        contentPane.add(login);


        JTextArea register = new JTextArea();
        register.setBackground(UIManager.getColor("Panel.background"));
        register.setText("Register");
        register.setBounds(10, 100, 68, 22);
        contentPane.add(register);

        txtrOr = new JTextArea();
        txtrOr.setBackground(UIManager.getColor("Panel.background"));
        txtrOr.setText("OR");
        txtrOr.setBounds(114, 68, 20, 22);
        contentPane.add(txtrOr);

        JButton btnConnect = new JButton("Connect");
        btnConnect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                try {
                    lu = new BufferedReader(new FileReader(loggedUsersFileName));
                    cu = new BufferedReader(new FileReader(registeredUsersFileName));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    if(txtLogin.getText().length() != 0) {
                        if(register(txtLogin.getText()) == false) {
                            prompt.setText("User not registered. Register first");
                            txtLogin.setText("");
                        }
                        else {
                            if (login(txtLogin.getText()) == true) txtLogin.setText("");
                            else renderClientWindow(txtLogin.getText());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    if(txtRegister.getText().length() != 0) {
                        if (register(txtRegister.getText()) == true) txtRegister.setText("");
                        else renderClientWindow(txtRegister.getText());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        btnConnect.setBounds(91, 195, 89, 23);
        contentPane.add(btnConnect);

        prompt = new JTextArea();
        prompt.setBackground(UIManager.getColor("Panel.background"));
        prompt.setBounds(7, 149, 255, 22);
        contentPane.add(prompt);

        setVisible(true);
        txtLogin.requestFocusInWindow();
    }

    /*
    Checks whether any client is logged in with a particular client handle
    Reads list of logged in users from a file.
     */
    private boolean login(String input) throws IOException {
        String user = lu.readLine();
        boolean loggedIn = false;

        while(user != null && loggedIn != true) {
            if(user.equals(input) == true) loggedIn = true;
            user = lu.readLine();
        }

        if(loggedIn == true) {
            txtLogin.setText("");
            prompt.setText(input + " already logged in. Logout to login again.");
            return loggedIn;
        }
        return false;
    }

    /*
    Checks whether any client is registered with a particular client handle
    Reads list of registered users from a file
     */
    private boolean register(String input) throws IOException {
        String user = cu.readLine();
        boolean alreadyRegistered = false;

        while(user != null && alreadyRegistered != true) {
            if(user.equals(input) == true) alreadyRegistered = true;
            user = cu.readLine();
        }

        if(alreadyRegistered == true) {
            txtRegister.setText("");
            prompt.setText("Handle already exists. Please choose another.");
            return alreadyRegistered;
        }
        return false;
    }

    /*
    Disposes off the current window and invokes a new client window
     */
    private void renderClientWindow(String clientHandle) {
        dispose();
        new ClientWindow(clientHandle);
    }

    public static void main(String[] args) throws IOException {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    Login frame = new Login();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }
}
