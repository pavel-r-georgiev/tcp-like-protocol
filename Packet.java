/**
 * Created by Pavel on 08/10/2017.
 */
public class Packet {
    private byte[] buffer;
    public static final int PACKET_SIZE = 1027;

    public Packet(){
        this.buffer = new byte[PACKET_SIZE];
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
        for(int i = 0; i < data.length; i++){
            buffer[i + 3] = data[i];
        }
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public int getSequenceNumber() {
        return (int) (buffer[1] << 8 | buffer[0]);
    }
}
