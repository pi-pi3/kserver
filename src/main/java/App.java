import java.net.UnknownHostException;
import java.io.IOException;
import java.util.Scanner;

import server.KlausurenServer;

public class App {
    public static void usage(String arg0) {
        System.out.println("usage: " + arg0 + " PORT");
    }

    public static void main(String[] args) {
        String port = null;
        int portno = -1;

        if (args.length < 2) {
            port = new Scanner(System.in).nextLine().trim();
        } else if(!args[1].equals("-h")) {
            port = args[1];
        } else {
            App.usage(args[0]);
            System.exit(1);
        }

        try {
            portno = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            System.exit(2);
        }

        KlausurenServer sv = new KlausurenServer(portno);
        try {
            sv.start();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(3);
        }
    }
}
