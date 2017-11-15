import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashSet;

public class AckThreadSR implements Runnable {
    private boolean running = true;
    public static HashSet<Integer> receivedAcks = new HashSet<Integer>();
    private boolean debug = Sender2b.debug;
    private boolean fileEnded;

    @Override
    public void run() {
        while(running){
            AckPacket ack = new AckPacket();
            try {
                DatagramPacket ackPacket = new DatagramPacket(ack.getBuffer(), AckPacket.ACK_BUFFER_LENGTH);
                if(Sender2b.clientSocket.isClosed()){
                    break;
                }
                Sender2b.clientSocket.receive(ackPacket);
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            int base = Sender2b.getBase();
            int nextSequenceNumber = Sender2b.getNextSequence();

            if(Sender2b.isEndOfFile()){
                fileEnded = true;
            }

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

                if(fileEnded && base == Sender2b.lastSequenceNumber && Sender2b.isUnackedEmpty()){
                    Sender2b.lastAckReceived();
                    running = false;
                    break;
                }
            }
            Thread.yield();
        }
    }
}
