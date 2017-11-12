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
    public static HashSet<Integer> receivedAcks = new HashSet<>();

    public AckThread(int ackPort) {
        try {
            serverSocket = new DatagramSocket(ackPort);
            serverSocket.setSoTimeout(0);
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

                if(base <= ackSequenceNumber && ackSequenceNumber <= (base + Sender2a.windowSize - 1)) {
                    // Mark ACK as received
                    receivedAcks.add(ackSequenceNumber);
                    // Remove ACK from unreceived ACKS on Sender thread
                    Sender2a.setAckReceived(ackSequenceNumber);

                    base = ackSequenceNumber + 1;
                    // Change base
                    Sender2a.setBase(base);

                    Sender2a.stopTimer();
                    if(base != nextSequenceNumber) {
                        Sender2a.startTimer();
                    }

                    if(base == nextSequenceNumber && endOfFile){
                        running = false;
                    }
                }
        }
    }
}
