public class Timer implements Runnable {
    private int sequenceNumber;
    private boolean running = true;
    private long timePast;
    private int timeout;
    private long timeCreated;

    public Timer(int sequenceNumber){
        this.sequenceNumber = sequenceNumber;
        this.timeout = Sender2a.timeout;
        this.timeCreated = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() && running) {
            System.out.println("Timer running - >" + sequenceNumber);
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
        System.out.println("Timeout. Resending packets");
        Sender2a.resendPackets();
    }
}
