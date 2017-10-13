import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class AckPacket {
    private byte[] buffer;
    public static final int ACK_BUFFER_LENGTH = 2;

    public AckPacket() {
        this.buffer = new byte[ACK_BUFFER_LENGTH];
    }

    public AckPacket(int sequenceNumber){
        this();
//       Store sequence number based in header
        buffer[0] = (byte) ((sequenceNumber >> 8) & 0xFF);
        buffer[1] = (byte) (sequenceNumber & 0xFF);
    }

    public int getSequenceNumber() {
//        Reconstructs the sequence number from the buffer
        return (int)((buffer[0] & 0xFF) << 8 | (buffer[1] & 0xFF));
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public void sendAck(InetAddress IPAddress, int port, DatagramSocket socket) throws IOException {
        DatagramPacket ackPacket = new DatagramPacket(buffer, ACK_BUFFER_LENGTH, IPAddress, port);
        socket.send(ackPacket);
    }
}
