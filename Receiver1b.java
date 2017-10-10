/* Pavel Georgiev s1525701 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class Receiver1b {

    public static void main(String[] args) throws IOException {
//        Get port and filename from command line arguments
        final int port = Integer.parseInt(args[0]);
        final String filename = args[2];
//        Store incoming packets to a file
        receiveFile(port, filename);

    }

    public static void receiveFile(int port, String filename) throws IOException {
//        Create the server socket and output objects
        DatagramSocket serverSocket = new DatagramSocket(port);
        File file = new File(filename);
        FileOutputStream fileOutputStream = new FileOutputStream(file);

        boolean endOfFile = false;
        int previousSequenceNumber = -1;

        while(!endOfFile){
//            Create a new packet and put the data received in it
            Packet packet = new Packet();
            DatagramPacket receivePacket = new DatagramPacket(packet.getBuffer(), Packet.PACKET_BUFFER_SIZE);
            serverSocket.receive(receivePacket);
            InetAddress IPAddress = serverSocket.getInetAddress();
            int clientPort = serverSocket.getPort();


            int sequenceNumber = packet.getSequenceNumber();
            AckPacket ackPacket;
//              Discard duplicate packets
            if(sequenceNumber == previousSequenceNumber + 1){
//              Write the data to the file output stream after stripping away the header and EoF bits.
                fileOutputStream.write(packet.getData());
//              Create ACK packet to acknowledge file received correctly
                previousSequenceNumber = sequenceNumber;
                ackPacket = new AckPacket(sequenceNumber);
            } else {
//              Create ACK packet to show that latest packet does not have correct sequence number
                ackPacket = new AckPacket(previousSequenceNumber);
            }
//              Send ACK packet back to client
            ackPacket.sendAck(IPAddress, clientPort, serverSocket);

//            If this is the last packet - close the file stream and the server socket
            if(packet.isLastPacket()){
                endOfFile = true;
                fileOutputStream.close();
                serverSocket.close();
            }

        }


    }
}
