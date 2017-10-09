import java.io.File;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Receiver1a {

    public static void main(String[] args) throws SocketException {
        final int port = Integer.parseInt(args[0]);
        final String filename = args[2];

        File file = receiveFile(port, filename);

    }

    public static File receiveFile(int port, String filename) throws SocketException {
        DatagramSocket serverSocket = new DatagramSocket(port);
        Packet packet = new Packet();
        byte[] packetBuffer = new byte[1027];

        serverSocket.receive();

    }
}
