import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class AckPacket {
    private byte[] buffer;
    public static final int ACK_BUFFER_LENGTH = 2;

    /**
     * Constructor for an empty ACK packet
     */
    public AckPacket() {
        this.buffer = new byte[ACK_BUFFER_LENGTH];
    }

    /**
     * Constructor for ACK with given sequence number
     * @param sequenceNumber    sequence number of the ACK packet
     */
    public AckPacket(int sequenceNumber){
        this();
//       Store sequence number based in header
        buffer[0] = (byte) ((sequenceNumber >> 8) & 0xFF);
        buffer[1] = (byte) (sequenceNumber & 0xFF);
    }

    /**
     * Reconstructs the sequence number from the buffer
     * @return int for the sequence number of the packet
     */
    public int getSequenceNumber() {
        return (int)((buffer[0] & 0xFF) << 8 | (buffer[1] & 0xFF));
    }

    /**
     * Returns buffer of the packet
     * @return  byte array representing the buffer of the ACK packet
     */
    public byte[] getBuffer() {
        return buffer;
    }

    /**
     * Sends ACK packet back to sender
     * @param IPAddress
     * @param port
     * @param socket
     * @throws IOException
     */
    public void sendAck(InetAddress IPAddress, int port, DatagramSocket socket) throws IOException {
        DatagramPacket ackPacket = new DatagramPacket(buffer, ACK_BUFFER_LENGTH, IPAddress, port);
        socket.send(ackPacket);
    }
}
