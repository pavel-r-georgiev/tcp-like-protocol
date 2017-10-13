import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class Sender1b {
    public static void main(String[] args) throws IOException {
        if(args.length != 4){
            System.err.println("Run with arguments <RemoteHost> <Port> <Filename> <RetryTimeout>.");
        }
//        Get host, port and filename from command line arguments
        final String remoteHost = args[0];
        final int port =  Integer.parseInt(args[1]);
        final String filename = args[2];
        final int timeout = Integer.parseInt(args[3]);

        InetAddress IPAddress = InetAddress.getByName(remoteHost);
        File file = new File(filename);

        sendFile(IPAddress, port, file, timeout);

        System.out.println("File " + filename + " sent successfully.");
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
            int bytesLeft = (int)(file.length() - position);
            if(bytesLeft <= 1024){
                endOfFile = true;
            }

            //            If it is last packet reduce buffer size
            int dataSize = endOfFile ? bytesLeft : Packet.PACKET_DEFAULT_DATA_SIZE;

//            Read from file and construct a packet to be sent
            byte[] data = new byte[dataSize];
            fileStream.read(data);
            Packet packet = new Packet(data, sequenceNumber, endOfFile);
            DatagramPacket sendPacket = new DatagramPacket(packet.getBuffer(), packet.getBufferSize(), IPAddress, port);

            boolean correctAckReceived = false, ackReceived;

            while(!correctAckReceived){
//                Send the packet
                clientSocket.send(sendPacket);
                AckPacket ack = new AckPacket();
                try {
                    clientSocket.setSoTimeout(timeout);
                    DatagramPacket ackPacket = new DatagramPacket(ack.getBuffer(), AckPacket.ACK_BUFFER_LENGTH);
                    clientSocket.receive(ackPacket);
                    ackReceived = true;
                }catch (SocketTimeoutException e) {
                    ackReceived = false;
                    System.out.println("Socket timeout, while waiting for ACK.");
                }

                int ackSequenceNumber = ack.getSequenceNumber();

                if(ackSequenceNumber == sequenceNumber && ackReceived) {
                    correctAckReceived = true;
                    System.out.println("Ack received. Ack #: " + ackSequenceNumber);
                } else {
                    retransmissions++;
                    System.out.println("Retransmitting packet #" + sequenceNumber);
                }
            }

            sequenceNumber++;
            position += 1024;
        }

//       End timer for throughput measurement
        long endTime =  System.currentTimeMillis();
//        Get the transfer time in seconds
        long transferTime = (endTime  - startTime) / 1000;
//        Get file size in KBytes
        long fileSize = file.length() / 1024;
        double throughput = (double) fileSize / transferTime;
//        Output retransmissions and throughput
        System.out.println(retransmissions + " " + throughput);

        clientSocket.close();
    }
}
