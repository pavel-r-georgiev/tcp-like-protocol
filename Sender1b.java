import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Sender1b {
    public static void main(String[] args) throws IOException {
//        Get host, port and filename from command line arguments
        final String remoteHost = args[0];
        final int port =  Integer.parseInt(args[1]);
        final String filename = args[2];
        final int timeout = Integer.parseInt(args[3]);

        InetAddress IPAddress = InetAddress.getByName(remoteHost);
        File file = new File(filename);

        sendFile(IPAddress, port, file, timeout);
    }

    private static void sendFile(InetAddress IPAddress, int port, File file, int timeout) throws IOException {
//        Initialize socket for the sender
        DatagramSocket clientSocket = new DatagramSocket();
        FileInputStream fileStream = new FileInputStream(file);

//        Start timer for throughput measurement
        long startTime = System.currentTimeMillis();

        int sequenceNumber = 0, retransmissions = 0;
        boolean endOfFile = false;
        int position = 0;

        while(!endOfFile){
//            Check if this is last packet of the file
            if(position >= file.length() - 1024){
                endOfFile = true;
            }
//            Read from file and construct a packet to be sent
            byte[] data = new byte[1024];
            fileStream.read(data);
            Packet packet = new Packet(data, sequenceNumber, endOfFile);
            DatagramPacket sendPacket = new DatagramPacket(packet.getBuffer(), Packet.PACKET_BUFFER_SIZE, IPAddress, port);

            boolean correctAckReceived = false;

            while(!correctAckReceived){
//                Send the packet
                clientSocket.send(sendPacket);
                clientSocket.setSoTimeout(timeout);
                AckPacket ack = new AckPacket();
                DatagramPacket receivePacket = new DatagramPacket(ack.getBuffer(), AckPacket.ACK_BUFFER_LENGTH);

                clientSocket.receive(receivePacket);

                int ackSequence = ack.getSequenceNumber();

                if(ackSequence == sequenceNumber) {
                    correctAckReceived = true;
                } else {
                    retransmissions++;
                }
            }

            sequenceNumber++;
            position += 1024;

        }

        clientSocket.close();

    }
}
