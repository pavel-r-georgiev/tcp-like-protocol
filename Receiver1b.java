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

//        System.out.println("File received successfully and saved as " + filename + ".");
    }

    public static void receiveFile(int port, String filename) throws IOException {
//        Create the server socket and output objects
        DatagramSocket serverSocket = new DatagramSocket(port);
        File file = new File(filename);
        FileOutputStream fileOutputStream = new FileOutputStream(file);

//        Flag to show end of received file
        boolean endOfFile = false;
//        Sequence number of previous successfully transferred file.
        int previousSequenceNumber = -1;

        while(!endOfFile){
//            Create a new packet and put the data received in it
            Packet packet = new Packet();
            DatagramPacket receivedPacket = new DatagramPacket(packet.getBuffer(), Packet.PACKET_DEFAULT_BUFFER_SIZE);
            serverSocket.receive(receivedPacket);

//            Get IP and port of sender - used to send ACKs back
            InetAddress IPAddress = receivedPacket.getAddress();
            int clientPort = receivedPacket.getPort();

//            System.out.println("Packet received: # " + packet.getSequenceNumber());

//            Get the true length of data received and sequence number of packet
            int dataLength = receivedPacket.getLength();
            int sequenceNumber = packet.getSequenceNumber();
            AckPacket ackPacket;

//              Discard duplicate packets
            if(sequenceNumber == (previousSequenceNumber + 1) % 2){
//              Write the data to the file output stream after stripping away the header and EoF bits.
                fileOutputStream.write(packet.getData(dataLength));
                previousSequenceNumber = sequenceNumber;
                ackPacket = new AckPacket(sequenceNumber);
            } else {
//                Save last sequence number in the ACK instead
                ackPacket = new AckPacket(previousSequenceNumber);
//                System.out.println("Discarding duplicate packet with # " + sequenceNumber);
            }
//              Send ACK packet with corresponding sequence number back to client
            ackPacket.sendAck(IPAddress, clientPort, serverSocket);

//            System.out.println("Sending ACK # " + ackPacket.getSequenceNumber());

//            If this is the last packet - close the file stream and change flag
            if(packet.isLastPacket()){
                endOfFile = true;
                fileOutputStream.close();
            }

        }
//          Close server socket
        serverSocket.close();
    }
}
