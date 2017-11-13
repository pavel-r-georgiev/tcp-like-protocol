import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashSet;

public class AckThread implements Runnable {
    private DatagramSocket serverSocket;
    private int lastSequenceNumber;
    private boolean running = true;
    public static HashSet<Integer> receivedAcks = new HashSet<Integer>();
    private boolean debug;

    public AckThread(int ackPort) {
        try {
            serverSocket = new DatagramSocket(ackPort);
            serverSocket.setSoTimeout(0);
            debug = Sender2a.debug;
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

                int base = Sender2a.getBase();
                int nextSequenceNumber = Sender2a.getNextSequence();
                boolean endOfFile = Sender2a.isEndOfFile();

                int ackSequenceNumber = ack.getSequenceNumber();

                if(debug){
                    System.out.println("Received ACK # " + ackSequenceNumber);
                }

                if(base <= ackSequenceNumber && ackSequenceNumber <= (base + Sender2a.windowSize - 1)) {
                    // Mark ACK as received
                    receivedAcks.add(ackSequenceNumber);
                    // Remove ACK from unreceived ACKS on Sender thread
                    Sender2a.setAckReceived(ackSequenceNumber);

                    if(endOfFile && base == nextSequenceNumber - 1){
                        Sender2a.stopTimer();
                        Sender2a.lastAckReceived();
                        running = false;
                        break;
                    }

                    base = ackSequenceNumber + 1;
                    // Change base
                    Sender2a.setBase(base);

                    if(base == nextSequenceNumber) {
                        Sender2a.stopTimer();
                    } else {
                        Sender2a.restartTimer();
                    }
                }
        }

        serverSocket.close();
    }
}
