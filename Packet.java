/**
 * Created by Pavel on 08/10/2017.
 */
public class Packet {
    final byte[] buffer = new byte[2017];

    public Packet(byte[] data, short sequenceNumber){
//        Store sequence number based in Little Endian manner
        buffer[0] = (byte)sequenceNumber;
        buffer[1] = (byte)(sequenceNumber >> 8);

//        Transfer data into packet buffer
        for(int i = 0; i < data.length; i++){
            buffer[i + 3] = data[i];
        }
    }

    public Packet(byte[] data, short sequenceNumber, boolean endOfFile){
        this(data, sequenceNumber);

//        Mark end of file
        if(endOfFile){
            buffer[2] = 1;
        }
    }
}
