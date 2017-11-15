import java.io.IOException;
import java.net.DatagramPacket;
import java.util.HashSet;

public class AckThreadSR implements Runnable {
    private boolean running = true;
    public static HashSet<Integer> receivedAcks = new HashSet<Integer>();
    private boolean debug = Sender2b.debug;

    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted() && running){
            if(Sender2b.clientSocket.isClosed()) {
                running = false;
                return;
            }

            AckPacket ack = new AckPacket();
            try {
                DatagramPacket ackPacket = new DatagramPacket(ack.getBuffer(), AckPacket.ACK_BUFFER_LENGTH);
                Sender2b.clientSocket.receive(ackPacket);
            } catch (IOException e ) {
                running = false;
                return;
            }


            int base = Sender2b.getBase();

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

                if(Sender2b.isEndOfFile() && ackSequenceNumber == Sender2b.lastSequenceNumber){
                    Sender2b.lastAckReceived();
                }
            }
            Thread.yield();
        }
    }
}
