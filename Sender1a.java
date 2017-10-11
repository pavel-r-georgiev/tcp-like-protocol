/* Pavel Georgiev s1525701 */
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;

/**
 * Created by Pavel on 08/10/2017.
 */
public class Sender1a {

    public static void main(String[] args) throws IOException {
        if(args.length != 3){
            System.err.println("Run with arguments <RemoteHost> <Port> <Filename>.");
        }
//        Get host, port and filename from command line arguments
        final String remoteHost = args[0];
        final int port =  Integer.parseInt(args[1]);
        final String filename = args[2];

        InetAddress IPAddress = InetAddress.getByName(remoteHost);
        File file = new File(filename);

        sendFile(IPAddress, port, file);
    }

    private static void sendFile(InetAddress IPAddress, int port, File file) throws IOException {
//        Initialize socket for the sender
        DatagramSocket clientSocket = new DatagramSocket();
        FileInputStream fileStream = new FileInputStream(file);

        int sequenceNumber = 0;
        boolean endOfFile = false;
        int position = 0;

        while(!endOfFile){
//            Check if this is last packet of the file
            int bytesLeft = (int)(file.length() - position);
            if(bytesLeft <= 1024){
                endOfFile = true;
            }

//            If it is last packet reduce buffer size
            int dataSize = endOfFile ? bytesLeft : Packet.PACKET_DEFAULT_DATA_SIZE;

//            Read from file and construct a packet to be sent
            byte[] data = new byte[dataSize];
            fileStream.read(data);
            Packet packet = new Packet(data, sequenceNumber, endOfFile);
            DatagramPacket sendPacket = new DatagramPacket(packet.getBuffer(), packet.getBufferSize(), IPAddress, port);

//            Send the packet and increment indices
            clientSocket.send(sendPacket);
            sequenceNumber++;
            position += 1024;

//            Sleep for 10ms to avoid queue overflow
            try {
                Thread.sleep(10);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }

        clientSocket.close();

    }
}
