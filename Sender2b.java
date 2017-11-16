import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class Sender2b {
    public static boolean debug = false;
    public static final int MAXIMUM_CONSECUTIVE_RETRANSMISSIONS = 20;
    private static final HashMap<Integer, Integer> retransmissions = new HashMap<Integer, Integer>();
    public static int windowSize;
    private static int base;
    private static int nextSequenceNumber;
    public static int timeout;
    public static boolean running = true;
    private static boolean lastAckReceived = false;
    public static DatagramSocket clientSocket;
    public static InetAddress IPAddress;
    public static int port;
    public static volatile int lastSequenceNumber;
    //        Flag to show end of transmitted file
    private static boolean endOfFile = false;
    private static Thread ackThread;
    //   Memory reference to the packets. Used for retransmission of packets.
    public static HashMap<Integer,Packet> packets = new HashMap<Integer,Packet>();
    //   List of unacked packets
    public static volatile ConcurrentSkipListSet<Integer> unackedPackets = new ConcurrentSkipListSet<Integer>();


    //        Variables to keep track of start and end time of transmission
    private static long startTime = 0, endTime = 0;


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
        if(args.length == 6 && args[5].equals("debug")){
            debug = true;
        }

        IPAddress = InetAddress.getByName(remoteHost);
        File file = new File(filename);

        base = 1;
        nextSequenceNumber = 1;

        sendFile(file);

        if(debug){
            System.out.println("File " + filename + " sent successfully.");
        }

        System.exit(0);
    }

    private static void sendFile(File file) throws IOException {
//        Initialize socket for the sender
        clientSocket = new DatagramSocket();
        ackThread = new Thread(new AckThreadSR());
        ackThread.setPriority(Thread.MAX_PRIORITY - 1);
        ackThread.start();
        FileInputStream fileStream = new FileInputStream(file);

//        Flag to show if packet is first to be transmitted - used to starting the timer
        boolean firstPacket = true;

        int sequenceNumber = 1;

//        Position in bytes showing progress of transmission of file
        int position = 0;

        while (!endOfFile || running) {
            while (nextSequenceNumber < base + windowSize && !endOfFile) {
                if(debug){
                    System.out.printf("Windows [%d %d]%n", base, nextSequenceNumber);
                }
//            Check if this is last packet of the file
                int bytesLeft = (int) (file.length() - position);
                if (bytesLeft <= 1024) {
                    endOfFile = true;
                    lastSequenceNumber = sequenceNumber;
                }

//            If it is last packet reduce buffer size
                int dataSize = endOfFile ? bytesLeft : Packet.PACKET_DEFAULT_DATA_SIZE;

//            Read from file and construct a packet to be sent
                byte[] data = new byte[dataSize];
                fileStream.read(data);
                Packet packet = new Packet(data, sequenceNumber, endOfFile);
                packets.put(packet.getSequenceNumber(), packet);
                DatagramPacket sendPacket = new DatagramPacket(packet.getBuffer(), packet.getBufferSize(), IPAddress, port);

                unackedPackets.add(sequenceNumber);

                //                Send the packet
                clientSocket.send(sendPacket);
                if(debug){
                    System.out.println("Sending packet #" + sequenceNumber);
                }
                startTimer(sequenceNumber);


                //                Start timer for throughput measurement
                if (firstPacket) {
                    startTime = System.currentTimeMillis();
                    firstPacket = false;
                }

                nextSequenceNumber++;

                sequenceNumber++;
                position += 1024;
            }

            if(lastAckReceived && unackedPackets.isEmpty() && endOfFile){
                shutdown();
            }
        }

        fileStream.close();

//        Get the transfer time in milliseconds
        long elapsedTime = (endTime  - startTime);
//        Convert milliseconds to seconds.
        double transferTimeSeconds = elapsedTime / 1000.0;

//        Get file size in KBytes
        double fileSize = file.length() / 1024.0;
        double throughput = (fileSize / transferTimeSeconds);

//        Output retransmissions and throughput
        System.out.printf("%f %n", throughput);
    }

    private static void shutdown() {
        if(debug){
            System.out.println("Closing socket...");
            System.out.println("Ending ACK Thread...");
            System.out.println("Ending transmission...");
        }
        ackThread.interrupt();
        clientSocket.close();
        running = false;
    }

    public static synchronized void checkRetransmissionLimit(int sequenceNumber){
        if(retransmissions.containsKey(sequenceNumber)){
            retransmissions.put(sequenceNumber, retransmissions.get(sequenceNumber) + 1);
        } else {
            retransmissions.put(sequenceNumber, 1);
        }

        int consecutiveRetransmissions = retransmissions.get(sequenceNumber);

        if(consecutiveRetransmissions >= MAXIMUM_CONSECUTIVE_RETRANSMISSIONS && endOfFile){
            if(debug){
                System.out.println("Maximum retransmissions reached.");
            }
            shutdown();
        }
    }


    public static synchronized int getBase() {
        return base;
    }


    public static synchronized void setAckReceived(int ackSequenceNumber) {
        endTime = System.currentTimeMillis();
//        Mark as ACKed as received
        unackedPackets.remove(ackSequenceNumber);
    }

    public static synchronized void setBase() {
        if (!unackedPackets.isEmpty()) {
            base = unackedPackets.first();
        } else if(base != nextSequenceNumber){
            base = nextSequenceNumber;
        } else {
            base = base + 1;
        }
    }

    public static synchronized boolean isEndOfFile(){
        return endOfFile;
    }

    public static synchronized void startTimer(int sequenceNumber) {
        if(debug){
            System.out.println("Timer started for #" + sequenceNumber);
        }
        new Thread(new TimerSR(sequenceNumber)).start();
    }


    public static synchronized void lastAckReceived() {
        lastAckReceived = true;
    }
}

