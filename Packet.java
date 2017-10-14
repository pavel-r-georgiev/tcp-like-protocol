/* Pavel Georgiev s1525701 */
import java.util.Arrays;

public class Packet {
    private byte[] buffer;
//    Variables for default data, buffer and header sizes. Initialized as static so they can be used across classes.
    public static final int PACKET_HEADER_SIZE = 3;
    public static final int PACKET_DEFAULT_DATA_SIZE = 1024;
    public static final int PACKET_DEFAULT_BUFFER_SIZE = PACKET_DEFAULT_DATA_SIZE + PACKET_HEADER_SIZE;
//    Private variables to keep actual data and buffer sizes. Useful for size of last packet.
    private int PACKET_DATA_SIZE = PACKET_DEFAULT_DATA_SIZE;
    private int PACKET_BUFFER_SIZE = PACKET_DEFAULT_BUFFER_SIZE;

    /**
     * Constructor to initialize empty packet
     */
    public Packet(){
        this.buffer = new byte[PACKET_DEFAULT_BUFFER_SIZE];
    }

    /**
     * Constructor for already constructed buffer
     *
     * @param buffer buffer of the packet to be created
     */
    public Packet(byte[] buffer){
        this.buffer = buffer;
    }

    /**
     * Constructor for the Packet class
     * @param data              data to store in the packet
     * @param sequenceNumber    sequence number of the packet
     * @param endOfFile         end of file flag of the packet
     */
    public Packet(byte[] data, int sequenceNumber, boolean endOfFile){
         this();

        //        Mark end of file and create smaller packet buffer if needed
        if(endOfFile){
            PACKET_DATA_SIZE = data.length;
            PACKET_BUFFER_SIZE = this.getBufferSize();
            buffer = new byte[PACKET_BUFFER_SIZE];
            buffer[2] = 1;
        }

//       Store sequence number based in header
        buffer[0] = (byte) ((sequenceNumber >> 8) & 0xFF);
        buffer[1] = (byte) (sequenceNumber & 0xFF);


//        Transfer data into packet buffer
        for(int i = 0; i < PACKET_DATA_SIZE; i++){
            buffer[i + 3] = data[i];
        }
    }

    /**
     * Return the buffer - header and data of the packet.
     *
     * @return byte array representing the buffer of the packet.
     */
    public byte[] getBuffer() {
        return buffer;
    }

    /**
     * Returns the whole data part of the buffer
     *
     * @return byte array with the data part of the packet
     */
    public byte[] getData() {
//
        return Arrays.copyOfRange(buffer, PACKET_HEADER_SIZE, this.getBufferSize());
    }

    /**
     *  Returns the data part of the packet.
     *  Useful when you have the actual packet size or you want only a part of the data array.
     *
     * @param packetSize size of packet
     * @return byte array representing part of specified size from the data in the packet
     */
    public byte[] getData(int packetSize) {
        return Arrays.copyOfRange(buffer, PACKET_HEADER_SIZE, Math.min(PACKET_DEFAULT_BUFFER_SIZE, packetSize));
    }

    /**
     *  Returns actual size of buffer for the current packet. Might differ from default values for last packet.
     *
     * @return buffer size of packet
     */
    public int getBufferSize() {
        return PACKET_HEADER_SIZE + PACKET_DATA_SIZE;
    }

    /**
     * Checks if packet is last to be transmitted
     *
     * @return true if packet is last in sequence or false otherwise
     */
    public boolean isLastPacket() {
        if(buffer[2] == 1){
            return true;
        }
        return false;
    }

    /**
     * Reconstructs the sequence number from the buffer
     */
    public int getSequenceNumber() {
        return (int) ((buffer[0] & 0xFF) << 8 | (buffer[1] & 0xFF));
    }
}
