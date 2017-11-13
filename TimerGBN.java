public class TimerGBN implements Runnable {
    public static int sequenceNumber;
    private boolean running = true;
    private long timePast;
    private int timeout;
    private long timeCreated;
    private boolean debug;

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
            System.out.println("TimerGBN timeout for packet # " + sequenceNumber);
        }
        Sender2a.resendPackets();
    }
}
