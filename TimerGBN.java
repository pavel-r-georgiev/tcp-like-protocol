public class TimerGBN implements Runnable {
    final private int sequenceNumber;
    private boolean running = true;
    final private int timeout;
    final private long timeCreated;
    final private boolean debug;

    public TimerGBN(int sequenceNumber){
        this.sequenceNumber = sequenceNumber;
        this.timeout = Sender2a.timeout;
        this.debug = Sender2a.debug;
        this.timeCreated = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() && running) {
            if (AckThreadGBN.receivedAcks.contains(sequenceNumber)) {
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
        if(debug){
            System.out.println("Timer timeout for packet # " + sequenceNumber);
        }
        Sender2a.resendPackets();
    }
}
