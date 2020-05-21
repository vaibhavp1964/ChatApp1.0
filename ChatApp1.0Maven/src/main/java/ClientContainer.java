import java.net.InetAddress;

public class ClientContainer {

    public String clientHandle;

    int port;
    public InetAddress ip;

    public InetAddress getIp() {
        return this.ip;
    }

    public int getPort() {
        return port;
    }

    public String getClientHandle() {
        return clientHandle;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ClientContainer(String clientHandle, int port, InetAddress ip) {
        this.clientHandle = clientHandle;
        this.port = port;
        this.ip = ip;
    }
}
