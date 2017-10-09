public class AckPacket {
    private byte[] buffer;
    public static final int ACK_BUFFER_LENGTH = 2;

    public AckPacket() {
        this.buffer = new byte[ACK_BUFFER_LENGTH];
    }

    public AckPacket(int sequenceNumber){
//       Store sequence number based in header
        this.buffer[0] = (byte) (sequenceNumber & 0xFF);
        this.buffer[1] = (byte) ((sequenceNumber >> 8) & 0xFF);
    }

    public int getSequenceNumber() {
//        Reconstructs the sequence number from the buffer
        return (int) (buffer[1] << 8 | buffer[0]);
    }

    public byte[] getBuffer() {
        return buffer;
    }
}
