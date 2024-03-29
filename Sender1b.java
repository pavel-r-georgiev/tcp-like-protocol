import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

public class Sender1b {
    static boolean debug = false;
    public static final int MAXIMUM_CONSECUTIVE_RETRANSMISSIONS = 10;
    public static void main(String[] args) throws IOException {
        if(args.length != 4 && args.length != 5){
            System.err.println("Run with arguments <RemoteHost> <Port> <Filename> <RetryTimeout> <Debug flag(optional)>.");
        }
//        Get host, port and filename from command line arguments
        final String remoteHost = args[0];
        final int port =  Integer.parseInt(args[1]);
        final String filename = args[2];
        final int timeout = Integer.parseInt(args[3]);
//        Set debug flag to true - used to print debugging statements
        if(args.length == 5 && args[4].equals("debug")){
            debug = true;
        }

        InetAddress IPAddress = InetAddress.getByName(remoteHost);
        File file = new File(filename);

        sendFile(IPAddress, port, file, timeout);

        if(debug){
            System.out.println("File " + filename + " sent successfully.");
        }
    }

    private static void sendFile(InetAddress IPAddress, int port, File file, int timeout) throws IOException {
//        Initialize socket for the sender
        DatagramSocket clientSocket = new DatagramSocket();
        FileInputStream fileStream = new FileInputStream(file);

//        Variables to keep track of start and end time of transmission
        long startTime = 0, endTime = 0;
//        Flag to show if packet is first to be transmitted - used to starting the timer
        boolean firstPacket = true;


        int sequenceNumber = 0, retransmissions = 0;
//        Position in bytes showing progress of transmission of file
        int position = 0;
//        Flag to show end of transmitted file
        boolean endOfFile = false;

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

//            Flags to track receipt of ACKs
            boolean correctAckReceived = false, ackReceived;

//            Counter to track consecutive retransmissions
            int consecutiveRetransmissions = 0;

            while(!correctAckReceived){
//                Send the packet
                clientSocket.send(sendPacket);

//                Start timer for throughput measurement
                if(firstPacket){
                    startTime = System.currentTimeMillis();
                    firstPacket = false;
                }

                AckPacket ack = new AckPacket();
                try {
                    clientSocket.setSoTimeout(timeout);
                    DatagramPacket ackPacket = new DatagramPacket(ack.getBuffer(), AckPacket.ACK_BUFFER_LENGTH);
                    clientSocket.receive(ackPacket);
                    ackReceived = true;
//                  Record the time of arrival of last ACK packet
                    endTime =  System.currentTimeMillis();
                }catch (SocketTimeoutException e) {
                    ackReceived = false;
                    if(debug){
                        System.out.println("Socket timeout, while waiting for ACK.");
                    }
                }

                int ackSequenceNumber = ack.getSequenceNumber();

//                Reset consecutive retransmissions if any ACK is received
                if(ackReceived){
                    consecutiveRetransmissions = 0;
                }

                if(ackSequenceNumber == sequenceNumber && ackReceived) {
                    correctAckReceived = true;
                    if(debug){
                        System.out.println("Ack received. Ack #: " + ackSequenceNumber);
                    }
                } else {
//                    Increment retransmissions when ack is not received or sequence numbers do not match
                    retransmissions++;

//                    Increment consecutive retransmissions in the case of ACK for last packet not received
                    if(!ackReceived && endOfFile) {
                        consecutiveRetransmissions++;
                    }
//                    In case of no ACKs received in the last fixed amount of retransmissions stop trying to resend
                    if(consecutiveRetransmissions > MAXIMUM_CONSECUTIVE_RETRANSMISSIONS){
                        retransmissions -= MAXIMUM_CONSECUTIVE_RETRANSMISSIONS;
                        break;
                    }

                    if(debug){
                        System.out.println("Retransmitting packet #" + sequenceNumber);
                    }
                }
            }

            sequenceNumber = (sequenceNumber + 1) % 2;
            position += 1024;
        }

        fileStream.close();
        clientSocket.close();

//        Get the transfer time in milliseconds
        long elapsedTime = (endTime  - startTime);
//        Convert milliseconds to seconds.
        double transferTimeSeconds = elapsedTime / 1000.0;

//        Get file size in KBytes
        double fileSize = file.length() / 1024.0;
        double throughput = (fileSize / transferTimeSeconds);

//        Output retransmissions and throughput
        System.out.printf("%d %f%n", retransmissions, throughput);
    }
}
