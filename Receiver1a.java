/* Pavel Georgiev s1525701 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;


public class Receiver1a {

    public static void main(String[] args) throws IOException {
        if(args.length != 2){
            System.err.println("Run with arguments <Port> <Filename>.");
        }
//        Get port and filename from command line arguments
        final int port = Integer.parseInt(args[0]);
        final String filename = args[1];
//        Store incoming packets to a file
       receiveFile(port, filename);

       System.out.println("File received successfully and saved as " + filename + ".");

    }

    public static void receiveFile(int port, String filename) throws IOException {
//        Create the server socket and output objects
        DatagramSocket serverSocket = new DatagramSocket(port);
        File file = new File(filename);
        FileOutputStream fileOutputStream = new FileOutputStream(file);

        boolean endOfFile = false;

        while(!endOfFile){
//            Create a new packet and put the data received in it
            Packet packet = new Packet();
            DatagramPacket receivePacket = new DatagramPacket(packet.getBuffer(), Packet.PACKET_DEFAULT_BUFFER_SIZE);
            serverSocket.receive(receivePacket);
            System.out.println("Packet received: # " + packet.getSequenceNumber());

//            Get the true length of data received and write the data to the file output stream after stripping away the header and EoF bits.
            int dataLength = receivePacket.getLength();
            fileOutputStream.write(packet.getData(dataLength));

//            If this is the last packet - close the file stream and the server socket
            if(packet.isLastPacket()){
                endOfFile = true;
                fileOutputStream.close();
                serverSocket.close();
            }

        }


    }
}
