import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

public class Sender2a {
    static boolean debug = false;
    public static final int MAXIMUM_CONSECUTIVE_RETRANSMISSIONS = 10;
    public static int windowSize;
    private static int base;
    private static int nextSequenceNumber;
    public static int timeout;
    public static boolean running = true;
    private static DatagramSocket clientSocket;
    private static InetAddress IPAddress;
    private static int port;
//        Flag to show end of transmitted file
    private static boolean endOfFile = false;
//   Memory reference to the packets. Used for retransmission of packets.
    public static HashMap<Integer,Packet> packets = new HashMap<>();
//   List of uncacked packets
    public static ArrayList<Packet> unackedPackets = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        if(args.length != 5 && args.length != 6){
            System.err.println("Run with arguments <RemoteHost> <Port> <Filename> <RetryTimeout> <WindowSize> <Debug flag(optional)>.");
        }
//        Get host, port and filename from command line arguments
        final String remoteHost = args[0];
        port =  Integer.parseInt(args[1]);
        final String filename = args[2];
        timeout = Integer.parseInt(args[3]);
        windowSize = Integer.parseInt(args[4]);
//        Set debug flag to true - used to print debugging statements
        if(args.length == 5 && args[4].equals("debug")){
            debug = true;
        }

        IPAddress = InetAddress.getByName(remoteHost);
        File file = new File(filename);

        base = 0;
        nextSequenceNumber = 0;

        Thread ackThread = new Thread(new AckThread(port + 1));
        ackThread.start();

        if(debug){
            System.out.println("File " + filename + " sent successfully.");
        }
    }

    private static void sendFile(File file) throws IOException {
//        Initialize socket for the sender
        clientSocket = new DatagramSocket();
        FileInputStream fileStream = new FileInputStream(file);

//        Variables to keep track of start and end time of transmission
        long startTime = 0, endTime = 0;
//        Flag to show if packet is first to be transmitted - used to starting the timer
        boolean firstPacket = true;


        int sequenceNumber = 0;

//        Position in bytes showing progress of transmission of file
        int position = 0;


        while (nextSequenceNumber < base + windowSize && !endOfFile) {
//            Check if this is last packet of the file
            int bytesLeft = (int) (file.length() - position);
            if (bytesLeft <= 1024) {
                endOfFile = true;
            }


//            If it is last packet reduce buffer size
            int dataSize = endOfFile ? bytesLeft : Packet.PACKET_DEFAULT_DATA_SIZE;

//            Read from file and construct a packet to be sent
            byte[] data = new byte[dataSize];
            fileStream.read(data);
            Packet packet = new Packet(data, sequenceNumber, endOfFile);
            packets.put(packet.getSequenceNumber(), packet);

            DatagramPacket sendPacket = new DatagramPacket(packet.getBuffer(), packet.getBufferSize(), IPAddress, port);

            //                Send the packet
            clientSocket.send(sendPacket);

            //                Start timer for throughput measurement
            if(firstPacket){
                startTime = System.currentTimeMillis();
                firstPacket = false;
            }

            if(base == nextSequenceNumber){
               startTimer();
            }
            nextSequenceNumber++;

            sequenceNumber = (sequenceNumber + 1);
            position += 1024;
        }

        fileStream.close();
        clientSocket.close();

////        Get the transfer time in milliseconds
//        long elapsedTime = (endTime  - startTime);
////        Convert milliseconds to seconds.
//        double transferTimeSeconds = elapsedTime / 1000.0;
//
////        Get file size in KBytes
//        double fileSize = file.length() / 1024.0;
//        double throughput = (fileSize / transferTimeSeconds);
//
////        Output retransmissions and throughput
//        System.out.printf("%d %f%n", retransmissions, throughput);
    }

    public static synchronized void resendPackets() throws IOException {
        startTimer();
        for(int i = base; i < nextSequenceNumber; i++){
            Packet packet = packets.get(i);
            DatagramPacket sendPacket = new DatagramPacket(packet.getBuffer(), packet.getBufferSize(), IPAddress, port);
            clientSocket.send(sendPacket);
        }
    }

    public static synchronized int getBase() {
        return base;
    }


    public static synchronized void setAckReceived(int ackSequenceNumber) {
//        Mark as ACKed all packets before the last ACK received - cumulative ACK
        while (unackedPackets.size() > 0
                && unackedPackets.get(0).getSequenceNumber() <= ackSequenceNumber) {
            unackedPackets.remove(0);
        }
    }

    public static synchronized void setBase(int lastAckNumber) {
        base = lastAckNumber + 1;
    }

    public static synchronized boolean isEndOfFile(){
        return endOfFile;
    }

    public static synchronized int getNextSequence() {
        return nextSequenceNumber;
    }

    public static synchronized void stopTimer() {

    }

    public static synchronized void startTimer() {
    }
}