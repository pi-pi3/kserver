package server;

import java.net.Socket;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Handler extends Interpreter implements Runnable {
    private Socket conn;
    private BufferedReader in;
    private DataOutputStream out;

    public Handler(KlausurenServer server, Socket conn) throws IOException {
        super(server);
        this.conn = conn;
        this.in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        this.out = new DataOutputStream(conn.getOutputStream());
    }

    public void run() {
        try {
            String line = in.readLine();
            if (line != null) {
                Response resp = this.exec(line);
                out.writeBytes(resp.toString());
            }

            this.in.close();
            this.out.close();
            this.conn.close();
        } catch (IOException e) {
            e.printStackTrace();
            // TODO
        }
    }
}
