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
        int previousSequenceNumber = -1;

        while(!endOfFile){
//            Create a new packet and put the data received in it
            Packet packet = new Packet();
            DatagramPacket receivedPacket = new DatagramPacket(packet.getBuffer(), Packet.PACKET_DEFAULT_BUFFER_SIZE);
            serverSocket.receive(receivedPacket);
            InetAddress IPAddress = receivedPacket.getAddress();
            int clientPort = receivedPacket.getPort();



            int sequenceNumber = packet.getSequenceNumber();
            AckPacket ackPacket;
//              Discard duplicate packets
            if(sequenceNumber == previousSequenceNumber + 1){
//              Write the data to the file output stream after stripping away the header and EoF bits.
                fileOutputStream.write(packet.getData());
                previousSequenceNumber = sequenceNumber;
                ackPacket = new AckPacket(sequenceNumber);
            } else {
                System.out.println("Discarding duplicate packet with # " + sequenceNumber);
                ackPacket = new AckPacket(previousSequenceNumber);
            }
//              Send ACK packet with corresponding sequence number back to client
            System.out.println("Sending ACK # " + ackPacket.getSequenceNumber());
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
