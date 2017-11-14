import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashSet;

public class AckThreadSR implements Runnable {
    private DatagramSocket serverSocket;
    private int lastSequenceNumber;
    private boolean running = true;
    public static HashSet<Integer> receivedAcks = new HashSet<Integer>();
    private boolean debug;

    public AckThreadSR(int ackPort) {
        try {
            serverSocket = new DatagramSocket(ackPort);
            serverSocket.setSoTimeout(0);
            debug = Sender2b.debug;
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while(running){
            AckPacket ack = new AckPacket();
            try {
                serverSocket.setSoTimeout(0);
                DatagramPacket ackPacket = new DatagramPacket(ack.getBuffer(), AckPacket.ACK_BUFFER_LENGTH);
                serverSocket.receive(ackPacket);
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            int base = Sender2b.getBase();
            int nextSequenceNumber = Sender2b.getNextSequence();
            boolean endOfFile = Sender2b.isEndOfFile();

            int ackSequenceNumber = ack.getSequenceNumber();

            if(debug){
                System.out.println("Received ACK # " + ackSequenceNumber);
            }


            if(base <= ackSequenceNumber && ackSequenceNumber <= (base + Sender2b.windowSize - 1)) {
                // Mark ACK as received
                receivedAcks.add(ackSequenceNumber);
                // Remove ACK from unreceived ACKS on Sender thread
                Sender2b.setAckReceived(ackSequenceNumber);


                if(ackSequenceNumber == base){
                    Sender2b.setBase();
                }

                if(endOfFile && base == nextSequenceNumber - 1){
                    Sender2b.lastAckReceived();
                    running = false;
                    break;
                }
            }
            Thread.yield();
        }

        serverSocket.close();
    }
}
