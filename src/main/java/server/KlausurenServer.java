package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.io.IOException;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import java.util.Scanner;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

public class KlausurenServer {
    private static final String DB_PATH = "db_klausuren";

    private String host;
    private int portno;

    private boolean running;
    private ServerSocket socket;
    private Map<String, TreeSet<Integer>> db;
    private File dbFile;

    private Pattern commandPat;
    private Pattern keyPat;
    private Pattern valuePat;
    private Pattern splitPat;

    public KlausurenServer(int portno) {
        this("127.0.0.1", portno);
    }

    public KlausurenServer(String host, int portno) {
        this.host = host;
        this.portno = portno;

        int flags = Pattern.CASE_INSENSITIVE;
        this.commandPat = Pattern.compile("put|get|del|getall|stop", flags);
        this.keyPat = Pattern.compile(".+@.+");
        this.valuePat = Pattern.compile("\\d+(?:, *\\d+)*");
        this.splitPat = Pattern.compile(", *");
    }

    public synchronized Response put(Scanner sc) {
        Collection<?> response = null;

        String key = sc.next(this.keyPat);
        String value = sc.findInLine(this.valuePat);
        List<Integer> values = Arrays.stream(this.splitPat.split(value))
            .map(Integer::parseInt)
            .collect(Collectors.toList());
        if (this.db.containsKey(key)) {
            TreeSet<Integer> oldset = this.db.get(key);
            response = oldset;
        }

        TreeSet<Integer> newset = new TreeSet<>();
        newset.addAll(values);
        this.db.put(key, newset);

        return new Response(true, response);
    }

    public Response get(Scanner sc) {
        String key = sc.next(this.keyPat);
        return new Response(this.db.get(key));
    }

    public synchronized Response del(Scanner sc) {
        String key = sc.next(this.keyPat);
        return new Response(this.db.remove(key));
    }

    public Response getall(Scanner sc) {
        List<TreeSet<Integer>> union = new ArrayList<>();

        for (TreeSet<Integer> set : this.db.values()) {
            boolean contained = union.stream()
                .anyMatch((TreeSet<Integer> s) -> s.containsAll(set));

            if (!contained) {
                union.add(set);
            }
        }

        return new Response(union);
    }

    public synchronized Response stop() {
        this.running = false;
        return new Response(true);
    }

    public Response exec(String line) {
        Response resp = Response.FAILURE;

        Scanner sc = new Scanner(line);
        try {
            String command = sc.next(this.commandPat).toLowerCase();

            switch (command) {
                case "put":
                    resp = this.put(sc);
                    break;
                case "get":
                    resp = this.get(sc);
                    break;
                case "del":
                    resp = this.del(sc);
                    break;
                case "getall":
                    resp = this.getall(sc);
                    break;
                case "stop":
                    resp = this.stop();
                    break;
            }
        } catch (NoSuchElementException e) {
            // invalid command
            // e.printStackTrace();
            return Response.FAILURE;
        }

        if (sc.hasNext()) {
            // invalid command
            return Response.FAILURE;
        }

        return resp;
    }

    public void load() throws IOException {
        this.load(this.dbFile);
    }

    public synchronized void load(File db) throws IOException {
        BufferedReader r = new BufferedReader(new FileReader(db));

        String line;
        while ((line = r.readLine()) != null) {
            Scanner sc = new Scanner(line);
            this.put(sc);
        }

        r.close();
    }

    public void save() throws IOException {
        this.save(this.dbFile);
    }

    public synchronized void save(File db) throws IOException {
        BufferedWriter w = new BufferedWriter(new FileWriter(db));

        for (Map.Entry<String, TreeSet<Integer>> e : this.db.entrySet()) {
            w.write(e.getKey());
            w.write(' ');
            String values = e.getValue()
                .stream()
                .map((Integer i) -> String.valueOf(i))
                .collect(Collectors.joining(","));
            w.write(values);
            w.write('\n');
        }

        w.close();
    }

    public synchronized void start() throws UnknownHostException, IOException {
        this.db = new HashMap<>();
        this.dbFile = new File(DB_PATH);
        if (this.dbFile.exists()) {
            this.load();
        } else {
            this.dbFile.createNewFile();
        }

        InetAddress addr;

        try {
            addr = InetAddress.getByName(this.host);
        } catch (UnknownHostException e) {
            throw e;
        }

        try {
            this.socket = new ServerSocket(this.portno, 0, addr);
        } catch (IOException e) {
            throw e;
        }

        this.running = true;
        while (this.running) {
            try {
                Socket conn = this.socket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                DataOutputStream out = new DataOutputStream(conn.getOutputStream());

                String line = in.readLine();
                if (line != null) {
                    Response resp = this.exec(line);
                    out.writeBytes(resp.toString());
                }

                in.close();
                out.close();
                conn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.save();
        this.socket.close();
    }
}
