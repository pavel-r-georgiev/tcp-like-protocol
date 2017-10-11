/* Pavel Georgiev s1525701 */
import java.util.Arrays;

/**
 * Created by Pavel on 08/10/2017.
 */
public class Packet {
    private byte[] buffer;
//    Records the last index of the data stored. Used for the last packet of the file.
    public static final int PACKET_HEADER_SIZE = 3;
    public static final int PACKET_DEFAULT_DATA_SIZE = 1024;
    public static final int PACKET_DEFAULT_BUFFER_SIZE = PACKET_DEFAULT_DATA_SIZE + PACKET_HEADER_SIZE;
    private int PACKET_DATA_SIZE;
    private int PACKET_BUFFER_SIZE;


    public Packet(){
        this.buffer = new byte[PACKET_DEFAULT_BUFFER_SIZE];
    }

    public Packet(byte[] buffer){
        this.buffer = buffer;
    }

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
        buffer[0] = (byte) (sequenceNumber & 0xFF);
        buffer[1] = (byte) ((sequenceNumber >> 8) & 0xFF);


//        Transfer data into packet buffer
        for(int i = 0; i < PACKET_DATA_SIZE; i++){
            buffer[i + 3] = data[i];
        }
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public byte[] getData() {
        return Arrays.copyOfRange(buffer, PACKET_HEADER_SIZE, this.getBufferSize());
    }

    public byte[] getData(int packetSize) {
//        Returns the data part of the packet when packet is of size "packetSize"
        return Arrays.copyOfRange(buffer, PACKET_HEADER_SIZE, Math.min(PACKET_DEFAULT_BUFFER_SIZE, packetSize));
    }

    public int getBufferSize() {
        return PACKET_HEADER_SIZE + PACKET_DATA_SIZE;
    }

    public boolean isLastPacket() {
        if(buffer[2] == 1){
            return true;
        }
        return false;
    }

    public int getSequenceNumber() {
//        Reconstructs the sequence number from the buffer
        return (int) (buffer[1] << 8 | buffer[0]);
    }
}
