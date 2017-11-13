public class Timer implements Runnable {
    public static int sequenceNumber;
    private boolean running = true;
    private long timePast;
    private int timeout;
    private long timeCreated;
    private boolean debug;

    public Timer(int sequenceNumber){
        this.sequenceNumber = sequenceNumber;
        this.timeout = Sender2a.timeout;
        this.debug = Sender2a.debug;
        this.timeCreated = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() && running) {
            if (AckThread.receivedAcks.contains(sequenceNumber)) {
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
