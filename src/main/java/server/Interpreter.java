package server;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Interpreter {
    private static final Pattern COMMAND_REGEX = Pattern.compile(
            "(?<command>put|get|del|getall|stop)" +  // command
            "(?:[ \\t]+(?<key>\\S+@\\S+)" +  // key
              "(?:[ \\t]+(?<value>\\d+(?:,[ \\t]*\\d+)*))?" +  // value (optional)
            ")?");  // key-val (optional)
    private static final Pattern SPLIT_REGEX = Pattern.compile(",[ \\t]*");

    private KlausurenServer server;

    public Interpreter(KlausurenServer server) {
        this.server = server;
    }

    private Response put(String key, String value) {
        if (key == null || value == null) {
            return Response.FAILURE;
        }

        List<Integer> values = Arrays.stream(SPLIT_REGEX.split(value))
            .map(Integer::parseInt)
            .collect(Collectors.toList());

        return this.server.put(key, values);
    }

    private Response get(String key, String value) {
        if (key == null || value != null) {
            return Response.FAILURE;
        }

        return this.server.get(key);
    }

    private Response del(String key, String value) {
        if (key == null || value != null) {
            return Response.FAILURE;
        }

        return this.server.del(key);
    }

    private Response getall(String key, String value) {
        if (key != null || value != null) {
            return Response.FAILURE;
        }

        return this.server.getall();
    }

    private Response stop(String key, String value) {
        if (key != null || value != null) {
            return Response.FAILURE;
        }

        return this.server.stop();
    }

    public Response exec(String line) {
        Response resp = Response.FAILURE;
        Matcher m = COMMAND_REGEX.matcher(line.trim());

        if (m.matches()) {
            String command = m.group("command");
            String key = m.group("key");
            String value = m.group("value");

            switch (command) {
                case "put":
                    return this.put(key, value);
                case "get":
                    return this.get(key, value);
                case "del":
                    return this.del(key, value);
                case "getall":
                    return this.getall(key, value);
                case "stop":
                    return this.stop(key, value);
            }
        }

        return resp;
    }
}
