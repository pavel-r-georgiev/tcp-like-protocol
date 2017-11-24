import java.io.IOException;
import java.net.DatagramPacket;
import java.util.TimerTask;

public class Timeout extends TimerTask {
    final private int sequenceNumber;
    final private boolean debug;

    public Timeout(int sequenceNumber){
        this.sequenceNumber = sequenceNumber;
        this.debug = Sender2b.debug;
    }


    public void run() {
        timeout();
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

