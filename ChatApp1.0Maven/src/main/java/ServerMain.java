import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ServerMain {

    private int port;
    private Server server;

    private static PrintWriter cu, lu;
    private static String registeredUsersFileName = "registeredUsers.txt";
    private static String loggedUsersFileName = "loggedInUsers.txt";

    private int getDefaultValueForPort() {
        return 8192;
    }

    public ServerMain() throws IOException {
        this.port = getDefaultValueForPort();
        server = new Server(port);
    }

    public static void main(String[] args) throws IOException {
        cu = new PrintWriter(new FileWriter(registeredUsersFileName));
        cu.println();
        cu.close();

        lu = new PrintWriter(new FileWriter(loggedUsersFileName));
        lu.println();
        lu.close();

        new ServerMain();
    }
}
