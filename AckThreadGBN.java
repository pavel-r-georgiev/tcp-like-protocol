import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashSet;

public class AckThreadGBN implements Runnable {
    private DatagramSocket serverSocket;
    private boolean endOfFile = false;
    private boolean running = true;
    public static HashSet<Integer> receivedAcks = new HashSet<Integer>();
    private boolean debug;

    public AckThreadGBN(int ackPort) {
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

                if(Sender2a.isEndOfFile()){
                    endOfFile = true;
                }

                int ackSequenceNumber = ack.getSequenceNumber();

                if(debug){
                    System.out.println("Received ACK # " + ackSequenceNumber);
                }

                if(base <= ackSequenceNumber && ackSequenceNumber <= (base + Sender2a.windowSize - 1)) {
                    // Mark ACK as received
                    receivedAcks.add(ackSequenceNumber);
                    // Inform Sender thread that ack is received
                    Sender2a.ackReceived();

                    base = ackSequenceNumber + 1;

                    if(endOfFile && base == nextSequenceNumber){
                        Sender2a.stopTimer();
                        Sender2a.lastAckReceived();
                        running = false;
                        break;
                    }


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

        serverSocket.close();
    }
}
