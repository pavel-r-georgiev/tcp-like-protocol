/* Pavel Georgiev s1525701 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;


public class Receiver1a {

    public static void main(String[] args) throws IOException {
//        Get port and filename from command line arguments
        final int port = Integer.parseInt(args[0]);
        final String filename = args[2];

       receiveFile(port, filename);

    }

    public static void receiveFile(int port, String filename) throws IOException {
        DatagramSocket serverSocket = new DatagramSocket(port);
        File file = new File(filename);
        FileOutputStream fileOutputStream = new FileOutputStream(file);

        boolean endOfFile = false;

        while(!endOfFile){
            Packet packet = new Packet();
            DatagramPacket receivePacket = new DatagramPacket(packet.getBuffer(), Packet.PACKET_BUFFER_SIZE);
            serverSocket.receive(receivePacket);
            fileOutputStream.write(packet.getData());

            if(packet.isLastPacket()){
                endOfFile = true;
                fileOutputStream.close();
                serverSocket.close();
            }

        }


    }
}
