/* Pavel Georgiev s1525701 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class Receiver2a {
    static boolean debug = false;
    public static void main(String[] args) throws IOException {
        if(args.length != 2 && args.length != 3){
            System.err.println("Run with arguments <Port> <Filename> <Debug flag (optional)>.");
        }
//        Get port and filename from command line arguments
        final int port = Integer.parseInt(args[0]);
        final String filename = args[1];
//        Set debug flag to true - used to print debugging statements
        if(args.length == 3 && args[2].equals("debug")){
            debug = true;
        }
//        Store incoming packets to a file
        receiveFile(port, filename);

        if(debug) {
            System.out.println("File received successfully and saved as " + filename + ".");
        }
    }

    public static void receiveFile(int port, String filename) throws IOException {
//        Create the server socket and output objects
        DatagramSocket serverSocket = new DatagramSocket(port);
        File file = new File(filename);
        FileOutputStream fileOutputStream = new FileOutputStream(file);

//        Flag to show end of received file
        boolean endOfFile = false;
//        Sequence number of previous successfully transferred file.
        int expectedSequenceNumber = 1;
        int lastPacketNumber = 0;

        while(!endOfFile){
//            Create a new packet and put the data received in it
            Packet packet = new Packet();
            DatagramPacket receivedPacket = new DatagramPacket(packet.getBuffer(), Packet.PACKET_DEFAULT_BUFFER_SIZE);
            serverSocket.receive(receivedPacket);

//            Get IP and port of sender - used to send ACKs back
            InetAddress IPAddress = receivedPacket.getAddress();
            int clientPort = receivedPacket.getPort();

//            Get the true length of data received and sequence number of packet
            int dataLength = receivedPacket.getLength();
            int sequenceNumber = packet.getSequenceNumber();
            AckPacket ackPacket;

            if(debug){
                System.out.println("Packet received: # " + sequenceNumber);
            }

//              Discard duplicate packets
            if(sequenceNumber == expectedSequenceNumber){
//              Write the data to the file output stream after stripping away the header and EoF bits.
                fileOutputStream.write(packet.getData(dataLength));
                ackPacket = new AckPacket(sequenceNumber);
                expectedSequenceNumber++;
                lastPacketNumber = sequenceNumber;
            } else {
//                Save last sequence number in the ACK instead
                ackPacket = new AckPacket(lastPacketNumber);
                if(debug){
                    System.out.println("Discarding packet with # " + sequenceNumber);
                }
            }
//              Send ACK packet with corresponding sequence number back to client
            ackPacket.sendAck(IPAddress, clientPort, serverSocket);

            if(debug){
                System.out.println("Sending ACK # " + ackPacket.getSequenceNumber());
            }

//            If this is the last packet - close the file stream and change flag
            if(packet.isLastPacket() && sequenceNumber == expectedSequenceNumber - 1){
                endOfFile = true;
                fileOutputStream.close();
            }

        }

        serverSocket.close();
    }
}