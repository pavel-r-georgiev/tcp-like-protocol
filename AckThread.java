import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class AckThread implements Runnable {
    private DatagramSocket serverSocket;
    private int lastSequenceNumber;
    private boolean running = true;
    public static ArrayList<Integer> receivedAcks = new ArrayList<>();

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
                int ackSequenceNumber = ack.getSequenceNumber();

                if(base <= ackSequenceNumber && ackSequenceNumber <= (base + Sender2a.windowSize - 1)) {
                    // Mark ACK as received
                    receivedAcks.add(ackSequenceNumber);
                    // Remove ACK from unreceived ACKS on Sender thread
                    Sender2a.setAckReceived(ackSequenceNumber);
                    // Change base
                    Sender2a.setBase(ackSequenceNumber);

                    if(base == Sender2a.getNextSequence()){
                        Sender2a.stopTimer();
                    } else {
                        Sender2a.startTimer();
                    }

                    if(Sender2a.isEndOfFile()){
                        running = false;
                    }
                }
        }
    }
}
