import java.io.IOException;
import java.net.*;

public class Client {

    private String handle;
    private DatagramSocket socket;
    private String address;
    private int port;

    private Thread send;

    public Client(String handle) {
        this.handle = handle;
        this.address = "localhost";
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHandle() {
        if(this.handle != null) return this.handle;
        return null;
    }

    public boolean openConnection() {
        try {
            socket = new DatagramSocket();
            setPort(socket.getLocalPort());
        } catch (SocketException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String receivePacket() {
        byte[] dataArray = new byte[1024];
        DatagramPacket packet = new DatagramPacket(dataArray, dataArray.length);

        try {
            socket.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String data = new String(packet.getData());
        return data;
    }

    public void sendPacket(final byte[] dataArray) {
        send = new Thread("send") {
            public void run() {
                DatagramPacket packet = null;
                try {
                    packet = new DatagramPacket(dataArray, dataArray.length, InetAddress.getByName("localHost"), 8192);
                    socket.send(packet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        send.start();
    }

    public void close() {
        new Thread() {
            public void run() {
                synchronized (socket) {
                    socket.close();
                }
            }
        }.start();
    }

    public void clientToServerCommunication(String communication) {
        final byte[] byteArray = communication.getBytes();
        sendPacket(byteArray);
    }
}
