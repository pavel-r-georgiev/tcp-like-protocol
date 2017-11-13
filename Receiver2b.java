/* Pavel Georgiev s1525701 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Comparator;
import java.util.TreeSet;


public class Receiver2b {
        static boolean debug = false;
        public static void main(String[] args) throws IOException {
            if(args.length != 3 && args.length != 4){
                System.err.println("Run with arguments <Port> <Filename> <Window Size> <Debug flag (optional)>.");
            }
//        Get port and filename from command line arguments
            final int port = Integer.parseInt(args[0]);
            final String filename = args[1];
            final int windowSize = Integer.parseInt(args[2]);
//        Set debug flag to true - used to print debugging statements
            if(args.length == 4 && args[3].equals("debug")){
                debug = true;
            }
//        Store incoming packets to a file
            receiveFile(port, filename, windowSize);

            if(debug) {
                System.out.println("File received successfully and saved as " + filename + ".");
            }
        }

        public static void receiveFile(int port, String filename, int windowSize) throws IOException {
//        Create the server socket and output objects
            DatagramSocket serverSocket = new DatagramSocket(port);
            File file = new File(filename);
            FileOutputStream fileOutputStream = new FileOutputStream(file);

            TreeSet<Packet> bufferedPackets = new TreeSet<Packet>(new Comparator<Packet>() {
//                Old comparator because DICE uses java 1.6....
                @Override
                public int compare(Packet o1, Packet o2) {
                    return o1.getSequenceNumber() - o2.getSequenceNumber();
                }
            });

            int base = 1;

//        Flag to show end of received file
            boolean endOfFile = false;
//        Sequence number of previous successfully transferred file.
            int expectedSequenceNumber = 1;

            while(!endOfFile){
//            Create a new packet and put the data received in it
                Packet packet = new Packet();
                DatagramPacket receivedPacket = new DatagramPacket(packet.getBuffer(), Packet.PACKET_DEFAULT_BUFFER_SIZE);
                serverSocket.receive(receivedPacket);

//            Get IP and port of sender - used to send ACKs back
                InetAddress IPAddress = receivedPacket.getAddress();
                int clientPort = port + 1;

//              int clientPort = receivedPacket.getPort();
//            Get the true length of data received and sequence number of packet
                int dataLength = receivedPacket.getLength();
                int sequenceNumber = packet.getSequenceNumber();

                if(debug){
                    System.out.println("Packet received: # " + sequenceNumber);
                }

                AckPacket ackPacket;
                ackPacket = new AckPacket(sequenceNumber);


                if(sequenceNumber >= base && sequenceNumber <= base + windowSize - 1) {
                    if(sequenceNumber == expectedSequenceNumber){
//              Write the data to the file output stream after stripping away the header and EoF bits.
                        fileOutputStream.write(packet.getData(dataLength));
                        expectedSequenceNumber++;
                        base = sequenceNumber;
                    } else {
//                        Buffer the packet if it is out of order.
                        bufferedPackets.add(packet);
                        if (debug) {
                            System.out.println("Buffering packet with # " + sequenceNumber);
                        }
                    }
                } else if(sequenceNumber < base - windowSize || sequenceNumber > base - 1) {
//                    Packet is out of bounds where it should be ACKed it, so just continue
                    continue;
                }

//              Send ACK packet with corresponding sequence number back to client
                ackPacket.sendAck(IPAddress, clientPort, serverSocket);

                if(debug){
                    System.out.println("Sending ACK # " + ackPacket.getSequenceNumber());
                }

//                Check for buffered packets that can written to file after file transmission
                while (bufferedPackets.size() > 0 && bufferedPackets.first().getSequenceNumber() == expectedSequenceNumber) {
                    packet = bufferedPackets.pollFirst();
                    base = packet.getSequenceNumber();
                    expectedSequenceNumber++;
                    fileOutputStream.write(packet.getData(dataLength));
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

