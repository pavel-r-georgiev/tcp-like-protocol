/* Pavel Georgiev s1525701 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashSet;

public class AckThreadGBN implements Runnable {
    private boolean running = true;
    public static HashSet<Integer> receivedAcks = new HashSet<Integer>();
    private boolean debug = Sender2a.debug;

    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted() && running){
            if(Sender2a.clientSocket.isClosed()) {
                running = false;
                return;
            }

            AckPacket ack = new AckPacket();
                try {
                    DatagramPacket ackPacket = new DatagramPacket(ack.getBuffer(), AckPacket.ACK_BUFFER_LENGTH);
                    Sender2a.clientSocket.receive(ackPacket);
                } catch (IOException e ) {
                    running = false;
                    return;
                }

                int base = Sender2a.getBase();
                int nextSequenceNumber = Sender2a.getNextSequence();

                int ackSequenceNumber = ack.getSequenceNumber();

                if(debug){
                    System.out.println("Received ACK # " + ackSequenceNumber);
                }

                if(base <= ackSequenceNumber && ackSequenceNumber <= (base + Sender2a.windowSize - 1)) {
                    // Mark ACK as received
                    receivedAcks.add(ackSequenceNumber);
                    // Inform Sender thread that ack is received
                    Sender2a.ackReceived();

                    if(Sender2a.isEndOfFile() && ackSequenceNumber == Sender2a.lastSequenceNumber){
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
            Thread.yield();
        }
    }
}
