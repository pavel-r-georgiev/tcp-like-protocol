import sun.security.x509.IPAddressName;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by Pavel on 08/10/2017.
 */
public class Sender1a {

    public static void main(String[] args) throws UnknownHostException, FileNotFoundException {
//        Get host, port and filename from command line arguments
        final String remoteHost = args[0];
        final String remotePort = args[1];
        final String filename = args[2];

        InetAddress IPAddress = InetAddress.getByName(remoteHost);
        int port = Integer.parseInt(remotePort);
        FileInputStream file = new FileInputStream(filename);

        sendFile(IPAddress, port, file);
    }

    private static void sendFile(InetAddress IPAddress, int port, FileInputStream file){

    }
}
