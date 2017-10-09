/* Pavel Georgiev s1525701 */
import java.util.Arrays;

/**
 * Created by Pavel on 08/10/2017.
 */
public class Packet {
    private byte[] buffer;
    public static final int PACKET_DATA_SIZE = 1024;
    public static final int PACKET_HEADER_SIZE = 3;
    public static final int PACKET_BUFFER_SIZE = PACKET_HEADER_SIZE + PACKET_DATA_SIZE;


    public Packet(){
        this.buffer = new byte[PACKET_BUFFER_SIZE];
    }

    public Packet(byte[] buffer){
        this.buffer = buffer;
    }

    public Packet(byte[] data, int sequenceNumber, boolean endOfFile){
         this();
//       Store sequence number based in header
        buffer[0] = (byte) (sequenceNumber & 0xFF);
        buffer[1] = (byte) ((sequenceNumber >> 8) & 0xFF);

//        Mark end of file
        if(endOfFile){
            buffer[2] = 1;
        }

//        Transfer data into packet buffer
        for(int i = 0; i < Math.min(PACKET_BUFFER_SIZE,data.length); i++){
            buffer[i + 3] = data[i];
        }
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public byte[] getData() { return Arrays.copyOfRange(buffer, PACKET_HEADER_SIZE, PACKET_BUFFER_SIZE); }

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
