/* Pavel Georgiev s1525701 */
import java.io.IOException;
import java.net.DatagramPacket;

public class TimerSR implements Runnable {
    final private int sequenceNumber;
    private boolean running = true;
    final private int timeout;
    final private long timeCreated;
    final private boolean debug;

    public TimerSR(int sequenceNumber){
        this.sequenceNumber = sequenceNumber;
        this.timeout = Sender2b.timeout;
        this.debug = Sender2b.debug;
        this.timeCreated = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() && running) {
            if (AckThreadSR.receivedAcks.contains(sequenceNumber)) {
                running = false;
                break;
            }

            if(System.currentTimeMillis() - timeCreated >= timeout){
                timeout();
                running = false;
            }
        }
    }

    private synchronized  void timeout() {
        if(Sender2b.clientSocket.isClosed()){
            return;
        }

        if(debug){
            System.out.println("Timer timeout for packet # " + sequenceNumber);
        }
        Packet packet = Sender2b.packets.get(sequenceNumber);
        DatagramPacket sendPacket = new DatagramPacket(packet.getBuffer(), packet.getBufferSize(), Sender2b.IPAddress, Sender2b.port);

        try {
            if(debug){
                System.out.println("Resending packet # " + sequenceNumber);
            }
            Sender2b.clientSocket.send(sendPacket);
            Sender2b.startTimer(sequenceNumber);
            Sender2b.checkRetransmissionLimit(sequenceNumber);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
