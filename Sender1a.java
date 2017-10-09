import sun.security.x509.IPAddressName;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;

/**
 * Created by Pavel on 08/10/2017.
 */
public class Sender1a {

    public static void main(String[] args) throws IOException {
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
            if(position >= file.length() - 1024){
                endOfFile = true;
            }
//            Read from file and construct a packet to be sent
            byte[] data = new byte[1024];
            fileStream.read(data);
            Packet packet = new Packet(data, sequenceNumber, endOfFile);
            DatagramPacket sendPacket = new DatagramPacket(packet.getBuffer(), packet.getBuffer().length, IPAddress, port);

//            Send the packet and increment indices
            clientSocket.send(sendPacket);
            sequenceNumber++;
            position += 1024;

            try {
                Thread.sleep(10);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }

        clientSocket.close();

    }
}
