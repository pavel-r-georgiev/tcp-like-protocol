import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;

public class Sender2b {
    public static boolean debug = false;
    public static final int MAXIMUM_CONSECUTIVE_RETRANSMISSIONS = 10;
    private static int consecutiveRetransmissions = 0;
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
    private static Thread ackThread;
    //   Memory reference to the packets. Used for retransmission of packets.
    public static HashMap<Integer,Packet> packets = new HashMap<Integer,Packet>();
    //   List of unacked packets
    public static volatile ConcurrentSkipListSet<Integer> unackedPackets = new ConcurrentSkipListSet<Integer>();
//    List of timers
    public static HashMap<Integer, Thread> timers = new HashMap<Integer, Thread>();


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

        ackThread = new Thread(new AckThreadSR(port + 1));
        ackThread.start();
        sendFile(file);

        if(debug){
            System.out.println("File " + filename + " sent successfully.");
        }
    }

    private static void sendFile(File file) throws IOException {
//        Initialize socket for the sender
        clientSocket = new DatagramSocket();
        FileInputStream fileStream = new FileInputStream(file);

//        Flag to show if packet is first to be transmitted - used to starting the timer
        boolean firstPacket = true;


        int sequenceNumber = 1;

//        Position in bytes showing progress of transmission of file
        int position = 0;

        while (!endOfFile || running) {
            while (nextSequenceNumber < base + windowSize && !endOfFile) {
                synchronized (unackedPackets) {
                    if (debug) {
                        System.out.println("Base " + base);
                        System.out.println("Next Seq #" + nextSequenceNumber);
                    }
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


                    unackedPackets.add(sequenceNumber);
                    clientSocket.send(sendPacket);
                }

                //                Send the packet
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
        System.out.printf("%f %n", throughput);
    }


    public static synchronized void resendPacket(int sequenceNumber) {
        if(debug){
            System.out.println("Resending packet # " + base);
        }

        consecutiveRetransmissions++;

//        If threshold consecutive transmissions reached at end of file end the sender. Last ACK most likely got lost.
        if(consecutiveRetransmissions >= MAXIMUM_CONSECUTIVE_RETRANSMISSIONS && endOfFile){
            running = false;
        }

        Packet packet = packets.get(sequenceNumber);
        DatagramPacket sendPacket = new DatagramPacket(packet.getBuffer(), packet.getBufferSize(), IPAddress, port);

        try {
            if(debug){
                System.out.println("Sending packet #" + packet.getSequenceNumber());
            }
            clientSocket.send(sendPacket);
            startTimer(sequenceNumber);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized int getBase() {
        return base;
    }


    public static synchronized void setAckReceived(int ackSequenceNumber) {
        consecutiveRetransmissions = 0;
//        Mark as ACKed as received
        unackedPackets.remove(ackSequenceNumber);
        stopTimer(ackSequenceNumber);
    }

    public static synchronized void setBase() {
        synchronized (unackedPackets) {
            if (!unackedPackets.isEmpty()) {
                base = unackedPackets.first();
            } else {
                System.out.println(unackedPackets.toString());
            }
        }
    }

    public static synchronized void setBase(int newBase) {
        base = newBase;
    }

    public static synchronized boolean isUnackedPacketsEmpty(){
        return unackedPackets.isEmpty();
    }

    public static synchronized int firstUnacked() {
        System.out.println(unackedPackets.toString());
        return unackedPackets.first();
    }
    public static synchronized boolean isEndOfFile(){
        return endOfFile;
    }

    public static synchronized int getNextSequence() {
        return nextSequenceNumber;
    }

    public static synchronized void stopTimer(int ackSequenceNumber) {
        if(debug) {
            System.out.println("Timer stopped for # " + ackSequenceNumber);
        }
        Thread timer = timers.get(ackSequenceNumber);
        if(timer != null) {
            timer.interrupt();
        }
    }

    public static synchronized void startTimer(int sequenceNumber) {
        if(debug){
            System.out.println("Timer started for #" + sequenceNumber);
        }
        Thread timer = new Thread(new TimerSR(sequenceNumber));
        timers.put(sequenceNumber, timer);
        timer.start();
    }


    public static synchronized void lastAckReceived() {
        endTime = System.currentTimeMillis();
        running = false;
    }
}
