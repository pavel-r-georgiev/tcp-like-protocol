/* Pavel Georgiev s1525701 */
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

public class Sender2a {
    public static boolean debug = false;
    public static final int MAXIMUM_CONSECUTIVE_RETRANSMISSIONS = 20;
    public volatile static int consecutiveRetransmissions = 0;
    public static int windowSize;
    private volatile static int base;
    private volatile static int nextSequenceNumber;
    public static int timeout;
    public static boolean running = true;
    public static DatagramSocket clientSocket;
    private static InetAddress IPAddress;
    private static int port;
//        Flag to show end of transmitted file
    private static volatile boolean endOfFile = false;
    public static volatile int lastSequenceNumber;
//   Memory reference to the packets. Used for retransmission of packets.
    public static HashMap<Integer,Packet> packets = new HashMap<Integer,Packet>();
//   Future for callable
    private static Thread timer = null;
    //        Variables to keep track of start and end time of transmission
    private static long startTime = 0, endTime = 0;
    private static boolean lastAckReceived = false;
    private static Thread ackThread;



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
        ackThread = new Thread(new AckThreadGBN());
        ackThread.start();

        FileInputStream fileStream = new FileInputStream(file);

//        Flag to show if packet is first to be transmitted - used to starting the timer
        boolean firstPacket = true;


        int sequenceNumber = 1;

//        Position in bytes showing progress of transmission of file
        int position = 0;

        while (!endOfFile || running) {
            while (nextSequenceNumber < base + windowSize && !endOfFile) {
//            Check if this is last packet of the file
                int bytesLeft = (int) (file.length() - position);
                if (bytesLeft <= 1024) {
                    endOfFile = true;
                    lastSequenceNumber = sequenceNumber;
                }

                if(debug){
                    System.out.printf("Windows [%d %d]%n", base, nextSequenceNumber);
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

                if(debug){
                    System.out.println("Sending packet #" + sequenceNumber);
                }

                //                Start timer for throughput measurement
                if (firstPacket) {
                    startTime = System.currentTimeMillis();
                    firstPacket = false;
                }

                if (base == nextSequenceNumber) {
                    startTimer();
                }


                nextSequenceNumber++;

                sequenceNumber++;
                position += 1024;
            }

            if(lastAckReceived && endOfFile){
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


    public static synchronized void resendPackets() {
        if(debug){
            System.out.println("Resending packets from # " + base);
        }

        consecutiveRetransmissions++;

//        If threshold consecutive transmissions reached at end of file end the sender. Last ACK most likely got lost.
        if(consecutiveRetransmissions >= MAXIMUM_CONSECUTIVE_RETRANSMISSIONS && endOfFile){
            if(debug){
                System.out.println("Maximum retransmissions reached.");
            }
            shutdown();
            return;
        }

        restartTimer();
        for(int i = base; i < nextSequenceNumber; i++){
            Packet packet = packets.get(i);

            DatagramPacket sendPacket = new DatagramPacket(packet.getBuffer(), packet.getBufferSize(), IPAddress, port);
            try {
                if(debug){
                    System.out.println("Sending packet #" + packet.getSequenceNumber());
                }
                clientSocket.send(sendPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static synchronized int getBase() {
        return base;
    }


    public static synchronized void ackReceived() {
        endTime = System.currentTimeMillis();
        consecutiveRetransmissions = 0;
    }

    public static synchronized void setBase(int newBase) {
        base = newBase;
    }

    public static synchronized boolean isEndOfFile(){
        return endOfFile;
    }

    public static synchronized int getNextSequence() {
        return nextSequenceNumber;
    }

    public static synchronized void stopTimer() {
        if(debug) {
            System.out.println("Timer stopped for " + base);
        }
        if(timer != null) {
            timer.interrupt();
        }
    }

    public static synchronized void startTimer() {
        if(debug){
            System.out.println("Timer started for #" + base);
        }
        timer = new Thread(new TimerGBN(base));
        timer.start();
    }

    public static synchronized void restartTimer() {
        stopTimer();
        startTimer();
    }

    public static synchronized void lastAckReceived() {
        stopTimer();
        lastAckReceived = true;
    }

    private static void shutdown() {
        if(debug){
            System.out.println("Closing socket...");
            System.out.println("Ending ACK Thread...");
            System.out.println("Ending transmission...");
        }
        clientSocket.close();
        ackThread.interrupt();
        running = false;
    }

}
