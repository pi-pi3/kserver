package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
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
    private static final int SO_TIMEOUT = 2000;
    private static final long THREAD_TIMEOUT = 100l;

    private int portno;

    private Boolean running;
    private ServerSocket socket;
    private Map<String, TreeSet<Integer>> db;
    private File dbFile;

    private Object runLock = new Object();

    public KlausurenServer(int portno) {
        this.portno = portno;
        this.running = false;
    }

    public boolean isRunning() {
        synchronized (this.running) {
            return this.running;
        }
    }

    public Response put(String key, List<Integer> values) {
        Collection<?> response = null;

        synchronized (this.db) {
            if (this.db.containsKey(key)) {
                TreeSet<Integer> oldset = this.db.get(key);
                response = oldset;
            }

            TreeSet<Integer> newset = new TreeSet<>();
            newset.addAll(values);
            this.db.put(key, newset);
        }

        return new Response(true, response);
    }

    public Response get(String key) {
        synchronized (this.db) {
            return new Response(this.db.get(key));
        }
    }

    public Response del(String key) {
        synchronized (this.db) {
            return new Response(this.db.remove(key));
        }
    }

    public Response getall() {
        List<TreeSet<Integer>> union = new ArrayList<>();

        synchronized (this.db) {
            for (TreeSet<Integer> set : this.db.values()) {
                boolean contained = union.stream()
                    .anyMatch((TreeSet<Integer> s) -> s.containsAll(set));

                if (!contained) {
                    union.add(set);
                }
            }
        }

        return new Response(union);
    }

    public Response stop() {
        synchronized (this.running) {
            this.running = false;
        }
        return new Response(true);
    }

    public void load() throws IOException {
        BufferedReader r = new BufferedReader(new FileReader(this.dbFile));

        String line;
        Interpreter interp = new Interpreter(this);
        while ((line = r.readLine()) != null) {
            interp.exec("put " + line);
        }

        r.close();
    }

    public void save() throws IOException {
        BufferedWriter w = new BufferedWriter(new FileWriter(this.dbFile));

        synchronized (this.db) {
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
        }

        w.close();
    }

    public void start() throws IOException {
        synchronized (this.runLock) {
            this.db = new HashMap<>();
            this.dbFile = new File(DB_PATH);

            if (this.dbFile.exists()) {
                this.load();
            } else {
                this.dbFile.createNewFile();
            }

            this.socket = new ServerSocket(this.portno);
            this.socket.setSoTimeout(SO_TIMEOUT);

            List<Thread> threads = new ArrayList<>();
            this.running = true;

            while (this.isRunning()) {
                try {
                    Socket conn = this.socket.accept();
                    Thread handler = new Thread(new Handler(this, conn));
                    handler.start();
                    threads.add(handler);
                } catch (SocketTimeoutException e) {
                    // ignore & continue
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            for (Thread handler : threads) {
                try {
                    handler.join(THREAD_TIMEOUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            this.save();
            this.socket.close();
        }
    }
}
