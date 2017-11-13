public class TimerSR implements Runnable {
    public static int sequenceNumber;
    private boolean running = true;
    private long timePast;
    private int timeout;
    private long timeCreated;
    private boolean debug;

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
        if(debug){
            System.out.println("Timer timeout for packet # " + sequenceNumber);
        }
        Sender2b.resendPacket(sequenceNumber);
    }
}
